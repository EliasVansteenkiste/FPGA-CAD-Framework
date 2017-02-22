package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

class GradientLegalizer extends Legalizer {

	private LegalizerBlock[] blocks;

    private final int discretisation;
    private final Loc[][] massMap;

    private final double[] horizontalPotential;
    private final double[] verticalPotential;

    private final HashSet<Integer> illegalColumns;
    private int illegalColumn;
    private int firstIllegalColumn, lastIllegalColumn;

    private int iteration;
    private int overlap;

	private final double stepSize, speedAveraging;

	private final DetailedLegalizer detailedLegalizer;

	private static final boolean debug = false;
	private TimingTree timer;
    private static final boolean timing = false;
    private static final boolean visual = false;
    private static final boolean interVisual = false;

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

    	this.discretisation = 4;
    	if(this.discretisation % 2 != 0){
    		System.out.println("Discretisation should be even, now it is equal to " + this.discretisation);
    		System.exit(0);
    	}
    	if(this.discretisation > 20){
    		System.out.println("Discretisation should be smaller than 20, now it is equal to " + this.discretisation);
    		System.exit(0);
    	}
    	
    	this.stepSize = 0.75;
    	this.speedAveraging = 0.2;

    	int width = (this.width + 2) * this.discretisation;
    	int height = (this.height + 2) * this.discretisation;
    	this.massMap = new Loc[width][height];
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			this.massMap[x][y] = new Loc();
    		}
    	}
    	
    	this.horizontalPotential = new double[width];
    	this.verticalPotential = new double[height];
    	
    	this.illegalColumns = new HashSet<Integer>();

    	this.detailedLegalizer = new DetailedLegalizer(this.width, this.height);

    	if(timing){
    		this.timer = new TimingTree();
    	}
    	
    	boolean doVisual = visual || interVisual;
    	if(doVisual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	if(timing) this.timer.start("Legalize BlockType");

    	this.initializeData(blocksStart, blocksEnd);
    	
    	int maximumOverlap = this.discretisation * this.discretisation * (blocksEnd - blocksStart);
    	double allowedOverlap = maximumOverlap * 0.01;
    	double work = this.overlap - allowedOverlap;

    	this.ioPotential();
    	
    	int legalIterations = 500;
    	double progress = 0.0, maxPotential = 0.03;
    	this.iteration = 0;
    	do{
    		progress = 1.0 - (this.overlap - allowedOverlap) / work;
    		this.legalPotential(progress, maxPotential);
    		
        	this.applyPushingForces();
        	this.iteration += 1;
        }while(this.overlap > allowedOverlap && this.iteration < legalIterations);
    	
    	this.addVisual("intermediate legal solution");
    	
    	this.setIllegalCapacity();
    	int flipColumnIterations = 100;
    	int centerOfMass = (int)Math.round(this.horizontalCenterOfMass());
    	while(!this.illegalColumns.isEmpty()){
    		this.illegalColumn = this.findClosestColumn(centerOfMass);
    		if(this.firstIllegalColumn == -1 && this.lastIllegalColumn == -1){
    			this.firstIllegalColumn = this.illegalColumn;
    			this.lastIllegalColumn = this.illegalColumn;
    		}else if(this.illegalColumn < this.firstIllegalColumn){
    			this.firstIllegalColumn = this.illegalColumn;
    		}else if(this.illegalColumn > this.lastIllegalColumn){
    			this.lastIllegalColumn = this.illegalColumn;
    		}
    		this.flipColumn(this.illegalColumn, centerOfMass);
    		if(this.columnUsed(this.illegalColumn)){
        		this.iteration = 0;
            	do{
                	this.applyPushingForces();
                	this.iteration += 1;
                }while(this.overlap > allowedOverlap && this.iteration < flipColumnIterations);

            	this.addVisual("intermediate legal solution");
    		}
        	this.illegalColumns.remove(this.illegalColumn);
    	}
    	
    	this.addVisual("intermediate legal solution");

        //LEGAL SOLUTION
    	this.updateLegal();

    	this.addVisual("Legalized Postions");

    	//this.detailedLegalizer.shiftLegal(this.blocks, this.blockType, this.circuit, 0.05);
    	
    	this.updateLegal();

    	if(timing) this.timer.time("Legalize BlockType");
    }
    private int findClosestColumn(int centerOfMass){
    	if(timing) this.timer.start("Find Closest Column");
    	
		int closestColumn = Integer.MAX_VALUE;
		for(int column:this.illegalColumns){
			if(Math.abs(column - centerOfMass) < Math.abs(closestColumn - centerOfMass)){
				closestColumn = column;
			}
		}
		
		if(timing) this.timer.time("Find Closest Column");
		
		return closestColumn;
    }
    
    private boolean columnUsed(int column){
    	if(timing) this.timer.start("Column Used");
    	
    	for(LegalizerBlock block:this.blocks){
    		if(Math.abs(block.horizontal() - column) < 1){
    			if(timing) this.timer.time("Column Used");
    			return true;
    		}
    	}
    	if(timing) this.timer.time("Column Used");
    	return false;
    }

    //INITIALISATION
    private void initializeData(int blocksStart, int blocksEnd){
    	if(timing) this.timer.start("Initialize Data");
    	
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, this.linearX[b], this.linearY[b], this.blockType.getHeight() * this.heights[b]);
    	}

    	this.iteration = 0;
    	this.overlap = 0;
    	
    	this.setIllegalColumns(this.blockType);
    	this.resetGrid(this.blockType);
    	this.initializeMassMap();

    	boolean doVisual = visual || interVisual;
    	if(doVisual){
    		for(int i = 0; i < this.linearX.length; i++){
    			this.visualX[i] = this.linearX[i];
    			this.visualY[i] = this.linearY[i];
    		}
    	}
    	
    	if(timing) this.timer.time("Initialize Data");
    }
    private void setIllegalColumns(BlockType blockType){
    	if(timing) this.timer.start("Set Illegal columns");
    	
    	this.illegalColumns.clear();
    	this.illegalColumn = -1;
    	this.firstIllegalColumn = -1;
    	this.lastIllegalColumn = -1;
    	
    	for(int column = 1; column < this.width + 1; column++){
    		if(!this.circuit.getColumnType(column).equals(blockType)){
    			this.illegalColumns.add(column);
    		}
    	}

    	if(timing) this.timer.time("Set Illegal columns");
    }
    private void resetGrid(BlockType blockType){
    	if(timing) this.timer.start("Reset Grid");

    	for(int x = 0; x < this.width + 2; x++){
    		for(int y = 0; y < this.height + 2; y++){
    			for(int k = 0; k < this.discretisation; k++){
    				for(int l = 0; l < this.discretisation; l++){
    					this.massMap[x * this.discretisation + k][y * this.discretisation + l].reset(1);
    				}
    			}
    		}
    	}

    	if(timing) this.timer.time("Reset Grid");
    }
    private void initializeMassMap(){
    	if(timing) this.timer.start("Initialize Mass Map");

    	for(LegalizerBlock block:this.blocks){
        		
        	int x = (int)Math.ceil(block.horizontal() * this.discretisation);
        	int y = (int)Math.ceil(block.vertical() * this.discretisation);
        		
            for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < this.discretisation * block.height(); l++){
            		if(this.massMap[x + k][y + l].increase()) this.overlap++;
            	}
            }
    	}
    	
    	if(timing) this.timer.time("Initialize Mass Map");
    }

    //POTENTIAL
    private void setIllegalCapacity(){
    	if(timing) this.timer.start("Set Illegal Capacity");

    	for(int x = 0; x < this.width + 2; x++){
    		if(this.illegalColumns.contains(x)){
        		for(int y = 0; y < this.height + 2; y++){
        			for(int k = 0; k < this.discretisation; k++){
        				for(int l = 0; l < this.discretisation; l++){
        					this.overlap += this.massMap[x * this.discretisation + k][y * this.discretisation + l].setCapacity(0);
        				}
        			}
        		}
    		}
    	}
    	
    	if(timing) this.timer.time("Set Illegal Capacity");
    }
    private void ioPotential(){
    	if(timing) this.timer.start("IO Potential");
    	
    	for(int d = 0; d < this.discretisation; d++){
    		this.horizontalPotential[d] = this.discretisation - d;
    		this.verticalPotential[d] = this.discretisation - d;
    		
    		this.horizontalPotential[(this.width + 2) * this.discretisation - 1 - d] = this.discretisation - d;
    		this.verticalPotential[(this.height + 2) * this.discretisation - 1 - d] = this.discretisation - d;
    	}
    	
    	if(timing) this.timer.time("IO Potential");
    }
    private void legalPotential(double progress, double maxPotential){
    	if(timing) this.timer.start("Legal Potential");
    	
    	maxPotential = maxPotential * progress;
 	
    	//Horizontal
    	for(int x = 1; x < this.width + 1; x++){
    		if(this.illegalColumns.contains(x)){
    			this.illegalHorizontal(x, maxPotential);
    		}else{
    			this.legalHorizontal(x, maxPotential);
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
    private void legalHorizontal(int x, double maxPotential){
    	for(int k = 0; k < this.discretisation; k++){
			this.horizontalPotential[x*this.discretisation + k] = Math.abs(maxPotential - 2 * maxPotential * (k + 0.5) / this.discretisation);
		}
    }
    private void illegalHorizontal(int x, double maxPotential){
    	for(int k = 0; k < this.discretisation; k++){
			this.horizontalPotential[x*this.discretisation + k] = maxPotential - maxPotential / this.discretisation;
		}
    }
    private void flipColumn(int column, int centerOfMass){
    	if(timing) this.timer.start("Flip Column");
    	
    	if(column < centerOfMass){
        	for(int d = 0; d < this.discretisation; d++){
        		this.horizontalPotential[column*this.discretisation + d] = d + 1;
        	}
    	}else if(column > centerOfMass){
        	for(int d = 0; d < this.discretisation; d++){
        		this.horizontalPotential[column*this.discretisation + d] = this.discretisation - d;
        	}
    	}else{
        	for(int d = 0; d < this.discretisation; d++){
        		this.horizontalPotential[column*this.discretisation + d] = this.discretisation - d;
        	}
    	}
    	
    	for(int x = this.firstIllegalColumn + 1; x < this.lastIllegalColumn; x++){
    		for(int d = 0; d < this.discretisation; d++){
    			this.horizontalPotential[x*this.discretisation + d] = this.discretisation;
    		}
    	}
    	
    	if(timing) this.timer.time("Flip Column");
    }
    private double horizontalCenterOfMass(){
    	double sum = 0.0;
    	int mass = 0;
    	
    	for(LegalizerBlock block:this.blocks){
    		sum += block.horizontal() * block.height();
    		mass += block.height();
    	}
    	return sum / mass;
    }

    //PUSHING GRAVITY FORCES
    private void applyPushingForces(){
    	if(timing) this.timer.start("Apply Pushing Forces");

		this.fpgaPullForces();
		this.solve();
		
		this.addVisual();
		
		if(timing) this.timer.time("Apply Pushing Forces");
    }
    private void solve(){
    	if(timing) this.timer.start("Solve");

    	int origX, origY, newX, newY;
    	
    	for(LegalizerBlock block:this.blocks){
    		origX = (int)Math.ceil(block.horizontal() * this.discretisation);
    		origY = (int)Math.ceil(block.vertical() * this.discretisation);
        		
    		block.solve(this.stepSize, this.speedAveraging);
        		
    		if(block.horizontal() > this.width) block.setHorizontal(this.width);
    		if(block.horizontal() < 1) block.setHorizontal(1);
        		
    		if(block.vertical() + block.height() - 1 > this.height) block.setVertical(this.height - block.height() + 1);
    		if(block.vertical() < 1) block.setVertical(1);
                
    		newX = (int)Math.ceil(block.horizontal() * this.discretisation);
    		newY = (int)Math.ceil(block.vertical() * this.discretisation);
                
    		//UPDATE MASS MAP
    		this.updateMassMap(origX, origY, newX, newY, block.height());
    	}

    	if(debug){
    		this.testMassMap();
    		this.testOverlap();
    	}

    	if(timing) this.timer.time("Solve");
    }
    private void updateMassMap(int origX, int origY, int newX, int newY, int height){
        if(origX == newX && origY == newY){
        	//Do nothing
        }else if(origY == newY){
        	this.horizontalMassMapUpdate(origX, origY, newX, newY, height);
        }else if(origX == newX){
        	this.verticalMassMapUpdate(origX, origY, newX, newY, height);
        }else{
        	this.twoDimensionalMassMapUpdate(origX, origY, newX, newY, height);
        }
    }
    private void horizontalMassMapUpdate(int origX, int origY, int newX, int newY, int height){
    	if(origX < newX){//Move right
    		int horizontalDistance = newX - origX;
    		for(int k = 0; k < horizontalDistance; k++){
            	for(int l = 0; l < this.discretisation * height; l++){
            		if(this.massMap[origX + k][origY + l].decrease()) this.overlap--;
            		if(this.massMap[newX + this.discretisation - 1 - k][newY + l].increase()) this.overlap++;
            	}
            }
    	}else{//Move left
    		int horizontalDistance = origX - newX;
    		for(int k = 0; k < horizontalDistance; k++){
            	for(int l = 0; l < this.discretisation * height; l++){
            		if(this.massMap[origX + this.discretisation - 1 - k][origY + l].decrease()) this.overlap--;
            		if(this.massMap[newX + k][newY + l].increase()) this.overlap++;
            	}
            }
    	}
    }
    private void verticalMassMapUpdate(int origX, int origY, int newX, int newY, int height){
    	if(origY < newY){//Move up
    		int verticalDistance = newY - origY;
    		for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < verticalDistance; l++){
            		if(this.massMap[origX + k][origY + l].decrease()) this.overlap--;
            		if(this.massMap[newX + k][newY + height * this.discretisation - 1 - l].increase()) this.overlap++;
            	}
            }
    	}else{//Move down
    		int verticalDistance = origY - newY;
    		for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < verticalDistance; l++){
            		if(this.massMap[origX + k][origY + height * this.discretisation - 1 - l].decrease()) this.overlap--;
            		if(this.massMap[newX + k][newY + l].increase()) this.overlap++;
            	}
            }
    	}
    }
    private void twoDimensionalMassMapUpdate(int origX, int origY, int newX, int newY, int height){
        for(int k = 0; k < this.discretisation; k++){
        	for(int l = 0; l < this.discretisation * height; l++){
        		if(this.massMap[origX + k][origY + l].decrease()) this.overlap--;
        		if(this.massMap[newX + k][newY + l].increase()) this.overlap++;
        	}
        }
    }
    private void testMassMap(){
    	int[][] testMassMap = new int[this.massMap.length][this.massMap[0].length];
    			
    	for(LegalizerBlock block:this.blocks){	
    		
    		for(int h = 0; h < block.height(); h++){
    			
            	int i = (int)Math.ceil(block.horizontal() * this.discretisation);
            	int j = (int)Math.ceil((block.vertical() + h) * this.discretisation);
            		
            	for(int k = 0; k < this.discretisation; k++){
            		for(int l = 0; l < this.discretisation; l++){
            			testMassMap[i + k][j + l]++;
            		}
            	}
    		}
    	}
    	
    	for(int i = 0; i < this.massMap.length; i++){
    		for(int j = 0; j < this.massMap[0].length; j++){
    			if(testMassMap[i][j] != this.massMap[i][j].mass){
    				System.out.println("Something is wrong in the incremental mass map update");
    				System.out.println("\tLocation [" + i + "," + j + "]");
    				System.out.println("\t\tTest Mass Map: " + testMassMap[i][j]);
    				System.out.println("\t\tMass Map: " + this.massMap[i][j]);
    			}
    		}
    	}
    }
    private void testOverlap(){
    	int testOverlap = 0;
		
        for(int x = 0; x < this.massMap.length; x++){
        	for(int y = 0; y < this.massMap[0].length; y++){
        		testOverlap += this.massMap[x][y].overlap();
        	}
        }
    	if(testOverlap != this.overlap){
    		System.out.println("Something is wrong in the incremental overlap update!");
    		System.out.println("\tTest overlap: " + testOverlap);
    		System.out.println("\tOverlap: " + this.overlap);
    	}
    }

    //PUSHING GRAVITY FORCES BETWEEN THE BLOCKS
    private void fpgaPullForces(){
    	if(timing) this.timer.start("Gravity Push Forces");

    	int area = this.discretisation * this.discretisation / 2;
    	
    	for(LegalizerBlock block:this.blocks){ 		
			double horizontalForce = 0.0;
	    	double verticalForce = 0.0;
	    		
	    	int x = (int)Math.ceil(block.horizontal() * this.discretisation);
	    	int y = (int)Math.ceil(block.vertical() * this.discretisation);
	    	
	    	int height = block.height();
	    	
    		for(int k = 0; k < this.discretisation / 2; k++){
	    		for(int l = 0; l < this.discretisation / 2 * height; l++){
	    			horizontalForce += this.massMap[x + k][y + l].horizontalForce(this.horizontalPotential[x + k]);
	    			verticalForce += this.massMap[x + k][y + l].verticalForce(this.verticalPotential[y + l]);
	    		}
	    		for(int l = this.discretisation / 2 * height; l < this.discretisation * height; l++){
	    			horizontalForce += this.massMap[x + k][y + l].horizontalForce(this.horizontalPotential[x + k]);
	    			verticalForce -= this.massMap[x + k][y + l].verticalForce(this.verticalPotential[y + l]);
	    		}
	    	}
	    	for(int k = this.discretisation / 2; k < this.discretisation; k++){
	    		for(int l = 0; l < this.discretisation / 2 * height; l++){
	    			horizontalForce -= this.massMap[x + k][y + l].horizontalForce(this.horizontalPotential[x + k]);
	    			verticalForce += this.massMap[x + k][y + l].verticalForce(this.verticalPotential[y + l]);
	    		}
	    		for(int l = this.discretisation / 2 * height; l < this.discretisation * height; l++){
	    			horizontalForce -= this.massMap[x + k][y + l].horizontalForce(this.horizontalPotential[x + k]);
	    			verticalForce -= this.massMap[x + k][y + l].verticalForce(this.verticalPotential[y + l]);
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
    		if(!block.isLegal){	
    			block.legalize();
    		}
    		
    		this.legalX[block.index] = block.legalHorizontal();
    		this.legalY[block.index] = block.legalVertical();
    	}
    	
    	if(timing) this.timer.time("Update Legal");
    }
    
    // Visual
    private void addVisual(String name){
    	boolean addVisual = interVisual || visual;
    	if(addVisual){
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

    class Loc {
    	int mass;
    	int capacity;

    	Loc(){
    		this.reset(0);
    	}
    	
    	void reset(int capacity){
    		this.mass = 0;
    		this.capacity = capacity;
    	}
    	int setCapacity(int capacity){
    		int oldCapacity = this.capacity;
    		this.capacity = capacity;
    		
    		//Return the overlap increase of decrease
    		return  Math.max(0, this.mass - this.capacity) - Math.max(0, this.mass - oldCapacity);
    	}

    	boolean increase(){
    		this.mass++;
    		
    		//Return true if overlap increases
    		if(this.mass > this.capacity){
    			return true;
    		}else{
    			return false;
    		}
    	}
    	boolean decrease(){
    		this.mass--;

    		//Return true if overlap decreases
    		if(this.mass >= this.capacity){
    			return true;
    		}else{
    			return false;
    		}
    	}

    	int overlap(){
    		return Math.max(0, this.mass - this.capacity);
    	}

        private double horizontalForce(double potential){
        	return 1.0 - 1.0/(this.mass + potential);
        }
        private double verticalForce(double potential){
        	return 1.0 - 1.0/(this.mass + potential);
        }
    }
    
    //LegalizerBlock
    class LegalizerBlock {
    	final int index;

    	final Dimension horizontal;
    	final Dimension vertical;

    	final int height;

    	boolean isLegal;

    	LegalizerBlock(int index, double x, double y, int height){
    		this.index = index;

    		this.horizontal = new Dimension(x);
    		this.vertical = new Dimension(y);

    		this.height = height;

    		this.isLegal = false;
    	}
    	
    	void solve(double stepSize, double speedAveraging){
    		this.horizontal.solve(stepSize, speedAveraging);
    		this.vertical.solve(stepSize, speedAveraging);
    	}
    	void legalize(){
    		this.horizontal.coordinate = (int)Math.round(this.horizontal());
    		this.vertical.coordinate = (int)Math.round(this.vertical());
    		this.isLegal = true;
    	}

    	void setHorizontal(double horizontal){
    		this.horizontal.coordinate = horizontal;
    	}
    	void setVertical(double vertical){
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
    	int height(){
    		return this.height;
    	}
    }
    
    class Dimension {
    	double coordinate;
    	double speed;
    	double force;
    	
    	Dimension(double coordinate){
    		this.coordinate = coordinate;
    		this.speed = 0.0;
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

    protected void initializeLegalizationAreas(){
    	//DO NOTHING
    }
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}