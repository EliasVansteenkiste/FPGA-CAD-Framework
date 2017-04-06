package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNetBlock;

public class HardblockConnectionLegalizer{

	private double[] linearX, linearY;
    private int[] legalX, legalY;
    
    private Block[] blocks;
    private Net[] nets;
    private List<TimingNet> timingNets;
    
    private final int gridWidth, gridHeigth;
    
    private int firstRow, rowRepeat;
    private int firstColumn, columnRepeat;
    private int numRows, numColumns;
    private int siteCapacity;

	HardblockConnectionLegalizer(
			double[] linearX,
			double[] linearY,
			int[] legalX, 
			int[] legalY, 
			int[] heights,
			int gridWidth,
			int gridHeight,
			List<AnalyticalAndGradientPlacer.Net> placerNets,
			List<TimingNet> timingNets,
			Map<GlobalBlock, NetBlock> blockIndexes){

		this.linearX = linearX;
		this.linearY = linearY;

		this.legalX = legalX;
		this.legalY = legalY;
		
		this.gridWidth = gridWidth;
		this.gridHeigth = gridHeight;

		//Make all objects
		this.blocks = new Block[legalX.length];
		this.nets = new Net[placerNets.size()];

		for(int i = 0; i < placerNets.size(); i++){
			this.nets[i] = new Net(i);
		}
		for(int i = 0; i < legalX.length; i++){
			int offset = (1- heights[i]) / 2;
			this.blocks[i] = new Block(i, offset);
		}
		
		//Connect objects
		for(int i = 0; i < placerNets.size(); i++){
			Net legalizerNet = this.nets[i];
			for(NetBlock block:placerNets.get(i).blocks){
				Block legalizerBlock = this.blocks[block.blockIndex];
				legalizerNet.addBlock(legalizerBlock);
				legalizerBlock.addNet(legalizerNet);
			}
		}
		
		this.timingNets = timingNets;
	}
	public void legalizeBlockType(int firstBlockIndex, int lastBlockIndex, int firstColumn, int columnRepeat, int blockHeight, int capacity){
		boolean io = firstColumn == -1 && columnRepeat == -1;
		
		Site[] legalizeSites = null;
		this.siteCapacity = capacity;

        // Blocks
        for(Block block:this.blocks){
        	block.setLinearCoordinates(this.linearX[block.index], this.linearY[block.index] + block.offset);
        	block.setLegalCoordinates(this.legalX[block.index], this.legalY[block.index] + block.offset);
        }
        Block[] legalizeBlocks = new Block[lastBlockIndex - firstBlockIndex];
		for(int i = firstBlockIndex; i < lastBlockIndex; i++){
			legalizeBlocks[i - firstBlockIndex] = this.blocks[i];
			
	        //Offset test
	        if(this.blocks[i].offset != 0){
	        	System.out.println("The offset of hard  block is equal to " + this.blocks[i].offset + ", should be 0");
	        }
		}

		// Sites
		if(io){
			legalizeSites = new Site[2 * (this.gridWidth + this.gridHeigth) * this.siteCapacity];
			int l = 0;
			for(int i = 1; i <= this.gridWidth; i++){
				for(int p = 0; p < this.siteCapacity; p++){
					legalizeSites[l++] = new Site(i, 0);
					legalizeSites[l++] = new Site(i, this.gridHeigth + 1);
				}
			}
			for(int i = 1; i <= this.gridHeigth; i++){
				for(int p = 0; p < this.siteCapacity; p++){
					legalizeSites[l++] = new Site(0, i);
					legalizeSites[l++] = new Site(this.gridWidth + 1, i);
				}
			}
		}else{
			this.firstRow = 1;
			this.rowRepeat = blockHeight;

			this.firstColumn = firstColumn;
			this.columnRepeat = columnRepeat;

	        this.numColumns = (int) Math.floor((this.gridWidth - this.firstColumn) / this.columnRepeat + 1);
	        this.numRows = (int) Math.floor(this.gridHeigth / this.rowRepeat);
	        
			legalizeSites = new Site[this.numColumns * this.numRows * this.siteCapacity];
			for(int c = 0; c < this.numColumns; c++){
				int column = this.firstColumn + c * this.columnRepeat;
				for(int r = 0; r < this.numRows; r++){
					int row = this.firstRow + r * this.rowRepeat;

					for(int p = 0; p < this.siteCapacity; p++){
						legalizeSites[(c * this.numRows + r) * this.siteCapacity + p] = new Site(column, row);
					}
				}
			}
		}

		this.criticalConnections(legalizeBlocks);

		if(io){
			this.ioInitialization(legalizeBlocks, legalizeSites);
		}else{
			this.bucketInitialization(legalizeBlocks, legalizeSites);
		}
		//this.randomInitialization(legalizeBlocks, legalizeSites);
		
		this.anneal(legalizeBlocks, legalizeSites);

		this.updateLegal();

		this.cleanData();
	}
	
