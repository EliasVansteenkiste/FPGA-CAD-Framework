package place.placers.analytical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import place.util.Timer;
import place.visual.PlacementVisualizer;

class GradientLegalizer extends Legalizer {
	private List<Block> blocks;
	private List<Cluster> clusters;
	private Map<Integer, Column> columns;

    private int numLegalColumns, numIllegalColumns;
    private final double scalingFactor;
    
    private final MassMap massMap;

    private Column bestColumn;
    private double bestCost;
    
    private int iterationCounter;
    
    private final double[] visualX;
    private final double[] visualY;
    private final static boolean doVisual = false;
    
    //Timing functionality
	private Timer clusterSpreading = new Timer();
	private Timer clusterMoving = new Timer();
	private Timer blockSpreading = new Timer();
	private Timer hierarchicalSpreading = new Timer();
	private Timer spreading = new Timer();

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
    	
    	this.numLegalColumns = 0;
    	this.numIllegalColumns = 0;
    	for(BlockType blockType:this.circuit.getBlockTypes()){
    		if(blockType.getCategory().equals(BlockCategory.CLB)){
    			this.numLegalColumns += this.circuit.getColumnsPerBlockType(blockType).size();
    		}else if(blockType.getCategory().equals(BlockCategory.HARDBLOCK)){
    			this.numIllegalColumns += this.circuit.getColumnsPerBlockType(blockType).size();
    		}
    	}
    	this.scalingFactor = ((double)this.numLegalColumns) / ((double)(this.numLegalColumns + this.numIllegalColumns));

    	this.massMap = new MassMap(this.numLegalColumns, this.height);
    	
    	this.iterationCounter = 0;
    	
