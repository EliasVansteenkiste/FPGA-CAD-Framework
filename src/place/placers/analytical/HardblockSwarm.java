package place.placers.analytical;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import place.circuit.architecture.BlockType;
import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Column;
import place.placers.analytical.HardblockConnectionLegalizer.Crit;
import place.placers.analytical.HardblockConnectionLegalizer.Net;
import place.placers.analytical.HardblockConnectionLegalizer.Site;

public class HardblockSwarm {
	private Block[] blocks;
	private Site[] sites;
	
//	private int numBlocks;
	private int numSites;
		
	//PSO
	private static final int SWARM_SIZE = 10;
	private static final int MAX_ITERATION = 10;
	private static final double COGNITIVE_CONSTANT = 2.05;
	private static final double SOCIAL_CONSTANT = 2.05;
	private static final double W_UPPERBOUND = 0.9;
	private static final double W_LOWERBOUND = 0.4;	
	private static final int VEL_SIZE = 20;//size of swap sequence TODO never used
	private final Random rand;
	private List<Particle> swarm;
	private Double[] fitnessList;
	private Double[] pBest;
	private Double gBest;
	private List<Site[]> pBestLocation;
	private Site[] gBestLocation;
	
	private BlockType blockType;
	
	HardblockSwarm(int seed){						
		this.rand = new Random();
	}
	//legalize hard block
	public void doPSO(Column column, BlockType blockType){
		this.blockType = blockType;
		
		this.swarm = new ArrayList<>();
		this.fitnessList = new Double[SWARM_SIZE];
		this.pBest = new Double[SWARM_SIZE];
		
		this.pBestLocation = new ArrayList<Site[]>();
		
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.sites = column.sites;
		this.numSites = this.sites.length;
		this.gBestLocation = new Site[this.numSites];
		this.doPSO();
	}
	
	/*******************************
	* particle swarm optimization
	********************************/
	private void doPSO(){
//		boolean printStatistics = false;
		this.numSites = this.sites.length;
		this.gBestLocation = new Site[this.numSites];//TODO

		initializeSwarm();
		updateFitnessList();
		
		for(int i = 0; i < SWARM_SIZE; i++){
			this.pBest[i] = this.swarm.get(i).getFitness();
			this.pBestLocation.add(this.swarm.get(i).getLocation());
		}
		
		int iteration = 0;
		double w;//weight decrease linearly
		
		while(iteration < MAX_ITERATION){
//			System.out.println(iteration);//TODO
			for(int i = 0; i < SWARM_SIZE; i++){
				//update pBest
				if(this.fitnessList[i] < this.pBest[i]){
					this.pBest[i] = this.fitnessList[i];
					this.pBestLocation.set(i, this.swarm.get(i).getLocation());
				}
				
				//update gBest
				int bestParticleIndex = getMinPos(this.fitnessList);
				if(iteration == 0 || this.fitnessList[bestParticleIndex] < this.gBest){
					this.gBest = this.fitnessList[bestParticleIndex];
//					System.out.println(i + " "+ this.gBest);//TODO
					System.arraycopy(this.swarm.get(i).getLocation(), 0, this.gBestLocation, 0, this.numSites);
				}
				
				w = W_UPPERBOUND - (((double) iteration) / MAX_ITERATION) * (W_UPPERBOUND - W_LOWERBOUND);
				
				//update velocity
				Particle p = swarm.get(i);
				List<Velocity> newVel = updateVelocity(p, w, this.pBestLocation.get(i), this.gBestLocation);
				p.setVelocity(newVel);
				//update location
				Site[] newLocation = updateLocations(p.getLocation(), newVel);
				p.setLocation(newLocation);
			}			
			iteration++;
			updateFitnessList();
		}
		for(Site site:this.gBestLocation){
			if(site.hasBlock()){
				site.getBlock().setSite(site);
			}	
		}
	}
	
