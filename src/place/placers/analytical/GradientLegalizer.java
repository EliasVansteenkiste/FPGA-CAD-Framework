package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

class GradientLegalizer extends Legalizer {

	private LegalizerBlock[] blocks;

    private final int discretisation;
    private final Loc[][] massMap;

    private int iteration;
    private int overlap;

	private final double stepSize, speedAveraging;

	private Timer timer;
    private static final boolean timing = false;
    private static final boolean visual = false;

    // Arrays to visualize the legalisation progress
    private double[] visualX;
    private double[] visualY;

    GradientLegalizer(
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

        this.stepSize = 1;
        this.speedAveraging = 0.2;

    	this.discretisation = 4;
    	if(this.discretisation % 2 != 0){
    		System.out.println("Discretisation should be even, now it is equal to " + this.discretisation);
    		System.exit(0);
    	}

    	int width = (this.width + 2) * this.discretisation;
    	int height = (this.height + 2) * this.discretisation;
    	this.massMap = new Loc[width][height];
    	for(int i = 0; i < width; i++){
    		for(int j = 0; j < height; j++){
    			this.massMap[i][j] = new Loc(i, j, this.discretisation, 0.05);
    		}
    	}

    	if(timing){
    		this.timer = new Timer();
    	}
    	if(visual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	this.initializeData(blocksStart, blocksEnd);
        
        do{
        	this.applyPushingForces();
        	this.iteration += 1;
        }while(this.overlap > 500 && this.iteration < 750);

    	this.updateLegal();
    	this.shiftLegal();
    }
    private void initializeData(int blocksStart, int blocksEnd){
    	if(timing) this.timer.start();

    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, this.linearX[b], this.linearY[b]);
    	}    	

    	this.iteration = 0;
    	this.overlap = 0;

    	this.resetMassMap();
    	this.initializeMassMap();

    	if(visual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = this.linearX[i];
    			this.visualY[i] = this.linearY[i];
    		}
    	}

    	if(timing) this.timer.time("Initialize Data");
    }

    //MASS MAP
    private void resetMassMap(){
    	int width = this.massMap.length;
    	int height = this.massMap[0].length;
    	for(int i = 0; i < width; i++){
    		for(int j = 0; j < height; j++){
    			this.massMap[i][j].reset();
    		}
    	}
    }
    private void initializeMassMap(){
    	if(timing) this.timer.start();

    	for(LegalizerBlock block:this.blocks){
        	double x = block.horizontal.coordinate;
        	double y = block.vertical.coordinate;
        		
        	int i = (int)Math.ceil(x * this.discretisation);
        	int j = (int)Math.ceil(y * this.discretisation);
        		
        	for(int k = 0; k < this.discretisation; k++){
        		for(int l = 0; l < this.discretisation; l++){
        			Loc loc = this.massMap[i + k][j + l];
        			loc.increase();
        			if(loc.overlap()) this.overlap++;
        		}
        	}
    	}
    	
    	if(timing) this.timer.time("Initialize Mass Map");
    }


    //PUSHING GRAVITY FORCES
    private void applyPushingForces(){
		this.initializeIteration();
		
		this.fpgaPullForces();
		
		this.solve();
		
		this.addVisual();
    }
    private void initializeIteration(){
    	if(timing) this.timer.start();

    	for(LegalizerBlock block:this.blocks){
    		block.reset();
    	}

    	if(timing) this.timer.time("Initialize Iteration");
    }
    private void solve(){
    	if(timing) this.timer.start();
    	
    	double origX, origY, newX, newY;
    	int origI, origJ, newI, newJ;
    	
    	for(LegalizerBlock block:this.blocks){
    		
    		origX = block.horizontal.coordinate;
    		origY = block.vertical.coordinate;
    		
            origI = (int)Math.ceil(origX * this.discretisation);
            origJ = (int)Math.ceil(origY * this.discretisation);
    		
    		block.horizontal.solve(this.stepSize, this.speedAveraging);
    		block.vertical.solve(this.stepSize, this.speedAveraging);
    		
    		if(block.horizontal.coordinate > this.width) block.horizontal.coordinate = this.width;
    		if(block.horizontal.coordinate < 1) block.horizontal.coordinate = 1;
    		
    		if(block.vertical.coordinate > this.height) block.vertical.coordinate = this.height;
    		if(block.vertical.coordinate < 1) block.vertical.coordinate = 1;
    		
    		newX = block.horizontal.coordinate;
    		newY = block.vertical.coordinate;
            
            newI = (int)Math.ceil(newX * this.discretisation);
            newJ = (int)Math.ceil(newY * this.discretisation);
            
    		//UPDATE MASS MAP //TODO NOT FULLY INCREMENTAL
            for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < this.discretisation; l++){
            		Loc origLoc = this.massMap[origI + k][origJ + l];
            		origLoc.decrease();
            		if(origLoc.mass > 0) this.overlap--;
            		
            		Loc newLoc = this.massMap[newI + k][newJ + l];
            		newLoc.increase();
            		if(newLoc.mass > 1) this.overlap++;
            	}
            }
    	}

