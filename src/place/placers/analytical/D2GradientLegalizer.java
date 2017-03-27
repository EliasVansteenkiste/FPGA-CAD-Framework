package place.placers.analytical;

import java.util.ArrayList;
import java.util.Arrays;
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

class D2GradientLegalizer extends Legalizer {

	private LegalizerBlock[] blocks;

	final BlockType lab;
    private int[] legalColumns;//1 if legal, 0 if illegal
    final D2PushingSpreader spreader;

    private static final boolean timing = false;
	private TimingTree timer = null;
    private static final boolean visual = true;

    // Arrays to visualize the legalisation progress
    private double[] visualX;
    private double[] visualY;

    D2GradientLegalizer(
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
    	this.legalColumns = new int[this.width + 2];
    	for(int column = 0; column < this.width + 2; column++){
    		if(this.circuit.getColumnType(column).equals(this.lab)){
    			this.legalColumns[column] = 1;
    		}else{
    			this.legalColumns[column] = 0;
    		}
    	}
    	
    	this.spreader = new D2PushingSpreader(
    			Arrays.stream(this.legalColumns).sum(),	//width
    			this.height,							//height
    			1.00,									//step size
    			0.2,									//speed averaging
    			this.timer);

    	if(visual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
        if(timing) this.timer.start("Legalize BlockType");

        this.initializeData(blocksStart, blocksEnd);
        this.removeIllegalColumns();

        this.addVisual("Remove illegal columns");
        	
        //Initial Spreading
        this.spreader.doSpreading(this.blocks, this.blocks.length);
        this.addVisual("Initial spreading");
        
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
    	
    	//Visual
    	if(visual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = this.linearX[i];
    			this.visualY[i] = this.linearY[i];
    		}
    	}
    	
    	if(timing) this.timer.time("Initialize Data");
    }
    void removeIllegalColumns(){
    	List<ScaleCluster> clusters = new ArrayList<ScaleCluster>();
    	
    	int counter = 0;
    	while(this.legalColumns[counter] == 0) counter++;
    	this.findScaleClusters(0.0, counter, clusters);
    	
    	//for(ScaleCluster cluster:clusters){
    	//	System.out.println(cluster);
    	//}
    	
    	for(ScaleCluster cluster:clusters){
    		for(LegalizerBlock block:this.blocks){
    			if(block.horizontal.coordinate >= cluster.illegalStart - 0.5){
    				if(block.horizontal.coordinate < cluster.illegalEnd - 0.5){
    					cluster.add(block);
    				}
    			}
    		}
    	}
    	for(ScaleCluster cluster:clusters){
    		cluster.scale();
    	}
    	
    	this.addVisual("before subtract");
    	
    	double subtract = 0.0;
    	for(ScaleCluster cluster:clusters){
    		subtract += cluster.legalStart - cluster.illegalStart;
    		
    		for(LegalizerBlock block:cluster.blocks){
    			block.horizontal.coordinate -= subtract;
    			
    			if(block.horizontal.coordinate < 0){
    				block.horizontal.coordinate = 0.1;
    			}
    		}
    		
    		subtract += cluster.illegalEnd - cluster.legalEnd;
    	}
    }
    void findScaleClusters(double illegalStart, int counter, List<ScaleCluster> clusters){
    	
    	int legalStart = counter;
    	while(this.legalColumns[counter] == 1) counter++;
    	int legalEnd = counter;

    	int illegalColumns = 0;
        while(this.legalColumns[counter] == 0){
        	illegalColumns++;
        	counter++;

        	if(counter == this.width + 2){
           		double illegalEnd = counter;
           		clusters.add(new ScaleCluster(illegalStart, legalStart, illegalEnd, legalEnd));
           		return;
           	}
        }
        
       	double illegalEnd = (double)counter - illegalColumns / 2.0;
       	clusters.add(new ScaleCluster(illegalStart, legalStart, illegalEnd, legalEnd));
       	this.findScaleClusters(illegalEnd, counter, clusters);
    }
    class ScaleCluster {
    	double illegalStart;
    	int legalStart;
    	