	private void criticalConnections(Block[] legalizeBlocks){
		double criticalityThreshold = 0.8;
		
		int firstBlockIndex = legalizeBlocks[0].index;
		int numBlocks = legalizeBlocks.length;
        
		for(TimingNet net : this.timingNets){
            NetBlock source = net.source;

            for(TimingNetBlock sink : net.sinks) {
                double criticality = sink.timingEdge.getCriticality();
                if(criticality > criticalityThreshold) {

                    if(source.blockIndex != sink.blockIndex) {
                    	
                    	if(source.blockIndex - firstBlockIndex < numBlocks || sink.blockIndex - firstBlockIndex < numBlocks){
                    		Block sourceBlock = this.blocks[source.blockIndex];
                    		Block sinkBlock = this.blocks[sink.blockIndex];
                    		
                    		CriticalConnection conn = new CriticalConnection(sourceBlock, sinkBlock, criticality);
                    		sourceBlock.criticalConnections.add(conn);
                    		sinkBlock.criticalConnections.add(conn);
                    	}
                    }
                }
            }
        }
	}
	
	void ioInitialization(Block[] annealBlocks, Site[] annealSites){
		for(Block block:annealBlocks){
			Site site = this.findEmptySite(block.legalX, block.legalY, annealSites);
			
			site.addBlock(block);
			block.setSite(site);
		}
	}
	void randomInitialization(Block[] annealBlocks, Site[] annealSites){
		Random random = new Random(200);
		
		for(Block block:annealBlocks){
			Site site = annealSites[random.nextInt(annealSites.length)];
			while(site.hasBlock()){
				site = annealSites[random.nextInt(annealSites.length)];
			}
			site.addBlock(block);
			block.setSite(site);
		}
	}
	void bucketInitialization(Block[] legalizeBlocks, Site[] legalizeSites){
        //Make the buckets for the current hard block type
        Bucket[] buckets = new Bucket[this.numColumns];
        for(int b = 0; b < this.numColumns; b++){
			buckets[b] = new Bucket(this.numRows, this.rowRepeat, this.firstColumn + b * this.columnRepeat);
		}

		for(Block block:legalizeBlocks){
			int columnIndex = (int) Math.round(Math.max(Math.min((block.linearX - this.firstColumn) / this.columnRepeat, this.numColumns - 1), 0));
			int rowIndex =    (int) Math.round(Math.max(Math.min((block.linearY - this.firstRow)    / this.rowRepeat,    this.numRows - 1)   , 0));
			block.setLegalCoordinates(this.firstColumn + this.columnRepeat * columnIndex, this.firstRow + this.rowRepeat * rowIndex);
			
			buckets[columnIndex].addBlock(block);
		}

		//Find buckets with empty positions
		HashSet<Bucket> availableBuckets = new HashSet<Bucket>();
		for(Bucket bucket:buckets){
			if(bucket.usedPos() < bucket.numPos()){
				availableBuckets.add(bucket);
			}
		}

		//Initialize connection cost
		for(Net net:this.nets){
			net.initializeConnectionCost();
		}
		for(Block block:this.blocks){
			block.initializeConnectionCost();
		}
		
		//Distribute blocks along the buckets
		Bucket largestBucket = this.largestBucket(buckets);
		while(largestBucket.usedPos() > largestBucket.numPos()){

			Block bestBlock = null;
			Bucket bestBucket = null;
			double minimumIncrease = Double.MAX_VALUE;
			
			for(Block block: largestBucket.blocks){
				double currentCost = block.connectionCost();
				for(Bucket bucket:availableBuckets){

					block.legalX = bucket.coordinate;
					double newCost = block.connectionCost(largestBucket.coordinate, bucket.coordinate, block.legalY, block.legalY);
					block.legalX = largestBucket.coordinate;

					double increase = newCost - currentCost;
					if(increase < minimumIncrease){
						minimumIncrease = increase;
						bestBlock = block;
						bestBucket = bucket;
					}
				}
			}

			largestBucket.removeBlock(bestBlock);
			bestBucket.addBlock(bestBlock);
			
			bestBlock.legalX = bestBucket.coordinate;
			bestBlock.connectionCost(largestBucket.coordinate, bestBucket.coordinate, bestBlock.legalY, bestBlock.legalY);
			bestBlock.updateConnectionCost();
			
			if(bestBucket.usedPos() == bestBucket.numPos()){
				availableBuckets.remove(bestBucket);
			}
			
			largestBucket = this.largestBucket(buckets);
		}

		//Spread the blocks in the buckets
		for(Bucket bucket:buckets){
			bucket.legalize();
		}

		//Assign sites to blocks and blocks to sites
		for(Block block:legalizeBlocks){
			//int columnIndex = (block.legalX - this.firstColumn) / this.columnRepeat;
			//int rowIndex    = (block.legalY - this.firstRow)    / this.rowRepeat;
			//Site site = annealSites[columnIndex * this.numRows + rowIndex];

			Site site = this.findEmptySite(block.legalX, block.legalY, legalizeSites);

			site.addBlock(block);
			block.setSite(site);
		}
	}
	private Bucket largestBucket(Bucket[] buckets){
		Bucket result = buckets[0];
		for(Bucket bucket:buckets){
			if(bucket.usedPos() > result.usedPos()){
				result = bucket;
			}
		}
		return result;
	}
	private Site findEmptySite(int column, int row, Site[] sites){
		for(Site site:sites){
			if(site.getColumn() == column){
				if(site.getRow() == row){
					if(!site.hasBlock()){
						return site;
					}
				}
			}
		}
		return null;
	}

    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// ANNEAL ///////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	private void anneal(Block[] annealBlocks, Site[] annealSites){
		int numBlocks = annealBlocks.length;
		int numSites = annealSites.length;
		
