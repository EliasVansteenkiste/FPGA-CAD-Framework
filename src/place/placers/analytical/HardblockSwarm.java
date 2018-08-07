package place.placers.analytical;

import java.util.Random;
import java.lang.Thread;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
	
	private int minmumIteration;
	private int interval;
	
	private static final double W_H = 0.9;
	private static final double W_L = 0.4;
	
	private double c1, c2;
	
	private int velMaxSize;
	
	private final Random rand;
	private List<RunnableParticle> swarm;
	
//	private int iteration;
	private int globalBestIndex;
	private volatile double gBest;
	private List<Double> gBestList;
	private volatile int[] gBestBlockIdList;
	
	private final TimingTree timingTree;
	
	private final boolean printout = true;
//	private int numGBestRepeat;
	
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
	public void doPSO(Column column, BlockType blockType, int numParticles, double quality, double c1, double c2, int minimumIter, int interval){	
//		this.timingTree.start("pso column");
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.quality = quality;
		
		this.c1 = c1;
		this.c2 = c2;
		
		this.minmumIteration = minimumIter;
		this.interval = interval;
		
		this.sites = column.sites;
	
		this.numParticles = numParticles;
		if(!this.printout) System.out.println("[" + blockType + "" + column.index + ": " + column.blocks.size() + ", " + column.sites.length + "]");	
		
		this.doPSO();
//		System.out.println("!!!!!!!!!!!!!!!!!!!!!column" +blockType + column.index + "done!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//		System.out.println(this.gBest);
//		for(int index = 0; index < this.gBestBlockIdList.length; index++)
//			System.out.println(this.gBestBlockIdList[index]);
		this.setBlockLegal(this.gBestBlockIdList);
		
//		this.timingTree.time("pso column");
		
	}
	
	/*******************************
	* particle swarm optimization synchronized
	********************************/
	private void doPSO(){	
 		this.numBlocks = this.blocks.length;
		this.numSites = this.sites.length;
		
		this.velMaxSize = (int)Math.round(38.94 + 0.026 * this.numBlocks);//this.numSites; this.numBlocks// 
		this.gBestBlockIdList = new int[this.numSites];
		
		this.columnNets.clear();
		this.columnCrits.clear();
		
//		ExecutorService threadPool=  Executors.newFixedThreadPool(24);
//		List<Future<CostAndIndexList>> resultFutures= new ArrayList<>();//TODO
		
//		for(Block block:this.blocks){
//			for(Net net:block.nets){
//				this.columnNets.add(net);
//			}
//			for(Crit crit:block.crits){
//				this.columnCrits.add(crit);
//			}
//		}
//		
		for(Block block:this.blocks){
			for(Net net:block.mergedNetsMap.keySet()){
				this.columnNets.add(net);
			}
			for(Crit crit:block.crits){
				this.columnCrits.add(crit);
			}
		}
		
		this.initializeSwarm();
		
		this.gBestList = new ArrayList<>();
		
		
		this.gBest = Double.MAX_VALUE;
		this.updateGBestIfNeeded();

		double w;//weight decrease linearly
//		double c11, c22;
		
//		boolean printInfo = true;
//		if(printInfo) {
//			System.out.println();System.out.println();System.out.println();
//		}
//		double originalGbest = this.gBest;
		
//		List<List<Double>> pcosts = new ArrayList<>();
//		List<List<Double>> pBestcosts = new ArrayList<>();
//		List<Double> gBests = new ArrayList<>();
		double learningRate;
		
		boolean finish = false;
		int iteration = 0;
//		this.numGBestRepeat = 0;
		
		this.timingTree.start("pso processing");
		while(!finish) {
//		for(int iteration = 0;iteration < MAX_ITERATION; iteration++){
			if(!this.printout) System.out.println("\tPSO iteration: "+ iteration);
			
			w = W_H - (((double) iteration) / MAX_ITERATION) * (W_H - W_L);//w1
			learningRate = this.c1 - (((double) iteration) / MAX_ITERATION)*(this.c1 - this.c2);
//			gBests.add(this.gBest);
//			w=(W_H - W_L)*(iteration/MAX_ITERATION)*(iteration/MAX_ITERATION) + (W_L - W_H)*(2*iteration/MAX_ITERATION) + W_H;//w2
//			w = Math.pow(W_L*(W_H/W_L), 1/(1+10*iteration/MAX_ITERATION));//w3
					
//			c11 = COGNITIVE_H - (((double) iteration) / MAX_ITERATION) * (COGNITIVE_H - COGNITIVE_L); 
//			c22 = SOCIAL_L + (((double) iteration) / MAX_ITERATION) * (SOCIAL_H - SOCIAL_L);
			
			//*********************************threads using ExecutorService********************************
//			for(RunnableParticle p:this.swarm){
//				p.setParameters(w, c1*this.rand.nextDouble(), c2*this.rand.nextDouble(), this.gBestBlockIdList);		
//			}
//			try {
//				resultFutures = threadPool.invokeAll(this.swarm);
//			} catch (InterruptedException e) {
//				// Auto-generated catch block
//				e.printStackTrace();
//			}
//			for(Future<CostAndIndexList> f:resultFutures){
//				try {
//					CostAndIndexList result = f.get();
//					if(result.cost < this.gBest){
//						this.gBest = result.cost;
//						System.arraycopy(result.list, 0, this.gBestBlockIdList, 0, this.numSites);
////						System.out.println(result.index);
//					}
////					System.out.println(result.index + "\t" + result.cost);
//				} catch (InterruptedException | ExecutionException e) {
//					// Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			this.gBestList.add(this.gBest);
//			System.out.println("breakpoint here");
			//*********************************end threads using ExecutorService****************************
			
			//for threads***********************************************************************************
//			for(RunnableParticle p:this.swarm){
//				this.askBest(p);
//				p.setParameters(w, c1*this.rand.nextDouble(), c2*this.rand.nextDouble(), this.gBestBlockIdList);
//				p.resume();
//			}
//			this.gBestList.add(this.gBest);
//			if(printInfo)System.out.printf("%.0f\n", this.gBest);				
			//for threads**********************************************************************************
			
			
			//for single thread*******************************************************************************
			for(RunnableParticle p : this.swarm){
//				if(p.pBest < this.gBest){
////					System.out.println(p.pIndex);
//					numGBestRepeat = 0;
//					this.gBest = p.pBest;
//					System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
//				}else{
//					numGBestRepeat++;
////					System.out.println("same as previous" + numGBestRepeat);
//				}
//				if(p.pIndex == 0){
//					System.out.println("Iteration " + iteration + ": ");
//					System.out.println("\tp\t" + p.pCost + "\t" + Arrays.toString(p.blockIndexList) );
//					System.out.println("\tgb\t" + this.gBest + "\t" + Arrays.toString(this.gBestBlockIdList));
//					System.out.println("\tpb\t" + p.pBest +  "\t" + Arrays.toString(p.pBestIndexList));
//				}
				
				
				/////////////////////////get data for drawing swarm
//				if(iteration == 0){
//					List<Double> tmppcost = new ArrayList<>();
//					tmppcost.add(p.pCost);
//					pcosts.add(p.pIndex, tmppcost);
//					
//					List<Double> tmppBestcost = new ArrayList<>();
//					tmppBestcost.add(p.pBest);
//					pBestcosts.add(p.pIndex, tmppBestcost);			
//				}else{
//					pcosts.get(p.pIndex).add(p.pCost);
//					pBestcosts.get(p.pIndex).add(p.pBest);
//				}
				////////////////////////////////////////////////////
				
				//update velocity
				double newc1 = this.c1;
				double newc2 = this.c2;
				double alpha;
//				if(p.pCost != this.gBest){
//					alpha = (p.pBest - this.gBest)/(p.pCost - this.gBest);
////					if(p.pIndex == 0)System.out.println(alpha);
//					newc1 += 2*alpha;
//					newc2 -= 2*alpha;
//				}
//				p.updateVelocity(w, newc1*this.rand.nextDouble(), newc2*this.rand.nextDouble(), this.gBestBlockIdList);
				
				p.updateVelocity(w, learningRate*this.rand.nextDouble(), learningRate*this.rand.nextDouble(), this.gBestBlockIdList);
				
				
//				p.updateVelocity(w, learningRate, learningRate, this.gBestBlockIdList);
//				if(p.pIndex == 0){
//					for(Swap s:p.getVelopcity()){
//						System.out.println("\t" + s.fromIndex + "\t"+ s.toIndex);
//					}
//				}
				//update blockIndex list		
				p.updateLocations();					
				if(p.changed){				
					p.updateBlocksInfo();				
					//update pBest
//					p.pCost = p.getCost(p.pIndex);
					p.pCost = p.getCost();	
					if(p.pCost < p.pBest){
						p.pBest = p.pCost;						
						System.arraycopy(p.blockIndexList, 0, p.pBestIndexList, 0, this.numSites);									
					}
//					if(p.pCost < this.gBest){
//						this.gBest = p.pCost;
////						this.gBestList.add(this.gBest);
//						System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
//					}
				}			
			}
			
			this.updateGBestIfNeeded();
			this.gBestList.add(this.gBest);
			
//			if(this.gBestList.size() > 0){
//				if(this.gBest < this.gBestList.get(this.gBestList.size() - 1))
//					this.numGBestRepeat = 0;
//				else
//					this.numGBestRepeat++;
//			}
			
//			System.out.println(this.numGBestRepeat);
//			System.out.printf("%.0f\n", this.gBest);		
			//end for single thread****************************************************************************
			
			iteration++;
			int gbestsize = this.gBestList.size();
			if(gbestsize > this.minmumIteration){
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;				
				for(int i = gbestsize - this.interval; i < gbestsize; i++) {
					double cgb = this.gBestList.get(i);
					if(min > cgb) {
						min = cgb;
					}
					if(cgb > max) {
						max = cgb;
					}
				}				
				double ratio = max / min;
//				System.out.println(this.quality);
				if(ratio < this.quality) {//TODO
					finish = true;
				}else if(iteration == MAX_ITERATION){//doesn't matter MAX or MAX/2
					finish = true;
				}
			}else{
				finish = false;
			}
		}
//		System.out.println("gbest = " + gBests);
//		System.out.println("pcostLists = " + pcosts);
//		System.out.println("pBestcostLists = " + pBestcosts);
//		System.out.println();
		
		//TODO DRIES
//		double finalGbest = this.gBest;
//		if(printInfo) {
//			System.out.println();System.out.println();System.out.println();
//			System.out.printf("improvement: %.1f\n", (100 * ((originalGbest/finalGbest) - 1)));
//		}
		//TODO DRIES
		
//		this.stopThreads();
//		threadPool.shutdown();
		this.timingTree.time("pso processing");
	}
	private Swap permutator(){//TODO try this
		
		int from = this.rand.nextInt(this.numSites);
		int to = this.rand.nextInt(this.numSites);
		while(to == from){
			to = this.rand.nextInt(this.numSites);
		}
//		System.out.println(from + " " + to); 
		Swap mutation = new Swap(from, to);
		return mutation;
	}
	private void stopThreads(){
		for(RunnableParticle p:this.swarm){
			p.stop();
		}
	}
	private void askPbest(RunnableParticle p, Future future){
		while(!future.isDone()){}
		double cost = 0;
		try {
			cost = (Double) future.get();
		} catch (InterruptedException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		if(cost < this.gBest){
			this.gBest = cost;
			System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
		}
	}
	private void askBest(){
		for(RunnableParticle p:this.swarm){
			if(p.paused() && p.pBest < this.gBest){
				this.gBest = p.pBest;
				System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
			}
		}
	}
	private void askBest(RunnableParticle p){
		while(!p.paused()){} //WAIT //TODO OPTIMIZE
		if(p.pBest < this.gBest){
			this.gBest = p.pBest;
			System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
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
	
	private Site getSite(int[] list, int value){
		int position = 0;
		for(int pos = 0; pos < list.length; pos++){
			if(list[pos] == value) position = pos;
		}
		return this.sites[position];
	}
	
	private void updateGBestIfNeeded(){
		for(RunnableParticle p:this.swarm){
			if(p.pCost < this.gBest){
//				System.out.println(p.pIndex);
//				numGBestRepeat  = 0;
				this.globalBestIndex = p.pIndex;
				this.gBest = p.pCost;
				System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
			}	
		}
	}
	//initialize swarm
	private void initializeSwarm(){
		this.swarm = new ArrayList<>();
//		this.particleThreadPool.clear();
		
		this.addBaseLineParticle();
		
		for(int i = 1; i < this.numParticles; i++){
			this.swarm.add(this.generateParticleRandomly(i));
//			this.initializeParticlesShuffle(1, this.numParticles);
		}
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
		
//		this.duplicateData(0);///////////////////////TODO
		
		baseLineParticle.setPNets(this.columnNets);
		baseLineParticle.setPCrits(this.columnCrits);
//		baseLineParticle.pCost = baseLineParticle.getCost(0);
		baseLineParticle.pCost = baseLineParticle.getCost();
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
	private RunnableParticle generateParticleRandomly(int i){
				
			this.randomlyPlaceBlocks();

//			this.duplicateData(i);

			
			int velLength = this.rand.nextInt(this.velMaxSize);
			List<Swap> vel = new ArrayList<Swap>();
			for(int m = 1; m < velLength; m++){
				vel.add(new Swap(0, 0));	
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
			
//			particle.pCost = particle.getCost(i);
			particle.pCost = particle.getCost();
			//initial pbest info
			particle.pBest = particle.pCost;
			System.arraycopy(particle.blockIndexList, 0, particle.pBestIndexList, 0, this.numSites);//initial pbest location		
			
			return particle;
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
	private void initializeParticlesShuffle(int startPIndex, int endPIndex){
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
}
