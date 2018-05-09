package place.placers.analytical;

import java.util.Random;
import java.lang.Thread;
import java.util.Set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import place.circuit.architecture.BlockType;
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
	
	private Set<Net> columnNets;
	private Set<Crit> columnCrits;
	
	private double quality;
	
	//PSO
	private int numParticles;
	private static final int MAX_ITERATION = 500;

	private static final double COGNITIVE_L = 0.01;
	private static final double COGNITIVE_H = 2;

	private static final double SOCIAL_L = 0.01;
	private static final double SOCIAL_H = 2;
	private static final double W_UPPERBOUND = 0.9;
	private static final double W_LOWERBOUND = 0.4;
	
	private int velMaxSize;
	
	private final Random rand;
	private List<Particle> swarm;
	
	private int iteration;
	
	private double gBest;
	
	private List<Double> gBestCostHistory;
	private int gBestHistoryIndex;
	private int[] gBestBlockIdList;
	
	private HashMap<Thread, Particle> particleThreadPool;
	
	private final TimingTree timingTree;
	
	private final boolean printout = true;
	
	HardblockSwarm(int seed){
		this.rand = new Random(seed);
		this.timingTree = new TimingTree(false);
		this.columnNets = new HashSet<>();
		this.columnCrits = new HashSet<>();
		
		this.swarm = new ArrayList<Particle>();
	}
	
	//legalize io block
	public void doPSO(Block[] ioBlocks, Site[] ioSites, BlockType blockType, int numParticles, double quality){

		this.blocks = ioBlocks;
		this.sites = ioSites;
		
		this.quality = quality;
		
		this.numParticles = numParticles;
		
		this.doPSO();
		this.setBlockLegal();
	}
	
	//legalize hard block
	public void doPSO(Column column, BlockType blockType, int numParticles, double quality){	
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.quality = quality;
		
		this.sites = column.sites;
	
		this.numParticles = numParticles;
		if(!this.printout) System.out.println("[" + blockType + "" + column.index + ": " + column.blocks.size() + ", " + column.sites.length + "]");
		this.doPSO();
		this.setBlockLegal();
		
		if(!this.printout){
			for(int i = 0; i < this.gBestCostHistory.size(); i++){
				System.out.println(i + " " + String.format("%.2f", this.gBestCostHistory.get(i)));
			}
		}
	}
	
	/*******************************
	* particle swarm optimization
	********************************/
	private void doPSO(){	
		this.numBlocks = this.blocks.length;
		this.numSites = this.sites.length;
		
		this.velMaxSize = (int)Math.round(38.94 + 0.026 * this.numBlocks);//(int)Math.round(28.376 + 0.026 * this.numBlocks);// (int) Math.round(Math.pow(this.numBlocks, 4/3));
		this.gBestBlockIdList = new int[this.numSites];
		
		this.columnNets.clear();
		this.columnCrits.clear();

		for(Block block:this.blocks){
			for(Net net:block.mergedNetsMap.keySet()){
				net.setTotalNum(block.mergedNetsMap.get(net));
				this.columnNets.add(net);
			}
			for(Crit crit:block.crits){
				this.columnCrits.add(crit);
			}
		}
		this.timingTree.start("Initialize the swarm");
		this.initializeSwarm();
		
		this.timingTree.time("Initialize the swarm");
		
		this.gBestCostHistory = new ArrayList<>();
		this.gBestHistoryIndex = 0;
		
		this.getGlobalBest();
		
		this.iteration = 0;
		boolean finalIteration = false;
		
		double w;//weight decrease linearly
		double r1, r2;
		
		for(int iteration = 0;iteration < MAX_ITERATION; iteration++){
//		while(!finalIteration){
			if(!this.printout) System.out.println("\tPSO iteration: "+ iteration);
			
			w = W_UPPERBOUND - (((double) iteration) / MAX_ITERATION) * (W_UPPERBOUND - W_LOWERBOUND);
			r1 = COGNITIVE_H - (((double) iteration) / MAX_ITERATION) * (COGNITIVE_H - COGNITIVE_L); 
			r2 = SOCIAL_L + (((double) iteration) / MAX_ITERATION) * (SOCIAL_H - SOCIAL_L);
			
			for(Particle p : this.swarm){
				//update velocity
				p.updateVelocity(w, r1*this.rand.nextDouble(), r2*this.rand.nextDouble(), this.gBestBlockIdList);			
				//update blockIndex list		
				p.updateLocations();					
				if(p.changed){				
					p.updateBlocksInfo();					
					//update pBest
					p.pCost = p.getCost(p.pIndex);//TODO					
					if(p.pCost < p.pBest){					
						p.pBest = p.pCost;						
						System.arraycopy(p.blockIndexList, 0, p.pBestIndexList, 0, this.numSites);									
					}
				}
				
				//for thread.run
//				p.setParameters(w, r1*this.rand.nextDouble(), r2*this.rand.nextDouble(), this.gBestBlockIdList);
//				Thread t = new Thread(p);
//				t.start();
			}
			
			this.getGlobalBest();
//			this.iteration++;
//			finalIteration = this.finalIteration(this.gBest);
		}		
	}
	private boolean finalIteration(double cost){
		this.gBestCostHistory.add(cost);
		
		int historySize = this.gBestCostHistory.size();
		if(this.gBestCostHistory.size() > 200){
			double max = this.gBestCostHistory.get(historySize - 1);
			double min = this.gBestCostHistory.get(historySize - 1);
			
			for(int i = 0; i < 200; i++){
				double value = this.gBestCostHistory.get(historySize - 1 - i);
				if(value > max){
					max = value;
				}
				if(value < min){
					min = value;
				}
			}
			
			if(min < 1){
				return true;
			}
			
			double ratio = max / min;
			
			if(ratio < 1 + this.quality){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	private void setBlockLegal(){
		if(!this.printout){
			System.out.println("legalized blocks' order");
			for(int m = 0; m < this.numSites; m++){
				System.out.print(this.gBestBlockIdList[m] + " ");
			}
			System.out.println();
		}
		for(Block block : this.blocks){
			Site site = this.getSite(this.gBestBlockIdList, block.index);
			block.setSite(site);
			site.setBlock(block);
			block.setLegalXY(site.column, site.row);
		}
	}
	private Site getSite(int[] list, int value){
		int position = 0;
		for(int pos = 0; pos < list.length; pos++){
			if(list[pos] == value) position = pos;
		}
		return this.sites[position];
	}
	private int getMinPbestIndex(){
		int pos = 0;
		double minValue = Double.MAX_VALUE;
		for(Particle particle : this.swarm){
			if(particle.pBest < minValue){
				pos = particle.pIndex;
				minValue = particle.pBest;
			}
		}
		return pos;
	}
	
	//initialize swarm
	private void initializeSwarm(){
		this.swarm.clear();
		
		this.addBaseLineParticle();
		
		this.initializeParticlesRandomly(1, this.numParticles);
	}
	
	private void initializeParticlesRandomly(int startPIndex, int endPIndex){
		for(int i = startPIndex; i < endPIndex; i++){		
			this.timingTree.start("randomly assign blocks");
			this.randomlyPlaceBlocks();
			this.timingTree.time("randomly assign blocks");
			
			this.timingTree.start("duplicating data");
			this.duplicateData(i);
			this.timingTree.time("duplicating data");
			
			int velLength = this.rand.nextInt(this.velMaxSize);
			List<Swap> vel = new ArrayList<Swap>();
			for(int m = 1; m < velLength; m++){
				Swap v = new Swap();
				v.setFromIndex(0);
				v.setToIndex(0);
				vel.add(v);	
			}
			
			Particle particle = new Particle(i, this.numSites, this.velMaxSize);
			particle.setVelocity(vel);
			for(int m = 0; m < this.numSites; m++){
				if(this.sites[m].hasBlock()) particle.blockIndexList[m] = this.sites[m].block.index;
				else particle.blockIndexList[m] = -1;
				
				particle.pBestIndexList[m] = particle.blockIndexList[m];
			}
			particle.setPNets(this.columnNets);
			particle.setPCrits(this.columnCrits);			
			
			particle.pCost = particle.getCost(i);
			//initial pbest info
			particle.pBest = particle.pCost;
			System.arraycopy(particle.blockIndexList, 0, particle.pBestIndexList, 0, this.numSites);//initial pbest location		
			this.swarm.add(particle);	
		}
	}
	
	private void addBaseLineParticle(){
		Particle baseLineParticle = new Particle(0, this.numSites, this.velMaxSize);
		
		int j = 0;
		for(Site site : this.sites){
			if(site.hasBlock()){
				baseLineParticle.blockIndexList[j] = site.block.index;
			}else baseLineParticle.blockIndexList[j] = -1;
			baseLineParticle.pBestIndexList[j] = baseLineParticle.blockIndexList[j];
			j++;
		}
		
		this.duplicateData(0);
		
		baseLineParticle.setPNets(this.columnNets);
		baseLineParticle.setPCrits(this.columnCrits);
		baseLineParticle.pCost = baseLineParticle.getCost(0);
		baseLineParticle.pBest = baseLineParticle.pCost;
		
		this.swarm.add(baseLineParticle);
		
		if(!this.printout) System.out.println("BP cost: " + String.format("%.2f",  baseLineParticle.pCost) + " ");
		
		if(!this.printout) System.out.println(baseLineParticle.pCost);
		if(!this.printout){
			System.out.println("particle 0");
			for(int a = 0; a < this.numSites; a++){
				System.out.println("\t" + a + "\t" + baseLineParticle.blockIndexList[a]);
			}
		}	
	}
	private void duplicateData(int i){
		for(Block b:this.blocks){
			b.duplicateData(i);
		}
	}
	private void randomlyPlaceBlocks(){
		for(Site site : this.sites){
			site.removeBlock();
		}
		for(Block block : this.blocks){
			Site site = this.sites[this.rand.nextInt(this.numSites)];
			while(site.hasBlock()){
				site = this.sites[this.rand.nextInt(this.numSites)];
			}
			site.setBlock(block);
			block.setLegalXY(site.column, site.row);	
		}
	}
	private void getGlobalBest(){
		int bestParticleIndex = this.getMinPbestIndex();
		this.gBest = this.swarm.get(bestParticleIndex).pBest;
		this.gBestCostHistory.add(this.gBest);
		
		if(!this.printout)System.out.println(String.format("%.2f", this.gBestCostHistory.get(gBestHistoryIndex)));
		this.gBestHistoryIndex++;
		System.arraycopy(this.swarm.get(bestParticleIndex).blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
	}
	private Block getBlock(int blockId){
		Block block = null;
		for(Block b:this.blocks){
			if(b.index == blockId) block = b;
		}
		return block;
	}
	public int[] doOneSwap(int[] indexList, int from, int to){	
		int tmp;
		int indexFrom = indexList[from];
		int indexTo = indexList[to];
		if(indexFrom != indexTo){
			tmp = indexFrom;
			indexList[from] = indexTo;
			indexList[to] = tmp;
		}
		return indexList;
	}
	//Velocity multiplied by a constant
	private List<Swap> multipliedByC(List<Swap> vel, double c){
		List<Swap> weightedVel = new ArrayList<Swap>();
		int newSize = (int)Math.floor(vel.size() * c);
		if(!this.printout) System.out.println(vel.size() + " " + c + " " + newSize);
		if(vel.size() != 0){
			if(c == 0){
				weightedVel = null;
			}
			if(c == 1){
				for(int newVelIndex = 0; newVelIndex < newSize; newVelIndex++){
					weightedVel.add(vel.get(newVelIndex));
				}
			}
			if(c < 1){	
				for(int newVelIndex = 0; newVelIndex < newSize; newVelIndex++){
					weightedVel.add(vel.get(newVelIndex));
				}
			}else if(c > 1){
				int nLoop = (int)Math.floor(newSize / vel.size());
				for(int n = 0; n < nLoop; n++){
					for(int newVelIndex = 0; newVelIndex < vel.size(); newVelIndex++){
						weightedVel.add(vel.get(newVelIndex));
					}
				}
				int leftLength = newSize - nLoop * vel.size();
				for(int newVelIndex = 0; newVelIndex < leftLength; newVelIndex++){
					weightedVel.add(vel.get(newVelIndex));
				}
			}
		}
		return weightedVel;
	}
	
	private class Particle implements Runnable{
		private final int pIndex;
		private final int numSites;
		private int velMaxSize;
		
		private List<Crit> pCrits;
		private List<Net> pNets;
		
		private List<Swap> swaps;
		private List<Swap> newVel;
		
		private Set<Integer> affectedBlockIndex;
		private Set<Block> affectedBlocks;
		private Set<Net> affectedNets;
		private Set<Crit> affectedCrits;
		
		
		private int[] blockIndexList;
		private int[] oldBlockIndexList;
		private boolean changed;
		private List<Swap> velocity;
		
		private double pCost;
		private double pBest;
		private int[] pBestIndexList;
		double inertiaWeight, congnitiveRate, socialRate; 
		int[] gBestBlockIdList;
		
		private int thread;//TODO
		
		Particle(int index, int numSites, int velMaxSize){
			this.pIndex = index;
			this.numSites = numSites;
			this.velMaxSize = velMaxSize;
			
			this.velocity = new ArrayList<Swap>();
			this.blockIndexList = new int[numSites];
			this.oldBlockIndexList = new int[numSites];
			this.changed = false;
			this.pCost = 0.0;
			this.pBest = 0.0;
			this.pBestIndexList = new int[this.numSites];
			this.gBestBlockIdList = new int[this.numSites];
			
			this.swaps = new ArrayList<Swap>();
			this.newVel = new ArrayList<Swap>();
			
			this.affectedBlockIndex = new HashSet<>();
			this.affectedBlocks = new HashSet<>();
			this.affectedNets = new HashSet<>();
			this.affectedCrits = new HashSet<>();
		}
		private void setVelocity(List<Swap> vel){
			this.velocity = new ArrayList<Swap>(vel);
		}
		private void setPNets(Set<Net> columnNets){
			this.pNets = new ArrayList<Net>(columnNets);
//			this.pNets = new HashSet<Net>(columnNets);			
		}
		private void setPCrits(Set<Crit> columnCrits){
			this.pCrits = new ArrayList<Crit>(columnCrits);
		}
		@SuppressWarnings("unused")
		private double getCost(){
			double cost = 0.0;
			double timing = 0.0;
			double conn = 0.0;

			for(Net net:this.pNets) conn += net.connectionCost()*net.getTotalNum();
			for(Crit crit:this.pCrits) timing += crit.timingCost();
			cost = timing + conn;
			
			return cost;
		}
		
		private double getCost(int i){
			double cost = 0.0;
			double timing = 0.0;
			double conn = 0.0;
			for(Net net:this.pNets) conn += net.connectionCost(i)*net.getTotalNum();
			for(Crit crit:this.pCrits) timing += crit.timingCost(i);
			cost = timing + conn;
			cost = timing + conn;		
			return cost;
		}
		
		private void updateVelocity(double w, double r1, double r2, int[] gBestLocation){		
			List<Swap> weightedVel = multipliedByC(this.velocity, w);			
			
			this.getSwapSequence(this.pBestIndexList);				
			List<Swap> cognitiveVel = multipliedByC(this.swaps, r1);		
			
			this.getSwapSequence(gBestLocation);
			List<Swap> socialVel = multipliedByC(this.swaps, r2);// * Math.random());
			
			this.newVel.clear();
			
			int length0 = 0;
			int length1 = 0;
			int length2 = 0;
			if(weightedVel != null) length0 = weightedVel.size();
			if(cognitiveVel != null) length1 = cognitiveVel.size();
			if(socialVel != null) length2 = socialVel.size();
			
			if(length0 + length1 + length2 < this.velMaxSize){
				if(weightedVel != null) this.newVel.addAll(weightedVel);
				if(cognitiveVel != null) this.newVel.addAll(cognitiveVel);
				if(socialVel != null) this.newVel.addAll(socialVel);
			}else{
				int length0Max = (int)Math.round(w / (w + r1 + r2)*this.velMaxSize);
				int length1Max = (int)Math.round(r1 / (w+ r1 + r2) * this.velMaxSize);
				int length2Max = this.velMaxSize - length1Max - length0Max;
				if(weightedVel != null && length0Max != 0){
					if(weightedVel.size() <= length0Max){
						this.newVel.addAll(weightedVel);
					}else{
						for(int l = 0; l < length0Max; l++){
							this.newVel.add(weightedVel.get(l));
						}
					}
				}
				if(cognitiveVel != null && length1Max != 0){
					if(cognitiveVel.size() <= length1Max){
						this.newVel.addAll(cognitiveVel);
					}else{
						for(int l = 0; l < length1Max; l++){
							this.newVel.add(cognitiveVel.get(l));
						}
					}
				}
				if(socialVel != null && length2Max != 0){
					if(socialVel.size() <= length2Max){
						this.newVel.addAll(socialVel);
					}else{
						for(int l = 0; l < length2Max; l++){
							this.newVel.add(socialVel.get(l));
						}
					}
				}
			}
			this.setVelocity(this.newVel);
		}
		//pBest(gBest) - X 
		private void getSwapSequence(int[] bestLoc){	
			this.swaps.clear();
			
			int[] tmpLoc = new int[this.numSites];
			System.arraycopy(this.blockIndexList, 0, tmpLoc, 0, this.numSites);
			
			if(!Arrays.equals(bestLoc, tmpLoc)){
				for(int m = 0; m < bestLoc.length; m++){
					int value = bestLoc[m];
					if(value != -1){
						for(int n = 0; n < tmpLoc.length; n++){
							if(value == tmpLoc[n]){
								if(m != n){
									Swap swap = new Swap(0, 0);
									swap.setFromIndex(m);
									swap.setToIndex(n);
									doOneSwap(tmpLoc, m , n);	
									this.swaps.add(swap);
									break;
								}						
							}
						}
					}		
				}
			}	
		}
		//do swaps to update particle's location: X + Velocity 
		private void updateLocations(){
			System.arraycopy(this.blockIndexList, 0, this.oldBlockIndexList, 0, this.numSites);
			int swapsSize = 0;
			if(this.newVel != null) swapsSize = this.newVel.size();
			if(swapsSize > 0){	
				for(int velIndex = 0; velIndex < swapsSize; velIndex++){
					int from = this.newVel.get(velIndex).getFromIndex();
					int to = this.newVel.get(velIndex).getToIndex();
					if(from != to) doOneSwap(this.blockIndexList, from, to);//only update blockIndexList for a particle
				}
			}
			if(Arrays.equals(this.oldBlockIndexList, this.blockIndexList)) this.changed = false;
			else this.changed = true;
		}
		
		@SuppressWarnings("unused")
		private void getParticleAffectedNetsCrits(){		
			this.affectedBlockIndex.clear();
			this.affectedBlocks.clear();
			this.affectedNets.clear();
			this.affectedCrits.clear();
			for(Swap s:this.newVel){
				this.affectedBlockIndex.add(this.blockIndexList[s.fromIndex]);
				this.affectedBlockIndex.add(this.blockIndexList[s.toIndex]);
			}
			for(int id:this.affectedBlockIndex){
				if(id != -1){
					Block affectedBlock = getBlock(id);
					this.affectedBlocks.add(affectedBlock);
					this.affectedNets.addAll(affectedBlock.mergedNetsMap.keySet());
					this.affectedCrits.addAll(affectedBlock.crits);
				}		
			}
		}
		private void updateBlocksInfo(){
			for(Block block:blocks){		
				Site site = getSite(this.blockIndexList, block.index);
				block.setLegalXYs(this.pIndex, site.column, site.row);
			}
		}
		@SuppressWarnings("unused")
		private double getCostofAffectedBlocks(){
			double oldCostofAffectedBlocks = 0;
			for(Net net:this.affectedNets){
				oldCostofAffectedBlocks += net.connectionCost(this.pIndex)*net.totalNum;
			}
			for(Crit crit:this.affectedCrits){
				oldCostofAffectedBlocks += crit.timingCost(this.pIndex);
			}
			return oldCostofAffectedBlocks;
		}
		private void setParameters(double w, double r1, double r2, int[] gBest){
			this.inertiaWeight = w;
			this.congnitiveRate = r1;
			this.socialRate = r2;
			this.gBestBlockIdList = gBest;
		}
		public void run() {
			// TODO Auto-generated method stub
			//update velocity
			this.updateVelocity(this.inertiaWeight, this.congnitiveRate, this.socialRate, this.gBestBlockIdList);
			
			//update blockIndex list		
			this.updateLocations();	
			
			if(this.changed){				
				this.updateBlocksInfo();					
				//update pBest
				this.pCost = this.getCost(pIndex);//TODO
				
				if(this.pCost < this.pBest){					
					this.pBest = this.pCost;						
					System.arraycopy(this.blockIndexList, 0, this.pBestIndexList, 0, this.numSites);									
				}
			}
		}
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			
//		}

	}

	class Swap{
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