	private int getMinPos(Double[] fitnessList) {
		int pos = 0;
		double minValue = fitnessList[pos];
		for(int i = 0; i < fitnessList.length; i++){
			if(fitnessList[i] < minValue){
				pos = i;
				minValue = fitnessList[i];
			}
		}
		return pos;
	}
	//initialize swarm
	private void initializeSwarm(){
		Particle particle;
//		System.out.println(this.blockType + " => Num sites: " + this.numSites);//TODO REMOVE
		Site[] locations = new Site[this.numSites];
		for(int i = 0; i < SWARM_SIZE; i++){
			for(Site site : this.sites){
				site.removeBlock();
			}
			for(Block block : this.blocks){
				Site site = this.sites[this.rand.nextInt(this.numSites)];
				while(site.hasBlock()){//TODO faster
					site = this.sites[this.rand.nextInt(this.numSites)];
				}
				site.setBlock(block);
				block.setSite(site);
			}
			System.arraycopy(this.sites, 0, locations, 0, this.numSites);
			
			int velLength = this.rand.nextInt(VEL_SIZE);
			List<Velocity> vel = new ArrayList<Velocity>();
			for(int j = 0; j < velLength; j++){
				Velocity v = new Velocity();
				v.setFromIndex(0);
				v.setToIndex(0);
				vel.add(v);	
			}
			particle = new Particle(locations, vel);
			this.swarm.add(particle);
//			System.out.println("particle " + i + " location.length " + this.swarm.get(i).getLocation().length);
		}
	}
	
	//record fitness of all particles in the swarm
	private void updateFitnessList(){
		for(int i = 0; i < SWARM_SIZE; i++){
			this.fitnessList[i] = this.swarm.get(i).getFitness();
//			System.out.println(i + " " + this.fitnessList[i]);
		}
	}

	private List<Velocity> updateVelocity(Particle p, double w, Site[] pBestLocation, Site[] gBestLocation){
		List<Velocity> newVel = new ArrayList<Velocity>();
		
		List<Velocity> weightedVel = multipliedByC(p.getVelocity(), w);
		List<Velocity> cognitiveVel = multipliedByC(getSwapSequence(pBestLocation, p.getLocation()), COGNITIVE_CONSTANT * Math.random());
		List<Velocity> socialVel = multipliedByC(getSwapSequence(gBestLocation, p.getLocation()), SOCIAL_CONSTANT * Math.random());

		if(weightedVel != null){
			newVel.addAll(weightedVel);
		}
		if(cognitiveVel != null){
			newVel.addAll(cognitiveVel);
		}
		if(socialVel != null){
			newVel.addAll(socialVel);
		}
		return newVel;
	}

