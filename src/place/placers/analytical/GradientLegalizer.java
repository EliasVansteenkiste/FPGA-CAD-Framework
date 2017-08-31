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
import place.visual.PlacementVisualizer;

class GradientLegalizer extends Legalizer {
	private Block[] blocks;
	private Cluster mainCluster;
	private Cluster[] hierarchyClusters;

	private final double stepSize, speedAveraging;

    private final List<Integer> legalColumns;
    private final List<Integer> illegalColumns;
    private final Map<Integer, Integer> columnMap;
    private final double scalingFactor;

    GradientLegalizer(
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

    	this.stepSize = 2;
    	this.speedAveraging = 0.2;
    }

    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
        this.initializeData(blocksStart, blocksEnd, this.legalColumns.size(), this.height);
        this.visual("Before spreading");
        this.doSpreading(1000);
        this.visual("After spreading");
       	this.updateLegal();
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
    	this.hierarchyClusters = new Cluster[clusterIndex.size()];
    	for(int i = 0; i < this.hierarchyClusters.length; i++){
    		this.hierarchyClusters[i] = new Cluster(gridWidth, gridHeight);
    	}
    	this.mainCluster = new Cluster(gridWidth, gridHeight);

    	for(Block lb:this.blocks){
    		this.hierarchyClusters[lb.leafNode].addBlock(lb);
    		this.mainCluster.addBlock(lb);
    	}

