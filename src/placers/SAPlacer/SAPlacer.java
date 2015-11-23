package placers.SAPlacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Arrays;
import java.util.Random;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.exceptions.PlacementException;


import placers.Placer;
import visual.PlacementVisualizer;

abstract class SAPlacer extends Placer {

    public static void initOptions(Options options) {
        options.add("greedy", "place greedy", Boolean.FALSE);
        options.add("detailed", "place detailed", Boolean.FALSE);

        options.add("effort level", "multiplier for the number of swap iterations", new Double(1));
        options.add("temperature", "multiplier for the starting temperature", new Double(1));

        options.add("rlim", "starting maximum distance for a swap", new Integer(-1));
        options.add("max rlim", "maximum rlim for all iterations", new Integer(-1));

        options.add("fix pins", "fix the IO pins", Boolean.TRUE);
    }


    private double Rlimd;
    private int Rlim, maxRlim;
    private double temperature;

    private final double temperatureMultiplier;
    private final double temperatureMultiplierGlobalPlacement = 5;

    private final boolean fixPins;
    private final boolean greedy, detailed;
    private final int movesPerTemperature;

    protected boolean circuitChanged = true;
    private double[] deltaCosts;


    protected SAPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);


        this.greedy = this.options.getBoolean("greedy");
        this.detailed = this.options.getBoolean("detailed");

        this.fixPins = this.options.getBoolean("fix pins");

        double effortLevel = this.options.getDouble("effort level");
        this.movesPerTemperature = (int) (effortLevel * Math.pow(this.circuit.getNumGlobalBlocks(), 4.0/3.0));

        this.temperatureMultiplier = this.options.getDouble("temperature");

        // Set Rlim options
        int size = Math.max(this.circuit.getWidth(), this.circuit.getHeight());

        int RlimOption = this.options.getInteger("rlim");
        if(RlimOption == -1) {
            RlimOption = size;
        }

        int maxRlimOption = this.options.getInteger("max rlim");
        if(maxRlimOption == -1) {
            maxRlimOption = size;
        }

        // Set maxRlim first, because Rlim depends on it
        this.setMaxRlim(maxRlimOption);
        this.setRlimd(RlimOption);
    }


    protected abstract void initializePlace();
    protected abstract void initializeSwapIteration();
    protected abstract String getStatistics();
    protected abstract double getCost();
    protected abstract double getDeltaCost(Swap swap);
    protected abstract void pushThrough(int iteration);
    protected abstract void revert(int iteration);


    @Override
    public void initializeData() {
        // Do nothing
    }


    @Override
    public void place() throws PlacementException {
        this.initializePlace();

        if(this.greedy) {
            this.doSwapIteration();

        } else {
            this.calculateInitialTemperature();

            this.logger.println("Initial temperature: " + this.temperature);


            int iteration = 0;

            // Do placement
            while(this.temperature > 0.005 * this.getCost() / this.circuit.getNumGlobalBlocks()) {
                int numSwaps = this.doSwapIteration();
                double alpha = ((double) numSwaps) / this.movesPerTemperature;

                this.updateRlim(alpha);
                this.updateTemperature(alpha);

                this.logger.printf("Temperature %d = %.9f, Rlim = %d, %s\n",
                        iteration, this.temperature, this.Rlim, this.getStatistics());

                iteration++;
            }

            this.logger.println();
        }
    }


    private void calculateInitialTemperature() throws PlacementException {
        if(this.detailed) {
            this.temperature = this.calculateInitialTemperatureDetailed();
        } else {
            this.temperature = this.calculateInitialTemperatureGlobal();
        }
    }

    private double calculateInitialTemperatureGlobal() throws PlacementException {
        int numSamples = this.circuit.getNumGlobalBlocks();
        double stdDev = this.doSwapIteration(numSamples, false);

        return this.temperatureMultiplier * this.temperatureMultiplierGlobalPlacement * stdDev;
    }

    private double calculateInitialTemperatureDetailed() throws PlacementException {
        // Use the method described in "Temperature Measurement and
        // Equilibrium Dynamics of Simulated Annealing Placements"

        int numSamples = Math.max(this.circuit.getNumGlobalBlocks() / 5, 500);
        this.doSwapIteration(numSamples, false);

        Arrays.sort(this.deltaCosts);

        int zeroIndex = Arrays.binarySearch(this.deltaCosts, 0);
        if(zeroIndex < 0) {
            zeroIndex = -zeroIndex - 1;
        }

        double Emin = integral(this.deltaCosts, 0, zeroIndex, 0);
        double maxEplus = integral(this.deltaCosts, zeroIndex, numSamples, 0);

        if(maxEplus < Emin) {
            this.logger.raise("SA failed to get a temperature estimate");
        }

        double minT = 0;
        double maxT = Double.MAX_VALUE;

        // very coarse estimate
        double temperature = this.deltaCosts[this.deltaCosts.length - 1] / 1000;

        while(minT == 0 || maxT / minT > 1.1) {
            double Eplus = integral(this.deltaCosts, zeroIndex, numSamples, temperature);

            if(Emin < Eplus) {
                if(temperature < maxT) {
                    maxT = temperature;
                }

                if(minT == 0) {
                    temperature /= 8;
                } else {
                    temperature = (maxT + minT) / 2;
                }

            } else {
                if(temperature > minT) {
                    minT = temperature;
                }

                if(maxT == Double.MAX_VALUE) {
                    temperature *= 8;
                } else {
                    temperature = (maxT + minT) / 2;
                }
            }
        }

        return temperature * this.temperatureMultiplier;
    }

    private double integral(double[] values, int start, int stop, double temperature) {
        double sum = 0;
        for(int i = start; i < stop; i++) {
            if(temperature == 0) {
                sum += values[i];
            } else {
                sum += values[i] * Math.exp(-values[i] / temperature);
            }
        }

        return Math.abs(sum / values.length);
    }



    private int doSwapIteration() throws PlacementException {
        return (int) this.doSwapIteration(this.movesPerTemperature, true);
    }

    private double doSwapIteration(int moves, boolean pushThrough) throws PlacementException {

        this.initializeSwapIteration();

        int numSwaps = 0;


        double sumDeltaCost = 0;
        double quadSumDeltaCost = 0;
        if(!pushThrough) {
            this.deltaCosts = new double[moves];
        }


        for (int i = 0; i < moves; i++) {
            Swap swap = this.findSwap(this.Rlim);

            if((swap.getBlock1() == null || !swap.getBlock1().isFixed())
                    && (swap.getBlock2() == null || !swap.getBlock2().isFixed())) {

                double deltaCost = this.getDeltaCost(swap);


                if(pushThrough) {
                    if(deltaCost <= 0 || (this.greedy == false && this.random.nextDouble() < Math.exp(-deltaCost / this.temperature))) {

                        swap.apply();
                        numSwaps++;

                        this.pushThrough(i);
                        this.circuitChanged = true;

                    } else {
                        this.revert(i);
                    }

                } else {
                    this.revert(i);
                    this.deltaCosts[i] = deltaCost;
                    sumDeltaCost += deltaCost;
                    quadSumDeltaCost += deltaCost * deltaCost;
                }
            }
        }

        if(pushThrough) {
            return numSwaps;

        } else {
            double sumQuads = quadSumDeltaCost;
            double quadSum = sumDeltaCost * sumDeltaCost;

            double numBlocks = this.circuit.getNumGlobalBlocks();
            double quadNumBlocks = numBlocks * numBlocks;

            return Math.sqrt(Math.abs(sumQuads / numBlocks - quadSum / quadNumBlocks));
        }
    }



    protected Swap findSwap(int Rlim) {
        GlobalBlock fromBlock = null;
        AbstractSite toSite = null;
        do {
            // Find a suitable block
            do {
                fromBlock = this.circuit.getRandomBlock(this.random);
            } while(this.isFixed(fromBlock));

            // Find a suitable site near this block
            do {
                toSite = this.circuit.getRandomSite(fromBlock, Rlim, this.random);
            } while(toSite != null && fromBlock.getSite() == toSite);

            // If toSite == null, this means there are no suitable blocks near the block
            // Try another block
        } while(toSite == null);

        Swap swap = new Swap(fromBlock, toSite, this.random);
        return swap;
    }

    private boolean isFixed(GlobalBlock block) {
        // Only IO blocks are fixed, if fixPins option is true
        return this.fixPins && block.getCategory() == BlockCategory.IO;
    }



    protected final void updateTemperature(double alpha) {
        double gamma;

        if (alpha > 0.96) {
            gamma = 0.5;
        } else if (alpha > 0.8) {
            gamma = 0.9;
        } else if (alpha > 0.15) {
            gamma = 0.95;
        } else {
            gamma = 0.8;
        }

        this.temperature *= gamma;
    }


    protected final int getRlim() {
        return this.Rlim;
    }
    protected final double getRlimd() {
        return this.Rlimd;
    }

    protected final void setMaxRlim(int maxRlim) {
        this.maxRlim = maxRlim;
    }
    protected final void setRlimd(double Rlimd) {
        this.Rlimd = Rlimd;
        this.Rlim = (int) Math.round(this.Rlimd);
    }


    protected final void updateRlim(double alpha) {
        this.updateRlim(alpha, this.maxRlim);
    }

    protected final void updateRlim(double alpha, int maxValue) {
        double newRlimd = this.Rlimd * (1 - 0.44 + alpha);

        if(newRlimd > maxValue) {
            newRlimd = maxValue;
        }

        if(newRlimd < 1) {
            newRlimd = 1;
        }

        this.setRlimd(newRlimd);
    }
}