package placers.simulatedannealing;

import interfaces.Logger;
import interfaces.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.block.GlobalBlock;
import circuit.block.Site;
import circuit.exceptions.PlacementException;


import placers.Placer;
import visual.PlacementVisualizer;

abstract class SimulatedAnnealingPlacer extends Placer {

    private static final String
        O_GREEDY = "greedy",
        O_DETAILED = "detailed",
        O_EFFORT_LEVEL = "effort level",
        O_EFFORT_EXPONENT = "effort exponent",
        O_TEMPERATURE = "temperature",
        O_RLIM = "rlim",
        O_MAX_RLIM = "max rlim",
        O_FIX_IO_PINS = "fix io pins";

    public static void initOptions(Options options) {
        options.add(
                O_GREEDY,
                "place greedy",
                Boolean.FALSE);

        options.add(
                O_DETAILED,
                "place detailed",
                Boolean.FALSE);


        options.add(
                O_EFFORT_LEVEL,
                "multiplier for the number of swap iterations",
                new Double(1));

        options.add(
                O_EFFORT_EXPONENT,
                "exponent to calculater inner num",
                new Double(4.0 / 3.0));

        options.add(
                O_TEMPERATURE,
                "multiplier for the starting temperature",
                new Double(1));


        options.add(
                O_RLIM,
                "maximum distance for a swap at start of placement",
                new Integer(-1));

        options.add(
                O_MAX_RLIM,
                "maximum rlim for all iterations",
                new Integer(-1));


        options.add(
                O_FIX_IO_PINS,
                "fix the IO pins",
                Boolean.TRUE);
    }


    protected double rlim;
    protected int initialRlim, maxRlim;
    private double temperature;

    private final double temperatureMultiplier;

    private final boolean fixPins;
    private final boolean greedy, detailed;
    protected final int movesPerTemperature;

    protected boolean circuitChanged = true;
    private double[] deltaCosts;


