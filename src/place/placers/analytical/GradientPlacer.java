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

        O_MAX_CONN_LENGTH_RATIO_SPARSE = "max conn length ratio sparse",
        O_MAX_CONN_LENGTH_DENSE = "max conn length dense",

        O_BETA1 = "beta1",
        O_BETA2 = "beta2",
        O_EPS = "eps",

        O_OUTER_EFFORT_LEVEL = "outer effort level",
        
        O_INNER_EFFORT_LEVEL_START = "inner effort level start",
        O_INNER_EFFORT_LEVEL_STOP = "inner effort level stop",
        
        /////////////////////////
        // Parameters to sweep //
        /////////////////////////
        O_INTERPOLATION_FACTOR = "interpolation",
        O_CLUSTER_SCALING_FACTOR = "cluster scaling factor",
        O_SPREAD_BLOCK_ITERATIONS = "spread block iterations",
        		
        O_STEP_SIZE_START = "step size start",
        O_STEP_SIZE_STOP = "step size stop";

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
                O_OUTER_EFFORT_LEVEL,
                "number of solve-legalize iterations",
                new Integer(27));
        options.add(
                O_INNER_EFFORT_LEVEL_START,
                "number of gradient steps to take in each outer iteration in the beginning",
                new Integer(200));
        options.add(
                O_INNER_EFFORT_LEVEL_STOP,
                "number of gradient steps to take in each outer iteration at the end",
                new Integer(50));
        
        //Parameters to sweep
        options.add(
                O_INTERPOLATION_FACTOR,
                "the interpolation between linear and legal solution as starting point for detailed legalization",
                new Double(0.5));
        options.add(
                O_CLUSTER_SCALING_FACTOR,
                "the force of the inter-cluster spreading is scaled to avoid large forces",
                new Double(0.75));
        options.add(
        		O_SPREAD_BLOCK_ITERATIONS,
                "the number of independent block spreading iterations",
                new Integer(250));
        options.add(
                O_STEP_SIZE_START,
                "initial step size in gradient cluster legalizer",
                new Double(0.2));
        options.add(
                O_STEP_SIZE_STOP,
                "final step size in gradient cluster legalizer",
                new Double(0.05));
    }

    protected double anchorWeight;
    protected final double anchorWeightStop, anchorWeightExponent;
    
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
    
    private final int numNetWorkers;
    private NetWorker[] netWorkers;
    protected SolverGradient solver;

    private NetMap netMap;
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

    	this.effortLevelStart = this.options.getInteger(O_INNER_EFFORT_LEVEL_START);
    	this.effortLevelStop = this.options.getInteger(O_INNER_EFFORT_LEVEL_STOP);
    	this.effortLevel = this.effortLevelStart;
    	
    	this.numIterations = this.options.getInteger(O_OUTER_EFFORT_LEVEL) + 1;

        this.learningRate = this.options.getDouble(O_LEARNING_RATE_START);
        this.learningRateMultiplier = Math.pow(this.options.getDouble(O_LEARNING_RATE_STOP) / this.options.getDouble(O_LEARNING_RATE_START), 
        								1.0 / (this.numIterations - 1.0));

        this.beta1 = this.options.getDouble(O_BETA1);
        this.beta2 = this.options.getDouble(O_BETA2);
        this.eps = this.options.getDouble(O_EPS);
        
        this.numNetWorkers = 20;

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
	        double widthFactor = Math.pow((1.0 * this.circuit.getWidth()) / 100.0, 1.3);
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
	        this.legalizer.addSetting(
	        		"anneal_quality", 
	        		0.1,
	        		0.001);
	        this.legalizer.addSetting(
	        		"step_size", 
	        		widthFactor * this.options.getDouble(O_STEP_SIZE_START),  
	        		widthFactor * this.options.getDouble(O_STEP_SIZE_STOP));
	        this.legalizer.addSetting(
	        		"interpolation", 
	        		this.options.getDouble(O_INTERPOLATION_FACTOR));
	        this.legalizer.addSetting(
	        		"cluster_scaling",
	        		this.options.getDouble(O_CLUSTER_SCALING_FACTOR));
	        this.legalizer.addSetting(
	        		"block_spreading",
	        		this.options.getInteger(O_SPREAD_BLOCK_ITERATIONS));
        }
        
        this.fixed = new boolean[this.linearX.length];
    	this.coordinatesX = new double[this.linearX.length];
    	this.coordinatesY = new double[this.linearY.length];
    	
        this.makeNetMap();
        this.makeNetWorkers();
        
        //TODO DEBUG
        System.out.println("The system has " + this.netMap.getNets().length + " nets" + " => num real nets " + this.numRealNets);
        for(BlockType type:this.circuit.getGlobalBlockTypes()){
        	System.out.println("\t" + type + " has " + this.netMap.getNets(type).length + " nets");
        }
    	 
    	this.solver = new SolverGradient(
    							this.netWorkers,
    							this.coordinatesX,
    							this.coordinatesY,
    							this.fixed,
    							this.beta1,
    							this.beta2,
    							this.eps);


        this.costCalculator = new CostCalculatorWLD(this.nets);

        this.stopTimer(T_INITIALIZE_DATA);
    }
    
    class NetMap {
    	private NetArray netArray;
    	private Map<BlockType, NetArray> netArrayPerBlocktype;
    	
    	public NetMap(List<BlockType> blockTypes) {
    		this.netArray = new NetArray();
    		this.netArrayPerBlocktype = new HashMap<>();
    		for(BlockType blockType: blockTypes) {
    			this.netArrayPerBlocktype.put(blockType, new NetArray());
    		}
    	}
    	
    	public void finish() {
    		this.netArray.finish();
    		for(NetArray netArray:this.netArrayPerBlocktype.values()){
    			netArray.finish();
    		}
    	}
    	
    	public void addNet(Net net) {
    		this.netArray.add(net);
    	}
    	public void addNet(BlockType blockType, Net net) {
    		this.netArrayPerBlocktype.get(blockType).add(net);
    	}
    	
    	public NetArray getNetArray(){
    		return this.netArray;
    	}
    	public NetArray getNetArray(BlockType blockType){
    		return this.netArrayPerBlocktype.get(blockType);
    	}
    	
    	public Net[] getNets() {
    		return this.netArray.getAll();
    	}
    	public Net[] getNets(BlockType blockType) {
    		return this.netArrayPerBlocktype.get(blockType).getAll();
    	}
    }
    class NetArray {
    	private Net[] values;
    	private List<Net> temp;
    	
    	private int counter;
    	
    	public NetArray() {
    		this.temp = new ArrayList<>();
    	}
    	
    	public void finish() {
            this.values = new Net[this.temp.size()];
            this.temp.toArray(this.values);
            this.temp.clear();
            this.temp = null;
    	}
    	
    	public void add(Net net) {
    		this.temp.add(net);
    	}
    	public Net[] getAll() {
    		return this.values;
    	}
    	
    	public void initializeStream() {
    		this.counter = 0;
    	}
    	public Net getNext() {
    		return this.values[this.counter++];
    	}
    	public boolean hasNext() {
    		return this.counter < this.values.length;
    	}
    }
    
    private void makeNetMap() {
        this.netMap = new NetMap(this.circuit.getGlobalBlockTypes());

        for(int netCounter = 0; netCounter < this.numRealNets; netCounter++) {
        	Net net = this.nets.get(netCounter);
            
        	this.netMap.addNet(net);
        	for(BlockType blockType:net.blockTypes){
        		this.netMap.addNet(blockType, net);
        	}
        }
        
        this.netMap.finish();
    }
    private void makeNetWorkers() {
    	this.netWorkers = new NetWorker[this.numNetWorkers];
    	for(int i = 0; i < this.numNetWorkers; i++){
            this.netWorkers[i] = new NetWorker(
            		"Networker_" + i,
                    this.coordinatesX,
                    this.coordinatesY,
                    this.maxConnectionLength);
    	}
    }
    
    @Override
    protected void solveLinear(int iteration) {
    	Arrays.fill(this.fixed, false);
		for(BlockType blockType : BlockType.getBlockTypes(BlockCategory.IO)){
			this.fixBlockType(blockType);
		}

		this.doSolveLinear(this.netMap.getNetArray());
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

		this.doSolveLinear(this.netMap.getNetArray(solveType));
    }

    private void fixBlockType(BlockType fixBlockType){
    	for(GlobalBlock block:this.netBlocks.keySet()){
    		if(block.getType().equals(fixBlockType)){
    			int blockIndex = this.netBlocks.get(block).getBlockIndex();
    			this.fixed[blockIndex] = true;
    		}
    	}
    }
    private void doSolveLinear(NetArray nets){
    	this.setCoordinates();
		this.initializeNetWorkers(nets);
        for(int i = 0; i < this.effortLevel; i++) {
            this.solveLinearIteration();
        }
        this.updateCoordinates();
        
        int c = 0;
        for(NetWorker n : this.netWorkers){
        	c += n.netCounter + n.critCounter;
        }
        System.out.println(c);
    }
    private void setCoordinates() {
		for(int i = 0; i < this.linearX.length; i++){
			if(this.fixed[i]){
				this.coordinatesX[i] = this.legalX[i];
				this.coordinatesY[i] = this.legalY[i];
			}else{
				this.coordinatesX[i] = this.linearX[i];
				this.coordinatesY[i] = this.linearY[i];
			}
		}
    }
    private void updateCoordinates() {
		for(int i = 0; i < this.linearX.length; i++) {
			if(!this.fixed[i]){
				this.linearX[i] = this.coordinatesX[i];
				this.linearY[i] = this.coordinatesY[i];
			}
		}
    }
    private void initializeNetWorkers(NetArray nets) {
		for(NetWorker netWorker:this.netWorkers){
			netWorker.reset();
		}
		
    	this.netWorkers[0].setCriticalConnections(this.criticalConnections);
		
		if(this.numNetWorkers == 1){
			this.netWorkers[0].setNets(nets.getAll());
		}else if(this.numNetWorkers == 2){
			this.netWorkers[1].setNets(nets.getAll());
		}else{
			int totalFanout = 0;
			for(Net net:nets.getAll()) totalFanout += net.numBlocks;
			int fanoutPerThread = (int) Math.ceil((double)totalFanout / (this.numNetWorkers - 1));
			
			nets.initializeStream();
			for(int i = 1; i < this.numNetWorkers; i++){
				NetWorker netWorker = this.netWorkers[i];
				List<Net> workerNets = new ArrayList<>();
				int threadFanout = 0;
				while(threadFanout < fanoutPerThread && nets.hasNext()){
					Net net = nets.getNext();
					threadFanout += net.numBlocks;
					workerNets.add(net);
				}
				netWorker.setNets(workerNets);
			}
		}
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration() {
        this.startTimer(T_BUILD_LINEAR);

        for(NetWorker netWorker:this.netWorkers){
        	netWorker.resume();
        	//synchronized(netWorker){
        	//	netWorker.notify();
        	//}
        }
        
        //Wait on all threads to finish
        boolean running = true;
        while(running){
        	running = false;
        	for(NetWorker n:this.netWorkers){
        		if(!n.isFinished()){
        			running = true;
        		}
        	}
        }
        
        // Add pseudo connections
        if(this.anchorWeight != 0.0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            this.solver.addPseudoConnections(this.legalX, this.legalY);
        }

        this.stopTimer(T_BUILD_LINEAR);

        // Solve and save result
        this.startTimer(T_SOLVE_LINEAR);
        this.solver.solve(this.anchorWeight, this.learningRate);
        this.stopTimer(T_SOLVE_LINEAR);
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
    protected void calculateCost(int iteration){
    	this.startTimer(T_UPDATE_CIRCUIT);

    	this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
    	this.legalCost = this.costCalculator.calculate(this.legalX, this.legalY);
    	
    	this.currentCost = this.legalCost;

    	if(this.isTimingDriven()){
    		this.calculateTimingCost();
    		this.currentCost *= this.timingCost;
    	}
    	
    	double costMultiplier = 1 + 0.2 / (1 + Math.exp(0.2 * iteration));
    	
    	if(this.currentCost < this.bestCost){
    		this.currentCost *= costMultiplier;
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
