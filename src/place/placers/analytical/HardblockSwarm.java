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
import place.util.TimingTree;

public class HardblockSwarm {
	private Block[] blocks;
	private Site[] sites;
	private int numSites;
	private int numBlocks;
//	private BlockType blockType;
	private int blockHeight;
	private int legalcordinateX;
	
	private Set<Net> columnNets;
	private Set<Crit> columnCrits;
		
	//PSO
	private int numParticles;
	private static final int MAX_ITERATION = 30;

	private static final double COGNITIVE_L = 0.01;
	private static final double COGNITIVE_H = 1.6;

	private static final double SOCIAL_L = 0.01;
	private static final double SOCIAL_H = 1.6;
	private static final double W_UPPERBOUND = 0.9;
	private static final double W_LOWERBOUND = 0.2;
	private static final int VEL_SIZE = 20;//size of swap sequence TODO
	private final Random rand;
	private List<Particle> swarm;
	
	private double gBest;

	private int[] gBestIndexList;
	
	private List<Swap> swaps;
	private List<Swap> newVel;
	private double deltaCost;
	
	private final TimingTree timingTree;
	
	private static final boolean printout = true;
	
	HardblockSwarm(int seed){						
		this.rand = new Random(seed);
		this.timingTree = new TimingTree(false);
		this.columnNets = new HashSet<>();
		this.columnCrits = new HashSet<>();
		
		this.swarm = new ArrayList<Particle>();
		this.swaps = new ArrayList<Swap>();
		this.newVel = new ArrayList<Swap>();
		this.deltaCost = 0.0;
	}
	//legalize hard block
	public void doPSO(Column column, BlockType blockType, int numParticles){
//		this.blockType = blockType;
		this.blockHeight = blockType.getHeight();
		this.legalcordinateX = column.coordinate;
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		
		this.numBlocks = blocks.length;
		this.numSites = column.sites.length;
		this.sites = new Site[this.numSites];
		System.arraycopy(column.sites, 0, this.sites, 0, this.numSites);	
		if(!this.printout){
			System.out.println(" ->initial");
			for(int k = 0; k < this.numSites; k++){
				if(this.sites[k].hasBlock()){
					System.out.println("\t" + k + "\t" + this.sites[k].block.index);
				}else System.out.println("\t" + k + "\t" + -1);
			}
		}
		
		this.columnNets.clear();
		this.columnCrits.clear();
		for(Block block:this.blocks){
			for(Net net:block.nets){
				this.columnNets.add(net);
			}
			for(Crit crit:block.crits){
				this.columnCrits.add(crit);
			}
		}	
		
		this.numParticles = numParticles;
		
		this.gBestIndexList = new int[this.numSites];
		
//		System.out.printf(this.blockType + " column" +  + column.coordinate + " => Num sites: " + this.numSites + " => Num blocks: " + this.numBlocks);//TODO REMOVE
		
		this.doPSO();
		this.setBlockLegal();
//		System.out.println("breakpoint");
		}
	
