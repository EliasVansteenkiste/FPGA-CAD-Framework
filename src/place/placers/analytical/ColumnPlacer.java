package place.placers.analytical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import place.circuit.block.GlobalBlock;

import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Column;
import place.placers.analytical.HardblockConnectionLegalizer.Crit;
import place.placers.analytical.HardblockConnectionLegalizer.Net;
import place.placers.analytical.HardblockConnectionLegalizer.Site;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

public class ColumnPlacer {
	private Block[] allBlocks;

	private Column[] columns;
	private Block[] blocks;
	private Net[] nets;
	private List<Crit> critConns;
	
	private int columnHeight, blockHeight;

	private int blocksStart, blocksEnd;

	private final int halfMaxConnectionLength;
	private double stepSize, speedAveraging;

	final double[] coordinate, speed;

	final double[] direction;
	final double[] totalPositiveNetSize, totalNegativeNetSize;
	final int[] numPositiveNets, numNegativeNets;

	int minCoordinate, maxCoordinate;

	private TimingTree timing;
	
	private int numColumns;
	private final List<double[]> massMap;
	final double[] areaNorth, areaSouth;
	final double[] force;

    private final boolean doVisual;
    private final PlacementVisualizer visualizer;
    private final Map<GlobalBlock, NetBlock> netBlocks;
    private final double[] visualX, visualY;

	ColumnPlacer(
			TimingTree timingTree, 
			Block[] allBlocks,
			int height,
			PlacementVisualizer visualizer,
			Map<GlobalBlock, NetBlock> blockIndexes,
			Boolean visual){

		this.timing = timingTree;

		this.allBlocks = allBlocks;

		this.halfMaxConnectionLength = 15;
		this.stepSize = 0.25;
		this.speedAveraging = 0.2;
		
		this.columnHeight = height;

		int numBlocks = this.allBlocks.length;

		this.coordinate = new double[numBlocks];

		this.speed = new double[numBlocks];

		this.direction = new double[numBlocks];
		this.totalPositiveNetSize = new double[numBlocks];
		this.totalNegativeNetSize = new double[numBlocks];
		this.numPositiveNets = new int[numBlocks];
		this.numNegativeNets = new int[numBlocks];

		this.massMap = new ArrayList<>();
		this.areaNorth = new double[numBlocks];
		this.areaSouth = new double[numBlocks];
		this.force = new double[numBlocks];

		this.doVisual = visual;
		this.visualizer = visualizer;
		this.netBlocks = blockIndexes;
		this.visualX = new double[numBlocks];
		this.visualY = new double[numBlocks];
	}
	
	public void doPlacement(Column[] columns, Block[] blocks, Net[] nets, List<Crit> critConns, int blockHeight){
		this.timing.start("Column placer");

		this.columns = columns;
		this.blocks = blocks;
		this.nets = nets;
		this.critConns = critConns;

		this.blockHeight = blockHeight;
		this.minCoordinate = 1;
		this.maxCoordinate = this.columnHeight - this.blockHeight;

		this.numColumns = this.columns.length;
		this.massMap.clear();
		for(int c = 0; c < this.numColumns; c++){
			this.massMap.add(new double[(this.columnHeight + 2) * 2]);
		}

		this.blocksStart = blocks[0].index;
		this.blocksEnd = blocksStart + blocks.length;

		this.initializeData();

		this.timing.start("Connection forces");
		for(int iteration = 0; iteration < 10; iteration++){
			this.initializeConnectionData();
			//SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW
			this.processNets();
			//SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW
			this.doConnectionForce();

			//this.addVisual("Connection iteration " + iteration);
		}
		this.timing.time("Connection forces");
		
		this.addVisual("before legalize");
		

		//this.pushingLegalizer();
		//this.clusterLegalizer();
		//this.priorityLegalizer();
		this.minimumConnectionCost();


		this.addVisual("Legalized slow solution");

		this.updateData();

		this.timing.time("Column placer");
	}
	boolean overlap(Cluster firstCluster){
		Cluster cluster = firstCluster;
		do{
			if(cluster.overlap()){
    			return true;
    		}	
			cluster = cluster.next;
		}while(cluster != null);
		return false;
	}
	
	
	
	
	
	
	
	
	
