package placers.SAPlacer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import architecture.FourLutSanitized;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.Input;
import circuit.Output;
import circuit.PackedCircuit;

public class SAPlacer
{
	
	private double Rlimd;
	private EfficientCostCalculator calculator;
	private PackedCircuit circuit;
	private FourLutSanitized architecture;
	private Random rand;
	
	public SAPlacer(EfficientCostCalculator calculator, FourLutSanitized architecture, PackedCircuit circuit)
	{
		this.calculator = calculator;
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
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
	
	private Swap findSwap(int Rlim)
	{
		Swap swap=new Swap();
		Block b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		swap.pl1 = b.getSite();
		if(b.type==BlockType.CLB)
		{
			swap.pl2 = architecture.randomSite(Rlim, swap.pl1);
		}
		else if(b.type == BlockType.INPUT)
		{
			swap.pl2 = architecture.randomISite(Rlim, swap.pl1);
		}
		else if(b.type == BlockType.OUTPUT)
		{
			swap.pl2 = architecture.randomOSite(Rlim, swap.pl1);
		}
		return swap;
	}
	
	private Swap findSwapInCircuit()
	{
		Swap swap = new Swap();
		Block b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		swap.pl1 = b.getSite();
		if(b.type == BlockType.CLB)
		{
			int nbClbs = circuit.clbs.values().size();
			Clb[] clbs = new Clb[nbClbs];
			circuit.clbs.values().toArray(clbs);
			Clb clbToSwap = clbs[rand.nextInt(nbClbs)];
			swap.pl2 = clbToSwap.getSite();
		}
		else if(b.type == BlockType.INPUT)
		{
			int nbInputs = circuit.getInputs().values().size();
			Input[] inputs = new Input[nbInputs];
			circuit.getInputs().values().toArray(inputs);
			Input inputToSwap = inputs[rand.nextInt(nbInputs)];
			swap.pl2 = inputToSwap.getSite();
		}
		else if(b.type == BlockType.OUTPUT)
		{
			int nbOutputs = circuit.getOutputs().values().size();
			Output[] outputs = new Output[nbOutputs];
			circuit.getOutputs().values().toArray(outputs);
			Output outputToSwap = outputs[rand.nextInt(nbOutputs)];
			swap.pl2 = outputToSwap.getSite();
		}
		//System.out.println("From (" + swap.pl1.x + "," + swap.pl1.y + ") to (" + swap.pl2.x + "," + swap.pl2.y + ")");
		return swap;
	}
	
	private double updateTemperature(double temperature, double alpha)
	{
		double gamma;
		if (alpha > 0.96)     	gamma=0.5;
		else if (alpha > 0.8)	gamma=0.9;
		else if (alpha > 0.15)	gamma=0.95;
		else 					gamma=0.8;
		return temperature*gamma;
	}
	
	private int initialRlim()
	{
		int Rlim=(int)Math.round(Rlimd);
		return Rlim;
	}
	
	private int updateRlim(double alpha)
	{
		Rlimd=Rlimd*(1-0.44+alpha);
		int maxFPGAdimension = Math.max(architecture.height, architecture.width);
		if(Rlimd>maxFPGAdimension)
		{
			Rlimd=maxFPGAdimension;
		}
		if(Rlimd<1)
		{
			Rlimd=1;
		}
		return  (int) Math.round(Rlimd);
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
	
	private int getBiggestDistance()
	{
		Collection<Clb> clbs = circuit.clbs.values();
		Iterator<Clb> clbIterator = clbs.iterator();
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		while(clbIterator.hasNext())
		{
			Clb curClb = clbIterator.next();
			int curX = curClb.getSite().x;
			int curY = curClb.getSite().y;
			if(curX > maxX)
			{
				maxX = curX;
			}
			if(curX < minX)
			{
				minX = curX;
			}
			if(curY > maxY)
			{
				maxY = curY;
			}
			if(curY < minY)
			{
				minY = curY;
			}
		}
		int maxDistance;
		if(maxX - minX > maxY - minY)
		{
			maxDistance = maxX - minX;
		}
		else
		{
			maxDistance = maxY - minY;
		}
		return maxDistance;
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
