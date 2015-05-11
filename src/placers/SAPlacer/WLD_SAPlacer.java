package placers.SAPlacer;

import java.util.Random;

import architecture.FourLutSanitized;
import circuit.PackedCircuit;

public class WLD_SAPlacer extends SAPlacer
{

	public WLD_SAPlacer(FourLutSanitized architecture, PackedCircuit circuit)
	{
		super(architecture, circuit);
	}
	
	@Override
	public void place(double inner_num)
	{
		//Initialize SA parameters
		calculator.recalculateFromScratch();
		rand = new Random(1);
		Rlimd = Math.max(architecture.width,architecture.height);
		int Rlim = initialRlim();
		double T = calculateInitialTemperature();
		int movesPerTemperature = (int) (inner_num*Math.pow(circuit.numBlocks(),4.0/3.0));
		
		//Print SA parameters
		System.out.println("Initial temperature: " + T);
		System.out.println("Moves per temperature: " + movesPerTemperature);
		
		//Initialize EfficientBoundingBoxNetCC
		
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
						//calculator.apply(swap);
						swap.apply();
						alphaAbs+=1;
						calculator.pushThrough();
					}
					else
					{
						if(rand.nextDouble()<Math.exp(-deltaCost/T))
						{
							//calculator.apply(swap);
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
	
	private double calculateInitialTemperature()
	{
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		for (int i = 0; i < circuit.numBlocks(); i++) 
		{
			int maxFPGAdimension = Math.max(architecture.width, architecture.height);
			Swap swap = findSwap(maxFPGAdimension);			
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				//calculator.apply(swap);
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
	
	private double calculateInitialTemperatureLow() {
//		int counter = 0;
//		int[] countArray = new int[25];
//		for(int i = 0; i < countArray.length; i++)
//		{
//			countArray[i] = 0;
//		}
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		for (int i = 0; i < circuit.numBlocks(); i++) {
			//Swap swap=manipulator.findSwap(manipulator.maxFPGAdimension());
			Swap swap = findSwapInCircuit();
			
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				//calculator.apply(swap);
				swap.apply();
				calculator.pushThrough();
			}
			else
			{
				calculator.revert();
			}

			if(deltaCost > 0)
			{
//				counter++;
//				if(deltaCost > 100.0)
//				{
//					System.out.println("Delta cost: " + deltaCost);
//				}
			}
			else
			{
//				if(deltaCost < -100.0)
//				{
//					System.out.println("Delta cost: " + deltaCost);
//				}
				sumNegDeltaCost -= deltaCost;
				quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
				numNegDeltaCost++;
			}
			
//			double temporary = (deltaCost + 100) / 10;
//			int temporary2 = (int) temporary;
//			if(temporary2 < 0)
//			{
//				temporary2 = 0;
//			}
//			if(temporary2 >= countArray.length)
//			{
//				temporary2 = countArray.length - 1;
//			}
//			countArray[temporary2] = countArray[temporary2] + 1;
//			if(deltaCost >= 100 && deltaCost <= 110)
//			{
//				System.out.println("YES");
//			}
		}
		
//		System.out.println("Positive costs: " + counter + " times / " + manipulator.numBlocks() + " in total");
//		System.out.printf("... to -90: %d\n", countArray[0]);
//		for(int i = 1; i < countArray.length-1; i++)
//		{
//			System.out.printf("%d - %d: %d\n", (i-10)*10, (i-9)*10,countArray[i]);
//		}
//		System.out.printf("140 to ...: %d\n", countArray[countArray.length - 1]);
		
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
	
}
