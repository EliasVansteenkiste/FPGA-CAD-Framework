package placers;
import java.util.Random;

import com.sun.org.apache.bcel.internal.generic.SWAP;


public class Vplace {
	private double Rlimd;
	PlacementManipulator manipulator;
	CostCalculator calculator;
	
	public Vplace(PlacementManipulator manipulator, CostCalculator calculator) {
		this.manipulator=manipulator;
		this.calculator=calculator;
	}

	public void place(double inner_num) {

		Random rand= new Random(1);
		
		Rlimd=manipulator.maxFPGAdimension();
		int Rlim = initialRlim();

		double T = calculateInitialTemperature();
		System.out.println("Initial temperature: "+T);
		
		int movesPerTemperature=(int) (inner_num*Math.pow(manipulator.numBlocks(),4.0/3.0));
		System.out.println("Moves per temperature: "+movesPerTemperature);
		//generate array with random moves here
		long timeSpendFindSwap = 0;
		long timeSpendCalcDCAndApplySwap = 0;
		
		while (T>0.005*calculator.averageNetCost()) {
			int alphaAbs=0;
//			System.out.println("Temperature: "+T+", Total Cost: "+calculator.calculateTotalCost());
			for (int i =0; i<movesPerTemperature;i++) {
				Swap swap;
				
				final long startTime1 = System.nanoTime();
				final long endTime1;
				try {
					swap=manipulator.findSwap(Rlim);
				} finally {
				  endTime1 = System.nanoTime();
				}
				final long duration1 = endTime1 - startTime1;
				timeSpendFindSwap+=duration1;
				
				
				
				final long startTime2 = System.nanoTime();
      			final long endTime2;
      			
				try 
				{
					if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
					{
						double deltaCost = calculator.calculateDeltaCost(swap);
	
						if(deltaCost<=0)
						{
							calculator.apply(swap);
							alphaAbs+=1;
						}
						else
						{
							if(rand.nextDouble()<Math.exp(-deltaCost/T))
							{
								calculator.apply(swap);
								alphaAbs+=1;
							}
						}
					}
				}
				finally 
				{
				  endTime2 = System.nanoTime();
				}
				final long duration2 = endTime2 - startTime2;
				timeSpendCalcDCAndApplySwap+=duration2;
				

			}

			double alpha = (double)alphaAbs/movesPerTemperature;
		//	System.out.println("alphaAbs = "+alphaAbs);
			Rlim = updateRlim(alpha);
			T=updateTemperature(T,alpha);
		//	System.out.println("Current temperature : "+ T);
		
		}
	//	System.out.println("timeSpendFindSwap: "+(timeSpendFindSwap/1.0E9));
	//	System.out.println("timeSpendCalcDCAndApplySwap: "+(timeSpendCalcDCAndApplySwap/1.0E9));
		System.out.println("Last temp: " + T);
	}
	
	public void lowTempAnneal(double initialTemp, int initialRLim, int nbMovesPerTemp)
	{
		Random rand= new Random(1);
		int Rlim = initialRLim;
		double T = initialTemp;
		int movesPerTemperature = nbMovesPerTemp;
		
		while (T>0.005*calculator.averageNetCost()) {
			int alphaAbs=0;
//			System.out.println("Temperature: "+T+", Total Cost: "+calculator.calculateTotalCost());
			for (int i =0; i<movesPerTemperature;i++) {
				Swap swap;
				
				try
				{
					swap=manipulator.findSwap(Rlim);
				}
				finally
				{

				}
      			
				try 
				{
					if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
					{
						double deltaCost = calculator.calculateDeltaCost(swap);
	
						if(deltaCost<=0)
						{
							calculator.apply(swap);
							alphaAbs+=1;
						}
						else
						{
							if(rand.nextDouble()<Math.exp(-deltaCost/T))
							{
								calculator.apply(swap);
								alphaAbs+=1;
							}
						}
					}
				}
				finally 
				{
				 
				}
			}

			double alpha = (double)alphaAbs/movesPerTemperature;
			Rlim = updateRlim(alpha);
			T=updateTemperature(T,alpha);
		}
		
		System.out.println("Last temp: " + T);
	}

