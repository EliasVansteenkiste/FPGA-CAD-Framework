package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.interfaces.Logger;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

class GradientLegalizerFlatFast extends Legalizer {
	private List<Block> blocks;
	private List<Column> columns;

	private double stepSize, speedAveraging;

    private int legalColumns, illegalColumns;
    private final double scalingFactor;
    
    private double requiredOverlap;
    private List<Double> overlapHistory;
    
    private final MassMap massMap;

    GradientLegalizerFlatFast(
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
            List<Net> nets,
            Map<GlobalBlock, NetBlock> netBlocks,
            Logger logger){

    	super(circuit, blockTypes, blockTypeIndexStarts, numIterations, linearX, linearY, legalX, legalY, heights, leafNode, nets, visualizer, netBlocks, logger);
    	
    	this.legalColumns = 0;
    	this.illegalColumns = 0;
    	for(BlockType blockType:this.circuit.getBlockTypes()){
    		if(blockType.getCategory().equals(BlockCategory.CLB)){
    			this.legalColumns += this.circuit.getColumnsPerBlockType(blockType).size();
    		}else if(blockType.getCategory().equals(BlockCategory.HARDBLOCK)){
    			this.illegalColumns += this.circuit.getColumnsPerBlockType(blockType).size();
    		}
    	}
    	this.scalingFactor = (double)this.legalColumns / (this.legalColumns + this.illegalColumns);

    	this.massMap = new MassMap(this.legalColumns, this.height);
    }

    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	this.stepSize = this.getSettingValue("step_size");
    	this.speedAveraging = this.getSettingValue("speed_averaging");

