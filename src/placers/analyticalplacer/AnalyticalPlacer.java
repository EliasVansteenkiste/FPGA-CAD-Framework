package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Option;
import interfaces.Options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.block.AbstractBlock;
import circuit.block.GlobalBlock;
import circuit.exceptions.PlacementException;
import circuit.pin.AbstractPin;

import placers.Placer;
import placers.analyticalplacer.linear_solver.LinearSolver;
import placers.analyticalplacer.linear_solver.LinearSolverComplete;
import placers.analyticalplacer.linear_solver.LinearSolverGradient;
import visual.PlacementVisualizer;

public abstract class AnalyticalPlacer extends Placer {

    protected static enum SolveMode {GRADIENT, COMPLETE};

    public static void initOptions(Options options) {
        options.add(new Option("solve mode", "either \"complete\" or \"gradient\"", "gradient"));

        options.add(new Option("max utilization", "comma-separated list of maximum tile capacity for each iteration", "1"));
        options.add(new Option("anchor weight", "starting anchor weight", new Double(0)));
        options.add(new Option("anchor weight increase", "value that is added to the anchor weight in each iteration", new Double(0.2)));

        options.add(new Option("stop ratio", "ratio between linear and legal cost above which placement should be stopped", new Double(0.95)));

        options.add(new Option("gradient speed", "ratio of distance to optimal position that is moved", new Double(0.4)));
        options.add(new Option("gradient iterations", "number of gradient steps to take in each outer iteration", new Integer(40)));
    }


    private final SolveMode algorithmSolveMode;

    private final double[] maxUtilizationSequence;
    private final double startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal;
    private final int gradientIterations;
    private double gradientSpeed;

    protected final Map<GlobalBlock, Integer> blockIndexes = new HashMap<>();
    protected final List<int[]> nets = new ArrayList<>();
    private int numBlocks, numIOBlocks;

    private double[] linearX, linearY;
    private LinearSolver solver;

    private CostCalculator costCalculator;
    protected Legalizer legalizer;




