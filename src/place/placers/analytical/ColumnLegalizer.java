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
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

class ColumnLegalizer extends Legalizer {
    private final List<Block> blocks;
	private final List<Column> columns;

	private TimingTree timingTree;

	ColumnLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            PlacementVisualizer visualizer,
            List<Net> nets,
            List<TimingNet> timingNets,
            Map<GlobalBlock, NetBlock> netBlocks) throws IllegalArgumentException {

        super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, nets, timingNets, visualizer, netBlocks);

        this.heights = heights;

        this.timingTree = new TimingTree(false);

        this.blocks = new ArrayList<>();

        this.columns = new ArrayList<>();
        for(int columnIndex:this.circuit.getColumnsPerBlockType(BlockType.getBlockTypes(BlockCategory.CLB).get(0))){
        	Column column = new Column(columnIndex, this.circuit.getHeight());
        	this.columns.add(column);
        }
	}

    @Override
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	this.timingTree.start("Column legalizer");
    	
    	this.timingTree.start("Make sorted blocks");
    	this.blocks.clear();
    	for(int index = blocksStart; index < blocksEnd; index++) {
    		int height = this.heights[index];
    		double weight = 1.0 / height;
    		
    		Block block = new Block(index, this.linearX[index], this.linearY[index], height, weight);
    		this.blocks.add(block);
    	}
    	Collections.sort(this.blocks, Comparators.VERTICAL);
    	this.timingTree.time("Make sorted blocks");
    	
    	this.timingTree.start("Clear columns");
    	for(Column column:this.columns){
    		column.clear();
    	}
    	this.timingTree.time("Clear columns");

    	this.timingTree.start("Place blocks");
    	for(Block block:this.blocks){
    		
    		Column bestColumn = null;
    		double bestCost = Double.MAX_VALUE;
    		
    		for(Column column:this.columns){
    			if(column.usedSize + block.size <= column.size){
        			double cost = column.tryBlock(block);

        			if(cost < bestCost){
        				bestColumn = column;
        				bestCost = cost;
        			}
    			}
    		}
    		bestColumn.addBlock(block);
    	}
    	this.timingTree.time("Place blocks");

    	this.timingTree.start("Update legal");
    	for(Block block:this.blocks){
    		block.processed = false;
    	}
    	for(Column column:this.columns){
    		for(Site site:column.sites){
    			if(site.hasBlock()){
    				if(!site.block.processed){
            			this.legalX[site.block.index] = site.x;
            			this.legalY[site.block.index] = site.y;
            			site.block.processed = true;
    				}
    			}
    		}
    	}
    	this.timingTree.time("Update legal");
    	
    	this.timingTree.time("Column legalizer");
    }
    private class Column{
    	final int column;
    	final int size;

    	final Site[] sites;
    	
    	int usedSize;
    	
    	Site lastUsedSite, oldLastUsedSite;
    	Site firstFreeSite, oldFirstFreeSite;
    	
    	double cost, oldCost;
    	
    	Column(int column, int size){
    		this.column = column;
    		this.size = size;
    		
    		this.sites = new Site[this.size];
    		for(int i = 0; i < this.size; i++){
    			this.sites[i] = new Site(this.column, i + 1);
    		}
    		for(int i = 0; i < this.size; i++){
    			if(i > 0) this.sites[i].previous = this.sites[i - 1];
    			if(i < this.size - 1) this.sites[i].next = this.sites[i + 1];
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
    		this.usedSize += block.size;

    		int optimalY = Math.max(Math.min((int)Math.round(block.linearY - 1), this.size - 1),  0);
    		
    		Site bestSite = this.sites[optimalY];
    		if(bestSite.hasBlock()){
        		Site currentSite = this.lastUsedSite;
        		for(int s = 0; s < block.size; s++){
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
        		for(int s = 0; s < block.size - 1; s++){
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
    		for(int s = 0; s < block.size; s++){
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
    		this.usedSize -= block.size;
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
    private class Block {
  	   int index;

  	   double linearX, linearY;
  	  
  	   int size;
  	   double weight;
  	   
  	   boolean processed;
      	
  	   Block(int index, double linearX, double linearY, int width, double weight){
  		   this.index = index;
  		   this.linearX = linearX;
  		   this.linearY = linearY;
  		   this.size = width;
  		   this.weight = weight;
  	   }
  	   double cost(int legalX, int legalY){
  		   return this.weight * ((this.linearX - legalX) * (this.linearX - legalX) + (this.linearY - legalY) * (this.linearY - legalY));
  	   }
    }
    private class Site {
    	final int x,y;
    	
    	Block block, oldBlock;
    	
    	Site previous;
    	Site next;
    	
    	Site(int x, int y){
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
 			   return Double.compare(b1.linearY, b2.linearY);
 		   }
 	   };
    }
}
