package place.placers.analytical;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class GradientPlacer extends AnalyticalAndGradientPlacer {

    private static final String
        O_ANCHOR_WEIGHT_EXPONENT = "anchor weight exponent",
        O_ANCHOR_WEIGHT_STOP = "anchor weight stop",

        O_LEARNING_RATE_START = "learning rate start",
        O_LEARNING_RATE_STOP = "learning rate stop",
        
        O_STEP_SIZE_START = "step size start",
        O_STEP_SIZE_STOP = "step size stop",

        O_MAX_CONN_LENGTH_RATIO_SPARSE = "max conn length ratio sparse",
        O_MAX_CONN_LENGTH_DENSE = "max conn length dense",

        O_BETA1 = "beta1",
        O_BETA2 = "beta2",
        O_EPS = "eps",

        O_OUTER_EFFORT_LEVEL = "outer effort level",
        
        O_INNER_EFFORT_LEVEL_START = "inner effort level start",
        O_INNER_EFFORT_LEVEL_STOP = "inner effort level stop",
    
    	O_LEAF_NODE_WEIGHT = "leaf node weight";

    public static void initOptions(Options options) {
        AnalyticalAndGradientPlacer.initOptions(options);

        options.add(
                O_ANCHOR_WEIGHT_EXPONENT,
                "anchor weight exponent",
                new Double(2));

        options.add(
                O_ANCHOR_WEIGHT_STOP,
                "anchor weight at which the placement is finished (max: 1)",
                new Double(0.8));

        options.add(
                O_LEARNING_RATE_START,
                "ratio of distance to optimal position that is moved",
                new Double(1));

        options.add(
                O_LEARNING_RATE_STOP,
                "ratio of distance to optimal position that is moved",
                new Double(0.2));
        
        options.add(
                O_STEP_SIZE_START,
                "initial step size in gradient cluster legalizer",
                new Double(0.4));
        
        options.add(
                O_STEP_SIZE_STOP,
                "final step size in gradient cluster legalizer",
                new Double(0.075));
        
        options.add(
                O_MAX_CONN_LENGTH_RATIO_SPARSE,
                "maximum connection length in sparse placement is ratio of circuit width",
                new Double(0.25));

        options.add(
                O_MAX_CONN_LENGTH_DENSE,
                "maximum connection length in dense placement",
                new Integer(30));

        options.add(
                O_BETA1,
                "adam gradient descent beta1 parameter",
                new Double(0.9));

        options.add(
                O_BETA2,
                "adam gradient descent beta2 parameter",
                new Double(0.999));

        options.add(
                O_EPS,
                "adam gradient descent eps parameter",
                new Double(10e-10));

        options.add(
                O_OUTER_EFFORT_LEVEL,
                "number of solve-legalize iterations",
                new Integer(20));

        options.add(
                O_INNER_EFFORT_LEVEL_START,
                "number of gradient steps to take in each outer iteration in the beginning",
                new Integer(200));
        options.add(
                O_INNER_EFFORT_LEVEL_STOP,
                "number of gradient steps to take in each outer iteration at the end",
                new Integer(50));
        
        options.add(
                O_LEAF_NODE_WEIGHT,
                "multiplier to reduce weigth between connections in a leaf node (packing cluster)",
                new Double(0.7));
    }

    protected double anchorWeight;
    protected final double anchorWeightStop, anchorWeightExponent;

    private final double stepSizeStart, stepSizeStop;
    
    private final double maxConnectionLength;
    protected double learningRate, learningRateMultiplier;
    private final double beta1, beta2, eps;

    protected final int numIterations;
    protected int effortLevel;
    protected final int effortLevelStart, effortLevelStop;

    // Only used by GradientPlacerTD
    protected double tradeOff; 
    protected List<CritConn> criticalConnections;

    private CostCalculator costCalculator;

    protected Legalizer legalizer;
    protected LinearSolverGradient solver;

    private Map<BlockType, boolean[]> netMap;
    private boolean[] allTrue;
    private int[] netStarts;
    private int[] netEnds;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;
    
    private double leafNodeWeight;

    protected boolean[] fixed;
    private double[] coordinatesX;
    private double[] coordinatesY;
    
    private int[] blockTypeNumbers;

    public GradientPlacer(
            Circuit circuit,
            Options options,
            Random random,
            Logger logger,
            PlacementVisualizer visualizer){

        super(circuit, options, random, logger, visualizer);

        this.anchorWeight = 0.0;
        this.anchorWeightExponent = this.options.getDouble(O_ANCHOR_WEIGHT_EXPONENT);
        this.anchorWeightStop = this.options.getDouble(O_ANCHOR_WEIGHT_STOP);

    	this.effortLevelStart = this.options.getInteger(O_INNER_EFFORT_LEVEL_START);
    	this.effortLevelStop = this.options.getInteger(O_INNER_EFFORT_LEVEL_STOP);
    	this.effortLevel = this.effortLevelStart;
    	
    	this.stepSizeStart =  this.options.getDouble(O_STEP_SIZE_START);
    	this.stepSizeStop =  this.options.getDouble(O_STEP_SIZE_STOP);
    	
    	this.numIterations = this.options.getInteger(O_OUTER_EFFORT_LEVEL) + 1;

        this.learningRate = this.options.getDouble(O_LEARNING_RATE_START);
        this.learningRateMultiplier = Math.pow(this.options.getDouble(O_LEARNING_RATE_STOP) / this.options.getDouble(O_LEARNING_RATE_START), 1.0 / (this.numIterations - 1.0));

        this.beta1 = this.options.getDouble(O_BETA1);
        this.beta2 = this.options.getDouble(O_BETA2);
        this.eps = this.options.getDouble(O_EPS);
        
        this.leafNodeWeight = this.options.getDouble(O_LEAF_NODE_WEIGHT);

        if(this.circuit.dense()) {
        	this.maxConnectionLength = this.options.getInteger(O_MAX_CONN_LENGTH_DENSE);
        } else {
        	this.maxConnectionLength = this.circuit.getWidth() * this.options.getDouble(O_MAX_CONN_LENGTH_RATIO_SPARSE);
        }

        this.criticalConnections = new ArrayList<>();
    }

    protected abstract void initializeIteration(int iteration);
    protected abstract void calculateTimingCost();

    @Override
    public void initializeData() {
        super.initializeData();

        this.blockTypeNumbers = new int[this.legalX.length];
        for(NetBlock block:this.netBlocks.values()){
        	this.blockTypeNumbers[block.blockIndex] = block.getBlockTypeHash();
        }
        
        this.startTimer(T_INITIALIZE_DATA);
        
        if(this.circuit.ratioUsedCLB() > 0.8){
        	this.legalizer = new HeapLegalizer(
                    this.circuit,
                    this.blockTypes,
                    this.blockTypeIndexStarts,
                    this.numIterations,
                    this.linearX,
                    this.linearY,
                    this.legalX,
                    this.legalY,
                    this.heights,
                    this.leafNode,
                    this.visualizer,
                    this.nets,
                    this.netBlocks,
                    this.logger);
            this.legalizer.addSetting("anneal_quality", 0.1,  0.001);
        }else{
            double widthFactor = Math.pow((1.0 * this.circuit.getWidth()) / 100.0, 1.2);
            this.logger.println("------------------");
            this.logger.println("Circuit width: " + this.circuit.getWidth());
            this.logger.println("Width factor: " + String.format("%.2f", widthFactor));
            this.logger.println("------------------\n");
            
            this.legalizer = new GradientLegalizer(
                    this.circuit,
                    this.blockTypes,
                    this.blockTypeIndexStarts,
                    this.numIterations,
                    this.linearX,
                    this.linearY,
                    this.legalX,
                    this.legalY,
                    this.heights,
                    this.leafNode,
                    this.visualizer,
                    this.nets,
                    this.netBlocks,
                    this.logger);
            this.legalizer.addSetting("anneal_quality", 0.1,  0.001);
            this.legalizer.addSetting("step_size", widthFactor * this.stepSizeStart, widthFactor * this.stepSizeStop);
            this.legalizer.addSetting("speed_averaging", 0.2, 0.2);
        }

        // Juggling with objects is too slow (I profiled this,
        // the speedup is around 40%)
        // Build some arrays of primitive types
        int netBlockSize = 0;
        for(int i = 0; i < this.numRealNets; i++) {
            netBlockSize += this.nets.get(i).blocks.length;
        }

        this.allTrue = new boolean[this.numRealNets];
        Arrays.fill(this.allTrue, true);
        this.netMap = new HashMap<>();
        for(BlockType blockType:this.blockTypes){
        	this.netMap.put(blockType, new boolean[this.numRealNets]);
        	Arrays.fill(this.netMap.get(blockType), false);
        }

        this.netStarts = new int[this.numRealNets];
        this.netEnds = new int[this.numRealNets];
        this.netBlockIndexes = new int[netBlockSize];
        this.netBlockOffsets = new float[netBlockSize];

        int netBlockCounter = 0;
        for(int netCounter = 0; netCounter < this.numRealNets; netCounter++) {
        	this.netStarts[netCounter] = netBlockCounter;

        	Net net = this.nets.get(netCounter);

            for(NetBlock block : net.blocks) {
                this.netBlockIndexes[netBlockCounter] = block.blockIndex;
                this.netBlockOffsets[netBlockCounter] = block.offset;

                netBlockCounter++;

                this.netMap.get(block.blockType)[netCounter] = true;
            }

            this.netEnds[netCounter] = netBlockCounter;
        }

        this.fixed = new boolean[this.linearX.length];

    	this.coordinatesX = new double[this.linearX.length];
    	this.coordinatesY = new double[this.linearY.length];

        this.solver = new LinearSolverGradient(
                this.coordinatesX,
                this.coordinatesY,
                this.leafNode,
                this.leafNodeWeight,
                this.blockTypeNumbers,
                this.netBlockIndexes,
                this.netBlockOffsets,
                this.maxConnectionLength,
                this.fixed,
                this.beta1, 
                this.beta2, 
                this.eps);

        this.costCalculator = new CostCalculatorWLD(this.nets);

        this.stopTimer(T_INITIALIZE_DATA);
    }

    @Override
    protected void solveLinear(int iteration) {
    	Arrays.fill(this.fixed, false);
		for(BlockType blockType : BlockType.getBlockTypes(BlockCategory.IO)){
			this.fixBlockType(blockType);
		}

		this.doSolveLinear(this.allTrue);
    }

    @Override
    protected void solveLinear(BlockType solveType, int iteration) {
    	Arrays.fill(this.fixed, false);
		for(BlockType blockType : BlockType.getBlockTypes(BlockCategory.IO)){
			this.fixBlockType(blockType);
		}

    	for(BlockType blockType : BlockType.getBlockTypes(BlockCategory.CLB)){
    		if(!blockType.equals(solveType)){
    			this.fixBlockType(blockType);
    		}
    	}
    	for(BlockType blockType : BlockType.getBlockTypes(BlockCategory.HARDBLOCK)){
    		if(!blockType.equals(solveType)){
    			this.fixBlockType(blockType);
    		}
    	}

		this.doSolveLinear(this.netMap.get(solveType));
    }

    private void fixBlockType(BlockType fixBlockType){
    	for(GlobalBlock block:this.netBlocks.keySet()){
    		if(block.getType().equals(fixBlockType)){
    			int blockIndex = this.netBlocks.get(block).getBlockIndex();
    			this.fixed[blockIndex] = true;
    		}
    	}
    }
    private void doSolveLinear(boolean[] processNets){
		for(int i = 0; i < this.linearX.length; i++){
			if(this.fixed[i]){
				this.coordinatesX[i] = this.legalX[i];
				this.coordinatesY[i] = this.legalY[i];
			}else{
				this.coordinatesX[i] = this.linearX[i];
				this.coordinatesY[i] = this.linearY[i];
			}
		}
        
        for(int i = 0; i < this.effortLevel; i++) {
            this.solveLinearIteration(processNets);

            //this.visualizer.addPlacement(String.format("gradient descent step %d", i), this.netBlocks, this.solver.getCoordinatesX(), this.solver.getCoordinatesY(), -1);
        }
        
		for(int i = 0; i < this.linearX.length; i++){
			if(!this.fixed[i]){
				this.linearX[i] = this.coordinatesX[i];
				this.linearY[i] = this.coordinatesY[i];
			}
		}
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(boolean[] processNets) {
        this.startTimer(T_BUILD_LINEAR);

        // Set value of alpha and reset the solver
        this.solver.initializeIteration(this.anchorWeight, this.learningRate);

        // Process nets
        this.processNets(processNets);

        // Add pseudo connections
        if(this.anchorWeight != 0.0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            this.solver.addPseudoConnections(this.legalX, this.legalY);
        }

        this.stopTimer(T_BUILD_LINEAR);

        // Solve and save result
        this.startTimer(T_SOLVE_LINEAR);
        this.solver.solve();
        this.stopTimer(T_SOLVE_LINEAR);
    }

    protected void processNets(boolean[] processNets) {
    	int numNets = this.netEnds.length;
    	for(int netIndex = 0; netIndex < numNets; netIndex++) {
    		if(processNets[netIndex]){
    			this.solver.processNet(this.netStarts[netIndex], this.netEnds[netIndex]);
    		}
    	}
    }

    @Override
    protected void solveLegal(boolean isLastIteration) {
        this.startTimer(T_LEGALIZE);
        for(BlockType legalizeType:BlockType.getBlockTypes(BlockCategory.CLB)){
        	this.legalizer.legalize(legalizeType, isLastIteration);
        }
        for(BlockType legalizeType:BlockType.getBlockTypes(BlockCategory.HARDBLOCK)){
        	this.legalizer.legalize(legalizeType, isLastIteration);
        }
        for(BlockType legalizeType:BlockType.getBlockTypes(BlockCategory.IO)){
        	this.legalizer.legalize(legalizeType, isLastIteration);
        }
        this.stopTimer(T_LEGALIZE);
    }

    @Override
    protected void solveLegal(BlockType legalizeType, boolean lastIteration) {
        this.startTimer(T_LEGALIZE);
        this.legalizer.legalize(legalizeType, lastIteration);
        this.stopTimer(T_LEGALIZE);
    }

    @Override
    protected void calculateCost(){
    	this.startTimer(T_UPDATE_CIRCUIT);

    	this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
    	this.legalCost = this.costCalculator.calculate(this.legalX, this.legalY);
    	
    	this.currentCost = this.legalCost;

    	if(this.isTimingDriven()){
    		this.calculateTimingCost();
    		this.currentCost *= this.timingCost;
    	}
    	
    	if(this.currentCost < this.bestCost){
    		this.bestCost = this.currentCost;
    		for(int i = 0; i < this.linearX.length; i++){
    			this.bestLinearX[i] = this.linearX[i];
    			this.bestLinearY[i] = this.linearY[i];
    			this.bestLegalX[i] = this.legalX[i];
    			this.bestLegalY[i] = this.legalY[i];
    		}
    	}
    	this.stopTimer(T_UPDATE_CIRCUIT);
    }

    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("it");
        titles.add("effort level");
        titles.add("stepsize");
        titles.add("anchor");
        titles.add("max conn length");

        //Wirelength cost
        titles.add("BB linear");
        titles.add("BB legal");

        //Timing cost
        if(this.isTimingDriven()){
        	titles.add("max delay");
        }
        
        titles.add("best");

        titles.add("time (ms)");
        titles.add("crit conn");
        
        for(String setting:this.legalizer.getLegalizerSetting()){
        	titles.add(setting);
        }
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        List<String> stats = new ArrayList<>();

        stats.add(Integer.toString(iteration));
        stats.add(Integer.toString(this.effortLevel));
        stats.add(String.format("%.3f", this.learningRate));
        stats.add(String.format("%.3f", this.anchorWeight));
        stats.add(String.format("%.1f", this.maxConnectionLength));

        //Wirelength cost
        stats.add(String.format("%.0f", this.linearCost));
        stats.add(String.format("%.0f", this.legalCost));

        //Timing cost
        if(this.isTimingDriven()){
        	stats.add(String.format("%.4g", this.timingCost));
        }
        
        stats.add(this.currentCost == this.bestCost ? "yes" : "");

        stats.add(String.format("%.0f", time*Math.pow(10, 3)));
        stats.add(String.format("%d", this.criticalConnections.size()));
        
        for(String setting:this.legalizer.getLegalizerSetting()){
        	stats.add(String.format("%.3f", this.legalizer.getSettingValue(setting)));
        }

        this.printStats(stats.toArray(new String[0]));
    }

    @Override
    protected int numIterations() {
    	return this.numIterations;
    }

    @Override
    protected boolean stopCondition(int iteration) {
    	return iteration + 1 >= this.numIterations;
    }
    
    @Override
    public void printLegalizationRuntime(){
    	this.legalizer.printLegalizationRuntime();
    }
}
