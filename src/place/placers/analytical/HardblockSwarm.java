package place.placers.analytical;

import java.util.Random;
import java.util.Set;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private static final int MAX_ITERATION = 500;

	private static final double COGNITIVE_L = 0.01;
	private static final double COGNITIVE_H = 2;//1.6;

	private static final double SOCIAL_L = 0.01;
	private static final double SOCIAL_H = 2;
	private static final double W_UPPERBOUND = 0.9;
	private static final double W_LOWERBOUND = 0.4;
	
	private int velMaxSize;
	
	private final Random rand;
	private List<Particle> swarm;
	
	private double gBest;

	private int[] gBestIndexList;
	
	private List<Swap> swaps;
	private List<Swap> newVel;
	
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
	}
	////////////////////////////////////TO TEST RANDOMLY PLACEMENT FOR EACH COLUMN/////////////
	public void doRandomly(Column column, BlockType blockType){
		this.blockHeight = blockType.getHeight();
		this.legalcordinateX = column.coordinate;
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.numBlocks = blocks.length;
		this.numSites = column.sites.length;
		this.sites = new Site[this.numSites];
		System.arraycopy(column.sites, 0, this.sites, 0, this.numSites);
		this.randomlyPlaceBlocks();
	}
	///////////////////////////////////////////////////////////////////////////////////////////
	
	//legalize hard block
	public void doPSO(Column column, BlockType blockType, int numParticles){
		this.blockHeight = blockType.getHeight();
		this.legalcordinateX = column.coordinate;
		
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		
		if(!this.printout){
			System.out.println("blocks' initial order in the columnBlocks");
			for(Block block:this.blocks){
				System.out.print(block.index + " ");
			}
			System.out.println();
		}
		
		this.numBlocks = blocks.length;
		this.numSites = column.sites.length;
		
		this.velMaxSize = (int)Math.round(38.94 + 0.026 * this.numBlocks);
		
		
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
//		System.out.println(blockType + "" + column.index + " -> " + this.numBlocks);
		
		this.columnNets.clear();
		this.columnCrits.clear();

		
//		for(Block block:this.blocks){
//			for(Net net:block.nets){
//				this.columnNets.add(net);
//			}
//			for(Crit crit:block.crits){
//				this.columnCrits.add(crit);
//			}
//		}
		for(Block block:this.blocks){
			for(Net net:block.mergedNetsMap.keySet()){
				net.setTotalNum(block.mergedNetsMap.get(net));
				this.columnNets.add(net);
			}
			for(Crit crit:block.crits){
				this.columnCrits.add(crit);
			}
		}
//		for(Block block:this.blocks){
//			block.getAttachedBlocksMap();
//		}

//		int size = 0;
//		for(Block block:this.blocks){
//			for(Net n:block.mergedNetsMap.keySet()){
//				size += block.mergedNetsMap.get(n);
//			}
//		}
//		for(Net n:this.columnNets){
//			size += n.totalNum;
//		}
//		System.out.println(size);

		//////TO TEST THE MERGING OF NETS FOR EACH BLOCK///////////////////////////////////////////////
//		for(Block block:this.blocks){
//			System.out.println("for block " + block.index + " with " + block.nets.size() + " nets");
//			for(Net net:block.nets){
//				List<Integer> netBlockIndex = new ArrayList<>();
//				for(Block b:net.blocks){
//					netBlockIndex.add(b.index);
//				}
//			System.out.println(net.index + " -> " + netBlockIndex);
//			}
//		}
//		for(Block block:this.blocks){
//			System.out.println("for " + block.index + " with " + block.mergedNetsMap.size());
//			block.getAttachedBlocksMap();
//			for(Block tmp:block.attachedBlocksMap.keySet()){
//				System.out.println(tmp.index + " -> " + block.attachedBlocksMap.get(tmp));
//			}
//			for(Net net:block.mergedNetsMap.keySet()){
//				List<Integer> blockIndexList = new ArrayList<>();
//				List<Integer> yIndexList = new ArrayList<>();
//				for(Block b:net.blocks){
//					blockIndexList.add(b.index);
//					yIndexList.add(b.legalY);
//				}
//				System.out.println(net.index + " -> " + net.totalNum + " -> " + blockIndexList + " -> " + yIndexList);
//			}
//			System.out.println();
//		}
//		System.out.println("break");
		
//		int sum = 0;
//		for(Net net:this.columnNets){
//			sum += net.totalNum;
//		}
//		DecimalFormat df = new DecimalFormat("0.00");
//		String reduction = df.format((float)(sum - this.columnNets.size())/sum);
//
//		System.out.println(sum + "\t" + "->" + "\t" + this.columnNets.size() + "\t" + reduction);
		////// END TO TEST THE MERGING OF NETS FOR EACH BLOCK///////////////////////////////////////////////
		
//		System.out.println(blockType + "" + column.index + " numNets -> " + this.columnNets.size() + " numCrits -> " + this.columnCrits.size() + " numBlocks -> " + this.numBlocks);
		this.numParticles = numParticles;
		
		this.gBestIndexList = new int[this.numSites];
		
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
//		this.initialization();
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
			if(!this.printout) System.out.println("PSO iteration: "+ iteration);//TODO remove
			
			w = W_UPPERBOUND - (((double) iteration) / MAX_ITERATION) * (W_UPPERBOUND - W_LOWERBOUND);
			r1 = COGNITIVE_H - (((double) iteration) / MAX_ITERATION) * (COGNITIVE_H - COGNITIVE_L); 
			r2 = SOCIAL_L + (((double) iteration) / MAX_ITERATION) * (SOCIAL_H - SOCIAL_L);
			for(Particle p : this.swarm){							
				//update velocity
				if(!this.printout)
					System.out.println("for particle " + p.pIndex);
				
				if(!this.printout) System.out.println("w-> " + w + " r1-> " + r1 + " r2-> " + r2);
				this.updateVelocity(p.getVelocity(), w, r1*this.rand.nextDouble(), r2*this.rand.nextDouble(), p.blockIndexList, p.pBestIndexList, this.gBestIndexList);
				
				p.setVelocity(this.newVel);
				
				//update blockIndex list
				this.updateLocations(p.pIndex, p.blockIndexList, this.newVel);	
//				double sumDelta = this.updateLocations(p.pIndex, p.blockIndexList, this.newVel);
//				System.out.println(sumDelta);
				if(!this.printout){
					for(int a = 0; a < this.numSites; a++){
						System.out .println(a + " " + p.blockIndexList[a]);
					}
				}
				
				//set blocks's tmpLegal by connecting each site with each block in the order from indexList
				for(Block block : this.blocks){
					int siteIndex = this.getSiteIndex(p.blockIndexList, block.index);
					block.setLegalXY(this.legalcordinateX, siteIndex * this.blockHeight +1);
//					block.setLegalXYs(p.pIndex, this.legalcordinateX, siteIndex * this.blockHeight +1);//deal with legalXs, minXs, minYs
				}
		
				//update pBest
				p.pCost = p.getCost();//TODO
//				p.pCost = this.getCostBasedOnBlock();
//				p.pCost = p.getCost(p.pIndex);//calculation based on int[] minXs maxXs minYs maxYs
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
					System.out.println(String.format("%.2f", p.pCost));
//					System.out.println("pBest\t" + String.format("%.2f",  this.pBest[p.pIndex])); 	
				}
			}
			this.getGlobalBest();
			if(!this.printout) System.out.println("gBest\t" + String.format("%.2f",  this.gBest));
		}
	}
	private void setBlockLegal(){
//		System.out.println(" psogBest -> " + String.format("%.2f",  this.gBest));
		if(!this.printout){
			System.out.println("legalized blocks' order");
			for(int m = 0; m < this.numSites; m++){
				System.out.print(this.gBestIndexList[m] + " ");
			}
			System.out.println();
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
		double minValue = Double.MAX_VALUE;
		for(Particle particle : this.swarm){
			if(particle.pBest < minValue){
				pos = particle.pIndex;
				minValue = particle.pBest;
			}
		}
		return pos;
	}
	//Initialization based on blocks' importance
	private void initialization(){
		this.swarm.clear();
		List<Block> sortedBlocks = new ArrayList<>();
		Collections.addAll(sortedBlocks, this.blocks);
		for(Block block:this.blocks){
			block.updateCriticalityBasedonMap();
		}
		Collections.sort(sortedBlocks, new Comparator<Block>(){
			public int compare(Block b1, Block b2){
				return b1.compareTo(b2);
			}
		});
		for(Block b:sortedBlocks){
			System.out.println(b.index + " -> " + b.criticality);
		}
		System.out.println();
	}
	
	//initialize swarm
	private void initializeSwarm(){
		this.swarm.clear();
		
		Particle baseLineParticle = new Particle(0, this.numSites);
		baseLineParticle.setPNets(this.columnNets);
		baseLineParticle.setPCrits(this.columnCrits);
		baseLineParticle.pCost = baseLineParticle.getCost();
//		baseLineParticle.pCost = this.getCostBasedOnBlock();
		baseLineParticle.pBest = baseLineParticle.pCost;
		if(!this.printout) System.out.print("BP cost: " + String.format("%.2f",  baseLineParticle.pCost) + " ");
		int j = 0;
		for(Site site : this.sites){
			if(site.hasBlock()){
				baseLineParticle.blockIndexList[j] = site.block.index;
				site.block.setLegalXY(site.column, site.row);
			}else baseLineParticle.blockIndexList[j] = -1;
			j++;
		}
		this.swarm.add(baseLineParticle);
		
		if(!this.printout) System.out.println(baseLineParticle.pCost);
		if(!this.printout){
			System.out.println("particle 0");
			for(int a = 0; a < this.numSites; a++){
				System.out.println("\t" + a + "\t" + baseLineParticle.blockIndexList[a]);
			}

		}		
		for(int i = 1; i < this.numParticles; i++){		
			this.timingTree.start("randomly assign blocks");
			this.randomlyPlaceBlocks();
			this.timingTree.time("randomly assign blocks");
			
//			for(Block block:this.blocks){
//				block.duplicateData(i);
//			}

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
			
			int velLength = this.rand.nextInt(this.velMaxSize);
			List<Swap> vel = new ArrayList<Swap>();
			for(int m = 1; m < velLength; m++){
				Swap v = new Swap();
//				v.setFromIndex(this.rand.nextInt(this.numSites));
//				v.setToIndex(this.rand.nextInt(this.numSites));
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
			particle.setPNets(this.columnNets);
			particle.setPCrits(this.columnCrits);
			double tmpCost = particle.getCost();
//			particle.pCost = this.getCostBasedOnBlock();
			particle.pCost = tmpCost;
			//initial pbest info
			particle.pBest = particle.pCost;
//			System.arraycopy(particle.blockIndexList, 0, particle.pBestIndexList, 0, this.numSites);//initial pbest location
//			double test = this.getCost();// TEST if p.getCost() works 
//			System.out.println(String.format("%.2f", tmpCost));// + " " + String.format("%.2f", test));
			
			this.swarm.add(particle);
		}
	}
	private double getCostBasedOnBlock(){
		double cost = 0.0;
		double conn = 0.0;
		double timing = 0.0;
		
		for(Block block:this.blocks){
			for(Block attachedBlock:block.attachedBlocksMap.keySet()){
				double deltaY = Math.abs(block.legalY - attachedBlock.legalY);
				conn += deltaY*block.attachedBlocksMap.get(attachedBlock);
			}
		}
		for(Crit crit:this.columnCrits) timing += crit.timingCost();
		cost = conn + timing;
		return cost;
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
//			block.setSite(site);// FOR doRandomly()////////////
//			block.setLegal(site.column, site.row);
			block.setLegalXY(site.column, site.row);	
		}
	}
	private void getGlobalBest(){
		int bestParticleIndex = this.getMinPbestIndex();
		this.gBest = this.swarm.get(bestParticleIndex).pBest;
		if(!this.printout)System.out.println("best index " + bestParticleIndex + "\t" + String.format("%.2f", this.gBest));
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
				System.out.println("weightedVel\tfrom\tto");
				for(int p = 0; p < weightedVel.size(); p++){
					System.out.println("\t" + p + "\t" + weightedVel.get(p).fromIndex + "\t" + weightedVel.get(p).toIndex);
				}
			}else System.out.println("weightedVel part is null");
		}

		this.getSwapSequence(pBestLocation, pLocation);
