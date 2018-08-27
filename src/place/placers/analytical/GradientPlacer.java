package place.placers.analytical;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
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

        O_MAX_CONN_LENGTH_RATIO_SPARSE = "max conn length ratio sparse",
        O_MAX_CONN_LENGTH_DENSE = "max conn length dense",

        O_BETA1 = "beta1",
        O_BETA2 = "beta2",
        O_EPS = "eps",
        
        O_USE_PSO = "usePSO",
    	O_PSO_COGNITIVE_LEARNING_RATE = "c1",
    	O_PSO_SOCIAL_LEARNING_RATE = "c2",
    	O_PSO_PROBILITY_INTERVAL_PBEST = "forPbest",
    	O_PSO_PROBILITY_INTERVAL_GBEST = "forGbest",
    	O_PSO_PROBILITY_INTERVAL_WPBGB = "forall",
    	O_PSO_MINIMUM_ITERATION = "minimumIter",
    	O_PSO_INTERVAL = "interval",
    	O_PSO_QUALITY = "psoQuality",

        O_OUTER_EFFORT_LEVEL = "outer effort level",
        O_INNER_EFFORT_LEVEL = "inner effort level";

    public static void initOptions(Options options) {
        AnalyticalAndGradientPlacer.initOptions(options);

        options.add(
                O_ANCHOR_WEIGHT_EXPONENT,
                "anchor weight exponent",
                new Double(2));

        options.add(
                O_ANCHOR_WEIGHT_STOP,
                "anchor weight at which the placement is finished (max: 1)",
                new Double(0.85));

        options.add(
                O_LEARNING_RATE_START,
                "ratio of distance to optimal position that is moved",
                new Double(1));

        options.add(
                O_LEARNING_RATE_STOP,
                "ratio of distance to optimal position that is moved",
                new Double(0.2));

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
        		O_USE_PSO,
                "switch between SA and PSO",
                new Boolean(true));
        
        options.add(
        		O_PSO_COGNITIVE_LEARNING_RATE,
                "pso cognitive learning rate",
                new Double(2.05));
        
        options.add(
        		O_PSO_SOCIAL_LEARNING_RATE,
                "pso social learning rate",
                new Double(1.4));
        
        options.add(
        		O_PSO_PROBILITY_INTERVAL_PBEST,
                "pso probility interval for pbest",
                new Double(0.75));
        options.add(
        		O_PSO_PROBILITY_INTERVAL_WPBGB,
                "pso probility interval for all",
                new Double(0.5));
        options.add(
        		O_PSO_PROBILITY_INTERVAL_GBEST,
                "pso probility interval for gbest",
                new Double(0.25));
        
        options.add(
        		O_PSO_MINIMUM_ITERATION,
                "pso minimum interation",
                new Integer(100));
        
        options.add(
        		O_PSO_INTERVAL,
                "pso interval",
                new Integer(20));
        
        options.add(
        		O_PSO_QUALITY,
        		"pso quality",
        		new Double(1.0005)
        		);

        options.add(
                O_OUTER_EFFORT_LEVEL,
                "number of solve-legalize iterations",
                new Integer(40));

        options.add(
                O_INNER_EFFORT_LEVEL,
                "number of gradient steps to take in each outer iteration",
                new Integer(40));
    }

    protected double anchorWeight;
    protected final double anchorWeightStop, anchorWeightExponent;

    private final double maxConnectionLength;
    protected double learningRate, learningRateMultiplier;
    private final double beta1, beta2, eps;
    private final boolean usePSO;
    private final double c1, c2;
    private double forPbest, forGbest, forall;
    private final int minimumIter, interval;
    private final double psoQuality;

    private double latestCost, minCost;

    protected final int numIterations, effortLevel;

    // Only used by GradientPlacerTD
    protected double tradeOff; 
    protected List<CritConn> criticalConnections;

    private CostCalculator costCalculator;

    protected Legalizer legalizer;
    protected LinearSolverGradient solver;

    private Map<BlockType, boolean[]> netMap;
    private boolean[] allTrue;
    private int[] netEnds;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;

    protected boolean[] fixed;
    private double[] coordinatesX;
    private double[] coordinatesY;

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

    	this.effortLevel = this.options.getInteger(O_INNER_EFFORT_LEVEL);
    	this.numIterations = this.options.getInteger(O_OUTER_EFFORT_LEVEL) + 1;

        this.learningRate = this.options.getDouble(O_LEARNING_RATE_START);
        this.learningRateMultiplier = Math.pow(this.options.getDouble(O_LEARNING_RATE_STOP) / this.options.getDouble(O_LEARNING_RATE_START), 1.0 / (this.numIterations - 1.0));

        this.beta1 = this.options.getDouble(O_BETA1);
        this.beta2 = this.options.getDouble(O_BETA2);
        this.eps = this.options.getDouble(O_EPS);
        
        this.usePSO = this.options.getBoolean(O_USE_PSO);
        this.c1 = this.options.getDouble(O_PSO_COGNITIVE_LEARNING_RATE);
        this.c2 = this.options.getDouble(O_PSO_SOCIAL_LEARNING_RATE);
        this.forPbest = this.options.getDouble(O_PSO_PROBILITY_INTERVAL_PBEST);
        this.forGbest = this.options.getDouble(O_PSO_PROBILITY_INTERVAL_GBEST);
        this.forall = this.options.getDouble(O_PSO_PROBILITY_INTERVAL_WPBGB);
        
        this.psoQuality = this.options.getDouble(O_PSO_QUALITY);
        
        this.minimumIter = this.options.getInteger(O_PSO_MINIMUM_ITERATION);
        this.interval = this.options.getInteger(O_PSO_INTERVAL);

        this.latestCost = Double.MAX_VALUE;
        this.minCost = Double.MAX_VALUE;

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

        this.startTimer(T_INITIALIZE_DATA);

        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes,
                this.blockTypeIndexStarts,
                this.linearX,
                this.linearY,
                this.legalX,
                this.legalY,
                this.heights,
                this.visualizer,
                this.nets,
                this.netBlocks);
        this.legalizer.setQuality(0.1,  0.9);
        this.legalizer.setChoice(this.usePSO);
        this.legalizer.setpsoQuality(this.psoQuality);
