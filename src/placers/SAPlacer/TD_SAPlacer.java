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

	public TD_SAPlacer(Architecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
	{
		super(architecture, circuit);
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
	}
	
	public void place(HashMap<String, String> options)
	{
		//Effort level: affects the number of swaps per temperature
		double inner_num = 1.0; //Default = Low effort
		if(options.get("inner_num") != null)
		{
			inner_num = Double.parseDouble(options.get("inner_num"));
		}
		
		//Timing trade off factor: 0.0 = wire-length-driven
		double tradeOffFactor = 0.5;
		if(options.get("trade_off_factor") != null)
		{
			tradeOffFactor = Double.parseDouble(options.get("trade_off_factor"));
		}
		
		//Starting criticality exponent
		double tdPlaceExpFirst = 1.0;
		if(options.get("crit_exp_first") != null)
		{
			tdPlaceExpFirst = Double.parseDouble(options.get("crit_exp_first"));
		}
		
		//Final criticality exponent
		double tdPlaceExpLast = 1.0;
		if(options.get("crit_exp_last") != null)
		{
			tdPlaceExpLast = Double.parseDouble(options.get("crit_exp_last"));
		}
		
		int nbIterationsBeforeRecalculate = 100000;
		if(options.get("nb_iterations_before_recalculate") != null)
		{
			nbIterationsBeforeRecalculate = Integer.parseInt(options.get("nb_iterations_before_recalculate"));
		}
		
		placeLoop(inner_num, tradeOffFactor, tdPlaceExpFirst, tdPlaceExpLast, nbIterationsBeforeRecalculate);
	}
	
	@Override
	public void place(double inner_num)
	{
		System.out.println("Effort level: " + inner_num);
		
		double tradeOffFactor = 0.5;
		double tdPlaceExpFirst = 1.0;
		double tdPlaceExpLast = 8.0;
		int nbIterationsBeforeRecalculate = 100000;
		
		placeLoop(inner_num, tradeOffFactor, tdPlaceExpFirst, tdPlaceExpLast, nbIterationsBeforeRecalculate);
	}
	
	private void placeLoop(double inner_num, double tradeOffFactor, double tdPlaceExpFirst, 
										double tdPlaceExpLast, int nbIterationsBeforeRecalculate)
	{
		//Initialize SA parameters
		calculator.recalculateFromScratch();
		rand = new Random(1);
		Rlimd = Math.max(architecture.getWidth(),architecture.getHeight());
		int Rlim = initialRlim();
		double firstRlim = Rlimd;
		double criticalityExponent = updateCriticalityExponent(firstRlim, tdPlaceExpFirst, tdPlaceExpLast);
		timingGraph.setCriticalityExponent(criticalityExponent);
		timingGraph.recalculateAllSlacksCriticalities();
		previousBBCost = calculator.calculateTotalCost();
		previousTDCost = timingGraph.calculateTotalCost();
		double T = calculateInitialTemperature(tradeOffFactor, nbIterationsBeforeRecalculate);
		int movesPerTemperature = (int) (inner_num*Math.pow(circuit.numBlocks(),4.0/3.0));

		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		while(T > 0.005 / circuit.getNets().values().size())
		{
			timingGraph.setCriticalityExponent(criticalityExponent);
			timingGraph.recalculateAllSlacksCriticalities();
			previousBBCost = calculator.calculateTotalCost();
			previousTDCost = timingGraph.calculateTotalCost();
			
			int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
												(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
				{
					double deltaBBCost = calculator.calculateDeltaCost(swap);
					double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
					
					double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / previousBBCost
										+ (tradeOffFactor * deltaTimingCost) / previousTDCost;
					//double deltaCost = deltaBBCost;
					
					if(deltaCost<=0)
					{
						swap.apply();
						alphaAbs+=1;
						calculator.pushThrough();
						timingGraph.pushThrough();
					}
					else
					{
						if(rand.nextDouble()<Math.exp(-deltaCost/T))
						{
							swap.apply();
							alphaAbs+=1;
							calculator.pushThrough();
							timingGraph.pushThrough();
						}
						else
						{
							calculator.revert();
							timingGraph.revert();
						}
					}
				}
				iterationsToGoBeforeRecalculate--;
				if(iterationsToGoBeforeRecalculate <= 0)
				{
					previousBBCost = calculator.calculateTotalCost();
					previousTDCost = timingGraph.calculateTotalCost();
					iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
				}
			}
			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlim(alpha);
			T=updateTemperature(T,alpha);
			criticalityExponent = updateCriticalityExponent(firstRlim, tdPlaceExpFirst, tdPlaceExpLast);
		}
	}
	
	@Override
	public void lowTempAnneal(double innerNum)
	{
		double tradeOffFactor = 0.5;
		double tdPlaceExpFirst = 1.0;
		double tdPlaceExpLast = 8.0;
		int nbIterationsBeforeRecalculate = 100000;
		
		//Initialize SA parameters
		calculator.recalculateFromScratch();
		rand = new Random(1);
		int biggestDistance = getBiggestDistance();
		//int maxValue = (int)Math.floor(biggestDistance / 3.0);
		int maxValue = (int)Math.floor(biggestDistance / 16.0);
		if(maxValue < 1)
		{
			maxValue = 1;
		}
		Rlimd = maxValue;
		int Rlim = initialRlim();
		double firstRlim = Rlimd;
		double criticalityExponent = updateCriticalityExponent(firstRlim, tdPlaceExpFirst, tdPlaceExpLast);
		timingGraph.setCriticalityExponent(criticalityExponent);
		timingGraph.recalculateAllSlacksCriticalities();
		previousBBCost = calculator.calculateTotalCost();
		previousTDCost = timingGraph.calculateTotalCost();
		double T = calculateInitialTemperatureLow(Rlim, tradeOffFactor);
		int movesPerTemperature = (int) (innerNum*Math.pow(circuit.numBlocks(),4.0/3.0));
		
		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		while(T > 0.005 / circuit.getNets().values().size())
		{
			timingGraph.setCriticalityExponent(criticalityExponent);
			timingGraph.recalculateAllSlacksCriticalities();
			previousBBCost = calculator.calculateTotalCost();
			previousTDCost = timingGraph.calculateTotalCost();
			
			int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
							(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
				{
					double deltaBBCost = calculator.calculateDeltaCost(swap);
					double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
					double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / previousBBCost
										+ (tradeOffFactor * deltaTimingCost) / previousTDCost;
					if(deltaCost<=0)
					{
						swap.apply();
						alphaAbs+=1;
						calculator.pushThrough();
						timingGraph.pushThrough();
					}
					else
					{
						if(rand.nextDouble()<Math.exp(-deltaCost/T))
						{
							swap.apply();
							alphaAbs+=1;
							calculator.pushThrough();
							timingGraph.pushThrough();
						}
						else
						{
							calculator.revert();
							timingGraph.revert();
						}
					}
				}
				iterationsToGoBeforeRecalculate--;
				if(iterationsToGoBeforeRecalculate <= 0)
				{
					previousBBCost = calculator.calculateTotalCost();
					previousTDCost = timingGraph.calculateTotalCost();
					iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
				}
			}
			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlimLimited(alpha, maxValue);
			T=updateTemperature(T,alpha);
			criticalityExponent = updateCriticalityExponent(firstRlim, tdPlaceExpFirst, tdPlaceExpLast);
		}
	}
	
	private double calculateInitialTemperature(double tradeOffFactor, int nbIterationsBeforeRecalculate)
	{
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		
		int iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
		
		for (int i = 0; i < circuit.numBlocks(); i++) 
		{
			int maxFPGAdimension = Math.max(architecture.getWidth(), architecture.getHeight());
			Swap swap = findSwap(maxFPGAdimension);			
			
			//Swap
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
								(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
			{
				double deltaBBCost = calculator.calculateDeltaCost(swap);
				double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
				double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / previousBBCost
									+ (tradeOffFactor * deltaTimingCost) / previousTDCost;
				//double deltaCost = deltaBBCost;
				
				swap.apply();
				calculator.pushThrough();
				timingGraph.pushThrough();
				somDeltaKost+=deltaCost;
				kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
			}
			else
			{
				calculator.revert();
				timingGraph.revert();
			}
			
			iterationsToGoBeforeRecalculate--;
			if(iterationsToGoBeforeRecalculate <= 0)
			{
				previousBBCost = calculator.calculateTotalCost();
				previousTDCost = timingGraph.calculateTotalCost();
				iterationsToGoBeforeRecalculate = nbIterationsBeforeRecalculate;
			}
		}
		double somKwadraten = kwadratischeSomDeltaKost;
		double kwadraatSom = Math.pow(somDeltaKost,2);
		double nbElements = circuit.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		double T=20*stdafwijkingDeltaKost;
		
		return T;
	}
	
	private double calculateInitialTemperatureLow(int Rlim, double tradeOffFactor)
	{
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		for (int i = 0; i < circuit.numBlocks(); i++)
		{
			//Swap swap = findSwapInCircuit();
			Swap swap = findSwap(Rlim);

			//Swap
			if((swap.pl1.getBlock() == null || (!swap.pl1.getBlock().fixed)) && 
									(swap.pl2.getBlock() == null || (!swap.pl2.getBlock().fixed)))
			{
				double deltaBBCost = calculator.calculateDeltaCost(swap);
				double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
				double deltaCost = ((1 - tradeOffFactor) * deltaBBCost) / previousBBCost
						+ (tradeOffFactor * deltaTimingCost) / previousTDCost;
				
				if(deltaCost <= 0)
				{
					swap.apply();
					calculator.pushThrough();
					timingGraph.pushThrough();
					sumNegDeltaCost -= deltaCost;
					quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
					numNegDeltaCost++;
				}
				else
				{
					calculator.revert();
					timingGraph.revert();
				}
			}
			else
			{
				calculator.revert();
				timingGraph.revert();
			}
		}
		
		double somNegKwadraten = quadraticSumNegDeltaCost;
		double negKwadraatSom = Math.pow(sumNegDeltaCost, 2);
		double stdafwijkingNegDeltaKost = Math.sqrt(somNegKwadraten/numNegDeltaCost - negKwadraatSom/(numNegDeltaCost*numNegDeltaCost));
		System.out.println("Negative standard deviation: " + stdafwijkingNegDeltaKost);
		
		double T = 2*stdafwijkingNegDeltaKost;
		
		if(!(T > 0 && T < 10000))
		{
			System.err.println("Trouble with initial low temperature");
			T = 1.0;
		}
		
		return T;
	}
	
	private double updateCriticalityExponent(double firstRlim, double tdPlaceExpFirst, double tdPlaceExpLast)
	{
		double finalRlim = 1.0;
		double inverseDeltaRlim = 1 / (firstRlim - finalRlim);
		return (1 - (Rlimd - finalRlim) * inverseDeltaRlim) * (tdPlaceExpLast - tdPlaceExpFirst) + tdPlaceExpFirst;
	}
	
}
