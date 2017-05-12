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
import place.util.TimingTree;

public class HardblockAnneal {
	private Block[] blocks;
	private Site[] sites;
	
	private final Set<Net> nets;
	private final Set<Crit> crits;
	
	private int numBlocks, numSites;
	
	private double temperature;
	private int movesPerTemperature;
	private int iteration;
	
	private double cost;
	private final List<Double> costHistory;
	
	private final Random random;

	private TimingTree timing;
	
	HardblockAnneal(TimingTree timing, int seed){
		this.timing = timing;
		this.random = new Random(seed);
		
		this.nets = new HashSet<>();
		this.crits = new HashSet<>();
		
		this.costHistory = new ArrayList<>();
	}
	public void doAnneal(Column[] columns){
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
		
		this.doAnneal();
	}
	public void doAnneal(Column column){
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.sites = column.sites;
		
		this.doAnneal();
	}
	public void doAnneal(Block[] annealBlocks, Site[] annealSites){
		this.blocks = annealBlocks;
		this.sites = annealSites;

		this.doAnneal();
	}
	private void doAnneal(){
		this.timing.start("Anneal");
		
		boolean printStatistics = false;

		this.numBlocks = this.blocks.length;
		this.numSites = this.sites.length;
		
		this.timing.start("Find nets and crits");
		this.nets.clear();
		this.crits.clear();
		for(Block block:this.blocks){
			for(Net net:block.nets){
				this.nets.add(net);
			}
			for(Crit crit:block.crits){
				this.crits.add(crit);
			}
		}
		this.timing.time("Find nets and crits");
		
		this.timing.start("Calculate cost");
		this.cost = 0.0;
		for(Net net:this.nets){
			this.cost += net.connectionCost();
		}
		for(Crit crit:this.crits){
			this.cost += crit.timingCost();
		}
		this.timing.time("Calculate cost");
		
		for(Net net:this.nets) net.initializeConnectionCost();
		for(Crit crit:this.crits) crit.initializeTimingCost();

		this.timing.start("Initialize anneal");
		this.temperature = this.calculateInitialTemperature();
		this.movesPerTemperature = (int)Math.pow(this.numBlocks, 4/3);
		this.timing.time("Initialize anneal");
		
		this.iteration = 0;

		if(printStatistics){
			System.out.println("Anneal " + this.blocks.length + " blocks:");
			System.out.println("\tit\talpha\ttemp\tcost");
			System.out.println("\t--\t-----\t----\t----");
		}
		
		boolean finalIteration = false;
		this.costHistory.clear();
		
		this.timing.start("Do anneal");
		while(!finalIteration){
			double numSwaps = this.doSwapIteration(this.movesPerTemperature, true);
			double alpha = numSwaps / this.movesPerTemperature;

			if(printStatistics) System.out.printf("\t%d\t%.2f\t%.2f\t%.2f\n", this.iteration, alpha, this.temperature, this.cost);

			this.updateTemperature(alpha);
			this.iteration++;
			
			finalIteration = this.finalIteration(this.cost);
		}
		this.timing.time("Do anneal");
		
		if(printStatistics) System.out.println();

		this.timing.time("Anneal");
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
			
			if(ratio < 1.001){
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
        } else {
        	 this.temperature *= 0.95;
        }
    }
	private double doSwapIteration(int moves, boolean pushTrough){
		int numSwaps = 0;

		double sumDeltaCost = 0;
		double quadSumDeltaCost = 0;
		
        for(int i = 0; i < moves; i++){
			
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