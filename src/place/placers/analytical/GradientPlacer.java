package place.placers.analytical;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.interfaces.Options.Required;
import place.visual.PlacementVisualizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class GradientPlacer extends AnalyticalAndGradientPlacer {

    private static final String
        O_ANCHOR_WEIGHT_START = "anchor weight start",
        O_ANCHOR_WEIGHT_STEP = "anchor weight step",
        O_ANCHOR_WEIGHT_STOP = "anchor weight stop",
        O_STEP_SIZE = "step size",
        O_MAX_CONNECTION_LENGTH = "max connection length",
        O_SPEED_AVERAGING = "speed averaging",
        O_EFFORT_LEVEL = "effort level",
        O_FIRST_EFFORT = "first effort",
        O_LAST_EFFORT = "last effort",
        O_PRINT_OUTER_COST = "print outer cost",
        O_PRINT_INNER_COST = "print inner cost";

    public static void initOptions(Options options) {
        AnalyticalAndGradientPlacer.initOptions(options);

        options.add(
                O_ANCHOR_WEIGHT_START,
                "starting anchor weight",
                new Double(0.0));

        options.add(
                O_ANCHOR_WEIGHT_STEP,
                "value that is added to the anchor weight in each iteration (default: 1/effort level)",
                Double.class,
                Required.FALSE);

        options.add(
                O_ANCHOR_WEIGHT_STOP,
                "anchor weight at which the placement is finished (max: 1)",
                new Double(0.85));


        options.add(
                O_STEP_SIZE,
                "ratio of distance to optimal position that is moved",
                new Double(0.4));

        options.add(
                O_MAX_CONNECTION_LENGTH,
                "length to which connection lengths are platformed",
                new Double(30));

        options.add(
                O_SPEED_AVERAGING,
                "averaging factor for block speeds",
                new Double(0.2));


        options.add(
                O_EFFORT_LEVEL,
                "number of gradient steps to take in each outer iteration",
                new Integer(15));

        options.add(
                O_FIRST_EFFORT,
                "multiplier for the effort level in the first outer iteration",
                new Double(1));

        options.add(
                O_LAST_EFFORT,
                "multiplier for the effort level in the last outer iteration",
                new Double(0.07));

        options.add(
                O_PRINT_OUTER_COST,
                "print the WLD cost after each outer iteration",
                new Boolean(false));

        options.add(
                O_PRINT_INNER_COST,
                "print the WLD cost after each inner iteration",
                new Boolean(false));
    }


    protected double anchorWeight;
    protected double anchorWeightStart, anchorWeightStop, anchorWeightStep;

    private double stepSize, maxConnectionLength, speedAveraging;

    private int effortLevel;
    private double firstEffortMultiplier, lastEffortMultiplier;
    protected double tradeOff; // Only used by GradientPlacerTD

    private boolean printInnerCost, printOuterCost;
    private CostCalculator costCalculator; // Only used if printOuterCost or printInnerCost is true

    protected double utilization;

    protected int numIterations;
    private int iterationEffortLevel;

    protected HeapLegalizer legalizer;
    protected LinearSolverGradient solver;


    private int[] netEnds;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;


    protected abstract void addStatTitlesGP(List<String> titles);
    protected abstract void addStats(List<String> stats);

    private boolean[] fixed;
    private ArrayList<BlockType> fixTypes;
    private int fixTypePointer;
    
    private double[] coordinatesX;
    private double[] coordinatesY;

    public GradientPlacer(
            Circuit circuit,
            Options options,
            Random random,
            Logger logger,
            PlacementVisualizer visualizer) {

        super(circuit, options, random, logger, visualizer);

        this.anchorWeightStart = this.options.getDouble(O_ANCHOR_WEIGHT_START);
        this.anchorWeightStop = this.options.getDouble(O_ANCHOR_WEIGHT_STOP);
        this.anchorWeight = this.anchorWeightStart;

        this.stepSize = this.options.getDouble(O_STEP_SIZE);
        this.maxConnectionLength = this.options.getDouble(O_MAX_CONNECTION_LENGTH);
        this.speedAveraging = this.options.getDouble(O_SPEED_AVERAGING);

        this.effortLevel = this.options.getInteger(O_EFFORT_LEVEL);
        this.firstEffortMultiplier = this.options.getDouble(O_FIRST_EFFORT);
        this.lastEffortMultiplier = this.options.getDouble(O_LAST_EFFORT);

        if(!this.options.isSet(O_ANCHOR_WEIGHT_STEP)) {
            this.options.set(O_ANCHOR_WEIGHT_STEP, new Double(0.7 / this.effortLevel));
        }
        this.anchorWeightStep = this.options.getDouble(O_ANCHOR_WEIGHT_STEP);

        this.numIterations = (int) Math.ceil((this.anchorWeightStop - this.anchorWeightStart) / this.anchorWeightStep + 1);

        this.printInnerCost = this.options.getBoolean(O_PRINT_INNER_COST);
        this.printOuterCost = this.options.getBoolean(O_PRINT_OUTER_COST);
    }

    protected abstract void initializeIteration(int iteration);
    protected abstract void updateLegalIfNeeded(int iteration);


    @Override
    public void initializeData() {
        super.initializeData();

        this.startTimer(T_INITIALIZE_DATA);

        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes,
                this.blockTypeIndexStarts,
                this.linearX,
                this.linearY,
                this.legalX,
                this.legalY,
                this.heights);

        // Juggling with objects is too slow (I profiled this,
        // the speedup is around 40%)
        // Build some arrays of primitive types
        int netBlockSize = 0;
        for(int i = 0; i < this.numRealNets; i++) {
            netBlockSize += this.nets.get(i).blocks.length;
        }

        this.netEnds = new int[this.numRealNets];
        this.netBlockIndexes = new int[netBlockSize];
        this.netBlockOffsets = new float[netBlockSize];

        int netBlockCounter = 0;
        for(int netCounter = 0; netCounter < this.numRealNets; netCounter++) {
            Net net = this.nets.get(netCounter);

            for(NetBlock block : net.blocks) {
                this.netBlockIndexes[netBlockCounter] = block.blockIndex;
                this.netBlockOffsets[netBlockCounter] = block.offset;

                netBlockCounter++;
            }

            this.netEnds[netCounter] = netBlockCounter;
        }

        this.fixed = new boolean[this.legalX.length];
        this.coordinatesX = new double[this.legalX.length];
        this.coordinatesY = new double[this.legalX.length];
        this.fixTypes = new ArrayList<BlockType>();
        this.fixTypes.add(null);
        this.fixTypePointer = -1;
        BlockType dspType = BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(1);
        BlockType m9kType = BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(2);
        BlockType m144kType = BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(3);
        if(this.circuit.getBlocks(dspType).size() > 0) this.fixTypes.add(dspType);
        if(this.circuit.getBlocks(m9kType).size() > 0) this.fixTypes.add(m9kType);
        if(this.circuit.getBlocks(m144kType).size() > 0) this.fixTypes.add(m144kType);

        this.solver = new LinearSolverGradient(
                this.coordinatesX,
                this.coordinatesY,
                this.netBlockIndexes,
                this.netBlockOffsets,
                this.stepSize,
                this.maxConnectionLength,
                this.speedAveraging,
                this.fixed);

        if(this.printInnerCost || this.printOuterCost) {
            this.costCalculator = new CostCalculatorWLD(this.nets);
        }

        this.stopTimer(T_INITIALIZE_DATA);
    }

    @Override
    protected void solveLinear(int iteration) {

        if(iteration > 0) {
            this.anchorWeight += this.anchorWeightStep;
        }

        this.initializeIteration(iteration);
        this.setFixedBlocks();

        this.iterationEffortLevel = this.getIterationEffortLevel(iteration);
        for(int i = 0; i < this.iterationEffortLevel; i++) {
            this.solveLinearIteration();

            if(this.printInnerCost) {
                double cost = this.costCalculator.calculate(this.linearX, this.linearY);
                System.out.printf("Cost inner iteration %3d: %.4g\n", i, cost);
            }
        }
        this.freeFixedBlocks();
    }
    private void setFixedBlocks(){
        this.fixTypePointer += 1;
        this.fixTypePointer = this.fixTypePointer % this.fixTypes.size();
        
    	Arrays.fill(this.fixed, false);

        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        this.fixBlockType(ioType);
        
        BlockType fixType = this.fixTypes.get(this.fixTypePointer);
        if(fixType != null){
        	this.fixBlockType(fixType);
        }
        
        for(int i=0; i<this.fixed.length; i++){
        	if(this.fixed[i]){
        		this.coordinatesX[i] = this.legalX[i];
        		this.coordinatesY[i] = this.legalY[i];
        	}else{
        		this.coordinatesX[i] = this.linearX[i];
        		this.coordinatesY[i] = this.linearY[i];
        	}
        }
    }
    private void fixBlockType(BlockType fixType){
    	for(GlobalBlock block:this.netBlocks.keySet()){
    		if(block.getType().equals(fixType)){
    			int blockIndex = this.netBlocks.get(block).getBlockIndex();
    			this.fixed[blockIndex] = true;
    		}
    	}
    }
    public void freeFixedBlocks(){
        for(int i=0; i<this.fixed.length; i++){
        	if(!this.fixed[i]){
        		this.linearX[i] = this.coordinatesX[i];
        		this.linearY[i] = this.coordinatesY[i];
        	}
        }
    }
    private int getIterationEffortLevel(int iteration) {
        int iterationEffortLevel = (int) Math.round(this.effortLevel * (1 + (this.lastEffortMultiplier - 1) * iteration / (this.numIterations - 1)));
        if(iteration == 0) {
            iterationEffortLevel *= this.firstEffortMultiplier;
        }

        return iterationEffortLevel;
    }
    
    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration() {
        this.startTimer(T_BUILD_LINEAR);

        // Reset the solver
        this.solver.initializeIteration(this.anchorWeight);

        // Process nets
        this.processNets();

        // Add pseudo connections
        if(this.anchorWeight != 0.0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            this.solver.addPseudoConnections(this.legalizer.getLegalX(), this.legalizer.getLegalY());
        }

        this.stopTimer(T_BUILD_LINEAR);

        // Solve and save result
        this.startTimer(T_SOLVE_LINEAR);
        this.solver.solve();
        this.stopTimer(T_SOLVE_LINEAR);
    }

    protected void processNets() {
        int numNets = this.netEnds.length;

        int netStart, netEnd = 0;
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            netStart = netEnd;
            netEnd = this.netEnds[netIndex];

            this.solver.processNet(netStart, netEnd);
        }
    }


    @Override
    protected void solveLegal(int iteration) {
        double slope = (this.startUtilization - 1) / (this.anchorWeightStart - this.anchorWeightStop);
        this.utilization = Math.max(1, 1 + slope * (this.anchorWeight - this.anchorWeightStop));

        this.startTimer(T_LEGALIZE);
        this.legalizer.legalize(this.utilization);
        this.stopTimer(T_LEGALIZE);

        this.startTimer(T_UPDATE_CIRCUIT);
        this.updateLegalIfNeeded(iteration);
        this.stopTimer(T_UPDATE_CIRCUIT);
    }
    
    @Override
    protected void initializeLegalizationAreas(){
    	this.legalizer.initializeLegalizationAreas();
    }
    
    @Override
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return this.legalizer.getLegalizationAreas();
    }


    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("iteration");
        titles.add("anchor weight");
        titles.add("utilization");
        titles.add("effort level");

        if(this.printOuterCost) {
            titles.add("BB linear cost");
            titles.add("BB legal cost");
        }

        this.addStatTitlesGP(titles);

        titles.add("time");
        titles.add("fix type");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        List<String> stats = new ArrayList<>();

        stats.add(Integer.toString(iteration));
        stats.add(String.format("%.3f", this.anchorWeight));
        stats.add(String.format("%.3g", this.utilization));
        stats.add(Integer.toString(this.iterationEffortLevel));

        if(this.printOuterCost) {
            double linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
            double legalCost = this.costCalculator.calculate(this.legalX, this.legalY);

            stats.add(String.format("%.4g", linearCost));
            stats.add(String.format("%.4g", legalCost));
        }

        this.addStats(stats);

        stats.add(String.format("%.3g", time));
        
        stats.add("" + this.fixTypes.get(this.fixTypePointer));

        this.printStats(stats.toArray(new String[0]));
    }


    @Override
    protected boolean stopCondition(int iteration) {
        return iteration + 1 >= this.numIterations || this.getIterationEffortLevel(iteration + 1) == 0;
    }
}
