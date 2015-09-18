package placers.SAPlacer;

import java.util.HashMap;
import java.util.Random;

import timinganalysis.TimingGraph;
import architecture.Architecture;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public class TD_SAPlacer extends SAPlacer
{
	
	private TimingGraph timingGraph;
	private double previousBBCost;
	private double previousTDCost;
	
	static {
		//Effort level: affects the number of swaps per temperature
		defaultOptions.put("inner_num", "1.0");
		
		//Timing trade off factor: 0.0 = wire-length-driven
		defaultOptions.put("trade_off_factor", "0.5");
		
		//Starting criticality exponent
		defaultOptions.put("crit_exp_first", "1.0");
		
		//Final criticality exponent
		defaultOptions.put("crit_exp_last", "1.0");
		
		defaultOptions.put("nb_iterations_before_recalculate", "100000");
		
		
		defaultOptions.put("greedy", "0");
		defaultOptions.put("Rlim", "-1");
		defaultOptions.put("maxRlim", "-1");
		defaultOptions.put("T_multiplier", "20");
	}
	

	public TD_SAPlacer(Architecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit, HashMap<String, String> options)
	{
		super(architecture, circuit, options);
		this.timingGraph = new TimingGraph(prePackedCircuit);
		this.timingGraph.buildTimingGraph();
	}
	
	@Override
	public void place()
	{
		
		double inner_num = Double.parseDouble(this.options.get("inner_num"));
		double tradeOffFactor = Double.parseDouble(this.options.get("trade_off_factor"));
		double tdPlaceExpFirst = Double.parseDouble(this.options.get("crit_exp_first"));
		double tdPlaceExpLast = Double.parseDouble(this.options.get("crit_exp_last"));
		int nbIterationsBeforeRecalculate = Integer.parseInt(this.options.get("nb_iterations_before_recalculate"));
		
		placeLoop(inner_num, tradeOffFactor, tdPlaceExpFirst, tdPlaceExpLast, nbIterationsBeforeRecalculate);
	}
	
	
	private void placeLoop(double inner_num, double tradeOffFactor, double tdPlaceExpFirst, 
										double tdPlaceExpLast, int nbIterationsBeforeRecalculate)
	{
		//Initialize SA parameters
		this.calculator.recalculateFromScratch();
		this.rand = new Random(1);
		
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
		int firstRlim = optionRlim;
		
		
		
		double criticalityExponent = updateCriticalityExponent(this.getRlim(), tdPlaceExpFirst, tdPlaceExpLast);
		this.timingGraph.setCriticalityExponent(criticalityExponent);
		this.timingGraph.recalculateAllSlacksCriticalities();
		this.previousBBCost = this.calculator.calculateTotalCost();
		this.previousTDCost = this.timingGraph.calculateTotalCost();
		double T = calculateInitialTemperature(tradeOffFactor, nbIterationsBeforeRecalculate);
		int movesPerTemperature = (int) (inner_num*Math.pow(this.circuit.numBlocks(),4.0/3.0));

		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		while(T > 0.005 / this.circuit.getNets().values().size())
		{
			this.timingGraph.setCriticalityExponent(criticalityExponent);
			this.timingGraph.recalculateAllSlacksCriticalities();
			this.previousBBCost = this.calculator.calculateTotalCost();
			this.previousTDCost = this.timingGraph.calculateTotalCost();
			
			int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(this.getRlim());
				if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
												(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
				{
					double deltaBBCost = this.calculator.calculateDeltaCost(swap);
					double deltaTimingCost = this.timingGraph.calculateDeltaCost(swap);
					
					double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / this.previousBBCost
										+ (tradeOffFactor * deltaTimingCost) / this.previousTDCost;
					//double deltaCost = deltaBBCost;
					
					if(deltaCost<=0)
					{
						swap.apply();
						alphaAbs+=1;
						this.calculator.pushThrough();
						this.timingGraph.pushThrough();
					}
					else
					{
						if(this.rand.nextDouble() < Math.exp(-deltaCost/T))
						{
							swap.apply();
							alphaAbs+=1;
							this.calculator.pushThrough();
							this.timingGraph.pushThrough();
						}
						else
						{
							this.calculator.revert();
							this.timingGraph.revert();
						}
					}
				}
				iterationsToGoBeforeRecalculate--;
				if(iterationsToGoBeforeRecalculate <= 0)
				{
					this.previousBBCost = this.calculator.calculateTotalCost();
					this.previousTDCost = this.timingGraph.calculateTotalCost();
					iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
				}
			}
			double alpha = (double)alphaAbs/movesPerTemperature;
			this.updateRlim(alpha);
			this.updateTemperature(alpha);
			criticalityExponent = updateCriticalityExponent(firstRlim, tdPlaceExpFirst, tdPlaceExpLast);
		}
	}
	
	
	private double calculateInitialTemperature(double tradeOffFactor, int nbIterationsBeforeRecalculate)
	{
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		
		int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
		
		for (int i = 0; i < this.circuit.numBlocks(); i++) {
			Swap swap = findSwap(this.getRlim());
			
			//Swap
			double deltaBBCost = this.calculator.calculateDeltaCost(swap);
			double deltaTimingCost = this.timingGraph.calculateDeltaCost(swap);
			double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / this.previousBBCost
								+ (tradeOffFactor * deltaTimingCost) / this.previousTDCost;
			
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed))
					&& (swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed))
					&& deltaCost < 0) {
				
				swap.apply();
				this.calculator.pushThrough();
				this.timingGraph.pushThrough();
				somDeltaKost+=deltaCost;
				kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
			}
			else
			{
				this.calculator.revert();
				this.timingGraph.revert();
			}
			
			iterationsToGoBeforeRecalculate--;
			if(iterationsToGoBeforeRecalculate <= 0)
			{
				this.previousBBCost = this.calculator.calculateTotalCost();
				this.previousTDCost = this.timingGraph.calculateTotalCost();
				iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			}
		}
		double somKwadraten = kwadratischeSomDeltaKost;
		double kwadraatSom = Math.pow(somDeltaKost,2);
		double nbElements = this.circuit.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		double T=20*stdafwijkingDeltaKost;
		
		return T;
	}
	
	private double updateCriticalityExponent(double firstRlim, double tdPlaceExpFirst, double tdPlaceExpLast)
	{
		double finalRlim = 1.0;
		double inverseDeltaRlim = 1 / (firstRlim - finalRlim);
		return (1 - (this.getRlimd() - finalRlim) * inverseDeltaRlim) * (tdPlaceExpLast - tdPlaceExpFirst) + tdPlaceExpFirst;
	}
	
}