    public AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);


        // Max utilization sequence
        String maxUtilizationSequenceString = this.options.getString("max utilization");
        String[] maxUtilizationSequenceStrings = maxUtilizationSequenceString.split(",");
        this.maxUtilizationSequence = new double[maxUtilizationSequenceStrings.length];
        for(int i = 0; i < maxUtilizationSequenceStrings.length; i++) {
            this.maxUtilizationSequence[i] = Double.parseDouble(maxUtilizationSequenceStrings[i]);
        }

        // Anchor weights and stop ratio
        this.startingAnchorWeight = this.options.getDouble("anchor weight");
        this.anchorWeightIncrease = this.options.getDouble("anchor weight increase");
        this.stopRatioLinearLegal = this.options.getDouble("stop ratio");

        // Gradient solver
        String solveModeString = this.options.getString("solve mode");
        switch (solveModeString) {
            case "gradient":
                this.algorithmSolveMode = SolveMode.GRADIENT;
                break;

            case "complete":
                this.algorithmSolveMode = SolveMode.COMPLETE;
                break;

            default:
                throw new IllegalArgumentException("Unknown solve mode: " + solveModeString);
        }

        this.gradientSpeed = this.options.getDouble("gradient speed");
        this.gradientIterations = this.options.getInteger("gradient iterations");
    }


    protected abstract CostCalculator createCostCalculator();
    protected abstract void initializePlacementIteration();


    @Override
    public void initializeData() {
        // Get number of blocks
        int numBlocksCounter = 0;
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            numBlocksCounter += this.circuit.getBlocks(blockType).size();
        }
        this.numBlocks = numBlocksCounter;


        // Make a list of all block types, with IO blocks first
        BlockType ioBlockType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        List<BlockType> blockTypes = new ArrayList<>();
        blockTypes.add(ioBlockType);
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            if(!blockType.equals(ioBlockType)) {
                blockTypes.add(blockType);
            }
        }



        // Add all blocks
        this.linearX = new double[this.numBlocks];
        this.linearY = new double[this.numBlocks];

        List<Integer> blockTypeIndexStarts = new ArrayList<>();
        int blockIndex = 0;
        blockTypeIndexStarts.add(0);

        for(BlockType blockType : blockTypes) {
            for(AbstractBlock abstractBlock : this.circuit.getBlocks(blockType)) {
                GlobalBlock block = (GlobalBlock) abstractBlock;

                this.linearX[blockIndex] = block.getX();
                this.linearY[blockIndex] = block.getY();

                this.blockIndexes.put(block, blockIndex);
                blockIndex++;
            }

            blockTypeIndexStarts.add(blockIndex);
        }

        this.numIOBlocks = blockTypeIndexStarts.get(1);



        // Add all nets
        // Loop through all net sources
        for(GlobalBlock sourceBlock : this.circuit.getGlobalBlocks()) {
            for(AbstractPin sourcePin : sourceBlock.getOutputPins()) {

                // Build a set of all blocks connected to the net
                Set<GlobalBlock> netBlocks = new HashSet<>();
                netBlocks.add((GlobalBlock) sourcePin.getOwner());

                for(AbstractPin pin : sourcePin.getSinks()) {
                    netBlocks.add((GlobalBlock) pin.getOwner());
                }

                // Make a list of the block indexes
                int numNetBlocks = netBlocks.size();

                // Ignore nets of size 1
                // Due to this, the WLD costcalculator (which uses these nets
                // for speedup) is not entirely accurate, but that doesn't
                // matter, because we use the same (inaccurate) costcalculator
                // to calculate both the linear and legal cost, so the deviation
                // cancels out.
                if(numNetBlocks > 1) {
                    int[] netBlockIndexes = new int[numNetBlocks];
                    int i = 0;
                    for(GlobalBlock netBlock : netBlocks) {
                        netBlockIndexes[i] = this.blockIndexes.get(netBlock);
                        i++;
                    }

                    // Add this "net" to the list of nets
                    this.nets.add(netBlockIndexes);
                }
            }
        }

        this.costCalculator = createCostCalculator();

        try {
            this.legalizer = new NewLegalizer(
                    this.circuit, this.costCalculator,
                    this.blockIndexes,
                    blockTypes, blockTypeIndexStarts,
                    this.linearX, this.linearY);

        } catch(IllegalArgumentException error) {
            this.logger.raise(error);
        }
    }


    @Override
    public void place() {

        int iteration = 0;
        double anchorWeight = this.startingAnchorWeight;
        double linearCost, legalCost;
        boolean firstSolve = true;

        this.logger.println("Iteration    anchor weight    max utilization    linear cost    legal cost    time");
        this.logger.println("---------    -------------    ---------------    -----------    ----------    ----");

        do {
            this.initializePlacementIteration();

            // Solve linear
            double timerBegin = System.nanoTime();
            if(this.algorithmSolveMode == SolveMode.COMPLETE) {
                if(firstSolve) {
                    for(int i = 0; i < 5; i++) {
                        this.solveLinearComplete(true, anchorWeight);
                    }
                } else {
                    this.solveLinearComplete(false, anchorWeight);
                }

            } else {
                int iterations = firstSolve ? 4 * this.gradientIterations : this.gradientIterations;
                for(int i = 0; i < iterations; i++) {
                    this.solveLinearGradient(firstSolve, anchorWeight);
                }
            }

            // Legalize
            int sequenceIndex = Math.min(iteration, this.maxUtilizationSequence.length - 1);
            double maxUtilization = this.maxUtilizationSequence[sequenceIndex];

            if(iteration == 80) {
                int d = 0;
            }

            try {
                this.legalizer.legalize(maxUtilization);
            } catch(PlacementException error) {
                this.logger.raise(error);
            }

            anchorWeight += this.anchorWeightIncrease;
            firstSolve = false;

            double timerEnd = System.nanoTime();
            double time = (timerEnd - timerBegin) * 1e-9;

            // Update the visualizer
            this.visualizer.addPlacement(
                    String.format("iteration %d: linear", iteration),
                    this.blockIndexes, this.linearX, this.linearY);
            this.visualizer.addPlacement(
                    String.format("iteration %d: legal", iteration),
                    this.blockIndexes, this.legalizer.getAnchorsX(), this.legalizer.getAnchorsY());

            // Get the costs and print them
            linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
            legalCost = this.legalizer.getCost();

            this.logger.printf("%-13d%-17f%-19f%-15f%-14f%f\n", iteration, anchorWeight, maxUtilization, linearCost, legalCost, time);

            iteration++;

            // TODO: make the stop criterion placer dependent
            // for the gradient placer, no cost calculation is required
        } while(linearCost / legalCost < this.stopRatioLinearLegal);

        this.logger.println();


        try {
            this.legalizer.updateCircuit();
        } catch(PlacementException error) {
            this.logger.raise(error);
        }
    }


    // These two methods only exist to be able to profile the "complete" part
    // of the execution to the "gradient" part.
    private void solveLinearComplete(boolean firstSolve, double pseudoWeight) {
        double epsilon = 0.0001;
        this.solver = new LinearSolverComplete(this.linearX, this.linearY, this.numIOBlocks, pseudoWeight, epsilon);
        this.solveLinear(firstSolve);
    }
    private void solveLinearGradient(boolean firstSolve, double pseudoWeight) {
        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.numIOBlocks, pseudoWeight, this.gradientSpeed);
        this.solveLinear(firstSolve);
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    private void solveLinear(boolean firstSolve) {

        // Add connections between blocks that are connected by a net
        this.processNets();

        // Add pseudo connections
        if(!firstSolve) {
            this.solver.addPseudoConnections(
                    this.legalizer.getAnchorsX(),
                    this.legalizer.getAnchorsY());
        }


        // Solve and save result
        this.solver.solve();
    }


    protected void processNets() {
        for(int[] net : this.nets) {
            this.solver.processNet(net);
        }
    }


    public static double getWeight(int size) {
        switch (size) {
            case 1:
            case 2:
            case 3:  return 1;
            case 4:  return 1.0828;
            case 5:  return 1.1536;
            case 6:  return 1.2206;
            case 7:  return 1.2823;
            case 8:  return 1.3385;
            case 9:  return 1.3991;
            case 10: return 1.4493;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15: return (size-10) * (1.6899-1.4493) / 5 + 1.4493;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20: return (size-15) * (1.8924-1.6899) / 5 + 1.6899;
            case 21:
            case 22:
            case 23:
            case 24:
            case 25: return (size-20) * (2.0743-1.8924) / 5 + 1.8924;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30: return (size-25) * (2.2334-2.0743) / 5 + 2.0743;
            case 31:
            case 32:
            case 33:
            case 34:
            case 35: return (size-30) * (2.3895-2.2334) / 5 + 2.2334;
            case 36:
            case 37:
            case 38:
            case 39:
            case 40: return (size-35) * (2.5356-2.3895) / 5 + 2.3895;
            case 41:
            case 42:
            case 43:
            case 44:
            case 45: return (size-40) * (2.6625-2.5356) / 5 + 2.5356;
            case 46:
            case 47:
            case 48:
            case 49:
            case 50: return (size-45) * (2.7933-2.6625) / 5 + 2.6625;
            default: return (size-50) * 0.02616 + 2.7933;
        }
    }
}