    	//Visualization of legalization progress
    	this.visualX = new double[this.linearX.length];
    	this.visualY = new double[this.linearX.length];
    	for(int i = 0; i < this.linearX.length; i++){
    		this.visualX[i] = -2;
    		this.visualY[i] = 0;
    	}
    }
    private void addVisual(String name, List<Block> blocks){
    	if(doVisual){
        	for(int i = 0; i < this.linearX.length; i++){
        		this.visualX[i] = -2;
        		this.visualY[i] = 0;
        	}
        	for(Block block:blocks){
        		this.visualX[block.index] = block.horizontal.coordinate;
        		this.visualY[block.index] = block.vertical.coordinate;
        	}
        	this.addVisual(name, this.visualX, this.visualY);
    	}
    }

    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	double stepSize = this.getSettingValue("step_size");
    	
    	if(this.isLastIteration){
    		Timer t = new Timer();
    		t.start();
    		
    		this.makeBlocks(blocksStart, blocksEnd, this.width, this.height);
    		this.initializeBlocks(stepSize);
    		this.sortBlocks();
    		this.initializeColumns();
    		this.legalizeBlocks();
    		this.updateLegal();
    		
    		t.stop();
    		
    		this.logger.println("\n");
    		this.logger.println("-----------------------------------");
        	this.logger.println("| Spreading              | " + String.format("%.2f s", this.spreading.getTime()) + " |");
        	this.logger.println("| Hierarchical spreading | " + String.format("%.2f s", this.hierarchicalSpreading.getTime()) + " |");
        	this.logger.println("| Cluster spreading      | " + String.format("%.2f s", this.clusterSpreading.getTime()) + " |");
        	this.logger.println("| Cluster moving         | " + String.format("%.2f s", this.clusterMoving.getTime()) + " |");
        	this.logger.println("| Block spreading        | " + String.format("%.2f s", this.blockSpreading.getTime()) + " |");
        	this.logger.println("-----------------------------------");
    		this.logger.println("| Detailed legalization  | " + String.format("%.2f s", t.getTime()) + " |");
    		this.logger.println("-----------------------------------");
    		this.logger.println("\n");
    	}else{
    		if(this.iterationCounter == 0){
        		this.makeBlocks(blocksStart, blocksEnd, this.numLegalColumns, this.height);
        		this.initializeClusters();
    		}
    		this.initializeBlocks(stepSize);
            this.doSpreading();
            this.updateLegal();
    	}
    	
    	this.iterationCounter++;
    }

    //Initialization
    private void makeBlocks(int blocksStart, int blocksEnd, int gridWidth, int gridHeight){
    	this.blocks = new ArrayList<>(blocksEnd - blocksStart);
    	for(int b = blocksStart; b < blocksEnd; b++){
    		int blockHeight = this.heights[b];
    		float offset = (1 - blockHeight) / 2f;
    		int leafNode = this.leafNode[b];
    		this.blocks.add(new Block(b, offset, blockHeight, leafNode, gridWidth, gridHeight));
    	}
    }
    private void initializeBlocks(double stepSize){
		double x,y;
    	for(Block block:this.blocks){
    		
			if(this.isLastIteration){
				double interpolation = this.getSettingValue("interpolation");
				x = (1.0 - interpolation) * this.legalX[block.index] + interpolation * this.linearX[block.index];
				y = (1.0 - interpolation) * this.legalY[block.index] + interpolation * this.linearY[block.index];
			}else{
				x = this.linearX[block.index];
				y = this.linearY[block.index];
			}

    		if(this.isLastIteration){
    			y = y + Math.ceil(block.offset);
    		}else{
    			x = x * this.scalingFactor;
    			y = y + block.offset;
    		}
    		
    		block.initialize(x, y, stepSize);
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
        this.columns = new HashMap<>();
        for(int columnIndex : this.circuit.getColumnsPerBlockType(BlockType.getBlockTypes(BlockCategory.CLB).get(0))){
        	Column column = new Column(columnIndex, this.height);
        	this.columns.put(columnIndex, column);
        }
    }
    
    public void initializeMassMap(){
    	this.initializeMassMap(this.blocks);
    }
    public void initializeMassMap(Cluster cluster){
    	this.initializeMassMap(cluster.blocks);
    }
    private void initializeMassMap(List<Block> addedBlocks){
    	this.massMap.reset();

    	for(Block block:addedBlocks){
    		this.massMap.add(block);
    	}
    }
    
    //Spreading
    private void doSpreading(){
    	this.spreading.start();
    	
    	int clusterIteration = 0;
		
    	this.initializeMassMap();
    	
    	this.massMap.printToFile("before", this.iterationCounter);
    	this.addVisual("Before spreading", this.blocks);
    	
    	double clusterScaling = this.getSettingValue("cluster_scaling");
    	int blockSpreadingIterations = this.getIntSettingValue("block_spreading");
    	
    	this.hierarchicalSpreading.start();
    	while(this.massMap.overlap() / this.blocks.size() > 0.2 && clusterIteration < 20){
    		this.massMap.reset();
    		
    		this.clusterSpreading.start();
    		this.spreadClusters(15);
    		this.addVisual("Spread clusters", this.blocks);
    		this.clusterSpreading.stop();
    		
    		this.clusterMoving.start();
    		this.moveClusters(20, clusterScaling);
    		this.addVisual("Move clusters", this.blocks);
    		this.clusterMoving.stop();
    		
    		clusterIteration++;
    	}
    	this.hierarchicalSpreading.stop();
    	
    	if(clusterIteration >= 20){
    		//this.logger.raise(clusterIteration + " hierachical cluster iterations required");
    	}
    	
    	this.blockSpreading.start();
    	this.addVisual("Before independent block spreading", blocks);
    	this.moveBlocks(blockSpreadingIterations);
    	this.addVisual("After independent block spreading", blocks);
    	this.blockSpreading.stop();
    	
    	this.massMap.printToFile("after", this.iterationCounter);
    	
    	this.spreading.stop();
    }
    public void spreadClusters(int numIterations){
    	//The mass map is empty before cluster spreading
    	for(Cluster cluster:this.clusters){
    		
    		//Add the blocks to the mass map
    		for(Block block:cluster.blocks){
    			this.massMap.add(block);
    		}
    		
    		for(int i = 0; i < numIterations; i++){
    			this.applyPushingBlockForces(cluster);
    		}
    		
    		//Remove the blocks from the mass map
    		for(Block block:cluster.blocks){
    			this.massMap.remove(block);
    		}
    	}
    }
    public void moveClusters(int numIterations, double scaleFactor){
    	//The mass map is empty before cluster moving
    	
    	//Add the blocks to the mass map
    	for(Block block:this.blocks){
    		this.massMap.add(block);
    	}
    	
    	for(int i = 0 ; i < numIterations; i++){
        	for(Cluster cluster:this.clusters){
        		cluster.horizontalForce = 0.0;
        		cluster.verticalForce = 0.0;
        		
        		for(Block block:cluster.blocks){
        			this.massMap.remove(block);
        		}
        		
        		for(Block block:cluster.blocks){
        			block.setForce(this.massMap);
        			
        			cluster.horizontalForce += block.horizontal.getForce();
        			cluster.verticalForce += block.vertical.getForce();
        		}

        		cluster.horizontalForce = this.scaleForce(cluster.horizontalForce, scaleFactor);
        		cluster.verticalForce = this.scaleForce(cluster.verticalForce, scaleFactor);

        		for(Block block:cluster.blocks){
        			block.horizontal.setForce(cluster.horizontalForce);
        			block.vertical.setForce(cluster.verticalForce);
        		}
        		
        		for(Block block:cluster.blocks){
        			block.doForce();
        		}
        		
        		for(Block block:cluster.blocks){
        			this.massMap.add(block);
        		}
        	}
    	}
    	
    	//The blocks are not removed after cluster moving, the mass
    	//map of all blocks is required to calculate the overlap
	}
    private double scaleForce(double input, double scaleFactor){
    	if(input < 0.0){
    		return -Math.pow(-input, scaleFactor);
    	}else{
    		return Math.pow(input, scaleFactor);
    	}
    }
    public void moveBlocks(int numIterations){
    	//this.initializeMassMap();
    	this.applyPushingBlockForces(numIterations);
    }
    
    private void applyPushingBlockForces(int numIterations){
    	for(int i = 0; i < numIterations; i++){
    		for(Block block: this.blocks){
    			this.massMap.remove(block);
        		block.setForce(this.massMap);
        		block.doForce();
        		this.massMap.add(block);
        	}
    	}
    }
    private void applyPushingBlockForces(Cluster cluster){
    	for(Block block: cluster.blocks){
    		this.massMap.remove(block);
    		block.setForce(this.massMap);
    		block.doForce();
    		this.massMap.add(block);
    	}
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
    		if(this.blocks.contains(block)){
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

    	public Block(int index, float offset, int height, int leafNode, int gridWidth, int gridHeight){
    		this.index = index;

    		this.offset = offset;
    		this.height = height;

    		this.horizontal = new Dimension(gridWidth);
    		this.vertical = new Dimension(gridHeight - this.height + 1);
    		
    		this.leafNode = leafNode;
    	}
    	void initialize(double horizontalCoordinate, double verticalCoordinate, double stepSize){
    		this.horizontal.initialize(horizontalCoordinate, stepSize);
    		this.vertical.initialize(verticalCoordinate, stepSize);
    		
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
        	this.ceilx = (int) Math.ceil(2.0 * this.horizontal.coordinate);
        	this.ceily = (int) Math.ceil(2.0 * this.vertical.coordinate);
        	
    		double xLeft  = (0.5 * this.ceilx) - this.horizontal.coordinate;
    		double xRight = 0.5 - xLeft;

    		double yLeft  = (0.5 * this.ceily) - this.vertical.coordinate;
    		double yRight = 0.5 - yLeft;

    		this.area_sw = xLeft  * yLeft;
    		this.area_nw = xLeft  * yRight;
    		this.area_se = xRight * yLeft;
    		this.area_ne = xRight * yRight;
    	}
    	double cost(int legalX, int legalY){
    		double horizontalDistance = Math.abs(this.horizontal.coordinate - legalX);
    		double verticalDistance = Math.abs(this.vertical.coordinate - legalY);
    		return horizontalDistance + verticalDistance;
    	}
    	double horizontalCost(int legalX){
    		double horizontalDistance = Math.abs(this.horizontal.coordinate - legalX);
    		return horizontalDistance;
    	}
    }
    class Dimension {
    	private double coordinate;
    	private double force;
    	private double stepSize;
    	
    	private final int size;

    	Dimension(int size){
    		this.size = size;
    	}
    	void initialize(double coordinate, double stepSize){
    		this.coordinate = coordinate;
    		this.force = 0.0;
    		
    		this.stepSize = stepSize;
    	}
    	void setForce(double force){
    		this.force = force;
    	}
    	double getForce(){
    		return this.force;
    	}
    	void solve(){
    		if(this.force != 0.0){
            	this.coordinate += this.stepSize * this.force;
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
        public void printToFile(String name, int iteration){
        	if(doVisual){
        		if(iteration == 1){
            		System.out.println("Print massmap to file: /Users/drvercru/Desktop/massmap/" + name + ".txt");
                	try{
                    	File file = new File("/Users/drvercru/Documents/Doctoraat/Papers/FPL2018/figures/massmap/" + name + ".txt");
                        FileWriter fw = new FileWriter(file.getAbsoluteFile());
                        BufferedWriter bw = new BufferedWriter(fw);
                        
                        int hor = this.massMap.length;
                        int ver = this.massMap[0].length;
                        
                        bw.write("Dimensions: " + hor + " x " + ver + "\n\n");
                        
                        // Write in file
                        for(int i = 0; i < hor; i++){
                        	for(int j = 0; j < ver; j++){
                        		if(j == (ver - 1)){
                        			bw.write(String.format("%.2f", this.massMap[i][j]));
                        		}else{
                        			bw.write(String.format("%.2f;", this.massMap[i][j]));
                        		}
                        		
                        	}
                        	bw.write("\n");
                        }
                        
                        // Close connection
                        bw.close();
                    }catch(Exception e){
                    	System.out.println(e);
                	}
            	}
    		}
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

            double horizontalForce = block.force_sw + block.force_nw - block.force_se - block.force_ne;
            double verticalForce = block.force_sw - block.force_nw + block.force_se - block.force_ne;
            	
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
    		this.bestColumn = null;
    		this.bestCost = Double.MAX_VALUE;
    		
    		int index = (int) Math.round(block.horizontal.coordinate);
    		
    		this.tryColumn(index, block);
    		for(int moveIndex = 1; moveIndex <= this.width; moveIndex++){
    			this.tryColumn(index - moveIndex, block);
    			this.tryColumn(index + moveIndex, block);
    		}
    		
    		this.bestColumn.addBlock(block);
    	}
    	
    	//Update block coordinates
    	for(Block block:this.blocks){
    		block.processed = false;
    	}
    	for(Column column:this.columns.values()){
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
    private void tryColumn(int columnIndex, Block block){
    	if(this.columns.containsKey(columnIndex)){
			double naiveCost = block.horizontalCost(columnIndex);
			if(naiveCost < this.bestCost){
				Column column = this.columns.get(columnIndex);
				if(column.usedSize + block.height <= column.height){
					double cost = column.tryBlock(block);
					
					if(cost < this.bestCost){
						this.bestColumn = column;
						this.bestCost = cost;
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
