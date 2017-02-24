package place.placers.analytical;

import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.util.TimingTree;
import place.visual.PlacementVisualizer;

/*
 * In this legalizer we remove the illegal columns before spreading the blocks out.
 * This way we can spread the blocks on a homogeneous FPGA.
 * Afterwards the illegal columns are inserted.
 */

class GradientLegalizer extends Legalizer {

    private final int discretisation;
	private double stepSize, speedAveraging;
	
	private LegalizerBlock[] blocks;
    private int[][] massMap;
    
    private int legalColumns, illegalColumns;

    private double[] horizontalPotential;
    private double[] verticalPotential;

    private int iteration;
    private int overlap;
    
    final HeapLegalizer detailedLegalizer;

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
    	
    	this.stepSize = 0.75;
    	this.speedAveraging = 0.2;
    	
    	this.detailedLegalizer = new HeapLegalizer(
                					circuit,
                					blockTypes,
                					blockTypeIndexStarts,
                					linearX,
                					linearY,
                					legalX,
                					legalY,
                					heights,
                					visualizer,
                					blockIndexes);

    	if(timing){
    		this.timer = new TimingTree();
    	}
    	
    	boolean doVisual = false;
    	if(visual) doVisual = true;
    	if(interVisual) doVisual = true;
    	if(doVisual){
    		this.visualX = new double[this.linearX.length];
    		this.visualY = new double[this.linearY.length];
    	}
    }
 
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	if(this.blockType.equals(BlockType.getBlockTypes(BlockCategory.CLB).get(0))){
        	if(timing) this.timer.start("Legalize BlockType");

        	this.initializeData(blocksStart, blocksEnd);
        	
        	this.addVisual("Start of gradient legalization");
        	
        	int maximumOverlap = 0;
        	for(LegalizerBlock block:this.blocks){
        		maximumOverlap += this.discretisation * this.discretisation * block.height;
        	}
        	double allowedOverlap = maximumOverlap * 0.05;
        	double work = this.overlap - allowedOverlap;
        	
        	//this.stepSize = 1.5 * (double)this.overlap / maximumOverlap;

        	this.ioPotential();
        	
        	int maxNumIterations = this.blocks.length;

        	double progress = 0.0, maxPotential = 0.05;
        	this.iteration = 0;
        	do{
        		
            	progress = 1.0 - (this.overlap - allowedOverlap) / work;
            	this.legalPotential(progress, maxPotential);

            	this.applyPushingForces();
            	this.iteration += 1;
            }while(this.overlap > allowedOverlap && this.iteration < maxNumIterations);
        	
        	this.addVisual("After gradient descent");
        	
        	this.updateLegal();
        	
        	this.addVisual("Update Legal");
        	
        	this.insertIllegalColumns();
        	
        	this.updateLegal();
        	
        	this.addVisual("Insert illegal columns");

        	if(timing) this.timer.time("Legalize BlockType");
    	}else{
    		this.detailedLegalizer.legalize(1.0, this.blockType);
        	for(int i = blocksStart; i < blocksEnd; i++){
        		this.legalX[i] = this.detailedLegalizer.getLegalX()[i];
        		this.legalY[i] = this.detailedLegalizer.getLegalY()[i];
        	}
    	}
    }

    //INITIALISATION
    private void initializeData(int blocksStart, int blocksEnd){
    	if(timing) this.timer.start("Initialize Data");

    	//Initialize all blocks
    	this.blocks = new LegalizerBlock[blocksEnd - blocksStart];
    	for(int b = blocksStart; b < blocksEnd; b++){
    		this.blocks[b - blocksStart] = new LegalizerBlock(b, this.linearX[b], this.linearY[b], this.blockType.getHeight() * this.heights[b], this.discretisation);
    	}
    	
    	this.legalColumns = 0;
    	this.illegalColumns = 0;
    	for(int column = 1; column <= this.width; column++){
    		if(this.circuit.getColumnType(column).equals(this.blockType)){
    			this.legalColumns += 1;
    		}else{
    			this.illegalColumns += 1;
    		}
    	}
    	
    	double scalingFactor = (double)this.legalColumns / (this.legalColumns + this.illegalColumns);
    	for(LegalizerBlock block:this.blocks){
    		block.scaleHorizontal(scalingFactor);
    	}

    	int width = (this.legalColumns + 2) * this.discretisation;
    	int height = (this.height + 2) * this.discretisation;
    	this.massMap = new int[width][height];
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			this.massMap[x][y] = 0;
    		}
    	}
    	
    	this.horizontalPotential = new double[width];
    	this.verticalPotential = new double[height];

    	this.iteration = 0;
    	this.overlap = 0;

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
    private void initializeMassMap(){
    	if(timing) this.timer.start("Initialize Mass Map");

    	for(LegalizerBlock block:this.blocks){
        		
        	int x = block.horizontal.grid;
        	int y = block.vertical.grid;
        		
            for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < this.discretisation * block.height; l++){
            		if(++this.massMap[x + k][y + l] > 1) this.overlap++;
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
    private void legalPotential(double progress, double maxPotential){
    	if(timing) this.timer.start("Legal Potential");
    	
    	maxPotential = maxPotential * progress;

    	//Horizontal
    	for(int x = 1; x < this.legalColumns + 1; x++){
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
		
		this.addVisual();
		
		if(timing) this.timer.time("Apply Pushing Forces");
    }

    private void fpgaPushForces(){
    	if(timing) this.timer.start("Gravity Push Forces");

    	int horizontal = this.discretisation;
    	int halfHorizontal = horizontal / 2;

    	for(LegalizerBlock block:this.blocks){
	    	int x = block.horizontal.grid;
	    	int y = block.vertical.grid;
	    	
	    	int vertical = this.discretisation * block.height;
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

	    	block.horizontal.setForce((southWest + nordWest - southEast - nordEast + horizontalPotential) / mass);
	    	block.vertical.setForce((southWest - nordWest + southEast - nordEast + verticalPotential) / mass);
    	}    	
    	if(timing) this.timer.time("Gravity Push Forces");
    }
    private void solve(){
    	if(timing) this.timer.start("Solve");

    	for(LegalizerBlock block:this.blocks){

    		int origX = block.horizontal.grid;
    		int origY = block.vertical.grid;

    		//Horizontal
    		block.horizontal.solve(this.stepSize, this.speedAveraging);

    		if(block.horizontal.coordinate > this.legalColumns) block.setHorizontal(this.legalColumns);
    		if(block.horizontal.coordinate < 1) block.setHorizontal(1);

    		this.horizontalMassMapUpdate(block.vertical.grid, origX, block.horizontal.grid, block.height);

    		//Vertical
    		block.vertical.solve(this.stepSize, this.speedAveraging);

    		if(block.vertical.coordinate + block.height - 1 > this.height) block.setVertical(this.height - block.height + 1);
    		if(block.vertical.coordinate < 1) block.setVertical(1);
    		
    		this.verticalMassMapUpdate(block.horizontal.grid, origY, block.vertical.grid, block.height);
    	}

    	if(timing) this.timer.time("Solve");

    	if(timing) this.timer.start("Test incremental functionality");

    	if(debug){
    		this.testMassMap();
    		this.testOverlap();
    	}

    	if(timing) this.timer.time("Test incremental functionality");
    }
    private void horizontalMassMapUpdate(int y, int origX, int newX, int height){
    	if(origX < newX){//Move right
    		int horizontalDistance = newX - origX;
    		for(int l = 0; l < this.discretisation * height; l++){
    			for(int k = 0; k < horizontalDistance; k++){
            		if(--this.massMap[origX + k][y + l] >= 1) this.overlap--;
            		if(++this.massMap[newX + this.discretisation - 1 - k][y + l] > 1) this.overlap++;
            	}
            }
    	}else if(origX > newX){//Move left
    		int horizontalDistance = origX - newX;
    		for(int l = 0; l < this.discretisation * height; l++){
    			for(int k = 0; k < horizontalDistance; k++){
            		if(--this.massMap[origX + this.discretisation - 1 - k][y + l] >= 1) this.overlap--;
            		if(++this.massMap[newX + k][y + l] > 1) this.overlap++;
            	}
            }
    	}
    }
    private void verticalMassMapUpdate(int x, int origY, int newY, int height){
    	if(origY < newY){//Move up
    		int verticalDistance = newY - origY;
    		for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < verticalDistance; l++){
            		if(--this.massMap[x + k][origY + l] >= 1) this.overlap--;
            		if(++this.massMap[x + k][newY + height * this.discretisation - 1 - l] > 1) this.overlap++;
            	}
            }
    	}else if(origY > newY){//Move down
    		int verticalDistance = origY - newY;
    		for(int k = 0; k < this.discretisation; k++){
            	for(int l = 0; l < verticalDistance; l++){
            		if(--this.massMap[x + k][origY + height * this.discretisation - 1 - l] >= 1) this.overlap--;
            		if(++this.massMap[x + k][newY + l] > 1) this.overlap++;
            	}
            }
    	}
    }
    private void testMassMap(){
    	int[][] testMassMap = new int[this.massMap.length][this.massMap[0].length];
    			
    	for(LegalizerBlock block:this.blocks){
        	int i = block.horizontal.grid;
        	int j = block.vertical.grid;
        		
        	for(int k = 0; k < this.discretisation; k++){
        		for(int l = 0; l < this.discretisation * block.height; l++){
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
    private void testOverlap(){
    	int testOverlap = 0;
		
        for(int x = 0; x < this.massMap.length; x++){
        	for(int y = 0; y < this.massMap[0].length; y++){
        		testOverlap += Math.max(0, this.massMap[x][y] - 1);
        	}
        }
    	if(testOverlap != this.overlap){
    		System.out.println("Something is wrong in the incremental overlap update!");
    		System.out.println("\tTest overlap: " + testOverlap);
    		System.out.println("\tOverlap: " + this.overlap);
    	}
    }
    
    //Set legal coordinates of all blocks
    private void updateLegal(){
    	if(timing) this.timer.start("Update Legal");
    	
    	for(LegalizerBlock block:this.blocks){
    		int x = (int) Math.round(block.horizontal.coordinate);
    		int y = (int) Math.round(block.vertical.coordinate);
    		
    		block.setHorizontal(x);
    		block.setVertical(y);
    		
    		this.legalX[block.index] = x;
    		this.legalY[block.index] = y;
    	}
    	
    	if(timing) this.timer.time("Update Legal");
    }
    private void insertIllegalColumns(){
    	int[] newColumn = new int[this.legalColumns + 2];
    	for(int i = 0; i < newColumn.length; i++){
    		newColumn[i] = 0;
    	}
    	int illegalIncrease = 0;

    	newColumn[0] = 0;
    	for(int column = 1; column < this.width + 1; column++){
    		if(this.circuit.getColumnType(column).equals(this.blockType)){
    			newColumn[column - illegalIncrease] = column;
    		}else{
    			illegalIncrease += 1;
    		}
    	}
    	newColumn[this.width + 1 - illegalIncrease] = this.width + 1;
    	
    	for(LegalizerBlock block:this.blocks){
    		block.setHorizontal(newColumn[(int)Math.round(block.horizontal.coordinate)]);
    	}
    }
    
    // Visual
    private void addVisual(String name){
    	boolean doVisual = false;
    	if(interVisual) doVisual = true;
    	if(visual) doVisual = true;
    	if(doVisual){
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
    
    //LegalizerBlock
    class LegalizerBlock {
    	final int index;

    	final Dimension horizontal;
    	final Dimension vertical;

    	final int height;

    	LegalizerBlock(int index, double x, double y, int height, int discretisation){
    		this.index = index;

    		this.horizontal = new Dimension(x, discretisation);
    		this.vertical = new Dimension(y, discretisation);

    		this.height = height;
    	}
    	
    	void scaleHorizontal(double scalingFactor){
    		this.horizontal.scale(scalingFactor);
    	}
    	
    	void setHorizontal(double horizontal){
    		this.horizontal.setCoordinate(horizontal);
    	}
    	void setVertical(double vertical){
    		this.vertical.setCoordinate(vertical);
    	}
    }
    
    class Dimension {
    	double coordinate;
    	double speed;
    	double force;

    	final int discretisation;
    	int grid;

    	Dimension(double coordinate, int discretisation){
    		this.coordinate = coordinate;
    		this.speed = 0.0;
    		this.force = 0.0;
    		
    		this.discretisation = discretisation;
    		this.updateGridValue();
    	}
    	void updateGridValue(){
    		this.grid = (int)Math.round(this.coordinate * this.discretisation);
    	}
    	
    	void setForce(double force){
    		this.force = force;
    	}
    	void setCoordinate(double coordinate){
    		this.coordinate = coordinate;
    		this.updateGridValue();
    	}
    	void scale(double scalingFactor){
    		this.coordinate *= scalingFactor;
    		this.updateGridValue();
    	}
    	
    	void solve(double stepSize, double speedAveraging){
    		if(this.force != 0.0){
    			double newSpeed = stepSize * this.force;

            	this.speed = speedAveraging * this.speed + (1 - speedAveraging) * newSpeed;
            	this.coordinate += this.speed;

            	this.updateGridValue();
    		}
    	}
    }
}