package place.placers.analytical;

import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

abstract class Legalizer {

    protected Circuit circuit;
    protected int width, height;

    private List<BlockType> blockTypes;
    private List<Integer> blockTypeIndexStarts;
    private int numBlocks, numIOBlocks;

    protected double[] linearX, linearY;
    protected int[] legalX, legalY;
    protected int[] heights;

    private double quality, qualityMultiplier;
    
    private double chooseQuality;
    private boolean usePSO;
    private double psoQuality, psoQualityMultiplier;
    private double c1;
    private double forPbest, forGbest;
    private int interval;

    // Properties of the blockType that is currently being legalized
    protected BlockType blockType;
    protected BlockCategory blockCategory;
    protected int blockStart, blockRepeat, blockHeight;
    
    //Hard Block Legalizer
    private HardblockSwarmLegalizer hardblockLegalizer;
    
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
        
        this.hardblockLegalizer = new HardblockSwarmLegalizer(this.linearX, this.linearY, this.legalX, this.legalY, this.heights, this.width, this.height, nets);
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
    
    void setQuality(double initialQuality, double qualityMultiplier){
    	this.quality = initialQuality;
    	this.qualityMultiplier = qualityMultiplier;
    }
    void increaseQuality(){
    	this.quality *= this.qualityMultiplier;
    }
    double getQuality(){
    	return this.quality;
    }
    //**********************
    //**for pso legalizer
    //**********************
    void setChoice(boolean usePSO){
    	this.usePSO = usePSO;
    }
    void setpsoQuality(double fixedQuality){
    	this.psoQuality = fixedQuality;
    }
    void setPSOFixedLearningRate(double cognitiveRate, double socialRate){
    	this.c1 = cognitiveRate;  	
    }
    void setPSOVaringLearningRate(double start){
    	this.c1 = start;  	
    } 
   void setPSOVaryingQuality(double initialPSOquality, double psoQualityMultiplier){
    	this.psoQuality = initialPSOquality;
    	this.psoQualityMultiplier = psoQualityMultiplier;
    }
    void increasePSOQuality(){
    	this.psoQuality *= this.psoQualityMultiplier;
    	
    }
    void setPSOstopParameter(int interval){
    	this.interval = interval;
//    	if(this.height < 80){
//    		this.interval = (int) Math.round(5 + this.height * 0.02);
//    	}else if(this.height < 100){
//    		this.interval = (int) Math.round(5 + this.height * 0.035);
//    	}else if(this.height < 120){
//    		this.interval = (int) Math.round(10 + this.height * 0.037);
//    	}else if(this.height < 140){
//    		this.interval = (int) Math.round(30 + this.height * 0.04);
//    	}else if(this.height < 160){
//    		this.interval = (int) Math.round(30 + this.height * 0.042);
//    	}else if(this.height < 180){
//    		this.interval = (int) Math.round(35 + this.height * 0.069);
//    	}else{
//    		this.interval = (int) Math.round(35 + this.height * 0.079);
//    	}
    }
    void setPobilityInterval(double forPbest, double forGbest){
    	this.forPbest = forPbest;
    	this.forGbest = forGbest;
    }
    
    protected abstract void legalizeBlockType(int blocksStart, int blocksEnd);

    void updateCriticalConnections(List<CritConn> criticalConnections){
    	this.hardblockLegalizer.updateCriticalConnections(criticalConnections);
    }

    void legalize(BlockType legalizeType) {
        for(int i = 0; i < this.blockTypes.size(); i++) {
        	if(this.blockTypes.get(i).equals(legalizeType)){
        		this.legalizeBlockType(i);
        	}
        }
    }

    private void legalizeBlockType(int i){
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
            	this.legalizeBlockType(blocksStart, blocksEnd);//CLBs are still legalized by HeAP
        	}else if(this.blockType.getCategory().equals(BlockCategory.HARDBLOCK)){
//        		this.legalizeBlockType(blocksStart, blocksEnd);//TODO change to HeAP legalizer
        		//hard block quality for pso
        		if(this.usePSO){
        			this.chooseQuality = this.psoQuality;
        			
        		}else{
        			this.chooseQuality = this.quality;
        		}
        		
        		this.hardblockLegalizer.legalizeHardblock(this.blockType, this.blockStart, this.blockRepeat, this.blockHeight, this.chooseQuality, this.c1,  
        				this.forPbest, this.forGbest, this.interval, this.usePSO);
        	}else if(this.blockType.getCategory().equals(BlockCategory.IO)){
        		this.hardblockLegalizer.legalizeIO(this.blockType, this.quality);
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
    int getLegalX(int i) {
        return this.legalX[i];
    }
    int getLegalY(int i) {
        return this.legalY[i];
    }

    protected void addVisual(String name, double[] linearX, double[] linearY){
    	this.visualizer.addPlacement(name, this.netBlocks, linearX, linearY, -1);
    }
    protected void addVisual(String name, int[] linearX, int[] linearY){
    	this.visualizer.addPlacement(name, this.netBlocks, linearX, linearY, -1);
    }
}
