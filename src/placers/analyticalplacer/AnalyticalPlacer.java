package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import architecture.BlockType;
import architecture.BlockType.BlockCategory;
import architecture.circuit.Circuit;
import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.GlobalBlock;
import architecture.circuit.pin.AbstractPin;
import architecture.circuit.pin.GlobalPin;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.Placer;


abstract class AnalyticalPlacer extends Placer {
	
	private int startingStage;
	private double[] maxUtilizationSequence;
	private double startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal;
	
	protected Map<GlobalBlock, Integer> blockIndexes = new HashMap<GlobalBlock, Integer>(); // Maps a block (CLB or hardblock) to its integer index
	private List<BlockType> blockTypes = new ArrayList<BlockType>();
	private List<Integer> blockTypeIndexStarts = new ArrayList<Integer>();
	private int numBlocks;
	
	protected Crs xMatrix, yMatrix;
	protected double[] xVector, yVector;
	protected double[] linearX, linearY;
	
	protected CostCalculator costCalculator;
	protected Legalizer legalizer;
	
	
	static {
		//startingStage = 0 ==> start with initial solves (no anchors)
		//startingStage = 1 ==> start from existing placement that is incorporated in the packedCircuit passed with the constructor
		defaultOptions.put("starting_stage", "0");
		
		//initialize maxUtilizationSequence used by the legalizer
		defaultOptions.put("max_utilization_sequence", "1");
		
		//The first anchorWeight factor that will be used in the main solve loop
		defaultOptions.put("starting_anchor_weight", "0.3");
		
		//The amount with which the anchorWeight factor will be increased each iteration (multiplicative)
		defaultOptions.put("anchor_weight_increase", "1.5");
		
		//The ratio of linear solutions cost to legal solution cost at which we stop the algorithm
		defaultOptions.put("stop_ratio_linear_legal", "0.85");
	}
	
	public AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		// Parse options
		this.parsePrivateOptions();
		
