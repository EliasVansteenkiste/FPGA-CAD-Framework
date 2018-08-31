package place.placers.analytical;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import place.circuit.architecture.BlockType;
import place.placers.analytical.HardblockSwarmLegalizer.Block;
import place.placers.analytical.HardblockSwarmLegalizer.Column;
import place.placers.analytical.HardblockSwarmLegalizer.Crit;
import place.placers.analytical.HardblockSwarmLegalizer.Net;
import place.placers.analytical.HardblockSwarmLegalizer.Site;
import place.util.TimingTree;
import place.placers.analytical.Particle;
import place.placers.analytical.Particle.Swap;

public class HardblockSwarmOptimization {
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

	private double c1, c2;
	private double forPbest, forGbest; 
	
	private int velMaxSize;
	
	private final Random rand;
	private final Random randControl;
	private List<Particle> swarm;

	private volatile double gBest;
	private int gBestPindex;
	private List<Double> gBestList;
	private volatile int[] gBestBlockIdList;
	double[] allpCosts;
	double[] distanceForEachP; 
	
	private final boolean printout = false;

	HardblockSwarmOptimization(int seed){
		this.rand = new Random(seed);
		this.randControl = new Random(seed+10);

		this.columnNets = new HashSet<>();
		this.columnCrits = new HashSet<>();
		
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
	public void doPSO(Column column, BlockType blockType, int numParticles, double quality, double c1, double c2, double forPbest, double forGbest, int minimumIter, int interval){
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.quality = quality;
		
		this.numParticles = numParticles;
		
		this.c1 = c1;
		this.c2 = c2;
		this.forPbest = forPbest;
		this.forGbest = forGbest;
		
		this.allpCosts = new double[this.numParticles];
		this.distanceForEachP = new double[this.numParticles];
		
		this.minmumIteration = minimumIter;
		this.interval = interval;
		
		this.sites = column.sites;
			
		if(this.printout) System.out.println("[" + blockType + "" + column.index + ": " + column.blocks.size() + ", " + column.sites.length + "]");	
				
		this.doPSO();

		this.setBlockLegal(this.gBestBlockIdList);	
	}
	
	/*******************************
	* particle swarm optimization synchronized
	********************************/
	private void doPSO(){	
 		this.numBlocks = this.blocks.length;
		this.numSites = this.sites.length;
		
		this.velMaxSize = (int)Math.round(38.94 + 0.026 * this.numBlocks);//this.numSites; // (int)Math.round(20*Math.pow(this.numBlocks, 4/3));//
		this.gBestBlockIdList = new int[this.numSites];
		
		this.columnNets.clear();
		this.columnCrits.clear();

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

		double w;
		double p1, p2;
			
//		List<List<Double>> pcosts = new ArrayList<>();
//		List<List<Double>> pBestcosts = new ArrayList<>();
//		List<Double> gBests = new ArrayList<>();
		
		double learningRate;
		
		boolean finish = false;
		int iteration = 0;
		
		while(!finish) {
//		for(int iteration = 0;iteration < MAX_ITERATION; iteration++){
			for(Particle p: this.swarm){
				this.allpCosts[p.pIndex] = p.pCost;
			}
			double f = this.evolutionaryFactor(this.allpCosts);

			w = 1.0 / (1 + 1.5*Math.exp(-2.6*f));
			
			if(this.printout) System.out.println(f + "\t" + w + "\t" + this.gBest);
			learningRate = this.c1 - (((double) iteration) / MAX_ITERATION)*(this.c1 - this.c2);
			
			
//			if(this.printout){
//				System.out.println("Iteration " + iteration + ": ");
//			}	
			
			//for single thread*******************************************************************************
			for(Particle p : this.swarm){
				
//				if(this.printout){
//					if(p.pIndex == 0){
//						
//						System.out.println("\tp\t" + p.pCost + "\t" + Arrays.toString(p.blockIndexList) );
//						System.out.println("\tgb\t" + this.gBest + "\t" + Arrays.toString(this.gBestBlockIdList));
//						System.out.println("\tpb\t" + p.pBest +  "\t" + Arrays.toString(p.pBestIndexList));
//						System.out.println();
//					}
//				}			
				
				/////////////////////////get data for drawing swarm
//				if(!this.printout){
//					if(iteration == 0){
//						List<Double> tmppcost = new ArrayList<>();
//						tmppcost.add(p.pCost);
//						pcosts.add(p.pIndex, tmppcost);
//						
//						List<Double> tmppBestcost = new ArrayList<>();
//						tmppBestcost.add(p.pBest);
//						pBestcosts.add(p.pIndex, tmppBestcost);			
//					}else{
//						pcosts.get(p.pIndex).add(p.pCost);
//						pBestcosts.get(p.pIndex).add(p.pBest);
//					}
//				}
				////////////////////////////////////////////////////
				
				//update velocity
				
				p1 = learningRate*this.rand.nextDouble();
				p2 = learningRate*this.rand.nextDouble();
				
//				p.setParameters(w, p1, p2, this.gBestBlockIdList);
				
				double probility = this.randControl.nextDouble();
				if(probility > this.forPbest){
					p.setParameters(0, p1, 0, this.gBestBlockIdList);
				}else if(probility > this.forGbest){
					p.setParameters(0, 0, p2, this.gBestBlockIdList);
				}else{
					p.setParameters(w, 0, 0, this.gBestBlockIdList);
				}
				
				p.doWork();
					
			}
			
			this.updateGBestIfNeeded();
			this.gBestList.add(this.gBest);
						
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
				double ratio = max / (min);
//				System.out.println(ratio);
				if(ratio < this.quality) {//TODO
					finish = true;
				}else if(iteration == MAX_ITERATION){
					finish = true;
				}
			}else{
				finish = false;
			}
			
//			if(zerofnum > 20) finish = true;
		}
		
//		if(!this.printout){
//			System.out.println("gbest = " + gBests);
//			System.out.println("pcostLists = " + pcosts);
//			System.out.println("pBestcostLists = " + pBestcosts);
//			System.out.println();
//		}
	}
	private double evolutionaryFactor(double[] array){
		double f = 0;
		double sumDis;
		
		for(int a = 0; a < this.numParticles; a++){
			sumDis = 0;
			for(int b = 0; b < this.numParticles; b++){
				if(a != b) 
					sumDis += Math.abs(array[a] - array[b]);
			}
			this.distanceForEachP[a] = sumDis / (this.numParticles - 1);
		}
		double disgBest = this.distanceForEachP[this.gBestPindex];
		Arrays.sort(this.distanceForEachP);
		
		f = (disgBest - this.distanceForEachP[0])/(this.distanceForEachP[this.numParticles - 1] - this.distanceForEachP[0] + 0.0000001);
		
		return f;
	}

