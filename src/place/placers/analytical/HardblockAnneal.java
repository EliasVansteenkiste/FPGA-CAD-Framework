package place.placers.analytical;

import java.util.Random;

import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Column;
import place.placers.analytical.HardblockConnectionLegalizer.Site;
import place.util.TimingTree;

public class HardblockAnneal {
	private Block[] blocks;
	private Column[] columns;
	private Site[] sites;
	
	private int numBlocks;
	private int numSites;
	
	private double temperature;
	private int movesPerTemperature;
	
	private final Random random;

	private TimingTree timing;
	
	HardblockAnneal(TimingTree timing, int seed){
		this.timing = timing;
		this.random = new Random(seed);
	}
	public void doAnneal(Block[] annealBlocks, Column[] annealColumns){
		for(Column column:annealColumns){
			for(Block block:column.blocks){
				block.column = column;
			}
		}
		this.blocks = annealBlocks;
		this.columns = annealColumns;
		this.sites = null;
		
		this.doAnneal();
	}
	public void doAnneal(Column column){
		this.blocks = column.blocks.toArray(new Block[column.blocks.size()]);
		this.columns = null;
		this.sites = column.sites;
		
		this.doAnneal();
	}
	public void doAnneal(Block[] annealBlocks, Site[] annealSites){
		this.blocks = annealBlocks;
		this.columns = null;
		this.sites = annealSites;
		
		this.doAnneal();
	}
	private void doAnneal(){
		this.timing.start("Anneal");
		
		this.numBlocks = this.blocks.length;
		if(this.sites == null){
			this.numSites = this.columns[0].sites.length;
		}else{
			this.numSites = this.sites.length;
		}
		
		this.temperature = this.calculateInitialTemperature();
		this.movesPerTemperature = (int)Math.pow(this.numBlocks, 4/3);

		while(this.temperature > 0.01){
			double numSwaps = this.doSwapIteration(this.movesPerTemperature, true);
			double alpha = numSwaps / this.movesPerTemperature;
			this.updateTemperature(alpha);
		}

		this.timing.time("Anneal");
	}
    private double calculateInitialTemperature(){
        int numSamples = this.blocks.length;
        double stdDev = this.doSwapIteration(numSamples, false);
        return stdDev;
    }
    private void updateTemperature(double alpha) {
        if (alpha > 0.96) {
        	 this.temperature *= 0.5;
        } else if (alpha > 0.8) {
        	 this.temperature *= 0.9;
        } else if (alpha > 0.15 ) {
        	 this.temperature *= 0.92;
        } else {
        	 this.temperature *= 0.95;
        }
    }
	private double doSwapIteration(int moves, boolean pushTrough){
		int numSwaps = 0;

		double sumDeltaCost = 0;
		double quadSumDeltaCost = 0;

		Block block1 = null, block2 = null;
		Site site1 = null, site2 = null;
		
        for(int i = 0; i < moves; i++){
			
			block1 = this.blocks[this.random.nextInt(this.numBlocks)];
			site1 = block1.getSite();
			
			if(this.sites == null){
				Column column = block1.column;
				site2 = column.sites[this.random.nextInt(this.numSites)];
				while(site1.equals(site2)){
					site2 = column.sites[this.random.nextInt(this.numSites)];
				}
			}else{
				site2 = this.sites[this.random.nextInt(this.numSites)];
				while(site1.equals(site2)){
					site2 = this.sites[this.random.nextInt(this.numSites)];
				}
			}
			block2 = site2.getBlock();
			
			double deltaCost = this.deltaCost(block1, site1, block2, site2);
			
			if(pushTrough){
				if(deltaCost <= 0 || random.nextDouble() < Math.exp(-deltaCost / temperature)) {
					this.pushTrough(block1, site1, block2, site2);
				}else{
					this.revert(block1, site1, block2, site2);
				}
			}else{
				this.revert(block1, site1, block2, site2);
                sumDeltaCost += deltaCost;
                quadSumDeltaCost += deltaCost * deltaCost;
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
	private double deltaCost(Block block1, Site site1, Block block2, Site site2){
		double oldCost = 0.0;
		if(block1 != null) oldCost += block1.connectionCost();
		if(block2 != null) oldCost += block2.connectionCost();

		if(block1 != null){
			block1.setSite(site2);
			site2.setBlock(block1);
		}
		
		if(block2 != null){
			block2.setSite(site1);
			site1.setBlock(block2);
		}
		
		if(block1 == null) site2.removeBlock();
		if(block2 == null) site1.removeBlock();
			
		double newCost = 0.0;
		if(block1 != null) newCost += block1.connectionCost(site1.column, site2.column, site1.row, site2.row);
		if(block2 != null) newCost += block2.connectionCost(site2.column, site1.column, site2.row, site1.row);

		return newCost - oldCost;
	}
	private void pushTrough(Block block1, Site site1, Block block2, Site site2){
		if(block1 != null) block1.updateConnectionCost();
		if(block2 != null) block2.updateConnectionCost();
	}
	private void revert(Block block1, Site site1, Block block2, Site site2){
		if(block1 != null){
			block1.setSite(site1);
			site1.setBlock(block1);
		}
		
		if(block2 != null){
			site2.setBlock(block2);
			block2.setSite(site2);
		}
		
		if(block1 == null) site1.removeBlock();
		if(block2 == null) site2.removeBlock();
	}
}