//    	//CONTROL MASS MAP
//    	
//    	boolean ok = true;
//   	
//    	int width = (this.width + 2) * this.discretisation;
//    	int height = (this.height + 2) * this.discretisation;
//    	int[][] controlMassMap = new int[width][height];
//    			
//    	for(LegalizerBlock block:this.blocks){
//        	double x = block.horizontal.coordinate;
//        	double y = block.vertical.coordinate;
//        		
//        	int i = (int)Math.ceil(x * this.discretisation);
//        	int j = (int)Math.ceil(y * this.discretisation);
//        		
//        	for(int k = 0; k < this.discretisation; k++){
//        		for(int l = 0; l < this.discretisation; l++){
//        			controlMassMap[i + k][j + l]++;
//        		}
//        	}
//    	}
//    	
//    	for(int i = 0; i < width; i++){
//    		for(int j = 0; j < height; j++){
//    			if(controlMassMap[i][j] != this.massMap[i][j].mass){
//    				ok = false;
//    				//System.out.println(controlMassMap[i][j] + " " + this.massMap[i][j].mass);
//    			}
//    		}
//    	}
//    	System.out.println(ok);

//    	//CONTROL OVERLAP
//    	
//    	int testOverlap = 0;
//    	
//    	int width = (this.width + 2) * this.discretisation;
//    	int height = (this.height + 2) * this.discretisation;
//    			
//        for(int i = 0; i < width; i++){
//        	for(int j = 0; j < height; j++){
//        		if(this.massMap[i][j].mass > 1){
//        			testOverlap += this.massMap[i][j].mass - 1;
//        		}
//        	}
//        }
//    	System.out.println(testOverlap == this.overlap);

    	if(timing) this.timer.time("Solve");
    }

    //PUSHING GRAVITY FORCES BETWEEN THE BLOCKS
    private void fpgaPullForces(){
    	if(timing) this.timer.start();
    	
    	//Local known variables
    	double area = (double)this.discretisation * (double)this.discretisation / 2.0;
    	int discretisation = this.discretisation;
    	int halfDiscretisation = this.discretisation / 2;
    	
    	for(LegalizerBlock block:this.blocks){
    		double x = block.horizontal.coordinate;
    		double y = block.vertical.coordinate;
    		   		
	    	int i = (int)Math.ceil(x * this.discretisation);
	    	int j = (int)Math.ceil(y * this.discretisation);
	    		
	    	//Horizontal
	    	double horizontalForce = 0.0;
	    	for(int k = 0; k < halfDiscretisation; k++){
	    		for(int l = 0; l < discretisation; l++){
	    			//Positive force
	    			horizontalForce += this.massMap[i+k][j+l].horizontalForce();
	    		}
	    	}
	    	for(int k = halfDiscretisation; k < discretisation; k++){
	    		for(int l = 0; l < discretisation; l++){
	    			//Negative force
	    			horizontalForce -= this.massMap[i+k][j+l].horizontalForce();
	    		}
	    	}
	    	block.horizontal.force += horizontalForce / area;
	    	
	    	//Vertical
	    	double verticalForce = 0.0;
	    	for(int k = 0; k < discretisation; k++){
	    		for(int l = 0; l < halfDiscretisation; l++){
	    			//Positive force
	    			verticalForce += this.massMap[i+k][j+l].verticalForce();
	    		}
	    	}
	    	for(int k = 0; k < discretisation; k++){
	    		for(int l = halfDiscretisation; l < discretisation; l++){
	    			//Negative force
	    			verticalForce -= this.massMap[i+k][j+l].verticalForce();
	    		}
	    	}
	    	block.vertical.force += verticalForce / area;	
    		
    	}    	
    	if(timing) this.timer.time("Gravity Push Forces");
    }

    //Set legal coordinates of all blocks
    private void updateLegal(){
    	if(timing) this.timer.start();
    	
    	for(LegalizerBlock block:this.blocks){
    		block.horizontal.coordinate = Math.round(block.horizontal.coordinate);
    		block.vertical.coordinate = Math.round(block.vertical.coordinate);
    		
    		this.legalX[block.index] = (int)block.horizontal.coordinate;
    		this.legalY[block.index] = (int)block.vertical.coordinate;
    	}
    	
    	if(timing) this.timer.time("Update legal");
    }
    private void shiftLegal(){
    	if(timing) this.timer.start();
    	
    	this.iteration += 1;
    	
    	int width = this.width + 2;
    	int height = this.height + 2;
    	
    	LegalizerBlock[][] legalMap = new LegalizerBlock[width][height];
    	ArrayList<LegalizerBlock> unplacedBlocks = new ArrayList<LegalizerBlock>();
    	for(LegalizerBlock block:this.blocks){
    		int x = this.legalX[block.index];
    		int y = this.legalY[block.index];
    		
    		if(legalMap[x][y] == null){
    			legalMap[x][y] = block;
    		}else{
    			unplacedBlocks.add(block);
    		}
    	}
    	if(unplacedBlocks.size() < this.blocks.length * 0.02){
        	int movingX = 0, movingY = 0;
        	for(LegalizerBlock block:unplacedBlocks){

        		int x = (int)block.horizontal.coordinate;
        		int y = (int)block.vertical.coordinate;
        		
        		int horizontalLeft = 0;
        		int horizontalRight = 0;
        		int verticalDown = 0;
        		int verticalUp = 0;
        		
        		//Horizontal left
        		movingX = x;
        		while(legalMap[movingX][y] != null){
        			movingX--;
        			horizontalLeft++;
        		}

        		//Horizontal right
        		movingX = x;
        		while(legalMap[movingX][y] != null){
        			movingX++;
        			horizontalRight++;
        		}
        			
        		//Vertical down
        		movingY = y;
        		while(legalMap[x][movingY] != null){
        			movingY--;
        			verticalDown++;
        		}
        		
        		//Vertical up
        		movingY = y;
        		while(legalMap[x][movingY] != null){
        			movingY++;
        			verticalUp++;
        		}

        		//Move blocks to make place for the current block
        		int min = Math.min(Math.min(horizontalLeft, horizontalRight), Math.min(verticalDown, verticalUp));
        		
        		x = (int)block.horizontal.coordinate;
        		y = (int)block.vertical.coordinate;
        		
        		if(horizontalLeft == min){
            		while(legalMap[x][y] != null){
            			x--;
            		}
            		while(x < (int)block.horizontal.coordinate){
            			legalMap[x][y] = legalMap[x + 1][y];
            			x++;
            		}
            		legalMap[x][y] = block;
        		}else if(horizontalRight == min){
            		while(legalMap[x][y] != null){
            			x++;
            		}
            		while(x > (int)block.horizontal.coordinate){
            			legalMap[x][y] = legalMap[x - 1][y];
            			x--;
            		}
            		legalMap[x][y] = block;
        		}else if(verticalDown == min){
            		while(legalMap[x][y] != null){
            			y--;
            		}
            		while(y < (int)block.vertical.coordinate){
            			legalMap[x][y] = legalMap[x][y + 1];
            			y++;
            		}
            		legalMap[x][y] = block;
        		}else if(verticalUp == min){
            		while(legalMap[x][y] != null){
            			y++;
            		}
            		while(y > (int)block.vertical.coordinate){
            			legalMap[x][y] = legalMap[x][y - 1];
            			y--;
            		}
            		legalMap[x][y] = block;
        		}
        	}
        	
        	for(int i = 0; i < width; i++){
        		for(int j = 0; j < height; j++){
        			if(legalMap[i][j] != null){
        				LegalizerBlock block = legalMap[i][j];
        				block.horizontal.coordinate = i;
        				block.vertical.coordinate = j;
        				
        	    		this.legalX[block.index] = (int)block.horizontal.coordinate;
        	    		this.legalY[block.index] = (int)block.vertical.coordinate;
        			}
        		}
        	}
        	
        	this.addVisual();
    	}
    	if(timing) this.timer.time("Shift legal");
    }
    
    // Visual
    private void addVisual(){
		if(visual){
			if(timing) this.timer.start();

			for(LegalizerBlock block:this.blocks){
				this.visualX[block.index] = block.horizontal.coordinate;
				this.visualY[block.index] = block.vertical.coordinate;	
			}
			this.addVisual(String.format("gradient expand step %d", this.iteration), this.visualX, this.visualY);
			
			if(timing) this.timer.time("Add Visual");
		}
    }

    private class Loc {
    	private int mass;

    	final double horizontalPotential;
    	final double verticalPotential;

//    	double horizontalForce;
//    	double verticalForce;
//    	
//    	boolean forceValid;
    	
    	Loc(int x, int y, int discretisation, double maxPotential){
    		this.mass = 0;

        	this.horizontalPotential = this.potential(x, discretisation, maxPotential);
        	this.verticalPotential =  this.potential(y, discretisation, maxPotential);
        	
//        	this.forceValid = false;
    	}
    	private double potential(int val, int discretisation, double maxPotential){
    		double rest = (val % discretisation) + 1.0/discretisation;
    		return Math.abs(maxPotential - 2*maxPotential*rest/discretisation);
    	}
    	
    	void reset(){
    		this.mass = 0;
    	}

    	void decrease(){
    		this.mass--;
//    		this.forceValid = false;
    	}
    	void increase(){
    		this.mass++;
//    		this.forceValid = false;
    	}
    	
    	boolean overlap(){
    		return this.mass > 1;
    	}
    	
//    	void setForce(){
//    		this.horizontalForce = 1.0 - 1.0/(this.mass + this.horizontalPotential);
//    		this.verticalForce = 1.0 - 1.0/(this.mass + this.verticalPotential);
//    	}
        private double horizontalForce(){
//        	if(!this.forceValid){
//        		this.setForce();
//        		this.forceValid = true;
//        	}
//        	return this.horizontalForce;
        	return 1.0 - 1.0/(this.mass + this.horizontalPotential);
        }
        private double verticalForce(){
//        	if(!this.forceValid){
//        		this.setForce();
//        		this.forceValid = true;
//        	}
//        	return this.verticalForce;
        	return 1.0 - 1.0/(this.mass + this.verticalPotential);
        }
    }
    
    //LegalizerBlock
    private class LegalizerBlock {
    	final int index;
    	
    	final Direction horizontal;
    	final Direction vertical;

    	LegalizerBlock(int index, double x, double y){
    		this.index = index;
    		
    		this.horizontal = new Direction(x);
    		this.vertical = new Direction(y);
    	}
    	
    	void reset(){
    		this.horizontal.reset();
    		this.vertical.reset();
    	}
    }
    
    private class Direction {
    	double coordinate;
    	double speed;
    	double force;
    	
    	Direction(double coordinate){
    		this.coordinate = coordinate;
    		this.speed = 0;
    		this.force = 0;
    	}
    	
    	void reset(){
    		this.force = 0.0;
    	}
    	
    	void solve(double stepSize, double speedAveraging){
        	
    		double netGoal = this.coordinate;
        	
        	if(this.force != 0.0){
        		netGoal += this.force;
        	} else {
        		return;
        	}
        	
        	double newSpeed = stepSize * (netGoal - this.coordinate);
        	
        	this.speed = speedAveraging * this.speed + (1 - speedAveraging) * newSpeed;
        	this.coordinate += this.speed;
    	}
    }
    
    private class Timer {
    	private long start;
    	
    	Timer(){}
    	
    	void start(){
    		this.start = System.nanoTime();
    	}

    	void time(String type){
        	double time = (System.nanoTime() - this.start) * Math.pow(10, -6);
        		
        	if(time < 10){
        		time *= Math.pow(10, 3);
        		System.out.printf(type + " took %.0f ns\n", time);
        	}else{
        		System.out.printf(type + " took %.0f ms\n", time);	
    		}
    	}
    }

    protected void initializeLegalizationAreas(){
    	//DO NOTHING
    }
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}