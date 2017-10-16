package place.placers.analytical;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Column;
import place.placers.analytical.HardblockConnectionLegalizer.Crit;
import place.placers.analytical.HardblockConnectionLegalizer.Net;
import place.placers.analytical.HardblockConnectionLegalizer.Site;

public class HardblockAnneal {
	private Block[] blocks;
	private Site[] sites;
	
	private double quality;
	private double effortLevel;

	private int numBlocks, numSites;
	
	private double temperature;
	private int movesPerTemperature;
	private int iteration;
	
	private double cost, minimumCost;
	private List<Double> costHistory;
	
	private final Random random;
//	private int counter;
	
	HardblockAnneal(int seed){
		this.random = new Random(seed);
	}
	public void doAnneal(Column[] columns, double quality){
		int numBlocks = 0;
		int numSites = 0;
		
		for(Column column:columns){
			numBlocks += column.blocks.size();
			numSites += column.sites.length;
		}
		
		this.blocks = new Block[numBlocks];
		this.sites = new Site[numSites];
		
		int blockIndex = 0, siteIndex = 0;
		for(Column column:columns){
			for(Block block:column.blocks){
				this.blocks[blockIndex++] = block;
			}
			for(Site site:column.sites){
				this.sites[siteIndex++] = site;
			}
		}

		this.quality = quality;
		this.effortLevel = 1.0;

		this.doAnneal();
	}
	public void doAnneal(Column column, double quality){
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.sites = column.sites;
		
//		this.numBlocks = this.blocks.length;
//		this.numSites = this.sites.length;//TODO

		this.quality = quality;
		this.effortLevel = 1.0;

		this.doAnneal();
	}
	public void doAnneal(Block[] annealBlocks, Site[] annealSites, double quality){
		this.blocks = annealBlocks;
		this.sites = annealSites;

		this.quality = quality;
		this.effortLevel = Math.max(0.01 / quality, 1.0);

		this.doAnneal();
	}
	private void doAnneal(){
		boolean printStatistics = false;
//		this.counter = 0;
		this.numBlocks = this.blocks.length;//TODO
		this.numSites = this.sites.length;

		Set<Net> nets = new HashSet<>();
		Set<Crit> crits = new HashSet<>();
		for(Block block:this.blocks){
			for(Net net:block.nets){
				nets.add(net);
			}
			for(Crit crit:block.crits){
				crits.add(crit);
			}
		}

		this.cost = 0.0;
		for(Net net:nets){
			this.cost += net.connectionCost();
		}
		for(Crit crit:crits){
			this.cost += crit.timingCost();
		}
		this.minimumCost = this.cost;

		this.temperature = this.calculateInitialTemperature();
		this.movesPerTemperature = (int)Math.round(this.effortLevel * Math.pow(this.numBlocks, 4/3));

		this.iteration = 0;

		if(printStatistics){
			System.out.println("Anneal " + this.blocks.length + " blocks:");
			System.out.println("\tit\talpha\ttemp\tcost");
			System.out.println("\t--\t-----\t----\t----");
		}
		
		boolean finalIteration = false;
		this.costHistory = new ArrayList<>();
		
		for(Block block:this.blocks){
			block.initializeOptimalSite();
		}

		while(!finalIteration){
			double numSwaps = this.doSwapIteration(this.movesPerTemperature, true);
			double alpha = numSwaps / this.movesPerTemperature;

			if(printStatistics) System.out.printf("\t%d\t%.2f\t%.2f\t%.2f\n", this.iteration, alpha, this.temperature, this.cost);

			this.updateTemperature(alpha);
			this.iteration++;
			
			if(this.cost < this.minimumCost){
				this.minimumCost = this.cost;
				
				for(Block block:this.blocks){
					block.saveOptimalSite();
				}
			}
			finalIteration = this.finalIteration(this.cost);
		}
		
		if(this.minimumCost < this.cost){
			for(Block block:this.blocks){
				block.setOptimalSite();
			}
		}
//		System.out.println(this.counter);
		if(printStatistics) System.out.println();
	}
	private boolean finalIteration(double cost){
		this.costHistory.add(this.cost);
		if(this.costHistory.size() > 10){
			double max = this.costHistory.get(this.costHistory.size() - 1);
			double min = this.costHistory.get(this.costHistory.size() - 1);
			
			for(int i = 0; i < 10; i++){
				double value = this.costHistory.get(this.costHistory.size() - 1 - i);
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
    private double calculateInitialTemperature(){
        int numSamples = this.numBlocks;
        double stdDev = this.doSwapIteration(numSamples, false);
        return stdDev;
    }
    private void updateTemperature(double alpha) {
        if (alpha > 0.96) {
        	 this.temperature *= 0.5;
        } else if (alpha > 0.8) {
        	 this.temperature *= 0.9;
        } else if (alpha > 0.15){
        	 this.temperature *= 0.95;
        } else {
        	this.temperature *= 0.8;
        }
    }
	private double doSwapIteration(int moves, boolean pushTrough){
		int numSwaps = 0;

		double sumDeltaCost = 0;
		double quadSumDeltaCost = 0;
		
        for(int i = 0; i < moves; i++){
//			this.counter++;
        	Swap swap = this.getSwap();
    		swap.deltaCost();
			
			if(pushTrough){
				if(swap.deltaCost <= 0 || this.random.nextDouble() < Math.exp(-swap.deltaCost / this.temperature)) {
					numSwaps++;
					swap.pushTrough();
					this.cost += swap.deltaCost;
				}else{
					swap.revert();
				}
			}else{
				swap.revert();
                sumDeltaCost += swap.deltaCost;
                quadSumDeltaCost += swap.deltaCost * swap.deltaCost;
			}
		}
        if(pushTrough){
        	return numSwaps;
        }else{
            double sumQuads = quadSumDeltaCost;
            double quadSum = sumDeltaCost * sumDeltaCost;

            double numBlocks = this.blocks.length;
            double quadNumBlocks = numBlocks * numBlocks;

            return Math.sqrt(Math.abs(sumQuads / numBlocks - quadSum / quadNumBlocks));
        }
	}
	Swap getSwap(){
//		System.out .println(this.blocks.length + " " + this.numBlocks);//TODO
		Block block1 = this.blocks[this.random.nextInt(this.numBlocks)];
		Site site1 = block1.getSite();
		
		Site site2 = this.sites[this.random.nextInt(this.numSites)];
		while(site1.equals(site2) ){
			site2 = this.sites[this.random.nextInt(this.numSites)];
		}
		Block block2 = site2.getBlock();
		
		Swap swap = new Swap(block1, site1, block2, site2);
		return swap;
	}
	class Swap {
		final Block block1;
		final Block block2;
		
		final Site site1;
		final Site site2;
		
		final boolean block1valid;
		final boolean block2valid;
		
		double deltaCost;
		
		Swap(Block block1, Site site1, Block block2, Site site2){
			this.block1 = block1;
			this.site1 = site1;
			this.block2 = block2;
			this.site2 = site2;
			
			this.block1valid = this.block1 != null;
			this.block2valid = this.block2 != null;
		}
		void deltaCost(){
			this.deltaCost = 0.0;
			
			this.site1.removeBlock();
			this.site2.removeBlock();
			
			if(this.block1valid){
				this.block1.setSite(this.site2);
				this.site2.setBlock(this.block1);
				this.block1.tryLegal(this.site2.column, this.site2.row);
			}
			
			if(this.block2valid){
				this.block2.setSite(this.site1);
				this.site1.setBlock(this.block2);
				this.block2.tryLegal(this.site1.column, this.site1.row);
			}
				
			if(this.block1valid){
				for(Net net:this.block1.nets){
					this.deltaCost += net.deltaHorizontalConnectionCost();
					this.deltaCost += net.deltaVerticalConnectionCost();
				}
				for(Crit crit:this.block1.crits){
					this.deltaCost += crit.deltaHorizontalTimingCost();
					this.deltaCost += crit.deltaVerticalTimingCost();
				}
			}
			if(this.block2valid){
				for(Net net:this.block2.nets){
					this.deltaCost += net.deltaHorizontalConnectionCost();
					this.deltaCost += net.deltaVerticalConnectionCost();

				}
				for(Crit crit:this.block2.crits){
					this.deltaCost += crit.deltaHorizontalTimingCost();
					this.deltaCost += crit.deltaVerticalTimingCost();
				}
			}
		}
		void pushTrough(){
			if(this.block1valid){
				this.block1.pushTrough();
			}
			if(this.block2valid){
				this.block2.pushTrough();
			}
		}
		void revert(){
			this.site1.removeBlock();
			this.site2.removeBlock();
			
			if(this.block1valid){
				this.block1.setSite(this.site1);
				this.site1.setBlock(this.block1);
				this.block1.revert();
			}
			
			if(this.block2valid){
				this.site2.setBlock(this.block2);
				this.block2.setSite(this.site2);
				this.block2.revert();
			}
		}
	}
}