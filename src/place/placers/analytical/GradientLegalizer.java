package place.placers.analytical;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

class GradientLegalizer extends Legalizer {

	private LegalizerBlock[] blocks;

    final PushingSpreader spreader;
    private final DetailedLegalizer detailedLegalizer;

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
            List<TimingNet> timingNets,
            Map<GlobalBlock, NetBlock> netBlocks){

    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, nets, timingNets, visualizer, netBlocks);

    	this.timer = new TimingTree(timing);
    	
    	this.spreader = new PushingSpreader(
    			this.width,			//width
    			this.height,		//height
    			0.4,				//step size
    			0.2,				//speed averaging
    			this.timer);

    	this.detailedLegalizer = new DetailedLegalizer(circuit);
    	
    	if(visual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
        if(timing) this.timer.start("Legalize BlockType");

        this.initializeData(blocksStart, blocksEnd);
        	
        this.spreader.doSpreading(this.blocks, this.blocks.length);
        this.addVisual("Initial spreading");
        
        this.detailedLegalizer.legalize(this.blocks);
        this.addVisual("Detailed legalizer");

       	this.updateLegal();
        
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

    	boolean processed;
    	
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
    	
    	double cost(int legalX, int legalY){
    		return (this.horizontal.coordinate - legalX) * (this.horizontal.coordinate - legalX) + (this.vertical.coordinate - legalY) * (this.vertical.coordinate - legalY);
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
    public static class Comparators {
    	public static Comparator<LegalizerBlock> VERTICAL = new Comparator<LegalizerBlock>() {
    		@Override
    		public int compare(LegalizerBlock b1, LegalizerBlock b2) {
    			return Double.compare(b1.vertical.coordinate, b2.vertical.coordinate);
    		}
    	};
    }
}