    	if(this.isLastIteration){
    		this.initializeBlocks(blocksStart, blocksEnd, this.width, this.height);
    		this.sortBlocks();
    		this.initializeColumns();
    		this.legalizeBlocks();
    		this.updateLegal();
    	}else{
    		this.initializeBlocks(blocksStart, blocksEnd, this.legalColumns, this.height);
    		this.initializeMassMap();
        	this.overlapHistory = new ArrayList<>();
        	this.requiredOverlap = this.blocks.size() * 0.01;
            this.doSpreading();
            this.updateLegal();
    	}
    }

    //Initialization
    private void initializeBlocks(int blocksStart, int blocksEnd, int gridWidth, int gridHeight){
    	this.blocks = new ArrayList<>(blocksEnd - blocksStart);
    	for(int b = blocksStart; b < blocksEnd; b++){
			double x = this.linearX[b];
			double y = this.linearY[b];

    		int blockHeight = this.heights[b];
    		float offset = (1 - blockHeight) / 2f;

    		if(!this.isLastIteration) x = x * this.scalingFactor;

    		//TODO Is this required?
    		if(this.isLastIteration){
    			y = y + Math.ceil(offset);
    		}else{
    			y = y + offset;
    		}

    		this.blocks.add(new Block(b, x, y, offset, blockHeight, this.stepSize, this.speedAveraging, gridWidth, gridHeight, !this.isLastIteration));
    	}
    }
    public void sortBlocks(){
    	Collections.sort(this.blocks, Comparators.VERTICAL);
    }
    public void initializeColumns(){
        this.columns = new ArrayList<>();
        for(int columnIndex:this.circuit.getColumnsPerBlockType(BlockType.getBlockTypes(BlockCategory.CLB).get(0))){
        	Column column = new Column(columnIndex, this.height);
        	this.columns.add(column);
        }
    }
    public void initializeMassMap(){
    	this.massMap.reset();

    	for(Block block:this.blocks){
    		this.massMap.add(block);
    	}
    }

    //Spreading
    private void doSpreading(){
    	while(!this.finalIteration(this.massMap.overlap())){
    		this.applyPushingBlockForces(10);
    	}
    }
	private boolean finalIteration(double overlap){
		this.overlapHistory.add(overlap);
		if(overlap < this.requiredOverlap){
			return true;
		}else if(this.overlapHistory.size() > 10){
			double max = this.overlapHistory.get(this.overlapHistory.size() - 1);
			double min = this.overlapHistory.get(this.overlapHistory.size() - 1);

			for(int i = 0; i < 10; i++){
				double value = this.overlapHistory.get(this.overlapHistory.size() - 1 - i);
				if(value > max){
					max = value;
				}
				if(value < min){
					min = value;
				}
			}

			double ratio = max / min;
			if(ratio < 1.1){
				return true;
			}
		}
		return false;
	}
    private void applyPushingBlockForces(int numIterations){
    	for(int i = 0; i < numIterations; i++){
            for(Block block: this.blocks){
            	this.massMap.setForce(block);
            	this.massMap.substract(block);
            	block.doForce(this.width, this.height);
            	this.massMap.add(block);
            }
    	}
    }
    private void legalizeBlocks(){
    	for(Block block:this.blocks){
    		Column bestColumn = null;
    		double bestCost = Double.MAX_VALUE;

    		for(Column column:this.columns){
    			if(column.usedSize + block.height <= column.height){
        			double cost = column.tryBlock(block);
        			
        			if(cost < bestCost){
        				bestColumn = column;
        				bestCost = cost;
        			}
    			}
    		}
    		bestColumn.addBlock(block);
    	}
    	
    	//Update block coordinates
    	for(Block block:this.blocks){
    		block.processed = false;
    	}
    	for(Column column:this.columns){
    		for(Site site:column.sites){
    			if(site.hasBlock()){
    				Block block = site.block;
    				if(!block.processed){
    					block.horizontal.coordinate = site.x;
    					block.vertical.coordinate = site.y;
            			block.processed = true;
    				}
    			}
    		}
    	}
    }
    
    //Finalize
    private void updateLegal(){
    	for(Block block:this.blocks){
    		double x = block.horizontal.coordinate;
    		double y = block.vertical.coordinate;

    		if(!this.isLastIteration) x = x / this.scalingFactor;

    		if(this.isLastIteration){
    			y = y + Math.floor(-block.offset);
    		}else{
    			y = y - block.offset;
    		}

    		this.legalX[block.index] = x;
    		this.legalY[block.index] = y;
    	}
    }

    public class Block {
    	final int index, gridWidth, gridHeight;

    	final Dimension horizontal, vertical;

    	final float offset;
    	final int height;

    	int ceilx, ceily;

    	double area_ne, area_nw, area_se, area_sw;
    	double force_ne, force_nw, force_se, force_sw;
    	
    	boolean processed;

    	public Block(int index, double x, double y, float offset, int height, double stepSize, double speedAveraging, int gridWidth, int gridHeight, boolean update){
    		this.index = index;

    		this.horizontal = new Dimension(x, stepSize, speedAveraging);
    		this.vertical = new Dimension(y, stepSize, speedAveraging);

    		this.offset = offset;
    		this.height = height;
    		
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
    		if(update) this.update();
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
    		double horizontalDistance = this.horizontal.coordinate - legalX;
    		double verticalDistance = this.vertical.coordinate - legalY;
    		return Math.sqrt((horizontalDistance * horizontalDistance) + (verticalDistance * verticalDistance));//TODO SQRT?
    	}
    }
    class Dimension {
    	double coordinate;
    	double force;
    	double speed;
    	double stepSize, speedAveraging;

    	Dimension(double coordinate,  double stepSize, double speedAveraging){
    		this.coordinate = coordinate;
    		this.force = 0;
    		this.speed = 0;

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
        public double overlap(){
        	double overlap = 0.0;
        	for(int i=0; i < this.gridWidth; i++){
        		for(int j = 0; j < this.gridHeight; j++){
        			if(this.massMap[i][j] > 0.25){
        				overlap += this.massMap[i][j] - 0.25;
        			}
        		}
        	}
        	return overlap;
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

    private class Column{
    	final int index, height;

    	final Site[] sites;

    	int usedSize;

    	Site lastUsedSite, oldLastUsedSite;
    	Site firstFreeSite, oldFirstFreeSite;

    	double cost, oldCost;

    	Column(int index, int size){
    		this.index = index;
    		this.height = size;
    		
    		this.sites = new Site[this.height];
    		for(int i = 0; i < this.height; i++){
    			this.sites[i] = new Site(this.index, i + 1);
    		}
    		for(int i = 0; i < this.height; i++){
    			if(i > 0) this.sites[i].previous = this.sites[i - 1];
    			if(i < this.height - 1) this.sites[i].next = this.sites[i + 1];
    		}

    		this.usedSize = 0;
    		this.cost = 0.0;
    	}
    	
    	private void saveState(){
    		for(Site site:this.sites){
    			site.saveState();
    		}
    		this.oldLastUsedSite = this.lastUsedSite;
    		this.oldFirstFreeSite = this.firstFreeSite;
    		
    		this.oldCost = this.cost;
    	}
    	private void restoreState(){
    		for(Site site:this.sites){
    			site.restoreState();
    		}
    		this.lastUsedSite = this.oldLastUsedSite;
    		this.firstFreeSite = this.oldFirstFreeSite;
    		
    		this.cost = this.oldCost;
    	}
    	private double tryBlock(Block block){
    		this.saveState();

    		double oldCost = this.cost;
    		this.addBlock(block);
    		double newCost = this.cost;

    		this.removeBlock(block);
    		this.restoreState();

    		return newCost - oldCost;
    	}
    	private void addBlock(Block block){
    		this.usedSize += block.height;

    		int optimalY = Math.max(Math.min((int)Math.round(block.vertical.coordinate - 1), this.height - 1),  0);
    		
    		Site bestSite = this.sites[optimalY];
    		if(bestSite.hasBlock()){
        		Site currentSite = this.lastUsedSite;
        		for(int s = 0; s < block.height; s++){
        			if(currentSite.next == null){
        				this.move();
        			}else{
        				currentSite = currentSite.next;
        			}
        		}

        		bestSite = this.lastUsedSite.next;

        		this.putBlock(block, bestSite);
    		}else{
    			Site currentSite = bestSite;
        		for(int s = 0; s < block.height - 1; s++){
        			if(currentSite.next == null){
        				bestSite = bestSite.previous;
        				if(bestSite.hasBlock()){
        					this.lastUsedSite = bestSite;
        					this.move();
        				}
        			}else{
        				currentSite = currentSite.next;
        			}
        		}
        		this.putBlock(block, bestSite);
    		}
			this.minimumCostShift();
    	}
    	private void putBlock(Block block, Site site){
    		for(int s = 0; s < block.height; s++){
    			if(site == null){
    				System.out.println("Not enough space to put block at end of column");
    			}else{
    				site.setBlock(block);
    				this.lastUsedSite = site;
    				this.cost += site.block.cost(site.x, site.y);

        			site = site.next;
    			}
    		}
    	}
    	private void removeBlock(Block block){
    		this.usedSize -= block.height;
    	}
    	private boolean move(){
    		this.firstFreeSite = this.lastUsedSite;
    		while(this.firstFreeSite.hasBlock()){
    			this.firstFreeSite = this.firstFreeSite.previous;
    			
    			if(this.firstFreeSite == null){
    				return false;
    			}
    		}

    		Site currentSite = this.firstFreeSite;
    		while(currentSite != this.lastUsedSite){

    			this.cost -= currentSite.next.block.cost(currentSite.next.x, currentSite.next.y);
    			currentSite.block = currentSite.next.block;
    			this.cost += currentSite.block.cost(currentSite.x, currentSite.y);

    			currentSite = currentSite.next;
    		}
    		this.lastUsedSite.block = null;
    		this.lastUsedSite = this.lastUsedSite.previous;

    		return true;
    	}
    	private void revert(){
    		this.lastUsedSite = this.lastUsedSite.next;
    		Site currentSite = this.lastUsedSite;
    		while(currentSite != this.firstFreeSite){
    			this.cost -= currentSite.previous.block.cost(currentSite.previous.x, currentSite.previous.y);
    			currentSite.block = currentSite.previous.block;
    			this.cost += currentSite.block.cost(currentSite.x, currentSite.y);

    			currentSite = currentSite.previous;
    		}
    		this.firstFreeSite.block = null;
    	}
    	private void minimumCostShift(){
    		double minimumCost = this.cost;
    		while(this.move()){
    			double cost = this.cost;
    			if(cost < minimumCost){
    				minimumCost = cost;
    			}else{
    				revert();
    				return;
    			}
    		}
    	}
	}
    class Site {
    	final int x,y;
    	
    	Block block, oldBlock;
    	
    	Site previous;
    	Site next;
    	
    	public Site(int x, int y){
    		this.x = x;
    		this.y = y;
    		this.block = null;
    	}
    	boolean hasBlock(){
    		return this.block != null;
    	}
    	void setBlock(Block block){
    		this.block = block;
    	}
    	
    	void saveState(){
    		this.oldBlock = this.block;
    	}
    	void restoreState(){
    		this.block = this.oldBlock;
    	}
    }
    public static class Comparators {
    	public static Comparator<Block> VERTICAL = new Comparator<Block>() {
    		@Override
    		public int compare(Block b1, Block b2) {
    			return Double.compare(b1.vertical.coordinate, b2.vertical.coordinate);
    		}
    	};
    }
}