    	double illegalEnd;
    	int legalEnd;
    	
    	ArrayList<LegalizerBlock> blocks;
    	
    	ScaleCluster(double illegalStart, int legalStart, double illegalEnd, int legalEnd){
    		this.illegalStart = illegalStart;
    		this.legalStart = legalStart;
    		this.illegalEnd = illegalEnd;
    		this.legalEnd = legalEnd;
    		
    		this.blocks = new ArrayList<LegalizerBlock>();
    	}
    	void add(LegalizerBlock block){
    		this.blocks.add(block);
    	}
    	void scale(){
    		double illegalLeft = this.legalStart - this.illegalStart;
    		double illegalRight = this.illegalEnd - this.legalEnd;
    		
    		double illegalWidth = this.illegalEnd - this.illegalStart;
    		
    		double center = this.illegalStart - 0.5 + illegalWidth * illegalLeft / (illegalLeft + illegalRight);
    		
    		double scalingFactor = (center - this.legalStart + 0.5) / (center - this.illegalStart + 0.5);
    		
    		for(LegalizerBlock block:this.blocks){
    			block.horizontal.coordinate = center - (center - block.horizontal.coordinate) * scalingFactor;
    		}
    	}
    	
    	public String toString(){
    		String s = new String();
    		s += "Cluster | " + String.format("%.1f", this.illegalStart) + " | " + String.format("%3d", this.legalStart) + " | --- | " + String.format("%3d", this.legalEnd) + " | " + String.format("%.1f", this.illegalEnd);
    		return s;
    	}
    }

    //Set legal coordinates of all blocks
    private void updateLegal(){
    	if(timing) this.timer.start("Update Legal");
    	
    	for(LegalizerBlock block:this.blocks){
    		int x = (int) Math.round(block.horizontal.coordinate);
    		int y = (int) Math.round(block.vertical.coordinate);
    		
    		block.horizontal.coordinate = x;
    		block.vertical.coordinate = y;
    		
    		this.legalX[block.index] = x;
    		this.legalY[block.index] = y  - block.offset;
    	}
    	
    	if(timing) this.timer.time("Update Legal");
    }
    private void insertIllegalColumns(){
    	int[] newColumn = new int[Arrays.stream(this.legalColumns).sum() + 2];
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
    		block.horizontal.coordinate = newColumn[(int)Math.round(block.horizontal.coordinate)];
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
    	final int index;

    	final Dimension horizontal;
    	final Dimension vertical;

    	final int offset;
    	final int height;
    	
    	double a1, a2, a3, a4;
    	
    	LegalizerBlock(int index, double x, double y, int offset, int height){
    		this.index = index;

    		this.horizontal = new Dimension(x);
    		this.vertical = new Dimension(y + offset);

    		this.offset = offset;
    		this.height = height;
    	}
    	void updateArea(){
    		double xLeft = (Math.ceil(this.horizontal.coordinate * 2.0) * 0.5) - this.horizontal.coordinate;
    		double xRight = 0.5 - xLeft;

    		double yLeft = (Math.ceil(this.vertical.coordinate * 2.0) * 0.5) - this.vertical.coordinate;
    		double yRight = 0.5 - yLeft;
    		
    		this.a1 = xLeft * yLeft;
    		this.a2 = xLeft * yRight;
    		this.a3 = xRight * yLeft;
    		this.a4 = xRight * yRight;
    	}
    }
    
    class Dimension {
    	double coordinate;
    	double speed;
    	double force;
    	
    	Dimension(double coordinate){
    		this.coordinate = coordinate;
    		this.speed = 0.0;
    		this.force = 0.0;
    	}
    	void setForce(double force){
    		this.force = force;
    	}
    	
    	void solve(double stepSize, double speedAveraging){
    		if(this.force != 0.0){
    			double newSpeed = stepSize * this.force;

            	this.speed = speedAveraging * this.speed + (1 - speedAveraging) * newSpeed;
            	
            	this.coordinate += this.speed;
    		}
    	}
    }
}