    protected SimulatedAnnealingPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);


        this.greedy = this.options.getBoolean(O_GREEDY);
        this.detailed = this.options.getBoolean(O_DETAILED);

        this.fixPins = this.options.getBoolean(O_FIX_IO_PINS);

        double effortLevel = this.options.getDouble(O_EFFORT_LEVEL);
        double effortExponent = this.options.getDouble(O_EFFORT_EXPONENT);
        this.movesPerTemperature = (int) (effortLevel * Math.pow(this.circuit.getNumGlobalBlocks(), effortExponent));

        this.temperatureMultiplier = this.options.getDouble(O_TEMPERATURE);

        // Set Rlim options
        int size = Math.max(this.circuit.getWidth(), this.circuit.getHeight()) - 2;

        int RlimOption = this.options.getInteger(O_RLIM);
        if(RlimOption == -1) {
            RlimOption = size - 1;
        }

        int maxRlimOption = this.options.getInteger(O_MAX_RLIM);
        if(maxRlimOption == -1) {
            maxRlimOption = size - 1;
        }

        this.initialRlim = RlimOption;

        this.maxRlim = maxRlimOption;
        this.rlim = Math.min(RlimOption, this.maxRlim);
    }


    protected abstract void addStatisticsTitlesSA(List<String> titles);
    protected abstract void addStats(List<String> statistics);

    protected abstract void initializePlace();
    protected abstract void initializeSwapIteration();
    protected abstract double getCost();
    protected abstract double getDeltaCost(Swap swap);
    protected abstract void pushThrough(int iteration);
    protected abstract void revert(int iteration);


    @Override
    public void initializeData() {
        // Do nothing
    }

    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("iteration");
        titles.add("temperature");
        titles.add("rlim");
        titles.add("succes rate");
        titles.add("t multiplier");

        this.addStatisticsTitlesSA(titles);
    }

    private void printStatistics(Integer iteration, Double temperature, Double rlim, Double succesRate, Double gamma) {
        List<String> stats = new ArrayList<>();

        stats.add(iteration.toString());
        stats.add(String.format("%.4g", temperature));
        stats.add(String.format("%.3g", rlim));
        stats.add(String.format("%.3f", succesRate));
        stats.add(gamma.toString());

        this.addStats(stats);

        this.printStats(stats.toArray(new String[0]));
    }


    @Override
    protected void doPlacement() throws PlacementException {
        this.initializePlace();

        int iteration = 0;

        if(!this.greedy) {
            this.calculateInitialTemperature();

            // Do placement
            while(this.temperature > 0.005 * this.getCost() / this.circuit.getNumGlobalBlocks()) {
                int numSwaps = this.doSwapIteration();
                double alpha = ((double) numSwaps) / this.movesPerTemperature;

                double previousTemperature = this.temperature;
                double previousRlim = this.rlim;
                this.updateRlim(alpha);
                double gamma = this.updateTemperature(alpha);

                this.printStatistics(iteration, previousTemperature, previousRlim, alpha, gamma);

                iteration++;
            }

            this.rlim = 3;
        }

        // Finish with a greedy iteration
        this.temperature = 0;
        int numSwaps = this.doSwapIteration();
        double alpha = ((double) numSwaps) / this.movesPerTemperature;
        this.printStatistics(iteration, this.temperature, this.rlim, alpha, 0.0);


        this.logger.println();
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

        return this.temperatureMultiplier * stdDev;
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

        int intRlim = (int) Math.round(this.rlim);

        for (int i = 0; i < moves; i++) {
            Swap swap = this.findSwap(intRlim);
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
        while(true) {
            // Find a suitable from block
            GlobalBlock fromBlock = null;
            do {
                fromBlock = this.circuit.getRandomBlock(this.random);
            } while(this.isFixed(fromBlock));

            BlockType blockType = fromBlock.getType();

            int freeAbove = 0;
            if(fromBlock.isInMacro()) {
                fromBlock = fromBlock.getMacro().getBlock(0);
                freeAbove = fromBlock.getMacro().getHeight() - 1;
            }

            int column = fromBlock.getColumn();
            int row = fromBlock.getRow();
            int minRow = Math.max(1, row - Rlim);
            int maxRow = Math.min(this.circuit.getHeight() - 2 - freeAbove, row + Rlim);

            // Find a suitable site near this block
            int maxTries = Math.min(4 * Rlim * Rlim / fromBlock.getType().getHeight(), 10);
            for(int tries = 0; tries < maxTries; tries++) {
                Site toSite = (Site) this.circuit.getRandomSite(blockType, column, Rlim, minRow, maxRow, this.random);

                // If toSite is null, no swap is possible with this fromBlock
                // Go find another fromBlock
                if(toSite == null) {
                    break;

                // Check if toSite contains fromBlock
                } else if(!fromBlock.getSite().equals(toSite)) {

                    // Make sure toSite doesn't contain a block that is in a macro
                    // (This is also not supported in VPR)
                    boolean toBlocksInMacro = false;
                    int toColumn = toSite.getColumn();
                    int toMinRow = toSite.getRow();
                    int toMaxRow = toMinRow + freeAbove;
                    for(int toRow = toMinRow; toRow <= toMaxRow; toRow++) {
                        GlobalBlock toBlock = ((Site) this.circuit.getSite(toColumn, toRow)).getBlock();
                        if(toBlock != null && toBlock.isInMacro()) {
                            toBlocksInMacro = true;
                            break;
                        }
                    }

                    if(!toBlocksInMacro) {
                        Swap swap = new Swap(this.circuit, fromBlock, toSite);
                        return swap;
                    }
                }
            }
        }
    }

    private boolean isFixed(GlobalBlock block) {
        // Only IO blocks are fixed, if fixPins option is true
        return this.fixPins && block.getCategory() == BlockCategory.IO;
    }



    protected final double updateTemperature(double alpha) {
        double gamma;

        if (alpha > 0.96) {
            gamma = 0.5;
        } else if (alpha > 0.8) {
            gamma = 0.9;
        } else if (alpha > 0.15  || this.rlim > 1) {
            gamma = 0.95;
        } else {
            gamma = 0.8;
        }

        this.temperature *= gamma;

        return gamma;
    }


    protected final void setMaxRlim(int maxRlim) {
        this.maxRlim = maxRlim;
    }

    protected final void updateRlim(double alpha) {
        this.rlim *= (1 - 0.44 + alpha);

        this.rlim = Math.max(Math.min(this.rlim, this.maxRlim), 1);
    }
}