		// Get number of blocks
		this.numBlocks = 0;
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			if(blockType.getCategory() != BlockCategory.IO) {
				this.numBlocks += this.circuit.getBlocks(blockType).size();
			}
		}
		
		
		// Initialize blocks and positions
		this.linearX = new double[this.numBlocks];
		this.linearY = new double[this.numBlocks];
		
		int blockIndex = 0;
		this.blockTypeIndexStarts.add(blockIndex);
		
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			if(blockType.getCategory() == BlockCategory.IO) {
				continue;
			}
			
			List<AbstractBlock> blocksOfType = this.circuit.getBlocks(blockType);
			for(AbstractBlock abstractBlock : blocksOfType) {
				GlobalBlock block = (GlobalBlock) abstractBlock;
				
				// Set the linear position to be equal to the current legal position
				this.linearX[blockIndex] = block.getX();
				this.linearY[blockIndex] = block.getY();
				
				this.blockIndexes.put(block, blockIndex);
				blockIndex++;
			}
			
			this.blockTypes.add(blockType);
			this.blockTypeIndexStarts.add(blockIndex);
		}
		
		this.costCalculator = this.createCostCalculator();
		this.legalizer = new Legalizer(
				circuit, this.costCalculator,
				this.blockIndexes, this.blockTypes, this.blockTypeIndexStarts,
				this.linearX, this.linearY);
	}
	
	private void parsePrivateOptions() {
		
		// Starting stage (0 or 1)
		this.startingStage = Integer.parseInt(this.options.get("starting_stage"));
		this.startingStage = Math.min(1, Math.max(0, this.startingStage)); //startingStage can only be 0 or 1
		
		// Max utilization sequence
		String maxUtilizationSequenceString = this.options.get("max_utilization_sequence");
		String[] maxUtilizationSequenceStrings = maxUtilizationSequenceString.split(",");
		this.maxUtilizationSequence = new double[maxUtilizationSequenceStrings.length];
		for(int i = 0; i < maxUtilizationSequenceStrings.length; i++) {
			this.maxUtilizationSequence[i] = Double.parseDouble(maxUtilizationSequenceStrings[i]);
		}
		
		// Anchor weights and stop ratio
		this.startingAnchorWeight = this.parseDoubleOption("starting_anchor_weight");
		this.anchorWeightIncrease = this.parseDoubleOption("anchor_weight_increase");
		this.stopRatioLinearLegal = this.parseDoubleOption("stop_ratio_linear_legal");
	}
	
	
	
	protected abstract CostCalculator createCostCalculator();
	protected abstract void initializePlacement();
	protected abstract void processNets(BlockType blockType, int startIndex, boolean firstSolve);
	
	
	public void place() {
		double pseudoWeightFactor;
		int iteration;
		
		// Initialize the data structures
		
		if(this.startingStage == 0) {
			
			//Initial linear solves, should normally be done 5-7 times
			int blockTypeIndex = -1;
			for(int i = 0; i < 7; i++) {
				this.solveLinear(true, blockTypeIndex, 0.0);
			}
			
			//Initial legalization
			this.legalizer.legalize(blockTypeIndex, this.maxUtilizationSequence[0]);
			
			pseudoWeightFactor = this.startingAnchorWeight;
			iteration = 1;
		
		
		} else {
			// Initial legalization
			this.legalizer.initializeArrays();
			pseudoWeightFactor = this.startingAnchorWeight;
			iteration = 0;
		}
		
		// Do the actual placing
		int blockTypeIndex = -1;
		double linearCost, legalCost;
		
		this.initializePlacement();
		
		do {
			String blockType = blockTypeIndex == -1 ? "all" : this.blockTypes.get(blockTypeIndex).getName();
			System.out.format("Iteration %d: pseudoWeightFactor = %f, blockType = %s",
					iteration, pseudoWeightFactor, blockType);
			
			// Solve linear
			this.solveLinear(false, blockTypeIndex, pseudoWeightFactor);
			
			// Legalize
			int sequenceIndex = Math.min(iteration, this.maxUtilizationSequence.length - 1);
			double maxUtilizationLegalizer = this.maxUtilizationSequence[sequenceIndex];
			
			this.legalizer.legalize(blockTypeIndex, maxUtilizationLegalizer);
			
			
			// Get the costs and print them
			linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
			legalCost = this.legalizer.getBestCost();
			
			System.out.format(", linear cost = %f, legal cost = %f\n", linearCost, legalCost);
			
			
			blockTypeIndex = (blockTypeIndex + 2) % (this.blockTypes.size() + 1) - 1;
			if(blockTypeIndex < 0) {
				pseudoWeightFactor *= this.anchorWeightIncrease;
				iteration++;
			}
			
		} while(linearCost / legalCost < this.stopRatioLinearLegal);
		
		
		this.legalizer.updateCircuit();
	}
	
	
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 */
	private void solveLinear(boolean firstSolve, int blockTypeIndex, double pseudoWeightFactor) {
		
		BlockType blockType = null;
		int startIndex, endIndex;
		
		// Solve all blocks
		if(blockTypeIndex == -1) {
			startIndex = 0;
			endIndex = this.numBlocks;
		
		// Solve blocks of one type
		} else {
			blockType = this.blockTypes.get(blockTypeIndex);
			startIndex = this.blockTypeIndexStarts.get(blockTypeIndex);
			endIndex = this.blockTypeIndexStarts.get(blockTypeIndex + 1);
		}
		
		int numBlocks = endIndex - startIndex;
		this.xMatrix = new Crs(numBlocks);
		this.yMatrix = new Crs(numBlocks);
		this.xVector = new double[numBlocks];
		this.yVector = new double[numBlocks];
		
		
		//Add pseudo connections
		if(!firstSolve) {
			// Process pseudonets
			int[] anchorPointsX = this.legalizer.getAnchorPointsX();
			int[] anchorPointsY = this.legalizer.getAnchorPointsY();
			
			for(int index = startIndex; index < endIndex; index++) {
				double deltaX = Math.max(Math.abs(anchorPointsX[index] - this.linearX[index]), 0.005);
				double deltaY = Math.max(Math.abs(anchorPointsY[index] - this.linearY[index]), 0.005);
				
				double pseudoWeightX = 2 * pseudoWeightFactor / deltaX;
				double pseudoWeightY = 2 * pseudoWeightFactor / deltaY;
				
				int relativeIndex = index - startIndex;
				this.xMatrix.setElement(relativeIndex, relativeIndex,
						this.xMatrix.getElement(relativeIndex, relativeIndex) + pseudoWeightX);
				this.yMatrix.setElement(relativeIndex, relativeIndex,
						this.yMatrix.getElement(relativeIndex, relativeIndex) + pseudoWeightY);
				
				this.xVector[relativeIndex] += pseudoWeightX * anchorPointsX[relativeIndex];
				this.yVector[relativeIndex] += pseudoWeightY * anchorPointsY[relativeIndex];
			}
		}
		
		
		this.processNets(blockType, startIndex, firstSolve);
		
		
		double epsilon = 0.0001;
		
		// Solve x problem
		CGSolver xSolver = new CGSolver(this.xMatrix, this.xVector);
		double[] xSolution = xSolver.solve(epsilon);
		
		// Solve y problem
		CGSolver ySolver = new CGSolver(this.yMatrix, this.yVector);
		double[] ySolution = ySolver.solve(epsilon);
		
		
		// Save results
		System.arraycopy(xSolution, 0, this.linearX, startIndex, numBlocks);
		System.arraycopy(ySolution, 0, this.linearY, startIndex, numBlocks);
	}
	
	
	protected void processNetsB2B(BlockType blockType, int startIndex) {
		for(GlobalBlock sourceBlock : this.circuit.getGlobalBlocks()) {
			for(AbstractPin sourcePin : sourceBlock.getOutputPins()) {
				this.processNetB2B(blockType, startIndex, (GlobalPin) sourcePin); 
			}
		}
	}
	
	private void processNetB2B(BlockType blockType, int startIndex, GlobalPin sourcePin) {
		List<AbstractPin> pins = new ArrayList<AbstractPin>();
		// The source pin *must* be added first!
		pins.add(sourcePin);
		pins.addAll(sourcePin.getSinks());
		
		int numPins = pins.size();
		if(numPins < 2) {
			return;
		}
		
		
		double[] pinX = new double[numPins];
		double[] pinY = new double[numPins];
		boolean[] pinFixed = new boolean[numPins];
		int[] pinBlockIndex = new int[numPins];
		
		int minXIndex = -1, maxXIndex = -1,
			minYIndex = -1, maxYIndex = -1;
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
			   minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i < numPins; i++) {
			AbstractPin pin = pins.get(i); 
			GlobalBlock block = ((GlobalPin) pin).getOwner();
			
			
			double x, y;
			int blockIndex;
			
			boolean fixed = isFixed(block, blockType);
			if(fixed) {
				if(block.getCategory() == BlockCategory.IO) {
					blockIndex = -1;
					x = block.getX();
					y = block.getY();
				
				} else {
					blockIndex = this.blockIndexes.get(block);
					x = this.legalizer.getAnchorPointsX()[blockIndex];
					y = this.legalizer.getAnchorPointsY()[blockIndex];
				}
			
			} else {
				blockIndex = this.blockIndexes.get(block);
				x = this.linearX[blockIndex];
				y = this.linearY[blockIndex];
			}
			
			
			pinX[i] = x;
			pinY[i] = y;
			pinFixed[i] = fixed;
			pinBlockIndex[i] = blockIndex;
			
			
			if(x < minX) {
				minX = x;
				minXIndex = i;
			}
			if(x > maxX) {
				maxX = x;
				maxXIndex = i;
			}
			if(y < minY) {
				minY = y;
				minYIndex = i;
			}
			if(y > maxY) {
				maxY = y;
				maxYIndex = i;
			}
		}
		
		
		double weightMultiplier = 2.0 / (numPins + 1) * getWeight(numPins);
		boolean minXFixed = pinFixed[minXIndex], maxXFixed = pinFixed[maxXIndex],
				minYFixed = pinFixed[minYIndex], maxYFixed = pinFixed[maxYIndex];
		
		for(int i = 0; i < numPins; i++) {
			
			// Add connections from bound to bound model
			if(i != minXIndex) {
				this.addConnection(
						minX, minXFixed, pinBlockIndex[minXIndex] - startIndex,
						pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.xMatrix, this.xVector);
			}
			if(i != maxXIndex) {
				this.addConnection(
						maxX, maxXFixed, pinBlockIndex[maxXIndex] - startIndex,
						pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.xMatrix, this.xVector);
			}
			
			if(i != minYIndex) {
				this.addConnection(
						minY, minYFixed, pinBlockIndex[minYIndex] - startIndex,
						pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.yMatrix, this.yVector);
			}
			if(i != maxYIndex) {
				this.addConnection(
						maxY, maxYFixed, pinBlockIndex[maxYIndex] - startIndex,
						pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.yMatrix, this.yVector);
			}
			
			// Add connections from source-sink model
			if(i > 0) {
				this.addConnection(
						pinX[0], pinFixed[0], pinBlockIndex[0] - startIndex,
						pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.xMatrix, this.xVector);
				
				this.addConnection(
						pinY[0], pinFixed[0], pinBlockIndex[0] - startIndex,
						pinY[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.yMatrix, this.yVector);
			}
		}
	}
	
	
	
	protected boolean isFixed(GlobalBlock block, BlockType blockType) {

		// IOs are always considered fixed
		if(block.getCategory() == BlockCategory.IO) {
			return true;
		
		// We are solving all block types => no types except input are fixed 
		} else if(blockType == null)  {
			return false;
		
		// The pin belongs to the block type that is being solved
		} else if(blockType.equals(block.getType())) {
			return false;
		
		// The pin belongs to a different block type
		} else {
			return true;
		}
	}
	
	
	protected void addConnection(
			double coor1, boolean fixed1, int index1,
			double coor2, boolean fixed2, int index2,
			double weightMultiplier, Crs matrix, double[] vector) {
		
		
		
		if(fixed1 && fixed2) {
			return;
		}
		
		double delta = Math.max(coor1 - coor2, 0.005);
		double weight = weightMultiplier / delta;
		
		if(fixed1) {
			matrix.setElement(index2, index2, matrix.getElement(index2, index2) + weight);
			vector[index2] += weight * coor1;
		
		} else if(fixed2) {
			matrix.setElement(index1, index1, matrix.getElement(index1, index1) + weight);
			vector[index1] += weight * coor2;
		
		} else {
			matrix.setElement(index1, index1, matrix.getElement(index1, index1) + weight);
			matrix.setElement(index1, index2, matrix.getElement(index1, index2) - weight);
			matrix.setElement(index2, index1, matrix.getElement(index2, index1) - weight);
			matrix.setElement(index2, index2, matrix.getElement(index2, index2) + weight);
		}
	}
	
	
	
	static double getWeight(int size) {
		switch (size) {
			case 1:  return 1;
			case 2:  return 1;
			case 3:  return 1;
			case 4:  return 1.0828;
			case 5:  return 1.1536;
			case 6:  return 1.2206;
			case 7:  return 1.2823;
			case 8:  return 1.3385;
			case 9:  return 1.3991;
			case 10: return 1.4493;
			case 11:
			case 12:
			case 13:
			case 14:
			case 15: return (size-10) * (1.6899-1.4493) / 5 + 1.4493;				
			case 16:
			case 17:
			case 18:
			case 19:
			case 20: return (size-15) * (1.8924-1.6899) / 5 + 1.6899;
			case 21:
			case 22:
			case 23:
			case 24:
			case 25: return (size-20) * (2.0743-1.8924) / 5 + 1.8924;		
			case 26:
			case 27:
			case 28:
			case 29:
			case 30: return (size-25) * (2.2334-2.0743) / 5 + 2.0743;		
			case 31:
			case 32:
			case 33:
			case 34:
			case 35: return (size-30) * (2.3895-2.2334) / 5 + 2.2334;		
			case 36:
			case 37:
			case 38:
			case 39:
			case 40: return (size-35) * (2.5356-2.3895) / 5 + 2.3895;		
			case 41:
			case 42:
			case 43:
			case 44:
			case 45: return (size-40) * (2.6625-2.5356) / 5 + 2.5356;		
			case 46:
			case 47:
			case 48:
			case 49:
			case 50: return (size-45) * (2.7933-2.6625) / 5 + 2.6625;
			default: return (size-50) * 0.02616 + 2.7933;
		}
	}
}
