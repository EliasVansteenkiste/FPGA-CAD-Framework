package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

class TowerLegalizer extends Legalizer {
	
	private LegalizerBlock[] blocks;
	
    private static final boolean timing = false;
    private static final boolean visual = false;

    private Tower[][] grid;
    private ArrayList<Tower> towerList;
    
    private int[] visualX;
    private int[] visualY;

    TowerLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> blockIndexes){
    	
    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, visualizer, blockIndexes);
    	
    	int width = this.width + 2;
    	int height = this.height + 2;
    	this.grid = new Tower[width][height];
    	this.towerList = new ArrayList<Tower>();
    	for(int i = 0; i < width; i++){
    		for(int j = 0; j < height; j++){
    			Tower tower = new Tower(i, j, 1);
    			this.grid[i][j] = tower;
    			this.towerList.add(tower);
    		}
    	}
    	
    	this.visualX = new int[this.legalX.length];
    	this.visualY = new int[this.legalY.length];
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	this.initializeData(blocksStart, blocksEnd);
    	this.addVisual();
    	
    	while(overlap()){
    		Tower tower = this.findHighestTower();
    		this.spreadTower(tower);
    		
    		this.addVisual();
    	}
    	
    	this.updateLegal();
    }
    private void initializeData(int blocksStart, int blocksEnd){
    	Timer t = new Timer();

    	for(Tower tower:this.towerList){
    		tower.reset();
    	}

    	t.time("Reset Towers");
    	
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, this.linearX[b], this.linearY[b]);
    	}

    	t.time("Make Legalizer Blocks");

    	for(LegalizerBlock block:this.blocks){
    		int x = (int)Math.round(block.x);
    		int y = (int)Math.round(block.y);
    		
    		this.grid[x][y].add(block);
    	}

    	t.time("Fill Grid");
    	
    	if(visual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = (int)Math.round(this.linearX[i]);
    			this.visualY[i] = (int)Math.round(this.linearY[i]);
    		}
    	}
    }
    boolean overlap(){
    	for(Tower tower:this.towerList){
    		if(tower.overlap()){
    			return true;	
    		}
    	}
    	return false;
    }
    Tower findHighestTower(){
    	Tower tower = this.towerList.get(0);
    	for(Tower otherTower:this.towerList){
    		if(otherTower.height() > tower.height()){
    			tower = otherTower;
    		}
    	}
    	return tower;
    }
    void spreadTower(Tower tower){
    	int x = tower.x;
    	int y = tower.y;
    	
    	int direction = 0;
    	while(tower.overlap()){
    		Tower neighbour = this.getNeighBour(x, y, direction);
    		if(neighbour != null){
    			if(direction == 0) neighbour.add(tower.getLeft());
    			else if(direction == 1) neighbour.add(tower.getUp());
    			else if(direction == 2) neighbour.add(tower.getRight());
    			else if(direction == 3) neighbour.add(tower.getDown());
    		}

    		direction += 1;
    		direction = direction % 4;
    	}
    	tower.fix();
    }
    Tower getNeighBour(int x, int y, int direction){
    	Tower result = null;
    	if(direction == 0){//Left
    		if(x > 1) result = this.grid[x - 1][y];
    	}else if(direction == 1){//Up
    		if(y < this.height) result = this.grid[x][y + 1];
    	}else if(direction == 2){//Right
    		if(x <  this.width) result = this.grid[x + 1][y];
    	}else if(direction == 3){//Down
    		if(y > 1) result = this.grid[x][y - 1];
    	}
    	if(result.fixed()) return null;
    	else return result;

    }
    void updateLegal(){
    	for(Tower tower:this.towerList){
    		if(!tower.isEmpty()){
    			if(tower.overlap()){
    				System.out.println("Tower " + tower + " still has overlap");
    			}
    				
    			LegalizerBlock block = tower.get();
    				
    			block.x = tower.x;
    			block.y = tower.y;
    				
    			this.legalX[block.index] = tower.x;
    			this.legalY[block.index] = tower.y;
    		}
    	}
    }
    
    //TOWER
    private class Tower {
    	int x;
    	int y;

    	LinkedList<LegalizerBlock> blocks;
    	int maxHeight;

    	boolean fixed;

    	Tower(int x, int y, int maxHeight){
    		this.x = x;
    		this.y = y;

    		this.blocks = new LinkedList<LegalizerBlock>();
    		this.maxHeight = maxHeight;

    		this.fixed = false;
    	}

    	void reset(){
    		this.blocks.clear();
    		this.fixed = false;
    	}

    	void fix(){
    		this.fixed = true;
    	}
    	boolean fixed(){
    		return this.fixed;
    	}

    	void add(LegalizerBlock block){
    		this.blocks.add(block);
    	}
    	LegalizerBlock get(){
    		return this.blocks.removeFirst();
    	}
    	
    	LegalizerBlock getLeft(){
    		LegalizerBlock result = blocks.get(0);
    		for(LegalizerBlock block:this.blocks){
    			if(block.x < result.x){
    				result = block;
    			}
    		}
    		this.blocks.remove(result);
    		return result;
    	}
    	LegalizerBlock getRight(){
    		LegalizerBlock result = blocks.get(0);
    		for(LegalizerBlock block:this.blocks){
    			if(block.x > result.x){
    				result = block;
    			}
    		}
    		this.blocks.remove(result);
    		return result;
    	}
    	LegalizerBlock getDown(){
    		LegalizerBlock result = blocks.get(0);
    		for(LegalizerBlock block:this.blocks){
    			if(block.y < result.y){
    				result = block;
    			}
    		}
    		this.blocks.remove(result);
    		return result;
    	}
    	LegalizerBlock getUp(){
    		LegalizerBlock result = blocks.get(0);
    		for(LegalizerBlock block:this.blocks){
    			if(block.y > result.y){
    				result = block;
    			}
    		}
    		this.blocks.remove(result);
    		return result;
    	}

    	boolean isEmpty(){
    		return this.blocks.isEmpty();
    	}
    	int height(){
    		return this.blocks.size();
    	}

    	boolean overlap(){
    		return this.height() > this.maxHeight;
    	}
    }
    
    //LegalizerBlock
    private class LegalizerBlock {
    	double x;
    	double y;
    	
    	int index;

    	LegalizerBlock(int index, double x, double y){
    		this.x = x;
    		this.y = y;
    		
    		this.index = index;
    	}
    }

    private class Timer {
    	long start;
    	
    	Timer(){
    		if(timing){
    			this.start = System.nanoTime();
    		}
    	}
    	
    	void time(String type){
    		if(timing){
        		double time = (System.nanoTime() - this.start) * Math.pow(10, -6);
        		
        		if(time < 10){
        			time *= Math.pow(10, 3);
        			System.out.printf(type + " took %.0f ns\n", time);
        		}else{
        			System.out.printf(type + " took %.0f ms\n", time);	
        		}
    		}
    		
    		this.start = System.nanoTime();
    	}
    }
    
    private void addVisual(){
		if(visual){
			Timer t = new Timer();
		    
		    for(Tower tower:this.towerList){
		    	for(LegalizerBlock block:tower.blocks){
		    		this.visualX[block.index] = tower.x;
		    		this.visualY[block.index] = tower.y;
		    	}
		    }
			
			this.addVisual("tower expantion", this.visualX, this.visualY);
			
			t.time("Add Visual");
		}
    }

    protected void initializeLegalizationAreas(){
    	//DO NOTHING
    }
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}