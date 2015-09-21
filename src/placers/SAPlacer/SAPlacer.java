package placers.SAPlacer;

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
		
		defaultOptions.put("greedy", "0");
		defaultOptions.put("Rlim", "-1");
		defaultOptions.put("maxRlim", "-1");
	}
	
	private double Rlimd;
	private int Rlim, maxRlim;
	private double T, T_multiplier;
	
	private boolean greedy;
	private double effortLevel;
	private int movesPerTemperature;
	
	protected boolean circuitChanged = true;
	
	
	protected Random random;
	
	public SAPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		// Get greedy option
		try {
			int greedy_int = Integer.parseInt(this.options.get("greedy"));
			this.greedy = (greedy_int > 0);
		
		} catch(NumberFormatException e) {
			this.greedy = Boolean.parseBoolean(this.options.get("greedy"));
		}
		
		
		// Get Rlim and maxRlim option
		int optionMaxRlim = Integer.parseInt(this.options.get("Rlim"));
		if(optionMaxRlim == -1) {
			optionMaxRlim = Math.max(this.circuit.getWidth(), this.circuit.getHeight());
		}
		this.setMaxRlim(optionMaxRlim);
		
		int optionRlim = Integer.parseInt(this.options.get("Rlim"));
		if(optionRlim == -1) {
			optionRlim = Math.max(this.circuit.getWidth(), this.circuit.getHeight());
		}
		this.setRlimd(optionRlim);
		
		// Get inner_num option
		this.effortLevel = Double.parseDouble(this.options.get("effort_level"));
		this.movesPerTemperature = (int) (this.effortLevel * Math.pow(this.circuit.getNumGlobalBlocks(), 4.0/3.0));
		
		// Get T multiplier option
		this.T_multiplier = Double.parseDouble(this.options.get("T_multiplier"));
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
			this.calculateInititalTemperature();
			System.out.println("Initial temperature: " + this.T);
			
			
			int iteration = 0;
			
			//Do placement
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
	
	
	
	private void calculateInititalTemperature() {
		double stdDeviation = this.doSwapIteration(this.circuit.getNumGlobalBlocks(), false);
		this.T = this.T_multiplier * stdDeviation;
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
		GlobalBlock fromBlock;
		do {
			fromBlock = this.circuit.getRandomBlock(this.random);
		} while(fromBlock.isFixed());
		
		AbstractSite toSite = this.circuit.getRandomSite(fromBlock, Rlim, this.random);
		
		return new Swap(fromBlock, toSite, this.random);
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