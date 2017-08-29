package place.placers.analytical;

import java.util.List;
import java.util.Map;

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

    //Grid force and anneal quality
    protected double gridForce, gridForceMultiplier;
    private double annealQuality, annealQualityMultiplier;

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

    Legalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
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
    }

    Legalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
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
        
    	this(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, leafNode, visualizer, netBlocks, logger);

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

    void setAnnealQuality(double initialQuality, double finalQuality, int numIterations){
    	this.annealQuality = initialQuality;
    	this.annealQualityMultiplier = Math.pow(finalQuality / initialQuality, 1.0 / (numIterations - 1));
    }
    void setGridForce(double initialGridForce, double finalGridForce, int numIterations){
    	this.gridForce = initialGridForce;
    	this.gridForceMultiplier = Math.pow(finalGridForce / initialGridForce, 1.0 / (numIterations - 1));
    }
    void increaseAnnealQualityAndGridForce(){
    	this.annealQuality *= this.annealQualityMultiplier;
    	this.gridForce *= this.gridForceMultiplier;
    }
    double getQuality(){
    	return this.annealQuality;
    }

    protected abstract void legalizeBlockType(int blocksStart, int blocksEnd);

    void updateCriticalConnections(List<CritConn> criticalConnections){
    	this.hardblockLegalizer.updateCriticalConnections(criticalConnections);
    }

    void legalize(BlockType legalizeType) {
        for(int i = 0; i < this.blockTypes.size(); i++) {
        	if(this.blockTypes.get(i).equals(legalizeType)){
        		try {
        			this.legalizeBlockType(i);
        		} catch (BlockTypeException error) {
        			this.logger.raise("Unrecognized block type: " + this.blockType, error);
				}
        	}
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
        		this.hardblockLegalizer.legalizeHardblock(this.blockType, this.blockStart, this.blockRepeat, this.blockHeight, this.annealQuality);
        	}else if(this.blockType.getCategory().equals(BlockCategory.IO)){
        		this.hardblockLegalizer.legalizeIO(this.blockType, this.annealQuality);
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
}
