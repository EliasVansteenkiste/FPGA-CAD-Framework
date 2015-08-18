package placers.old;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import placers.SAPlacer.Swap;

public class pplacer {
	private double Rlimd;
	PlacementManipulator manipulator;
	CostCalculator calculator;
	
	public pplacer(PlacementManipulator manipulator, CostCalculator calculator) {
		this.manipulator=manipulator;
		this.calculator=calculator;
		
	}

	/**
	 * @param inner_num
	 */
	public void place(double inner_num) {

		
		
		Rlimd=manipulator.maxFPGAdimension();
		int Rlim = initialRlim();

		double T = calculateInitialTemperature();
		System.out.println("Initial temperature: "+T);
		
		int movesPerTemperature=(int) (inner_num*Math.pow(manipulator.numBlocks(),4.0/3.0));
		System.out.println("Moves per temperature: "+movesPerTemperature);
		//generate array with random moves here
		
		//long timeSpendFindSwap = 0;
		//long timeSpendCalcDCAndApplySwap = 0;
		
		
		
		while (T>0.005*calculator.calculateAverageNetCost()) {
			//GENERATION OF RANDOM SWAPS
			final HashSet<Swap> randommoves = new HashSet<Swap>();// Creating array list
			for (int i =1; i<movesPerTemperature;i++) {
				Swap swap = manipulator.findSwap(Rlim);
				randommoves.add(swap);
			}
			// GENERATE ARRAYS FOR SAVING THE RANDOM GENERATED SWAPS
			 ArrayList<Swap> a0 = new ArrayList<Swap>();
			 ArrayList<Swap> a1 = new ArrayList<Swap>();
			 ArrayList<Swap> a2 = new ArrayList<Swap>();
		//	 ArrayList<Swap> a3 = new ArrayList<Swap>();
		//	 ArrayList<Swap> a4 = new ArrayList<Swap>();
			 
			// HERE THE SWAPS ARE SAVED IN ARRAYS FROM THE HASHSET 
            int i = 0;
			Iterator<Swap> it=randommoves.iterator();
			 while(it.hasNext())
		        {
		            Swap value =(Swap)it.next(); 
		            int r = i%3;
		            switch (r)
		            {
		                 case (0):
		                	 a0.add(value);
		                    break;
		                 case (1):
			            	    a1.add(value);
			                    break;
		                 case (2):
		                	 a2.add(value);
			                    break;  
		            }
		            i+= 1;
		        }
			
		//	System.out.println("The size of random moves generated :" +randommoves.size());
			 //HERE THE OBJECT FOR THREAD CLASS IS CREATED AND THE INPUT ARGUMENTS ARE PASSED
			threadssync worker = new threadssync(a0,a1,a2,T,movesPerTemperature,calculator);
		    
			//THE THREADS ARE MADE TO RUN
			worker.run();
			// THE ALPHAABS VALUE IS CALLED IN ORDER TO UPDATE THE TEMPERATURE
			double alphaAbs = worker.getvalue();
	//      System.out.println("Alphaabs value" +alphaAbs);  
//			System.out.println("Temperature: "+T+", Total Cost: "+calculator.calculateTotalCost());
			
			double alpha1 = alphaAbs/movesPerTemperature;
			
			Rlim = updateRlim(alpha1);
			T=updateTemperature(T,alpha1);
			
		}
	//	System.out.println("timeSpendFindSwap: "+(timeSpendFindSwap/1.0E9));
	//	System.out.println("timeSpendCalcDCAndApplySwap: "+(timeSpendCalcDCAndApplySwap/1.0E9));
	}

	private double updateTemperature(double temperature, double alpha1) {
		double gamma;
		if (alpha1 > 0.96)     	gamma=0.5;
		else if (alpha1 > 0.8)	gamma=0.9;
		else if (alpha1 > 0.15)	gamma=0.95;
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
			//calculator.apply(swap);
			swap.apply();

			somDeltaKost+=deltaCost;
			kwadratischeSomDeltaKost+=Math.pow(deltaCost,2);
		}
		double stdafwijkingDeltaKost=Math.sqrt(kwadratischeSomDeltaKost/manipulator.numBlocks()-Math.pow(somDeltaKost/manipulator.numBlocks(),2));
		double T=20*stdafwijkingDeltaKost;
		return T;
	}
	
  }


			
		
	
		

	



