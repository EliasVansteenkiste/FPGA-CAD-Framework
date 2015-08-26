package placers.SAPlacer;

import java.util.HashMap;
import java.util.Random;

import timinganalysis.TimingGraph;
import architecture.Architecture;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public class TD_SAPlacer extends SAPlacer
{
	
	static {
		defaultOptions.put("inner_num", "1");
	}
	
	private static final double TRADE_OFF_FACTOR = 0.5;
	private static final double TD_PLACE_EXP_FIRST = 1.0;
	private static final double TD_PLACE_EXP_LAST = 8.0;
	private static final int NB_ITERATIONS_BEFORE_RECALCULATE = 100000;
	
	private TimingGraph timingGraph;
	private double previousBBCost;
	private double previousTDCost;

	public TD_SAPlacer(Architecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit, HashMap<String, String> options)
	{
		super(architecture, circuit, options);
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
	}
	
	@Override
	public void place()
	{
		double inner_num = Double.parseDouble(options.get("inner_num"));
		System.out.println("Effort level: " + inner_num);
		
		//Initialize SA parameters
		calculator.recalculateFromScratch();
		rand = new Random(1);
		Rlimd = Math.max(architecture.getWidth(),architecture.getHeight());
		int Rlim = initialRlim();
		double firstRlim = Rlimd;
		double criticalityExponent = updateCriticalityExponent(firstRlim);
		timingGraph.setCriticalityExponent(criticalityExponent);
		timingGraph.recalculateAllSlacksCriticalities();
		previousBBCost = calculator.calculateTotalCost();
		previousTDCost = timingGraph.calculateTotalCost();
		double T = calculateInitialTemperature();
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
			
			int iterationsToGoBeforeRecalculate = NB_ITERATIONS_BEFORE_RECALCULATE;
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaBBCost = calculator.calculateDeltaCost(swap);
					double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
					
					double deltaCost = ((1 - TRADE_OFF_FACTOR) * deltaBBCost) / previousBBCost
										+ (TRADE_OFF_FACTOR * deltaTimingCost) / previousTDCost;
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
					iterationsToGoBeforeRecalculate = NB_ITERATIONS_BEFORE_RECALCULATE;
				}
			}
			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlim(alpha);
			T=updateTemperature(T,alpha);
			criticalityExponent = updateCriticalityExponent(firstRlim);
		}
	}
	
	@Override
	public void lowTempAnneal(double innerNum)
	{
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
		double criticalityExponent = updateCriticalityExponent(firstRlim);
		timingGraph.setCriticalityExponent(criticalityExponent);
		timingGraph.recalculateAllSlacksCriticalities();
		previousBBCost = calculator.calculateTotalCost();
		previousTDCost = timingGraph.calculateTotalCost();
		double T = calculateInitialTemperatureLow(Rlim);
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
			
			int iterationsToGoBeforeRecalculate = NB_ITERATIONS_BEFORE_RECALCULATE;
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaBBCost = calculator.calculateDeltaCost(swap);
					double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
					double deltaCost = ((1 - TRADE_OFF_FACTOR) * deltaBBCost) / previousBBCost
										+ (TRADE_OFF_FACTOR * deltaTimingCost) / previousTDCost;
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
					iterationsToGoBeforeRecalculate = NB_ITERATIONS_BEFORE_RECALCULATE;
				}
			}
			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlimLimited(alpha, maxValue);
			T=updateTemperature(T,alpha);
			criticalityExponent = updateCriticalityExponent(firstRlim);
		}
	}
	
	private double calculateInitialTemperature()
	{
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		
		int iterationsToGoBeforeRecalculate = NB_ITERATIONS_BEFORE_RECALCULATE;
		
		for (int i = 0; i < circuit.numBlocks(); i++) 
		{
			int maxFPGAdimension = Math.max(architecture.getWidth(), architecture.getHeight());
			Swap swap = findSwap(maxFPGAdimension);			
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				double deltaBBCost = calculator.calculateDeltaCost(swap);
				double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
				double deltaCost = ((1 - TRADE_OFF_FACTOR) * deltaBBCost) / previousBBCost
									+ (TRADE_OFF_FACTOR * deltaTimingCost) / previousTDCost;
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
				iterationsToGoBeforeRecalculate = NB_ITERATIONS_BEFORE_RECALCULATE;
			}
		}
		double somKwadraten = kwadratischeSomDeltaKost;
		double kwadraatSom = Math.pow(somDeltaKost,2);
		double nbElements = circuit.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		double T=20*stdafwijkingDeltaKost;
		
		return T;
	}
	
	private double calculateInitialTemperatureLow(int Rlim)
	{
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		for (int i = 0; i < circuit.numBlocks(); i++)
		{
			//Swap swap = findSwapInCircuit();
			Swap swap = findSwap(Rlim);

			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				double deltaBBCost = calculator.calculateDeltaCost(swap);
				double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
				double deltaCost = ((1 - TRADE_OFF_FACTOR) * deltaBBCost) / previousBBCost
						+ (TRADE_OFF_FACTOR * deltaTimingCost) / previousTDCost;
				
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
	
	private double updateCriticalityExponent(double firstRlim)
	{
		double finalRlim = 1.0;
		double inverseDeltaRlim = 1 / (firstRlim - finalRlim);
		return (1 - (Rlimd - finalRlim) * inverseDeltaRlim) * (TD_PLACE_EXP_LAST - TD_PLACE_EXP_FIRST) + TD_PLACE_EXP_FIRST;
	}
	
}