	private Site getSite(int[] list, int value){
		int position = 0;
		for(int pos = 0; pos < list.length; pos++){
			if(list[pos] == value) position = pos;
		}
		return this.sites[position];
	}
	
	private void updateGBestIfNeeded(){
		for(Particle p:this.swarm){
			if(p.pCost < this.gBest){
				this.gBest = p.pCost;
				this.gBestPindex = p.pIndex;
				System.arraycopy(p.blockIndexList, 0, this.gBestBlockIdList, 0, this.numSites);
			}	
		}
	}
	//initialize swarm
	private void initializeSwarm(){
		this.swarm = new ArrayList<>();
		
		this.addBaseLineParticle();
		
		for(int i = 1; i < this.numParticles; i++){
			this.swarm.add(this.generateParticleRandomly(i));
		}
	}
	private void addBaseLineParticle(){
		Particle baseLineParticle = new Particle(0, this.blocks, this.sites, this.velMaxSize);///, latch);
		
		int j = 0;
		for(Site site : this.sites){
			if(site.hasBlock()){
				baseLineParticle.blockIndexList[j] = site.block.index;
			}else baseLineParticle.blockIndexList[j] = -1;
			baseLineParticle.pBestIndexList[j] = baseLineParticle.blockIndexList[j];
			j++;
		}
		
//		this.duplicateData(0);
		
		baseLineParticle.setPNets(this.columnNets);
		baseLineParticle.setPCrits(this.columnCrits);

		baseLineParticle.pCost = baseLineParticle.getCost();
		baseLineParticle.pBest = baseLineParticle.pCost;
		
		this.swarm.add(baseLineParticle);
		
//		if(!this.printout) System.out.println("BP cost: " + String.format("%.2f",  baseLineParticle.pCost) + " ");
//		
//		if(!this.printout) System.out.println(baseLineParticle.pCost);
//		
//		if(!this.printout){
//			System.out.println("particle 0");
//			for(int a = 0; a < this.numSites; a++){
//				System.out.println("\t" + a + "\t" + baseLineParticle.blockIndexList[a]);
//			}
//		}	
	}

	private Particle generateParticleRandomly(int i){
				
			this.randomlyPlaceBlocks();
			
//			this.duplicateData(i);
			
			int velLength = this.rand.nextInt(this.velMaxSize);
			List<Swap> vel = new ArrayList<>();
			for(int m = 1; m < velLength; m++){
				Swap s = new Swap(0, 0);
				vel.add(s);	
			}
			
			Particle particle = new Particle(i, this.blocks, this.sites, this.velMaxSize);//, latch);
			particle.setVelocity(vel);
			for(int m = 0; m < this.numSites; m++){
				if(this.sites[m].hasBlock()) particle.blockIndexList[m] = this.sites[m].block.index;
				else particle.blockIndexList[m] = -1;
			}
			particle.setPNets(this.columnNets);
			particle.setPCrits(this.columnCrits);			
			
			particle.pCost = particle.getCost();

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
	private void setBlockLegal(int[] blockIndexList){
		for(Block block : this.blocks){
			Site site = this.getSite(blockIndexList, block.index);
			block.setSite(site);
			site.setBlock(block);
			block.setLegalXY(site.column, site.row);
		}
	}
	private void duplicateData(int i){
		for(Block b:this.blocks){
			b.duplicateData(i);
		}
	}
}
