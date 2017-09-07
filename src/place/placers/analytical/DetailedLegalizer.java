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
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

class DetailedLegalizer extends Legalizer{
	private final List<Block> blocks;
	private final List<Column> columns;

    DetailedLegalizer(
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
            Map<GlobalBlock, NetBlock> netBlocks,
            Logger logger){

    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, leafNode, visualizer, netBlocks, logger);

    	this.blocks = new ArrayList<>();
        this.columns = new ArrayList<>();
        for(int columnIndex:circuit.getColumnsPerBlockType(BlockType.getBlockTypes(BlockCategory.CLB).get(0))){
        	Column column = new Column(columnIndex, circuit.getHeight());
        	this.columns.add(column);
        }
	}

	@Override
	protected void legalizeBlockType(int blocksStart, int blocksEnd) {
		//Initialize all blocks
		this.blocks.clear();
    	for(int b = blocksStart; b < blocksEnd; b++){
    		double x,y;
    		boolean startFromLegal = false;
    		if(startFromLegal){
    			x = Math.round(this.legalX[b]);
    			y = Math.round(this.legalY[b]);
    		}else{
    			x = Math.round(this.linearX[b]);
    			y = Math.round(this.linearY[b]);
    		}
			
    		int height = this.heights[b];

    		float offset = (1 - height) / 2f;

    		Block block = new Block(b, x, y, offset, height * this.blockType.getHeight());

    		this.blocks.add(block);
    	}
    	Collections.sort(this.blocks, Comparators.VERTICAL);

    	//Clear columns
    	for(Column column:this.columns){
    		column.clear();
    	}

    	//Place blocks
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

    	//Update legal
    	for(Block block:this.blocks){
    		block.processed = false;
    	}
    	for(Column column:this.columns){
    		for(Site site:column.sites){
    			if(site.hasBlock()){
    				Block block = site.block;
    				if(!block.processed){
    					block.x = site.x;
    					block.y = site.y;

    					this.legalX[block.index] = block.x;
    					this.legalY[block.index] = block.y + Math.floor(-block.offset);
            			block.processed = true;
    				}
    			}
    		}
    	}
    }

    class Block {
    	final int index, height;
    	final float offset;
    	double x, y;
    	
    	boolean processed;

    	Block(int index, double x, double y, float offset, int height){
    		this.index = index;

    		this.x = x;
    		this.y = y + Math.ceil(offset);

    		this.offset = offset;
    		this.height = height;
    	}
    	
    	double cost(int legalX, int legalY){
    		return (this.x - legalX) * (this.x - legalX) + (this.y - legalY) * (this.y - legalY);
    	}
    }
    private class Column{
    	final int column, height;

    	final Site[] sites;
    	
    	int usedSize;
    	
    	Site lastUsedSite, oldLastUsedSite;
    	Site firstFreeSite, oldFirstFreeSite;
    	
    	double cost, oldCost;
    	
    	Column(int column, int size){
    		this.column = column;
    		this.height = size;
    		
    		this.sites = new Site[this.height];
    		for(int i = 0; i < this.height; i++){
    			this.sites[i] = new Site(this.column, i + 1);
    		}
    		for(int i = 0; i < this.height; i++){
    			if(i > 0) this.sites[i].previous = this.sites[i - 1];
    			if(i < this.height - 1) this.sites[i].next = this.sites[i + 1];
    		}
    		
    		this.usedSize = 0;
    		this.cost = 0.0;
    	}
    	void clear(){
    		for(Site site:this.sites){
    			site.block = null;
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

    		int optimalY = Math.max(Math.min((int)Math.round(block.y - 1), this.height - 1),  0);
    		
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
    			return Double.compare(b1.y, b2.y);
    		}
    	};
    }
}
