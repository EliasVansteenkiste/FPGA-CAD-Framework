package placers.SAPlacer;

import java.util.HashMap;
import java.util.Random;

import architecture.Architecture;
import circuit.PackedCircuit;

public class WLD_SAPlacer extends SAPlacer
{

	public WLD_SAPlacer(Architecture architecture, PackedCircuit circuit, HashMap<String, String> options)
	{
		super(architecture, circuit, options);
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
		double T = calculateInitialTemperature();
		int movesPerTemperature = (int) (inner_num*Math.pow(circuit.numBlocks(),4.0/3.0));
		
		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		int tNumber = 0;
		
		//Do placement
		while (T > 0.005*calculator.calculateAverageNetCost())
		{
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) 
			{
				Swap swap = findSwap(Rlim);
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaCost = calculator.calculateDeltaCost(swap);
					if(deltaCost<=0)
					{
						swap.apply();
						alphaAbs+=1;
						calculator.pushThrough();
					}
					else
					{
						if(rand.nextDouble()<Math.exp(-deltaCost/T))
						{
							swap.apply();
							alphaAbs+=1;
							calculator.pushThrough();
						}
						else
						{
							calculator.revert();
						}
					}
				}
			}

			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlim(alpha);
			T=updateTemperature(T,alpha);
			
			System.out.println("Temperature " + tNumber +" = " + T + ", cost = " + calculator.calculateTotalCost() + ", Rlim = " + Rlim);
			tNumber++;
		}
		
		System.out.println("Last temp: " + T);
	}
	

	
	@Override
	public void lowTempAnneal(double innerNum)
	{
		calculator.recalculateFromScratch();
		rand= new Random(1);
		int biggestDistance = getBiggestDistance();
		//int maxValue = (int)Math.floor(biggestDistance / 3.0);
		int maxValue = (int)Math.floor(biggestDistance / 16.0);
		if(maxValue < 1)
		{
			maxValue = 1;
		}
		Rlimd = maxValue;
		int Rlim = initialRlim();
		double T = calculateInitialTemperatureLow(Rlim);
		int movesPerTemperature=(int) (innerNum*Math.pow(circuit.numBlocks(),4.0/3.0));
		
		System.out.println("Initial temp = " + T + ", initial Rlim = " + Rlim);
		
		while (T>0.005*calculator.calculateAverageNetCost()) {
			int alphaAbs=0;
			for (int i =0; i<movesPerTemperature;i++) {
				Swap swap;
				swap = findSwap(Rlim);
      			
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaCost = calculator.calculateDeltaCost(swap);
					
					if(deltaCost<=0)
					{
						swap.apply();
						alphaAbs+=1;
						calculator.pushThrough();
					}
					else
					{
						if(rand.nextDouble()<Math.exp(-deltaCost/T))
						{
							swap.apply();
							alphaAbs+=1;
							calculator.pushThrough();
						}
						else
						{
							calculator.revert();
						}
					}
				}
			}

			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlimLimited(alpha, maxValue);
			T=updateTemperature(T,alpha);
		}
	}
	
	public void lowTempAnnealParametrized(double innerNum, double tempDivisionFactor, double maxValueDivisionFactor)
	{
		calculator.recalculateFromScratch();
		rand = new Random(1);
		int biggestDistance = getBiggestDistance();
		int maxValue = (int)Math.floor(biggestDistance / maxValueDivisionFactor);
		if(maxValue < 1)
		{
			maxValue = 1;
		}
		Rlimd = maxValue;
		int Rlim = initialRlim();
		double T = calculateInitialTemperatureLowDivided(tempDivisionFactor);
		int movesPerTemperature = (int) (innerNum*Math.pow(circuit.numBlocks(),4.0/3.0));
		
		while (T > 0.005 * calculator.calculateAverageNetCost()) {
			int alphaAbs = 0;
			for (int i = 0; i < movesPerTemperature; i++) {
				Swap swap;
				swap = findSwap(Rlim);
      			
				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
				{
					double deltaCost = calculator.calculateDeltaCost(swap);
					
					if(deltaCost <= 0)
					{
						swap.apply();
						alphaAbs+=1;
						calculator.pushThrough();
					}
					else
					{
						if(rand.nextDouble() < Math.exp(-deltaCost/T))
						{
							swap.apply();
							alphaAbs+=1;
							calculator.pushThrough();
						}
						else
						{
							calculator.revert();
						}
					}
				}
			}

			double alpha = (double) alphaAbs / movesPerTemperature;
			Rlim = updateRlimLimited(alpha, maxValue);
			T = updateTemperature(T,alpha);
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
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				swap.apply();
				calculator.pushThrough();
			}
			else
			{
				calculator.revert();
			}
			
			somDeltaKost+=deltaCost;
			kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
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
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				if(deltaCost <= 0)
				{
					swap.apply();
					calculator.pushThrough();
					sumNegDeltaCost -= deltaCost;
					quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
					numNegDeltaCost++;
				}
				else
				{
					calculator.revert();
				}
			}
			else
			{
				calculator.revert();
			}
		}
		
		double somNegKwadraten = quadraticSumNegDeltaCost;
		double negKwadraatSom = Math.pow(sumNegDeltaCost, 2);
		double stdafwijkingNegDeltaKost = Math.sqrt(somNegKwadraten/numNegDeltaCost - negKwadraatSom/(numNegDeltaCost*numNegDeltaCost));
		
		double T = 2*stdafwijkingNegDeltaKost;
		
		if(!(T > 0 && T < 10000))
		{
			System.out.println("Trouble");
			T = 1.0;
		}
		
		return T;
	}
	
	private double calculateInitialTemperatureLowDivided(double divisionFactor)
	{
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		for (int i = 0; i < circuit.numBlocks(); i++)
		{
			Swap swap = findSwapInCircuit();
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				if(deltaCost <= 0)
				{
					swap.apply();
					calculator.pushThrough();
					sumNegDeltaCost -= deltaCost;
					quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
					numNegDeltaCost++;
				}
				else
				{
					calculator.revert();
				}
			}
			else
			{
				calculator.revert();
			}
		}
		
		double somNegKwadraten = quadraticSumNegDeltaCost;
		double negKwadraatSom = Math.pow(sumNegDeltaCost, 2);
		double stdafwijkingNegDeltaKost = Math.sqrt(somNegKwadraten/numNegDeltaCost - negKwadraatSom/(numNegDeltaCost*numNegDeltaCost));
		
		double T = (2 * stdafwijkingNegDeltaKost) / divisionFactor;
		
		return T;
	}
}