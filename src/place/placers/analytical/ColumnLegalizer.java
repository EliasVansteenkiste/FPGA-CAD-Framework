package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
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
            Map<GlobalBlock, NetBlock> blockIndexes) throws IllegalArgumentException {

        super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, visualizer, blockIndexes);


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
    		double weight = 1.0;
    		
    		Block block = new Block(index, this.linearX[index], this.linearY[index], this.heights[index], weight);
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
    		
    		for(Column column:this.columns){//TODO MUCH FASTER
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
    	for(Column column:this.columns){
    		for(Site site:column.sites){//TODO MACRO PROBLEM
    			if(site.hasBlock()){
        			this.legalX[site.block.index] = site.x;
        			this.legalY[site.block.index] = site.y;
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
    	
    	Site lastUsedSite;//TODO FASTER
    	Site firstFreeSite;//TODO FASTER
    	
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
    	}
    	void clear(){
    		for(Site site:this.sites){
    			site.block = null;
    		}
    		this.usedSize = 0;
    	}
    	
    	private void saveState(){
    		for(Site site:this.sites){
    			site.saveState();
    		}
    	}
    	private void restoreState(){
    		for(Site site:this.sites){
    			site.restoreState();
    		}
    	}
    	private double tryBlock(Block block){
    		double oldCost = this.cost();
    		
    		this.saveState();
    		this.addBlock(block);
    		
    		double newCost = this.cost();
    		
    		this.removeBlock(block);
    		this.restoreState();
    		
    		return newCost - oldCost;
    	}
    	private void addBlock(Block block){
    		this.usedSize += block.size;

    		int optimalY = Math.max(Math.min((int)Math.round(block.linearY - 1), this.size - 1),  0);
    		
    		Site bestSite = this.sites[optimalY];
    		if(bestSite.hasBlock()){
    			while(bestSite.hasBlock()){//TODO FASTER
    				if(bestSite.next == null){//TODO MACRO PROBLEM
    					this.move();
    					break;
    				}else{
    					bestSite = bestSite.next;
    				}
    			}
    		}
    		bestSite.setBlock(block);
			this.minimumCostShift();
    	}
    	private void removeBlock(Block block){
    		this.usedSize -= block.size;
    	}
    	private boolean move(){
    		this.lastUsedSite = null;//TODO FASTER
    		for(Site site:this.sites){
    			if(site.hasBlock()){
    				this.lastUsedSite = site;
    			}
    		}
    		this.firstFreeSite = this.lastUsedSite;
    		while(this.firstFreeSite.hasBlock()){
    			this.firstFreeSite = this.firstFreeSite.previous;
    			
    			if(this.firstFreeSite == null){
    				return false;
    			}
    		}
    		
    		Site currentSite = this.firstFreeSite;
    		while(currentSite != this.lastUsedSite){
    			currentSite.block = currentSite.next.block;
    			currentSite = currentSite.next;
    		}
    		this.lastUsedSite.block = null;
    		
    		return true;
    	}
    	private void revert(){
    		Site currentSite = this.lastUsedSite;
    		while(currentSite != this.firstFreeSite){
    			currentSite.block = currentSite.previous.block;
    			currentSite = currentSite.previous;
    		}
    		this.firstFreeSite.block = null;
    	}
    	private void minimumCostShift(){
    		double minimumCost = this.cost();
    		while(this.move()){
    			double cost = this.cost();
    			if(cost < minimumCost){
    				minimumCost = cost;
    			}else{
    				revert();
    				return;
    			}
    		}
    	}
    	private double cost(){//TODO MUCH FASTER
    		double cost = 0.0;
    		for(Site site:this.sites){//TODO MACRO PROBLEM
    			if(site.hasBlock()){
    				cost += site.block.cost(site.x, site.y);
    			}
    		}
    		return cost;
    	}
	}
    private class Block {
  	   int index;

  	   double linearX, linearY;
  	  
  	   int size;
  	   double weight;
      	
  	   Block(int index, double linearX, double linearY, int width, double weight){
  		   this.index = index;
  		   this.linearX = linearX;
  		   this.linearY = linearY;
  		   this.size = width;
  		   this.weight = weight;
  	   }
  	   double cost(int legalX, int legalY){
  		   return this.weight * ((this.linearX - legalX) * (this.linearX - legalX) + (this.linearY - legalY) * (this.linearY - legalY));//TODO OFFSET PROBLEM
  	   }
    }
    private class Site {
    	final int x,y;
    	Block block;
    	Block oldBlock;
    	
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
    @Override
    protected void initializeLegalizationAreas(){
    	return;
    }
    @Override
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}