	//pBest(gBest) - X 
	private List<Velocity> getSwapSequence(Site[] bestLoc, Site[] currentLoc){
		List<Velocity> swapSequence = new ArrayList<Velocity>();
		Site[] tmpLoc = currentLoc.clone();
		
		Velocity vel = new Velocity();
		for(int i = 0; i < this.numSites; i++){
			if(bestLoc[i].hasBlock()){
				for(int j = 0; j < this.numSites; j++){
					if(bestLoc[i].getBlock().equals(tmpLoc[j].getBlock())){
						vel.setFromIndex(i);
						vel.setToIndex(j);
						this.doOneSwap(tmpLoc, vel.getFromIndex(), vel.getToIndex());
						swapSequence.add(vel);
						break;
					}
				}
			}else{
				break;
			}
		}
		return swapSequence;		
	}
	//do swaps to update particle's location: X + Velocity 
	//TODO use getSwap method to calculate delta cost of each cost. cost of one update can be calculated by summing each delta cost ?
	private Site[] updateLocations(Site[] locations , List<Velocity> vel){
		if(!vel.equals(null)){	
			for(int i = 0; i < vel.size(); i++){
				this.doOneSwap(locations, vel.get(i).getFromIndex(), vel.get(i).getToIndex());	
			}
		}
		return locations;
	}
	private Site[] doOneSwap(Site[] locations, int fromIndex, int toIndex){	
		if(locations[fromIndex].hasBlock() && locations[toIndex].hasBlock()){
			Block tmp;
			tmp = locations[fromIndex].block;
			locations[fromIndex].setBlock(locations[toIndex].block);
			locations[toIndex].block.setSite(locations[fromIndex]);//TODO check before using
			locations[toIndex].setBlock(tmp);	
			locations[fromIndex].block.setSite(locations[toIndex]);
		}else if(!locations[fromIndex].hasBlock() && locations[toIndex].hasBlock()){
			locations[fromIndex].setBlock(locations[toIndex].block);
			locations[toIndex].block.setSite(locations[fromIndex]);
			locations[toIndex].setBlock(null);
		}else if(locations[fromIndex].hasBlock() && !locations[toIndex].hasBlock()){
			locations[toIndex].setBlock(locations[fromIndex].block);
			locations[fromIndex].block.setSite(locations[toIndex]);
			locations[fromIndex].setBlock(null);
		}
		return locations;
	}
	//Velocity multiplied by a constant
	private List<Velocity> multipliedByC(List<Velocity> vel, double c){
		List<Velocity> newVel = new ArrayList<Velocity>();
		int newSize = (int)Math.floor(vel.size() * c);
//		System.out.println(vel.size() + " " + c + " " + newSize);
		if(vel.size() != 0){
			if(c == 0){
				newVel = null;
			}
			if(c == 1){
				for(int i = 0; i < newSize; i++){
					newVel.add(vel.get(i));
				}
			}
			if(c < 1){	
				for(int i = 0; i < newSize; i++){
					newVel.add(vel.get(i));
				}
			}else if(c > 1){
				int nLoop = (int)Math.floor(newSize / vel.size());
				for(int n = 0; n < nLoop; n++){
					for(int i = 0; i < vel.size(); i++){
						newVel.add(vel.get(i));
					}
				}
				for(int i = 0; i < newSize - nLoop * vel.size(); i++){
					newVel.add(vel.get(i));
				}
			}
		}else {
			newVel = null;
		}
		return newVel;
	}
	
	private class Particle{
		private double pFitness;
		private Site[] location;
		private List<Velocity> velocity;
		private Set<Net> pNets;
		private Set<Crit> pCrits;
		private Double pCost;
		
		Particle(Site[] location, List<Velocity> velocity){
			this.velocity = velocity;
			this.location = location;
			this.pNets = new HashSet<>();
			this.pCrits = new HashSet<>();
		}
		private Double getFitness(){
			this.pNets.clear();
			this.pCrits.clear();
			for(Site site:location){
				if(site.hasBlock()){
					for(Net net:site.block.nets){
						this.pNets.add(net);
					}
					for(Crit crit:site.block.crits){
						this.pCrits.add(crit);
					}
				}
				
				this.pCost = 0.0;
				
				for(Net net:this.pNets){
					this.pCost += net.connectionCost();
				}
				for(Crit crit:this.pCrits){
					this.pCost += crit.timingCost();
				}
			}
//			System.out.println(this.pCost);
			this.pFitness = 1.0 / this.pCost;
			return this.pFitness;
		}
		private List<Velocity> getVelocity(){
			return this.velocity;
		}
		private void setVelocity(List<Velocity> velocity){
			this.velocity = velocity;
		}
		private Site[] getLocation(){
			return this.location;
		}
		private void setLocation(Site[] location){
			this.location = location;
		}		
	}

	class Velocity {
		int fromIndex;
		int toIndex;
		public Velocity(){
			super();
		}
		public Velocity(int fromIndex, int toIndex) {
			super();
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
		
		public int getFromIndex() {
			return this.fromIndex;
		}
		public void setFromIndex(int fromIndex) {
			this.fromIndex = fromIndex;
		}
		public int getToIndex() {
			return this.toIndex;
		}
		public void setToIndex(int toIndex) {
			this.toIndex = toIndex;
		}			
	}
	
}
