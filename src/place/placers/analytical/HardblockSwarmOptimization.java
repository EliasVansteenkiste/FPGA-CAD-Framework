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
	
	private int interval;

	private double c1;
	private double forPbest, forGbest; 
	
	private int velMaxSize;
	
	private final Random rand;
	private final Random randControl;
	private List<Particle> swarm;

	private volatile double gBest;
	private int gBestPindex;
	private List<Double> evolFactors;
	private volatile int[] gBestBlockIdList;
	double[] allpCosts;
	double[] distanceForEachP; 
	

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
	public long doPSO(Column column, BlockType blockType, int numParticles, double quality, double c1, double forPbest, double forGbest, int interval){
		long start = System.nanoTime();
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.quality = quality;
		
		this.numParticles = numParticles;
		
		this.c1 = c1;
		this.forPbest = forPbest;
		this.forGbest = forGbest;
		
		this.allpCosts = new double[this.numParticles];
		this.distanceForEachP = new double[this.numParticles];
		
		this.interval = interval;
		
		this.sites = column.sites;
		
		this.doPSO();
//		System.out.println("\n\n");
		this.setBlockLegal(this.gBestBlockIdList);	
		
		return (System.nanoTime() - start);
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
		
		this.evolFactors = new ArrayList<>();
		
		
		this.gBest = Double.MAX_VALUE;
		this.updateGBestIfNeeded();

		double w;
		double p1, p2;
		
		double learningRate1, learningRate2;
		
		boolean finish = false;
		int iteration = 0;
		
		while(!finish) {
			
			for(Particle p: this.swarm){
				this.allpCosts[p.pIndex] = p.pCost;
			}
			double f = this.evolutionaryFactor(this.allpCosts);

			w = 1.0 / (1 + 1.5*Math.exp(-2.6*f));
			double delta = f-w;
			
			learningRate1 = this.c1 + w*delta;
			learningRate2 = this.c1 - w*delta;

			for(Particle p : this.swarm){						
								
				p1 = learningRate1*this.rand.nextDouble();
				p2 = learningRate2*this.rand.nextDouble();
				
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
			this.evolFactors.add(f);//for stop criterion based on evolutionary factor, adaptive controller
						
			iteration++;
			int gbestsize = this.evolFactors.size();
			if(gbestsize > this.interval){
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;				
				for(int i = gbestsize - this.interval; i < gbestsize; i++) {
					double cgb = this.evolFactors.get(i);
					if(min > cgb) {
						min = cgb;
					}
					if(cgb > max) {
						max = cgb;
					}
				}				
				double ratio = max / (min+1);

				if(ratio < this.quality ) {
					finish = true;
				}else if(iteration == 100){
					finish = true;
				}
			}else{
				finish = false;
			}
			
		}
		
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
		
		f = (disgBest - this.distanceForEachP[0])/(this.distanceForEachP[this.numParticles - 1] - this.distanceForEachP[0] + 1E-9);
		
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
		this.swarm = new ArrayList<>(this.numParticles);
		
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
//			particle.setVelocity(vel);
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
