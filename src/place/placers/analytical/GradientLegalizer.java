package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private List<Block> blocks;
	private List<Cluster> clusters;
	private List<Column> columns;

    private int legalColumns, illegalColumns;
    private final double scalingFactor;
    
    private final MassMap massMap;
    
    private static final boolean doVisual = false;

    GradientLegalizer(
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
    	double stepSize = this.getSettingValue("step_size");
    	double speedAveraging = this.getSettingValue("speed_averaging");

    	if(this.isLastIteration){
    		this.initializeBlocks(blocksStart, blocksEnd, this.width, this.height, stepSize, speedAveraging);
    		this.sortBlocks();
    		this.initializeColumns();
    		this.legalizeBlocks();
    		this.updateLegal();
    	}else{
    		this.initializeBlocks(blocksStart, blocksEnd, this.legalColumns, this.height, stepSize, speedAveraging);
    		this.initializeClusters();
            this.doSpreading();
            this.updateLegal();
    	}
    }

    //Initialization
    private void initializeBlocks(int blocksStart, int blocksEnd, int gridWidth, int gridHeight, double stepSize, double speedAveraging){
    	this.blocks = new ArrayList<>(blocksEnd - blocksStart);
    	for(int b = blocksStart; b < blocksEnd; b++){
			double x = this.linearX[b];
			double y = this.linearY[b];

    		int blockHeight = this.heights[b];
    		float offset = (1 - blockHeight) / 2f;

    		if(this.isLastIteration){
    			y = y + Math.ceil(offset);
    		}else{
    			x = x * this.scalingFactor;
    			y = y + offset;
    		}

    		this.blocks.add(new Block(b, x, y, offset, blockHeight, this.leafNode[b], stepSize, speedAveraging, gridWidth, gridHeight));
    	}
    }
    private void initializeClusters(){
    	boolean hasFloatingBlocks = false;
    	Set<Integer> clusterIndex = new HashSet<>();
    	for(int ln:this.leafNode){
    		clusterIndex.add(ln);
    		
    		if(ln == 0){
    			hasFloatingBlocks = true;
    		}
    	}
    	
    	this.clusters = new ArrayList<Cluster>();
    	for(int i = 0; i < clusterIndex.size(); i++){
    		this.clusters.add(new Cluster(i));
    	}

    	for(Block lb:this.blocks){
    		if(hasFloatingBlocks){
    			this.clusters.get(lb.leafNode).addBlock(lb);
    		}else{
    			this.clusters.get(lb.leafNode - 1).addBlock(lb);
    		}
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
    public void initializeMassMap(Cluster cluster){
    	this.massMap.reset();

    	for(Block block:cluster.blocks){
    		this.massMap.add(block);
    	}
    }

    //Spreading
    private void doSpreading(){
    	int iteration = 0;
    	this.initializeMassMap();
    	while(this.massMap.overlap() / this.blocks.size() > 0.33){
    		this.spreadClusters(25);
    		if(doVisual) this.addVisual(iteration);
    		
    		this.moveClusters(25);
    		if(doVisual) this.addVisual(iteration);
    		
    		iteration++;
    		
    		this.initializeMassMap();
    	}
    	this.moveBlocks(200);
    	if(doVisual) this.addVisual("final");
    }
    public void spreadClusters(int numIterations){
		for(Cluster cluster:this.clusters){
    		this.initializeMassMap(cluster);
    		
    		if(doVisual) this.addVisual(cluster);
    		
    		for(int i = 0; i < numIterations; i++){
    			this.applyPushingBlockForces(cluster);
    			
    			if(doVisual) this.addVisual(cluster);
    		}
    	}
    }
    public void moveClusters(int numIterations){
    	for(int i = 0 ; i < numIterations; i++){
        	this.initializeMassMap();
        	
        	for(Cluster cluster:this.clusters){
        		cluster.horizontalForce = 0.0;
        		cluster.verticalForce = 0.0;
        		
        		for(Block block:cluster.blocks){
        			block.setForce(this.massMap);
        			
        			cluster.horizontalForce += block.horizontal.force;
        			cluster.verticalForce += block.vertical.force;
        		}
        		
        		for(Block block:cluster.blocks){
        			block.horizontal.force = cluster.horizontalForce;
        			block.vertical.force = cluster.verticalForce;
        		}
        		
        		for(Block block:cluster.blocks){
        			this.massMap.remove(block);
        			block.doForce();
        			this.massMap.add(block);
        		}
        	}
    	}
	}
    public void moveBlocks(int numIterations){
    	this.initializeMassMap();
    	for(int i = 0; i < numIterations; i++){
    		this.applyPushingBlockForces();
    	}
    }
    private void applyPushingBlockForces(){
    	for(Block block: this.blocks){
    		this.massMap.setForce(block);
    		this.massMap.remove(block);
    		block.doForce();
    		this.massMap.add(block);
    	}
    }
    private void applyPushingBlockForces(Cluster cluster){
    	for(Block block: cluster.blocks){
    		block.setForce(this.massMap);
    		this.massMap.remove(block);
    		block.doForce();
    		this.massMap.add(block);
    	}
    }
    
    //Visual
    private void addVisual(Cluster cluster){
    	double[] coorX = new double[this.linearX.length];
		double[] coorY = new double[this.linearY.length];
		
		for(int i = 0; i < this.linearX.length; i++){
			coorX[i] = -1;
			coorY[i] = -1;
		}
		for(Block block:cluster.blocks){
			coorX[block.index] = block.horizontal.coordinate;
			coorY[block.index] = block.vertical.coordinate;
		}
		this.addVisual("Cluster: " + cluster.id, coorX, coorY);
    }
    private void addVisual(String name){
    	double[] coorX = new double[this.linearX.length];
		double[] coorY = new double[this.linearY.length];
		
		for(int i = 0; i < this.linearX.length; i++){
			coorX[i] = -1;
			coorY[i] = -1;
		}
		for(Block block:this.blocks){
			coorX[block.index] = block.horizontal.coordinate;
			coorY[block.index] = block.vertical.coordinate;
		}
		this.addVisual(name, coorX, coorY);
    }
    private void addVisual(int i){
    	this.addVisual("" + i);
    }
    
    //Finalize
    private void updateLegal(){
    	for(Block block:this.blocks){
    		double x = block.horizontal.coordinate;
    		double y = block.vertical.coordinate;

    		if(this.isLastIteration){
    			y = y + Math.floor(-block.offset);
    		}else{
    			x = x / this.scalingFactor;
    			y = y - block.offset;
    		}

    		this.legalX[block.index] = x;
    		this.legalY[block.index] = y;
    	}
    }

    class Cluster {
    	final int id;
    	final List<Block> blocks;
    	
    	double horizontalForce;
    	double verticalForce;

    	Cluster(int id){
    		this.id = id;
    		this.blocks = new ArrayList<>();
    	}
    	
    	void addBlock(Block block){
    		if(this.blocks.contains(block)){//TODO REMOVE THIS DEBUG LINE
    			System.out.println("Duplicate block in cluster!");
    		}else{
    			this.blocks.add(block);
    		}
    	}
    }
    
    public class Block {
    	final int index;

    	final Dimension horizontal, vertical;

    	final float offset;
    	final int height;
    	
    	final int leafNode;
    	
    	int ceilx, ceily;

    	double area_ne, area_nw, area_se, area_sw;
    	double force_ne, force_nw, force_se, force_sw;
    	
    	boolean processed;

    	public Block(int index, double x, double y, float offset, int height, int leafNode, double stepSize, double speedAveraging, int gridWidth, int gridHeight){
    		this.index = index;

    		this.offset = offset;
    		this.height = height;

    		this.horizontal = new Dimension(x, stepSize, speedAveraging, gridWidth);
    		this.vertical = new Dimension(y, stepSize, speedAveraging, gridHeight - this.height + 1);
    		
    		this.leafNode = leafNode;
    		
    		this.area_ne = 0.0;
    		this.area_nw = 0.0;
    		this.area_se = 0.0;
    		this.area_sw = 0.0;
    		
    		this.force_ne = 0.0;
    		this.force_nw = 0.0;
    		this.force_se = 0.0;
    		this.force_sw = 0.0;
    		
    		this.trim();
    		this.update();
    	}
    	void setForce(MassMap massMap){
    		massMap.setForce(this);
    	}
        void doForce(){
        	this.solve();
    		this.update();
        }
        void trim(){
    		this.horizontal.trim();
    		this.vertical.trim();
        }
        void solve(){
        	this.horizontal.solve();
        	this.vertical.solve();
        }
    	void update(){
        	this.ceilx = (int)Math.ceil(2 * this.horizontal.coordinate);
        	this.ceily = (int)Math.ceil(2 * this.vertical.coordinate);
        	
    		double xLeft = (0.5 * this.ceilx) - this.horizontal.coordinate;
    		double xRight = 0.5 - xLeft;

    		double yLeft = (0.5 * this.ceily) - this.vertical.coordinate;
    		double yRight = 0.5 - yLeft;

    		this.area_sw = xLeft * yLeft;
    		this.area_nw = xLeft * yRight;
    		this.area_se = xRight * yLeft;
    		this.area_ne = xRight * yRight;
    	}
    	double cost(int legalX, int legalY){
    		double horizontalDistance = this.horizontal.coordinate - legalX;
    		double verticalDistance = this.vertical.coordinate - legalY;
    		return Math.sqrt((horizontalDistance * horizontalDistance) + (verticalDistance * verticalDistance));
    	}
    }
    class Dimension {
    	double coordinate;
    	double force;
    	double speed;
    	double stepSize, speedAveraging, size;

    	Dimension(double coordinate,  double stepSize, double speedAveraging, int size){
    		this.coordinate = coordinate;
    		this.force = 0.0;
    		this.speed = 0.0;
    		
    		this.size = size;

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
            	
            	this.trim();
    		}
    	}
    	void trim(){
    		if(this.coordinate > this.size) this.coordinate = this.size;
    		if(this.coordinate < 1) this.coordinate = 1;
    	}
    }

    class MassMap {
    	private final int gridWidth, gridHeight;
        private final double[][] massMap;
        
        public MassMap(int width, int height){
        	this.gridWidth = (width + 2) * 2;
        	this.gridHeight = (height + 2) * 2;

        	this.massMap = new double[this.gridWidth][this.gridHeight];
        	
        	this.reset();
        }
        public void reset(){
        	for(int x = 0; x < this.gridWidth; x++){
        		for(int y = 0; y < this.gridHeight; y++){
        			this.massMap[x][y] = 0.0;
        		}
        	}
        }
        public double overlap(){
        	double overlap = 0.0;
        	for(int i = 0; i < this.gridWidth; i++){
        		for(int j = 0; j < this.gridHeight; j++){
        			if(this.massMap[i][j] > 0.25){
        				overlap += this.massMap[i][j] - 0.25;
        			}
        		}
        	}
        	return overlap;
        }
        public double usedRegion(){
        	double usedRegion = 0.0;
        	for(int i = 0; i < this.gridWidth; i++){
        		for(int j = 0; j < this.gridHeight; j++){
        			if(this.massMap[i][j] > 0.0){
        				usedRegion += 0.25;
        			}
        		}
        	}
        	return usedRegion;
        }
        public void setForce(Block block){
    		this.setPushingForce(block);

    		double mass = block.force_sw + block.force_nw + block.force_se + block.force_ne;

        	double horizontalForce = (block.force_sw + block.force_nw - block.force_se - block.force_ne) / mass;
        	double verticalForce = (block.force_sw - block.force_nw + block.force_se - block.force_ne) / mass;
        	
        	block.horizontal.setForce(horizontalForce);
        	block.vertical.setForce(verticalForce);
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
        public void remove(Block block){
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
    
    private void legalizeBlocks(){
    	for(Block block:this.blocks){
    		Column bestColumn = null;
    		double bestCost = Double.MAX_VALUE;

    		for(Column column:this.columns){
    			if(Math.abs(column.index - block.horizontal.coordinate) < 15){
	    			if(column.usedSize + block.height <= column.height){
	        			double cost = column.tryBlock(block);
	        			
	        			if(cost < bestCost){
	        				bestColumn = column;
	        				bestCost = cost;
	        			}
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