	/*******************************
	* particle swarm optimization
	********************************/
	private void doPSO(){

		this.timingTree.start("Initialize the swarm");
		this.initializeSwarm();
		this.timingTree.time("Initialize the swarm");
		
		this.timingTree.start("get gBest");
		this.getGlobalBest();
		this.timingTree.time("get gBest");
		if(!this.printout){
			System.out.println("gBest\t"+ String.format("%.2f", this.gBest));
			System.out.println("////////////////////////Initialization finished!////////////////////////");
		}
		
		double w;//weight decrease linearly
		double r1, r2;
		
		for(int iteration = 0;iteration < MAX_ITERATION; iteration++){
			if(!this.printout)
				System.out.println("PSO iteration: "+ iteration);//TODO remove
			
			w = W_UPPERBOUND - (((double) iteration) / MAX_ITERATION) * (W_UPPERBOUND - W_LOWERBOUND);
			r1 = COGNITIVE_H - (((double) iteration) / MAX_ITERATION) * (COGNITIVE_H - COGNITIVE_L); 
			r2 = SOCIAL_L + (((double) iteration) / MAX_ITERATION) * (SOCIAL_H - SOCIAL_L);
			for(Particle p : this.swarm){							
				//update velocity
//				if(this.printout)
//					System.out.println("for particle " + p.pIndex);
				
				this.updateVelocity(p.getVelocity(), w, r1, r2, p.blockIndexList, p.pBestIndexList, this.gBestIndexList);
				p.setVelocity(this.newVel);
				//update location
				this.updateLocations(p.blockIndexList, this.newVel);
				//set blocks's tmpLegal by connecting each site with each block in the order from indexList
				for(Block block : this.blocks){
					int siteIndex = this.getSiteIndex(p.blockIndexList, block.index);
					block.setLegal(this.legalcordinateX, siteIndex * this.blockHeight +1);
				}
				//update pBest
//				p.pCost += this.deltaCost;
				p.pCost = this.getCost();
				if(p.pCost < p.pBest){
					p.pBest = p.pCost;
					System.arraycopy(p.blockIndexList, 0, p.pBestIndexList, 0, this.numSites);
				}
				//////////////////to check the updated order of blocks//////////////////////////////////////////////////////
				if(!this.printout){
//					System.out.println("paticle " + p.pIndex + " updated");
//					for(int a = 0; a < this.numSites; a++){
//						System.out.println("\t" + a + "\t" + this.blockIndexMatrix[p.pIndex][a]);
//					}
					System.out.println("\t" + String.format("%.2f", p.pCost));
//					System.out.println("pBest\t" + String.format("%.2f",  this.pBest[p.pIndex]));//TODO print particle info	for debugging 	
				}
			}
			this.getGlobalBest();
			if(!this.printout) System.out.println("gBest\t" + String.format("%.2f",  this.gBest));
		}
	}
	private void setBlockLegal(){
		System.out.println(" psogBest -> " + String.format("%.2f",  this.gBest));
		if(!this.printout){
			for(int m = 0; m < this.numSites; m++){
				System.out.println(this.gBestIndexList[m] + "\t" + m);
			}
		}
//		System.out.println("/////////set legal/////////");
		for(Block block : this.blocks){
			int siteIndex = this.getSiteIndex(this.gBestIndexList, block.index);
			block.setSite(this.sites[siteIndex]);
			this.sites[siteIndex].setBlock(block);
			block.setLegalXY(this.legalcordinateX, siteIndex * this.blockHeight +1);
//			System.out.println(block.index + "\t" + siteIndex + "\t"+ block.legalX + "\t" + block.legalY);
		}
	}
	private int getSiteIndex(int[] list, int value){
		int position = 0;
		for(int pos = 0; pos < list.length; pos++){
			if(list[pos] == value) position = pos;
		}
		return position;
	}
	private int getMinPbestIndex(){
		int pos = 0;
		double minValue = this.swarm.get(0).pBest;
		for(Particle particle : this.swarm){
			if(particle.pBest < minValue){
				pos = particle.pIndex;
				minValue = particle.pBest;
			}
		}
		return pos;
	}
	private double getCost(){
//		for(Net net:this.columnNets){
//			net.initializeConnectionCost();;
//		}
//		for(Crit crit:this.columnCrits){
//			crit.initializeTimingCost();
//		}
		double cost = 0.0;
		for(Net net:this.columnNets){
			cost += net.connectionCost();
		}
		for(Crit crit:this.columnCrits){
			cost += crit.timingCost();
		}
		return cost;
	}
	//initialize swarm
	private void initializeSwarm(){
		this.swarm.clear();
		
//		Particle baseLineParticle = new Particle(0, this.numSites);
//		baseLineParticle.setLocation(this.sites);
//		this.pBest[0] = this.getCost();
//		this.swarm.add(baseLineParticle);
//		int j = 0;
//		for(Site site : this.sites){
//			if(site.hasBlock()){
//				this.blockIndexMatrix[0][j] = site.block.index;
//				site.block.setLegal(site.column, site.row);
//			}else this.blockIndexMatrix[0][j] = 0;
//			j++;
//		}
		
//		if(this.printout){
//			System.out.println("particle 0");
//			for(int a = 0; a < this.numSites; a++){
//				System.out.println("\t" + a + "\t" + this.blockIndexMatrix[0][a]);
//			}
//			System.out.println(this.pBest[0]);
//		}
//		
			
		for(int i = 0; i < this.numParticles; i++){		
			this.timingTree.start("randomly assign blocks");
			for(Site site : this.sites){
				site.removeBlock();
			}	
			for(Block block : this.blocks){
				Site site = this.sites[this.rand.nextInt(this.numSites)];
				while(site.hasBlock()){//TODO faster?
					site = this.sites[this.rand.nextInt(this.numSites)];
				}
				site.setBlock(block);
//				block.setTmpLegal(i, site.column, site.row);
				block.setLegal(site.column, site.row);
			}
			this.timingTree.time("randomly assign blocks");
			

//			System.arraycopy(this.sites, 0, locations, 0, this.numSites);
//			///////////////////////////////////TODO check if blocks are randomly placed onto locations/////////////////////////
			if(!this.printout){
				System.out.println("Particle: " + i);
				for(int k = 0; k < this.numSites; k++){
					if(this.sites[k].hasBlock()){
						System.out.println("\t" + k + "\t" + this.sites[k].block.index);
					}else System.out.println("\t" + k + "\t" + -1);
				}
			}			
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
			int velLength = this.rand.nextInt(VEL_SIZE);
			List<Swap> vel = new ArrayList<Swap>();
			for(int m = 0; m < velLength; m++){
				Swap v = new Swap();
				v.setFromIndex(0);
				v.setToIndex(0);
				vel.add(v);	
			}
			
			Particle particle = new Particle(i, this.numSites);
			particle.setVelocity(vel);
			for(int m = 0; m < this.numSites; m++){
				if(this.sites[m].hasBlock()) particle.blockIndexList[m] = this.sites[m].block.index;
				else particle.blockIndexList[m] = -1;
			}
			particle.pCost = this.getCost();
			particle.pBest = particle.pCost;
			
			if(!this.printout)System.out.println(String.format("%.2f", particle.pBest));
			this.swarm.add(particle);
		}
	}
	
