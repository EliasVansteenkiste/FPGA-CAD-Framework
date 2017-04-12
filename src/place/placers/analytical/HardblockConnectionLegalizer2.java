package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNetBlock;
import place.util.TimingTree;

public class HardblockConnectionLegalizer2{

	private final double[] linearX, linearY;
    private final int[] legalX, legalY;

    private final Block[] blocks;
    private final Net[] nets;

    private final List<TimingNet> timingNets;

    private final int gridWidth, gridHeigth;
    
    private TimingTree timingTree;

	HardblockConnectionLegalizer2(
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
			int offset = (1 - heights[i]) / 2;
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
		
		//Set net weight
		for(Net net:this.nets){
			net.setNetWeight();
		}
		
		this.timingNets = timingNets;
		
		this.timingTree = new TimingTree();
	}
	public void legalizeHardblock(int firstBlockIndex, int lastBlockIndex, int firstColumn, int columnRepeat, int blockHeight){
		this.timingTree.start("LegalizeHardBlock");
		
		int firstRow = 1;
		int rowRepeat = blockHeight;

        int numColumns = (int) Math.floor((this.gridWidth - firstColumn) / columnRepeat + 1);
        int numRows = (int) Math.floor(this.gridHeigth / rowRepeat);
        
		Block[] legalizerBlocks = this.getLegalizeBlocks(firstBlockIndex, lastBlockIndex);
		Column[] columns = new Column[numColumns];
		for(int c = 0; c < numColumns; c++){
			int column = firstColumn + c * columnRepeat;
			
			Site[] sites = new Site[numRows];
			for(int r = 0; r < numRows; r++){
				int row = firstRow + r * rowRepeat;
				sites[r] = new Site(column, row);
			}
			columns[c] = new Column(column, sites);
		}
		
		this.updateBlockCoordinates();
		for(Block block:legalizerBlocks){
			double linearX = this.linearX[block.index];
			double linearY = this.linearY[block.index];
			int columnIndex = (int) Math.round(Math.max(Math.min((linearX - firstColumn) / columnRepeat, numColumns - 1), 0));
			int rowIndex = (int) Math.round(Math.max(Math.min((linearY - firstRow) / rowRepeat, numRows - 1), 0));
			
			block.setCoordinates(firstColumn + columnIndex * columnRepeat, firstRow + rowIndex * rowRepeat);
			columns[columnIndex].addBlock(block);
		}

		this.initializeConnectionCost();

		this.columnSwap(columns);
		
		this.timingTree.start("Anneal");
		
		for(Column column: columns){
			this.timingTree.start("column " + column.coordinate);
			this.anneal(column);
			this.timingTree.time("column " + column.coordinate);
		}
		this.timingTree.time("Anneal");

		this.timingTree.start("Finish");
		
		this.updateLegal();
		this.cleanData();
		
		this.timingTree.time("Finish");
		
		this.timingTree.time("LegalizeHardBlock");
	}
	public void legalizeIO(int firstBlockIndex, int lastBlockIndex){
		int siteCapacity = 2;
		
		Block[] legalizerBlocks = this.getLegalizeBlocks(firstBlockIndex, lastBlockIndex);
		Site[] legalizeSites = new Site[2 * (this.gridWidth + this.gridHeigth) * siteCapacity];
		int l = 0;
		for(int i = 1; i <= this.gridWidth; i++){
			for(int p = 0; p < siteCapacity; p++){
				legalizeSites[l++] = new Site(i, 0);
				legalizeSites[l++] = new Site(i, this.gridHeigth + 1);
			}
		}
		for(int i = 1; i <= this.gridHeigth; i++){
			for(int p = 0; p < siteCapacity; p++){
				legalizeSites[l++] = new Site(0, i);
				legalizeSites[l++] = new Site(this.gridWidth + 1, i);
			}
		}
		
		this.updateBlockCoordinates();
		
		for(Block block:legalizerBlocks){
			
			double linearX = this.linearX[block.index];
			double linearY = this.linearY[block.index];
			
			double minimumCost = Double.MAX_VALUE;
			Site bestFreeSite = null;
			
			for(Site site:legalizeSites){
				if(!site.hasBlock()){
					double cost = (site.column - linearX) * (site.column - linearX) + (site.row - linearY) * (site.row - linearY);
					if(cost < minimumCost){
						minimumCost = cost;
						bestFreeSite = site;
					}
				}
			}
			block.setSite(bestFreeSite);
			bestFreeSite.setBlock(block);
		}
		
		this.initializeConnectionCost();

		this.anneal(legalizerBlocks, legalizeSites);
		
		this.updateLegal();
		this.cleanData();
	}
	private void updateBlockCoordinates(){
        for(Block block:this.blocks){
        	block.setCoordinates(this.legalX[block.index], this.legalY[block.index] + block.offset);
        }
	}
	private Block[] getLegalizeBlocks(int firstBlockIndex, int lastBlockIndex){
        Block[] legalizeBlocks = new Block[lastBlockIndex - firstBlockIndex];
		for(int i = firstBlockIndex; i < lastBlockIndex; i++){
			legalizeBlocks[i - firstBlockIndex] = this.blocks[i];
			
	        //Offset test
	        if(this.blocks[i].offset != 0){
	        	System.out.println("The offset of hard  block is equal to " + this.blocks[i].offset + ", should be 0");
	        }
		}
		return legalizeBlocks;
	}
	
