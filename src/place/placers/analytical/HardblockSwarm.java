package place.placers.analytical;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import place.circuit.architecture.BlockType;
//import place.placers.analytical.HardblockConnectionLegalizer.Block;
//import place.placers.analytical.HardblockConnectionLegalizer.Column;
//import place.placers.analytical.HardblockConnectionLegalizer.Crit;
//import place.placers.analytical.HardblockConnectionLegalizer.Net;
//import place.placers.analytical.HardblockConnectionLegalizer.Site;
import place.placers.analytical.HardblockSwarmLegalizer.Block;
import place.placers.analytical.HardblockSwarmLegalizer.Column;
import place.placers.analytical.HardblockSwarmLegalizer.Crit;
import place.placers.analytical.HardblockSwarmLegalizer.Net;
import place.placers.analytical.HardblockSwarmLegalizer.Site;

public class HardblockSwarm {
	private Block[] blocks;
	private Site[][] sites;
	
//	private int numBlocks;
	private int numSites;
		
	//PSO
	private int numParticles;
	private static final int MAX_ITERATION = 5;
	private static final double COGNITIVE_CONSTANT = 2.05;
	private static final double SOCIAL_CONSTANT = 2.05;
	private static final double W_UPPERBOUND = 0.9;
	private static final double W_LOWERBOUND = 0.4;	
	private static final int VEL_SIZE = 20;//size of swap sequence TODO
	private final Random rand;
	private List<Particle> swarm;
	private double[] pCostHistory;
	private double[] pBest;
	private double gBest;
	private Site[] pBestLocation;
	private Site[] gBestLocation;
	
	private BlockType blockType;
	
	HardblockSwarm(int seed){						
		this.rand = new Random(seed);
	}
	//legalize hard block
	public void doPSO(Column column, BlockType blockType, int numParticles){
		this.blockType = blockType;
		
		this.numParticles = numParticles;
		this.swarm = new ArrayList<Particle>();
		this.pCostHistory = new double[this.numParticles];
		this.pBest = new double[this.numParticles];
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.numSites = column.sites.length;
		//TODO make the site matrix for PSO
		this.sites = new Site[this.numParticles][this.numSites];
//		System.arraycopy(column.sites, 0, this.sites, 0, this.numSites);
		for(int i = 0; i < this.numParticles; i++){
			System.arraycopy(column.sites, 0, this.sites[i], 0, this.numSites);
//			System.out.println(this.sites[i]);		
		}
		
		this.gBestLocation = new Site[this.numSites];
		this.pBestLocation = new Site[this.numSites];
		this.doPSO();
	}
	
	/*******************************
	* particle swarm optimization
	********************************/
	private void doPSO(){

		initializeSwarm();
		List<Integer> index = new ArrayList<Integer>();
		index = this.swarm.get(0).getBlocksIdList();
		for(int i = 0; i < this.numSites; i++){
			System.out.println(index.get(i));
		}
		updateCostList();
	
		for(int i = 0; i < this.numParticles; i++){
			this.pBest[i] = Double.MAX_VALUE;
		}
		
		double w;//weight decrease linearly
		
		for(int iteration = 0;iteration < MAX_ITERATION; iteration++){
			System.out.println("PSO iteration: "+ iteration);//TODO remove
			for(int pIndex = 0; pIndex < this.numParticles; pIndex++){
				//update pBest
				if(this.pCostHistory[pIndex] < this.pBest[pIndex]){
					this.pBest[pIndex] = this.pCostHistory[pIndex];
					System.arraycopy(this.swarm.get(pIndex).getLocation(), 0, this.pBestLocation, 0, this.numSites);	
				}
//				for(Site site:this.pBestLocation){
//					if(site.hasBlock()){
//						System.out.println(site.block.index);
//					}
//				}
				//update gBest
				int bestParticleIndex = getMinPos(this.pCostHistory);
				if(iteration == 0 || this.pCostHistory[bestParticleIndex] < this.gBest){
					this.gBest = this.pCostHistory[bestParticleIndex];
					
					System.arraycopy(this.sites[bestParticleIndex], 0, this.gBestLocation, 0, this.numSites);
				
//					System.out.println("\t///////bestIndex is " + bestParticleIndex);
//					for(Site site:this.gBestLocation){
//						if(site.hasBlock()){
//							System.out.println(site.block.index);
//						}
//					}
				}
				
				w = W_UPPERBOUND - (((double) iteration) / MAX_ITERATION) * (W_UPPERBOUND - W_LOWERBOUND);				
				//update velocity
				Particle p = swarm.get(pIndex);
				List<Velocity> newVel = updateVelocity(pIndex, p.getVelocity(), w, p.getLocation(), this.pBestLocation, this.gBestLocation);
				p.setVelocity(newVel);
				//update location
				Site[] newLocation = updateLocations(pIndex, p.getLocation(), newVel);
				p.setLocation(newLocation);
				
				this.pCostHistory[pIndex] = p.getCost();
				
//				System.out.println("\tp" + pIndex + "'s" + "\tpCost" + "\tgBest");
//				System.out.println("\t" + p.getCost() + "\t" + this.pBest[pIndex] + "\t" + this.gBest);//TODO print particle info	for debugging 		
			}
		}
		//TODO to change
		for(Site site:this.gBestLocation){
			if(site.hasBlock()){
				site.block.setLegal(site.column, site.row);
			}
		}
	}
	
