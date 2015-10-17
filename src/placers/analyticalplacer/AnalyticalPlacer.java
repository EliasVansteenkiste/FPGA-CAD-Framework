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


abstract class AnalyticalPlacer extends Placer {
	
	private int startingStage;
	private double[] maxUtilizationSequence;
	private double startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal;
	private double gradientSpeed, gradientMultiplier;
	
	private List<Integer[]> nets;
	private int numBlocks, numIOBlocks, numMovableBlocks;
	
	protected double[] linearX, linearY, tmpX, tmpY;
	
	protected CostCalculator costCalculator;
	protected Legalizer legalizer;
	
	
	static {
		//startingStage = 0 ==> start with initial solves (no anchors)
		//startingStage = 1 ==> start from existing placement that is incorporated in the packedCircuit passed with the constructor
		defaultOptions.put("starting_stage", "0");
		
		//initialize maxUtilizationSequence used by the legalizer
		defaultOptions.put("max_utilization_sequence", "1");
		
		//The first anchorWeight factor that will be used in the main solve loop
		defaultOptions.put("starting_anchor_weight", "0.2");
		
		//The amount with which the anchorWeight factor will be increased each iteration (multiplicative)
		defaultOptions.put("anchor_weight_increase", "1.1");
		
		//The ratio of linear solutions cost to legal solution cost at which we stop the algorithm
		defaultOptions.put("stop_ratio_linear_legal", "0.95");
		
		// The speed at which the gradient solver moves to the optimal position
		defaultOptions.put("initial_gradient_speed", "0.63");
		defaultOptions.put("gradient_multiplier", "0.94");
	}
	
	public AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		// Parse options
		this.parsePrivateOptions();
		
