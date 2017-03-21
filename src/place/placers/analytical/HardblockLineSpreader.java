package place.placers.analytical;

import java.util.ArrayList;
import java.util.List;

import place.placers.analytical.HardblockLegalizer.Bucket;

class HardblockLineSpreader {

	private Block firstBlock;
	private Cluster firstCluster;
    private final int blockSize;
	
	private final int size;

    HardblockLineSpreader(int size, int blockSize, Bucket bucket){
    	this.blockSize = blockSize;
    	int numPos = (int) Math.floor(size / this.blockSize);
    	this.size = numPos * this.blockSize;
    	
    	if(bucket.blocks.size() > numPos){
    		System.out.println("There are too many blocks in the bucket, the linespreader cannot legalizer the blocks");
    	}
    	
		this.firstBlock = new Block(0, bucket.blocks.get(0).y, this.size, this.blockSize);
    	for(int b = 1; b < bucket.usedPostions(); b++){
    		Block block = new Block(b, bucket.blocks.get(b).y, this.size, this.blockSize);
    		
    		if(block.originalCoordinate < this.firstBlock.originalCoordinate){
    			this.firstBlock.insert(block);
    			this.firstBlock = block;
    		}else{
    			Block currentBlock = this.firstBlock;
    			while(block.originalCoordinate > currentBlock.originalCoordinate && currentBlock.next != null){
    				currentBlock = currentBlock.next;
    			}
    			if(block.originalCoordinate > currentBlock.originalCoordinate){
    				currentBlock.append(block);
    			}else{
    				currentBlock.insert(block);
    			}
    		}
    	}
    	
    	//Make the chain of clusters
    	this.firstCluster = new Cluster(this.firstBlock, this.blockSize, this.size);
    	Cluster previousCluster = this.firstCluster;
    	Block block = this.firstBlock.next;
    	while(block != null){
    		Cluster cluster = new Cluster(block, this.blockSize, this.size);

    		cluster.previous = previousCluster;
    		previousCluster.next = cluster;

    		previousCluster = cluster;
    		
    		block = block.next;
    	}

    	//this.printBlocks();
    	
    	while(this.overlap()){
        	Cluster cluster = this.firstCluster;
        	do{
        		cluster.absorbNeighbour();
        		
        		cluster = cluster.next;
        	}while(cluster != null);
    	}
    	
    	//this.printClusters();

    	block = this.firstBlock;
    	do{
    		bucket.blocks.get(block.index).y = block.coordinate;
    		block = block.next;
    	}while(block != null);
    }
    boolean overlap(){
    	Cluster cluster = this.firstCluster;
    	do{
    		if(this.overlap(cluster)){
    			return true;
    		}
    		cluster = cluster.next;
    	}while(cluster != null);
    	return false;
    }
    boolean overlap(Cluster cluster){
    	if(cluster.previous != null){
    		if(cluster.previous.right >= cluster.left){
    			return true;
    		}
    	}
    	if(cluster.next != null){
    		if(cluster.next.left <= cluster.left){
    			return true;
    		}
    	}
    	return false;
    }

    void printBlocks(){
		System.out.println("Print blocks:");
    	Block block = this.firstBlock;
    	do{
    		System.out.println("\t" + block);
    		block = block.next;
    	}while(block != null);
    	System.out.println();
    }
    void printClusters(){
		System.out.println("Print clusters:");
    	Cluster cluster = this.firstCluster;
    	do{
    		System.out.println("\t" + cluster);
    		cluster = cluster.next;
    	}while(cluster != null);
    	System.out.println();
    }
    class Block {
    	private final int index;
    	private final int gridSize, blockSize;
    	
    	private Block previous;
    	private Block next;
    	
    	private final double originalCoordinate;
    	private int coordinate;
    	