//        this.legalizer.setPSOFixedLearningRate(2.05, 2.05);
        this.legalizer.setPSOVaringLearningRate(this.c1, this.c2);
        this.legalizer.setPobilityInterval(forPbest, forGbest, forall);
        this.legalizer.setPSOstopParameter(this.minimumIter, this.interval);

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
        for(BlockType blockType:BlockType.getBlockTypes(BlockCategory.CLB)){
        	this.netMap.put(blockType, new boolean[this.numRealNets]);
        	Arrays.fill(this.netMap.get(blockType), false);
        }
        for(BlockType blockType:BlockType.getBlockTypes(BlockCategory.HARDBLOCK)){
        	this.netMap.put(blockType, new boolean[this.numRealNets]);
        	Arrays.fill(this.netMap.get(blockType), false);
        }
        for(BlockType blockType:BlockType.getBlockTypes(BlockCategory.IO)){
        	this.netMap.put(blockType, new boolean[this.numRealNets]);
        	Arrays.fill(this.netMap.get(blockType), false);
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

                this.netMap.get(block.blockType)[netCounter] = true;
            }

            this.netEnds[netCounter] = netBlockCounter;
        }

        this.fixed = new boolean[this.legalX.length];

    	this.coordinatesX = new double[this.legalX.length];
    	this.coordinatesY = new double[this.legalY.length];

        this.solver = new LinearSolverGradient(
                this.coordinatesX,
                this.coordinatesY,
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
		for(int i = 0; i < this.legalX.length; i++){
			if(this.fixed[i]){
				this.coordinatesX[i] = this.legalizer.getLegalX(i);
				this.coordinatesY[i] = this.legalizer.getLegalY(i);
			}else{
				this.coordinatesX[i] = this.linearX[i];
				this.coordinatesY[i] = this.linearY[i];
			}
		}
//		for(Net net:this.nets){
//        	for(NetBlock block:net.blocks){
//        		int blockIndex = block.getBlockIndex(); 
//            	block.initializeLinear(this.linearX[blockIndex], this.linearY[blockIndex]);
//            	block.initialLegal(this.legalizer.getLegalX(blockIndex), this.legalizer.getLegalY(blockIndex));
//        	}
//		}

        for(int i = 0; i < this.effortLevel; i++) {
            this.solveLinearIteration(processNets);
            //TODO to visualize the gradient descent step
            this.visualizer.addPlacement(String.format("gradient descent step %d", i), this.netBlocks, this.solver.getCoordinatesX(), this.solver.getCoordinatesY(), -1);
        }
        
		for(int i = 0; i < this.legalX.length; i++){
			if(!this.fixed[i]){
				this.linearX[i] = this.coordinatesX[i];
				this.linearY[i] = this.coordinatesY[i];
			}
		}
		
//		for(Net net:this.nets){
//    		for(NetBlock block:net.blocks){
//    			int blockIndex = block.getBlockIndex(); 
//        		if(!this.fixed[blockIndex])block.initializeLinear(this.coordinatesX[blockIndex], this.coordinatesY[blockIndex]);
//    		}
//		}
		
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudo connections into account
     */
    protected void solveLinearIteration(boolean[] processNets) {
        this.startTimer(T_BUILD_LINEAR);

        // Set value of alpha and reset the solver
        this.solver.initializeIteration(this.anchorWeight, this.learningRate);

        // Process nets
        this.processNets(processNets);
//        this.processNetsNew(processNets, this.fixed);
        
//        for(Net net:this.nets){
//    		for(NetBlock block:net.blocks){
//    			if(!block.isInitialized) System.out.println(block.getBlockIndex() + " not initialized");
//    		}
//    	}
//        
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

    protected void processNets(boolean[] processNet) {
        int numNets = this.netEnds.length;//this.netEnds = new int[this.numRealNets];

        int netStart, netEnd = 0;
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            netStart = netEnd;
            netEnd = this.netEnds[netIndex];
//            System.out.println(netIndex);
        	if(processNet[netIndex]){
                this.solver.processNet(netStart, netEnd);
        	}
//        	System.out.println();
        }
    }
    ////////////////////////////new change//////////////////////////////////////////////
    protected void processNetsNew(boolean[] processNet, boolean[] fixed) {	
        for(int netIndex = 0; netIndex < this.numRealNets; netIndex++) {
//            System.out.println(netIndex);
        	if(processNet[netIndex]){
                this.solver.processNetNew(this.nets.get(netIndex), this.fixed);
        	}
//        	System.out.println();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void solveLegal() {
        this.startTimer(T_LEGALIZE);
        for(BlockType legalizeType:BlockType.getBlockTypes(BlockCategory.CLB)){
        	this.legalizer.legalize(legalizeType);
        }
        for(BlockType legalizeType:BlockType.getBlockTypes(BlockCategory.HARDBLOCK)){
        	this.legalizer.legalize(legalizeType);
        }
        for(BlockType legalizeType:BlockType.getBlockTypes(BlockCategory.IO)){
        	this.legalizer.legalize(legalizeType);
        }
        this.stopTimer(T_LEGALIZE);
    }

    @Override
    protected void solveLegal(BlockType legalizeType) {
        this.startTimer(T_LEGALIZE);
        this.legalizer.legalize(legalizeType);
        this.stopTimer(T_LEGALIZE);
    }

    @Override
    protected void updateLegalIfNeeded(){
    	this.startTimer(T_UPDATE_CIRCUIT);

    	this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
    	this.legalCost = this.costCalculator.calculate(this.legalizer.getLegalX(), this.legalizer.getLegalY());

    	if(this.isTimingDriven()){
    		this.calculateTimingCost();
    		this.latestCost = Math.pow(this.legalCost, 1) * Math.pow(this.timingCost, 1);
    	}else{
    		this.latestCost = this.legalCost;
    	}

    	if(this.latestCost < this.minCost){
    		this.minCost = this.latestCost;
    		this.updateLegal(this.legalizer.getLegalX(), this.legalizer.getLegalY());
    	}
    	this.stopTimer(T_UPDATE_CIRCUIT);
    }

    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("it");
        titles.add("stepsize");
        titles.add("anchor");
        titles.add("anneal Q");
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
        titles.add("overlap");
    }

    @Override
    protected void printStatistics(int iteration, double time, int overlap) {
        List<String> stats = new ArrayList<>();

        stats.add(Integer.toString(iteration));
        stats.add(String.format("%.3f", this.learningRate));
        stats.add(String.format("%.3f", this.anchorWeight));
        stats.add(String.format("%.5f", this.legalizer.getQuality()));
        stats.add(String.format("%.1f", this.maxConnectionLength));

        //Wirelength cost
        stats.add(String.format("%.0f", this.linearCost));
        stats.add(String.format("%.0f", this.legalCost));

        //Timing cost
        if(this.isTimingDriven()){
        	stats.add(String.format("%.4g", this.timingCost));
        }

        stats.add(this.latestCost == this.minCost ? "yes" : "");
        stats.add(String.format("%.0f", time*Math.pow(10, 3)));
        stats.add(String.format("%d", this.criticalConnections.size()));
        stats.add(String.format("%d", overlap));

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
}
