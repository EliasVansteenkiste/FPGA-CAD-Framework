package placers.SAPlacer;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import placers.Placer;

import flexible_architecture.Circuit;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.site.AbstractSite;

public abstract class SAPlacer extends Placer
{
	
	static {
		defaultOptions.put("effort_level", "1");
		defaultOptions.put("T_multiplier", "1");
		
		defaultOptions.put("detailed", "0");
		defaultOptions.put("greedy", "0");
		defaultOptions.put("Rlim", "-1");
		defaultOptions.put("maxRlim", "-1");
	}
	
	private double Rlimd;
	private int Rlim, maxRlim;
	private double T, TMultiplier;
	
	private boolean greedy, detailed;
	private double effortLevel;
	private int movesPerTemperature;
	
	protected boolean circuitChanged = true;
	private double[] deltaCosts;
	
	private final double TMultiplierGlobal = 5; 
	
	
	protected Random random;
	
	public SAPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		// Get greedy option
		this.greedy = this.parseBooleanOption("greedy");
		
		// Get detailed option
		this.detailed = this.parseBooleanOption("detailed");
		
		
		// Get Rlim and maxRlim option
		int size = Math.max(this.circuit.getWidth(), this.circuit.getHeight());
		
		int optionMaxRlim = this.parseIntegerOptionWithDefault("maxRlim", size);
		int optionRlim = this.parseIntegerOptionWithDefault("Rlim", size);
		
		// Set maxRlim first, because Rlim depends on it
		this.setMaxRlim(optionMaxRlim);
		this.setRlimd(optionRlim);
		
		
		// Get inner_num option
		this.effortLevel = Double.parseDouble(this.options.get("effort_level"));
		this.movesPerTemperature = (int) (this.effortLevel * Math.pow(this.circuit.getNumGlobalBlocks(), 4.0/3.0));
		