		// Get number of blocks
		this.numBlocks = 0;
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			this.numBlocks += this.circuit.getBlocks(blockType).size();
		}
		
		
		// Initialize block positions at the center of the design 
		this.linearX = new double[this.numBlocks];
		this.linearY = new double[this.numBlocks];
		Arrays.fill(this.linearX, this.circuit.getWidth() / 2 - 1);
		Arrays.fill(this.linearY, this.circuit.getHeight() / 2 - 1);
		
		// Initialize temporary positions. The first numIOBlocks elements
		// of these arrays will never be used, but it's more convenient to
		// make them the same length as linearX and linearY
		this.tmpX = new double[this.numBlocks];
		this.tmpY = new double[this.numBlocks];
		
		
		// Make a list of all block types, with IO blocks first
		BlockType ioBlockType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
		List<BlockType> blockTypes = new ArrayList<BlockType>();
		blockTypes.add(ioBlockType);
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			if(!blockType.equals(ioBlockType)) {
				blockTypes.add(blockType);
			}
		}
		
		
		
		// Add all blocks
		Map<GlobalBlock, Integer> blockIndexes = new HashMap<GlobalBlock, Integer>();
		List<Integer> blockTypeIndexStarts = new ArrayList<Integer>();
		int blockIndex = 0;
		blockTypeIndexStarts.add(0);
		
		for(BlockType blockType : blockTypes) {
			boolean isFixed = blockType.equals(ioBlockType);
			
			for(AbstractBlock abstractBlock : this.circuit.getBlocks(blockType)) {
				GlobalBlock block = (GlobalBlock) abstractBlock;
				
				if(isFixed) {
					this.linearX[blockIndex] = block.getX();
					this.linearY[blockIndex] = block.getY();
				}
				
				blockIndexes.put(block, blockIndex);
				blockIndex++;
			}
			
			blockTypeIndexStarts.add(blockIndex);
		}
		
		this.numIOBlocks = blockTypeIndexStarts.get(1);
		this.numMovableBlocks = this.numBlocks - this.numIOBlocks;
		
		
		
		// Add all nets
		// Loop through all net sources
		this.nets = new ArrayList<Integer[]>();
		for(Map.Entry<GlobalBlock, Integer> sourceBlockEntry : blockIndexes.entrySet()) {
			GlobalBlock sourceBlock = sourceBlockEntry.getKey();
			
			for(AbstractPin sourcePin : sourceBlock.getOutputPins()) {
				
				// Build a set of all blocks connected to the net
				List<AbstractPin> netPins = new ArrayList<AbstractPin>();
				netPins.add(sourcePin);
				netPins.addAll(sourcePin.getSinks());
				
				Set<GlobalBlock> netBlocks = new HashSet<GlobalBlock>();
				for(AbstractPin pin : netPins) {
					netBlocks.add((GlobalBlock) pin.getOwner());
				}
				
				// Make a list of the block indexes
				int numNetBlocks = netBlocks.size();
				Integer[] netBlockIndexes = new Integer[numNetBlocks];
				int i = 0;
				for(GlobalBlock netBlock : netBlocks) {
					netBlockIndexes[i] = blockIndexes.get(netBlock);
					i++;
				}
				
				// Add this "net" to the list of nets
				this.nets.add(netBlockIndexes);
			}
		}
		
		
		this.costCalculator = this.createCostCalculator(circuit, blockIndexes);
		this.legalizer = new Legalizer(
				circuit, this.costCalculator,
				blockIndexes,
				blockTypes, blockTypeIndexStarts,
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
		this.gradientSpeed = this.parseDoubleOption("initial_gradient_speed");
		this.gradientMultiplier = this.parseDoubleOption("gradient_multiplier");
	}
	
	
	
	protected abstract CostCalculator createCostCalculator(Circuit circuit, Map<GlobalBlock, Integer> blockIndexes);
	protected abstract void initializePlacement();
	protected abstract void processNets(boolean firstSolve);
	
	
	public void place() {
		//Initial linear solves, should normally be done 5-7 times
		/*for(int i = 0; i < 1000; i++) {
			this.solveLinear(true, 0);
			System.out.println(this.costCalculator.calculate(this.linearX, this.linearY));
		}*/
		
        this.gradientSpeed /= 10;
        for(int i = 0; i < 20; i++) {
            this.solveLinear(true, 0);
            System.out.println(this.costCalculator.calculate(this.linearX, this.linearY));
        }
        this.gradientSpeed *= 10;
        
		// Do the actual placing
		int iteration = 0;
		double pseudoWeightFactor = this.startingAnchorWeight;
		double linearCost = 0, legalCost;
		
		this.initializePlacement();
		
		do {
			System.out.format("Iteration %d: pseudoWeightFactor = %f, gradientSpeed = %f",
					iteration, pseudoWeightFactor, this.gradientSpeed);
			
			// Solve linear
			for(int i = 0; i < 10; i++) {
				this.solveLinear(false, pseudoWeightFactor);
			}
			
			// Legalize
			int sequenceIndex = Math.min(iteration, this.maxUtilizationSequence.length - 1);
			double maxUtilizationLegalizer = this.maxUtilizationSequence[sequenceIndex];
			
			this.legalizer.legalize(maxUtilizationLegalizer);
			
			
			// Get the costs and print them
			linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
			legalCost = this.legalizer.getBestCost();
			//System.out.format("Legal gost = %f\n", legalCost);
			System.out.format(", linear cost = %f, legal cost = %f\n", linearCost, legalCost);
			
			
			//blockTypeIndex = (blockTypeIndex + 2) % (this.blockTypes.size() + 1) - 1;
			pseudoWeightFactor *= this.anchorWeightIncrease;
			this.gradientSpeed *= this.gradientMultiplier;
			iteration++;
			
		} while(linearCost / legalCost < this.stopRatioLinearLegal);
		
		
		this.legalizer.updateCircuit();
	}
	
	
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 */
	private void solveLinear(boolean firstSolve, double pseudoWeightFactor) {
		
		//System.arraycopy(this.linearX, this.numIOBlocks, this.tmpX, this.numIOBlocks, this.numMovableBlocks);
		//System.arraycopy(this.linearX, this.numIOBlocks, this.tmpY, this.numIOBlocks, this.numMovableBlocks);
		Arrays.fill(this.tmpX, 0);
		Arrays.fill(this.tmpY, 0);
		
		//Add pseudo connections
		if(!firstSolve) {
			this.addPseudoConnections(pseudoWeightFactor);
		}
		
		
		this.processNets(firstSolve);
		
		
		// Save results
		//System.arraycopy(this.tmpX, this.numIOBlocks, this.linearX, this.numIOBlocks, this.numMovableBlocks);
		//System.arraycopy(this.tmpX, this.numIOBlocks, this.linearY, this.numIOBlocks, this.numMovableBlocks);
		for(int i = 0; i < this.numBlocks; i++) {
			this.linearX[i] += this.tmpX[i];
			this.linearY[i] += this.tmpY[i];
		}
	}
	
	private void addPseudoConnections(double pseudoWeightFactor) {
		int[] legalX = this.legalizer.getBestLegalX();
		int[] legalY = this.legalizer.getBestLegalY();
		
		for(int index = this.numIOBlocks; index < this.numBlocks; index++) {
			
			this.addConnection(
					false, index, this.linearX[index],
					true, -1, legalX[index],
					pseudoWeightFactor, this.tmpX);
			
			this.addConnection(
					false, index, this.linearY[index],
					true, -1, legalY[index],
					pseudoWeightFactor, this.tmpY);
		}
	}
	
	
	protected void processNetsB2B() {
        int numNets = this.nets.size();
        for(int i = 0; i < numNets; i++) {
            this.processNetB2B(this.nets.get(i));
        }
	}
	
	private void processNetB2B(Integer[] blockIndexes) {
        
		int numNetBlocks = blockIndexes.length;
		double weightMultiplier = AnalyticalPlacer.getWeight(numNetBlocks) / (numNetBlocks - 1);
        
        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = blockIndexes[0], blockIndex2 = blockIndexes[1];
            boolean fixed1 = isFixed(blockIndex1), fixed2 = isFixed(blockIndex2);
            this.addConnection(fixed1, blockIndex1, this.linearX[blockIndex1], fixed2, blockIndex2, this.linearX[blockIndex2], weightMultiplier, this.tmpX);
            this.addConnection(fixed1, blockIndex1, this.linearY[blockIndex1], fixed2, blockIndex2, this.linearY[blockIndex2], weightMultiplier, this.tmpY);
            return;
        }
		
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
			   minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		int minXIndex = -1, maxXIndex = -1,
			minYIndex = -1, maxYIndex = -1;
		
		for(int i = 0; i < numNetBlocks; i++) {
			int blockIndex = blockIndexes[i];
			double x = this.linearX[blockIndex], y = this.linearY[blockIndex];
			
			if(x < minX) {
				minX = x;
				minXIndex = blockIndex;
			}
			if(x > maxX) {
				maxX = x;
				maxXIndex = blockIndex;
			}
			
			if(y < minY) {
				minY = y;
				minYIndex = blockIndex;
			}
			if(y > maxY) {
				maxY = y;
				maxYIndex = blockIndex;
			}
		}
		
		boolean minXFixed = isFixed(minXIndex),maxXFixed = isFixed(maxXIndex),
				minYFixed = isFixed(minYIndex),maxYFixed = isFixed(maxYIndex);
		
		for(int i = 0; i < numNetBlocks; i++) {
			int blockIndex = blockIndexes[i];
			boolean isFixed = isFixed(blockIndex);
			
			if(blockIndex != minXIndex) {
                this.addConnection(isFixed, blockIndex, this.linearX[blockIndex], minXFixed, minXIndex, this.linearX[minXIndex], weightMultiplier, this.tmpX);
				
				if(blockIndex != maxXIndex) {
					this.addConnection(isFixed, blockIndex, this.linearX[blockIndex], maxXFixed, maxXIndex, this.linearX[maxXIndex], weightMultiplier, this.tmpX);
				}
			}
			
			if(blockIndex != minYIndex) {
				this.addConnection(isFixed, blockIndex, this.linearY[blockIndex], minYFixed, minYIndex, this.linearY[minYIndex], weightMultiplier, this.tmpY);
                
				if(blockIndex != maxYIndex) {
					this.addConnection(isFixed, blockIndex, this.linearY[blockIndex], maxYFixed, maxYIndex, this.linearY[maxYIndex], weightMultiplier, this.tmpY);
				}
			}
		}
	}
	
	private boolean isFixed(int blockIndex) {
		return blockIndex < this.numIOBlocks;
	}
	
	
	protected void addConnection(
			boolean fixed1, int index1, double coordinate1,
			boolean fixed2, int index2, double coordinate2,
			double weightMultiplier, double[] newCoordinates) {
		
		double diff = coordinate2 - coordinate1;
        double weight = this.gradientSpeed * weightMultiplier * diff / (0.5 + Math.abs(diff));
		
		if(!fixed1) {
			newCoordinates[index1] += weight;
		}
		if(!fixed2) {
			newCoordinates[index2] -= weight;
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