	private void getGlobalBest(){
		int bestParticleIndex = this.getMinPbestIndex();
//		if(this.pBest[bestParticleIndex] < this.gBest)
		this.gBest = this.swarm.get(bestParticleIndex).pBest;
//		System.out.println("best index " + bestParticleIndex + "\t" + String.format("%.2f", this.gBest));
		System.arraycopy(this.swarm.get(bestParticleIndex).blockIndexList, 0, this.gBestIndexList, 0, this.numSites);
	}

	private void updateVelocity(List<Swap> vel, double w, double r1, double r2, int[] pLocation, int[] pBestLocation, int[] gBestLocation){
		if(!this.printout){
			System.out.println("paticle's vel:");
			for(int p = 0; p < vel.size(); p++){
				System.out.println("\t" + vel.get(p).fromIndex + "\t" + vel.get(p).toIndex);
			}
			System.out.println("p's location is:");
			for(int p = 0; p < pLocation.length; p++){
				System.out.println("\t" + p + "\t" + pLocation[p]);
			}
			System.out.println("pBest is:");
			for(int p = 0; p < pBestLocation.length; p++){
				System.out.println("\t" + p + "\t" + pBestLocation[p]);
			}
			System.out.println("gBest is:");
			for(int p = 0; p < gBestLocation.length; p++){
				System.out.println("\t" + p + "\t" + gBestLocation[p]);
			}
		}
		
		List<Swap> weightedVel = multipliedByC(vel, w);
		
		if(!this.printout){
			if(weightedVel != null){
				System.out.println("weight: "+ w + " weightedVel\tfrom\tto");
				for(int p = 0; p < weightedVel.size(); p++){
					System.out.println("\t" + p + "\t" + weightedVel.get(p).fromIndex + "\t" + weightedVel.get(p).toIndex);
				}
			}else System.out.println("weightedVel part is null");
		}

		this.getSwapSequence(pBestLocation, pLocation);
		
		if(!this.printout){
			if(this.swaps != null){
				System.out.println("cognitive\tfrom\tto");
				for(int p = 0; p < this.swaps.size(); p++){
					System.out.println("\t" + p + "\t" + this.swaps.get(p).fromIndex + "\t" + this.swaps.get(p).toIndex);
				}
			}else System.out.println("cognitivel part is null");
		}
		
		List<Swap> cognitiveVel = multipliedByC(this.swaps, r1);
		
		if(!this.printout){
			if(cognitiveVel != null){
				System.out.println("cognitiveVel\tfrom\tto");
				for(int p = 0; p < cognitiveVel.size(); p++){
					System.out.println("\t" + p + "\t" + cognitiveVel.get(p).fromIndex + "\t" + cognitiveVel.get(p).toIndex);
				}
			}else System.out.println("cognitiveVel part is null");
		}
		
		this.getSwapSequence(gBestLocation,  pLocation);
		List<Swap> socialVel = multipliedByC(this.swaps, r2);// * Math.random());

		if(!this.printout){
			if(this.swaps != null){
				System.out.println("social\tfrom\tto");
				for(int p = 0; p < this.swaps.size(); p++){
					System.out.println("\t" + p + "\t" + this.swaps.get(p).fromIndex + "\t" + this.swaps.get(p).toIndex);
				}
			}else System.out.println("social part is null");
			if(socialVel != null){
				System.out.println("socialVel\tfrom\tto");
				for(int p = 0; p < socialVel.size(); p++){
					System.out.println("\t" + p + "\t" + socialVel.get(p).fromIndex + "\t" + socialVel.get(p).toIndex);
				}
			}else System.out.println("socialVel part is null");
		}

		this.newVel.clear();
		if(weightedVel != null) newVel.addAll(weightedVel);
		if(cognitiveVel != null) newVel.addAll(cognitiveVel);
		if(socialVel != null) newVel.addAll(socialVel);
		if(!this.printout){
			if(newVel != null){
			System.out.println("newVel\tfrom\tto");
			for(int p = 0; p < newVel.size(); p++){
				System.out.println("\t" + p + "\t" + newVel.get(p).fromIndex + "\t" + newVel.get(p).toIndex);
			}
		}
		}
	}

