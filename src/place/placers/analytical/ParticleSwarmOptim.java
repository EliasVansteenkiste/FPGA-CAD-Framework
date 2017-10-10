package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Column;
import place.placers.analytical.HardblockConnectionLegalizer.Crit;
import place.placers.analytical.HardblockConnectionLegalizer.Net;
import place.placers.analytical.HardblockConnectionLegalizer.Site;

public class ParticleSwarmOptim {
	
	private static final int SWARM_SIZE = 20;
	private static final int MAX_ITERATION = 100;
	private static final int PROBLEM_DIMENSION = 2;
	private static final double COGNITIVE_CONSTANT = 2.05;
	private static final double SOCIAL_CONSTANT = 2.05;
	private static final double W_UPPERBOUND = 1.0;
	private static final double W_LOWERBOUND = 0.0;
	private static final double LOC_X_MAX = ;//WIDTH
	private static final double LOC_X_MIN = 0;
	private static final double LOC_Y_MAX = ;//HEIGHT
	private static final double LOC_Y_MIN = 0;
	private static final double VEL_MIN = 0;
	private static final double VEL_MAX = ;//
	
	private List<Particle> swarm = new ArrayList<Particle>();
	private double[] pBest = new double[SWARM_SIZE];
	private List<Location> pBestLocation = new ArrayList<Location>();
	private double gBest;
	private Location gBestLocation;
	private double[] fitnessList = new double[SWARM_SIZE];
	Random generator = new Random();
	
	public void psoMain(){
		initializeSwarm();
		updateFitnessList();
		
		for(int i = 0; i < SWARM_SIZE; i++){
			pBest[i] = fitnessList[i];
			pBestLocation.add(swarm.get(i).getLocation());
		}
		
		double w;
		
		for(int iteration = 0; iteration < MAX_ITERATION; iteration++){
			for(int i = 0; i < SWARM_SIZE; i++){
				if(fitnessList[i] < pBest[i]){
					fitnessList[i] = pBest[i];
					pBestLocation.set(i, swarm.get(i).getLocation());
				}
			}
			int gBbestIndex = getBestIndex(fitnessList);
			if(iteration == 0 || fitnessList[gBbestIndex] < gBest){
				gBest = fitnessList[gBbestIndex];
				gBestLocation = swarm.get(gBbestIndex).getLocation();
			}
			
			w = W_UPPERBOUND - ((double) iteration / MAX_ITERATION)*(W_UPPERBOUND - W_LOWERBOUND);//decrease linearly
			
			for(int i = 0; i < SWARM_SIZE; i++){
				double r1 = generator.nextDouble();
				double r2 = generator.nextDouble();
				
				Particle p = swarm.get(i);
				
				double[] newVel = new double[PROBLEM_DIMENSION];
				newVel[0] = w * p.getVelocity().getVel()[0] + r1 * COGNITIVE_CONSTANT * (pBestLocation.get(i).getLoc()[0] - p.getLocation().getLoc()[0])
							+ r2 * SOCIAL_CONSTANT *(gBestLocation.getLoc()[0] - p.getLocation().getLoc()[0]);
													
				newVel[1] = w * p.getVelocity().getVel()[1] + r1 * COGNITIVE_CONSTANT * (pBestLocation.get(i).getLoc()[1] - p.getLocation().getLoc()[1])
							+ r2 * SOCIAL_CONSTANT *(gBestLocation.getLoc()[1] - p.getLocation().getLoc()[1]);
				Velocity velocity = new Velocity(newVel);
				p.setVelocity(velocity);
				
				double[] newLoc = new double[PROBLEM_DIMENSION];
				newLoc[0] = p.getLocation().getLoc()[0] + newVel[0];
				newLoc[1] = p.getLocation().getLoc()[1] + newVel[1];
				Location location = new Location(newLoc);
			}
			System.out.println("ITERATION " + iteration + ": ");
			System.out.println("     Best X: " + gBestLocation.getLoc()[0]);
			System.out.println("     Best Y: " + gBestLocation.getLoc()[1]);
			
			updateFitnessList();
		}
	}
	public void initializeSwarm(){
		
		for (int i = 0; i < SWARM_SIZE; i++){
			double[] loc = new double[PROBLEM_DIMENSION];
			loc[0] = LOC_X_MIN + generator.nextDouble() * (LOC_X_MAX - LOC_X_MIN);
			loc[1] = LOC_Y_MIN + generator.nextDouble() * (LOC_Y_MAX - LOC_Y_MIN);
			Location location = new Location(loc);
			
			double[] vel = new double[PROBLEM_DIMENSION];
			vel[0] = VEL_MIN + generator.nextDouble() * (VEL_MAX - VEL_MIN);
			vel[1] = VEL_MIN + generator.nextDouble() * (VEL_MAX - VEL_MIN);
			Velocity velocity = new Velocity(vel);
			
			Particle p = new Particle(0.0, velocity, location);//TODO initialize fitness?
			swarm.add(p);
		}
	}
	public void updateFitnessList(){
		for (int i = 0; i < SWARM_SIZE; i++){
			fitnessList[i] = swarm.get(i).getFitness();
		}
	}
	private int getBestIndex(double[] array){
		int index = 0;
		double minValue = array[0];
		
		for(int i = 0; i < array.length; i++){
			if(array[i] < minValue){
				minValue = array[i];
				index = i;
			}	
		}
		return index;
	}
	private class Particle{
		private double fitness;
		private Velocity velocity;
		private Location location;
		
		Particle(double fitness, Velocity velocity, Location location){
			this.fitness = fitness;
			this.velocity = velocity;
			this.location = location;
		}
		
		private double getFitness(){
			return fitness ;//TODO
		}
		
		private Velocity getVelocity(){
			return this.velocity;
		}
		
		private void setVelocity(Velocity velocity){
			this.velocity = velocity;
		}
		
		private Location getLocation(){
			return this.location;
		}
		
		private void setLocation(Location location){
			this.location = location;
		}
		
		
	}
	private class Location{
		private Site[] location;//TODO to be specified to FPGA locations
		
		Location(Site[] location ){
			this.location = location;
		}
		private Site[] getLoc(){
			return this.location;
		}
		private void setVel(Site[] loc){
			this.location = loc;
		}
	}
	private class Velocity{
		private int[][] velocity;
		Velocity(int[][] velocity){
			this.velocity = velocity;
		}
		private int[][] getVel(){
			return velocity;
		}
		private void setVel(int[][] vel){
			this.velocity = vel;
		}
	}
	
	
	private ArrayList<Site> addVelocity(ArrayList<Site> currentLoc , int[][] vel){//TODO
//		ArrayList <Site> newLoc = new ArrayList<Site>();
		for(int i = 0; i < currentLoc.size(); i++){
			Collections.swap(currentLoc, vel[i][0], vel[i][1]);
		}
		return currentLoc;
	}
	
	
//	class Site {
//	    final int column, row, height;
//	    Block block;
//
//	    public Site(int column, int row, int height) {
//	        this.column = column;
//	        this.row = row;
//	        this.height = height;
//	        
//	        this.block = null;
//	    }
//	    
//	    public void setBlock(Block block){
//		    this.block = block;
//	    }
//	    public boolean hasBlock(){
//	    	return this.block != null;
//	    }
//	    public Block getBlock(){
//	        return this.block;
//	    }
//	    public void removeBlock(){
//	    	this.block = null;
//	    }
//	}
	

}
