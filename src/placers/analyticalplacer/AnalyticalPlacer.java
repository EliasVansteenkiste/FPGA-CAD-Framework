package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import placers.Placer;
import placers.analyticalplacer.linear_solver.LinearSolver;
import placers.analyticalplacer.linear_solver.LinearSolverComplete;
import placers.analyticalplacer.linear_solver.LinearSolverGradient;
import util.Logger;
import architecture.circuit.Circuit;
import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.BlockType;
import architecture.circuit.block.GlobalBlock;
import architecture.circuit.block.BlockType.BlockCategory;
import architecture.circuit.pin.AbstractPin;


public abstract class AnalyticalPlacer extends Placer {

    protected static enum SolveMode {GRADIENT, COMPLETE};

    private final SolveMode algorithmSolveMode;

    private final double[] maxUtilizationSequence;
    private final double startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal;
    private final double gradientMultiplier, finalGradientSpeed;
    private final int gradientIterations;
    private double gradientSpeed;

    protected final Map<GlobalBlock, Integer> blockIndexes = new HashMap<>();
    protected final List<int[]> nets = new ArrayList<>();
    private int numBlocks, numIOBlocks;

    private double[] linearX, linearY;
    private LinearSolver solver;

    private CostCalculator costCalculator;
    protected Legalizer legalizer;


    static {
        // Initialize maxUtilizationSequence used by the legalizer
        defaultOptions.put("max_utilization_sequence", "1");

        // The first anchorWeight factor that will be used in the main solve loop
        defaultOptions.put("starting_anchor_weight", "1");

        // The amount with which the anchorWeight factor will be multiplied each iteration
        defaultOptions.put("anchor_weight_increase", "1.1");

        // The ratio of linear solutions cost to legal solution cost at which we stop the algorithm
        defaultOptions.put("stop_ratio_linear_legal", "0.9");

        // The speed at which the gradient solver moves to the optimal position
        defaultOptions.put("solve_mode", "complete");
        defaultOptions.put("initial_gradient_speed", "0.1");
        defaultOptions.put("gradient_multiplier", "0.95");
        defaultOptions.put("final_gradient_speed", "0.1");
        defaultOptions.put("gradient_iterations", "30");
    }

    public AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
        super(circuit, options);

        // Parse options

        // Max utilization sequence
        String maxUtilizationSequenceString = this.options.get("max_utilization_sequence");
        String[] maxUtilizationSequenceStrings = maxUtilizationSequenceString.split(",");
        this.maxUtilizationSequence = new double[maxUtilizationSequenceStrings.length];
        for(int i = 0; i < maxUtilizationSequenceStrings.length; i++) {
            this.maxUtilizationSequence[i] = Double.parseDouble(maxUtilizationSequenceStrings[i]);
        }

        // Anchor weights and stop ratio
        this.startingAnchorWeight = this.parseDoubleOption("starting_anchor_weight");
        this.anchorWeightIncrease = this.parseDoubleOption("anchor_weight_increase");
        this.stopRatioLinearLegal = this.parseDoubleOption("stop_ratio_linear_legal");

        // Gradient solver
        String solveModeString = this.parseStringOption("solve_mode");
        switch (solveModeString) {
            case "gradient":
                this.algorithmSolveMode = SolveMode.GRADIENT;
                break;

            case "complete":
                this.algorithmSolveMode = SolveMode.COMPLETE;
                break;

            default:
                Logger.raise("Unknown solve mode: " + solveModeString);
                this.algorithmSolveMode = null;
        }

        this.gradientSpeed = this.parseDoubleOption("initial_gradient_speed");
        this.gradientMultiplier = this.parseDoubleOption("gradient_multiplier");
        this.finalGradientSpeed = this.parseDoubleOption("final_gradient_speed");
        this.gradientIterations = this.parseIntegerOption("gradient_iterations");
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