	private int getMinPos(double[] CostList) {
		int pos = 0;
		double minValue = CostList[pos];
		for(int position = 0; position < CostList.length; position++){
			if(CostList[position] < minValue){
				pos = position;
				minValue = CostList[position];
			}
		}
		return pos;
	}
	//initialize swarm
	private void initializeSwarm(){
		Particle particle;
		System.out.println(this.blockType + " => Num sites: " + this.numSites);//TODO REMOVE
		
		for(int i = 0; i < this.numParticles; i++){
			Site[] locations = new Site[this.numSites];
			for(Site site : this.sites[i]){
				site.removeBlock();
			}
			for(Block block : this.blocks){
				Site site = this.sites[i][this.rand.nextInt(this.numSites)];
				while(site.hasBlock()){//TODO faster?
					site = this.sites[i][this.rand.nextInt(this.numSites)];
				}
				site.setBlock(block);
//				block.setSite(site);
				block.setTmpLegal(i, site.column, site.row);
			}
			
			System.arraycopy(this.sites[i], 0, locations, 0, this.numSites);
//			//TODO check if blocks are randomly placed onto locations//////////////////////////////////////////////////////////
//			System.out.println("Particle: " + i);
//			System.out.println("\tcolumn" + "\trow" + "\tblockIndex");
//			for(int k = 0; k < this.numSites; k++){
//				if(locations[k].hasBlock()){
//					System.out.println("\t" + locations[k].column + "\t" + locations[k].row + "\t" + locations[k].block.index);
//				}
//			}
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			int velLength = this.rand.nextInt(VEL_SIZE);
			List<Velocity> vel = new ArrayList<Velocity>();
			for(int j = 0; j < velLength; j++){
				Velocity v = new Velocity();
				v.setFromIndex(2);
				v.setToIndex(5);
				vel.add(v);	
			}
			
			particle = new Particle(i, this.numSites);
			particle.setLocation(locations);
			particle.setVelocity(vel);
			
			this.swarm.add(particle);
		}
		//TODO to check if each particle is unique/////////////////////////////////////////////////////////////////////////////
//		System.out.println(Arrays.equals(this.swarm.get(0).getLocation(), this.swarm.get(1).getLocation()));
//		///???????????? why this.swarm.get(0).getLocation()[i] +  this.swarm.get(1).getLocation()[i] visit the same address????
//		System.out.println(this.swarm.get(0).getLocation() + " " + this.swarm.get(1).getLocation());		
//		System.out.println("\tp0'sLocation" + "\tp1'sLocation");
//		for(int i = 0; i < this.numSites; i++){
//			int p0BlockIndex = 0;
//			int p1BlockIndex = 0;
//			System.out.println(this.swarm.get(0).getOneSite(i) + " " + this.swarm.get(1).getOneSite(i));
//			if(this.swarm.get(0).getLocation()[i].hasBlock()){
//				p0BlockIndex = this.swarm.get(0).getLocation()[i].getBlock().index;
//			}else{
//				p0BlockIndex = 0;
//			}
//			if(this.swarm.get(1).getLocation()[i].hasBlock()){
//				p1BlockIndex = this.swarm.get(1).getLocation()[i].getBlock().index;
//			}else{
//				p1BlockIndex = 0;
//			}
//			System.out.println("\t" + p0BlockIndex + "\t" + p1BlockIndex);
//		}
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	}
	
