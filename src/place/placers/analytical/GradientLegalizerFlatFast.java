package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

class GradientLegalizerFlatFast extends Legalizer {
	private Block[] blocks;

	private double stepSize, speedAveraging;

    private final List<Integer> legalColumns;
    private final List<Integer> illegalColumns;
    private final Map<Integer, Integer> columnMap;
    private final double scalingFactor;
    
    private final MassMap massMap;
    
    private final TimingTree timing;

    GradientLegalizerFlatFast(
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
            List<Net> nets,
            Map<GlobalBlock, NetBlock> netBlocks,
            Logger logger){

    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, leafNode, nets, visualizer, netBlocks, logger);

    	this.timing = new TimingTree(false);
    	
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
    	this.scalingFactor = (double)this.legalColumns.size() / (this.legalColumns.size() + this.illegalColumns.size());
    	
    	this.columnMap = new HashMap<>();
    	int substract = 0;
    	for(int c = 1; c < this.circuit.getWidth() + 1; c++){
    		if(this.circuit.getColumnType(c).getCategory().equals(BlockCategory.HARDBLOCK)){
    			substract += 1;
    		}
    		this.columnMap.put(c - substract, substract);
    	}

    	this.massMap = new MassMap(this.legalColumns.size(), this.height);
    	this.stepSize = 5.0;
    	this.speedAveraging = 0.75;
    }

    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
       	System.out.printf("Stepsize: %.2f  Speedaveraging: %.2f\n", this.stepSize, this.speedAveraging);

    	this.initializeData(blocksStart, blocksEnd, this.legalColumns.size(), this.height);
        this.visual("Before spreading");
        this.doSpreading(this.blocks.length / 5);
        this.visual("After spreading");
       	this.updateLegal();

       	this.stepSize *= 0.8;
       	this.speedAveraging *= 0.8;

       	for(Block block:this.blocks){
       		block.horizontal.stepSize = this.stepSize;
       		block.vertical.stepSize = this.stepSize;
       		block.horizontal.speedAveraging = this.speedAveraging;
       		block.vertical.speedAveraging = this.speedAveraging;
       	}
    }

    private void initializeData(int blocksStart, int blocksEnd, int gridWidth, int gridHeight){
    	//Initialize blocks
    	this.blocks = new Block[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
			double x = this.linearX[b];
			double y = this.linearY[b];

    		int blockHeight = this.heights[b];
    		float offset = (1 - blockHeight) / 2f;

    		int ln = this.leafNode[b];

    		//Add offset
    		y = y + offset;

    		//Scale
    		x *= this.scalingFactor;

    		this.blocks[b - blocksStart] = new Block(b, x, y, offset, blockHeight * this.blockType.getHeight(), this.stepSize, this.speedAveraging, ln, gridWidth, gridHeight);
    	}

    	//Initialize clusters
    	Set<Integer> clusterIndex = new HashSet<>();
    	for(int ln:this.leafNode){
    		clusterIndex.add(ln);
    	}
    	
    	this.initializeMassMap();
    }
    public void initializeMassMap(){
    	this.massMap.reset();
    	
    	for(Block block:this.blocks){
    		this.massMap.add(block);
    	}
    }
    private void doSpreading(int numIterations){
    	int iteration = 0;
    	do{
    		this.applyPushingBlockForces();
    		//this.visual(iteration);
    		iteration++;
    	}while(iteration < numIterations);
    }
    private void applyPushingBlockForces(){
    	this.setForce();
    	this.substract();
    	this.doForce();
    	this.add();
    }
    private void setForce(){
    	this.timing.start("set force");
    	for(Block block: this.blocks){
    		this.massMap.setForce(block);
    	}
    	this.timing.time("set force");
    }
    public void substract(){
    	this.timing.start("substract");
    	for(Block block: this.blocks){
    		this.massMap.substract(block);
    	}
    	this.timing.time("substract");
    }
    public void doForce(){
    	this.timing.start("do force");
    	for(Block block: this.blocks){
    		block.doForce(this.width, this.height);
    	}
    	this.timing.time("do force");
    }
    public void add(){
    	this.timing.start("add");
    	for(Block block: this.blocks){
    		this.massMap.add(block);
    	}
    	this.timing.time("add");
    }
    private void updateLegal(){
    	for(Block block:this.blocks){//TODO IMPROVE EXPAND FUNCTIONALITY
    		double x = block.horizontal.coordinate;
    		double y = block.vertical.coordinate;

    		//Expand with column map
    		int column = (int)Math.floor(x - 0.25);
    		x += this.columnMap.get(column);

    		//Add vertical offset
    		y = y - block.offset;

    		this.legalX[block.index] = x;
    		this.legalY[block.index] = y;
    	}
    }
    private void visual(int id){
    	this.visual("" + id);
    }
    private void visual(String name){
    	double[] coorX = new double[this.linearX.length];
    	double[] coorY = new double[this.linearX.length];
    	
    	for(int i = 0; i < this.linearX.length; i++){
    		coorX[i] = this.linearX[i];
    		coorY[i] = this.linearY[i];
    	}
    	for(Block block:this.blocks){
    		coorX[block.index] = block.horizontal.coordinate;
    		coorY[block.index] = block.vertical.coordinate;
    	}

    	this.addVisual(name, coorX, coorY);
    }


    public class Block {
    	final int index, gridWidth, gridHeight;

    	final Dimension horizontal, vertical;

    	final float offset;
    	final int height, leafNode;

    	int ceilx, ceily;
    	
    	boolean processed;

    	double area_ne, area_nw, area_se, area_sw;
    	double force_ne, force_nw, force_se, force_sw;

    	public Block(int index, double x, double y, float offset, int height, double stepSize, double speedAveraging, int leafNode, int gridWidth, int gridHeight){
    		this.index = index;

    		this.horizontal = new Dimension(x, stepSize, speedAveraging);
    		this.vertical = new Dimension(y, stepSize, speedAveraging);

    		this.offset = offset;
    		this.height = height;
    		
    		this.leafNode = leafNode;
    		
    		this.area_ne = 0.0;
    		this.area_nw = 0.0;
    		this.area_se = 0.0;
    		this.area_sw = 0.0;
    		
    		this.force_ne = 0.0;
    		this.force_nw = 0.0;
    		this.force_se = 0.0;
    		this.force_sw = 0.0;
    		
    		this.gridWidth = gridWidth;
    		this.gridHeight = gridHeight;
    		
    		this.trim();
    		this.update();
    	}
        void doForce(int gridWidth, int gridHeight){
        	this.solve();
    		this.trim();
    		this.update();
        }
        void solve(){
        	this.horizontal.solve();
        	this.vertical.solve();
        }
        void trim(){
    		if(this.horizontal.coordinate > this.gridWidth) this.horizontal.coordinate = this.gridWidth;
    		if(this.horizontal.coordinate < 1) this.horizontal.coordinate = 1;
    		
    		if(this.vertical.coordinate + this.height - 1 > this.gridHeight) this.vertical.coordinate = this.gridHeight - this.height + 1;
    		if(this.vertical.coordinate < 1) this.vertical.coordinate = 1;
        }
    	void update(){
        	this.ceilx = (int)Math.ceil(this.horizontal.coordinate + this.horizontal.coordinate);
        	this.ceily = (int)Math.ceil(this.vertical.coordinate + this.vertical.coordinate);
        	
    		double xLeft = (this.ceilx * 0.5) - this.horizontal.coordinate;
    		double xRight = 0.5 - xLeft;

    		double yLeft = (this.ceily * 0.5) - this.vertical.coordinate;
    		double yRight = 0.5 - yLeft;

    		this.area_sw = xLeft * yLeft;
    		this.area_nw = xLeft * yRight;
    		this.area_se = xRight * yLeft;
    		this.area_ne = xRight * yRight;
    	}
    	double cost(int legalX, int legalY){
    		return (this.horizontal.coordinate - legalX) * (this.horizontal.coordinate - legalX) + (this.vertical.coordinate - legalY) * (this.vertical.coordinate - legalY);
    	}
    }
    class Dimension {
    	double coordinate;
    	double speed;
    	double force;

    	 double stepSize, speedAveraging;

    	Dimension(double coordinate,  double stepSize, double speedAveraging){
    		this.coordinate = coordinate;
    		this.speed = 0;
    		this.force = 0;

    		this.stepSize = stepSize;
    		this.speedAveraging = speedAveraging;
    	}
    	void setForce(double force){
    		this.force = force;
    	}
    	void solve(){
    		if(this.force != 0.0){
    			double newSpeed = this.stepSize * this.force;

            	this.speed = this.speedAveraging * this.speed + (1 - this.speedAveraging) * newSpeed;

            	this.coordinate += this.speed;
    		}
    	}
    }

    class MassMap {
    	private final int width, height;
    	private final int gridWidth, gridHeight;
        private final double[][] massMap;
        
        public MassMap(int width, int height){
        	this.width = width;
        	this.height = height;
        	this.gridWidth = (this.width + 2) * 2;
        	this.gridHeight = (this.height + 2) * 2;

        	this.massMap = new double[this.gridWidth][this.gridHeight];
        	
        	this.reset();
        }
        public void reset(){
        	for(int x = 0; x < this.gridWidth; x++){
        		for(int y = 0; y < this.gridHeight; y++){
        			this.massMap[x][y] = 0;
        		}
        	}
        }
        public void setForce(Block block){
    		this.setPushingForce(block);

    		double mass = block.force_sw + block.force_nw + block.force_se + block.force_ne;

    		if(mass == 0.0){
        		block.horizontal.setForce(0);
        		block.vertical.setForce(0);
    		}else{
        		double horizontalForce = (block.force_sw + block.force_nw - block.force_se - block.force_ne) / mass;
        		double verticalForce = (block.force_sw - block.force_nw + block.force_se - block.force_ne) / mass;
        		
        		block.horizontal.setForce(horizontalForce);
        		block.vertical.setForce(verticalForce);
    		}
        }
        private void setPushingForce(Block block){
        	int x = block.ceilx;
    		int y = block.ceily;
    		
    		block.force_nw = 0.0;
    		block.force_ne = 0.0;
    		block.force_sw = 0.0;
    		block.force_se = 0.0;

        	for(int h = 0; h < block.height; h++){
        		block.force_nw += block.area_sw * this.massMap[x - 1][y];
        		block.force_nw += block.area_nw * this.massMap[x - 1][y + 1];
        		block.force_nw += block.area_se * this.massMap[x][y];
        		block.force_nw += block.area_ne * this.massMap[x][y + 1];

        		block.force_ne += block.area_sw * this.massMap[x][y];
        		block.force_ne += block.area_nw * this.massMap[x][y + 1];
        		block.force_ne += block.area_se * this.massMap[x + 1][y];
        		block.force_ne += block.area_ne * this.massMap[x + 1][y + 1];

        		block.force_sw += block.area_sw * this.massMap[x - 1][y - 1];
        		block.force_sw += block.area_nw * this.massMap[x - 1][y];
        		block.force_sw += block.area_se * this.massMap[x][y - 1];
        		block.force_sw += block.area_ne * this.massMap[x][y];

        		block.force_se += block.area_sw * this.massMap[x][y - 1];
        		block.force_se += block.area_nw * this.massMap[x][y];
        		block.force_se += block.area_se * this.massMap[x + 1][y - 1];
        		block.force_se += block.area_ne * this.massMap[x + 1][y];

        		y += 2;
    		}
        }
        public void add(Block block){
    		int x = block.ceilx;
    		int y = block.ceily;

    		for(int h = 0; h < block.height; h++){
        		this.massMap[x - 1][y - 1] += block.area_sw;
        		this.massMap[x][y - 1] += block.area_se + block.area_sw;
        		this.massMap[x + 1][y - 1] += block.area_se;

        		this.massMap[x - 1][y] += block.area_sw + block.area_nw;
        		this.massMap[x][y] += 0.25;
        		this.massMap[x + 1][y] += block.area_se + block.area_ne;

        		this.massMap[x - 1][y + 1] += block.area_nw;
        		this.massMap[x][y + 1] += block.area_nw + block.area_ne;
        		this.massMap[x + 1][y + 1] += block.area_ne;

        		y += 2;
    		}
        }
        public void substract(Block block){
    		int x = block.ceilx;
    		int y = block.ceily;

        	for(int h = 0; h < block.height; h++){
            	this.massMap[x - 1][y - 1] -= block.area_sw;
            	this.massMap[x][y - 1] -= block.area_se + block.area_sw;
            	this.massMap[x + 1][y - 1] -= block.area_se;

            	this.massMap[x - 1][y] -= block.area_sw + block.area_nw;
            	this.massMap[x][y] -= 0.25;
            	this.massMap[x + 1][y] -= block.area_se + block.area_ne;

            	this.massMap[x - 1][y + 1] -= block.area_nw;
            	this.massMap[x][y + 1] -= block.area_nw + block.area_ne;
            	this.massMap[x + 1][y + 1] -= block.area_ne;

            	y += 2;
        	}
        }
    }
}
