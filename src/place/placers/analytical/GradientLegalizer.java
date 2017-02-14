package place.placers.analytical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private final ArrayList<LegalizerBlock> unplacedBlocks;
    private final ArrayList<LegalizerBlock> moveBlocks;

    private final int gridWidth;
    private final int gridHeight;

    private int iteration;
    private int overlap;

	private final double stepSize, speedAveraging;

	private static final boolean debug = false;
	private Timer timer;
    private static final boolean timing = false;
    private static final boolean visual = false;
    private static final boolean interVisual = true;
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
    	this.unplacedBlocks = new ArrayList<LegalizerBlock>();
    	this.moveBlocks = new ArrayList<LegalizerBlock>();

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
    	
    	int legalIterations = 500;
    	if(timing) legalIterations = 500;

    	if(printPotential) this.printPotential();

    	this.iteration = 0;
    	do{
        	this.applyPushingForces();
        	this.iteration += 1;
        }while(this.overlap > allowedOverlap && this.iteration < legalIterations);
    	
    	this.addVisual("Legal Potential");

    	//ILLEGAL POTENTIAL
    	this.illegalPotential();

    	int illegalIterations = 500;
    	if(timing) illegalIterations = 500;
    	
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
    	
    	//TODO DUMMY FUNCTIONALITY
    	ArrayList<Integer> illegalColumns = new ArrayList<Integer>();
       	illegalColumns.add(32);
    	illegalColumns.add(33);
    	illegalColumns.add(34);

    	for(int x = 1; x < this.width + 1; x++){
    		if(illegalColumns.contains(x)){
    			for(int y = 1; y < this.height + 1; y++){
	    			this.legal[x][y] = false;
	    		}
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
    	
    	int start = 0, end = 0;
    	//Set the potential of illegal columns
    	for(int x = 1; x < this.width + 1; x++){
    		if(!this.legal[x][1]){
    			start = x;
    			
        		while(!this.legal[x][1]){
        			x++;
        			
        			if(x == this.width + 1){
        				break;
        			}
        		}
        		end = x;
        		
            	int width = end - start;
            	width *= this.discretisation;
            	
            	//TODO SLIDE DIRECTION?
            	for(int k = 0; k < width; k++){
                	for(int y = 1; y < this.height + 1; y+=2){
                		for(int l = 0; l < this.discretisation; l++){
                			this.massMap[start*this.discretisation + k][y*this.discretisation + l].setHorizontalPotential(k + 1);
                		}
                	}
                	for(int y = 2; y < this.height + 1; y+=2){
                		for(int l = 0; l < this.discretisation; l++){
                			this.massMap[start*this.discretisation + k][y*this.discretisation + l].setHorizontalPotential(width - k);
                		}
                	}
            	}
    		}
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
        	int x = (int)Math.ceil(block.horizontal() * this.discretisation);
        	int y = (int)Math.ceil(block.vertical() * this.discretisation);
        		
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

            origX = (int)Math.ceil(block.horizontal() * this.discretisation);
            origY = (int)Math.ceil(block.vertical() * this.discretisation);
    		
    		block.solve(this.stepSize, this.speedAveraging);
    		
    		if(block.horizontal() > this.width) block.setHorizontal(this.width);
    		if(block.horizontal() < 1) block.setHorizontal(1);
    		
    		if(block.vertical() > this.height) block.setVertical(this.height);
    		if(block.vertical() < 1) block.setVertical(1);
            
            newX = (int)Math.ceil(block.horizontal() * this.discretisation);
            newY = (int)Math.ceil(block.vertical() * this.discretisation);
            
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
            	int i = (int)Math.ceil(block.horizontal() * this.discretisation);
            	int j = (int)Math.ceil(block.vertical() * this.discretisation);
            		
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
	    	int x = (int)Math.ceil(block.horizontal() * this.discretisation);
	    	int y = (int)Math.ceil(block.vertical() * this.discretisation);
	    	
	    	double horizontalForce = 0.0;
	    	double verticalForce = 0.0;

	    	if(this.discretisation == 4){
	    		//LOOP UNROLLING
				horizontalForce += this.massMap[x][y].horizontalForce();
				horizontalForce += this.massMap[x][y + 1].horizontalForce();
				horizontalForce += this.massMap[x][y + 2].horizontalForce();
				horizontalForce += this.massMap[x][y + 3].horizontalForce();
				horizontalForce += this.massMap[x + 1][y].horizontalForce();
				horizontalForce += this.massMap[x + 1][y + 1].horizontalForce();
				horizontalForce += this.massMap[x + 1][y + 2].horizontalForce();
				horizontalForce += this.massMap[x + 1][y + 3].horizontalForce();
				horizontalForce -= this.massMap[x + 2][y].horizontalForce();
				horizontalForce -= this.massMap[x + 2][y + 1].horizontalForce();
				horizontalForce -= this.massMap[x + 2][y + 2].horizontalForce();
				horizontalForce -= this.massMap[x + 2][y + 3].horizontalForce();
				horizontalForce -= this.massMap[x + 3][y].horizontalForce();
				horizontalForce -= this.massMap[x + 3][y + 1].horizontalForce();
				horizontalForce -= this.massMap[x + 3][y + 2].horizontalForce();
				horizontalForce -= this.massMap[x + 3][y + 3].horizontalForce();
				
				verticalForce += this.massMap[x][y].verticalForce();
				verticalForce += this.massMap[x][y + 1].verticalForce();
				verticalForce -= this.massMap[x][y + 2].verticalForce();
				verticalForce -= this.massMap[x][y + 3].verticalForce();
				verticalForce += this.massMap[x + 1][y].verticalForce();
				verticalForce += this.massMap[x + 1][y + 1].verticalForce();
				verticalForce -= this.massMap[x + 1][y + 2].verticalForce();
				verticalForce -= this.massMap[x + 1][y + 3].verticalForce();
				verticalForce += this.massMap[x + 2][y].verticalForce();
				verticalForce += this.massMap[x + 2][y + 1].verticalForce();
				verticalForce -= this.massMap[x + 2][y + 2].verticalForce();
				verticalForce -= this.massMap[x + 2][y + 3].verticalForce();
				verticalForce += this.massMap[x + 3][y].verticalForce();
				verticalForce += this.massMap[x + 3][y + 1].verticalForce();
				verticalForce -= this.massMap[x + 3][y + 2].verticalForce();
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
    	if(timing) this.timer.start("Update Legal");
    	
    	for(LegalizerBlock block:this.blocks){
    		block.legalize();
    		
    		this.legalX[block.index] = block.legalHorizontal();
    		this.legalY[block.index] = block.legalVertical();
    	}
    	
    	if(timing) this.timer.time("Update Legal");
    }
    private void shiftLegal(){
    	if(timing) this.timer.start("Shift Legal");
    	Timer timer = new Timer();
    	timer.start("Shift Legal");
    	
    	this.resetLegalMap();
    	
    	for(LegalizerBlock block:this.blocks){
    		int x = this.legalX[block.index];
    		int y = this.legalY[block.index];
    		
    		if(this.legalPostion(x, y)){
    			this.legalMap[x][y] = block;
    		}else{
    			this.unplacedBlocks.add(block);
    		}
    	}
    	if(this.unplacedBlocks.size() < this.blocks.length * 0.05){
    		
    		while(!this.unplacedBlocks.isEmpty()){
    			
    			//Find block that leads to minimal displacement
    			LegalizerBlock overlappingBlock = this.unplacedBlocks.get(0);
    			int minimalDisplacement = this.legalizationDisplacement(overlappingBlock);
    			
    			for(LegalizerBlock candidateBlock:this.unplacedBlocks){
    				int legalizeDisplacement = this.legalizationDisplacement(candidateBlock);
    				if(legalizeDisplacement < minimalDisplacement){
    					overlappingBlock = candidateBlock;
    					minimalDisplacement = legalizeDisplacement;
    				}
    			}
    			
    			this.unplacedBlocks.remove(overlappingBlock);
        		
        		Direction movingDirection = bestMovingDirection(overlappingBlock);

        		int x = overlappingBlock.legalHorizontal();
        		int y = overlappingBlock.legalVertical();

        		this.moveBlocks.clear();
        		
        		if(movingDirection.equals(Direction.LEFT)){
        			while(!this.legalPostion(x, y)){
        				if(this.legal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			x--;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock moveBlock:this.moveBlocks){
        				x = moveBlock.legalHorizontal();
        				y = moveBlock.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					x--;
        				}
        				this.legalMap[x][y] = moveBlock;
        				this.legalMap[moveBlock.legalHorizontal()][moveBlock.legalVertical()] = null;
        				moveBlock.setHorizontal(x);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			x--;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setHorizontal(x);
        		}else if(movingDirection.equals(Direction.RIGHT)){
        			while(!this.legalPostion(x, y)){
        				if(this.legal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			x++;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock moveBlock:this.moveBlocks){
        				x = moveBlock.legalHorizontal();
        				y = moveBlock.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					x++;
        				}
        				
        				this.legalMap[x][y] = moveBlock;
        				this.legalMap[moveBlock.legalHorizontal()][moveBlock.legalVertical()] = null;
        				moveBlock.setHorizontal(x);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			x++;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setHorizontal(x);
        		}else if(movingDirection.equals(Direction.DOWN)){
        			while(!this.legalPostion(x, y)){
        				if(this.legal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			y--;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock moveBlock:this.moveBlocks){
        				x = moveBlock.legalHorizontal();
        				y = moveBlock.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					y--;
        				}
        				
        				this.legalMap[x][y] = moveBlock;
        				this.legalMap[moveBlock.legalHorizontal()][moveBlock.legalVertical()] = null;
        				moveBlock.setVertical(y);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			y--;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setVertical(y);
        		}else if(movingDirection.equals(Direction.UP)){
        			while(!this.legalPostion(x, y)){
        				if(this.legal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			y++;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock block:this.moveBlocks){
        				x = block.legalHorizontal();
        				y = block.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					y++;
        				}
        				
        				this.legalMap[x][y] = block;
                		this.legalX[block.index] = block.legalHorizontal();
                		this.legalY[block.index] = block.legalVertical();
                		
        				this.legalMap[block.legalHorizontal()][block.legalVertical()] = null;
        				block.setVertical(y);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			y++;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setVertical(y);
        		}
        	}
        	for(LegalizerBlock block:this.blocks){
        		this.legalX[block.index] = block.legalHorizontal();
        		this.legalY[block.index] = block.legalVertical();
        	}
        	this.addVisual();
    	}
    	timer.time("Shift Legal");
    	if(timing) this.timer.time("Shift Legal");
    }
    private void resetLegalMap(){
    	if(timing) this.timer.start("Reset Legal");
    	
    	int width = this.legalMap.length;
    	int height = this.legalMap[0].length;
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			this.legalMap[x][y] = null;
    		}
    	}
    	
    	this.unplacedBlocks.clear();
    	this.moveBlocks.clear();
    	
    	if(timing) this.timer.time("Reset Legal");
    }
    private boolean legalPostion(int x, int y){
    	if(this.legal[x][y] && this.legalMap[x][y] == null){
    		return true;
    	}else{
    		return false;
    	}
    }
    private int legalizationDisplacement(LegalizerBlock block){
		int left = this.displacement(block, Direction.LEFT);
		int right = this.displacement(block, Direction.RIGHT);
		int down = this.displacement(block, Direction.DOWN);
		int up = this.displacement(block, Direction.UP);
		
		return Math.min(Math.min(left, right), Math.min(down, up));
    }
    private int displacement(LegalizerBlock block, Direction direction){
    	int x = this.legalX[block.index];
		int y = this.legalY[block.index];
		
		if(direction.equals(Direction.LEFT)){
			int left = 0;
			while(!this.legalPostion(x, y)){
				left++;
				x--;
				
				if(x == 0){
					return Integer.MAX_VALUE;
				}
			}
			return left;
		}else if(direction.equals(Direction.RIGHT)){
			int right = 0;
			while(!this.legalPostion(x, y)){
				right++;
				x++;
				
				if(x == this.width + 1){
					return Integer.MAX_VALUE;
				}
			}
			return right;
		}else if(direction.equals(Direction.DOWN)){
			int down = 0;
			while(!this.legalPostion(x,y)){
				down++;
				y--;
				
				if(y == 0){
					return Integer.MAX_VALUE;
				}
			}
			return down;
		}else if(direction.equals(Direction.UP)){
			int up = 0;
			while(!this.legalPostion(x, y)){
				up++;
				y++;
				
				if(y == this.height + 1){
					return Integer.MAX_VALUE;
				}
			}
			return up;
		}
		return 0;
    }
    private Direction bestMovingDirection(LegalizerBlock block){
		int left = this.displacement(block, Direction.LEFT);
		int right = this.displacement(block, Direction.RIGHT);
		int down = this.displacement(block, Direction.DOWN);
		int up = this.displacement(block, Direction.UP);

		int min = Math.min(Math.min(left, right), Math.min(down, up));
		
		if(min == Integer.MAX_VALUE){
			System.out.println("All FPGA boundaries hit during shifting legalization of the blocks");
			return null;
		}

		if(left == min){
			return Direction.LEFT;
		}else if(right == min){
			return Direction.RIGHT;
		}else if(down == min){
			return Direction.DOWN;
		}else if(up == min){
			return Direction.UP;
		}
		
		System.out.println("Problem in best moving direction => min displacement equals " + min);
		return null;
    }
    
    private enum Direction {
    	LEFT,
    	RIGHT,
    	UP,
    	DOWN
    }
    
    // Visual
    private void addVisual(String name){
    	if(interVisual){
    		if(timing) this.timer.start("Add Inter Visual");
    		
    		for(LegalizerBlock block:this.blocks){
    			this.visualX[block.index] = block.horizontal();
    			this.visualY[block.index] = block.vertical();	
    		}
    		this.addVisual(name, this.visualX, this.visualY);
    		
    		if(timing) this.timer.time("Add Inter Visual");
    	}
	}
    private void addVisual(){
		if(visual){
			if(timing) this.timer.start("Add Visual");

			for(LegalizerBlock block:this.blocks){
				this.visualX[block.index] = block.horizontal();
				this.visualY[block.index] = block.vertical();	
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
    	private final int index;
    	
    	private final Dimension horizontal;
    	private final Dimension vertical;
    	
    	private boolean isLegal;

    	LegalizerBlock(int index, double x, double y){
    		this.index = index;
    		
    		this.horizontal = new Dimension(x);
    		this.vertical = new Dimension(y);
    		
    		this.isLegal = false;
    	}
    	
    	void reset(){
    		this.horizontal.reset();
    		this.vertical.reset();
    	}
    	
    	void solve(double stepSize, double speedAveraging){
    		this.horizontal.solve(stepSize, speedAveraging);
    		this.vertical.solve(stepSize, speedAveraging);
    	}
    	void legalize(){
    		this.setHorizontal((int)Math.round(this.horizontal()));
    		this.setVertical((int)Math.round(this.vertical()));
    		this.isLegal = true;
    	}
    	
    	void setHorizontal(int horizontal){
    		this.horizontal.coordinate = horizontal;
    	}
    	void setVertical(int vertical){
    		this.vertical.coordinate = vertical;
    	}
    	
    	double horizontal(){
    		return this.horizontal.coordinate;
    	}
    	double vertical(){
    		return this.vertical.coordinate;	
    	}
    	int legalHorizontal(){
    		if(this.isLegal){
    			return (int)this.horizontal();
    		}else{
    			System.out.println("The block is not legal!\n\t x: " + this.horizontal() + " y: " + this.vertical());
    			return 0;
    		}
    		
    	}
    	int legalVertical(){
    		if(this.isLegal){
    			return (int)this.vertical();
    		}else{
    			System.out.println("The block is not legal!\n\t x: " + this.horizontal() + " y: " + this.vertical());
    			return 0;
    		}
    	}
    }
    
    private class Dimension {
    	double coordinate;
    	double speed;
    	double force;
    	
    	Dimension(double coordinate){
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