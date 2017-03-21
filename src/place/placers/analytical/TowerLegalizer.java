package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

class TowerLegalizer extends Legalizer {
	
	private LegalizerBlock[] blocks;
	
	private Timer timer;
    private static final boolean timing = false;
    private static final boolean visual = true;

    private final Tower[][] grid;
    private final ArrayList<Tower> towers;
    
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
            List<Net> nets,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> blockIndexes){
    	
    	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, nets, visualizer, blockIndexes);
    	
    	int width = this.width + 2;
    	int height = this.height + 2;
    	this.grid = new Tower[width][height];
    	this.towers = new ArrayList<Tower>();
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			Tower tower = new Tower(x, y, 1);
    			this.grid[x][y] = tower;
    			this.towers.add(tower);
    		}
    	}

    	if(timing){
    		this.timer = new Timer();
    	}
    	if(visual){
        	this.visualX = new int[this.legalX.length];
        	this.visualY = new int[this.legalY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	if(timing) this.timer.start("Legalize BlockType");

    	this.initializeData(blocksStart, blocksEnd);

    	while(overlap()){
    		for(Tower tower:this.towers){
    			tower.fixed = false;
    		}
        	while(unfixed()){
        		Tower tower = this.getNextTower();
        		this.spreadTower(tower);
        	}
        	this.addVisual();
    	}
    	
    	this.updateLegal();
    	
    	if(timing) this.timer.time("Legalize BlockType");
    }
    
    //INITIALIZE DATA
    private void initializeData(int blocksStart, int blocksEnd){
    	if(timing) this.timer.start("Initialize Data");
    	
    	for(Tower tower:this.towers){
    		tower.reset();
    	}
    	
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, this.linearX[b], this.linearY[b]);
    	}

    	for(LegalizerBlock block:this.blocks){
    		int x = (int)Math.round(block.x);
    		int y = (int)Math.round(block.y);
    		
    		this.grid[x][y].add(block);
    	}
    	
    	if(visual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = (int)Math.round(this.linearX[i]);
    			this.visualY[i] = (int)Math.round(this.linearY[i]);
    		}
    	}
    	
    	if(timing) this.timer.time("Initialize Data");
    }

    //OVERLAP
    boolean unfixed(){
    	for(Tower tower:this.towers){
    		if(tower.overlap() && !tower.fixed()){
    			return true;
    		}
    	}
    	return false;
    }
    boolean overlap(){
    	for(Tower tower:this.towers){
    		if(tower.overlap()){
    			return true;
    		}
    	}
    	return false;
    }
    
    //NEXT TOWER
    Tower getNextTower(){
    	return this.findHighestTower();
    }
    Tower findHighestTower(){
    	if(timing) this.timer.start("Find Highest Tower");
    	
    	Tower highestTower = this.towers.get(0);
    	
    	for(Tower tower:this.towers){
    		if(!tower.fixed()){
        		if(tower.height() > highestTower.height()){
        			highestTower = tower;
        		}
    		}
    	}
    	return highestTower;
    }
    
    //SRPEAD TOWER
    void spreadTower(Tower tower){
    	if(timing) this.timer.start("Spread Tower");
    	
//    	System.out.println(tower);
//    	for(LegalizerBlock block:tower.blocks){
//    		System.out.println("\t" + block);
//    	}

    	ArrayList<Direction> validDirections = new ArrayList<Direction>();
    	for(Direction direction:Direction.values()){
    		Tower neighbour = this.getNeighBour(tower, direction);
    		if(neighbour != null){
    			if(!neighbour.fixed()){
    				validDirections.add(direction);
    			}
    		}
    	}

    	if(!validDirections.isEmpty()){
        	int d = 0;
        	while(tower.overlap()){
        		Direction direction = validDirections.get(d);
        		LegalizerBlock block = tower.getBlock(direction);
        		
        		Tower neighbour = this.getNeighBour(tower, direction);
        		
        		neighbour.add(block);
                
        		d++;
        		if(d == validDirections.size()){
        			d = 0;
        		}
        	}
    	}
    	
    	tower.fix();
    	
    	if(timing) this.timer.time("Spread Tower");
    }
    Tower getNeighBour(Tower tower, Direction direction){
    	if(direction.equals(Direction.NORD)){
    		if(tower.y < this.height) return this.grid[tower.x][tower.y + 1];
    	}else if(direction.equals(Direction.EAST) ){
    		if(tower.x < this.width) return this.grid[tower.x + 1][tower.y];
    	}else if(direction.equals(Direction.SOUTH)){
    		if(tower.y > 1) return this.grid[tower.x][tower.y - 1];
    	}else if(direction.equals(Direction.WEST)){
        		if(tower.x > 1) return this.grid[tower.x - 1][tower.y];
    	}
    	return null;
    }
    void updateLegal(){
    	if(timing) this.timer.start("Update Legal");
    	
    	for(Tower tower:this.towers){
    		for(LegalizerBlock block:tower.blocks){
    			block.x = tower.x;
    			block.y = tower.y;
    			
    			this.legalX[block.index] = tower.x;
    			this.legalY[block.index] = tower.y;
    		}
    	}
    	
    	if(timing) this.timer.time("Update Legal");
    }
    
    private enum Direction{
    	WEST,
    	SOUTH,
    	EAST,
    	NORD
    }
    
    //TOWER
    private class Tower {
    	int x;
    	int y;

    	final ArrayList<LegalizerBlock> blocks;
    	int maxHeight;

    	boolean fixed;

    	Tower(int x, int y, int maxHeight){
    		this.x = x;
    		this.y = y;

    		this.blocks = new ArrayList<LegalizerBlock>();
    		this.maxHeight = maxHeight;

    		this.fixed = false;
    	}
    	
    	void reset(){
    		this.fixed = false;
    		this.blocks.clear();
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

    	int height(){
    		return this.blocks.size();
    	}

    	boolean overlap(){
    		return this.height() > this.maxHeight;
    	}
    	
    	LegalizerBlock getBlock(Direction direction){
    		LegalizerBlock bestBlock = this.blocks.get(0);
    		if(direction.equals(Direction.NORD)){
    			for(LegalizerBlock candidateBlock:this.blocks){
    				if(candidateBlock.y > bestBlock.y){
    					bestBlock = candidateBlock;
    				}
    			}
    		}else if(direction.equals(Direction.EAST)){
    			for(LegalizerBlock candidateBlock:this.blocks){
    				if(candidateBlock.x > bestBlock.x){
    					bestBlock = candidateBlock;
    				}
    			}
    		}else if(direction.equals(Direction.SOUTH)){
    			for(LegalizerBlock candidateBlock:this.blocks){
    				if(candidateBlock.y < bestBlock.y){
    					bestBlock = candidateBlock;
    				}
    			}
    		}else if(direction.equals(Direction.WEST)){
    			for(LegalizerBlock candidateBlock:this.blocks){
    				if(candidateBlock.x < bestBlock.x){
    					bestBlock = candidateBlock;
    				}
    			}
    		}
    		this.blocks.remove(bestBlock);
    		return bestBlock;
    	}
    	
    	@Override
    	public String toString(){
    		return "Tower [" + this.x + "," + this.y + "] => " + this.height();
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
    	
    	@Override
    	public String toString(){
    		return "Block " + this.index + " [" + this.x + ", " + this.y + "]";
    	}
    }

    private class Timer {
    	private HashMap<String, Long> timers;
    	private ArrayList<Time> timingResult;

    	Timer(){
    		this.timers = new HashMap<String, Long>();
    		this.timingResult = new ArrayList<Time>();
    	}
    	
    	void start(String name){
    		this.timers.put(name, System.nanoTime());
    	}

    	void time(String name){
    		long start = this.timers.remove(name);
    		long end = System.nanoTime();
    		int index = this.timers.size();
    		
    		Time time = new Time(name, index, start, end);

    		this.timingResult.add(time);
    		
    		if(index == 0){
    			this.printTiming();
    		}
    	}
    	
    	void printTiming(){
    		Time head = this.timingResult.get(this.timingResult.size() - 1);
    		Time parent = head;
    		Time previous = head;
    		for(int i = this.timingResult.size() - 2; i >= 0; i--){
    			Time current = this.timingResult.get(i);
    			
    			if(current.index == parent.index){
    				parent = parent.getParent();
    				parent.addChild(current);
    				current.setParent(parent);
    			}else if(current.index == parent.index + 1){
    				parent.addChild(current);
    				current.setParent(parent);
    			}else if(current.index == parent.index + 2){
    				parent = previous;
    				parent.addChild(current);
    				current.setParent(parent);
    			}
    			
    			previous = current;
    		}
    		System.out.println(head);
    		this.timers = new HashMap<String, Long>();
    		this.timingResult = new ArrayList<Time>();
    	}
    }
    private class Time {
    	String name;
    	
    	int index;
    	
    	long start;
    	long end;
    	
    	Time parent;
    	ArrayList<Time> children;
    	
    	Time(String name, int index, long start, long end){
    		this.name = name;
    		
    		this.index = index;
    		
    		this.start = start;
    		this.end = end;
    		
    		this.parent = null;
    		this.children = new ArrayList<Time>();
    	}
    	
    	void setParent(Time parent){
    		this.parent = parent;
    	}
    	Time getParent(){
    		return this.parent;
    	}
    	void addChild(Time child){
    		this.children.add(child);
    	}
    	Time[] getChildren(){
    		int counter = this.children.size() - 1;
    		Time[] result = new Time[this.children.size()];
    		
    		for(Time child:this.children){
    			result[counter] = child;
    			counter--;
    		}
    		return result;
    	}
    	
    	@Override
    	public String toString(){
    		String result = new String();

    		for(int i = 0; i < this.index; i++){
    			this.name = "\t" + this.name;
    		}
    		double time = (this.end -  this.start) * Math.pow(10, -6);
        	if(time < 10){
        		time *= Math.pow(10, 3);
        		result += String.format(this.name + " took %.0f ns\n", time);
        	}else{
        		result += String.format(this.name + " took %.0f ms\n", time);
    		}
        	
    		for(Time child:this.getChildren()){
    			result += child.toString();
    		}
    		
    		return result;
    	}
    }
    
    private void addVisual(){
		if(visual){
			if(timing) this.timer.start("Add Visual");
		    
		    for(Tower tower:this.towers){
		    	for(LegalizerBlock block:tower.blocks){
		    		this.visualX[block.index] = tower.x;
		    		this.visualY[block.index] = tower.y;
		    	}
		    }
			
			this.addVisual("tower expantion", this.visualX, this.visualY);
			
			if(timing) this.timer.time("Add Visual");
		}
    }

    protected void initializeLegalizationAreas(){
    	//DO NOTHING
    }
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}