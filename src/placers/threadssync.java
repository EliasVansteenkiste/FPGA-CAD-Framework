package placers;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import architecture.Site;

public class threadssync{
	 
	 private double alphaAbs;
	 private double T;  
	 private ArrayList<Swap> moves1;
	 private ArrayList<Swap> moves2;
	 private ArrayList<Swap> moves3;
//	 private ArrayList<Swap> moves4;
//	 private ArrayList<Swap> moves5;
	 Random rand= new Random(1);
	 
	 HashSet<Site> acceptsite = new HashSet<Site>();//To ADD ALL THE ACCEPTED
	 HashSet<Site> refusedsite = new HashSet<Site>();// REJECTED SITES
	 HashSet<Swap> AcceptedSwaps = new HashSet<Swap>();
	 CostCalculator calculator;
	// THE ARRAYS ,TEMPERATURE AND CALCULATOR ARE CALLED HERE 
 public threadssync(ArrayList<Swap> m0,ArrayList<Swap> m1,ArrayList<Swap> m2,double Temp, int d, CostCalculator c)
 {
	this.alphaAbs = 0;
	this.calculator = c;
	this.T = Temp;
	this.moves1 = m0;
	this.moves2 = m1;
	this.moves3 = m2;
//	this.moves4 = m3;
//	this.moves5 = m4;

 }
	   
// THIS SYNCHRONIZED FUNCTION STORES THE SITES IF THEY ARE NOT PRESENT IN THE HASHSET BEFORE
public synchronized void hashadder( Site a){
	 acceptsite.add(a);
}
// THIS SYNCHRONIZED FUNCTION REMOVES THE SITES  
public synchronized void hashsubtracter(Site c){
	acceptsite.remove(c);
}
// THIS SYNCHRONIZED FUNCTION APPLIES THE MOVES SAVES THE ACCEPTED SWAPS AND ALSO INCREMENT'S 
//THE alphaAbs VALUE WHICH IS IMPORANTAN FOR UPGDATING THE TEMPERATURE
public synchronized void increment(Swap b){
	    calculator.apply(b);
	    AcceptedSwaps.add(b);
	    alphaAbs+=1;	
}
// THE REFUSED SWAPS ARE SAVED HERE
 public synchronized void refused(Site d){
	 refusedsite.add(d);
 }
		        
//------------------ THE THREADS START FROM HERE------------------- 	
	
	
	
public void run(){
		  Thread t1 = new Thread(new Runnable(){

			
			public void run() {
			
			 int j = 0;
			 while(j < moves1.size()-1)
		        {
		           
		          // MOVES ARE SPLIT HERE IN TO SITES  
		            Site splitpl1 = moves1.get(j).pl1;
		            Site splitpl2 = moves1.get(j).pl2;

		            //  IF LOOP CHECKS IF THE SITES ARE NOT PRESENT IN THE HASHSET(ACCEPTSITE)
		            if (!acceptsite.contains(splitpl1) && !acceptsite.contains(splitpl2)){
		            	     hashadder(splitpl1);
		            	     hashadder(splitpl2);
		                //  COST CALCULATION 	 
		                	 double deltaCost = calculator.calculateDeltaCost(moves1.get(j));
								if(deltaCost<=0){
									increment(moves1.get(j));
									hashsubtracter(splitpl1);
									hashsubtracter(splitpl2);
									
								    }
								else if(rand.nextDouble()<Math.exp(-deltaCost/T)) {
										increment(moves1.get(j));
										hashsubtracter(splitpl1);
										hashsubtracter(splitpl2);
									 }
								else {
									  hashsubtracter(splitpl1);
									  hashsubtracter(splitpl2);
								     }
							}
		            else {
		            	refused(splitpl1);
		            	refused(splitpl2);
		                 }
		                j+=1;
		              }
		            
		        }  	
		            
	});
		t1.start();
		Thread t2 = new Thread(new Runnable(){

			//IF LOOP HERE 
			public void run() {
			
			 int k = 0;
			 while(k < moves2.size()-1)
		        {
		           
		          // MOVES ARE SPLIT HERE IN TO SITES  
		            Site splitpl1 = moves2.get(k).pl1;
		            Site splitpl2 = moves2.get(k).pl2;

		            //  IF LOOP CHECKS IF THE SITES ARE NOT PRESENT IN THE HASHSET(ACCEPTSITE)
		            if (!acceptsite.contains(splitpl1) && !acceptsite.contains(splitpl2)){
		            	     hashadder(splitpl1);
		            	     hashadder(splitpl2);
		                //  COST CALCULATION 	 
		                	 double deltaCost = calculator.calculateDeltaCost(moves2.get(k));
								if(deltaCost<=0){
									increment(moves2.get(k));
									hashsubtracter(splitpl1);
									hashsubtracter(splitpl2);
									
								    }
								else if(rand.nextDouble()<Math.exp(-deltaCost/T)) {
										increment(moves2.get(k));
										hashsubtracter(splitpl1);
										hashsubtracter(splitpl2);
									 }
								else {
									  hashsubtracter(splitpl1);
									  hashsubtracter(splitpl2);
								     }
							}
		            else {
		            	refused(splitpl1);
		            	refused(splitpl2);
		                 }
		                k+=1;
		              }
		            
		        }  	
		            
	});
		t2.start();
		Thread t3 = new Thread(new Runnable(){

			
			public void run() {
			
			 int l = 0;
			 while(l < moves3.size()-1)
		        {
		           
		          // MOVES ARE SPLIT HERE IN TO SITES  
		            Site splitpl1 = moves3.get(l).pl1;
		            Site splitpl2 = moves3.get(l).pl2;

		            //  IF LOOP CHECKS IF THE SITES ARE NOT PRESENT IN THE HASHSET(ACCEPTSITE)
		            if (!acceptsite.contains(splitpl1) && !acceptsite.contains(splitpl2)){
		            	     hashadder(splitpl1);
		            	     hashadder(splitpl2);
		                //  COST CALCULATION 	 
		                	 double deltaCost = calculator.calculateDeltaCost(moves3.get(l));
								if(deltaCost<=0){
									increment(moves3.get(l));
									hashsubtracter(splitpl1);
									hashsubtracter(splitpl2);
									
								    }
								else if(rand.nextDouble()<Math.exp(-deltaCost/T)) {
										increment(moves3.get(l));
										hashsubtracter(splitpl1);
										hashsubtracter(splitpl2);
									 }
								else {
									  hashsubtracter(splitpl1);
									  hashsubtracter(splitpl2);
								     }
							}
		            else {
		            	refused(splitpl1);
		            	refused(splitpl2);
		                 }
		                l+=1;
		              }
		            
		        }  	
		            
	});
		t3.start();
	// HERE THE THREADS ARE JOINED IN ORDER THAT ALL THE THREADS COMPLETE THEIR WORK TOGETHER 	
	try {
			t1.join();
			t2.join();
			t3.join();
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
	//	System.out.println("The current temperature :" + T);
	//	System.out.println("The alphaabs1 :" +alphaAbs);
	//	System.out.println("Number of moves per temperature :" + movesize);
	//	System.out.println("The size of refused Sites :" +refusedsite.size());
	//	System.out.println("----------------The accepted moves are----------------" );
		
}
// alphaAbs VALUE IS RETURNED TO THE CLASS PPLACER 
public double getvalue(){
	return alphaAbs;
}

//public Iterator<Swap> iterate(){
//	Iterator<Swap> it=moves.iterator();
//	return it;
//}
	
}		