		Random random = new Random(100);
		
		//double temperature = 3000;

		//int temperatureSteps = 500;
		//int stepsPerTemperature = numBlocks;
		
		double temperature = 2000;

		int temperatureSteps = 500;
		int stepsPerTemperature = numBlocks;
		
		for(int temperatureStep = 0; temperatureStep < temperatureSteps; temperatureStep++){
			
			//temperature *= 0.98;
			
			temperature *= 0.98;
			
			for(int i = 0; i < stepsPerTemperature; i++){
				
				Block block1 = annealBlocks[random.nextInt(numBlocks)];
				Site site1 = block1.getSite();
				
				Site site2 = annealSites[random.nextInt(numSites)];
				while(site1.equals(site2)){
					site2 = annealSites[random.nextInt(numSites)];
				}
				Block block2 = site2.getBlock();
				
				double oldCost = block1.connectionCost();
				if(block2 != null){
					oldCost += block2.connectionCost();
				}
				
				block1.legalX = site2.column;
				block1.legalY = site2.row;
				if(block2 != null){
					block2.legalX = site1.column;
					block2.legalY = site1.row;
				}

				double newCost = block1.connectionCost(site1.column, site2.column, site1.row, site2.row);
				if(block2 != null){
					newCost += block2.connectionCost(site2.column, site1.column, site2.row, site1.row);
				}
				
				double deltaCost = newCost - oldCost;
				
 				if(deltaCost <= 0 || random.nextDouble() < Math.exp(-deltaCost / temperature)) {

 					site1.removeBlock(block1);
 					block1.removeSite(site1);
 					
 					if(block2 != null){
 						site2.removeBlock(block2);
 						block2.removeSite(site2);
 					}
 					
 					site2.addBlock(block1);
 					block1.setSite(site2);
 
 					if(block2 != null){
 						site1.addBlock(block2);
 						block2.setSite(site1);
 					}
 					
 					block1.updateConnectionCost();
 					
 					if(block2 != null){
 						block2.updateConnectionCost();
 					}
 					
 				}else{
 					
 					block1.legalX = site1.column;
 					block1.legalY = site1.row;
 					
 					if(block2 != null){
 						block2.legalX = site2.column;
 						block2.legalY = site2.row;
 					}
 				}
			}
		}
	}
	
    private void updateLegal(){
    	for(Block block:this.blocks){
    		this.legalX[block.index] = block.legalX;
    		this.legalY[block.index] = block.legalY - block.offset;
    	}
    }
    private void cleanData(){
    	for(Block block:this.blocks){
    		block.reset();
    	}
    }
	
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// BLOCK ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Block{
		final int index, offset;
		
		double linearX, linearY;
		int legalX, legalY;

		final List<Net> nets;
		
		private Site site;
		
		private List<CriticalConnection> criticalConnections;
		
		private double connectionCost;
		
		Block(int index, int offset){
			this.index = index;
			this.offset = offset;
			
			this.linearX = -1;
			this.linearY = -1;
			this.legalX = -1;
			this.legalY = -1;
			
			this.nets = new ArrayList<Net>();
			
			this.site = null;
			
			this.connectionCost = 0.0;
			
			this.criticalConnections = new ArrayList<CriticalConnection>();
		}
		void reset(){
			this.site = null;
			this.criticalConnections.clear();
		}
		void setLinearCoordinates(double linearX, double linearY){
			this.linearX = linearX;
			this.linearY = linearY;
		}
		void setLegalCoordinates(int legalX, int legalY){
			this.legalX = legalX;
			this.legalY = legalY;
		}
		
		void addNet(Net net){
			if(!this.nets.contains(net)){
				this.nets.add(net);
			}
		}
		
		//// Connection cost ////
		void initializeConnectionCost(){
			this.connectionCost = 0.0;
			
			for(Net net:this.nets){
				this.connectionCost += net.connectionCost();
			}
			for(CriticalConnection conn:this.criticalConnections){
				this.connectionCost += conn.timingCost();
			}
		}
		double connectionCost(){
			return this.connectionCost;
		}
		double connectionCost(int oldX, int newX, int oldY, int newY){
			double cost = 0.0;
			
			for(Net net:this.nets){
				cost += net.horizontalConnectionCost(oldX, newX);
				cost += net.verticalConnectionCost(oldY, newY);
			}
			for(CriticalConnection conn:this.criticalConnections){
				cost += conn.timingCost();
			}
			
			return cost;
		}
		void updateConnectionCost(){
			this.connectionCost = 0.0;
			
			for(Net net:this.nets){
				this.connectionCost += net.updateConnectionCost();
			}
			for(CriticalConnection conn:this.criticalConnections){
				this.connectionCost += conn.timingCost();
			}
		}
		
		//// Displacement cost ////
		double displacementCost(){
			return (this.linearX - this.legalX) * (this.linearX - this.legalX) + (this.linearY - this.legalY) * (this.linearY - this.legalY);
		}
		
    	public String toString(){
    		return String.format("%.2f  %.2f", this.linearX, this.linearY);
    	}
    	
    	//Site
    	void setSite(Site site){
    		if(this.site == null){
    			this.site = site;
    		}else{
    			System.out.println("This block already has a site");
    		}
    	}
    	Site getSite(){
    		return this.site;
    	}
    	void removeSite(Site removeSite){
    		if(this.site.equals(removeSite)){
    			this.site = null;
    		}else{
    			System.out.println("This block has no site");
    		}
    	}
    	boolean hasSite(){
    		return this.site != null;
    	}
	}
    public static class BlockComparator {
        public static Comparator<Block> HORIZONTAL = new Comparator<Block>() {
            @Override
            public int compare(Block b1, Block b2) {
                return Double.compare(b1.linearX, b2.linearX);
            }
        };
        public static Comparator<Block> VERTICAL = new Comparator<Block>() {
            @Override
            public int compare(Block b1, Block b2) {
                return Double.compare(b1.linearY, b2.linearY);
            }
        };
    }
    
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// NET //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Net{
		final int index;
		final List<Block> blocks;

		double netWeight;
		int minX, maxX;
		int minY, maxY;
		
		int tempMinX, tempMaxX;
		int tempMinY, tempMaxY;
		
		double horizontalConnectionCost;
		double verticalConnectionCost;
		double timingCost;

		Net(int index){
			this.index = index;
			this.blocks = new ArrayList<Block>();
		}

		void addBlock(Block block){
			this.blocks.add(block);
		}

		//// Connection cost ////
		double initializeConnectionCost(){
			this.netWeight = AnalyticalAndGradientPlacer.getWeight(this.blocks.size());
			
			this.minX = this.getMinX();
			this.maxX = this.getMaxX();
			
			this.minY = this.getMinY();
			this.maxY = this.getMaxY();
			
			this.horizontalConnectionCost = (this.maxX - this.minX + 1) * this.netWeight;
			this.verticalConnectionCost = (this.maxY - this.minY + 1) * this.netWeight;
			
			return this.connectionCost();
		}
		
		//All cost
		double connectionCost(){
			return this.horizontalConnectionCost + this.verticalConnectionCost;
		}
		double updateConnectionCost(){
			this.updateHorizontalConnectionCost();
			this.updateVerticalConnectionCost();
			
			return this.connectionCost();
		}
		
		// Horizontal
		double horizontalConnectionCost(int oldX, int newX){
            this.updateTempMinX(oldX, newX);
            this.updateTempMaxX(oldX, newX);
            return (this.tempMaxX - this.tempMinX + 1) * this.netWeight;
		}
		private void updateHorizontalConnectionCost(){
			this.minX = this.tempMinX;
			this.maxX = this.tempMaxX;
			
			this.horizontalConnectionCost = (this.maxX - this.minX + 1) * this.netWeight;
		}
		private void updateTempMinX(int oldX, int newX){
			if(newX <= this.minX){
            	this.tempMinX = newX;
			}else if(oldX == this.minX){
            	this.tempMinX = this.getMinX();
            }else{
            	this.tempMinX = this.minX;
            }
		}
		private void updateTempMaxX(int oldX, int newX){
            if(newX >= this.maxX){
            	this.tempMaxX = newX;
            }else if(oldX == this.maxX){
            	this.tempMaxX = this.getMaxX();
            }else{
            	this.tempMaxX = this.maxX;
            }
		}
		private int getMinX(){
			Block initialBlock = this.blocks.get(0);
	        int minX = initialBlock.legalX;
	        for(Block block:this.blocks){
	            if(block.legalX < minX) {
	                minX = block.legalX;
	            }
	        }
	        return minX;
		}
		private int getMaxX(){
			Block initialBlock = this.blocks.get(0);
	        int maxX = initialBlock.legalX;
	        for(Block block:this.blocks){
	            if(block.legalX > maxX) {
	                maxX = block.legalX;
	            }
	        }
	        return maxX;
		}
		
		
		// Vertical
		private double verticalConnectionCost(int oldY, int newY){
            this.updateTempMinY(oldY, newY);
            this.updateTempMaxY(oldY, newY);
            return (this.tempMaxY - this.tempMinY + 1) * this.netWeight;
		}
		private void updateVerticalConnectionCost(){
			this.minY = this.tempMinY;
			this.maxY = this.tempMaxY;
			
			this.verticalConnectionCost = (this.maxY - this.minY + 1) * this.netWeight;
		}
		private void updateTempMinY(int oldY, int newY){
			if(newY <= this.minY){
            	this.tempMinY = newY;
			}else if(oldY == this.minY){
				this.tempMinY = this.getMinY();
            }else{
            	this.tempMinY = this.minY;
            }
		}
		private void updateTempMaxY(int oldY, int newY){
            if(newY >= this.maxY){
            	this.tempMaxY = newY;
            }else if(oldY == this.maxY){
            	this.tempMaxY = this.getMaxY();
            }else{
            	this.tempMaxY = this.maxY;
            }
		}
		private int getMinY(){
			Block initialBlock = this.blocks.get(0);
	        int minY = initialBlock.legalY;
	        for(Block block:this.blocks){
	            if(block.legalY < minY) {
	                minY = block.legalY;
	            }
	        }
	        return minY;
		}
		private int getMaxY(){
			Block initialBlock = this.blocks.get(0);
	        int maxY = initialBlock.legalY;
	        for(Block block:this.blocks){
	            if(block.legalY > maxY) {
	                maxY = block.legalY;
	            }
	        }
	        return maxY;
		}
	}
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// CRITICAL CONNECTION //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class CriticalConnection {
		private Block sourceBlock, sinkBlock;
		private double criticality;
		
		CriticalConnection(Block sourceBlock, Block sinkBlock, double criticality){
			this.sourceBlock = sourceBlock;
			this.sinkBlock = sinkBlock;
			this.criticality = criticality;
		}
		int manhattanDistance(){
			return Math.abs(sourceBlock.legalX - sinkBlock.legalX)  + Math.abs(sourceBlock.legalY - sinkBlock.legalY);
		}
		double timingCost(){
			return 50 * this.criticality * this.manhattanDistance();
		}
	}
	
    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// BUCKET ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Bucket {
	    private final int numPos;
	    private final int blockSize;
		private final int gridSize;
		
		private final int firstRow, rowRepeat, numRows;

		private final int coordinate;
		
		private Cluster firstCluster;
		
		private List<Block> blocks;

	    Bucket(int numPos, int blockSize, int coordinate){
	    	this.numPos = numPos;
	    	this.blockSize = blockSize;
	    	this.gridSize = this.numPos * this.blockSize;
	    	
	    	this.firstRow = 1;
	    	this.rowRepeat = this.blockSize;
	    	this.numRows = (int) Math.floor(this.gridSize / this.rowRepeat);
	    	
	    	this.coordinate = coordinate;
	    	
	    	this.firstCluster = null;
	    	
	    	this.blocks = new LinkedList<Block>();
	    }

		void addBlock(Block block){
			this.blocks.add(block);
		}
		void removeBlock(Block block){
			this.blocks.remove(block);
		}
		
		double displacementCost(){
			double cost = 0.0;
			for(Block block:this.blocks){
				cost += block.displacementCost();
			}
			return cost;
		}
		
		int numPos(){
			return this.numPos;
		}
		int usedPos(){
			return this.blocks.size();
		}
		boolean isEmpty(){
			return this.blocks.isEmpty();
		}
		
		void legalize(){
			if(this.blocks.size() < 2){
				return;
			}else{
				Collections.sort(this.blocks, BlockComparator.VERTICAL);
				
				for(Block block:this.blocks){
					int rowIndex = (int) Math.round(Math.max(Math.min((block.linearY - this.firstRow) / this.rowRepeat, this.numRows - 1), 0));
					block.setLegalCoordinates(this.coordinate, 1 + this.blockSize * rowIndex);
				}
				
		    	//Make the chain of clusters
				Cluster previousCluster = null;
				for(Block block:this.blocks){
					Cluster cluster = new Cluster(block, this.blockSize, this.gridSize);
					
					if(previousCluster == null){
						this.firstCluster = cluster;
					}else{
			    		cluster.previous = previousCluster;
			    		previousCluster.next = cluster;
					}
					previousCluster = cluster;
				}
		    	
		    	while(this.overlap()){
		        	Cluster cluster = this.firstCluster;
		        	do{
		        		cluster.absorbNeighbour();
		        		
		        		cluster = cluster.next;
		        	}while(cluster != null);
		    	}
			}
		}
		boolean overlap(){
			Cluster cluster = this.firstCluster;
			do{
				if(this.overlap(cluster)){
	    			return true;
	    		}	
				cluster = cluster.next;
			}while(cluster != null);
			return false;
		}
		boolean overlap(Cluster cluster){
			if(cluster.previous != null){
				if(cluster.previous.right > cluster.left){
					return true;
				}
			}
			if(cluster.next != null){
				if(cluster.next.left < cluster.right){
					return true;
				}
			}
			return false;
		}
	    void printBlocks(){
	    	System.out.println("Print blocks:");
	    	for(Block block:this.blocks){
	    		System.out.println("\t" + block);
	    	}
	    	System.out.println();
	    }
	    void printClusters(){
			System.out.println("Print clusters:");
	    	Cluster cluster = this.firstCluster;
	    	do{
	    		System.out.println("\t" + cluster);
	    		cluster = cluster.next;
	    	}while(cluster != null);
	    	System.out.println();
	    }
		public String toString(){
			return this.blocks.size() + " | " + this.numPos;
		}
	}

    /////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// CLUSTER ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
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
    			cost += block.displacementCost();
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
    		result += "left = " + this.left + " | ";
    		result += "right = " + this.right + " | ";
    		for(Block block:this.blocks){
    			result += block + " ";
    		}
    		return result;
    	}
    }
	
    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// SITE //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	public class Site {

	    private final int column, row;
	    private Block block;

	    public Site(int column, int row) {
	        this.column = column;
	        this.row = row;
	        
	        this.block = null;
	    }

	    public int getColumn() {
	        return this.column;
	    }
	    public int getRow() {
	        return this.row;
	    }
	    
	    public void removeBlock(Block removeBlock){
	    	if(this.block.equals(removeBlock)){
	    		this.block = null;
	    	}else{
	    		System.out.println("This site has no block");
	    	}
	    }
	    public boolean hasBlock(){
	    	return this.block != null;
	    }
	    public void addBlock(Block block){
		    if(this.block == null){
		    	this.block = block;
	    	}else{
	    		System.out.println("This site already has a block");
	    	}
	    }
	    public Block getBlock(){
	        return this.block;
	    }
	}
}