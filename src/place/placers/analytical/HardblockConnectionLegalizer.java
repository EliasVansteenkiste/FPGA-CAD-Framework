package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.util.TimingTree;

public class HardblockConnectionLegalizer{
	private Circuit circuit;
	private BlockType blockType;

	private final double[] linearX, linearY;
    private final int[] legalX, legalY;

    private final Block[] blocks;
    private final Net[] nets;
    private final List<Crit> crits;

    private final HardblockColumnSwap columnSwap;
    private final HardblockAnneal hardblockAnneal;
    
    private final Map<BlockType, Block[]> blocksPerBlocktype;
    private final Map<BlockType, Net[]> netsPerBlocktype;

    private final int gridWidth, gridHeigth;
    
    private final TimingTree timingTree;

	HardblockConnectionLegalizer(
			Circuit circuit,
			double[] linearX,
			double[] linearY,
			int[] legalX, 
			int[] legalY,
			int[] heights,
			int gridWidth,
			int gridHeight,
			List<AnalyticalAndGradientPlacer.Net> placerNets){

		this.timingTree = new TimingTree(false);

		this.timingTree.start("Initialize Hardblock Connection Legalizer Data");

		this.circuit = circuit;
		
		this.linearX = linearX;
		this.linearY = linearY;

		this.legalX = legalX;
		this.legalY = legalY;

		this.gridWidth = gridWidth;
		this.gridHeigth = gridHeight;

		//Count the number of nets
		int maxFanout = 100;
		System.out.println("Nets with fanout larger than " + maxFanout + " are left out:");
		int numNets = 0;
		Map<Integer,Integer> fanoutMap = new HashMap<>();
		for(int i = 0; i < placerNets.size(); i++){
			int fanout = placerNets.get(i).blocks.length;
			if(fanout > maxFanout){
				if(!fanoutMap.containsKey(fanout)) fanoutMap.put(fanout, 0);
				fanoutMap.put(fanout, fanoutMap.get(fanout) + 1);
			}else{
				numNets += 1;
			}
		}
		boolean notEmpty = true;
		int min = maxFanout;
		int max = maxFanout * 2;
		while(notEmpty){
			notEmpty = false;
			int counter = 0;
			for(Integer key:fanoutMap.keySet()){
				if(key >= min && key < max){
					counter += fanoutMap.get(key);
				}else if(key >= max){
					notEmpty = true;
				}
			}
			System.out.println("\t" + counter + " nets with fanout between " + min + " and " + max);
			min = max;
			max *= 2;
		}
		System.out.println();

		//Make all objects
		this.blocks = new Block[legalX.length];
		this.nets = new Net[numNets];

		int l = 0;
		for(int i = 0; i < placerNets.size(); i++){
			int fanout = placerNets.get(i).blocks.length;
			if(fanout > maxFanout){
				//Do Nothing
			}else{
				double netWeight = AnalyticalAndGradientPlacer.getWeight(fanout);
				this.nets[l] = new Net(l, netWeight);
				l++;
			}
		}
		for(int i = 0; i < legalX.length; i++){
			float offset = (1 - heights[i]) / 2f;
			this.blocks[i] = new Block(i, offset);
		}

		//Connect objects
		l = 0;
		for(int i = 0; i < placerNets.size(); i++){
			int fanout = placerNets.get(i).blocks.length;
			if(fanout > maxFanout){
				//Do nothing
			}else{
				Net legalizerNet = this.nets[l];
				for(NetBlock block:placerNets.get(i).blocks){
					Block legalizerBlock = this.blocks[block.blockIndex];

					legalizerNet.addBlock(legalizerBlock);
					legalizerBlock.addNet(legalizerNet);
				}
				l++;
			}
		}

		//Finish
		for(Net net:this.nets){
			net.finish();
		}

		this.timingTree.time("Initialize Hardblock Connection Legalizer Data");

		this.crits = new ArrayList<>();

		this.blocksPerBlocktype = new HashMap<>();
		this.netsPerBlocktype = new HashMap<>();

		this.columnSwap = new HardblockColumnSwap();
		this.hardblockAnneal = new HardblockAnneal(100);
	}

