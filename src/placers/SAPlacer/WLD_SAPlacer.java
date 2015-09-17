package placers.SAPlacer;

import java.util.HashMap;
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
		defaultOptions.put("T_multiplier", "1");
	}
	
	public WLD_SAPlacer(Architecture architecture, PackedCircuit circuit, HashMap<String, String> options)
	{
		super(architecture, circuit, options);
	}
	
	@Override
	public void place()
	{
		this.rand = new Random(1);
		this.calculator.recalculateFromScratch();
		
		
		this.greedy = Boolean.parseBoolean(this.options.get("greedy"));
		
		int optionRlimd = Integer.parseInt(this.options.get("Rlim"));
		if(optionRlimd == -1) {
			optionRlimd = Math.max(this.architecture.getWidth(), this.architecture.getHeight());
		}
		this.setRlimd(optionRlimd);
		
		
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
	

	
	@Override
	public void lowTempAnneal(double innerNum)
	{
		this.calculator.recalculateFromScratch();
		this.rand = new Random(1);
		int biggestDistance = getBiggestDistance();
		
		int maxValue = (int)Math.floor(biggestDistance / 16.0);
		if(maxValue < 1)
		{
			maxValue = 1;
		}
		
		setRlimd(maxValue);
		
		calculateInitialTemperatureLow();
		int movesPerTemperature=(int) (innerNum*Math.pow(this.circuit.numBlocks(),4.0/3.0));
		
		System.out.println("Initial temp = " + this.T + ", initial Rlim = " + this.getRlim());
		
		while (this.T > 0.005 * this.calculator.calculateAverageNetCost()) {
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) {
				Swap swap = findSwap(this.getRlim());
      			
				if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
												(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
				{
					double deltaCost = this.calculator.calculateDeltaCost(swap);
					
					if(deltaCost<=0)
					{
						swap.apply();
						alphaAbs+=1;
						this.calculator.pushThrough();
					}
					else
					{
						if(this.rand.nextDouble()<Math.exp(-deltaCost / this.T))
						{
							swap.apply();
							alphaAbs+=1;
							this.calculator.pushThrough();
						}
						else
						{
							this.calculator.revert();
						}
					}
				}
			}

			double alpha = ((double) alphaAbs) / movesPerTemperature;
			
			updateRlim(alpha, maxValue);
			updateTemperature(alpha);
		}
	}
	
	
	private void calculateInitialTemperature() {
		calculateInitialTemperature(1);
	}
	
	private void calculateInitialTemperature(double T_multiplier) {
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		for (int i = 0; i < this.circuit.numBlocks(); i++) {
			Swap swap = findSwap(this.getRlim());
			double deltaCost = this.calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) 
					&& (swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed))
					&& deltaCost <= 0) {
				
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
		
		this.T = 20 * stdafwijkingDeltaKost * T_multiplier;
	}
	

	
	private void calculateInitialTemperatureLow() {
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		
		for (int i = 0; i < this.circuit.numBlocks(); i++) {
			int Rlim = this.getRlim();
			Swap swap = findSwap(Rlim);
			
			double deltaCost = this.calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
											(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed))) {
				if(deltaCost <= 0) {
					swap.apply();
					this.calculator.pushThrough();
					sumNegDeltaCost -= deltaCost;
					quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
					numNegDeltaCost++;
				
				} else {
					this.calculator.revert();
				}
				
			} else {
				this.calculator.revert();
			}
		}
		
		double somNegKwadraten = quadraticSumNegDeltaCost;
		double negKwadraatSom = Math.pow(sumNegDeltaCost, 2);
		double stdafwijkingNegDeltaKost = Math.sqrt(somNegKwadraten/numNegDeltaCost - negKwadraatSom/(numNegDeltaCost*numNegDeltaCost));
		
		this.T = 2*stdafwijkingNegDeltaKost;
		
		if(this.T <= 0 || this.T >= 10000) {
			System.out.println("Trouble");
			this.T = 1.0;
		}
	}
}