//		this.getSwapsStartRandomly(this.rand.nextInt(this.numSites), pBestLocation, pLocation);
		
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
//		this.getSwapsStartRandomly(this.rand.nextInt(this.numSites), gBestLocation, pLocation);
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
//		if(weightedVel != null) newVel.addAll(weightedVel);
//		if(cognitiveVel != null) newVel.addAll(cognitiveVel);
//		if(socialVel != null) newVel.addAll(socialVel);
		
		int length0 = 0;
		int length1 = 0;
		int length2 = 0;
		if(weightedVel != null) length0 = weightedVel.size();
		if(cognitiveVel != null) length1 = cognitiveVel.size();
		if(socialVel != null) length2 = socialVel.size();
		
		if(length0 + length1 + length2 < this.velMaxSize){
			if(weightedVel != null) newVel.addAll(weightedVel);
			if(cognitiveVel != null) newVel.addAll(cognitiveVel);
			if(socialVel != null) newVel.addAll(socialVel);
		}else{
			int length0Max = (int)Math.round(w / (w + r1 + r2)*this.velMaxSize);
			int length1Max = (int)Math.round(r1 / (w+ r1 + r2) * this.velMaxSize);
			int length2Max = this.velMaxSize - length1Max - length0Max;
			if(weightedVel != null && length0Max != 0){
				if(weightedVel.size() <= length0Max){
					newVel.addAll(weightedVel);
				}else{
					for(int l = 0; l < length0Max; l++){
						newVel.add(weightedVel.get(l));
					}
				}
			}
			if(cognitiveVel != null && length1Max != 0){
				if(cognitiveVel.size() <= length1Max){
					newVel.addAll(cognitiveVel);
				}else{
					for(int l = 0; l < length1Max; l++){
						newVel.add(cognitiveVel.get(l));
					}
				}
			}
			if(socialVel != null && length2Max != 0){
				if(socialVel.size() <= length2Max){
					newVel.addAll(socialVel);
				}else{
					for(int l = 0; l < length2Max; l++){
						newVel.add(socialVel.get(l));
					}
				}
			}
		}
		
		if(newVel != null){
			if(!this.printout){
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
		
		int[] tmpLoc = new int[this.numSites];
		System.arraycopy(particleLoc, 0, tmpLoc, 0, this.numSites);
		
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
	private void getSwapsStartRandomly(int startIndex, int[] bestLoc, int[] partucleLoc){
		this.swaps.clear();
		int[] tmp =partucleLoc.clone();
		
		for(int m = startIndex; m < bestLoc.length; m++){
			int index = bestLoc[m];
			if(index != -1){
				for(int n = 0; n < tmp.length; n++){
					if(index == tmp[n]){
						if(m != n){
							Swap swap = new Swap(0, 0);
							swap.setFromIndex(m);
							swap.setToIndex(n);
							this.doOneSwap(tmp, m , n);
							this.swaps.add(swap);
							break;
						}						
					}
				}
			
			}
		}
		for(int m = 0; m < bestLoc.length; m++){
			int index = bestLoc[m];
			if(index != -1){
				for(int n = 0; n < tmp.length; n++){
					if(index == tmp[n]){
						if(m != n){
							Swap swap = new Swap(0, 0);
							swap.setFromIndex(m);
							swap.setToIndex(n);
							this.doOneSwap(tmp, m , n);
							this.swaps.add(swap);
							break;
						}						
					}
				}
			
			}
		}
	}	
	private Block getBlock(int blockId){
		Block block = null;
		for(Block b:this.blocks){
			if(b.index == blockId) block = b;
		}
		return block;
	}
	//do swaps to update particle's location: X + Velocity 
	private double updateLocations(int pIndex, int[] locations , List<Swap> vel){
		double sumDeltaCost = 0;
		if(vel != null && !vel.isEmpty()){	
			for(int velIndex = 0; velIndex < vel.size(); velIndex++){
				int from = vel.get(velIndex).getFromIndex();
				int to = vel.get(velIndex).getToIndex();
				this.doOneSwap(locations, from, to);//only update blockIndexList for a particle
//				sumDeltaCost += deltaCost(pIndex, locations, from, to);
			}
		}
		return sumDeltaCost;
	}
	public double deltaCost(int pIndex, int[] indexList, int from, int to){
		//from, to site index
		Block block1 = null;
		Block block2 = null;		
		int blockIndex1 = indexList[from];
		int blockIndex2 = indexList[to];	
		int fromY = this.blockHeight * from + 1;
		int toY = this.blockHeight * to + 1;		
		if(blockIndex1 != -1){		
			block1 = this.getBlock(blockIndex1);//TODO GET ACCESS TO THE RIGHT BLOCK
//			block1.tryLegal(this.legalcordinateX, toY);//TODO check if this swap has an influence on its nets and crits, not by trying but real swap
			block1.updateVerticals(pIndex, toY);
		}
		if(blockIndex2 != -1){
			block2 = this.getBlock(blockIndex2);
			block2.updateVerticals(pIndex, fromY);
		}
		double deltaCost = 0.0;
		if(block1 != null){
			for(Net net : block1.nets){
				deltaCost += net.deltaVerticalConnectionCost(pIndex);
			}
			for(Crit crit : block1.crits){
				deltaCost += crit.deltaVerticalTimingCost(pIndex);
			}
		}
		if(block2 != null){
			for(Net net : block2.nets){
				deltaCost += net.deltaVerticalConnectionCost(pIndex);
			}
			for(Crit crit : block2.crits){
				deltaCost += crit.deltaVerticalTimingCost(pIndex);
			}
		}
		return deltaCost;
	}
	public int[] doOneSwap(int[] indexList, int from, int to){	
		int tmp;
		int indexFrom = indexList[from];
		int indexTo = indexList[to];
		if(from != to && indexFrom != indexTo){
			tmp = indexFrom;
			indexList[from] = indexTo;
			indexList[to] = tmp;
		}
		return indexList;
	}
	//Velocity multiplied by a constant
	private List<Swap> multipliedByC(List<Swap> vel, double c){
		List<Swap> newVel = new ArrayList<Swap>();
		int newSize = (int)Math.floor(vel.size() * c);
		if(!this.printout) System.out.println(vel.size() + " " + c + " " + newSize);
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
		}
		return newVel;
	}
	
	private class Particle{
		private final int pIndex;
		private final int numSites;
		
//		private HashSet<Net> pNets;
		private List<Crit> pCrits;
		private List<Net> pNets;
//		private HashSet<Crit> pCrits;
		
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
			this.pCost = 0.0;
			this.pBest = 0.0;
			this.pBestIndexList = new int[this.numSites];
		}
		private List<Swap> getVelocity(){
			return this.velocity;
		}
		private void setVelocity(List<Swap> vel){
			this.velocity = new ArrayList<Swap>(vel);
		}
		private void setPNets(Set<Net> columnNets){
			this.pNets = new ArrayList<>(columnNets);
		}
		private void setPCrits(Set<Crit> columnCrits){
			this.pCrits = new ArrayList<>(columnCrits);
		}
		private double getCost(){
			double cost = 0.0;
			double timing = 0.0;
			double conn = 0.0;

			if(this.pNets != null){
				for(Net net:this.pNets)	conn += net.connectionCost()*net.getTotalNum();;
			}
			int numCrits = this.pCrits.size();
			if(this.pCrits != null && numCrits != 0){
				for(Crit crit:this.pCrits) timing += crit.timingCost();
			}
			cost = timing + conn;
			
//			System.out.println(String.format("%.2f", timing) + "\t" + String.format("%.2f", conn));
			return cost;
		}
		
		private double getCost(int i){
			double cost = 0.0;
			double timing = 0.0;
			double conn = 0.0;
			if(this.pNets != null){
				for(Net net:this.pNets) conn += net.connectionCost(i);
			}
			int numCrits = this.pCrits.size();
			if(this.pCrits != null && numCrits != 0){
				for(Crit crit:this.pCrits) timing += crit.timingCost(i);
			}
			cost = timing + conn;
//			double product = timing * conn;//TEST 
//			if(numCrits != 0) System.out.println(String.format("%.2f", timing) + "\t" + String.format("%.2f", conn) + "\t" + String.format("%.2f", cost)+ "\t" + String.format("%.2f", product));
			
			return cost;
		}
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
	class vitualCLB{
		Block block;
	}
	
}
