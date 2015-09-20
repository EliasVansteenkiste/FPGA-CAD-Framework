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
	private double criticalityExponent;
	
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
	public void place() {
		
		this.calculator.recalculateFromScratch();
		this.rand = new Random(1);
		
		boolean greedy = Boolean.parseBoolean(this.options.get("greedy"));
		
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
		
		double inner_num = Double.parseDouble(this.options.get("inner_num"));
		this.movesPerTemperature = (int) (inner_num * Math.pow(this.circuit.numBlocks(), 4.0/3.0));
		
		System.out.println("Effort level: " + inner_num);
		System.out.println("Moves per temperature: " + this.movesPerTemperature);
		
		
		double tradeOffFactor = Double.parseDouble(this.options.get("trade_off_factor"));
		double tdPlaceExpFirst = Double.parseDouble(this.options.get("crit_exp_first"));
		double tdPlaceExpLast = Double.parseDouble(this.options.get("crit_exp_last"));
		int nbIterationsBeforeRecalculate = Integer.parseInt(this.options.get("nb_iterations_before_recalculate"));
		
		this.updateCriticalityExponent(this.getRlim(), tdPlaceExpFirst, tdPlaceExpLast);
		
		
		
		if(greedy) {
			doSwaps(firstRlim, tdPlaceExpFirst, tdPlaceExpLast, nbIterationsBeforeRecalculate, tradeOffFactor);
		
		} else {
		
			double T_multiplier = Double.parseDouble(this.options.get("T_multiplier"));
			calculateInitialTemperature(tradeOffFactor, nbIterationsBeforeRecalculate, T_multiplier);
	
			//Print SA parameters
			System.out.println("Initial temperature: " + this.T);
			System.out.println("Moves per temperature: " + this.movesPerTemperature);
			
			int tNumber = 0;
		
			while(this.T > 0.005 / this.circuit.getNets().values().size()) {
				int alphaAbs = doSwaps(firstRlim, tdPlaceExpFirst, tdPlaceExpLast, nbIterationsBeforeRecalculate, tradeOffFactor);
				
				double alpha = (double) alphaAbs / this.movesPerTemperature;
				
				this.updateRlim(alpha);
				this.updateTemperature(alpha);
				
				this.updateCriticalityExponent(firstRlim, tdPlaceExpFirst, tdPlaceExpLast);
				System.out.println("Temperature " + tNumber +" = " + this.T + ", cost = " + cost + ", Rlim = " + this.getRlim() + ", crit exp = " + this.criticalityExponent);
				tNumber++;
			}
		}
	}
	
	private int doSwaps(int firstRlim, double tdPlaceExpFirst, double tdPlaceExpLast, int nbIterationsBeforeRecalculate, double tradeOffFactor) {
		
		this.timingGraph.setCriticalityExponent(this.criticalityExponent);
		this.timingGraph.recalculateAllSlacksCriticalities();
		double previousBBCost = this.calculator.calculateTotalCost();
		double previousTDCost = this.timingGraph.calculateTotalCost();
		
		int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
		int alphaAbs=0;
		for (int i =0; i<this.movesPerTemperature;i++) 
		{
			Swap swap = findSwap(this.getRlim());
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
											(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
			{
				double deltaBBCost = this.calculator.calculateDeltaCost(swap);
				double deltaTimingCost = this.timingGraph.calculateDeltaCost(swap);
				
				double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / previousBBCost
									+ (tradeOffFactor * deltaTimingCost) / previousTDCost;
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
					if(this.rand.nextDouble() < Math.exp(-deltaCost / this.T))
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
				previousBBCost = this.calculator.calculateTotalCost();
				previousTDCost = this.timingGraph.calculateTotalCost();
				iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			}
		}
		
		return alphaAbs;
	}
	
	
	private void calculateInitialTemperature(double tradeOffFactor, int nbIterationsBeforeRecalculate, double T_multiplier) {
		
		this.timingGraph.setCriticalityExponent(this.criticalityExponent);
		this.timingGraph.recalculateAllSlacksCriticalities();
		double previousBBCost = this.calculator.calculateTotalCost();
		double previousTDCost = this.timingGraph.calculateTotalCost();
		
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		
		int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
		
		for (int i = 0; i < this.circuit.numBlocks(); i++) {
			Swap swap = findSwap(this.getRlim());
			
			//Swap
			double deltaBBCost = this.calculator.calculateDeltaCost(swap);
			double deltaTimingCost = this.timingGraph.calculateDeltaCost(swap);
			double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / previousBBCost
								+ (tradeOffFactor * deltaTimingCost) / previousTDCost;
			
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
				previousBBCost = this.calculator.calculateTotalCost();
				previousTDCost = this.timingGraph.calculateTotalCost();
				iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			}
		}
		double somKwadraten = kwadratischeSomDeltaKost;
		double kwadraatSom = Math.pow(somDeltaKost,2);
		double nbElements = this.circuit.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		
		this.T = T_multiplier * stdafwijkingDeltaKost;
	}
	
	private void updateCriticalityExponent(double firstRlim, double tdPlaceExpFirst, double tdPlaceExpLast) {
		double finalRlim = 1.0;
		double inverseDeltaRlim = 1 / (firstRlim - finalRlim);
		this.criticalityExponent = (1 - (this.getRlimd() - finalRlim) * inverseDeltaRlim) * (tdPlaceExpLast - tdPlaceExpFirst) + tdPlaceExpFirst;
	}
	
}
