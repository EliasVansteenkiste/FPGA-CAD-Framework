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

public abstract class AnalyticalPlacer extends AnalyticalAndGradientPlacer {

    private static final double EPSILON = 0.005;

    private static final String
    	O_ANCHOR_WEIGHT = "anchor weight",
    	O_ANCHOR_WEIGHT_MULTIPLIER = "anchor weight multiplier",

    	O_OUTER_EFFORT_LEVEL = "outer effort level";

    public static void initOptions(Options options) {
        AnalyticalAndGradientPlacer.initOptions(options);

        options.add(
                O_ANCHOR_WEIGHT,
                "starting anchor weight",
                new Double(0.2));

        options.add(
                O_ANCHOR_WEIGHT_MULTIPLIER,
                "anchor weight multiplier",
                new Double(1.1));

        options.add(
                O_OUTER_EFFORT_LEVEL,
                "number of solve-legalize iterations",
                new Integer(40));
    }

    protected double anchorWeight;
    protected final double anchorWeightMultiplier;

    private double latestCost, minCost;

    protected int numIterations;

    // This is only used by AnalyticalPlacerTD
    protected double tradeOff;
    protected List<CritConn> criticalConnections;

    protected CostCalculator costCalculator;

    protected Legalizer legalizer;
    protected LinearSolverAnalytical solver;

    private Map<BlockType, boolean[]> netMap;
    private boolean[] allTrue;

    protected boolean[] fixed;
    private double[] coordinatesX;
    private double[] coordinatesY;

    public AnalyticalPlacer(
    		Circuit circuit, 
    		Options options, 
    		Random random, 
    		Logger logger, 
    		PlacementVisualizer visualizer){

        super(circuit, options, random, logger, visualizer);

        this.anchorWeight = this.options.getDouble(O_ANCHOR_WEIGHT);
        this.anchorWeightMultiplier = this.options.getDouble(O_ANCHOR_WEIGHT_MULTIPLIER);

        this.numIterations = this.options.getInteger(O_OUTER_EFFORT_LEVEL) + 1;

        this.latestCost = Double.MAX_VALUE;
        this.minCost = Double.MAX_VALUE;

        this.criticalConnections = new ArrayList<>();
    }

    protected abstract void initializeIteration(int iteration);
    protected abstract void calculateTimingCost();

    @Override
    public void initializeData() {
        super.initializeData();

        this.startTimer(T_INITIALIZE_DATA);

        //Legalizer
        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes,
                this.blockTypeIndexStarts,
                this.linearX,
                this.linearY,
                this.bestLegalX,
                this.bestLegalY,
                this.heights,
                this.visualizer,
                this.nets,
                this.netBlocks);
        this.legalizer.setQuality(0.1, 0.001, this.numIterations);

        //Make a list of all the nets for each blockType
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

        for(int netCounter = 0; netCounter < this.numRealNets; netCounter++) {
            Net net = this.nets.get(netCounter);
            for(NetBlock block : net.blocks) {
                this.netMap.get(block.blockType)[netCounter] = true;
            }
        }

        this.fixed = new boolean[this.linearX.length];

    	this.coordinatesX = new double[this.linearX.length];
    	this.coordinatesY = new double[this.linearY.length];

    	this.costCalculator = new CostCalculatorWLD(this.nets);

        this.stopTimer(T_INITIALIZE_DATA);
    }

    @Override
    protected void solveLinear(int iteration) {
    	Arrays.fill(this.fixed, false);
		for(BlockType blockType : BlockType.getBlockTypes(BlockCategory.IO)){
			this.fixBlockType(blockType);
		}

    	this.doSolveLinear(this.allTrue, iteration);
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

    	this.doSolveLinear(this.netMap.get(solveType), iteration);
    }

    private void fixBlockType(BlockType fixBlockType){
    	for(GlobalBlock block:this.netBlocks.keySet()){
    		if(block.getType().equals(fixBlockType)){
    			int blockIndex = this.netBlocks.get(block).getBlockIndex();
    			this.fixed[blockIndex] = true;
    		}
    	}
    }

    protected void doSolveLinear(boolean[] processNets, int iteration){
		for(int i = 0; i < this.linearX.length; i++){
			if(this.fixed[i]){
				this.coordinatesX[i] = this.legalizer.getLegalX(i);
				this.coordinatesY[i] = this.legalizer.getLegalY(i);
			}else{
				this.coordinatesX[i] = this.linearX[i];
				this.coordinatesY[i] = this.linearY[i];
			}
		}

    	int innerIterations = iteration == 0 ? 5 : 1;
        for(int i = 0; i < innerIterations; i++) {

            this.solver = new LinearSolverAnalytical(
                    this.coordinatesX,
                    this.coordinatesY,
                    this.anchorWeight,
                    AnalyticalPlacer.EPSILON,
                    this.fixed);
            this.solveLinearIteration(processNets, iteration);
        }

		for(int i = 0; i < this.linearX.length; i++){
			if(!this.fixed[i]){
				this.linearX[i] = this.coordinatesX[i];
				this.linearY[i] = this.coordinatesY[i];
			}
		}
    }
    protected void solveLinearIteration(boolean[] processNets, int iteration) {

        this.startTimer(T_BUILD_LINEAR);

        // Add connections between blocks that are connected by a net
        this.processNetsWLD(processNets);
        
        this.processNetsTD();

        // Add pseudo connections
        if(iteration > 0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            this.solver.addPseudoConnections(this.getCurrentLegalX(), this.getCurrentLegalY());
        }

        this.stopTimer(T_BUILD_LINEAR);

        // Solve and save result
        this.startTimer(T_SOLVE_LINEAR);
        this.solver.solve();
        this.stopTimer(T_SOLVE_LINEAR);
    }

    protected void processNetsWLD(boolean[] processNet) {
        for(Net net : this.nets){
            this.solver.processNetWLD(net);
        }
    }
    protected void processNetsTD() {
        for(CritConn critConn:this.criticalConnections){
        	this.solver.processNetTD(critConn);
        }
    }

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
        this.legalCost = this.costCalculator.calculate(this.getCurrentLegalX(), this.getCurrentLegalY());
    	
    	if(this.isTimingDriven()){
    		this.calculateTimingCost();
    		this.latestCost = Math.pow(this.legalCost, 1) * Math.pow(this.timingCost, 1);
    	}else{
    		this.latestCost = this.legalCost;
    	}

    	if(this.latestCost < this.minCost){
    		this.minCost = this.latestCost;
    		this.updateLegal(this.getCurrentLegalX(), this.getCurrentLegalY());
    	}
    	this.stopTimer(T_UPDATE_CIRCUIT);
    }
    
    @Override
    protected int[] getCurrentLegalX(){
    	return this.legalizer.getLegalX();
    }
    protected int[] getCurrentLegalY(){
    	return this.legalizer.getLegalY();
    }


    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("it");
        titles.add("anchor");
        titles.add("anneal Q");
        
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
    	stats.add(String.format("%.2f", this.anchorWeight));
    	stats.add(String.format("%.5g", this.legalizer.getQuality()));

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