	//record Cost of all particles in the swarm
	private void updateCostList(){
		for(int i = 0; i < this.numParticles; i++){
			this.pCostHistory[i] = this.swarm.get(i).getCost();
			System.out.println("updated" + i + " " + this.pCostHistory[i]);
			}	
	}

	private List<Velocity> updateVelocity(int index, List<Velocity> vel, double w, Site[] pLocation, Site[] pBestLocation, Site[] gBestLocation){
		List<Velocity> newVel = new ArrayList<Velocity>();
		
		List<Velocity> weightedVel = multipliedByC(vel, w);
		
		List<Velocity> congVel = getSwapSequence(index, pBestLocation, pLocation);
		List<Velocity> cognitiveVel = multipliedByC(congVel, COGNITIVE_CONSTANT * Math.random());
		List<Velocity> socVel = getSwapSequence(index, gBestLocation,  pLocation);
		List<Velocity> socialVel = multipliedByC(socVel, SOCIAL_CONSTANT * Math.random());

		if(weightedVel != null){
			newVel.addAll(weightedVel);
//		}else{
//			System.out.println("weightedVel is null");
		}
		if(cognitiveVel != null){
			newVel.addAll(cognitiveVel);
//		}else{
//			System.out.println("congnitiveVel is null");
		}
		if(socialVel != null){
			newVel.addAll(socialVel);
//		}else{
//			System.out.println("socialVel is null");
		}
		return newVel;
	}

