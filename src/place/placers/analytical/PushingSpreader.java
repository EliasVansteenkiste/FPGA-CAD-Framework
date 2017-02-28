package place.placers.analytical;

import place.placers.analytical.GradientLegalizer.LegalizerBlock;
import place.util.TimingTree;

/*
 * In this legalizer we remove the illegal columns before spreading the blocks out.
 * This way we can spread the blocks on a homogeneous FPGA.
 * Afterwards the illegal columns are inserted.
 */

class PushingSpreader {

    private final int discretisation;
    private final double minStepSize, maxStepSize;
    private final double speedAveraging;
	
	private double stepSize;
	private int iteration;
	
	private LegalizerBlock[] blocks;
	
	private final int width, height;
	private final int gridWidth, gridHeight;
    private final int[][] massMap;

    private final double[] horizontalPotential;
    private final double[] verticalPotential;

	private final boolean debug = false;
	private TimingTree timer;
    private final boolean timing;

    PushingSpreader(
    		int width, 
    		int height, 
    		int discretisation,
    		double minStepSize, 
    		double maxStepSize, 
    		double potential, 
    		double speedAveraging,
    		TimingTree timingTree){
    	
    	this.width = width;
    	this.height = height;

    	this.discretisation = discretisation;
    	if(this.discretisation % 2 != 0){
    		System.out.println("Discretisation should be even, now it is equal to " + this.discretisation);
    		System.exit(0);
    	}

    	this.minStepSize = minStepSize;
    	this.maxStepSize = maxStepSize;

    	this.speedAveraging = speedAveraging;

    	this.gridWidth = (this.width + 2) * this.discretisation;
    	this.gridHeight = (this.height + 2) * this.discretisation;
    	
    	//MassMap
    	this.massMap = new int[this.gridWidth][this.gridHeight];
    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y] = 0;
    		}
    	}
    	
    	//Potential
    	this.horizontalPotential = new double[this.gridWidth];
    	this.verticalPotential = new double[this.gridHeight];
    	this.ioPotential();
    	this.legalPotential(potential);
    	
    	if(timingTree != null){
    		this.timer = timingTree;
    		this.timing = true;
    	}else{
    		this.timing = false;
    	}
    }
 
    protected void doSpreading(LegalizerBlock[] blocks, int iterations) {
    	this.blocks = blocks;
    	
    	for(LegalizerBlock block:this.blocks){
    		block.setDiscretisation(this.discretisation);
    	}
    	
    	this.iteration = 0;
    	this.stepSize = this.minStepSize;
    	this.initializeMassMap();
    	do{
        	this.applyPushingForces();
        	this.iteration += 1;
        	this.stepSize += (this.maxStepSize - this.minStepSize) / iterations;
        }while(this.iteration < iterations);
    }
    private void initializeMassMap(){
    	if(timing) this.timer.start("Initialize Mass Map");

    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y] = 0;
    		}
    	}
    	
    	for(LegalizerBlock block:this.blocks){
        		
        	int x = block.horizontal.grid();
        	int y = block.vertical.grid();
        		
            for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < this.discretisation * block.height(); l++){
            		this.massMap[x + k][y + l]++;
            	}
            }
    	}

    	if(timing) this.timer.time("Initialize Mass Map");
    }

    //POTENTIAL
    private void ioPotential(){
    	if(timing) this.timer.start("IO Potential");

    	for(int d = 0; d < this.discretisation; d++){
    		this.horizontalPotential[d] = this.discretisation - d;
    		this.verticalPotential[d] = this.discretisation - d;

    		this.horizontalPotential[this.massMap.length - 1 - d] = this.discretisation - d;
    		this.verticalPotential[this.massMap[0].length - 1 - d] = this.discretisation - d;
    	}

    	if(timing) this.timer.time("IO Potential");
    }
    private void legalPotential(double maxPotential){
    	if(timing) this.timer.start("Legal Potential");

    	//Horizontal
    	for(int x = 1; x < this.width + 1; x++){
        	for(int k = 0; k < this.discretisation; k++){
    			this.horizontalPotential[x*this.discretisation + k] = Math.abs(maxPotential - 2 * maxPotential * (k + 0.5) / this.discretisation);
    		}
    	}
    	
    	//Vertical
    	for(int y = 1; y < this.height + 1; y++){
			for(int l = 0; l < this.discretisation; l++){
    		    this.verticalPotential[y*this.discretisation + l] = Math.abs(maxPotential - 2 * maxPotential * (l + 0.5)/this.discretisation);
    		}
    	}
    	
    	if(timing) this.timer.time("Legal Potential");
    }

    //PUSHING GRAVITY FORCES
    private void applyPushingForces(){
    	if(timing) this.timer.start("Apply Pushing Forces");
    	
		this.fpgaPushForces();
		this.solve();
		
		if(timing) this.timer.time("Apply Pushing Forces");
    }

    private void fpgaPushForces(){
    	if(timing) this.timer.start("Gravity Push Forces");

    	int horizontal = this.discretisation;
    	int halfHorizontal = horizontal / 2;

    	for(LegalizerBlock block:this.blocks){
	    	int x = block.horizontal.grid();
	    	int y = block.vertical.grid();
	    	
	    	int vertical = this.discretisation * block.height();
	    	int halfVertical = vertical / 2;
	    	
    		int southWest = 0;
        	int southEast = 0;
        	int nordWest = 0;
        	int nordEast = 0;
	    	
    		for(int k = 0; k < halfHorizontal; k++){
	    		for(int l = 0; l < halfVertical; l++){
	    			southWest += this.massMap[x + k][y + l];
	    		}
	    		for(int l = halfVertical; l < vertical; l++){
	    			nordWest += this.massMap[x + k][y + l];
	    		}
	    	}
	    	for(int k = halfHorizontal; k < horizontal; k++){
	    		for(int l = 0; l < halfVertical; l++){
	    			southEast +=  this.massMap[x + k][y + l];
	    		}
	    		for(int l = halfVertical; l < vertical; l++){
	    			nordEast += this.massMap[x + k][y + l];
	    		}
	    	}
	    	
    		double horizontalPotential = 0.0;
        	double verticalPotential = 0.0;
        	
	    	for(int l = 0; l < halfVertical; l++){
	    		verticalPotential += this.verticalPotential[y + l] * horizontal;
	    	}
	    	for(int l = halfVertical; l < vertical; l++){
	    		verticalPotential -= this.verticalPotential[y + l] * horizontal;
	    	}
	    	for(int k = 0; k < halfHorizontal; k++){
	    		horizontalPotential += this.horizontalPotential[x + k] * vertical;
	    	}
	    	for(int k = halfHorizontal; k < horizontal; k++){
	    		horizontalPotential -= this.horizontalPotential[x + k] * vertical;
	    	}

	    	int mass = southWest + nordWest + southEast + nordEast;
	    	block.horizontal.setForce( (southWest + nordWest - southEast - nordEast + horizontalPotential) / mass);
	    	block.vertical.setForce(   (southWest - nordWest + southEast - nordEast + verticalPotential  ) / mass);
    	}    	
    	if(timing) this.timer.time("Gravity Push Forces");
    }
    private void solve(){
    	if(timing) this.timer.start("Solve");

    	for(LegalizerBlock block:this.blocks){

    		int origX = block.horizontal.grid();
    		int origY = block.vertical.grid();

    		//Horizontal
    		block.horizontal.solve(this.stepSize, this.speedAveraging);

    		if(block.horizontal.coordinate() > this.width) block.setHorizontal(this.width);
    		if(block.horizontal.coordinate() < 1) block.setHorizontal(1);

    		this.horizontalMassMapUpdate(block.vertical.grid(), origX, block.horizontal.grid(), block.height());

    		//Vertical
    		block.vertical.solve(this.stepSize, this.speedAveraging);

    		if(block.vertical.coordinate() + block.height() - 1 > this.height) block.setVertical(this.height - block.height() + 1);
    		if(block.vertical.coordinate() < 1) block.setVertical(1);
    		
    		this.verticalMassMapUpdate(block.horizontal.grid(), origY, block.vertical.grid(), block.height());
    	}

    	if(timing) this.timer.time("Solve");

    	if(debug){
    		this.testMassMap();
    	}
    }
    private void horizontalMassMapUpdate(int y, int origX, int newX, int height){
    	if(origX < newX){//Move right
    		int horizontalDistance = newX - origX;
    		for(int l = 0; l < this.discretisation * height; l++){
    			for(int k = 0; k < horizontalDistance; k++){
            		this.massMap[origX + k][y + l]--;
            		this.massMap[newX + this.discretisation - 1 - k][y + l]++;
            	}
            }
    	}else if(origX > newX){//Move left
    		int horizontalDistance = origX - newX;
    		for(int l = 0; l < this.discretisation * height; l++){
    			for(int k = 0; k < horizontalDistance; k++){
            		this.massMap[origX + this.discretisation - 1 - k][y + l]--;
            		this.massMap[newX + k][y + l]++;
            	}
            }
    	}
    }
    private void verticalMassMapUpdate(int x, int origY, int newY, int height){
    	if(origY < newY){//Move up
    		int verticalDistance = newY - origY;
    		for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < verticalDistance; l++){
            		this.massMap[x + k][origY + l]--;
            		this.massMap[x + k][newY + height * this.discretisation - 1 - l]++;
            	}
            }
    	}else if(origY > newY){//Move down
    		int verticalDistance = origY - newY;
    		for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < verticalDistance; l++){
            		this.massMap[x + k][origY + height * this.discretisation - 1 - l]--;
            		this.massMap[x + k][newY + l]++;
            	}
            }
    	}
    }
    private void testMassMap(){
    	int[][] testMassMap = new int[this.massMap.length][this.massMap[0].length];
    			
    	for(LegalizerBlock block:this.blocks){
        	int i = block.horizontal.grid();
        	int j = block.vertical.grid();
        		
        	for(int k = 0; k < this.discretisation; k++){
        		for(int l = 0; l < this.discretisation * block.height(); l++){
        			testMassMap[i + k][j + l]++;
        		}
        	}
    	}
    	
    	for(int i = 0; i < this.massMap.length; i++){
    		for(int j = 0; j < this.massMap[0].length; j++){
    			if(testMassMap[i][j] != this.massMap[i][j]){
    				System.out.println("Something is wrong in the incremental mass map update");
    				System.out.println("\tLocation [" + i + "," + j + "]");
    				System.out.println("\t\tTest Mass Map: " + testMassMap[i][j]);
    				System.out.println("\t\tMass Map: " + this.massMap[i][j]);
    			}
    		}
    	}
    }
}