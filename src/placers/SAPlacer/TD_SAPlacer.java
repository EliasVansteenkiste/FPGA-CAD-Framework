package placers.SAPlacer;

import java.util.Random;

import timinganalysis.TimingGraph;
import architecture.FourLutSanitized;
import circuit.PackedCircuit;

public class TD_SAPlacer extends SAPlacer
{
	
	private static final double TRADE_OFF_FACTOR = 0.5;
	
	private TimingGraph timingGraph;
	private double inversePreviousBBCost;
	private double inversePreviousTDCost;

	public TD_SAPlacer(EfficientCostCalculator calculator, FourLutSanitized architecture, PackedCircuit circuit, TimingGraph timingGraph)
	{
		super(calculator, architecture, circuit);
		this.timingGraph = timingGraph;
		this.timingGraph.buildTimingGraph();
	}
	
	@Override
	public void place(double inner_num)
	{
		//Initialize SA parameters
		calculator.recalculateFromScratch();
		rand = new Random(1);
		Rlimd = Math.max(architecture.width,architecture.height);
		int Rlim = initialRlim();
		double criticalityExponent = 9.0;
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
			criticalityExponent = updateCriticalityExponent();
		}
	}
	
	private double calculateInitialTemperature()
	{
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		for (int i = 0; i < circuit.numBlocks(); i++) 
		{
			int maxFPGAdimension = Math.max(architecture.width, architecture.height);
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
	
}
