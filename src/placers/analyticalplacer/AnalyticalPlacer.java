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
import placers.analyticalplacer.linear_solver.LinearSolver;
import placers.analyticalplacer.linear_solver.LinearSolverComplete;
import placers.analyticalplacer.linear_solver.LinearSolverGradient;
import util.Logger;


public abstract class AnalyticalPlacer extends Placer {
	
    protected static enum SolveMode {GRADIENT, COMPLETE};
    private static enum Axis {X, Y};
    
    private final SolveMode algorithmSolveMode;
    
	private final double[] maxUtilizationSequence;
	private final double startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal;
	private final double gradientMultiplier;
    private double gradientSpeed;
    
    protected final Map<GlobalBlock, Integer> blockIndexes = new HashMap<>();
	protected final List<int[]> nets = new ArrayList<>();
	private int numBlocks, numIOBlocks;
	
	private double[] linearX, linearY;
    private LinearSolver solver;
	
	private CostCalculator costCalculator;
	protected Legalizer legalizer;
	
	
	static {
		//initialize maxUtilizationSequence used by the legalizer
		defaultOptions.put("max_utilization_sequence", "1");
		
		//The first anchorWeight factor that will be used in the main solve loop
		defaultOptions.put("starting_anchor_weight", "1");
		
		//The amount with which the anchorWeight factor will be increased each iteration (multiplicative)
		defaultOptions.put("anchor_weight_increase", "1.15");
		
		//The ratio of linear solutions cost to legal solution cost at which we stop the algorithm
		defaultOptions.put("stop_ratio_linear_legal", "0.95");
		
		// The speed at which the gradient solver moves to the optimal position
        defaultOptions.put("solve_mode", "complete");
		defaultOptions.put("initial_gradient_speed", "0.05");
		defaultOptions.put("gradient_multiplier", "1");
	}
	
	public AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		// Parse options
		
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
        String solveModeString = this.parseStringOption("solve_mode");
        switch (solveModeString) {
            case "gradient":
                this.algorithmSolveMode = SolveMode.GRADIENT;
                break;
                
            case "complete":
                this.algorithmSolveMode = SolveMode.COMPLETE;
                break;
                
            default:
                Logger.raise("Unknown solve mode: " + solveModeString);
                this.algorithmSolveMode = null;
        }
        
