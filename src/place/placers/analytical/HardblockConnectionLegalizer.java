package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.placers.analytical.GradientPlacerTD.CriticalConnection;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

public class HardblockConnectionLegalizer{
	private BlockType blockType;

	private final double[] linearX, linearY;
    private final int[] legalX, legalY;

    private final Block[] blocks;
    private final Net[] nets;

    private final List<Crit> critConn;

    private final ColumnSwap columnSwap;
    private final HardblockAnneal hardblockAnneal;
    private final ColumnPlacer columnPlacer;
    
    private final Map<BlockType, Block[]> blocksPerBlocktype;
    private final Map<BlockType, Net[]> netsPerBlocktype;

    private final int gridWidth, gridHeigth;
    
    private final TimingTree timingTree;
    
    //Visualizer
    private static final boolean doVisual = true;
    private final PlacementVisualizer visualizer;
    private final Map<GlobalBlock, NetBlock> netBlocks;
    private final double[] visualX, visualY;

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
			PlacementVisualizer visualizer,
			Map<GlobalBlock, NetBlock> blockIndexes){

		this.timingTree = new TimingTree(true);
		
		this.timingTree.start("Initialize Hardblock Connection Legalizer Data");
		
		this.linearX = linearX;
		this.linearY = linearY;

		this.legalX = legalX;
		this.legalY = legalY;
		
		this.gridWidth = gridWidth;
		this.gridHeigth = gridHeight;

		//Count the number of nets
		System.out.println("Nets with large fanout:");
		int maxFanout = 100;
		int numNets = 0;
		for(int i = 0; i < placerNets.size(); i++){
			int fanout = placerNets.get(i).blocks.length;
			if(fanout > maxFanout){
				System.out.println("\tNet with fanout " + fanout + " is left out");
			}else{
				numNets += 1;
			}
		}

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
		
		this.timingTree.time("Initialize Hardblock Connection Legalizer Data");
		
		this.critConn = new ArrayList<>();
		
		this.blocksPerBlocktype = new HashMap<>();
		this.netsPerBlocktype = new HashMap<>();
		
		this.visualizer = visualizer;
		this.netBlocks = blockIndexes;
		this.visualX = new double[this.linearX.length];
		this.visualY = new double[this.linearY.length];
		
		this.columnSwap = new ColumnSwap(this.timingTree);
		this.hardblockAnneal = new HardblockAnneal(this.timingTree, 100);
		this.columnPlacer = new ColumnPlacer(this.timingTree, this.blocks, this.visualizer, this.netBlocks, doVisual);
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
	        	System.out.println("The offset of hard  block is equal to " + legalizeBlock.offset + ", should be 0");
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
	public void updateCriticalConnections(List<CriticalConnection> criticalConnections){
		this.timingTree.start("Update critical connections in hardblock connection legalizer");
		
		//Clear all data
		for(Block block:this.blocks){
			block.criticalConnections.clear();
		}
		this.critConn.clear();
		
		for(CriticalConnection critConn:criticalConnections){
        	Block sourceBlock = this.blocks[critConn.sourceIndex];
        	Block sinkBlock = this.blocks[critConn.sinkIndex];
        	
        	Crit conn = new Crit(sourceBlock, sinkBlock, critConn.weight);
        	sourceBlock.criticalConnections.add(conn);
        	sinkBlock.criticalConnections.add(conn);
        	
        	this.critConn.add(conn);
		}
		
		this.timingTree.time("Update critical connections in hardblock connection legalizer");
	}
	
	//Legalize hard block
	public void legalizeHardblock(BlockType blockType, int firstColumn, int columnRepeat, int blockHeight){
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
				sites[r] = new Site(column, row);
			}
			columns[c] = new Column(c, column, sites);
		}
		
		//Update the coordinates off all blocks based on the current legal placement
		this.updateBlockCoordinates();

		this.timingTree.start("Find best legal coordinates for all blocks based on minimal displacement cost");
		
		this.addVisual(this.blockType + " => Before best position for each hard block");
		
		//Update the coordinates of the current hard block type based on the minimal displacement from current linear placement
		for(Block block:legalizeBlocks){
			double linearX = this.linearX[block.index];
			double linearY = this.linearY[block.index];
			int columnIndex = (int) Math.round(Math.max(Math.min((linearX - firstColumn) / columnRepeat, numColumns - 1), 0));
			int rowIndex = (int) Math.round(Math.max(Math.min((linearY - firstRow) / rowRepeat, numRows - 1), 0));

			block.setCoordinates(firstColumn + columnIndex * columnRepeat, firstRow + rowIndex * rowRepeat);
			columns[columnIndex].addBlock(block);
		}
		
		this.addVisual(this.blockType + " => After best position for each hard block");
		
		this.timingTree.time("Find best legal coordinates for all blocks based on minimal displacement cost");
		
		//All blocks have a coordinate, initialize the connection cost based on the coordinates
		this.initializeConnectionCost(legalizeBlocks, legalizeNets);

		this.columnSwap.doSwap(columns);
		
		this.addVisual("After column swap");
		
		boolean annealNotColumnPlacer = false;
		if(annealNotColumnPlacer){
			this.timingTree.start("Legalize columns");
			for(Column column:columns){
				column.legalize();
			}
			this.timingTree.time("Legalize columns");
			
			this.addVisual("After legalize");
			
			this.timingTree.start("Anneal columns");
			for(Column column:columns){
				if(column.usedPos() > 0){
					this.hardblockAnneal.doAnneal(column);
				}
			}
			this.timingTree.time("Anneal columns");
		}else{
			this.columnPlacer.doPlacement(legalizeBlocks, legalizeNets, this.critConn, 1, this.gridHeigth - blockHeight);
		}
		
		this.addVisual(this.blockType + " => After column placement");
		
		this.updateLegal();
		this.cleanData();
		
		this.timingTree.time("Legalize " + blockType + " hardblock");
	}
	
	//Legalize IO block
	public void legalizeIO(BlockType blockType){
		this.timingTree.start("Legalize IO");
		
		this.blockType = blockType;
		
		int siteCapacity = 2;
		
		Block[] legalizeBlocks = this.blocksPerBlocktype.get(this.blockType);
		Net[] legalizeNets = this.netsPerBlocktype.get(this.blockType);
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
		
		//Update the coordinates of the current hard block type based on the current linear placement
		this.updateBlockCoordinates();
		
		this.timingTree.start("Find best site for all IO blocks based on minimal displacement cost");
		
		//Update the coordinates of the io blocks based on the minimal displacement from current linear placement
		for(Block block:legalizeBlocks){
			double linearX = this.linearX[block.index];
			double linearY = this.linearY[block.index];
			
			double minimumCost = Double.MAX_VALUE;
			Site bestFreeSite = null;
			
			for(Site site:legalizeSites){
				if(!site.hasBlock()){
					
					block.setCoordinates(site.column, site.row);
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
		
		this.timingTree.time("Find best site for all IO blocks based on minimal displacement cost");

		//All blocks have a coordinate, initialize the connection cost based on the coordinates
		this.initializeConnectionCost(legalizeBlocks, legalizeNets);

		//Anneal the IOs to find a good placement
		this.hardblockAnneal.doAnneal(legalizeBlocks, legalizeSites);
		
		this.updateLegal();
		this.cleanData();
		
		this.timingTree.time("Legalize IO");
	}
	private void updateBlockCoordinates(){
		this.timingTree.start("Update block coordinates");
		
        for(Block block:this.blocks){
        	block.setCoordinates(this.legalX[block.index], this.legalY[block.index]);
        }
        
        this.timingTree.time("Update block coordinates");
	}
	private void initializeConnectionCost(Block[] legalizeBlocks, Net[] legalizeNets){
		this.timingTree.start("Initialize connection cost");

		this.timingTree.start("Update nets");
		for(Net net:legalizeNets){
			net.initializeConnectionCost();
		}
		this.timingTree.time("Update nets");
		
		this.timingTree.start("Update blocks");
		for(Block block:legalizeBlocks){
			block.initializeConnectionCost();
		}
		this.timingTree.time("Update blocks");

		this.timingTree.time("Initialize connection cost");
	}
    private void updateLegal(){
    	this.timingTree.start("Update legal");
    	
    	for(Block block:this.blocks){
    		this.legalX[block.index] = block.x;
    		this.legalY[block.index] = block.y;
    	}
    	
    	this.timingTree.time("Update legal");
    }
    private void cleanData(){
    	this.timingTree.start("Clean data");
    	
    	for(Block block:this.blocks){
    		block.reset();
    	}
    	
    	this.timingTree.time("Clean data");
    }
	
    /////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// BLOCK ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
	class Block{
		final int index;
		final float offset;
		int x, y;
		Site site;

		final List<Net> nets;
		final List<Crit> criticalConnections;

		double connectionCost;
		
		Column column;

		Block(int index, float offset){
			this.index = index;
			this.offset = offset;

			this.nets = new ArrayList<Net>();
			this.criticalConnections = new ArrayList<Crit>();

			this.site = null;
		}
		void addNet(Net net){
			if(!this.nets.contains(net)){
				this.nets.add(net);
			}
		}

		double criticality(){
			double criticality = 0;
			for(Net net:this.nets){
				criticality += net.netWeight;
			}
			for(Crit crit:this.criticalConnections){
				criticality += crit.weight;
			}
			return criticality;
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
			for(Crit conn:this.criticalConnections){
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

			for(Crit conn:this.criticalConnections){
				cost += conn.timingCost();
			}
			
			return cost;
		}
		void updateConnectionCost(int oldX, int newX, int oldY, int newY){
			this.connectionCost = 0.0;

			for(Net net:this.nets){
				this.connectionCost += net.updateHorizontalConnectionCost(oldX, newX);
				this.connectionCost += net.updateVerticalConnectionCost(oldY, newY);
			}
			for(Crit conn:this.criticalConnections){
				this.connectionCost += conn.timingCost();
			}
		}
		void updateConnectionCost(){
			this.connectionCost = 0.0;

			for(Net net:this.nets){
				this.connectionCost += net.updateHorizontalConnectionCost();
				this.connectionCost += net.updateVerticalConnectionCost();
			}
			for(Crit conn:this.criticalConnections){
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
	class Site {
	    final int column, row;
	    Block block;

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
	class Net{
		final int index;
		final List<Block> blocks;
		final double netWeight;
		
		int minX, maxX;
		int minY, maxY;
		
		int tempMinX, tempMaxX;
		int tempMinY, tempMaxY;
		
		double horizontalConnectionCost;
		double verticalConnectionCost;
		
		boolean checkHorizontalChange;
		boolean horizontalChange;
		boolean checkVerticalChange;
		boolean verticalChange;

		Net(int index, double netWeight){
			this.index = index;
			this.blocks = new ArrayList<Block>();
			
			this.netWeight = netWeight;
			
			this.checkHorizontalChange = false;
			this.checkVerticalChange = false;
			this.horizontalChange = false;
			this.verticalChange = false;
		}
		void addBlock(Block block){
			if(!this.blocks.contains(block)){
				this.blocks.add(block);
			}
		}
		
		void checkForHorizontalChange(){
			this.checkHorizontalChange = true;
			this.horizontalChange = false;
		}
		void checkForVerticalChange(){
			this.checkVerticalChange = true;
			this.verticalChange = false;
		}

		//// Connection cost ////
		void initializeConnectionCost(){
			Block initialBlock = this.blocks.get(0);
	        this.minX = initialBlock.x;
	        this.maxX = initialBlock.x;
	        
	        this.minY = initialBlock.y;
	        this.maxY = initialBlock.y;
	        
	        for(int i = 1; i < this.blocks.size(); i++){
	        	Block block = this.blocks.get(i);
	            if(block.x < this.minX) {
	                this.minX = block.x;
	            }else if(block.x > this.maxX){
	            	this.maxX = block.x;
	            }
	            if(block.y < this.minY) {
	                this.minY = block.y;
	            }else if(block.y > this.maxY){
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
		double updateHorizontalConnectionCost(int oldX, int newX){
            this.updateTempMinX(oldX, newX);
            this.updateTempMaxX(oldX, newX);
            
            if(this.checkHorizontalChange){
                if(this.minX != this.tempMinX){
                	this.horizontalChange = true;
                }else if(this.maxX != this.tempMaxX){
                	this.horizontalChange = true;
                }
                this.checkHorizontalChange = false;
            }

			this.minX = this.tempMinX;
			this.maxX = this.tempMaxX;
			
			this.horizontalConnectionCost = (this.maxX - this.minX + 1) * this.netWeight;
			
			return this.horizontalConnectionCost;
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
	        for(int i = 1; i < this.blocks.size(); i++){
	        	Block block = this.blocks.get(i);
	            if(block.x < minX) {
	                minX = block.x;
	            }
	        }
	        return minX;
		}
		int getMaxX(){
			Block initialBlock = this.blocks.get(0);
	        int maxX = initialBlock.x;
	        for(int i = 1; i < this.blocks.size(); i++){
	        	Block block = this.blocks.get(i);
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
		double updateVerticalConnectionCost(int oldY, int newY){
			this.updateTempMinY(oldY, newY);
            this.updateTempMaxY(oldY, newY);
            
            if(this.checkVerticalChange){
                if(this.minY != this.tempMinY){
                	this.verticalChange = true;
                }else if(this.maxY != this.tempMaxY){
                	this.verticalChange = true;
                }
                this.checkVerticalChange = false;
            }
            
			this.minY = this.tempMinY;
			this.maxY = this.tempMaxY;
			
			this.verticalConnectionCost = (this.maxY - this.minY + 1) * this.netWeight;
			
			return this.verticalConnectionCost;
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
	        for(int i = 1; i < this.blocks.size(); i++){
	        	Block block = this.blocks.get(i);
	            if(block.y < minY) {
	                minY = block.y;
	            }
	        }
	        return minY;
		}
		private int getMaxY(){
			Block initialBlock = this.blocks.get(0);
	        int maxY = initialBlock.y;
	        for(int i = 1; i < this.blocks.size(); i++){
	        	Block block = this.blocks.get(i);
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
	class Crit {
		Block sourceBlock, sinkBlock;
		double weight;
		
		Crit(Block sourceBlock, Block sinkBlock, double weight){
			this.sourceBlock = sourceBlock;
			this.sinkBlock = sinkBlock;
			this.weight = weight;
		}
		int manhattanDistance(){
			return Math.abs(this.sourceBlock.x - this.sinkBlock.x) + Math.abs(this.sourceBlock.y - this.sinkBlock.y);
		}
		double timingCost(){
			return this.weight * this.manhattanDistance();
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
			if(this.usedPos() > this.numPos()){
				System.out.println("To many blocks in column to legalize");
			}else{
				for(Block block:this.blocks){
					int origX = block.x;
					int origY = block.y;
					
					Site site = this.getBestFreeSite(block);
					block.setSite(site);
					site.setBlock(block);
					
					block.updateConnectionCost(origX, site.column, origY, site.row);
				}
			}
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
    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// COLUMN ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    protected void addVisual(String name){
    	if(doVisual){
        	for(Block block:this.blocks){
        		this.visualX[block.index] = block.x;
        		this.visualY[block.index] = block.y;
        	}
        	this.visualizer.addPlacement(name, this.netBlocks, this.visualX, this.visualY, -1);
    	}
    }
}