	//ADD BLOCK TYPE
	public void addBlocktype(BlockType blockType, int firstBlockIndex, int lastBlockIndex){
		Block[] legalizeBlocks = this.getLegalizeBlocks(firstBlockIndex, lastBlockIndex);
		Net[] legalizeNets = this.getLegalizeNets(legalizeBlocks);

		this.blocksPerBlocktype.put(blockType, legalizeBlocks);
		this.netsPerBlocktype.put(blockType, legalizeNets);
	}
	private Block[] getLegalizeBlocks(int firstBlockIndex, int lastBlockIndex){
		Block[] legalizeBlocks = new Block[lastBlockIndex - firstBlockIndex];
		for(int i = firstBlockIndex; i < lastBlockIndex; i++){
			Block legalizeBlock = this.blocks[i];
			legalizeBlocks[i - firstBlockIndex] = legalizeBlock;
			
	        //Offset test -> hard blocks have no offset
	        if(legalizeBlock.offset != 0){
	        	System.out.println("The offset of hard block is equal to " + legalizeBlock.offset + ", should be 0");
	        }
		}
		return legalizeBlocks;
	}
	private Net[] getLegalizeNets(Block[] legalizeBlocks){
		Set<Net> hardblockNets = new HashSet<Net>();
		for(Block block:legalizeBlocks){
			for(Net net:block.nets){
				hardblockNets.add(net);
			}
		}
		return hardblockNets.toArray(new Net[hardblockNets.size()]);
	}

	//UPDATE CRITICAL CONNECTIONS
	public void updateCriticalConnections(List<CritConn> criticalConnections){
		this.timingTree.start("Update critical connections in hardblock connection legalizer");
		
		//Clear all data
		for(Block block:this.blocks){
			block.crits.clear();
		}
		this.crits.clear();
		
		int index = 0;
		
		for(CritConn critConn:criticalConnections){
        	Block sourceBlock = this.blocks[critConn.sourceIndex];
        	Block sinkBlock = this.blocks[critConn.sinkIndex];
        	
        	Crit conn = new Crit(sourceBlock, sinkBlock, index, critConn.weight);
        	sourceBlock.addCrit(conn);
        	sinkBlock.addCrit(conn);
        	
        	this.crits.add(conn);
        	
        	index++;
		}
		
		this.timingTree.time("Update critical connections in hardblock connection legalizer");
	}
	
	//Legalize hard block
	public void legalizeHardblock(BlockType blockType, int firstColumn, int columnRepeat, int blockHeight, double quality){
		this.timingTree.start("Legalize " + blockType + " hardblock");
		
		this.blockType = blockType;
		
		int firstRow = 1;
		int rowRepeat = blockHeight;

        int numColumns = (int) Math.floor((this.gridWidth - firstColumn) / columnRepeat + 1);
        int numRows = (int) Math.floor(this.gridHeigth / rowRepeat);
        
		Block[] legalizeBlocks = this.blocksPerBlocktype.get(this.blockType);
		Net[] legalizeNets = this.netsPerBlocktype.get(this.blockType);
		Column[] columns = new Column[numColumns];
		for(int c = 0; c < numColumns; c++){
			int column = firstColumn + c * columnRepeat;
			
			Site[] sites = new Site[numRows];
			for(int r = 0; r < numRows; r++){
				int row = firstRow + r * rowRepeat;
				sites[r] = new Site(column, row, this.blockType.getHeight());
			}
			columns[c] = new Column(c, column, sites);
		}
		
		//Update the coordinates off all blocks based on the current legal placement
		this.initializeLegalization(legalizeNets);

		this.timingTree.start("Find best legal coordinates for all blocks based on minimal displacement cost");
		
		//Update the coordinates of the current hard block type based on the minimal displacement from current linear placement
		for(Block block:legalizeBlocks){
			int columnIndex = (int) Math.round(Math.max(Math.min((block.linearX - firstColumn) / columnRepeat, numColumns - 1), 0));
			int rowIndex = (int) Math.round(Math.max(Math.min((block.linearY - firstRow) / rowRepeat, numRows - 1), 0));

			block.setLegal(firstColumn + columnIndex * columnRepeat, firstRow + rowIndex * rowRepeat);
			columns[columnIndex].addBlock(block);
		}

		this.timingTree.time("Find best legal coordinates for all blocks based on minimal displacement cost");

		//Column swap
		this.timingTree.start("Column swap");
		this.columnSwap.doSwap(columns);
		this.timingTree.time("Column swap");

		//Column legalize
		this.timingTree.start("Legalize columns");
		for(Column column:columns){
			column.legalize();
		}
		this.timingTree.time("Legalize columns");

		//Column anneal
		this.timingTree.start("Anneal columns");
		for(Column column:columns){
			if(column.usedPos() > 0){
				this.hardblockAnneal.doAnneal(column, quality);
			}
		}
		this.timingTree.time("Anneal columns");

		//Finish
		this.updateLegal(legalizeBlocks);
		this.cleanData();
		
		this.timingTree.time("Legalize " + blockType + " hardblock");
	}
	
