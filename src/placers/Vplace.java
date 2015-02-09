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
      			
				try {
					double deltaCost = calculator.calculateDeltaCost(swap);

					if(deltaCost<=0){
						calculator.apply(swap);
						alphaAbs+=1;
					}else{
						if(rand.nextDouble()<Math.exp(-deltaCost/T)) {
							calculator.apply(swap);
							alphaAbs+=1;
						}
					}
			} finally {
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
		for (int i=0;i<manipulator.numBlocks();i++) {
			Swap swap=manipulator.findSwap(manipulator.maxFPGAdimension());
			
			double deltaCost = calculator.calculateDeltaCost(swap);
			
			//Swap
			calculator.apply(swap);

			somDeltaKost+=deltaCost;
			kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
		}
		double stdafwijkingDeltaKost=Math.sqrt(kwadratischeSomDeltaKost/manipulator.numBlocks()-Math.pow(somDeltaKost/manipulator.numBlocks(),2));
		double T=20*stdafwijkingDeltaKost;
		return T;
	}


}
