package place.placers.analytical;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.circuit.exceptions.BlockTypeException;
import place.circuit.exceptions.OffsetException;
import place.interfaces.Logger;
import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

abstract class Legalizer {
    protected Circuit circuit;
    protected int width, height;

    private List<BlockType> blockTypes;
    private List<Integer> blockTypeIndexStarts;

    protected double[] linearX, linearY;
    protected double[] legalX, legalY;
    protected int[] heights;
    protected int[] leafNode;

    //Legalizer settings
    private final int numIterations;
    protected boolean isLastIteration;
    private final Map<String,LegalizerSetting> legalizerSettings;

    // Properties of the blockType that is currently being legalized
    protected BlockType blockType;
    protected BlockCategory blockCategory;
    protected int blockStart, blockRepeat, blockHeight;

    //Hard Block Legalizer
    private HardblockConnectionLegalizer hardblockLegalizer;

    //Visualizer
    private final PlacementVisualizer visualizer;
    private final Map<GlobalBlock, NetBlock> netBlocks;

    //Logger
    protected Logger logger;

    //Legalization runtime for each blocktype
    private final Map<BlockType,Integer> legalizationRuntime;

    Legalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            int numIterations,
            double[] linearX,
            double[] linearY,
            double[] legalX,
            double[] legalY,
            int[] heights,
            int[] leafNode,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> netBlocks,
            Logger logger) {

        // Store easy stuff
        this.circuit = circuit;
        this.width = this.circuit.getWidth();
        this.height = this.circuit.getHeight();
        this.numIterations = numIterations;

        // Store block types
        if(blockTypes.get(0).getCategory() != BlockCategory.IO) {
            throw new IllegalArgumentException("The first block type is not IO");
        }
        if(blockTypes.size() != blockTypeIndexStarts.size() - 1) {
            throw new IllegalArgumentException("The objects blockTypes and blockTypeIndexStarts don't have matching dimensions");
        }

        this.blockTypes = blockTypes;
        this.blockTypeIndexStarts = blockTypeIndexStarts;

        // Store linear solution (this array is updated by the linear solver
        this.linearX = linearX;
        this.linearY = linearY;
        this.heights = heights;
        this.leafNode = leafNode;

        // Initialize the solution with IO positions
        this.legalX = legalX;
        this.legalY = legalY;

        // Information to visualize the legalisation progress
        this.visualizer = visualizer;
        this.netBlocks = netBlocks;

        //Logger
        this.logger = logger;

        //Legalizer setting
        this.legalizerSettings = new HashMap<>();
        this.legalizationRuntime = new HashMap<>();
    }

    Legalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            int numIterations,
            double[] linearX,
            double[] linearY,
            double[] legalX,
            double[] legalY,
            int[] heights,
            int[] leafNode,
            List<Net> nets,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> netBlocks,
            Logger logger) {

    	this(circuit, blockTypes, blockTypeIndexStarts, numIterations, linearX, linearY, legalX, legalY, heights, leafNode, visualizer, netBlocks, logger);

    	try {
    		this.initializeHardblockLegalizer(nets);
    	} catch (OffsetException error) {
    		this.logger.raise("Offset problem in hardblock connection legalizer", error);
    	}
    }
    private void initializeHardblockLegalizer(List<Net> nets) throws OffsetException{
        this.hardblockLegalizer = new HardblockConnectionLegalizer(this.linearX, this.linearY, this.legalX, this.legalY, this.heights, this.width, this.height, nets, this.logger);
        for(int i = 0; i < this.blockTypes.size(); i++) {
            BlockType hardblockType = this.blockTypes.get(i);
            if(hardblockType.getCategory().equals(BlockCategory.HARDBLOCK) || hardblockType.getCategory().equals(BlockCategory.IO)){
            	int blocksStart = this.blockTypeIndexStarts.get(i);
                int blocksEnd = this.blockTypeIndexStarts.get(i + 1);

                if(blocksEnd > blocksStart) {
                	this.hardblockLegalizer.addBlocktype(hardblockType, blocksStart, blocksEnd);
                }
            }    
        }
    }

    //Legalizer settings
    void addSetting(String parameter, double initialValue, double finalValue){
    	this.legalizerSettings.put(parameter, new LegalizerSetting(initialValue, finalValue, this.numIterations));
    }
    double getSettingValue(String parameter){
    	return this.legalizerSettings.get(parameter).getValue();
    }
    void multiplySettings(){
    	for(LegalizerSetting setting:this.legalizerSettings.values()){
    		setting.multiply();
    	}
    }
    Set<String> getLegalizerSetting(){
    	return this.legalizerSettings.keySet();
    }

    protected abstract void legalizeBlockType(int blocksStart, int blocksEnd);

    void updateCriticalConnections(List<CritConn> criticalConnections){
    	this.hardblockLegalizer.updateCriticalConnections(criticalConnections);
    }

    void legalize(BlockType legalizeType, boolean isLastIteration) {
    	long startTime = System.nanoTime();

    	this.isLastIteration = isLastIteration;
        for(int i = 0; i < this.blockTypes.size(); i++) {
        	if(this.blockTypes.get(i).equals(legalizeType)){
        		try {
        			this.legalizeBlockType(i);
        		} catch (BlockTypeException error) {
        			this.logger.raise("Unrecognized block type: " + this.blockType, error);
				}
        	}
        }

        long endTime = System.nanoTime();
        int time = (int)Math.round((endTime - startTime) * Math.pow(10, -6));
        if(this.legalizationRuntime.containsKey(legalizeType)){
        	this.legalizationRuntime.put(legalizeType, this.legalizationRuntime.get(legalizeType) + time);
        }else{
        	this.legalizationRuntime.put(legalizeType, time);
        }
    }

    private void legalizeBlockType(int i) throws BlockTypeException{
    	this.blockType = this.blockTypes.get(i);

        int blocksStart = this.blockTypeIndexStarts.get(i);
        int blocksEnd = this.blockTypeIndexStarts.get(i + 1);

        if(blocksEnd > blocksStart) {
            this.blockCategory = this.blockType.getCategory();

            this.blockStart = Math.max(1, this.blockType.getStart());
            this.blockHeight = this.blockType.getHeight();
            this.blockRepeat = this.blockType.getRepeat();
            if(this.blockRepeat == -1) {
                this.blockRepeat = this.width;
            }

            if(this.blockType.getCategory().equals(BlockCategory.CLB)){
            	this.legalizeBlockType(blocksStart, blocksEnd);
        	}else if(this.blockType.getCategory().equals(BlockCategory.HARDBLOCK)){
        		this.hardblockLegalizer.legalizeHardblock(this.blockType, this.blockStart, this.blockRepeat, this.blockHeight, this.legalizerSettings.get("anneal_quality").getValue());
        	}else if(this.blockType.getCategory().equals(BlockCategory.IO)){
        		this.hardblockLegalizer.legalizeIO(this.blockType, this.legalizerSettings.get("anneal_quality").getValue());
        		for(int b = blocksStart; b < blocksEnd; b++){
        			this.linearX[b] = this.legalX[b];
        			this.linearY[b] = this.legalY[b];
        		}
        	}else{
        		throw new BlockTypeException();
        	}
        }
    }

    protected void addVisual(String name, double[] coorX, double[] coorY){
    	this.visualizer.addPlacement(name, this.netBlocks, coorX, coorY, -1);
    }

    public void printLegalizationRuntime(){
    	this.logger.println();
    	this.logger.println("----\t---------\t-------");
    	this.logger.println("Type\tTime (ms)\tRel(%)");
    	this.logger.println("----\t---------\t-------");

    	int totalTime = 0;
    	for(BlockType legalizeType:this.legalizationRuntime.keySet()){
    		totalTime += this.legalizationRuntime.get(legalizeType);
    	}

    	for(BlockType legalizeType:this.legalizationRuntime.keySet()){
    		this.logger.printf("%s\t%d\t\t%2.2f\n",legalizeType, this.legalizationRuntime.get(legalizeType), (100.0 * this.legalizationRuntime.get(legalizeType)) / totalTime);
    	}
    	this.logger.println("----\t---------\t-------");
    	this.logger.println("");
    }
}