	public void updateCriticalConnections(){
		this.timingTree.start("Update critical connections");
		
		//Clear all data
		for(Block block:this.blocks){
			block.criticalConnections.clear();
		}

		//Update critical connections
		double criticalityThreshold = 0.75;
        
		for(TimingNet net : this.timingNets){
            NetBlock source = net.source;

            for(TimingNetBlock sink : net.sinks) {
                double criticality = sink.timingEdge.getCriticality();
                if(criticality > criticalityThreshold) {

                    if(source.blockIndex != sink.blockIndex) {
                    	Block sourceBlock = this.blocks[source.blockIndex];
                    	Block sinkBlock = this.blocks[sink.blockIndex];
                    	
                    	CriticalConnection conn = new CriticalConnection(sourceBlock, sinkBlock, criticality);
                    	sourceBlock.criticalConnections.add(conn);
                    	sinkBlock.criticalConnections.add(conn);
                    }
                }
            }
        }
		
		this.timingTree.time("Update critical connections");
	}
	private void initializeConnectionCost(){
		this.timingTree.start("Initialize connection cost");
		
		//Initialize nets
		for(Net net:this.nets){
			net.initializeConnectionCost();
		}

		//Initialize blocks
		for(Block block:this.blocks){
			block.initializeConnectionCost();
		}
		
		this.timingTree.time("Initialize connection cost");
	}
	private void columnSwap(Column[] columns){
		this.timingTree.start("Column swap");
		
		//Find buckets with empty positions
		HashSet<Column> freeColumns = new HashSet<Column>();
		for(Column column:columns){
			if(column.usedPos() < column.numPos()){
				freeColumns.add(column);
			}
		}
		
		this.timingTree.start("Distribute blocks");
		//Distribute blocks along the buckets
		Column largestColumn = this.largestBucket(columns);
		while(largestColumn.usedPos() > largestColumn.numPos()){

			Block bestBlock = null;
			Column bestColumn = null;
			double minimumIncrease = Double.MAX_VALUE;
			
			for(Block block: largestColumn.blocks){
				double currentCost = block.connectionCost();
				for(Column bucket:freeColumns){

					block.x = bucket.coordinate;
					double newCost = block.connectionCost(largestColumn.coordinate, bucket.coordinate, block.y, block.y);
					block.x = largestColumn.coordinate;

					double increase = newCost - currentCost;
					if(increase < minimumIncrease){
						minimumIncrease = increase;
						bestBlock = block;
						bestColumn = bucket;
					}
				}
			}

			largestColumn.removeBlock(bestBlock);
			bestColumn.addBlock(bestBlock);
			
			bestBlock.x = bestColumn.coordinate;
			bestBlock.connectionCost(largestColumn.coordinate, bestColumn.coordinate, bestBlock.y, bestBlock.y);
			bestBlock.updateConnectionCost();
			
			if(bestColumn.usedPos() == bestColumn.numPos()){
				freeColumns.remove(bestColumn);
			}
			
			largestColumn = this.largestBucket(columns);
		}
		this.timingTree.time("Distribute blocks");

		//Spread the blocks in the buckets
		for(Column column:columns){
			column.legalize();
		}
		
		this.timingTree.time("Column swap");
	}
	private Column largestBucket(Column[] buckets){
		Column result = buckets[0];
		for(Column bucket:buckets){
			if(bucket.usedPos() > result.usedPos()){
				result = bucket;
			}
		}
		return result;
	}
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// ANNEAL ///////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	private void anneal(Column column){
		this.anneal(column.blocks.toArray(new Block[column.blocks.size()]), column.sites);
	}
	private void anneal(Block[] annealBlocks, Site[] annealSites){
		int numBlocks = annealBlocks.length;
		int numSites = annealSites.length;
		
		Random random = new Random(100);
		
		double temperature = 3000;

		int temperatureSteps = 500;
		int stepsPerTemperature = (int)Math.pow(numBlocks, 4/3);
		
		for(int temperatureStep = 0; temperatureStep < temperatureSteps; temperatureStep++){

			temperature *= 0.98;
			
			for(int i = 0; i < stepsPerTemperature; i++){
				
				Block block1 = annealBlocks[random.nextInt(numBlocks)];
				Site site1 = block1.getSite();
				
				Site site2 = annealSites[random.nextInt(numSites)];
				while(site1.equals(site2)){
					site2 = annealSites[random.nextInt(numSites)];
				}
				Block block2 = site2.getBlock();
				

				
				if(block2 != null){

					double oldCost = block1.connectionCost() + block2.connectionCost();

					block1.setSite(site2);
					block2.setSite(site1);
					
					site2.setBlock(block1);
					site1.setBlock(block2);

					double newCost = block1.connectionCost(site1.column, site2.column, site1.row, site2.row) + block2.connectionCost(site2.column, site1.column, site2.row, site1.row);
					double deltaCost = newCost - oldCost;
					
	 				if(deltaCost <= 0 || random.nextDouble() < Math.exp(-deltaCost / temperature)) {
	 					block1.updateConnectionCost();
	 					block2.updateConnectionCost();
	 				}else{
	 					block1.setSite(site1);
	 					site1.setBlock(block1);
	 					
	 					site2.setBlock(block2);
	 					block2.setSite(site2);
	 				}
				}else{
					
					double oldCost = block1.connectionCost();
					
					block1.setSite(site2);
					site2.setBlock(block1);
					site1.removeBlock();

					double newCost = block1.connectionCost(site1.column, site2.column, site1.row, site2.row);
					double deltaCost = newCost - oldCost;
					
	 				if(deltaCost <= 0 || random.nextDouble() < Math.exp(-deltaCost / temperature)) {
	 					block1.updateConnectionCost();
	 				}else{
	 					block1.setSite(site1);
	 					site1.setBlock(block1);
	 					site2.removeBlock();
	 				}
				}

			}
		}
	}
    private void updateLegal(){
    	for(Block block:this.blocks){
    		this.legalX[block.index] = block.x;
    		this.legalY[block.index] = block.y - block.offset;
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
	private class Block{
		final int index, offset;
		int x, y;
		Site site;

		final List<Net> nets;
		final List<CriticalConnection> criticalConnections;

		double connectionCost;

		Block(int index, int offset){
			this.index = index;
			this.offset = offset;

			this.nets = new ArrayList<Net>();
			this.criticalConnections = new ArrayList<CriticalConnection>();

			this.site = null;
		}
		void addNet(Net net){
			if(!this.nets.contains(net)){
				this.nets.add(net);
			}
		}

		void setCoordinates(int x, int y){
			this.x = x;
			this.y = y;
		}
		void reset(){
			this.site = null;
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
				this.connectionCost += net.updateHorizontalConnectionCost();
				this.connectionCost += net.updateVerticalConnectionCost();
			}
			for(CriticalConnection conn:this.criticalConnections){
				this.connectionCost += conn.timingCost();
			}
		}
    	
    	//Site
    	void setSite(Site site){
    		this.site = site;
    		this.x = site.column;
    		this.y = site.row;
    	}
    	boolean hasSite(){
    		return this.site != null;
    	}
    	Site getSite(){
    		return this.site;
    	}
	}
    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// SITE //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	private class Site {

	    private final int column, row;
	    private Block block;

	    public Site(int column, int row) {
	        this.column = column;
	        this.row = row;
	        
	        this.block = null;
	    }
	    
	    public void setBlock(Block block){
		    this.block = block;
	    }
	    public boolean hasBlock(){
	    	return this.block != null;
	    }
	    public Block getBlock(){
	        return this.block;
	    }
	    public void removeBlock(){
	    	this.block = null;
	    }
	}
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// NET //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	private class Net{
		final int index;
		final List<Block> blocks;
		double netWeight;
		
		int minX, maxX;
		int minY, maxY;
		
		int tempMinX, tempMaxX;
		int tempMinY, tempMaxY;
		
		double horizontalConnectionCost;
		double verticalConnectionCost;

		Net(int index){
			this.index = index;
			this.blocks = new ArrayList<Block>();
		}
		void addBlock(Block block){
			this.blocks.add(block);
		}
		void setNetWeight(){
			this.netWeight = AnalyticalAndGradientPlacer.getWeight(this.blocks.size());
		}

		//// Connection cost ////
		void initializeConnectionCost(){
			Block initialBlock = this.blocks.get(0);
	        this.minX = this.maxX = initialBlock.x;
	        this.minY = this.maxY = initialBlock.y;
	        for(Block block:this.blocks){
	            if(block.x < this.minX) {
	                this.minX = block.x;
	            }
	            if(block.x > this.maxX) {
	                this.maxX = block.x;
	            }
	            if(block.y < this.minY) {
	                this.minY = block.y;
	            }
	            if(block.y > this.maxY) {
	                this.maxY = block.y;
	            }
	        }

			this.horizontalConnectionCost = (this.maxX - this.minX + 1) * this.netWeight;
			this.verticalConnectionCost = (this.maxY - this.minY + 1) * this.netWeight;
		}
		double connectionCost(){
			return this.horizontalConnectionCost + this.verticalConnectionCost;
		}

		// Horizontal
		double horizontalConnectionCost(int oldX, int newX){
            this.updateTempMinX(oldX, newX);
            this.updateTempMaxX(oldX, newX);
            return (this.tempMaxX - this.tempMinX + 1) * this.netWeight;
		}
		double updateHorizontalConnectionCost(){
			this.minX = this.tempMinX;
			this.maxX = this.tempMaxX;
			
			this.horizontalConnectionCost = (this.maxX - this.minX + 1) * this.netWeight;
			
			return this.horizontalConnectionCost;
		}
		void updateTempMinX(int oldX, int newX){
			if(oldX == newX){
				this.tempMinX = this.minX;
			}else if(newX <= this.minX){
            	this.tempMinX = newX;
			}else if(oldX == this.minX){
            	this.tempMinX = this.getMinX();
            }else{
            	this.tempMinX = this.minX;
            }
		}
		void updateTempMaxX(int oldX, int newX){
			if(oldX == newX){
				this.tempMaxX = this.maxX;
			}else if(newX >= this.maxX){
            	this.tempMaxX = newX;
            }else if(oldX == this.maxX){
            	this.tempMaxX = this.getMaxX();
            }else{
            	this.tempMaxX = this.maxX;
            }
		}
		int getMinX(){
			Block initialBlock = this.blocks.get(0);
	        int minX = initialBlock.x;
	        for(Block block:this.blocks){
	            if(block.x < minX) {
	                minX = block.x;
	            }
	        }
	        return minX;
		}
		int getMaxX(){
			Block initialBlock = this.blocks.get(0);
	        int maxX = initialBlock.x;
	        for(Block block:this.blocks){
	            if(block.x > maxX) {
	                maxX = block.x;
	            }
	        }
	        return maxX;
		}

		// Vertical
		double verticalConnectionCost(int oldY, int newY){
			this.updateTempMinY(oldY, newY);
            this.updateTempMaxY(oldY, newY);
            return (this.tempMaxY - this.tempMinY + 1) * this.netWeight;
		}
		double updateVerticalConnectionCost(){
			this.minY = this.tempMinY;
			this.maxY = this.tempMaxY;
			
			this.verticalConnectionCost = (this.maxY - this.minY + 1) * this.netWeight;
			
			return this.verticalConnectionCost;
		}
		void updateTempMinY(int oldY, int newY){
			if(oldY == newY){
				this.tempMinY = this.minY;
			}else if(newY <= this.minY){
            	this.tempMinY = newY;
			}else if(oldY == this.minY){
				this.tempMinY = this.getMinY();
            }else{
            	this.tempMinY = this.minY;
            }
		}
		void updateTempMaxY(int oldY, int newY){
			if(oldY == newY){
				this.tempMaxY = this.maxY;
			}else if(newY >= this.maxY){
            	this.tempMaxY = newY;
            }else if(oldY == this.maxY){
            	this.tempMaxY = this.getMaxY();
            }else{
            	this.tempMaxY = this.maxY;
            }
		}
		private int getMinY(){
			Block initialBlock = this.blocks.get(0);
	        int minY = initialBlock.y;
	        for(Block block:this.blocks){
	            if(block.y < minY) {
	                minY = block.y;
	            }
	        }
	        return minY;
		}
		private int getMaxY(){
			Block initialBlock = this.blocks.get(0);
	        int maxY = initialBlock.y;
	        for(Block block:this.blocks){
	            if(block.y > maxY) {
	                maxY = block.y;
	            }
	        }
	        return maxY;
		}
	}
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// CRITICAL CONNECTION //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	private class CriticalConnection {
		private Block sourceBlock, sinkBlock;
		private double criticality;
		
		CriticalConnection(Block sourceBlock, Block sinkBlock, double criticality){
			this.sourceBlock = sourceBlock;
			this.sinkBlock = sinkBlock;
			this.criticality = criticality;
		}
		int manhattanDistance(){
			return Math.abs(sourceBlock.x - sinkBlock.x)  + Math.abs(sourceBlock.y - sinkBlock.y);
		}
		double timingCost(){
			return 50 * this.criticality * this.manhattanDistance();
		}
	}
	
    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// BUCKET ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	private class Column {
		private final Set<Block> blocks;
		private final Site[] sites;
		private final int coordinate;

	    Column(int coordinate, Site[] sites){
	    	this.blocks = new HashSet<Block>();
	    	this.sites = sites;
	    	this.coordinate = coordinate;
	    }

		void addBlock(Block block){
			this.blocks.add(block);
		}
		void removeBlock(Block block){
			this.blocks.remove(block);
		}
		
		int numPos(){
			return this.sites.length;
		}
		int usedPos(){
			return this.blocks.size();
		}
		
		void legalize(){
			if(this.usedPos() > this.numPos()){
				System.out.println("To many blocks in column to legalize");
			}else{
				for(Block block:this.blocks){
					Site site = this.getBestSite(block);
					
					if(!site.hasBlock()){
						block.setSite(site);
						site.setBlock(block);
					}
				}
				for(Block block:this.blocks){
					if(!block.hasSite()){
						Site site = this.getBestFreeSite(block);
						block.setSite(site);
						site.setBlock(block);
					}
				}
			}
		}
		Site getBestSite(Block block){
			
			Site bestSite = null;
			double minimumCost = Double.MAX_VALUE;
			
			for(Site site:this.sites){
				int oldX = block.x;
				int oldY = block.y;
				
				int newX = site.column;
				int newY = site.row;
				
				block.setCoordinates(newX, newY);
				double cost = block.connectionCost(oldX, newX, oldY, newY);
				
				if(cost < minimumCost){
					minimumCost = cost;
					bestSite = site;
				}
			}
			return bestSite;
		}
		Site getBestFreeSite(Block block){
			
			Site bestSite = null;
			double minimumCost = Double.MAX_VALUE;
			
			for(Site site:this.sites){
				if(!site.hasBlock()){
					int oldX = block.x;
					int oldY = block.y;
					
					int newX = site.column;
					int newY = site.row;
					
					block.setCoordinates(newX, newY);
					double cost = block.connectionCost(oldX, newX, oldY, newY);
					
					if(cost < minimumCost){
						minimumCost = cost;
						bestSite = site;
					}
				}
			}
			return bestSite;
		}
	}
}