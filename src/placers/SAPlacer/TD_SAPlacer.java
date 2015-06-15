package placers.SAPlacer;

import java.util.Random;

import timinganalysis.TimingGraph;
import architecture.HeterogeneousArchitecture;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public class TD_SAPlacer extends SAPlacer
{
	
	private static final double TRADE_OFF_FACTOR = 0.5;
	private static final double TD_PLACE_EXP_FIRST = 1.0;
	private static final double TD_PLACE_EXP_LAST = 8.0;
	
	private TimingGraph timingGraph;
	private double inversePreviousBBCost;
	private double inversePreviousTDCost;

	public TD_SAPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
	{
		super(architecture, circuit);
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
	}
	
	@Override
	public void place(double inner_num)
	{
		//Initialize SA parameters
		calculator.recalculateFromScratch();
		rand = new Random(1);
		Rlimd = Math.max(architecture.getWidth(),architecture.getHeight());
		int Rlim = initialRlim();
		double firstRlim = Rlimd;
		double criticalityExponent = updateCriticalityExponent(firstRlim);
		timingGraph.setCriticalityExponent(criticalityExponent);
		timingGraph.recalculateAllSlacksCriticalities();
		inversePreviousBBCost = 1 / calculator.calculateTotalCost();
		inversePreviousTDCost = 1 / timingGraph.calculateTotalCost();
		double T = calculateInitialTemperature();
		int movesPerTemperature = (int) (inner_num*Math.pow(circuit.numBlocks(),4.0/3.0));

		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		while(T > 0.005 / circuit.getNets().values().size())
		{
			timingGraph.setCriticalityExponent(criticalityExponent);
			timingGraph.recalculateAllSlacksCriticalities();
			inversePreviousBBCost = 1 / calculator.calculateTotalCost();
			inversePreviousTDCost = 1 / timingGraph.calculateTotalCost();
			
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaBBCost = calculator.calculateDeltaCost(swap);
					double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
					double deltaCost = (1 - TRADE_OFF_FACTOR) * deltaBBCost * inversePreviousBBCost
										+ TRADE_OFF_FACTOR * deltaTimingCost * inversePreviousTDCost;
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
		Rlimd = biggestDistance / 3;
		if(Rlimd == 0)
		{
			Rlimd = 1;
		}
		int Rlim = initialRlim();
		double firstRlim = Rlimd;
		double criticalityExponent = updateCriticalityExponent(firstRlim);
		timingGraph.setCriticalityExponent(criticalityExponent);
		timingGraph.recalculateAllSlacksCriticalities();
		inversePreviousBBCost = 1 / calculator.calculateTotalCost();
		inversePreviousTDCost = 1 / timingGraph.calculateTotalCost();
		double T = calculateInitialTemperatureLow();
		int movesPerTemperature = (int) (innerNum*Math.pow(circuit.numBlocks(),4.0/3.0));
		
		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		while(T > 0.005 / circuit.getNets().values().size())
		{
			timingGraph.setCriticalityExponent(criticalityExponent);
			timingGraph.recalculateAllSlacksCriticalities();
			inversePreviousBBCost = 1 / calculator.calculateTotalCost();
			inversePreviousTDCost = 1 / timingGraph.calculateTotalCost();
			
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaBBCost = calculator.calculateDeltaCost(swap);
					double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
					double deltaCost = (1 - TRADE_OFF_FACTOR) * deltaBBCost * inversePreviousBBCost
										+ TRADE_OFF_FACTOR * deltaTimingCost * inversePreviousTDCost;
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
			}
			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlim(alpha);
			T=updateTemperature(T,alpha);
			criticalityExponent = updateCriticalityExponent(firstRlim);
		}
	}
	
	private double calculateInitialTemperature()
	{
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		for (int i = 0; i < circuit.numBlocks(); i++) 
		{
			int maxFPGAdimension = Math.max(architecture.getWidth(), architecture.getHeight());
			Swap swap = findSwap(maxFPGAdimension);			
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				double deltaBBCost = calculator.calculateDeltaCost(swap);
				double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
				double deltaCost = (1 - TRADE_OFF_FACTOR) * deltaBBCost * inversePreviousBBCost
									+ TRADE_OFF_FACTOR * deltaTimingCost * inversePreviousTDCost;
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
		}
		double somKwadraten = kwadratischeSomDeltaKost;
		double kwadraatSom = Math.pow(somDeltaKost,2);
		double nbElements = circuit.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		double T=20*stdafwijkingDeltaKost;
		
		return T;
	}
	
	private double calculateInitialTemperatureLow()
	{
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		for (int i = 0; i < circuit.numBlocks(); i++)
		{
			Swap swap = findSwapInCircuit();			

			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				double deltaBBCost = calculator.calculateDeltaCost(swap);
				double deltaTimingCost = timingGraph.calculateDeltaCost(swap);
				double deltaCost = (1 - TRADE_OFF_FACTOR) * deltaBBCost * inversePreviousBBCost
						+ TRADE_OFF_FACTOR * deltaTimingCost * inversePreviousTDCost;
				swap.apply();
				calculator.pushThrough();
				timingGraph.pushThrough();
				if(deltaCost < 0)
				{
					sumNegDeltaCost -= deltaCost;
					quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
					numNegDeltaCost++;
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