    	Block(int index, double coordinate, int size, int blockSize){
    		this.index = index;
    		this.gridSize = size;
    		this.blockSize = blockSize;
    		
    		this.originalCoordinate = coordinate;
    		
    		//Best position on the grid for current blocks
    		int numRows = (int) Math.floor(this.gridSize / this.blockSize);
    		this.coordinate = (int) Math.round(Math.max(Math.min((coordinate - 1) / this.blockSize,  numRows - 1), 0)) * this.blockSize + 1;
    		
    		this.previous = null;
    		this.next = null;
    	}
    	void insert(Block block){
    		if(this.previous != null){
        		Block b1 = this.previous;
        		Block b2 = block;
        		Block b3 = this;
        		
        		b1.next = b2;
        		b2.previous = b1;
        		
        		b2.next = b3;
        		b3.previous = b2;
    		}else{
    			this.previous = block;
    			block.next = this;
    		}
    	}
    	void append(Block block){
    		if(this.next != null){
        		Block b1 = this;
        		Block b2 = block;
        		Block b3 = this.next;
        		
        		b1.next = b2;
        		b2.previous = b1;
        		
        		b2.next = b3;
        		b3.previous = b2;
    		}else{
    			this.next = block;
    			block.previous = this;
    		}
    	}
    	double cost(){
    		return (this.coordinate - this.originalCoordinate) * (this.coordinate - this.originalCoordinate);
    	}
    	
    	public String toString(){
    		return "[" + String.format("%3d", this.coordinate) + " | " + String.format("%.3f", this.originalCoordinate) + "]";
    	}
    }
    class Cluster {
    	private Cluster previous;
    	private Cluster next;
    	
    	private int left, right;
    	
    	private final List<Block> blocks;
    	
    	private final int blockSize, gridSize;
    	
    	Cluster(Block block, int blockSize, int gridSize){
    		this.blocks = new ArrayList<Block>();
    		this.blocks.add(block);
    		
    		this.blockSize = blockSize;
    		this.gridSize = gridSize;
    		
    		this.left = block.coordinate;
    		this.right = block.coordinate + this.blockSize - 1;

    		this.previous = null;
    		this.next = null;
    	}
    	void absorbNeighbour(){
    		if(this.next != null){
            	if(this.right >= this.next.left){
        			Cluster c1 = this;
        			Cluster c2 = this.next;
        			
        			for(Block block:c2.blocks){
        				block.coordinate = this.right + 1;
        				c1.blocks.add(block);
        				this.right += this.blockSize;
        			}
        			
            		if(this.next.next != null){
            			Cluster c3 = this.next.next;
            			c1.next = c3;
            			c3.previous = c1;
            		}else{
            			c1.next = null;
            		}
            		
            		c2 = null;
        			this.minimumCostShift();
        			
        			this.absorbNeighbour();
    			}
    		}
    	}
    	
    	private void minimumCostShift(){
    		this.fitInColumn();
    		
    		double cost = this.cost();
    		while(true){
        		this.decrease();
        		double localCost = this.cost();
        		if(localCost < cost){
        			cost = localCost;
        		}else{
        			this.increase();
        			break;
        		}
    		}
    		while(true){
        		this.increase();
        		double localCost = this.cost();
        		if(localCost < cost){
        			cost = localCost;
        		}else{
        			this.decrease();
        			break;
        		}
    		}
    		
    		this.fitInColumn();
    	}
    	void fitInColumn(){
    		while(this.right > this.gridSize){
    			this.decrease();
    		}
    		while(this.left < 1){
    			this.increase();
    		}
    	}
    	double cost(){
    		double cost = 0.0;
    		for(Block block:this.blocks){
    			cost += block.cost();
    		}
    		return cost;
    	}
    	void increase(){
    		this.left += this.blockSize;
    		this.right += this.blockSize;
    		for(Block block:this.blocks){
    			block.coordinate += this.blockSize;
    		}
    	}
    	void decrease(){
    		this.left -= this.blockSize;
    		this.right -= this.blockSize;
    		for(Block block:this.blocks){
    			block.coordinate -= this.blockSize;
    		}
    	}
    	
    	public String toString(){
    		String result = new String();
    		if(this.blocks.size() == 1){
    			result += "1 block | ";
    		}else{
    			result += this.blocks.size() + " blocks| ";
    		}
    		result += "left = " + this.left + " | ";
    		result += "right = " + this.right + " | ";
    		for(Block block:this.blocks){
    			result += block + " ";
    		}
    		return result;
    	}
    }
}