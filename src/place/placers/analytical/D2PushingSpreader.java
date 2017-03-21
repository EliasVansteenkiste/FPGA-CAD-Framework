package place.placers.analytical;

import java.util.ArrayList;

import place.placers.analytical.D2GradientLegalizer.LegalizerBlock;
import place.util.TimingTree;

class D2PushingSpreader {

    private final double stepSize;
    private final double speedAveraging;

	private int iteration;

	private LegalizerBlock[] blocks;

	private int start, end;
	private final int width, height;
	private final int gridWidth, gridHeight;
    private final double[][] massMap;

	private final boolean debug = false;
	private TimingTree timer;
    private final boolean timing;

    D2PushingSpreader(
    		int width, 
    		int height, 
    		double stepSize, 
    		double speedAveraging,
    		TimingTree timingTree){
    	
    	this.stepSize = stepSize;
    	this.speedAveraging = speedAveraging;

    	this.width = width;
    	this.height = height;
    	
    	this.gridWidth = (this.width + 2) * 2;
    	this.gridHeight = (this.height + 2) * 2;
    	
    	//MassMap
    	this.massMap = new double[this.gridWidth][this.gridHeight];
    	
    	if(timingTree != null){
    		this.timer = timingTree;
    		this.timing = true;
    	}else{
    		this.timing = false;
    	}
    }

    protected void doSpreading(LegalizerBlock[] blocks, int iterations) {
    	this.blocks = blocks;
    	
    	double initialGridForce = 0.004;
    	double finalGridForce = 0.004;
    	this.iteration = 0;
    	do{
    		double gridForce = initialGridForce - this.iteration * (initialGridForce - finalGridForce) / iterations;
        	this.applyPushingForces(gridForce);
        	this.iteration += 1;
        }while(this.iteration < iterations);
    }

    //PUSHING GRAVITY FORCES
    private void applyPushingForces(double gridForce){
    	if(timing) this.timer.start("Apply Pushing Forces");
    	
    	this.updateArea();
    	this.initializeMassMap();
		this.fpgaPushForces(gridForce);
		this.solve();
		
		if(timing) this.timer.time("Apply Pushing Forces");
    }

    private void updateArea(){
    	if(timing) this.timer.start("Update area");
    	
    	for(LegalizerBlock block:this.blocks){
    		block.updateArea();
    	}
    	
    	if(timing) this.timer.time("Update area");
    }
    private void initializeMassMap(){
    	if(timing) this.timer.start("Initialize Mass Map");

    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y] = 0.0;
    		}
    	}

    	for(LegalizerBlock block:this.blocks){
        	
    		int x = (int)Math.ceil(block.horizontal.coordinate * 2.0);
    		int y = (int)Math.ceil(block.vertical.coordinate * 2.0);
    		
    		for(int h = 0; h < block.height; h++){
        		this.massMap[x - 1][y - 1] += block.a1;
        		this.massMap[x][y - 1] += block.a3 + block.a1;
        		this.massMap[x + 1][y - 1] += block.a3;
        		
        		this.massMap[x - 1][y] += block.a1 + block.a2;
        		this.massMap[x][y] += 0.25;
        		this.massMap[x + 1][y] += block.a3 + block.a4;
        		
        		this.massMap[x - 1][y + 1] += block.a2;
        		this.massMap[x][y + 1] += block.a2 + block.a4;
        		this.massMap[x + 1][y + 1] += block.a4;

        		y += 2;
    		}
    	}
    	
    	if(debug){
        	double sum = 0.0;
        	for(int x = 0; x < this.gridWidth; x++){
        		for(int y = 0; y < this.gridHeight; y++){
        			sum += this.massMap[x][y];
        		}
        	}
        	System.out.printf("Blocks: %d\tMass: %.0f\n", this.blocks.length, sum);
    	}

    	if(timing) this.timer.time("Initialize Mass Map");
    }
    private void fpgaPushForces(double gridForce){
    	if(timing) this.timer.start("Gravity Push Forces");

    	double nordWest, nordEast, southWest, southEast;
    	
    	for(LegalizerBlock block:this.blocks){
    		
    		southWest = 0.0;
    		southEast = 0.0;
    		nordWest = 0.0;
    		nordEast = 0.0;
    		
    		int x = (int)Math.ceil(block.horizontal.coordinate * 2.0);
    		int y = (int)Math.ceil(block.vertical.coordinate * 2.0);
    		
    		for(int h = 0; h < block.height; h++){
        		southWest += block.a1 * this.massMap[x - 1][y - 1];
        		southWest += block.a2 * this.massMap[x - 1][y];
        		southWest += block.a3 * this.massMap[x][y - 1];
        		southWest += block.a4 * this.massMap[x][y];
        		
        		southEast += block.a1 * this.massMap[x][y - 1];
        		southEast += block.a2 * this.massMap[x][y];
        		southEast += block.a3 * this.massMap[x + 1][y - 1];
        		southEast += block.a4 * this.massMap[x + 1][y];
        		
        		nordWest += block.a1 * this.massMap[x - 1][y];
        		nordWest += block.a2 * this.massMap[x - 1][y + 1];
        		nordWest += block.a3 * this.massMap[x][y];
        		nordWest += block.a4 * this.massMap[x][y + 1];
        		
        		nordEast += block.a1 * this.massMap[x][y];
        		nordEast += block.a2 * this.massMap[x][y + 1];
        		nordEast += block.a3 * this.massMap[x + 1][y];
        		nordEast += block.a4 * this.massMap[x + 1][y + 1];
        		
        		y += 2;
    		}
    		
    		double xGrid = gridForce * (Math.round(block.horizontal.coordinate) - block.horizontal.coordinate);
    		double yGrid = gridForce * (Math.round(block.vertical.coordinate) - block.vertical.coordinate);
    		
    		double mass = southWest + nordWest + southEast + nordEast;
	    	block.horizontal.setForce( (southWest + nordWest - southEast - nordEast + xGrid) / (mass + xGrid));
	    	block.vertical.setForce(   (southWest - nordWest + southEast - nordEast + yGrid) / (mass + yGrid));
    	}    	
    	
    	if(timing) this.timer.time("Gravity Push Forces");
    }
    private void solve(){
    	if(timing) this.timer.start("Solve");

    	for(LegalizerBlock block:this.blocks){
    		//Horizontal
    		block.horizontal.solve(this.stepSize, this.speedAveraging);

    		if(block.horizontal.coordinate > this.width) block.horizontal.coordinate = this.width;
    		if(block.horizontal.coordinate < 1) block.horizontal.coordinate = 1;

    		//Vertical
    		block.vertical.solve(this.stepSize, this.speedAveraging);

    		if(block.vertical.coordinate + block.height - 1 > this.height) block.vertical.coordinate = this.height - block.height + 1;
    		if(block.vertical.coordinate < 1) block.vertical.coordinate = 1;
    	}

    	if(timing) this.timer.time("Solve");
    }
}