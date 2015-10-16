package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import placers.Placer;

import architecture.BlockType;
import architecture.BlockType.BlockCategory;
import architecture.circuit.Circuit;
import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.GlobalBlock;
import architecture.circuit.pin.AbstractPin;
import architecture.circuit.pin.GlobalPin;


abstract class AnalyticalPlacer extends Placer {
	
	private int startingStage;
	private double[] maxUtilizationSequence;
	private double startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal;
	private double gradientMultiplier;
	
	protected Map<GlobalBlock, Integer> blockIndexes = new HashMap<GlobalBlock, Integer>(); // Maps a block (CLB or hardblock) to its integer index
	private List<BlockType> blockTypes = new ArrayList<BlockType>();
	private List<Integer> blockTypeIndexStarts = new ArrayList<Integer>();
	private int numBlocks;
	
	protected double[] linearX, linearY;
	protected double[] pullX, pullY;
	
	protected CostCalculator costCalculator;
	protected Legalizer legalizer;
	
	
	static {
		//startingStage = 0 ==> start with initial solves (no anchors)
		//startingStage = 1 ==> start from existing placement that is incorporated in the packedCircuit passed with the constructor
		defaultOptions.put("starting_stage", "0");
		
		//initialize maxUtilizationSequence used by the legalizer
		defaultOptions.put("max_utilization_sequence", "1");
		
		//The first anchorWeight factor that will be used in the main solve loop
		defaultOptions.put("starting_anchor_weight", "0.1");
		
		//The amount with which the anchorWeight factor will be increased each iteration (multiplicative)
		defaultOptions.put("anchor_weight_increase", "1.02");
		
		//The ratio of linear solutions cost to legal solution cost at which we stop the algorithm
		defaultOptions.put("stop_ratio_linear_legal", "0.85");
		
		// The speed at which the gradient solver moves to the optimal position
		defaultOptions.put("gradient_multiplier", "0.1");
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
		Arrays.fill(this.linearX, this.circuit.getWidth() / 2);
		Arrays.fill(this.linearY, this.circuit.getHeight() / 2);
		
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
				//this.linearX[blockIndex] = block.getX();
				//this.linearY[blockIndex] = block.getY();
				
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
		
		// Gradient solver
		this.gradientMultiplier = this.parseDoubleOption("gradient_multiplier");
	}
	
	
	
	protected abstract CostCalculator createCostCalculator();
	protected abstract void initializePlacement();
	protected abstract void processNets(BlockType blockType, int startIndex, boolean firstSolve);
	
	
	public void place() {
		if(this.startingStage == 0) {	
			//Initial linear solves, should normally be done 5-7 times
			int blockTypeIndex = -1;
			for(int i = 0; i < 10000; i++) {
				this.solveLinear(true, blockTypeIndex, 0.0);
				System.out.println(this.costCalculator.calculate(this.linearX, this.linearY));
			}
		
		} else {
			// Initial legalization
			this.legalizer.initializeArrays();
		}
		
		// Do the actual placing
		int blockTypeIndex = -1, iteration = 0;
		double pseudoWeightFactor = this.startingAnchorWeight;
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
			
			
			//blockTypeIndex = (blockTypeIndex + 2) % (this.blockTypes.size() + 1) - 1;
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
		
		this.pullX = new double[numBlocks];
		this.pullY = new double[numBlocks];
		
		
		//Add pseudo connections
		if(!firstSolve) {
			// Process pseudonets
			int[] anchorPointsX = this.legalizer.getAnchorPointsX();
			int[] anchorPointsY = this.legalizer.getAnchorPointsY();
			
			for(int index = startIndex; index < endIndex; index++) {
				double deltaX = anchorPointsX[index] - this.linearX[index];
				double deltaY = anchorPointsY[index] - this.linearY[index];
				
				int relativeIndex = index - startIndex;
				this.pullX[relativeIndex] = pseudoWeightFactor;// * deltaX;
				this.pullY[relativeIndex] = pseudoWeightFactor;// * deltaY;
			}
		}
		
		
		this.processNets(blockType, startIndex, firstSolve);
		
		
		double[] xSolution = new double[numBlocks];
		double[] ySolution = new double[numBlocks];
		
		for(int index = startIndex; index < endIndex; index++) {
			int relativeIndex = index - startIndex;
			//xSolution[relativeIndex] = Math.max(Math.min(this.linearX[index] + this.gradientMultiplier * this.pullX[relativeIndex], this.circuit.getWidth() - 2), 1);
			//ySolution[relativeIndex] = Math.max(Math.min(this.linearY[index] + this.gradientMultiplier * this.pullY[relativeIndex], this.circuit.getWidth() - 2), 1);
			xSolution[relativeIndex] = this.linearX[index] + this.gradientMultiplier * this.pullX[relativeIndex];
			ySolution[relativeIndex] = this.linearY[index] + this.gradientMultiplier * this.pullY[relativeIndex];
		}
		
		
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
		
		Set<GlobalBlock> netBlocks = new HashSet<GlobalBlock>();
		
		List<AbstractPin> pins = new ArrayList<AbstractPin>();
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
			netBlocks.add(block);
			
			
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
		
		
		double weightMultiplier = 1.0 / (numPins - 1) * AnalyticalPlacer.getWeight(netBlocks.size());
		boolean minXFixed = pinFixed[minXIndex], maxXFixed = pinFixed[maxXIndex],
				minYFixed = pinFixed[minYIndex], maxYFixed = pinFixed[maxYIndex];
		
		for(int i = 0; i < numPins; i++) {
			
			// Add connection to min X block
			if(i != minXIndex) {
				this.addConnection(
						minX, minXFixed, pinBlockIndex[minXIndex] - startIndex,
						pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.pullX);
				
				// Add connection to max X block
				if(i != maxXIndex) {
					this.addConnection(
							maxX, maxXFixed, pinBlockIndex[maxXIndex] - startIndex,
							pinX[i], pinFixed[i], pinBlockIndex[i] - startIndex,
							weightMultiplier, this.pullX);
				}
			}
			
			// Add connection to min Y block
			if(i != minYIndex) {
				this.addConnection(
						minY, minYFixed, pinBlockIndex[minYIndex] - startIndex,
						pinY[i], pinFixed[i], pinBlockIndex[i] - startIndex,
						weightMultiplier, this.pullY);
				
				// Add connection to max Y block
				if(i != maxYIndex) {
					this.addConnection(
							maxY, maxYFixed, pinBlockIndex[maxYIndex] - startIndex,
							pinY[i], pinFixed[i], pinBlockIndex[i] - startIndex,
							weightMultiplier, this.pullY);
				}
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
			double weightMultiplier, double[] pull) {
		
		int sign = coor2 > coor1 ? 1 : -1;
		if(!fixed1) {
			pull[index1] += weightMultiplier * sign;// * (coor2 - coor1);
		}
		
		if(!fixed2) {
			pull[index2] -= weightMultiplier * sign;// * (coor1 - coor2);
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