	//Legalize IO block
	public void legalizeIO(BlockType blockType, double quality){
		this.timingTree.start("Legalize IO");
		
		this.blockType = blockType;
		
		int siteCapacity = this.circuit.getArchitecture().getIoCapacity();
		
		Block[] legalizeBlocks = this.blocksPerBlocktype.get(this.blockType);
		Net[] legalizeNets = this.netsPerBlocktype.get(this.blockType);
		Site[] legalizeSites = new Site[2 * (this.gridWidth + this.gridHeigth) * siteCapacity];
		int l = 0;
		for(int i = 1; i <= this.gridWidth; i++){
			for(int p = 0; p < siteCapacity; p++){
				legalizeSites[l++] = new Site(i, 0, this.blockType.getHeight());
				legalizeSites[l++] = new Site(i, this.gridHeigth + 1, this.blockType.getHeight());
			}
		}
		for(int i = 1; i <= this.gridHeigth; i++){
			for(int p = 0; p < siteCapacity; p++){
				legalizeSites[l++] = new Site(0, i, this.blockType.getHeight());
				legalizeSites[l++] = new Site(this.gridWidth + 1, i, this.blockType.getHeight());
			}
		}
		
		//Update the coordinates of the current hard block type based on the current linear placement
		this.initializeLegalization(legalizeNets);
		
		this.timingTree.start("Find best site for all IO blocks based on minimal displacement cost");
		
		//Update the coordinates of the io blocks based on the minimal displacement from current linear placement
		for(Block block:legalizeBlocks){
			
			double minimumCost = Double.MAX_VALUE;
			Site bestFreeSite = null;
			
			for(Site site:legalizeSites){
				if(!site.hasBlock()){
					double cost = (site.column - block.linearX) * (site.column - block.linearX) + (site.row - block.linearY) * (site.row - block.linearY);
					if(cost < minimumCost){
						minimumCost = cost;
						bestFreeSite = site;
					}
				}
			}
			block.setSite(bestFreeSite);
			bestFreeSite.setBlock(block);
			
			block.setLegal(bestFreeSite.column, bestFreeSite.row);
		}
		this.timingTree.time("Find best site for all IO blocks based on minimal displacement cost");

		//Anneal the IOs to find a good placement
		this.hardblockAnneal.doAnneal(legalizeBlocks, legalizeSites, quality);
		
		this.updateLegal(legalizeBlocks);
		this.cleanData();
		
		this.timingTree.time("Legalize IO");
	}
	private void initializeLegalization(Net[] legalizeNets){
		this.timingTree.start("Update block coordinates");
		
        for(Block block:this.blocks){
        	block.legalX = this.legalX[block.index];
        	block.legalY = this.legalY[block.index];
        	block.linearX = this.linearX[block.index];
        	block.linearY = this.linearY[block.index];
        }
        for(Net net:legalizeNets){
        	net.initializeConnectionCost();
        }
        for(Crit crit:this.crits){
        	crit.initializeTimingCost();
        }
        
        this.timingTree.time("Update block coordinates");
	}
    private void updateLegal(Block[] legalizeBlocks){
    	this.timingTree.start("Update legal");
    	
    	for(Block block:legalizeBlocks){
    		this.legalX[block.index] = block.legalX;
    		this.legalY[block.index] = block.legalY;
    	}
    	
    	this.timingTree.time("Update legal");
    }
    private void cleanData(){
    	this.timingTree.start("Clean data");
    	
    	for(Block block:this.blocks){
    		block.site = null;
    	}
    	
    	this.timingTree.time("Clean data");
    }
	
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// BLOCK ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Block{
		final int index;
		final float offset;