	private void pushingLegalizer(){
		this.timing.start("Pushing forces");
		Arrays.fill(this.speed, 0.0);
		for(int iteration = 0; iteration < 100; iteration++){
        	this.applyPushingForces(0.002);//0.002
        	//this.addVisual("Push overlap " + iteration);
        }
		this.timing.time("Pushing forces");
	}
    private void applyPushingForces(double gridForce){
    	//this.timing.start("Apply Pushing Forces");
    	
    	this.updateArea();
    	this.initializeMassMap();
		this.fpgaPushForces(gridForce);
		this.doOverlapForce();
		
		//this.timing.time("Apply Pushing Forces");
    }
    private void updateArea(){
    	//this.timing.start("Update area");
    	
    	for(Block block:this.blocks){
    		this.areaSouth[block.index] = (Math.ceil(this.coordinate[block.index] * 2.0) * 0.5) - this.coordinate[block.index];
    		this.areaNorth[block.index] = 0.5 - this.areaSouth[block.index];
    	}
    	
    	//this.timing.time("Update area");
    }
    private void initializeMassMap(){
    	//this.timing.start("Initialize Mass Map");

    	for(int column = 0; column < this.numColumns; column++){
        	double[] columnMap = this.massMap.get(column);
    		for(int i = 0; i < columnMap.length; i++){
        		columnMap[i] = 0.0;
        	}
    	}

    	for(Block block:this.blocks){
    		double[] columnMap = this.massMap.get(block.column.index);
    		int i = (int)Math.ceil(this.coordinate[block.index] * 2.0);

    		for(int h = 0; h < 2 * this.blockHeight; h++){
        		columnMap[i + h - 1] += this.areaSouth[block.index];
        		columnMap[i + h] += this.areaNorth[block.index];
    		}
    	}
 
    	//this.timing.time("Initialize Mass Map");
    }
    private void fpgaPushForces(double gridForce){
    	//this.timing.start("Gravity Push Forces");
    	
    	for(Block block:this.blocks){
    		
    		double north = 0.0;
    		double south = 0.0;

    		double[] columnMap = this.massMap.get(block.column.index);
    		int i = (int)Math.ceil(this.coordinate[block.index] * 2.0);

    		for(int h = 0; h < this.blockHeight; h++){
        		south += this.areaSouth[block.index] * columnMap[i + h - 1];
        		south += this.areaNorth[block.index] * columnMap[i + h];
    		}
    		for(int h = this.blockHeight; h < 2 * this.blockHeight; h++){
    			north += this.areaSouth[block.index] * columnMap[i + h - 1];
        		north += this.areaNorth[block.index] * columnMap[i + h];
    		}
    		
    		double grid = gridForce * (Math.round(this.coordinate[block.index]) - this.coordinate[block.index]);
	    	this.force[block.index] = (south - north + grid) / (south + north + grid);
	    	
 	    	//System.out.println(this.areaSouth[block.index] + " " + this.areaNorth[block.index] + " " + north + " " + south + " " + grid + " " + this.force[block.index]);
    	}   
    	
    	//this.timing.time("Gravity Push Forces");
    }
    private void doOverlapForce(){
    	//this.timing.start("Solve");

    	for(Block block:this.blocks){
    		
    		if(this.force[block.index] != 0.0){
    			double newSpeed = this.force[block.index];

            	this.speed[block.index] = this.speedAveraging * this.speed[block.index] + (1 - this.speedAveraging) * newSpeed;
            	
            	this.coordinate[block.index] += this.speed[block.index];
    		}

        	if(this.coordinate[block.index] < this.minCoordinate) this.coordinate[block.index] = this.minCoordinate;
        	if(this.coordinate[block.index] > this.maxCoordinate) this.coordinate[block.index] = this.maxCoordinate;
    	}

    	//this.timing.time("Solve");
    }
	
	
	
	
    
    
    
    
    
    
    
    
    private void clusterLegalizer(){
		for(Column column:this.columns){
			//SORT THE BLOCKS FROM SMALL TO LARGE
			if(column.blocks.size() > 0){
				Set<Block> remainingBlocks = new HashSet<Block>();
				for(Block block:column.blocks){
					remainingBlocks.add(block);
				}
				List<Block> sortedBlocks = new ArrayList<Block>();
				while(!remainingBlocks.isEmpty()){
					Block bestBlock = null;
					for(Block block:remainingBlocks){
						if(bestBlock == null){
							bestBlock = block;
						}else if(this.coordinate[block.index] < this.coordinate[bestBlock.index]){
							bestBlock = block;
						}
					}
					sortedBlocks.add(bestBlock);
					remainingBlocks.remove(bestBlock);
				}
				
				int numRows = (int) Math.floor(this.columnHeight / this.blockHeight);
				int firstRow = 1;
				int rowRepeat = this.blockHeight;
				for(Block block:sortedBlocks){
					int rowIndex = (int) Math.round(Math.max(Math.min((this.coordinate[block.index] - firstRow) / rowRepeat, numRows - 1), 0));
					block.legalY = firstRow + rowIndex * rowRepeat;
					block.linearY = this.coordinate[block.index];
				}
				
				this.addVisual("after legalize");
				
		    	//Make the chain of clusters
				Cluster firstCluster = null;
				Cluster previousCluster = null;
				for(Block block:sortedBlocks){
					Cluster cluster = new Cluster(block, this.blockHeight, this.columnHeight);
					
					if(previousCluster == null){
						firstCluster = cluster;
					}else{
			    		cluster.previous = previousCluster;
			    		previousCluster.next = cluster;
					}
					previousCluster = cluster;
				}
		    	
		    	while(this.overlap(firstCluster)){
		        	Cluster cluster = firstCluster;
		        	do{
		        		cluster.absorbNeighbour();
		        		
		        		cluster = cluster.next;
		        	}while(cluster != null);
		    	}
		    	
				for(Block block:sortedBlocks){
					this.coordinate[block.index] = block.legalY;
				}
			}
		}
    }
	
	
	
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
	
	
    private void minimumConnectionCost(){
    	this.timing.start("Minimum connection cost legalizer");
    	
    	for(Block block:this.blocks){
    		block.legalY = (int) Math.round(this.coordinate[block.index]);
    	}

    	for(Net net:this.nets){
    		net.initializeConnectionCost();
    	}
    	for(Block block:this.blocks){
    		block.initializeConnectionCost();
    	}


		Set<Block> remainingBlocks = new HashSet<Block>();
		
		for(Block block:this.blocks){
			remainingBlocks.add(block);
		}
		while(!remainingBlocks.isEmpty()){
			Block largestCriticalityBlock = null;
			
			for(Block block:remainingBlocks){
				if(largestCriticalityBlock == null){
					largestCriticalityBlock = block;
				}else if(block.criticality() > largestCriticalityBlock.criticality()){
					largestCriticalityBlock = block;
				}
			}

			int origX = largestCriticalityBlock.legalX;
			int origY = largestCriticalityBlock.legalY;

			Site bestSite = null;
			double minimumConnectionCost = Double.MAX_VALUE;

			//Best site based on connectivity!!!
			for(Site trySite:largestCriticalityBlock.column.sites){
				if(!trySite.hasBlock()){

					if(trySite.column != largestCriticalityBlock.legalX){
						System.out.println("Problem");
					}

					largestCriticalityBlock.legalY = trySite.row;
					double connectionCost = largestCriticalityBlock.connectionCost(origX, origX, origY, trySite.row);
					largestCriticalityBlock.legalY = origY;

					if(connectionCost < minimumConnectionCost){
						bestSite = trySite;
						minimumConnectionCost = connectionCost;
					}
				}
			}
			bestSite.setBlock(largestCriticalityBlock);
			largestCriticalityBlock.setSite(bestSite);
			largestCriticalityBlock.updateConnectionCost(origX, origX, origY, bestSite.row);
			remainingBlocks.remove(largestCriticalityBlock);
			largestCriticalityBlock.legalY = bestSite.row;
			this.coordinate[largestCriticalityBlock.index] = bestSite.row;
		}
		this.timing.time("Minimum connection cost legalizer");
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private void priorityLegalizer(){
		//SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW
		Set<Block> remainingBlocks = new HashSet<Block>();
		
		for(Block block:this.blocks){
			remainingBlocks.add(block);
		}
		while(!remainingBlocks.isEmpty()){
			Block largestCriticalityBlock = null;
			
			for(Block block:remainingBlocks){
				if(largestCriticalityBlock == null){
					largestCriticalityBlock = block;
				}else if(block.criticality() > largestCriticalityBlock.criticality()){
					largestCriticalityBlock = block;
				}
			}
			
			Site bestSite = null;
			double minimumManhattanDistance = Double.MAX_VALUE;
			
			//Best site based on connectivity!!!
			for(Site site:largestCriticalityBlock.column.sites){
				if(!site.hasBlock()){
					
					double manhattanDistance = (site.column - largestCriticalityBlock.legalX) * (site.column - largestCriticalityBlock.legalX) 
							+ (site.row - this.coordinate[largestCriticalityBlock.index]) * (site.row - this.coordinate[largestCriticalityBlock.index]);
					
					if(bestSite == null){
						bestSite = site;
						minimumManhattanDistance = manhattanDistance;
					}else{
						if(manhattanDistance < minimumManhattanDistance){
							bestSite = site;
							minimumManhattanDistance = manhattanDistance;
						}
					}
				}
			}
			bestSite.setBlock(largestCriticalityBlock);
			largestCriticalityBlock.setSite(bestSite);
			remainingBlocks.remove(largestCriticalityBlock);
			this.coordinate[largestCriticalityBlock.index] = bestSite.row;
		}
		//SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW SLOW
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
	
	
	private void initializeData(){
		Arrays.fill(this.coordinate, 0.0);
		Arrays.fill(this.speed, 0.0);
		
		Arrays.fill(this.direction, 0.0);
		Arrays.fill(this.totalPositiveNetSize, 0.0);
		Arrays.fill(this.totalNegativeNetSize, 0.0);
		Arrays.fill(this.numPositiveNets, 0);
		Arrays.fill(this.numNegativeNets, 0);
		
		Arrays.fill(this.areaNorth, 0.0);
		Arrays.fill(this.areaSouth, 0.0);
		
		for(Block block:this.allBlocks){
			this.coordinate[block.index] = block.legalY + block.offset;
		}
		for(Block block:this.blocks){
			this.coordinate[block.index] = block.linearY + block.offset;
		}
	}
	private void updateData(){
		for(Block block:this.blocks){
			block.legalY = (int)Math.round(this.coordinate[block.index] - block.offset);
		}
	}
	private void initializeConnectionData(){
		//this.timing.start("Initialize connection data");
		Arrays.fill(this.direction, this.blocksStart, this.blocksEnd, 0.0);
		Arrays.fill(this.totalPositiveNetSize, this.blocksStart, this.blocksEnd, 0.0);
		Arrays.fill(this.totalNegativeNetSize, this.blocksStart, this.blocksEnd, 0.0);
		Arrays.fill(this.numPositiveNets, this.blocksStart, this.blocksEnd, 0);
		Arrays.fill(this.numNegativeNets, this.blocksStart, this.blocksEnd, 0);
		//this.timing.time("Initialize connection data");
	}
	private void processNets(){
		//this.timing.start("Process nets");
		for(Net net:this.nets){
			this.processNet(net);
		}
		//this.timing.time("Process nets");
		//this.timing.start("Crit conns");
        for(Crit critConn:this.critConns){
        	this.processConnection(critConn);
        }
        //this.timing.time("Crit conns");
	}
	private void processNet(Net net){
        int numNetBlocks = net.blocks.size();
        
        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
        	Block block1 = net.blocks.get(0);
        	Block block2 = net.blocks.get(1);
     
        	if(this.coordinate[block1.index] < this.coordinate[block2.index]){
        		this.addConnection(block1, block2, net.weight);
        	}else{
        		this.addConnection(block2, block1, net.weight);
        	}
        // For bigger nets, we have to find the min and max block
        }else{
            Block minBlock = net.blocks.get(0);
            Block maxBlock = net.blocks.get(0);
            double minCoordinate = this.coordinate[minBlock.index];
            double maxCoordinate = this.coordinate[maxBlock.index];
            
            for(int i = 1; i < net.blocks.size(); i++){
            	Block block = net.blocks.get(i);
            	double coordinate = this.coordinate[block.index];
            	
            	if(coordinate < minCoordinate){
            		minCoordinate = coordinate;
            		minBlock = block;
            	}else if(coordinate > maxCoordinate){
            		maxCoordinate = coordinate;
            		maxBlock = block;
            	}
            }
            this.addConnection(minBlock, maxBlock, net.weight);
        }
	}
	private void processConnection(Crit conn){
    	Block block1 = conn.sourceBlock;
    	Block block2 = conn.sinkBlock;
 
    	if(this.coordinate[block1.index] < this.coordinate[block2.index]){
    		this.addConnection(block1, block2, conn.weight);
    	}else{
    		this.addConnection(block2, block1, conn.weight);
    	}
	}
	private void addConnection(Block minBlock, Block maxBlock, double weight){
		int minBlockIndex = minBlock.index;
		int maxBlockIndex = maxBlock.index;
		
		double coorDifference = this.coordinate[maxBlockIndex] - this.coordinate[minBlockIndex];
        double netSize = 2 * this.halfMaxConnectionLength * coorDifference / (this.halfMaxConnectionLength + coorDifference);
        
        this.totalPositiveNetSize[minBlockIndex] += weight * netSize;
        this.numPositiveNets[minBlockIndex] += weight;
        this.direction[minBlockIndex] += weight;

        this.totalNegativeNetSize[maxBlockIndex] += weight * netSize;
        this.numNegativeNets[maxBlockIndex] += weight;
        this.direction[maxBlockIndex] -= weight;
	}
	private void doConnectionForce(){
		//this.timing.start("Do connection force");
		for(Block block:this.blocks){
			this.doConnectionForce(block);
		}
		//this.timing.time("Do connection force");
	}
	private void doConnectionForce(Block block){
    	double netGoal = this.coordinate[block.index];
    	if(this.direction[block.index] > 0) {
    		netGoal += this.totalPositiveNetSize[block.index] / this.numPositiveNets[block.index];
    	} else if(this.direction[block.index] < 0) {
    		netGoal -= this.totalNegativeNetSize[block.index] / this.numNegativeNets[block.index];
    	} else {
    		return;
    	}

    	double newSpeed = this.stepSize * (netGoal - this.coordinate[block.index]);
    	this.speed[block.index] = this.speedAveraging * this.speed[block.index] + (1 - this.speedAveraging) * newSpeed;
    	this.coordinate[block.index] += this.speed[block.index];
    	
    	if(this.coordinate[block.index] < this.minCoordinate) this.coordinate[block.index] = this.minCoordinate;
    	if(this.coordinate[block.index] > this.maxCoordinate) this.coordinate[block.index] = this.maxCoordinate;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
    protected void addVisual(String name){
    	if(doVisual){
        	for(Block block:this.allBlocks){
        		this.visualX[block.index] = block.legalX;
        		this.visualY[block.index] = this.coordinate[block.index] - block.offset;
        	}
        	this.visualizer.addPlacement(name, this.netBlocks, this.visualX, this.visualY, -1);
    	}
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    class Cluster {
    	private Cluster previous;
    	private Cluster next;
    	
    	private int left, right;
    	
    	private final List<Block> blocks;
    	
    	private final int blockSize, gridSize;
    	
    	Cluster(Block block, int blockSize, int gridSize){
    		this.blocks = new ArrayList<Block>();
    		this.blocks.add(block);
    		
    		this.blockSize = blockSize;
    		this.gridSize = gridSize;

    		this.left = block.legalY;
    		this.right = block.legalY + this.blockSize;

    		this.previous = null;
    		this.next = null;
    	}
    	void absorbNeighbour(){
    		if(this.next != null){
            	if(this.right > this.next.left){
        			Cluster c1 = this;
        			Cluster c2 = this.next;
        			
        			for(Block block:c2.blocks){
        				block.legalY = this.right;
        				c1.blocks.add(block);
        				this.right += this.blockSize;
        			}
        			
            		if(this.next.next != null){
            			Cluster c3 = this.next.next;
            			c1.next = c3;
            			c3.previous = c1;
            		}else{
            			c1.next = null;
            		}
            		
            		c2 = null;
        			this.minimumCostShift();
        			
        			this.absorbNeighbour();
    			}
    		}
    	}
    	
    	private void minimumCostShift(){
    		this.fitInColumn();
    		
    		double cost = this.displacementCost();
    		while(true){
        		this.decrease();
        		double localCost = this.displacementCost();
        		if(localCost < cost){
        			cost = localCost;
        		}else{
        			this.increase();
        			break;
        		}
    		}
    		while(true){
        		this.increase();
        		double localCost = this.displacementCost();
        		if(localCost < cost){
        			cost = localCost;
        		}else{
        			this.decrease();
        			break;
        		}
    		}
    		
    		this.fitInColumn();
    	}
    	void fitInColumn(){
    		while(this.right > this.gridSize + 1){
    			this.decrease();
    		}
    		while(this.left < 1){
    			this.increase();
    		}
    	}
    	double displacementCost(){
    		double cost = 0.0;
    		for(Block block:this.blocks){
    			cost += block.verticalDisplacementCost();
    		}
    		return cost;
    	}
    	void increase(){
    		this.left += this.blockSize;
    		this.right += this.blockSize;
    		for(Block block:this.blocks){
    			block.legalY += this.blockSize;
    		}
    	}
    	void decrease(){
    		this.left -= this.blockSize;
    		this.right -= this.blockSize;
    		for(Block block:this.blocks){
    			block.legalY -= this.blockSize;
    		}
    	}
    	
    	public String toString(){
    		String result = new String();
    		if(this.blocks.size() == 1){
    			result += "1 block | ";
    		}else{
    			result += this.blocks.size() + " blocks| ";
    		}
    		result += "left = " + this.left + " | ";
    		result += "right = " + this.right + " | ";
    		for(Block block:this.blocks){
    			result += block + " ";
    		}
    		return result;
    	}
    	boolean overlap(){
    		if(this.previous != null){
    			if(this.previous.right > this.left){
    				return true;
    			}
    		}
    		if(this.next != null){
    			if(this.next.left < this.right){
    				return true;
    			}
    		}
    		return false;
    	}
    }
}