	//pBest(gBest) - X 
	private void getSwapSequence(int[] bestLoc, int[] particleLoc){	
		this.swaps.clear();
//		Swap swap = new Swap(0, 0);//cannot be here 10.13.2017
		int[] tmpLoc = new int[this.numSites];
		System.arraycopy(particleLoc, 0, tmpLoc, 0, this.numSites);
//		int[] tmpLoc = particleLoc.clone();
		for(int m = 0; m < bestLoc.length; m++){
			int value = bestLoc[m];
			if(value != -1){
				for(int n = 0; n < tmpLoc.length; n++){
					if(value == tmpLoc[n]){
						if(m != n){
							Swap swap = new Swap(0, 0);
							swap.setFromIndex(m);
							swap.setToIndex(n);
							this.doOneSwap(tmpLoc, m , n);
							this.swaps.add(swap);
							break;
						}						
					}
				}
			}		
		}
//		return swaps;	
	}
	private int getIndexInBlocks(int blockIndex){
		int index = -1;
		for(int a = 0; a < this.numBlocks; a++){
			if(this.blocks[a].index == blockIndex)
				index = a;;
		}
		return index;
	}
	//do swaps to update particle's location: X + Velocity 
	private void updateLocations(int[] locations , List<Swap> vel){
		if(vel != null && !vel.isEmpty()){	
			for(int velIndex = 0; velIndex < vel.size(); velIndex++){
				int from = vel.get(velIndex).getFromIndex();
				int to = vel.get(velIndex).getToIndex();
				this.doOneSwap(locations, from, to);
//				this.deltaCost(locations, from, to);
			}
		}
//		return locations;
	}
	public void deltaCost(int[] indexList, int from, int to){
		//from, to site index
		Block block1 = null;
		Block block2 = null;
		int fromY = this.blockHeight * from + 1;
		int toY = this.blockHeight * to + 1;
		int blockIndex1 = indexList[from];
		int blockIndex2 = indexList[to];
		int indexInblocks1 = this.getIndexInBlocks(blockIndex1);
		int indexInblocks2 = this.getIndexInBlocks(blockIndex2);
		if(blockIndex1 != -1){
			block1 = this.blocks[indexInblocks1];
			block1.tryLegal(this.legalcordinateX, toY);
//			block1.tryLegalY(toY);
		}
		if(blockIndex2 != -1){
			block2 = this.blocks[indexInblocks2];
//			block2.tryLegalY(fromY);
			block2.tryLegal(this.legalcordinateX, fromY);
		}
		this.deltaCost = 0.0;
		if(block1 != null){
			for(Net net : block1.nets){
				this.deltaCost += net.deltaVerticalConnectionCost();
				this.deltaCost += net.horizontalConnectionCost();
			}
			for(Crit crit : block1.crits){
				this.deltaCost += crit.deltaVerticalTimingCost();
				this.deltaCost += crit.deltaHorizontalTimingCost();
			}
		}
		if(block2 != null){
			for(Net net : block2.nets){
				this.deltaCost += net.deltaVerticalConnectionCost();
				this.deltaCost += net.deltaHorizontalConnectionCost();
			}
			for(Crit crit : block2.crits){
				this.deltaCost += crit.deltaVerticalTimingCost();
				this.deltaCost += crit.deltaHorizontalTimingCost();
			}
		}
	}
	public int[] doOneSwap(int[] indexList, int from, int to){	
		int tmp;
		int index1 = indexList[from];
		int index2 = indexList[to];
		if(from != to && index1 != index2){
			tmp = indexList[from];
			indexList[from] = indexList[to];
			indexList[to] = tmp;
		}
		return indexList;
	}
	//Velocity multiplied by a constant
	private List<Swap> multipliedByC(List<Swap> vel, double c){
		List<Swap> newVel = new ArrayList<Swap>();
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
				int leftLength = newSize - nLoop * vel.size();
				for(int newVelIndex = 0; newVelIndex < leftLength; newVelIndex++){
					newVel.add(vel.get(newVelIndex));
				}
			}
		}else {
			newVel = null;
		}
		return newVel;
	}
	
	private class Particle{
		private final int pIndex;
		private int numSites;
		private int[] blockIndexList;
		private List<Swap> velocity;
		
		private double pCost;
		private double pBest;
		private int[] pBestIndexList;

		
		Particle(int index, int numSites){
			this.pIndex = index;
			this.numSites = numSites;
			this.velocity = new ArrayList<Swap>();
			this.blockIndexList = new int[numSites];
			
			this.pBest = 0.0;
			this.pBestIndexList = new int[this.numSites];
		}
		private List<Swap> getVelocity(){
			return this.velocity;
		}
		private void setVelocity(List<Swap> vel){
			this.velocity.clear();
			for(int index = 0; index < vel.size(); index++){
				this.velocity.add(vel.get(index));
			}
//			System.out.println(this.velocity.size());
		}
	}

	class Swap {
		int fromIndex;
		int toIndex;
		public Swap(){
			super();
		}
		public Swap(int fromIndex, int toIndex) {
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