	//pBest(gBest) - X 
	private List<Velocity> getSwapSequence(int pIndex, Site[] bestLoc, Site[] particleLoc){
		List<Velocity> swapSequence = new ArrayList<Velocity>();
		Site[] tmpLoc = particleLoc.clone();	
		for(int bestSiteIndex = 0; bestSiteIndex < bestLoc.length; bestSiteIndex++){
			if(bestLoc[bestSiteIndex].hasBlock()){				
				for(int pSiteIndex = 0; pSiteIndex < tmpLoc.length; pSiteIndex++){
					if(tmpLoc[pSiteIndex].hasBlock()){
						if(bestLoc[bestSiteIndex].getBlock().index == tmpLoc[pSiteIndex].getBlock().index){
							if(bestSiteIndex!=pSiteIndex){
								Velocity vel = new Velocity();
								vel.setFromIndex(bestSiteIndex);
								vel.setToIndex(pSiteIndex);
								this.doOneSwap(pIndex, tmpLoc, bestSiteIndex, pSiteIndex);
								swapSequence.add(vel);								
								break;							
							}
						}
					}	
				}
			}
		}
		
		return swapSequence;		
	}
	//do swaps to update particle's location: X + Velocity 
	private Site[] updateLocations(int pIndex, Site[] locations , List<Velocity> vel){
		if(vel != null && !vel.isEmpty()){	
			for(int velIndex = 0; velIndex < vel.size(); velIndex++){
				this.doOneSwap(pIndex, locations, vel.get(velIndex).getFromIndex(), vel.get(velIndex).getToIndex());	
			}
		}
		return locations;
	}
	private Site[] doOneSwap(int index, Site[] locations, int fromIndex, int toIndex){	
		if(locations[fromIndex].hasBlock() && locations[toIndex].hasBlock()){
			Block tmp;
			tmp = locations[fromIndex].block;
			
			locations[fromIndex].setBlock(locations[toIndex].block);
//			locations[fromIndex].block.setSite(locations[fromIndex]);
			locations[fromIndex].block.setTmpLegal(index, locations[toIndex].column, locations[toIndex].row);
			
			locations[toIndex].setBlock(tmp);	
//			locations[toIndex].block.setSite(locations[toIndex]);
			locations[toIndex].block.setTmpLegal(index, locations[toIndex].column, locations[toIndex].row);
		}else if(!locations[fromIndex].hasBlock() && locations[toIndex].hasBlock()){
			locations[fromIndex].setBlock(locations[toIndex].block);
//			locations[fromIndex].block.setSite(locations[fromIndex]);
			locations[fromIndex].block.setTmpLegal(index, locations[toIndex].column, locations[toIndex].row);
			locations[toIndex].setBlock(null);
		}else if(locations[fromIndex].hasBlock() && !locations[toIndex].hasBlock()){
			locations[toIndex].setBlock(locations[fromIndex].block);
//			locations[toIndex].block.setSite(locations[toIndex]);
			locations[toIndex].block.setTmpLegal(index, locations[toIndex].column, locations[toIndex].row);
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
				for(int newVelIndex = 0; newVelIndex < newSize; newVelIndex++){
					newVel.add(vel.get(newVelIndex));
				}
			}
			if(c < 1){	
				for(int newVelIndex = 0; newVelIndex < newSize; newVelIndex++){
					newVel.add(vel.get(newVelIndex));
				}
			}else if(c > 1){
				int nLoop = (int)Math.floor(newSize / vel.size());
				for(int n = 0; n < nLoop; n++){
					for(int newVelIndex = 0; newVelIndex < vel.size(); newVelIndex++){
						newVel.add(vel.get(newVelIndex));
					}
				}
				for(int newVelIndex = 0; newVelIndex < newSize - nLoop * vel.size(); newVelIndex++){
					newVel.add(vel.get(newVelIndex));
				}
			}
		}else {
			newVel = null;
		}
		return newVel;
	}
	
	private class Particle{
		private int index;
		private int numSites;
		private Site[] location;
		private List<Velocity> velocity;
		private Set<Net> pNets;
		private Set<Crit> pCrits;
		private double pCost;
		
		Particle(int index, int numSites){
			this.index = index;
			this.numSites = numSites;
			this.velocity = new ArrayList<Velocity>();
			this.location = new Site[numSites];
			this.pNets = new HashSet<>();
			this.pCrits = new HashSet<>();
		}
		private List<Integer> getBlocksIdList(){
			List<Integer> indexList = new ArrayList<Integer>();
			int l = 0;
			for(Site site : this.location){
				if(site.hasBlock()){
					indexList.add(l++, site.getBlock().index);
				}else indexList.add(l++, site.getBlock().index);
			}
			return indexList;
		}
		private double getCost(){
			this.pNets.clear();
			this.pCrits.clear();
			for(Site site:this.location){
				if(site.hasBlock()){
					for(Net net:site.block.nets){
						this.pNets.add(net);
					}
					for(Crit crit:site.block.crits){
						this.pCrits.add(crit);
					}
				}
				for(Net net:this.pNets){
					net.initializeConnectionCost(this.index);
				}
				for(Crit crit:this.pCrits){
					crit.initializeTimingCost(this.index);
				}
				
				this.pCost = 0.0;
				
				for(Net net:this.pNets){
					this.pCost += net.connectionCost();
				}
				for(Crit crit:this.pCrits){
					this.pCost += crit.timingCost();
				}
			}
			return this.pCost;
		}
		private List<Velocity> getVelocity(){
			return this.velocity;
		}
		private void setVelocity(List<Velocity> vel){
			for(int index = 0; index < velocity.size(); index++){
				this.velocity.set(index, vel.get(index));
			}
		}
		private Site[] getLocation(){
			return this.location;
		}
		private Site getOneSite(int siteIndex){
			return this.location[siteIndex];
		}
		private void setLocation(Site[] location){
			System.arraycopy(location, 0, this.location, 0, this.numSites);
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