		this.gradientSpeed = this.parseDoubleOption("initial_gradient_speed");
		this.gradientMultiplier = this.parseDoubleOption("gradient_multiplier");
    }
    
    
    protected abstract CostCalculator createCostCalculator();
    protected abstract void initializePlacementIteration();
    
    
    @Override
    public void initializeData() {
        // Get number of blocks
		int numBlocksCounter = 0;
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			numBlocksCounter += this.circuit.getBlocks(blockType).size();
		}
        this.numBlocks = numBlocksCounter;
		
		
		// Initialize block positions at the center of the design 
		this.linearX = new double[this.numBlocks];
		this.linearY = new double[this.numBlocks];
		Arrays.fill(this.linearX, this.circuit.getWidth() / 2 - 1);
		Arrays.fill(this.linearY, this.circuit.getHeight() / 2 - 1);
        
		// Make a list of all block types, with IO blocks first
		BlockType ioBlockType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
		List<BlockType> blockTypes = new ArrayList<>();
		blockTypes.add(ioBlockType);
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			if(!blockType.equals(ioBlockType)) {
				blockTypes.add(blockType);
			}
		}
		
		
		
		// Add all blocks
		List<Integer> blockTypeIndexStarts = new ArrayList<>();
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
		
		
		
		// Add all nets
		// Loop through all net sources
		for(Map.Entry<GlobalBlock, Integer> sourceBlockEntry : blockIndexes.entrySet()) {
			GlobalBlock sourceBlock = sourceBlockEntry.getKey();
			
			for(AbstractPin sourcePin : sourceBlock.getOutputPins()) {
				
				// Build a set of all blocks connected to the net
				List<AbstractPin> netPins = new ArrayList<>();
				netPins.add(sourcePin);
				netPins.addAll(sourcePin.getSinks());
				
				Set<GlobalBlock> netBlocks = new HashSet<>();
				for(AbstractPin pin : netPins) {
					netBlocks.add((GlobalBlock) pin.getOwner());
				}
				
				// Make a list of the block indexes
				int numNetBlocks = netBlocks.size();
                
                // Ignore nets of size 1
                if(numNetBlocks > 1) {
                    int[] netBlockIndexes = new int[numNetBlocks];
                    int i = 0;
                    for(GlobalBlock netBlock : netBlocks) {
                        netBlockIndexes[i] = blockIndexes.get(netBlock);
                        i++;
                    }

                    // Add this "net" to the list of nets
                    this.nets.add(netBlockIndexes);
                }
			}
		}
        
        this.costCalculator = createCostCalculator();
        
        this.legalizer = new Legalizer(
				circuit, costCalculator,
				blockIndexes,
				blockTypes, blockTypeIndexStarts,
				this.linearX, this.linearY);
	}
	
    
	@Override
	public void place() {
        
		int iteration = 0;
		double pseudoWeightFactor = this.startingAnchorWeight;
		double linearCost, legalCost;
        boolean firstSolve = true;
        
		do {
			System.out.format("Iteration %d: pseudoWeightFactor = %f, gradientSpeed = %f",
					iteration, pseudoWeightFactor, this.gradientSpeed);
            
            this.initializePlacementIteration();
			
			// Solve linear
			if(firstSolve) {
                for(int i = 0; i < 5; i++) {
                    this.solveLinearComplete(true, pseudoWeightFactor);
                }
                
            } else if(this.algorithmSolveMode == SolveMode.COMPLETE) {
                this.solveLinearComplete(false, pseudoWeightFactor);
            
            } else {
                for(int i = 0; i < 1000; i++) {
                    this.solveLinearGradient(false, pseudoWeightFactor);
                    //System.out.println(i + ": " + this.costCalculator.calculate(this.linearX, this.linearY));
                }
            }
            
            pseudoWeightFactor *= this.anchorWeightIncrease;
            this.gradientSpeed *= this.gradientMultiplier;
            firstSolve = false;
			
			// Legalize
			int sequenceIndex = Math.min(iteration, this.maxUtilizationSequence.length - 1);
			double maxUtilizationLegalizer = this.maxUtilizationSequence[sequenceIndex];
			
			this.legalizer.legalize(maxUtilizationLegalizer);
			
			
			// Get the costs and print them
			linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
			legalCost = this.legalizer.getBestCost();
			//System.out.format("Legal gost = %f\n", legalCost);
			System.out.format(", linear cost = %f, legal cost = %f\n", linearCost, legalCost);
			
			iteration++;
			
		} while(linearCost / legalCost < this.stopRatioLinearLegal || true); // DEBUG
		
		
		this.legalizer.updateCircuit();
	}
    
    
    // These two methods only exist to be able to profile the "complete" part
    // of the execution to the "gradient" part.
    private void solveLinearComplete(boolean firstSolve, double pseudoweightFactor) {
        double epsilon = 0.0001;
        this.solver = new LinearSolverComplete(this.linearX, this.linearY, this.numIOBlocks, epsilon);
        this.solveLinear(firstSolve, pseudoweightFactor);
    }
    private void solveLinearGradient(boolean firstSolve, double pseudoweightFactor) {
        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.numIOBlocks, this.gradientSpeed);
        this.solveLinear(firstSolve, pseudoweightFactor);
    }
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 */
	private void solveLinear(boolean firstSolve, double pseudoWeightFactor) {
		
		//Add pseudo connections
		if(!firstSolve) {
			this.addPseudoConnections(pseudoWeightFactor);
		}
        
        // Add connections between blocks that are connected by a net
		this.processNets();
		
        
        // Solve and save result
        this.solver.solve();
	}
	
	private void addPseudoConnections(double pseudoWeightFactor) {
		int[] legalX = this.legalizer.getBestLegalX();
		int[] legalY = this.legalizer.getBestLegalY();
		
		for(int blockIndex = this.numIOBlocks; blockIndex < this.numBlocks; blockIndex++) {
            this.solver.addPseudoConnection(blockIndex, legalX[blockIndex], legalY[blockIndex], pseudoWeightFactor);
		}
	}
	
	
	protected void processNets() {
        for(int[] net : this.nets) {
            this.solver.processNet(net);
        }
	}
    
    
    public static double getWeight(int size) {
		switch (size) {
			case 1:
			case 2:
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