		// Get T multiplier option
		this.TMultiplier = Double.parseDouble(this.options.get("T_multiplier"));
	}
	
	
	public void place() {
		this.initializePlace();
		
		this.random = new Random(1);
		
		//Print parameters
		System.out.println("Effort level: " + this.effortLevel);
		System.out.println("Moves per temperature: " + this.movesPerTemperature);
		
		
		if(this.greedy) {
			this.doSwapIteration();
		
		} else {
			
			if(this.detailed) {
				this.calculateInititalTemperatureDetailed();
			} else {
				this.calculateInititalTemperatureGlobal();
			}
			
			System.out.println("Initial temperature: " + this.T);
			
			
			int iteration = 0;
			
			// Do placement
			while(this.T > 0.005 * this.getCost() / this.circuit.getNumGlobalBlocks()) {
				int numSwaps = this.doSwapIteration();
				double alpha = ((double) numSwaps) / this.movesPerTemperature;
				
				this.updateRlim(alpha);
				this.updateTemperature(alpha);
				
				System.out.format("Temperature %d = %.9f, Rlim = %d, %s\n",
						iteration, this.T, this.Rlim, this.getStatistics());
				
				iteration++;
			}
			
			System.out.println("Last temp: " + this.T);
		}
	}
	
	
	
	private void calculateInititalTemperatureGlobal() {
		int numSamples = this.circuit.getNumGlobalBlocks();
		double stdDev = this.doSwapIteration(numSamples, false);
		
		this.T = this.TMultiplier * this.TMultiplierGlobal * stdDev;
	}
	
	private void calculateInititalTemperatureDetailed() {
		// Use the method described in "Temperature Measurement and
		// Equilibrium Dynamics of Simulated Annealing Placements"
		
		int numSamples = Math.max(this.circuit.getNumGlobalBlocks() / 5, 500);
		this.doSwapIteration(numSamples, false);
		
		Arrays.sort(this.deltaCosts);
		
		int zeroIndex = Arrays.binarySearch(this.deltaCosts, 0);
		if(zeroIndex < 0) {
			zeroIndex = -zeroIndex - 1;
		}
		
		double Emin = integral(this.deltaCosts, 0, zeroIndex, 0);
		double maxEplus = integral(this.deltaCosts, zeroIndex, numSamples, 0);
		
		if(maxEplus < Emin) {
			System.err.println("SA failed to get a temperature estimate");
			System.exit(1);
		}
		
		double minT = 0;
		double maxT = Double.MAX_VALUE;
		
		// very coarse estimate
		this.T = this.deltaCosts[this.deltaCosts.length - 1] / 1000;
		
		while(minT == 0 || maxT / minT > 1.1) {
			double Eplus = integral(this.deltaCosts, zeroIndex, numSamples, this.T);
			
			if(Emin < Eplus) {
				if(this.T < maxT) {
					maxT = this.T;
				}
				
				if(minT == 0) {
					this.T /= 8;
				} else {
					this.T = (maxT + minT) / 2;
				}
			
			} else {
				if(this.T > minT) {
					minT = this.T;
				}
				
				if(maxT == Double.MAX_VALUE) {
					this.T *= 8;
				} else {
					this.T = (maxT + minT) / 2;
				}
			}
		}
		
		this.T *= this.TMultiplier;
	}
	
	private double integral(double[] values, int start, int stop, double temperature) {
		double sum = 0;
		for(int i = start; i < stop; i++) {
			if(temperature == 0) {
				sum += values[i];
			} else {
				sum += values[i] * Math.exp(-values[i] / temperature);
			}
		}
		
		return Math.abs(sum / values.length);
	}
	
	
	
	private int doSwapIteration() {
		return (int) this.doSwapIteration(this.movesPerTemperature, true);
	}
	
	private double doSwapIteration(int moves, boolean pushThrough) {
		
		this.initializeSwapIteration();
		
		int numSwaps = 0;
		int Rlim = this.getRlim();
		
		
		double sumDeltaCost = 0;
		double quadSumDeltaCost = 0;
		if(!pushThrough) {
			this.deltaCosts = new double[moves];
		}
		
		
		for (int i = 0; i < moves; i++) {
			Swap swap = this.findSwap(Rlim);
			
			if((swap.getBlock1() == null || !swap.getBlock1().isFixed())
					&& (swap.getBlock2() == null || !swap.getBlock2().isFixed())) {
				
				double deltaCost = this.getDeltaCost(swap);
				
				
				if(pushThrough) {
					if(deltaCost <= 0
							|| (this.greedy == false && this.random.nextDouble() < Math.exp(-deltaCost / this.T))) {
						
						swap.apply();
						numSwaps++;
						
						this.pushThrough(i);
						this.circuitChanged = true;
						
					} else {
						this.revert(i);
					}
				
				} else {
					this.revert(i);
					this.deltaCosts[i] = deltaCost;
					sumDeltaCost += deltaCost;
					quadSumDeltaCost += deltaCost * deltaCost;
				}
			}
		}
		
		if(pushThrough) {
			return numSwaps;
		
		} else {
			double sumQuads = quadSumDeltaCost;
			double quadSum = sumDeltaCost * sumDeltaCost;
			
			double numBlocks = this.circuit.getNumGlobalBlocks();
			double quadNumBlocks = numBlocks * numBlocks;
			
			return Math.sqrt(Math.abs(sumQuads / numBlocks - quadSum / quadNumBlocks));
		}
	}
	
	protected abstract void initializePlace();
	protected abstract void initializeSwapIteration();
	protected abstract String getStatistics();
	protected abstract double getCost();
	protected abstract double getDeltaCost(Swap swap);
	protected abstract void pushThrough(int iteration);
	protected abstract void revert(int iteration);
	
	
	
	
	protected Swap findSwap(int Rlim) {
		GlobalBlock fromBlock = null;
		AbstractSite toSite = null;
		do {
			// Find a suitable block
			do {
				fromBlock = this.circuit.getRandomBlock(this.random);
			} while(fromBlock.isFixed());
			
			// Find a suitable site near this block
			do {
				toSite = this.circuit.getRandomSite(fromBlock, Rlim, this.random);
			} while(toSite != null && fromBlock.getSite() == toSite);
			
			// If toSite == null, this means there are no suitable blocks near the block
			// Try another block
		} while(toSite == null);
		
		Swap swap = new Swap(fromBlock, toSite, this.random);
		return swap;
	}
	
	
	
	protected void updateTemperature(double alpha) {
		double gamma;
		
		if (alpha > 0.96)     	gamma = 0.5;
		else if (alpha > 0.8)	gamma = 0.9;
		else if (alpha > 0.15)	gamma = 0.95;
		else 					gamma = 0.8;
		
		this.T *= gamma;
	}
	
	
	protected int getRlim() {
		return this.Rlim;
	}
	protected double getRlimd() {
		return this.Rlimd;
	}
	
	protected void setMaxRlim(int maxRlim) {
		this.maxRlim = maxRlim;
	}
	protected void setRlimd(double Rlimd) {
		this.Rlimd = Rlimd;
		this.Rlim = (int) Math.round(this.Rlimd);
	}
	
	
	protected void updateRlim(double alpha) {
		this.updateRlim(alpha, this.maxRlim);
	}
	
	protected void updateRlim(double alpha, int maxValue) {
		double newRlimd = this.Rlimd * (1 - 0.44 + alpha);
		
		if(newRlimd > maxValue) {
			newRlimd = maxValue;
		}
		
		if(newRlimd < 1) {
			newRlimd = 1;
		}
		
		this.setRlimd(newRlimd);
	}
}