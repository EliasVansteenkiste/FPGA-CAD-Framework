package place.placers.analytical;

import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

/*
 * In this legalizer we remove the illegal columns before spreading the blocks out.
 * This way we can spread the blocks on a homogeneous FPGA.
 * Afterwards the illegal columns are inserted.
 */

class GradientLegalizer extends Legalizer {

	private LegalizerBlock[] blocks;

	final BlockType lab;
    private int legalColumns, illegalColumns;
    final PushingSpreader initialSpreading;
    final PushingSpreader mediumSpreading;
    final PushingSpreader detailedSpreading;
    final DetailedLegalizer detaildLegalizer;

    private static final boolean timing = false;
	private TimingTree timer = null;
    private static final boolean visual = true;

    // Arrays to visualize the legalisation progress
    private double[] visualX;
    private double[] visualY;

    GradientLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            PlacementVisualizer visualizer,
            List<Net> nets,
            Map<GlobalBlock, NetBlock> netBlocks){

    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, visualizer, nets, netBlocks);
    	
    	if(timing){
    		this.timer = new TimingTree();
    	}

    	this.lab = BlockType.getBlockTypes(BlockCategory.CLB).get(0);
    	this.legalColumns = 0;
    	this.illegalColumns = 0;
    	for(int column = 1; column <= this.width; column++){
    		if(this.circuit.getColumnType(column).equals(this.lab)){
    			this.legalColumns += 1;
    		}else{
    			this.illegalColumns += 1;
    		}
    	}
    	
    	this.initialSpreading = new PushingSpreader(
    			this.legalColumns,	//width
    			this.height,		//height
    			4,					//discretisation
    			0.75,				//min step size
    			0.75,				//max step size
    			0.025,				//potential
    			0.2,				//speed averaging
    			false,				//detailed
    			this.timer);
    	
    	this.mediumSpreading = new PushingSpreader(
    			this.legalColumns,	//width
    			this.height,		//height
    			6,					//discretisation
    			1.0,				//min step size
    			1.0,				//max step size
    			0.025,				//potential
    			0.2,				//speed averaging
    			false,				//detailed
    			this.timer);
    	
    	this.detailedSpreading = new PushingSpreader(
    			this.legalColumns,	//width
    			this.height,		//height
    			8,					//discretisation
    			0.5,				//min step size
    			0.5,				//max step size
    			0.05,				//potential
    			0.2,				//speed averaging
    			true,				//detailed
    			this.timer);
    	
    	this.detaildLegalizer = new DetailedLegalizer(
    			this.legalColumns,
    			this.height);

    	if(visual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
        if(timing) this.timer.start("Legalize BlockType");

        this.initializeData(blocksStart, blocksEnd);

        this.addVisual("Start of gradient legalization");
        	
        //Initial Spreading
        this.initialSpreading.doSpreading(this.blocks, this.blocks.length * 2);
        this.addVisual("Initial spreading");
        
        //Medium Spreading
        this.mediumSpreading.doSpreading(this.blocks, this.blocks.length * 2);
        this.addVisual("Medium spreading");
        
        //Detailed Spreading
       	this.detailedSpreading.doSpreading(this.blocks, this.blocks.length / 20);
       	this.addVisual("Detailed Spreading");
        
       	this.updateLegal();
       	this.addVisual("Update Legal");
       	
       	this.insertIllegalColumns();
       	this.updateLegal();
       	this.addVisual("Insert illegal columns");
        
       	if(timing) this.timer.time("Legalize BlockType");
    }

    //INITIALISATION
    private void initializeData(int blocksStart, int blocksEnd){
    	if(timing) this.timer.start("Initialize Data");

    	//Initialize all blocks
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		double x = this.linearX[b];
    		double y = this.linearY[b];
    		
    		int height = this.heights[b];
    		
    		int offset = (1- height) / 2;
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, x, y, offset, height * this.blockType.getHeight());
    	}
    	
    	//Remove illegal columns
    	double scalingFactor = (double)this.legalColumns / (this.legalColumns + this.illegalColumns);
    	for(LegalizerBlock block:this.blocks){
    		block.scaleHorizontal(scalingFactor);
    	}
    	
    	//Visual
    	if(visual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = this.linearX[i];
    			this.visualY[i] = this.linearY[i];
    		}
    	}
    	
    	if(timing) this.timer.time("Initialize Data");
    }

    //Set legal coordinates of all blocks
    private void updateLegal(){
    	if(timing) this.timer.start("Update Legal");
    	
    	for(LegalizerBlock block:this.blocks){
    		int x = (int) Math.round(block.horizontal.coordinate);
    		int y = (int) Math.round(block.vertical.coordinate);
    		
    		block.setHorizontal(x);
    		block.setVertical(y);
    		
    		this.legalX[block.index] = x;
    		this.legalY[block.index] = y  - block.offset;
    	}
    	
    	if(timing) this.timer.time("Update Legal");
    }
    private void insertIllegalColumns(){
    	int[] newColumn = new int[this.legalColumns + 2];
    	for(int i = 0; i < newColumn.length; i++){
    		newColumn[i] = 0;
    	}
    	int illegalIncrease = 0;

    	newColumn[0] = 0;
    	for(int column = 1; column < this.width + 1; column++){
    		if(this.circuit.getColumnType(column).equals(this.blockType)){
    			newColumn[column - illegalIncrease] = column;
    		}else{
    			illegalIncrease += 1;
    		}
    	}
    	newColumn[this.width + 1 - illegalIncrease] = this.width + 1;
    	
    	for(LegalizerBlock block:this.blocks){
    		block.setHorizontal(newColumn[(int)Math.round(block.horizontal.coordinate)]);
    	}
    }
    
    // Visual
    private void addVisual(String name){
    	if(visual){
    		if(timing) this.timer.start("Add Inter Visual");
    		
    		for(LegalizerBlock block:this.blocks){
    			this.visualX[block.index] = block.horizontal.coordinate;
    			this.visualY[block.index] = block.vertical.coordinate - block.offset;
    		}
    		this.addVisual(name, this.visualX, this.visualY);
    		
    		if(timing) this.timer.time("Add Inter Visual");
    	}
	}
    
    //LegalizerBlock
    class LegalizerBlock {
    	private final int index;

    	final Dimension horizontal;
    	final Dimension vertical;

    	private final int offset;
    	private final int height;
    	
    	LegalizerBlock(int index, double x, double y, int offset, int height){
    		this.index = index;

    		this.horizontal = new Dimension(x);
    		this.vertical = new Dimension(y + offset);

    		this.offset = offset;
    		this.height = height;
    	}
    	void setDiscretisation(int discretisation){
    		this.horizontal.setDiscretisation(discretisation);
    		this.vertical.setDiscretisation(discretisation);
    	}

    	int height(){
    		return this.height;
    	}
    	
    	void scaleHorizontal(double scalingFactor){
    		this.horizontal.scale(scalingFactor);
    	}
    	
    	void setHorizontal(double horizontal){
    		this.horizontal.setCoordinate(horizontal);
    	}
    	void setVertical(double vertical){
    		this.vertical.setCoordinate(vertical);
    	}
    }
    
    class Dimension {
    	private double originalCoordinate;
    	
    	private double coordinate;
    	private double speed;
    	private double force;

    	private int discretisation;
    	private int grid;

    	Dimension(double coordinate){
    		this.originalCoordinate = coordinate;
    		
    		this.coordinate = coordinate;
    		this.speed = 0.0;
    		this.force = 0.0;
    	}
    	
    	double originalCoordinate(){
    		return this.originalCoordinate;
    	}
    	double coordinate(){
    		return this.coordinate;
    	}
    	int grid(){
    		return this.grid;
    	}
    	double force(){
    		return this.force;
    	}
    	
    	void updateGridValue(){
    		this.grid = (int)Math.round(this.coordinate * this.discretisation);
    	}
    	
    	void setDiscretisation(int discretisation){
    		this.discretisation = discretisation;
    		this.updateGridValue();
    	}

    	void setForce(double force){
    		this.force = force;
    	}
    	void setCoordinate(double coordinate){
    		this.coordinate = coordinate;
    		this.updateGridValue();
    	}
    	void scale(double scalingFactor){
    		this.originalCoordinate *= scalingFactor;
    		this.coordinate *= scalingFactor;
    		this.updateGridValue();
    	}
    	
    	void solve(double stepSize, double speedAveraging){
    		if(this.force != 0.0){
    			double newSpeed = stepSize * this.force;

            	this.speed = speedAveraging * this.speed + (1 - speedAveraging) * newSpeed;
            	
            	this.coordinate += this.speed;

            	this.updateGridValue();
    		}
    	}
    }
}