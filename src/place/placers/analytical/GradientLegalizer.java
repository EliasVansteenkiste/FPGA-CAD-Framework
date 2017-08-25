package place.placers.analytical;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

class GradientLegalizer extends Legalizer {

	private LegalizerBlock[] blocks;

	private final double stepSize, speedAveraging;

    private final PushingSpreader spreader;
    private final DetailedLegalizer detailedLegalizer;

    private final List<Integer> legalColumns;
    private final List<Integer> illegalColumns;
    private Map<Integer, Integer> columnMap;

	private TimingTree timer = null;

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

    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, nets, visualizer, netBlocks);

    	this.legalColumns = new ArrayList<>();
    	this.illegalColumns = new ArrayList<>();
    	for(BlockType blockType:this.circuit.getBlockTypes()){
    		if(blockType.getCategory().equals(BlockCategory.CLB)){
    			for(int column:this.circuit.getColumnsPerBlockType(blockType)){
    				this.legalColumns.add(column);
    			}
    		}else if(blockType.getCategory().equals(BlockCategory.HARDBLOCK)){
    			for(int column:this.circuit.getColumnsPerBlockType(blockType)){
    				this.illegalColumns.add(column);
    			}
    		}
    	}
    	
    	this.columnMap = new HashMap<>();
    	int substract = 0;
    	for(int c = 1; c < this.circuit.getWidth() + 1; c++){
    		if(this.circuit.getColumnType(c).getCategory().equals(BlockCategory.HARDBLOCK)){
    			substract += 1;
    		}
    		this.columnMap.put(c - substract, substract);
    	}

    	this.stepSize = 0.6;
    	this.speedAveraging = 0.2;

    	this.timer = new TimingTree(false);

    	this.spreader = new PushingSpreader(
    			this.legalColumns.size(),	//width
    			this.height,				//height
    			this.timer);

    	this.detailedLegalizer = new DetailedLegalizer(circuit);
    }

    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
        this.timer.start("Legalize BlockType");

        this.initializeData(blocksStart, blocksEnd);

        float scalingFactor = (float)this.legalColumns.size() / (this.legalColumns.size() + this.illegalColumns.size());

        for(LegalizerBlock block:this.blocks){
        	block.horizontal.coordinate = block.horizontal.coordinate * scalingFactor;
        }
            
        this.timer.start("Spreading");
        this.spreader.doSpreading(this.blocks, this.blocks.length, this.gridForce);
        this.timer.time("Spreading");
           
        for(LegalizerBlock block:this.blocks){
        	int column = (int)Math.floor(block.horizontal.coordinate - 0.25);
        	
        	block.horizontal.coordinate += this.columnMap.get(column);
        }
        
        this.timer.start("Detailed legalizer");
        this.detailedLegalizer.legalize(this.blocks);
        this.timer.time("Detailed legalizer");

       	this.updateLegal();

       	this.timer.time("Legalize BlockType");
    }

    //INITIALISATION
    private void initializeData(int blocksStart, int blocksEnd){
    	this.timer.start("Initialize Data");

    	double x, y;

    	//Initialize all blocks
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
			x = this.linearX[b];
			y = this.linearY[b];

    		int height = this.heights[b];

    		int offset = (1- height) / 2;
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, x, y, offset, height * this.blockType.getHeight(), this.stepSize, this.speedAveraging);
    	}

    	this.timer.time("Initialize Data");
    }

    //Set legal coordinates of all blocks
    private void updateLegal(){
    	this.timer.start("Update Legal");

    	for(LegalizerBlock block:this.blocks){
    		this.legalX[block.index] = (int)Math.round(block.horizontal.coordinate);
    		this.legalY[block.index] = (int)Math.round(block.vertical.coordinate  - block.offset);
    	}

    	this.timer.time("Update Legal");
    }

    class LegalizerBlock {
    	final int index;

    	final Dimension horizontal;
    	final Dimension vertical;

    	final int offset;
    	final int height;

    	int ceilx, ceilxlow, ceilxhigh;
    	int ceily, ceilylow, ceilyhigh;
    	float sw, nw, se, ne;

    	boolean processed;

    	LegalizerBlock(int index, double x, double y, int offset, int height, double stepSize, double speedAveraging){
    		this.index = index;

    		this.horizontal = new Dimension(x, stepSize, speedAveraging);
    		this.vertical = new Dimension(y + offset, stepSize, speedAveraging);

    		this.offset = offset;
    		this.height = height;
    	}
    	void update(){
        	this.ceilx = (int)Math.ceil(this.horizontal.coordinate + this.horizontal.coordinate);
        	this.ceily = (int)Math.ceil(this.vertical.coordinate + this.vertical.coordinate);

        	this.ceilxlow = this.ceilx - 1;
        	this.ceilxhigh = this.ceilx + 1;

        	this.ceilylow = this.ceily - 1;
        	this.ceilyhigh = this.ceily + 1;

    		float xLeft = (this.ceilx * 0.5f) - this.horizontal.coordinate;
    		float xRight = 0.5f - xLeft;

    		float yLeft = (this.ceily * 0.5f) - this.vertical.coordinate;
    		float yRight = 0.5f - yLeft;

    		this.sw = xLeft * yLeft;
    		this.nw = xLeft * yRight;
    		this.se = xRight * yLeft;
    		this.ne = xRight * yRight;
    	}

    	double cost(int legalX, int legalY){
    		return (this.horizontal.coordinate - legalX) * (this.horizontal.coordinate - legalX) + (this.vertical.coordinate - legalY) * (this.vertical.coordinate - legalY);
    	}
    }
    class Dimension {
    	float coordinate;
    	float speed;
    	float force;

    	final float stepSize;
    	final float speedAveraging;

    	Dimension(double coordinate,  double stepSize, double speedAveraging){
    		this.coordinate = (float) coordinate;
    		this.speed = 0;
    		this.force = 0;

    		this.stepSize = (float) stepSize;
    		this.speedAveraging = (float) speedAveraging;
    	}
    	void setForce(float force){
    		this.force = force;
    	}

    	void solve(){
    		if(this.force != 0.0){
    			float newSpeed = this.stepSize * this.force;

            	this.speed = this.speedAveraging * this.speed + (1 - this.speedAveraging) * newSpeed;

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