        this.legalizer = new Legalizer(
                this.circuit, this.costCalculator,
                this.blockIndexes,
                blockTypes, blockTypeIndexStarts,
                this.linearX, this.linearY);
    }


    @Override
    public void place() {

        int iteration = 0;
        double pseudoWeightFactor = this.startingAnchorWeight;
        double linearCost, legalCost;
        boolean firstSolve = true;
        //boolean legalizeIOBlocks = (this.algorithmSolveMode == SolveMode.GRADIENT);
        boolean legalizeIOBlocks = false;

        do {
            System.out.format("Iteration %d: pseudoWeightFactor = %f, gradientSpeed = %f",
                    iteration, pseudoWeightFactor, this.gradientSpeed);

            this.initializePlacementIteration();

            // Solve linear
            double timerBegin = System.nanoTime();
            if(this.algorithmSolveMode == SolveMode.COMPLETE) {
                if(firstSolve) {
                    for(int i = 0; i < 5; i++) {
                        this.solveLinearComplete(true, pseudoWeightFactor);
                    }
                } else {
                    this.solveLinearComplete(false, pseudoWeightFactor);
                }

            } else {
                int iterations = firstSolve ? 4 * this.gradientIterations : this.gradientIterations;
                for(int i = 0; i < iterations; i++) {
                    this.solveLinearGradient(firstSolve, pseudoWeightFactor);
                    //System.out.println(this.costCalculator.calculate(this.linearX, this.linearY));
                }
            }

            pseudoWeightFactor *= this.anchorWeightIncrease;
            //this.gradientSpeed *= this.gradientMultiplier;
            this.gradientSpeed = 0.95 * this.gradientSpeed + 0.05 * this.finalGradientSpeed;
            firstSolve = false;

            // Legalize
            int sequenceIndex = Math.min(iteration, this.maxUtilizationSequence.length - 1);
            double maxUtilizationLegalizer = this.maxUtilizationSequence[sequenceIndex];

            this.legalizer.legalize(maxUtilizationLegalizer, legalizeIOBlocks);
            double timerEnd = System.nanoTime();
            double time = (timerEnd - timerBegin) * 1e-9;

            // Get the costs and print them
            linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
            legalCost = this.legalizer.getBestCost();
            System.out.format(", linear cost = %f, legal cost = %f, time = %f\n", linearCost, legalCost, time);

            iteration++;

        } while(linearCost / legalCost < this.stopRatioLinearLegal);


        this.legalizer.updateCircuit();
    }


    // These two methods only exist to be able to profile the "complete" part
    // of the execution to the "gradient" part.
    private void solveLinearComplete(boolean firstSolve, double pseudoweightFactor) {
        double epsilon = 0.0001;
        this.solver = new LinearSolverComplete(this.linearX, this.linearY, this.numIOBlocks, epsilon);
        this.solveLinear(firstSolve, pseudoweightFactor);
    }
    private void solveLinearGradient(boolean firstSolve, double pseudoweightFactor) {
        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.numIOBlocks, this.gradientSpeed);
        this.solveLinear(firstSolve, pseudoweightFactor);
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    private void solveLinear(boolean firstSolve, double pseudoWeightFactor) {

        // Add connections between blocks that are connected by a net
        this.processNets();

        // Add pseudo connections
        if(!firstSolve) {
            this.addPseudoConnections(pseudoWeightFactor);
        }


        // Solve and save result
        this.solver.solve();
    }

    private void addPseudoConnections(double pseudoWeightFactor) {
        int[] legalX = this.legalizer.getAnchorsX();
        int[] legalY = this.legalizer.getAnchorsY();

        int blockIndexStart = this.solver.getPseudoBlockIndexStart();
        for(int blockIndex = blockIndexStart; blockIndex < this.numBlocks; blockIndex++) {
            this.solver.addPseudoConnection(blockIndex, legalX[blockIndex], legalY[blockIndex], pseudoWeightFactor);
        }
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