package place.placers.analytical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private final int halfDiscretisation;
    private final Loc[][] massMap;
    
    private final boolean[][] legal;
    private final LegalizerBlock[][] legalMap;

    private final int gridWidth;
    private final int gridHeight;

    private int iteration;
    private int overlap;

	private final double stepSize, speedAveraging;

	private static final boolean debug = false;
	private Timer timer;
    private static final boolean timing = true;
    private static final boolean visual = false;
    private static final boolean interVisual = false;
    private static final boolean printPotential = false;

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

        this.stepSize = 0.75;
        this.speedAveraging = 0.2;

    	this.discretisation = 4;
    	if(this.discretisation % 2 != 0){
    		System.out.println("Discretisation should be even, now it is equal to " + this.discretisation);
    		System.exit(0);
    	}
    	if(this.discretisation > 20){
    		System.out.println("Discretisation should be smaller than 20, now it is equal to " + this.discretisation);
    		System.exit(0);
    	}
    	this.halfDiscretisation = this.discretisation / 2;

    	this.gridWidth = (this.width + 2) * this.discretisation;
    	this.gridHeight = (this.height + 2) * this.discretisation;
    	this.massMap = new Loc[this.gridWidth][this.gridHeight];
    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y] = new Loc();
    		}
    	}

    	this.legal = new boolean[this.width + 2][this.height + 2];
    	this.legalMap = new LegalizerBlock[this.width + 2][this.height + 2];

    	if(timing){
    		this.timer = new Timer();
    	}
    	
    	boolean doVisual = visual || interVisual;
    	if(doVisual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	if(timing) this.timer.start("Legalize BlockType");
    	
    	int maxOverlap = this.discretisation * this.discretisation * (blocksEnd - blocksStart);
    	double allowedOverlap = maxOverlap * 0.05;

    	this.initializeData(blocksStart, blocksEnd);

    	//LEGAL POTENTIAL
    	this.ioPotential();
    	this.legalPotential();
    	
    	int legalIterations = 250;
    	if(timing) legalIterations = 20;

    	if(printPotential) this.printPotential();

    	this.iteration = 0;
    	do{
        	this.applyPushingForces();
        	this.iteration += 1;
        }while(this.overlap > allowedOverlap && this.iteration < legalIterations);
    	
    	this.addVisual("Legal Potential");

    	//ILLEGAL POTENTIAL
    	this.illegalPotential();

    	int illegalIterations = 50;
    	if(timing) illegalIterations = 20;
    	
    	this.iteration = 0;
        do{
        	this.applyPushingForces();
        	this.iteration += 1;
        }while(this.overlap > allowedOverlap && this.iteration < illegalIterations);

        this.addVisual("Illegal Potential");

        //LEGAL SOLUTION
    	this.updateLegal();

    	this.addVisual("Legalized Postions");

    	this.shiftLegal();
    	
    	if(timing) this.timer.time("Legalize BlockType");
    }
    
    //INITIALISATION
    private void initializeData(int blocksStart, int blocksEnd){
    	if(timing) this.timer.start("Initialize Data");
    	
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, this.linearX[b], this.linearY[b]);
    	}
    	
    	this.setLegal(this.blockType);

    	this.iteration = 0;
    	this.overlap = 0;
    	
    	this.resetMassMap();
    	this.initializeMassMap();
    	
    	this.resetPotential();

    	boolean doVisual = visual || interVisual;
    	if(doVisual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = this.linearX[i];
    			this.visualY[i] = this.linearY[i];
    		}
    	}
    	
    	if(timing) this.timer.time("Initialize Data");
    }
    private void setLegal(BlockType blockType){
    	if(timing) this.timer.start("Set Legal");
    	
    	for(int x = 0; x < this.width + 2; x++){
    		for(int y = 0; y < this.height + 2; y++){
    			this.legal[x][y] = false;
    		}
    	}
    	for(int x = 1; x < this.width + 1; x++){
    		if(this.circuit.getColumnType(x).equals(blockType)){
    			for(int y = 1; y < this.height + 1; y++){
	    			this.legal[x][y] = true;
	    		}
    		}
    	}
    	for(int x = 0; x < this.width + 2; x++){
    		for(int y = 0; y < this.height + 2; y++){
    			this.massMap[x][y].setLegal(this.legal[x][y]);
    		}
    	}
    	
    	if(timing) this.timer.time("Set Legal");
    }
    
    //POTENTIAL
    private void resetPotential(){
    	if(timing) this.timer.start("Reset Potential");
    	
    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y].resetPotential();
    		}
    	}
    	
    	if(timing) this.timer.time("Reset Potential");
    }
    private void ioPotential(){
    	if(timing) this.timer.start("IO Potential");

    	for(int y = 0; y < this.gridHeight; y++){
    		for(int d = 0; d < this.discretisation; d++){
    			this.massMap[d][y].setHorizontalPotential(this.discretisation - d);
    			this.massMap[this.gridWidth - 1 - d][y].setHorizontalPotential(this.discretisation - d);
    		}
    	}
    	for(int x = 0; x < this.gridWidth; x++){
    		for(int d = 0; d < this.discretisation; d++){
    			this.massMap[x][d].setVerticalPotential(this.discretisation - d);
    			this.massMap[x][this.gridHeight - 1 - d].setVerticalPotential(this.discretisation - d);
    		}
    	}
    	
    	if(timing) this.timer.time("IO Potential");
    }
    private void illegalPotential(){
    	if(timing) this.timer.start("Illegal Potential");
    	
        int height = (this.height + 2) * this.discretisation;
    	for(int j = 0; j < height-1; j+=2){
    		this.massMap[124][j].setHorizontalPotential(4);
    		this.massMap[125][j].setHorizontalPotential(3);
    		this.massMap[126][j].setHorizontalPotential(2);
    		this.massMap[127][j].setHorizontalPotential(1);
    	}
    	for(int j = 0; j < height-1; j+=2){
    		this.massMap[124][j].setHorizontalPotential(1);
    		this.massMap[125][j].setHorizontalPotential(2);
    		this.massMap[126][j].setHorizontalPotential(3);
    		this.massMap[127][j].setHorizontalPotential(4);
    	}
    	
    	if(timing) this.timer.time("Illegal Potential");
    }
    private void legalPotential(){
    	if(timing) this.timer.start("Legal Potential");
    	
    	double maxPotential = 0.025;
    	double horizontalPotential, verticalPotential;

    	for(int x = 0; x < this.width + 2; x++){
    		for(int y = 0; y < this.height + 2; y++){
    			if(this.legal[x][y]){
    				for(int k = 0; k < this.discretisation; k++){
    					for(int l = 0; l < this.discretisation; l++){
    						
    						horizontalPotential = Math.abs(maxPotential - 2*maxPotential*(k + 0.5)/this.discretisation);
    						verticalPotential = Math.abs(maxPotential - 2*maxPotential*(l + 0.5)/this.discretisation);
    						
    		    			this.massMap[x*this.discretisation + k][y*this.discretisation + l].setHorizontalPotential(horizontalPotential);
    		    			this.massMap[x*this.discretisation + k][y*this.discretisation+ l].setVerticalPotential(verticalPotential);
    					}
    				}
    			}
    		}
    	}

    	if(timing) this.timer.time("Legal Potential");
    }
    private void printPotential(){
    	if(timing) this.timer.start("Print Potential");
    	
    	this.printHorizontalPotential();
    	this.printVerticalPotential();
    	
    	if(timing) this.timer.time("Print Potential");
    }
    private void printHorizontalPotential(){
    	try {
    		File file = new File("/Users/drvercru/Documents/Workspace/LiquidPart/plot/horizontalPotential.txt");
    		BufferedWriter output = new BufferedWriter(new FileWriter(file));

    		for(int x = 0; x < this.gridWidth; x++){
    			for(int y = 0; y < this.gridHeight; y++){
    				output.write(this.massMap[x][y].horizontalPotential + ";");
    			}
    			output.write("\n");
    		}

    		output.close();
    	} catch ( IOException e ) {
    		e.printStackTrace();
    	}
    }
    private void printVerticalPotential(){
    	try {
    		File file = new File("/Users/drvercru/Documents/Workspace/LiquidPart/plot/verticalPotential.txt");
    		BufferedWriter output = new BufferedWriter(new FileWriter(file));

    		for(int x = 0; x < this.gridWidth; x++){
    			for(int y = 0; y < this.gridHeight; y++){
    				output.write(this.massMap[x][y].verticalPotential + ";");
    			}
    			output.write("\n");
    		}

    		output.close();
    	} catch ( IOException e ) {
    		e.printStackTrace();
    	}
    }

    //MASS MAP
    private void resetMassMap(){
    	if(timing) this.timer.start("Reset Mass Map");

    	for(int x = 0; x < this.gridWidth; x++){
    		for(int y = 0; y < this.gridHeight; y++){
    			this.massMap[x][y].reset();
    		}
    	}

    	if(timing) this.timer.time("Reset Mass Map");
    }
    private void initializeMassMap(){
    	if(timing) this.timer.start("Initialize Mass Map");

    	for(LegalizerBlock block:this.blocks){
        	int x = (int)Math.ceil(block.horizontal.coordinate * this.discretisation);
        	int y = (int)Math.ceil(block.vertical.coordinate * this.discretisation);
        		
        	for(int k = 0; k < this.discretisation; k++){
        		for(int l = 0; l < this.discretisation; l++){
        			if(this.massMap[x + k][y + l].increase()) this.overlap++;
        		}
        	}
    	}
    	
    	if(timing) this.timer.time("Initialize Mass Map");
    }


    //PUSHING GRAVITY FORCES
    private void applyPushingForces(){
    	if(timing) this.timer.start("Apply Pushing Forces");
    	
		this.initializeIteration();
		this.fpgaPullForces();
		this.solve();
		
		this.addVisual();
		
		if(timing) this.timer.time("Apply Pushing Forces");
    }
    private void initializeIteration(){
    	if(timing) this.timer.start("Initialize Iteration");

    	for(LegalizerBlock block:this.blocks){
    		block.reset();
    	}

    	if(timing) this.timer.time("Initialize Iteration");
    }
    private void solve(){
    	if(timing) this.timer.start("Solve");

    	int origX, origY, newX, newY;
    	int horizontalDistance, verticalDistance;
    	
    	for(LegalizerBlock block:this.blocks){

            origX = (int)Math.ceil(block.horizontal.coordinate * this.discretisation);
            origY = (int)Math.ceil(block.vertical.coordinate * this.discretisation);
    		
    		block.horizontal.solve(this.stepSize, this.speedAveraging);
    		block.vertical.solve(this.stepSize, this.speedAveraging);
    		
//    		if(block.horizontal.coordinate > this.width) block.horizontal.coordinate = this.width;
//    		if(block.horizontal.coordinate < 1) block.horizontal.coordinate = 1;
//    		
//    		if(block.vertical.coordinate > this.height) block.vertical.coordinate = this.height;
//    		if(block.vertical.coordinate < 1) block.vertical.coordinate = 1;
            
            newX = (int)Math.ceil(block.horizontal.coordinate * this.discretisation);
            newY = (int)Math.ceil(block.vertical.coordinate * this.discretisation);
            
    		//UPDATE MASS MAP
            if(origX == newX && origY == newY){
            	//NO UPDATE REQUIRED
            	continue;
            }else if(origY == newY){
            	//HORIZONTAL MOVE
            	if(origX < newX){
            		//MOVE RIGHT
            		horizontalDistance = newX - origX;
            		for(int k = 0; k < horizontalDistance; k++){
                    	for(int l = 0; l < this.discretisation; l++){
                    		if(this.massMap[origX + k][origY + l].decrease()) this.overlap--;
                    		if(this.massMap[newX + this.discretisation - 1 - k][newY + l].increase()) this.overlap++;
                    	}
                    }
            		continue;
            	}else{
            		//MOVE LEFT
            		horizontalDistance = origX - newX;
            		for(int k = 0; k < horizontalDistance; k++){
                    	for(int l = 0; l < this.discretisation; l++){
                    		if(this.massMap[origX + this.discretisation - 1 - k][origY + l].decrease()) this.overlap--;
                    		if(this.massMap[newX + k][newY + l].increase()) this.overlap++;
                    	}
                    }
            		continue;
            	}
            }else if(origX == newX){
            	//VERTICAL MOVE
            	if(origY < newY){
            		//MOVE UP
            		verticalDistance = newY - origY;
            		for(int k = 0; k < this.discretisation; k++){
                    	for(int l = 0; l < verticalDistance; l++){
                    		if(this.massMap[origX + k][origY + l].decrease()) this.overlap--;
                    		if(this.massMap[newX + k][newY + this.discretisation - 1 - l].increase()) this.overlap++;
                    	}
                    }
            		continue;
            	}else{
            		//MOVE DOWN
            		verticalDistance = origY - newY;
            		for(int k = 0; k < this.discretisation; k++){
                    	for(int l = 0; l < verticalDistance; l++){
                    		if(this.massMap[origX + k][origY + this.discretisation - 1 - l].decrease()) this.overlap--;
                    		if(this.massMap[newX + k][newY + l].increase()) this.overlap++;
                    	}
                    }
            		continue;
            	}
            }
            for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < this.discretisation; l++){
            		if(this.massMap[origX + k][origY + l].decrease()) this.overlap--;
            		if(this.massMap[newX + k][newY + l].increase()) this.overlap++;
            	}
            }
    	}

    	if(debug){
    		//CONTROL MASS MAP
    		boolean ok = true;

        	int[][] controlMassMap = new int[this.gridWidth][this.gridHeight];
        			
        	for(LegalizerBlock block:this.blocks){	
            	int i = (int)Math.ceil(block.horizontal.coordinate * this.discretisation);
            	int j = (int)Math.ceil(block.vertical.coordinate * this.discretisation);
            		
            	for(int k = 0; k < this.discretisation; k++){
            		for(int l = 0; l < this.discretisation; l++){
            			controlMassMap[i + k][j + l]++;
            		}
            	}
        	}
        	
        	for(int i = 0; i < this.gridWidth; i++){
        		for(int j = 0; j < this.gridHeight; j++){
        			if(controlMassMap[i][j] != this.massMap[i][j].getMass()){
        				ok = false;
        			}
        		}
        	}
        	if(!ok){
        		System.out.println("Something is wrong in the incremental mass map update!");
        	}
        	
        	//CONTROL OVERLAP
        	int testOverlap = 0;
        			
            for(int x = 0; x < this.gridWidth; x++){
            	for(int y = 0; y < this.gridHeight; y++){
            		testOverlap += this.massMap[x][y].overlap();
            	}
            }
        	if(testOverlap != this.overlap){
        		System.out.println("Something is wrong in the incremental overlap update!");
        	}
    	}

    	if(timing) this.timer.time("Solve");
    }

    //PUSHING GRAVITY FORCES BETWEEN THE BLOCKS
    private void fpgaPullForces(){
    	if(timing) this.timer.start("Gravity Push Forces");
    	
    	//Local known variables
    	int area = this.discretisation * this.halfDiscretisation;
    	
    	for(LegalizerBlock block:this.blocks){ 		
	    	int x = (int)Math.ceil(block.horizontal.coordinate * this.discretisation);
	    	int y = (int)Math.ceil(block.vertical.coordinate * this.discretisation);
	    	
	    	double horizontalForce = 0.0;
	    	double verticalForce = 0.0;

	    	if(this.discretisation == 4){
	    		//LOOP UNROLLING
				horizontalForce += this.massMap[x][y].horizontalForce();
				verticalForce += this.massMap[x][y].verticalForce();
				
				horizontalForce += this.massMap[x][y + 1].horizontalForce();
				verticalForce += this.massMap[x][y + 1].verticalForce();
				
				horizontalForce += this.massMap[x][y + 2].horizontalForce();
				verticalForce -= this.massMap[x][y + 2].verticalForce();
		    	
				horizontalForce += this.massMap[x][y + 3].horizontalForce();
				verticalForce -= this.massMap[x][y + 3].verticalForce();

				horizontalForce += this.massMap[x + 1][y].horizontalForce();
				verticalForce += this.massMap[x + 1][y].verticalForce();
				
				horizontalForce += this.massMap[x + 1][y + 1].horizontalForce();
				verticalForce += this.massMap[x + 1][y + 1].verticalForce();
				
				horizontalForce += this.massMap[x + 1][y + 2].horizontalForce();
				verticalForce -= this.massMap[x + 1][y + 2].verticalForce();
		    	
				horizontalForce += this.massMap[x + 1][y + 3].horizontalForce();
				verticalForce -= this.massMap[x + 1][y + 3].verticalForce();				

				horizontalForce -= this.massMap[x + 2][y].horizontalForce();
				verticalForce += this.massMap[x + 2][y].verticalForce();

				horizontalForce -= this.massMap[x + 2][y + 1].horizontalForce();
				verticalForce += this.massMap[x + 2][y + 1].verticalForce();

				horizontalForce -= this.massMap[x + 2][y + 2].horizontalForce();
				verticalForce -= this.massMap[x + 2][y + 2].verticalForce();

				horizontalForce -= this.massMap[x + 2][y + 3].horizontalForce();
				verticalForce -= this.massMap[x + 2][y + 3].verticalForce();

				horizontalForce -= this.massMap[x + 3][y].horizontalForce();
				verticalForce += this.massMap[x + 3][y].verticalForce();

				horizontalForce -= this.massMap[x + 3][y + 1].horizontalForce();
				verticalForce += this.massMap[x + 3][y + 1].verticalForce();

				horizontalForce -= this.massMap[x + 3][y + 2].horizontalForce();
				verticalForce -= this.massMap[x + 3][y + 2].verticalForce();

				horizontalForce -= this.massMap[x + 3][y + 3].horizontalForce();
				verticalForce -= this.massMap[x + 3][y + 3].verticalForce();
	    	}else{
	    		Loc loc = null;
	    		for(int k = 0; k < this.halfDiscretisation; k++){
		    		for(int l = 0; l < this.halfDiscretisation; l++){
		    			loc = this.massMap[x+k][y+l];
		    			horizontalForce += loc.horizontalForce();
		    			verticalForce += loc.verticalForce();
		    		}
		    		for(int l = this.halfDiscretisation; l < this.discretisation; l++){
		    			loc = this.massMap[x+k][y+l];
		    			horizontalForce += loc.horizontalForce();
		    			verticalForce -= loc.verticalForce();
		    		}
		    	}
		    	for(int k = this.halfDiscretisation; k < this.discretisation; k++){
		    		for(int l = 0; l < this.halfDiscretisation; l++){
		    			loc = this.massMap[x+k][y+l];
		    			horizontalForce -= loc.horizontalForce();
		    			verticalForce += loc.verticalForce();
		    		}
		    		for(int l = this.halfDiscretisation; l < this.discretisation; l++){
		    			loc = this.massMap[x+k][y+l];
		    			horizontalForce -= loc.horizontalForce();
		    			verticalForce -= loc.verticalForce();
		    		}
		    	}
	    	}

	    	block.horizontal.setForce(horizontalForce / area);
	    	block.vertical.setForce(verticalForce / area);
    	}    	
    	if(timing) this.timer.time("Gravity Push Forces");
    }

    //Set legal coordinates of all blocks
    private void updateLegal(){
    	if(timing) this.timer.start("Update legal");
    	
    	for(LegalizerBlock block:this.blocks){
    		block.horizontal.coordinate = Math.round(block.horizontal.coordinate);
    		block.vertical.coordinate = Math.round(block.vertical.coordinate);
    		
    		this.legalX[block.index] = (int)block.horizontal.coordinate;
    		this.legalY[block.index] = (int)block.vertical.coordinate;
    	}
    	
    	if(timing) this.timer.time("Update legal");
    }
    private void shiftLegal(){//TODO
    	if(timing) this.timer.start("Shift legal");
    	
    	this.resetLegalMap();
    	
    	ArrayList<LegalizerBlock> unplacedBlocks = new ArrayList<LegalizerBlock>();
    	for(LegalizerBlock block:this.blocks){
    		int x = this.legalX[block.index];
    		int y = this.legalY[block.index];
    		
    		if(this.legalPostion(x, y)){
    			this.legalMap[x][y] = block;
    		}else{
    			unplacedBlocks.add(block);
    		}
    	}
    	if(unplacedBlocks.size() < this.blocks.length * 0.05){
        	int movingX = 0, movingY = 0;
        	
        	for(LegalizerBlock block:unplacedBlocks){
        		int x = this.legalX[block.index];
        		int y = this.legalY[block.index];
        		
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
    private void resetLegalMap(){
    	int width = this.legalMap.length;
    	int height = this.legalMap[0].length;
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			this.legalMap[x][y] = null;
    		}
    	}
    }
    private boolean legalPostion(int x, int y){
    	if(this.legal[x][y] && this.legalMap[x][y] == null){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    // Visual
    private void addVisual(String name){
    	if(interVisual){
    		if(timing) this.timer.start("Add Inter Visual");
    		
    		for(LegalizerBlock block:this.blocks){
    			this.visualX[block.index] = block.horizontal.coordinate;
    			this.visualY[block.index] = block.vertical.coordinate;	
    		}
    		this.addVisual(name, this.visualX, this.visualY);
    		
    		if(timing) this.timer.time("Add Inter Visual");
    	}
	}
    private void addVisual(){
		if(visual){
			if(timing) this.timer.start("Add Visual");

			for(LegalizerBlock block:this.blocks){
				this.visualX[block.index] = block.horizontal.coordinate;
				this.visualY[block.index] = block.vertical.coordinate;	
			}
			this.addVisual(String.format("gradient expand step %d", this.iteration), this.visualX, this.visualY);
			
			if(timing) this.timer.time("Add Visual");
		}
    }

    private class Loc {
    	boolean isLegal;
    	int mass;

    	double horizontalPotential;
    	double verticalPotential;

    	boolean validForce;

    	double horizontalForce;
    	double verticalForce;

    	Loc(){
    		this.reset();
    	}
    	void reset(){
    		this.mass = 0;
    		this.isLegal = false;
    		this.validForce = false;
    	}

    	void setLegal(boolean isLegal){
    		this.isLegal = isLegal;
    	}
    	
    	void resetPotential(){
    		this.horizontalPotential = 0.0;
    		this.verticalPotential = 0.0;
    		this.validForce = false;
    	}
    	void setHorizontalPotential(double potential){
    		this.horizontalPotential = potential;
    		this.validForce = false;
    	}
    	void setVerticalPotential(double potential){
    		this.verticalPotential =  potential;
    		this.validForce = false;
    	}

    	boolean increase(){
    		this.mass++;
    		this.validForce = false;
    		
    		if(!this.isLegal){
    			return true;//Overlap increases
    		}else if(this.mass > 1){
    			return true;//Overlap increases
    		}else{
    			return false;//No overlap increase
    		}
    	}
    	boolean decrease(){
    		this.mass--;
    		this.validForce = false;

    		if(!this.isLegal){
    			return true;//Overlap decreases
    		}else if(this.mass > 0){
    			return true;//Overlap decreases
    		}else{
    			return false;//No overlap decrease
    		}
    	}

    	int overlap(){
    		if(this.isLegal){
    			return Math.max(0, this.mass - 1);
    		}else{
    			return this.mass;
    		}
    	}
    	
    	int getMass(){
    		return this.mass;
    	}

    	void setForce(){
    		this.horizontalForce = 1.0 - 1.0/(this.mass + this.horizontalPotential);
    		this.verticalForce = 1.0 - 1.0/(this.mass + this.verticalPotential);
    	}
        private double horizontalForce(){
        	if(!this.validForce){
        		this.setForce();
        		this.validForce = true;
        	}
        	return this.horizontalForce;
        }
        private double verticalForce(){
        	if(!this.validForce){
        		this.setForce();
        		this.validForce = true;
        	}
        	return this.verticalForce;
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
    		this.speed = 0.0;
    		this.force = 0.0;
    	}
    	
    	void reset(){
    		this.force = 0.0;
    	}
    	
    	void setForce(double force){
    		this.force = force;
    	}
    	
    	void solve(double stepSize, double speedAveraging){
    		if(this.force != 0.0){
            	double newSpeed = stepSize * this.force;
            	
            	this.speed = speedAveraging * this.speed + (1 - speedAveraging) * newSpeed;
            	this.coordinate += this.speed;
    		}
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

    protected void initializeLegalizationAreas(){
    	//DO NOTHING
    }
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}