		int legalX, legalY;
		int oldLegalX, oldLegalY;
		double linearX, linearY;
		boolean alreadySaved;

		final List<Net> nets;
		final List<Crit> crits;

		double criticality;
		
		Site site;
		Column column;
		
		Site optimalSite;

		Block(int index, float offset){
			this.index = index;
			this.offset = offset;
			
			this.alreadySaved = false;

			this.nets = new ArrayList<Net>();
			this.crits = new ArrayList<Crit>();

			this.site = null;
			this.column = null;
		}
		void addNet(Net net){
			if(!this.nets.contains(net)){
				this.nets.add(net);
			}
		}
		void addCrit(Crit crit){
			if(!this.crits.contains(crit)){
				this.crits.add(crit);
			}
		}
		
		//Criticality
		void updateCriticality(){
			this.criticality = 0.0;
			for(Net net:this.nets) this.criticality += net.weight;
			for(Crit crit:this.crits) this.criticality += crit.weight;
		}

		//// Cost ////
		double cost(){
			double cost = 0.0;
			for(Net net:this.nets) cost += net.connectionCost();
			for(Crit crit:this.crits) cost += crit.timingCost();
			return cost;
		}
		double horizontalCost(){
			double cost = 0.0;
			for(Net net:this.nets) cost += net.horizontalConnectionCost();
			for(Crit crit:this.crits) cost += crit.horizontalTimingCost();
			return cost;
		}
		double verticalCost(){
			double cost = 0.0;
			for(Net net:this.nets) cost += net.verticalConnectionCost();
			for(Crit crit:this.crits) cost += crit.verticalTimingCost();
			return cost;
		}

		void setLegal(int newLegalX, int newLegalY){
			this.tryLegal(newLegalX, newLegalY);
			this.pushTrough();
		}
		void setLegalX(int newLegalX){
			this.tryLegalX(newLegalX);
			this.pushTrough();
		}
		void setLegalY(int newLegalY){
			this.tryLegalY(newLegalY);
			this.pushTrough();
		}
		
		
		void pushTrough(){
			this.alreadySaved = false;
			
			for(Net net:this.nets) net.pushTrough();
			for(Crit crit:this.crits) crit.pushTrough();
		}
		void revert(){
			if(this.alreadySaved){
				this.legalX = this.oldLegalX;
				this.legalY = this.oldLegalY;
				this.alreadySaved = false;
						
				for(Net net:this.nets) net.revert();
				for(Crit crit:this.crits) crit.revert();
			}
		}

		void saveState(){
			if(!this.alreadySaved){
				this.oldLegalX = this.legalX;
				this.oldLegalY = this.legalY;
				this.alreadySaved = true;
			}
		}
		void tryLegal(int newLegalX, int newLegalY){
			this.tryLegalX(newLegalX);
			this.tryLegalY(newLegalY);
		}
		void tryLegalX(int newLegalX){
			this.saveState();
			
			if(this.legalX != newLegalX){
				this.legalX = newLegalX;
				for(Net net:this.nets) net.tryHorizontalConnectionCost(this.oldLegalX, this.legalX);
				for(Crit crit:this.crits) crit.tryHorizontalTimingCost();
			}
		}
		void tryLegalY(int newLegalY){
			this.saveState();
			
			if(this.legalY != newLegalY){
				this.legalY = newLegalY;
				for(Net net:this.nets) net.tryVerticalConnectionCost(this.oldLegalY, this.legalY);
				for(Crit crit:this.crits) crit.tryVerticalTimingCost();
			}
		}

    	//Site
    	void setSite(Site site){
    		this.site = site;
    	}
    	boolean hasSite(){
    		return this.site != null;
    	}
    	Site getSite(){
    		return this.site;
    	}
    	
    	//Optimal site
    	void initializeOptimalSite(){
    		this.optimalSite = this.site;
    	}
    	void saveOptimalSite(){
    		this.optimalSite = this.site;
    	}
    	void setOptimalSite(){
    		this.site = this.optimalSite;
    		this.setLegal(this.site.column, this.site.row);
    		this.site.setBlock(this);
    	}
    	
