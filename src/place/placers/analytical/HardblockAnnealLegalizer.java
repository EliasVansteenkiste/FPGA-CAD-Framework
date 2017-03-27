package place.placers.analytical;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

public class HardblockLegalizer{

	private double[] linearX, linearY;
    private int[] legalX, legalY;
    
    private LegalizerBlock[] blocks;
    private LegalizerNet[] nets;
    
    private Bucket[] buckets;

	HardblockLegalizer(
			double[] linearX,
			double[] linearY,
			int[] legalX, 
			int[] legalY, 
			int[] heights,
			List<Net> nets){

		long start = System.nanoTime();

		this.linearX = linearX;
		this.linearY = linearY;

		this.legalX = legalX;
		this.legalY = legalY;

		//Make all objects
		this.blocks = new LegalizerBlock[legalX.length];
		this.nets = new LegalizerNet[nets.size()];

		for(int i = 0; i < nets.size(); i++){
			this.nets[i] = new LegalizerNet(i);
		}
		for(int i = 0; i < legalX.length; i++){
			int offset = (1- heights[i]) / 2;
			this.blocks[i] = new LegalizerBlock(i, offset);
		}
		
		//Connect objects
		for(int i = 0; i < nets.size(); i++){
			LegalizerNet legalizerNet = this.nets[i];
			for(NetBlock block:nets.get(i).blocks){
				LegalizerBlock legalizerBlock = this.blocks[block.blockIndex];
				legalizerNet.addBlock(legalizerBlock);
				legalizerBlock.addNet(legalizerNet);
			}
		}
		
		long end = System.nanoTime();
		double time = (end -  start) * Math.pow(10, -6);
    	System.out.printf("Hardblock legalizer initialization took %.0f ms\n", time);
	}
	public void legalizeBlockType(int blocksStart, int blocksEnd, int width, int height, int blockStart, int blockRepeat, int blockHeight){
		long start = System.nanoTime();
		
		boolean printTestOutput = false;
		
        int numColumns = (int) Math.floor((width - blockStart) / blockRepeat + 1);
        int numRows = (int) Math.floor(height / blockHeight);
        
        //Make the buckets for the current hard block type
        this.buckets = new Bucket[numColumns];
		int b = 0;
        for(int x = blockStart; x < width + 2; x += blockRepeat){
			this.buckets[b] = new Bucket(x, numRows);
			b++;
		}

        //Update the coordinates of the blocks and fill the buckets
        for(LegalizerBlock block:this.blocks){
        	block.setCoordinates(this.legalX[block.index], this.legalY[block.index]);
        }
		for(int i = blocksStart; i < blocksEnd; i++){
			LegalizerBlock block = this.blocks[i];
			
			int columnIndex = (int) Math.round(Math.max(Math.min((this.linearX[i] - blockStart) / blockRepeat, numColumns - 1), 0));
			block.x = blockStart + blockRepeat * columnIndex;
			block.y = this.linearY[i];
			
			this.buckets[columnIndex].addBlock(block);
		}

		//Find buckets with empty positions
		HashSet<Bucket> availableBuckets = new HashSet<Bucket>();
		for(Bucket bucket:this.buckets){
			if(bucket.usedPostions() < bucket.availablePositions()){
				availableBuckets.add(bucket);
			}
		}
		
		if(printTestOutput){
			//Test functionality
	        System.out.print("The system has " + this.buckets.length + " buckets with size " + this.buckets[0].availablePositions + ": ");
	        for(Bucket bucket:this.buckets){
	        	System.out.print(bucket.coordinate + " ");
	        }
	        System.out.println();
			System.out.println("Status of the buckets");
			for(Bucket bucket:this.buckets){
				System.out.println("\t" + bucket);
			}
			System.out.println("Largest bucket has " + this.largestBucket().usedPostions() + " blocks");
			System.out.println("Buckets with empty positions");
			for(Bucket bucket:availableBuckets){
				System.out.println("\t" + bucket);
			}
		}
		
		//Distribute blocks along the buckets
		Bucket largestBucket = this.largestBucket();
		while(largestBucket.usedPostions() > largestBucket.availablePositions()){

			LegalizerBlock bestBlock = null;
			Bucket bestBucket = null;
			double minimumIncrease = Double.MAX_VALUE;
			
			for(LegalizerBlock block: largestBucket.blocks){
				double currentCost = block.horizontalCost();
				for(Bucket bucket:availableBuckets){
					block.x = bucket.coordinate;
					
					double newCost = block.horizontalCost();
					double increase = newCost - currentCost;
					if(increase < minimumIncrease){
						minimumIncrease = increase;
						bestBlock = block;
						bestBucket = bucket;
					}
				}
				block.x = largestBucket.coordinate;
			}

			largestBucket.removeBlock(bestBlock);
			bestBucket.addBlock(bestBlock);
			bestBlock.x = bestBucket.coordinate;

			if(bestBucket.usedPostions() == bestBucket.availablePositions()){
				availableBuckets.remove(bestBucket);
			}
			
			largestBucket = this.largestBucket();
		}
		
		
		if(printTestOutput){
			//Test functionality
			System.out.println("Status of the buckets");
			for(Bucket bucket:this.buckets){
				System.out.println("\t" + bucket);
			}
		}

		//Spread the blocks in the buckets
		for(Bucket bucket:this.buckets){
			if(!bucket.isEmpty()){
				new HardblockLineSpreader(height, blockHeight, bucket);
			}
		}

		this.updateLegal();

		long end = System.nanoTime();
		double time = (end -  start) * Math.pow(10, -6);
    	System.out.printf("Hardblock legalizer took %.0f ms\n", time);
	}
	
