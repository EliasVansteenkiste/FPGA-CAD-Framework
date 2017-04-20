package place.placers.analytical;

import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.placers.analytical.GradientPlacerTD.CriticalConnection;
import place.visual.PlacementVisualizer;

abstract class Legalizer {

    protected Circuit circuit;
    protected int width, height;

    private List<BlockType> blockTypes;
    private List<Integer> blockTypeIndexStarts;
    private int numBlocks, numIOBlocks;

    protected double tileCapacity;

    protected double[] linearX, linearY;
    protected int[] legalX, legalY;
    protected int[] heights;

    // Properties of the blockType that is currently being legalized
    protected BlockType blockType;
    protected BlockCategory blockCategory;
    protected int blockStart, blockRepeat, blockHeight;
    
    //Hard Block Legalizer
    private HardblockConnectionLegalizer hardblockLegalizer;
    
    //Visualizer
    private final PlacementVisualizer visualizer;
    private final Map<GlobalBlock, NetBlock> netBlocks;

    Legalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            List<Net> nets,
            List<TimingNet> timingNets,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> netBlocks) {

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

        // Cache the number of blocks
        this.numBlocks = linearX.length;
        this.numIOBlocks = blockTypeIndexStarts.get(1);

        // Initialize the solution with IO positions
        this.legalX = new int[this.numBlocks];
        this.legalY = new int[this.numBlocks];

        System.arraycopy(legalX, 0, this.legalX, 0, this.numBlocks);
        System.arraycopy(legalY, 0, this.legalY, 0, this.numBlocks);

        //Hard block legalizer
        this.netBlocks = netBlocks;
        
        // Information to visualize the legalisation progress
        this.visualizer = visualizer;
        
        this.hardblockLegalizer = new HardblockConnectionLegalizer(this.linearX, this.linearY, this.legalX, this.legalY, this.heights, this.width, this.height, nets, timingNets, this.visualizer, this.netBlocks);
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

    Legalizer(Legalizer legalizer) {
        this.circuit = legalizer.circuit;
        this.width = legalizer.width;
        this.height = legalizer.height;

        this.blockTypes = legalizer.blockTypes;
        this.blockTypeIndexStarts = legalizer.blockTypeIndexStarts;

        this.linearX = legalizer.linearX;
        this.linearY = legalizer.linearY;
        this.legalX = legalizer.legalX;
        this.legalY = legalizer.legalY;
        this.heights = legalizer.heights;

        this.numBlocks = legalizer.numBlocks;
        this.numIOBlocks = legalizer.numIOBlocks;
        
        this.visualizer = legalizer.visualizer;
        this.netBlocks = legalizer.netBlocks;
    }

    protected abstract void legalizeBlockType(int blocksStart, int blocksEnd);

    void legalize(double tileCapacity, List<CriticalConnection> criticalConnections) {
    	long start = System.nanoTime();
    	
    	this.tileCapacity = tileCapacity;
        
        this.hardblockLegalizer.updateCriticalConnections(criticalConnections);

        // Skip i = 0: these are IO blocks
        for(int i = 1; i < this.blockTypes.size(); i++) {
            this.blockType = this.blockTypes.get(i);
            this.legalizeBlockType(i);
        }
        
        //Legalize the IO blocks at the end
        this.blockType = this.blockTypes.get(0);
        this.legalizeBlockType(0);
        
    	long end = System.nanoTime();
    	double time = (end -  start) * Math.pow(10, -6);
    	System.out.printf("Legalization took %.0f ms\n", time);
    }
    
    private void legalizeBlockType(int i){
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
            	long start = System.nanoTime();
            	this.legalizeBlockType(blocksStart, blocksEnd);
            	long end = System.nanoTime();
            	double time = (end -  start) * Math.pow(10, -6);
            	System.out.printf("Legalize LAB took %.0f ms\n", time);
        	}else if(this.blockType.getCategory().equals(BlockCategory.HARDBLOCK)){
        		this.hardblockLegalizer.legalizeHardblock(this.blockType, this.blockStart, this.blockRepeat, this.blockHeight);
        	}else if(this.blockType.getCategory().equals(BlockCategory.IO)){
        		this.hardblockLegalizer.legalizeIO(this.blockType);

        		for(int b = blocksStart; b < blocksEnd; b++){
        			this.linearX[b] = this.legalX[b];
        			this.linearY[b] = this.legalY[b];
        		 }
        	}else{
        		System.out.println("unrecognized block type: " + this.blockType);
        	}
        }
    }

    int[] getLegalX() {
        return this.legalX;
    }
    int[] getLegalY() {
        return this.legalY;
    }
    
    protected void addVisual(String name, double[] linearX, double[] linearY){
    	this.visualizer.addPlacement(name, this.netBlocks, linearX, linearY, -1);
    }
    protected void addVisual(String name, int[] linearX, int[] linearY){
    	this.visualizer.addPlacement(name, this.netBlocks, linearX, linearY, -1);
    }
}