	private double updateTemperature(double temperature, double alpha) {
		double gamma;
		if (alpha > 0.96)     	gamma=0.5;
		else if (alpha > 0.8)	gamma=0.9;
		else if (alpha > 0.15)	gamma=0.95;
		else 					gamma=0.8;
		return temperature*gamma;
	}

	private int initialRlim() {
		int Rlim=(int)Math.round(Rlimd);
		return Rlim;
	}

	private int updateRlim(double alpha) {
		Rlimd=Rlimd*(1-0.44+alpha);
		if (Rlimd>manipulator.maxFPGAdimension()) Rlimd=manipulator.maxFPGAdimension();
		if (Rlimd<1) Rlimd=1;
		return  (int) Math.round(Rlimd);
	}

	public double calculateInitialTemperature() {
		double	somDeltaKost=0;
		double 	kwadratischeSomDeltaKost=0;
		int counter = 0;
		int[] countArray = new int[25];
		for(int i = 0; i < countArray.length; i++)
		{
			countArray[i] = 0;
		}
		double sumNegDeltaCost = 0.0;
		int numNegDeltaCost = 0;
		double quadraticSumNegDeltaCost = 0.0;
		for (int i=0;i<manipulator.numBlocks();i++) {
			//Swap swap=manipulator.findSwap(manipulator.maxFPGAdimension());
			Swap swap = manipulator.findSwapInCircuit();
			
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				calculator.apply(swap);
			}

			if(deltaCost > 0)
			{
				counter++;
				if(deltaCost > 100.0)
				{
					System.out.println("Delta cost: " + deltaCost);
				}
			}
			else
			{
				if(deltaCost < -100.0)
				{
					System.out.println("Delta cost: " + deltaCost);
				}
				sumNegDeltaCost -= deltaCost;
				quadraticSumNegDeltaCost += Math.pow(deltaCost, 2);
				numNegDeltaCost++;
			}
			
			somDeltaKost+=deltaCost;
			kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
			
			double temp = (deltaCost + 100) / 10;
			int temp2 = (int) temp;
			if(temp2 < 0)
			{
				temp2 = 0;
			}
			if(temp2 >= countArray.length)
			{
				temp2 = countArray.length - 1;
			}
			countArray[temp2] = countArray[temp2] + 1;
			if(deltaCost >= 100 && deltaCost <= 110)
			{
				System.out.println("YES");
			}
		}
		System.out.println("Positive costs: " + counter + " times / " + manipulator.numBlocks() + " in total");
		System.out.println("Som delta kost: " + somDeltaKost);
		System.out.println("Som kwadraten: "  + kwadratischeSomDeltaKost);
		double somKwadraten = kwadratischeSomDeltaKost;
		System.out.println("Som v/d kwadraten: " + somKwadraten);
		double kwadraatSom = Math.pow(somDeltaKost,2);
		System.out.println("Kwadraat v/d som: " + kwadraatSom);
		double nbElements = manipulator.numBlocks();
		double stdafwijkingDeltaKost=Math.sqrt(Math.abs(somKwadraten/nbElements-kwadraatSom/(nbElements*nbElements)));
		System.out.println("Standaard afwijking: " + stdafwijkingDeltaKost);
		double T=20*stdafwijkingDeltaKost;
		
		System.out.printf("... to -90: %d\n", countArray[0]);
		for(int i = 1; i < countArray.length-1; i++)
		{
			System.out.printf("%d - %d: %d\n", (i-10)*10, (i-9)*10,countArray[i]);
		}
		System.out.printf("140 to ...: %d\n", countArray[countArray.length - 1]);
		
		double somNegKwadraten = quadraticSumNegDeltaCost;
		double negKwadraatSom = Math.pow(sumNegDeltaCost, 2);
		double stdafwijkingNegDeltaKost = Math.sqrt(somNegKwadraten/numNegDeltaCost - negKwadraatSom/(numNegDeltaCost*numNegDeltaCost));
		System.out.println("Negative standard deviation: " + stdafwijkingNegDeltaKost);
		
		double newT = 20*stdafwijkingNegDeltaKost;
		
		return T;
		//return newT;
	}


}
