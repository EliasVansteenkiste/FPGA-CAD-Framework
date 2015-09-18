package placers.SAPlacer;

import java.util.Map;
import java.util.Random;

import architecture.Architecture;
import circuit.PackedCircuit;

public class WLD_SAPlacer extends SAPlacer
{
	
	private int movesPerTemperature;
	
	static {
		//Effort level: affects the number of swaps per temperature
		defaultOptions.put("inner_num", "1.0");
		
		defaultOptions.put("greedy", "0");
		defaultOptions.put("Rlim", "-1");
		defaultOptions.put("maxRlim", "-1");
		defaultOptions.put("T_multiplier", "20");
	}
	
	public WLD_SAPlacer(Architecture architecture, PackedCircuit circuit, Map<String, String> options)
	{
		super(architecture, circuit, options);
	}
	
	@Override
	public void place()
	{
		this.rand = new Random(1);
		this.calculator.recalculateFromScratch();
		
		
		this.greedy = Boolean.parseBoolean(this.options.get("greedy"));
		
		int optionMaxRlim = Integer.parseInt(this.options.get("Rlim"));
		if(optionMaxRlim == -1) {
			optionMaxRlim = Math.max(this.architecture.getWidth(), this.architecture.getHeight());
		}
		this.setMaxRlim(optionMaxRlim);
		
		int optionRlim = Integer.parseInt(this.options.get("Rlim"));
		if(optionRlim == -1) {
			optionRlim = Math.max(this.architecture.getWidth(), this.architecture.getHeight());
		}
		this.setRlimd(optionRlim);
		
		
		double inner_num = Double.parseDouble(this.options.get("inner_num"));
		this.movesPerTemperature = (int) (inner_num * Math.pow(this.circuit.numBlocks(), 4.0/3.0));
		
		
		//Print SA parameters
		System.out.println("Effort level: " + inner_num);
		System.out.println("Moves per temperature: " + this.movesPerTemperature);
		
		if(this.greedy) {
			this.doSwaps();
		
		} else {
			double T_multiplier = Double.parseDouble(this.options.get("T_multiplier"));
			calculateInitialTemperature(T_multiplier);
			
			System.out.println(this.calculator.calculateTotalCost());
			System.out.println("Initial temperature: " + this.T);
			
			
			int tNumber = 0;
			
			//Do placement
			while (this.T > 0.005 * this.calculator.calculateAverageNetCost()) {
				int alphaAbs = this.doSwaps();

				double alpha = ((double) alphaAbs) / this.movesPerTemperature;
				this.updateRlim(alpha);
				this.updateTemperature(alpha);
				
				double cost = this.calculator.calculateTotalCost();
				System.out.println("Temperature " + tNumber +" = " + this.T + ", cost = " + cost + ", Rlim = " + this.getRlim());
				
				tNumber++;
			}
			
			System.out.println("Last temp: " + this.T);
		}
	}
	
	
	private int doSwaps() {
		int numSwaps = 0;
		int Rlim = this.getRlim();
		boolean greedy = (this.T == 0);
		
		
		for (int i = 0; i < this.movesPerTemperature; i++) {
			Swap swap = findSwap(Rlim);
			
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
					(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed))) {
				
				double deltaCost = this.calculator.calculateDeltaCost(swap);
				
				if(deltaCost <= 0 || (!greedy && this.rand.nextDouble() < Math.exp(-deltaCost / this.T))) {
					swap.apply();
					numSwaps++;
					this.calculator.pushThrough();
					
				} else {
					this.calculator.revert();
				}
			}
		}
		
		return numSwaps;
	}
	
	
	private void calculateInitialTemperature(double T_multiplier) {
		double somDeltaKost = 0;
		double kwadratischeSomDeltaKost = 0;
		
		for (int i = 0; i < this.circuit.numBlocks(); i++) {
			Swap swap = findSwap(this.getRlim());
			double deltaCost = this.calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) 
					&& (swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed))
					&& (deltaCost <= 0)) {
				
				swap.apply();
				this.calculator.pushThrough();
			
			} else {
				this.calculator.revert();
			}
			
			somDeltaKost+=deltaCost;
			kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
		}
		double somKwadraten = kwadratischeSomDeltaKost;
		double kwadraatSom = Math.pow(somDeltaKost,2);
		double nbElements = this.circuit.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		
		this.T = T_multiplier * stdafwijkingDeltaKost;
	}
}