    	this.mainCluster.initializeMassMap();
    	for(Cluster hierarchyCluster:this.hierarchyClusters){
    		hierarchyCluster.initializeMassMap();
    		this.mainCluster.addCluster(hierarchyCluster);
    	}
    }
    private void doSpreading(int numIterations){
    	int iteration = 0;
    	do{
    		this.mainCluster.applyPushingBlockForces(this.gridForce, false);
    		this.mainCluster.applyPushingBlockForces(this.gridForce, true);
    		iteration++;
    	}while(iteration < 250);
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

    	Area area;
    	Force force;

    	public Block(int index, double x, double y, float offset, int height, double stepSize, double speedAveraging, int leafNode, int gridWidth, int gridHeight){
    		this.index = index;

    		this.horizontal = new Dimension(x, stepSize, speedAveraging);
    		this.vertical = new Dimension(y, stepSize, speedAveraging);

    		this.offset = offset;
    		this.height = height;
    		
    		this.leafNode = leafNode;
    		
    		this.area = new Area();
    		this.force = new Force();
    		
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

    		this.area.sw = xLeft * yLeft;
    		this.area.nw = xLeft * yRight;
    		this.area.se = xRight * yLeft;
    		this.area.ne = xRight * yRight;
    	}
    }
    class Direction {
    	double sw, se, nw, ne, sum;
    	
    	public Direction(){
    		this.sw = 0.0;
    		this.se = 0.0;
    		this.ne = 0.0;
    		this.nw = 0.0;
    		this.sum = 0.0;
    	}
    	public void setSum(){
    		this.sum = this.sw + this.nw + this.se + this.ne;
    	}
    }
    class Area extends Direction {
    	public Area(){
    		super();
    	}
    }
    class Force extends Direction {
    	private double xgrid;
    	private double ygrid;
    	
    	public Force(){
    		super();
    	}

    	public double horizontal(){
    		return (this.sw + this.nw - this.se - this.ne + this.xgrid) / (this.sum + this.xgrid);
    	}
    	public double vertical(){
    		return (this.sw - this.nw + this.se - this.ne + this.ygrid) / (this.sum + this.ygrid);
    	}
    }
    class Dimension {
    	double coordinate;
    	double speed;
    	double force;

    	final double stepSize;
    	final double speedAveraging;

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
    
    class Cluster {
    	final int width, height;
    	final MassMap massMap;
    	
    	final List<Block> blocks;
    	final List<Cluster> clusters;
    	
    	Cluster(int width, int height){
        	this.width = width;
        	this.height = height;
        	this.massMap = new MassMap(this.width, this.height);
        	
    		this.blocks = new ArrayList<>();
    		this.clusters = new ArrayList<>();
    	}
    	
    	public void addBlock(Block block){
    		this.blocks.add(block);
    	}
    	public void addCluster(Cluster cluster){
    		this.clusters.add(cluster);
    	}

        public void initializeMassMap(){
        	this.massMap.reset();
        	
        	for(Block block:this.blocks){
        		this.massMap.add(block);
        	}
        }
        private void applyPushingBlockForces(double gridForce, boolean clusterBased){
        	this.massMap.setGridForce(gridForce);
        	if(clusterBased){
        		for(Cluster cluster:this.clusters){
	        		for(Block block:cluster.blocks){
	        			this.massMap.setForce(block);
	        		}
	
	        		double horizontalForce = 0.0;
	        		double verticalForce = 0.0;
	        		
	        		for(Block block:cluster.blocks){
	        			horizontalForce += block.horizontal.force;
	        			verticalForce += block.vertical.force;
	        		}
	
	        		horizontalForce /= 0.5 * cluster.blocks.size();
	        		verticalForce /= 0.5 * cluster.blocks.size();
	
	        		for(Block block:cluster.blocks){
	        			block.horizontal.setForce(horizontalForce);
	        			block.vertical.setForce(verticalForce);
	        		}
	        		
	        		for(Block block:cluster.blocks){
	        			this.massMap.substract(block);
	        			block.doForce(this.width, this.height);
	            		this.massMap.add(block);
	        		}
        		}
        	}else{
            	for(Block block:this.blocks){
            		this.massMap.setForce(block);
            		this.massMap.substract(block);
            		block.doForce(this.width, this.height);
            		this.massMap.add(block);
            	}
        	}
        }
    }
    
    class MassMap {
    	private final int width, height;
    	private final int gridWidth, gridHeight;
        private final double[][] massMap;
        
        private double gridForce;
        
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

        public void setGridForce(double gridForce){
        	this.gridForce = gridForce;
        }
        public void setForce(Block block){
    		this.setPushingForce(block);
    		this.setGridForce(block);

    		block.force.setSum();

    		block.horizontal.setForce(block.force.horizontal());
    		block.vertical.setForce(block.force.vertical());
        }
        private void setPushingForce(Block block){
        	int x = block.ceilx;
    		int y = block.ceily;
    		
    		block.force.nw = 0.0;
    		block.force.ne = 0.0;
    		block.force.sw = 0.0;
    		block.force.se = 0.0;

        	for(int h = 0; h < block.height; h++){
        		block.force.nw += block.area.sw * this.massMap[x - 1][y];
        		block.force.nw += block.area.nw * this.massMap[x - 1][y + 1];
        		block.force.nw += block.area.se * this.massMap[x][y];
        		block.force.nw += block.area.ne * this.massMap[x][y + 1];

        		block.force.ne += block.area.sw * this.massMap[x][y];
        		block.force.ne += block.area.nw * this.massMap[x][y + 1];
        		block.force.ne += block.area.se * this.massMap[x + 1][y];
        		block.force.ne += block.area.ne * this.massMap[x + 1][y + 1];

        		block.force.sw += block.area.sw * this.massMap[x - 1][y - 1];
        		block.force.sw += block.area.nw * this.massMap[x - 1][y];
        		block.force.sw += block.area.se * this.massMap[x][y - 1];
        		block.force.sw += block.area.ne * this.massMap[x][y];

        		block.force.se += block.area.sw * this.massMap[x][y - 1];
        		block.force.se += block.area.nw * this.massMap[x][y];
        		block.force.se += block.area.se * this.massMap[x + 1][y - 1];
        		block.force.se += block.area.ne * this.massMap[x + 1][y];

        		y += 2;
    		}
        }
        private void setGridForce(Block block){
        	//GridForce
    		double xval = block.horizontal.coordinate - Math.floor(block.horizontal.coordinate);
    		double yval = block.vertical.coordinate - Math.floor(block.vertical.coordinate);

    		if(xval < 0.25){
    			block.force.xgrid = - xval;
    		}else if(xval < 0.75){
    			block.force.xgrid = -0.5 + xval;
    		}else{
    			block.force.xgrid = 1 - xval;
    		}
	
    		if(yval < 0.25){
    			block.force.ygrid = - yval;
    		}else if(yval < 0.75){
    			block.force.ygrid = -0.5 + yval;
    		}else{
    			block.force.ygrid = 1 - yval;
    		}

    		block.force.xgrid *= this.gridForce;
    		block.force.ygrid *= this.gridForce;
        }

        public void add(Block block){
    		int x = block.ceilx;
    		int y = block.ceily;

    		for(int h = 0; h < block.height; h++){
        		this.massMap[x - 1][y - 1] += block.area.sw;
        		this.massMap[x][y - 1] += block.area.se + block.area.sw;
        		this.massMap[x + 1][y - 1] += block.area.se;

        		this.massMap[x - 1][y] += block.area.sw + block.area.nw;
        		this.massMap[x][y] += 0.25;
        		this.massMap[x + 1][y] += block.area.se + block.area.ne;

        		this.massMap[x - 1][y + 1] += block.area.nw;
        		this.massMap[x][y + 1] += block.area.nw + block.area.ne;
        		this.massMap[x + 1][y + 1] += block.area.ne;

        		y += 2;
    		}
        }
        public void substract(Block block){
    		int x = block.ceilx;
    		int y = block.ceily;

        	for(int h = 0; h < block.height; h++){
            	this.massMap[x - 1][y - 1] -= block.area.sw;
            	this.massMap[x][y - 1] -= block.area.se + block.area.sw;
            	this.massMap[x + 1][y - 1] -= block.area.se;

            	this.massMap[x - 1][y] -= block.area.sw + block.area.nw;
            	this.massMap[x][y] -= 0.25;
            	this.massMap[x + 1][y] -= block.area.se + block.area.ne;

            	this.massMap[x - 1][y + 1] -= block.area.nw;
            	this.massMap[x][y + 1] -= block.area.nw + block.area.ne;
            	this.massMap[x + 1][y + 1] -= block.area.ne;

            	y += 2;
        	}
        }
    }
}