	    @Override
	    public int hashCode() {
	        return 17 + 31 * this.index;
	    }
	}

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// SITE //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Site {
	    final int column, row, height;
	    Block block;

	    public Site(int column, int row, int height) {
	        this.column = column;
	        this.row = row;
	        this.height = height;
	        
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
	class Net{
		final int index;
		final double weight;

		Block[] blocks;
		final List<Block> tempBlocks;

		int size;

		int minX, maxX;
		int minY, maxY;
		int oldMinX, oldMaxX;
		int oldMinY, oldMaxY;
		boolean alreadySaved;

		boolean horizontalChange, verticalChange;
		boolean horizontalDeltaCostIncluded, verticalDeltaCostIncluded;

		Net(int index, double netWeight){
			this.index = index;
			this.tempBlocks = new ArrayList<Block>();

			this.weight = netWeight;

			this.alreadySaved = false;
			this.horizontalChange = false;
			this.verticalChange = false;
			this.horizontalDeltaCostIncluded = false;
			this.verticalDeltaCostIncluded = false;

			this.size = 0;
		}
		void addBlock(Block block){
			if(!this.tempBlocks.contains(block)){
				this.tempBlocks.add(block);
				this.size++;
			}
		}
		void finish(){
			this.blocks = new Block[this.size];
			this.tempBlocks.toArray(this.blocks);
		}

		//// Connection cost ////
		void initializeConnectionCost(){
			Block initialBlock = this.blocks[0];
	        this.minX = initialBlock.legalX;
	        this.maxX = initialBlock.legalX;
	        
	        this.minY = initialBlock.legalY;
	        this.maxY = initialBlock.legalY;
	        
	        for(Block block:this.blocks){
	            if(block.legalX < this.minX) {
	                this.minX = block.legalX;
	            }else if(block.legalX > this.maxX){
	            	this.maxX = block.legalX;
	            }
	            if(block.legalY < this.minY) {
	                this.minY = block.legalY;
	            }else if(block.legalY > this.maxY){
	            	this.maxY = block.legalY;
	            }
	        }
		}
		
		double connectionCost(){
			return this.horizontalConnectionCost() + this.verticalConnectionCost();
		}
		double horizontalConnectionCost(){
			return (this.maxX - this.minX + 1) * this.weight;
		}
		double verticalConnectionCost(){
			return (this.maxY - this.minY + 1) * this.weight;
		}
		
		void revert(){
			if(this.alreadySaved){
				if(this.horizontalChange){
					this.minX = this.oldMinX;
					this.maxX = this.oldMaxX;
					this.horizontalChange = false;
				}
				if(this.verticalChange){
					this.minY = this.oldMinY;
					this.maxY = this.oldMaxY;
					this.verticalChange = false;
				}
				
				this.alreadySaved = false;
			}
		}
		void pushTrough(){
			this.alreadySaved = false;
		}
		
		void saveState(){
			if(!this.alreadySaved){
				this.oldMinX = this.minX;
				this.oldMaxX = this.maxX;
				this.oldMinY = this.minY;
				this.oldMaxY = this.maxY;
				
				this.alreadySaved = true;
			}
		}
		// Horizontal
		void tryHorizontalConnectionCost(int oldX, int newX){
			this.saveState();
			
			if(this.size == 1){
				this.minX = this.blocks[0].legalX;
				this.maxX = this.blocks[0].legalX;
			}else if(this.size == 2){
				int l1 = this.blocks[0].legalX;
				int l2 = this.blocks[1].legalX;

				if(l1 < l2){
					this.minX = l1;
					this.maxX = l2;
				}else{
					this.maxX = l1;
					this.minX = l2;
				}
			}else{
				this.updateMinX(oldX, newX);
				this.updateMaxX(oldX, newX);
			}

            if(this.minX != this.oldMinX || this.maxX != this.oldMaxX){
            	this.horizontalChange = true;
            	this.horizontalDeltaCostIncluded = false;
            }else{
            	this.horizontalChange = false;
            	this.horizontalDeltaCostIncluded = true;
            }
		}
		double deltaHorizontalConnectionCost(){
			this.horizontalDeltaCostIncluded = true;
			return (this.maxX - this.minX - this.oldMaxX + this.oldMinX) * this.weight;
		}
		void updateMinX(int oldX, int newX){
			if(newX <= this.minX){
            	this.minX = newX;
			}else if(oldX == this.minX){
            	this.minX = this.getMinX();
            }
		}
		void updateMaxX(int oldX, int newX){
			if(newX >= this.maxX){
            	this.maxX = newX;
            }else if(oldX == this.maxX){
            	this.maxX = this.getMaxX();
            }
		}
		int getMinX(){
	        int minX = this.blocks[0].legalX;
	        for(Block block:this.blocks){
	            if(block.legalX < minX) {
	                minX = block.legalX;
	            }
	        }
	        return minX;
		}
		int getMaxX(){
	        int maxX = this.blocks[0].legalX;
	        for(Block block:this.blocks){
	            if(block.legalX > maxX) {
	                maxX = block.legalX;
	            }
	        }
	        return maxX;
		}

		// Vertical
		void tryVerticalConnectionCost(int oldY, int newY){
			this.saveState();
			
			if(this.size == 1){
				this.minY = this.blocks[0].legalY;
				this.maxY = this.blocks[0].legalY;
			}else if(this.size == 2){
				int l1 = this.blocks[0].legalY;
				int l2 = this.blocks[1].legalY;

				if(l1 < l2){
					this.minY = l1;
					this.maxY = l2;
				}else{
					this.maxY = l1;
					this.minY = l2;
				}
			}else{
				this.updateMinY(oldY, newY);
				this.updateMaxY(oldY, newY);
			}

            if(this.minY != this.oldMinY || this.maxY != this.oldMaxY){
            	this.verticalChange = true;
            	this.verticalDeltaCostIncluded = false;
            }else{
            	this.verticalChange = false;
            	this.verticalDeltaCostIncluded = true;
            }
        }
		double deltaVerticalConnectionCost(){
			this.verticalDeltaCostIncluded = true;
			return (this.maxY - this.minY - this.oldMaxY + this.oldMinY) * this.weight;
		}
		void updateMinY(int oldY, int newY){
			if(newY <= this.minY){
            	this.minY = newY;
			}else if(oldY == this.minY){
				this.minY = this.getMinY();
            }
		}
		void updateMaxY(int oldY, int newY){
			if(newY >= this.maxY){
            	this.maxY = newY;
            }else if(oldY == this.maxY){
            	this.maxY = this.getMaxY();
            }
		}
		private int getMinY(){
	        int minY = this.blocks[0].legalY;
	        for(Block block:this.blocks){
	            if(block.legalY < minY) {
	                minY = block.legalY;
	            }
	        }
	        return minY;
		}
		private int getMaxY(){
	        int maxY = this.blocks[0].legalY;
	        for(Block block:this.blocks){
	            if(block.legalY > maxY) {
	                maxY = block.legalY;
	            }
	        }
	        return maxY;
		}
		
	    @Override
	    public int hashCode() {
	    	return 17 + 31 * this.index;
	    }
	}

    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// CRITICAL CONNECTION //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Crit {
		Block sourceBlock, sinkBlock;
		double weight;
		
		int index;

		int minX, maxX;
		int minY, maxY;
		int oldMinX, oldMaxX;
		int oldMinY, oldMaxY;
		boolean alreadySaved;
		
		boolean horizontalChange, verticalChange;
		boolean horizontalDeltaCostIncluded, verticalDeltaCostIncluded;

		Crit(Block sourceBlock, Block sinkBlock, int index, double weight){
			this.sourceBlock = sourceBlock;
			this.sinkBlock = sinkBlock;
			this.weight = weight;
			
			this.alreadySaved = false;
			this.horizontalChange = false;
			this.verticalChange = false;
			
			this.index = index;
		}

		void initializeTimingCost(){
			if(this.sourceBlock.legalX < this.sinkBlock.legalX){
				this.minX = this.sourceBlock.legalX;
				this.maxX = this.sinkBlock.legalX;
			}else{
				this.minX = this.sinkBlock.legalX;
				this.maxX = this.sourceBlock.legalX;
			}
			
			if(this.sourceBlock.legalY < this.sinkBlock.legalY){
				this.minY = this.sourceBlock.legalY;
				this.maxY = this.sinkBlock.legalY;
			}else{
				this.minY = this.sinkBlock.legalY;
				this.maxY = this.sourceBlock.legalY;
			}
		}

		void pushTrough(){
			this.alreadySaved = false;
		}
		void revert(){
			if(this.alreadySaved){
				if(this.horizontalChange){
					this.minX = this.oldMinX;
					this.maxX = this.oldMaxX;
					this.horizontalChange = false;
				}
				if(this.verticalChange){
					this.minY = this.oldMinY;
					this.maxY = this.oldMaxY;
					this.verticalChange = false;
				}

				this.alreadySaved = false;
			}
		}


		double timingCost(){
			return this.horizontalTimingCost() + this.verticalTimingCost();
		}
		double horizontalTimingCost(){
			return (this.maxX - this.minX) * this.weight;
		}
		double verticalTimingCost(){
			return (this.maxY - this.minY) * this.weight;
		}

		void saveState(){
			if(!this.alreadySaved){
				this.oldMinX = this.minX;
				this.oldMaxX = this.maxX;
				this.oldMinY = this.minY;
				this.oldMaxY = this.maxY;

				this.alreadySaved = true;
			}
		}

		void tryHorizontalTimingCost(){
			this.saveState();

			if(this.sourceBlock.legalX < this.sinkBlock.legalX){
				this.minX = this.sourceBlock.legalX;
				this.maxX = this.sinkBlock.legalX;
			}else{
				this.minX = this.sinkBlock.legalX;
				this.maxX = this.sourceBlock.legalX;
			}

			if(this.minX != this.oldMinX || this.maxX != this.oldMaxX){
				this.horizontalChange = true;
				this.horizontalDeltaCostIncluded = false;
			}else{
				this.horizontalChange = false;
				this.horizontalDeltaCostIncluded = true;
			}
        }
		double deltaHorizontalTimingCost(){
			this.horizontalDeltaCostIncluded = true;
			return (this.maxX - this.minX - this.oldMaxX + this.oldMinX) * this.weight;
		}

		void tryVerticalTimingCost(){
			this.saveState();
			
			if(this.sourceBlock.legalY < this.sinkBlock.legalY){
				this.minY = this.sourceBlock.legalY;
				this.maxY = this.sinkBlock.legalY;
			}else{
				this.minY = this.sinkBlock.legalY;
				this.maxY = this.sourceBlock.legalY;
			}

			if(this.minY != this.oldMinY || this.maxY != this.oldMaxY){
				this.verticalChange = true;
				this.verticalDeltaCostIncluded = false;
			}else{
				this.verticalChange = false;
				this.verticalDeltaCostIncluded = true;
			}
        }
		double deltaVerticalTimingCost(){
			this.verticalDeltaCostIncluded = true;
			return (this.maxY - this.minY - this.oldMaxY + this.oldMinY) * this.weight;
		}
		
	    @Override
	    public int hashCode() {
	    	return 17 + 31 * this.index;
	    }
	}

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// COLUMN ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Column {
		final int index, coordinate;
		final Set<Block> blocks;
		final Site[] sites;

	    Column(int index, int coordinate, Site[] sites){
	    	this.index = index;
	    	this.coordinate = coordinate;
	    	this.blocks = new HashSet<Block>();
	    	this.sites = sites;
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
			for(Block block:this.blocks){
				block.updateCriticality();
			}

			for(int i = 0; i < this.blocks.size(); i++){
				Block largestCriticalityBlock = null;

				for(Block block:this.blocks){
					if(!block.hasSite()){
						if(largestCriticalityBlock == null){
							largestCriticalityBlock = block;
						}else if(block.criticality > largestCriticalityBlock.criticality){
							largestCriticalityBlock = block;
						}
					}
				}
				Site bestSite = this.getBestFreeSite(largestCriticalityBlock);
				largestCriticalityBlock.setSite(bestSite);
				bestSite.setBlock(largestCriticalityBlock);
				largestCriticalityBlock.setLegalY(bestSite.row);
			}
		}
		Site getBestFreeSite(Block block){
			Site bestSite = null;
			double minimumCost = Double.MAX_VALUE;

			for(Site site:this.sites){
				if(!site.hasBlock()){

					block.tryLegalY(site.row);
					double cost = block.verticalCost();
					block.revert();

					if(cost < minimumCost){
						minimumCost = cost;
						bestSite = site;
					}
				}
			}
			return bestSite;
		}

	    @Override
	    public int hashCode() {
	    	return 17 + 31 * this.index;
	    }
	}
}