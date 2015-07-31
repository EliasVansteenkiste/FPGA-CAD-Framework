package placers.SAPlacer;

import java.util.Random;

import architecture.HeterogeneousArchitecture;
import architecture.SiteType;
import circuit.PackedCircuit;

public class WLD_SAPlacer extends SAPlacer
{

	public WLD_SAPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit)
	{
		super(architecture, circuit);
	}
	
	@Override
	public void place(double inner_num)
	{
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
		}
		
		System.out.println("Last temp: " + T);
	}
	
//	public void placePackedIO(double inner_num)
//	{
//		//Initialize SA parameters
//		calculator.recalculateFromScratch();
//		rand = new Random(1);
//		Rlimd = Math.max(architecture.getWidth(),architecture.getHeight());
//		int Rlim = initialRlim();
//		double T = calculateInitialTemperaturePackedIO();
//		int movesPerTemperature = (int) (inner_num*Math.pow(circuit.numBlocks(),4.0/3.0));
//		
//		//Print SA parameters
//		System.out.println("Initial temperature: " + T);
//		System.out.println("Moves per temperature: " + movesPerTemperature);
//		
//		//Do placement
//		while (T > 0.005*calculator.calculateAverageNetCost())
//		{
//			int alphaAbs=0;
//			for (int i =0; i<movesPerTemperature;i++) 
//			{
//				Swap swap = findSwap(Rlim);
//				if(swap.pl1.type != SiteType.IO)
//				{
//					if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
//					{
//						double deltaCost = calculator.calculateDeltaCost(swap);
//						if(deltaCost<=0)
//						{
//							swap.apply();
//							alphaAbs+=1;
//							calculator.pushThrough();
//						}
//						else
//						{
//							if(rand.nextDouble()<Math.exp(-deltaCost/T))
//							{
//								swap.apply();
//								alphaAbs+=1;
//								calculator.pushThrough();
//							}
//							else
//							{
//								calculator.revert();
//							}
//						}
//					}
//				}
//				else //We are swapping IOs: swap'em both
//				{
//					int n = swap.pl1.n;
//					Swap swap2;
//					if(n == 0)
//					{
//						swap2 = new Swap();
//						swap2.pl1 = architecture.getSite(swap.pl1.x, swap.pl1.y, 1);
//						swap2.pl2 = architecture.getSite(swap.pl2.x, swap.pl2.y, 1);
//					}
//					else
//					{
//						swap2 = new Swap();
//						swap2.pl1 = architecture.getSite(swap.pl1.x, swap.pl1.y, 0);
//						swap2.pl2 = architecture.getSite(swap.pl2.x, swap.pl2.y, 0);
//					}
//					double deltaCost1 = calculator.calculateDeltaCost(swap);
//					calculator.revert();
//					double deltaCost2 = calculator.calculateDeltaCost(swap2);
//					calculator.revert();
//					double deltaCost = deltaCost1 + deltaCost2;
//					
//					if(deltaCost<=0)
//					{
//						calculator.calculateDeltaCost(swap);
//						calculator.pushThrough();
//						calculator.calculateDeltaCost(swap2);
//						calculator.pushThrough();
//						swap.apply();
//						swap2.apply();
//						alphaAbs+=1;
//						
//					}
//					else
//					{
//						if(rand.nextDouble()<Math.exp(-deltaCost/T))
//						{
//							calculator.calculateDeltaCost(swap);
//							calculator.pushThrough();
//							calculator.calculateDeltaCost(swap2);
//							calculator.pushThrough();
//							swap.apply();
//							swap2.apply();
//							alphaAbs+=1;
//						}
//					}
//				}
//			}
//
//			double alpha = (double)alphaAbs/movesPerTemperature;
//			Rlim = updateRlim(alpha);
//			T=updateTemperature(T,alpha);		
//		}
//		
//		System.out.println("Last temp: " + T);
//	}
	
	@Override
	public void lowTempAnneal(double innerNum)
	{
		calculator.recalculateFromScratch();
		rand= new Random(1);
		int biggestDistance = getBiggestDistance();
		Rlimd = biggestDistance / 3;
		if(Rlimd == 0)
		{
			Rlimd = 1;
		}
		int Rlim = initialRlim();
		double T = calculateInitialTemperatureLow();
		System.out.println("Initial temperature: "+T);
		int movesPerTemperature=(int) (innerNum*Math.pow(circuit.numBlocks(),4.0/3.0));
		System.out.println("Moves per temperature: "+movesPerTemperature);
		
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
			Rlim = updateRlim(alpha);
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
	
//	private double calculateInitialTemperaturePackedIO()
//	{
//		double	somDeltaKost=0;
//		double 	kwadratischeSomDeltaKost=0;
//		for (int i = 0; i < circuit.numBlocks(); i++) 
//		{
//			int maxFPGAdimension = Math.max(architecture.getWidth(), architecture.getHeight());
//			
//			Swap swap = findSwap(maxFPGAdimension);	
//			
//			double deltaCost;
//			if(swap.pl1.type != SiteType.IO)
//			{
//				deltaCost = calculator.calculateDeltaCost(swap);
//				//Swap
//				if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
//				{
//					swap.apply();
//					calculator.pushThrough();
//				}
//				else
//				{
//					calculator.revert();
//				}
//			}
//			else //swapping IOs
//			{
//				int n = swap.pl1.n;
//				Swap swap2;
//				if(n == 0)
//				{
//					swap2 = new Swap();
//					swap2.pl1 = architecture.getSite(swap.pl1.x, swap.pl1.y, 1);
//					swap2.pl2 = architecture.getSite(swap.pl2.x, swap.pl2.y, 1);
//				}
//				else
//				{
//					swap2 = new Swap();
//					swap2.pl1 = architecture.getSite(swap.pl1.x, swap.pl1.y, 0);
//					swap2.pl2 = architecture.getSite(swap.pl2.x, swap.pl2.y, 0);
//				}
//				double deltaCost1 = calculator.calculateDeltaCost(swap);
//				calculator.pushThrough();
//				double deltaCost2 = calculator.calculateDeltaCost(swap2);
//				calculator.pushThrough();
//				deltaCost = deltaCost1 + deltaCost2;
//			}
//			
//			somDeltaKost+=deltaCost;
//			kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
//		}
//		double somKwadraten = kwadratischeSomDeltaKost;
//		double kwadraatSom = Math.pow(somDeltaKost,2);
//		double nbElements = circuit.numBlocks();
//		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
//		double T=20*stdafwijkingDeltaKost;
//		
//		return T;
//	}
	
	private double calculateInitialTemperatureLow()
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
				swap.apply();
				calculator.pushThrough();
			}
			else
			{
				calculator.revert();
			}

			if(deltaCost <= 0)
			{
				sumNegDeltaCost -= deltaCost;
				quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
				numNegDeltaCost++;
			}
		}
		
		double somNegKwadraten = quadraticSumNegDeltaCost;
		double negKwadraatSom = Math.pow(sumNegDeltaCost, 2);
		double stdafwijkingNegDeltaKost = Math.sqrt(somNegKwadraten/numNegDeltaCost - negKwadraatSom/(numNegDeltaCost*numNegDeltaCost));
		System.out.println("Negative standard deviation: " + stdafwijkingNegDeltaKost);
		
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