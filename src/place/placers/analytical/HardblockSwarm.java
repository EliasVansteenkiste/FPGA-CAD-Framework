package place.placers.analytical;

import java.util.Random;
import java.lang.Thread;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
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
import place.placers.analytical.RunnableParticle;
import place.placers.analytical.RunnableParticle.Swap;

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
	private static final double W_H = 0.9;
	private static final double W_L = 0.4;
	
	private int velMaxSize;
	
	private final Random rand;
	private List<RunnableParticle> swarm;
	
//	private int iteration;
	
	private double gBest;
	
	private List<Double> gBestCostHistory;
	private int gBestHistoryIndex;
	private int[] gBestBlockIdList;
	
	private final TimingTree timingTree;
	
	private final boolean printout = true;
	
	HardblockSwarm(int seed){
		this.rand = new Random(seed);
		this.timingTree = new TimingTree(false);
		this.columnNets = new HashSet<>();
		this.columnCrits = new HashSet<>();
		
		this.swarm = new ArrayList<>();	
	}
	
	//legalize io block
	public void doPSO(Block[] ioBlocks, Site[] ioSites, BlockType blockType, int numParticles, double quality){

		this.blocks = ioBlocks;
		this.sites = ioSites;
		
		this.quality = quality;
		
		this.numParticles = numParticles;
		
		this.doPSO();
		this.setBlockLegal(this.gBestBlockIdList);
	}
	
	//legalize hard block
	public void doPSO(Column column, BlockType blockType, int numParticles, double quality){	
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.quality = quality;
		
		this.sites = column.sites;
	
		this.numParticles = numParticles;
		if(!this.printout) System.out.println("[" + blockType + "" + column.index + ": " + column.blocks.size() + ", " + column.sites.length + "]");	
		
		this.doPSO();
		this.setBlockLegal(this.gBestBlockIdList);
		
		if(!this.printout){
			for(int i = 0; i < this.gBestCostHistory.size(); i++){
				System.out.println(i + " " + String.format("%.2f", this.gBestCostHistory.get(i)));
			}
		}
	}
	
	/*******************************
	* particle swarm optimization synchronized??TODO
	********************************/
	private void doPSO(){	
		this.numBlocks = this.blocks.length;
		this.numSites = this.sites.length;
		
		this.velMaxSize = (int)Math.round(38.94 + 0.026 * this.numBlocks);//(int)Math.round(28.376 + 0.026 * this.numBlocks);// (int) Math.round(Math.pow(this.numBlocks, 4/3));
		this.gBestBlockIdList = new int[this.numSites];
		
		this.columnNets.clear();
		this.columnCrits.clear();
		
		ExecutorService executor =  Executors.newFixedThreadPool(this.numParticles); 
		
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
		
//		this.iteration = 0;
//		boolean finalIteration = false;
		
		double w;//weight decrease linearly
		double c1, c2;
		
		for(int iteration = 0;iteration < MAX_ITERATION; iteration++){
//		while(!finalIteration){
			if(!this.printout) System.out.println("\tPSO iteration: "+ iteration);
			
			w = W_H - (((double) iteration) / MAX_ITERATION) * (W_H - W_L);//w1
//			w=(W_H - W_L)*(iteration/MAX_ITERATION)*(iteration/MAX_ITERATION) + (W_L - W_H)*(2*iteration/MAX_ITERATION) + W_H;//w2
//			w = Math.pow(W_L*(W_H/W_L), 1/(1+10*iteration/MAX_ITERATION));//w3
			c1 = COGNITIVE_H - (((double) iteration) / MAX_ITERATION) * (COGNITIVE_H - COGNITIVE_L); 
			c2 = SOCIAL_L + (((double) iteration) / MAX_ITERATION) * (SOCIAL_H - SOCIAL_L);
			
			//for threads***********************************************************************************
			if(iteration == 0){
				for(RunnableParticle p:this.swarm){
					p.setParameters(w, c1*this.rand.nextDouble(), c2*this.rand.nextDouble(), this.gBestBlockIdList);
					p.newThreadStart();
				}
			}else{
				for(RunnableParticle p:this.swarm){
					p.setParameters(w, c1*this.rand.nextDouble(), c2*this.rand.nextDouble(), this.gBestBlockIdList);
					p.resume();
				}
			}
			
//			this.askBest();
			this.waitSwarmToFinish();			
			this.getGlobalBest();
			
			//for mulithreds**********************************************************************************
			//for single thread*******************************************************************************
//			for(RunnableParticle p : this.swarm){
//				//update velocity
//				p.updateVelocity(w, c1*this.rand.nextDouble(), c2*this.rand.nextDouble(), this.gBestBlockIdList);			
//				//update blockIndex list		
//				p.updateLocations();					
//				if(p.changed){				
//					p.updateBlocksInfo();					
//					//update pBest
//					p.pCost = p.getCost(p.pIndex);					
//					if(p.pCost < p.pBest){					
//						p.pBest = p.pCost;						
//						System.arraycopy(p.blockIndexList, 0, p.pBestIndexList, 0, this.numSites);									
//					}
//					
//					//异步更新
//					/*if(p.pCost < this.gBest){
//						this.gBest = p.pCost;
//						System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
//					}*/					
//				}			
//			}
//			this.getGlobalBest();
			//end for single thread****************************************************************************
			
//			this.iteration++;
//			finalIteration = this.finalIteration(this.gBest);
		}
		for(RunnableParticle p:this.swarm){
			p.stop();
		}
	}
	private void askBest(){
		for(RunnableParticle p:this.swarm){
			if(p.paused() && p.pBest < this.gBest){
				this.gBest = p.pBest;
			}
		}
	}
	private void waitSwarmToFinish(){
//		System.out.println("wait to finish");
		boolean allFinished = false;
		while(!allFinished){
			allFinished = true;
			for(RunnableParticle p:this.swarm){
				if(!p.paused()){
					allFinished = false;
				}
			}
		}
	}
	@SuppressWarnings("unused")
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
		for(RunnableParticle particle : this.swarm){
			if(particle.pBest < minValue){
				pos = particle.pIndex;
				minValue = particle.pBest;
			}
		}
		return pos;
	}
	
	//initialize swarm
	private void initializeSwarm(){
		this.swarm = new ArrayList<>();
//		this.particleThreadPool.clear();
		this.addBaseLineParticle();
		
		this.initializeParticlesRandomly(1, this.numParticles);
//		this.initializeParticlesShuffle(1, this.numParticles, this.doneSignal);
	}
	private void addBaseLineParticle(){
		RunnableParticle baseLineParticle = new RunnableParticle(0, this.blocks, this.sites, this.velMaxSize);///, latch);
		
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
			
			RunnableParticle particle = new RunnableParticle(i, this.blocks, this.sites, this.velMaxSize);//, latch);
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
	private void initializeParticlesShuffle(int startPIndex, int endPIndex, CountDownLatch latch){
		for(int i = startPIndex; i < endPIndex; i++){	
			int[] shuffledBlockIndexList = this.shuffleArray(this.swarm.get(0).blockIndexList);
			this.setBlockLegal(shuffledBlockIndexList);
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
			
			RunnableParticle particle = new RunnableParticle(i, this.blocks, this.sites, this.velMaxSize);//, latch);
			particle.setVelocity(vel);
			System.arraycopy(shuffledBlockIndexList, 0, particle.blockIndexList, 0, this.numSites);
			particle.setPNets(this.columnNets);
			particle.setPCrits(this.columnCrits);			
			
			particle.pCost = particle.getCost(i);
			//initial pbest info
			particle.pBest = particle.pCost;
			System.arraycopy(particle.blockIndexList, 0, particle.pBestIndexList, 0, this.numSites);//initial pbest location		
			this.swarm.add(particle);	
		}
	}
	private int[] shuffleArray(int[] baseLineIndexList){
		int length = baseLineIndexList.length;
		int[] initialIndexList = new int[length];
		int idi = length;
		int tmp, idj;
		System.arraycopy(baseLineIndexList, 0, initialIndexList, 0, length);
		while(idi > 0){
			idj = (int)Math.floor(this.rand.nextDouble()*idi--);//Math.floor(Math.random()*idi--);//
			tmp = initialIndexList[idi];
			initialIndexList[idi] = initialIndexList[idj];
			initialIndexList[idj] = tmp;
		}
		return initialIndexList;
	}
	private void setBlockLegal(int[] blockIndexList){
		for(Block block : this.blocks){
			Site site = this.getSite(blockIndexList, block.index);
			block.setSite(site);
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
}