	private Bucket largestBucket(){
		Bucket result = this.buckets[0];
		for(Bucket bucket:this.buckets){
			if(bucket.usedPostions() > result.usedPostions()){
				result = bucket;
			}
		}
		return result;
	}
    private void updateLegal(){
    	for(LegalizerBlock block:this.blocks){
    		this.legalX[block.index] = (int)Math.round(block.x);
    		this.legalY[block.index] = (int)Math.round(block.y);
    	}
    }
	
    //Circuit
	class LegalizerBlock{
		final int index, offset;
		double origX, origY;
		double x, y;
		final List<LegalizerNet> nets;
		
		LegalizerBlock(int index, int offset){
			this.index = index;
			this.offset = offset;
			this.nets = new ArrayList<LegalizerNet>();
		}
		void addNet(LegalizerNet net){
			if(!this.nets.contains(net)){
				this.nets.add(net);
			}
		}
		void setCoordinates(int x, int y){
			this.x = x;
			this.y = y;
		}
		
		double horizontalCost(){
			double cost = 0.0;
			
			for(LegalizerNet net:this.nets){
				cost += net.horizontalCost();
			}
			return cost;
		}
	}
	class LegalizerNet{
		final int index;
		final List<LegalizerBlock> blocks;
		
		LegalizerNet(int index){
			this.index = index;
			this.blocks = new ArrayList<LegalizerBlock>();
		}
		
		void addBlock(LegalizerBlock block){
			this.blocks.add(block);
		}
		
		double horizontalCost(){
	        int numNetBlocks = this.blocks.size();

	        LegalizerBlock initialBlock = this.blocks.get(0);
	        double minX = initialBlock.x;
	        double maxX = minX;

	        for(LegalizerBlock block:this.blocks) {
	        	double x = block.x;
	            if(x < minX) {
	                minX = x;
	            } else if(x > maxX) {
	                maxX = x;
	            }
	        }
	        return (maxX - minX + 1) * AnalyticalAndGradientPlacer.getWeight(numNetBlocks);
		}
	}
	
	//Hardblock bucket
	class Bucket{
		final int availablePositions;
		final int coordinate;
		final List<LegalizerBlock> blocks;
		
		Bucket(int coordinate, int availablePositions){
			this.coordinate = coordinate;
			this.availablePositions = availablePositions;
			this.blocks = new ArrayList<LegalizerBlock>();
		}
		void addBlock(LegalizerBlock block){
			this.blocks.add(block);
		}
		void removeBlock(LegalizerBlock block){
			this.blocks.remove(block);
		}
		
		int availablePositions(){
			return this.availablePositions;
		}
		int usedPostions(){
			return this.blocks.size();
		}
		boolean isEmpty(){
			return this.blocks.isEmpty();
		}
		
		public String toString(){
			return this.blocks.size() + " | " + this.availablePositions;
		}
	}
}