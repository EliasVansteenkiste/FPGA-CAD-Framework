package place.placers.analytical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;


class SimpleLegalizer extends Legalizer {
	
    
	SimpleLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> blockIndexes) throws IllegalArgumentException {

        	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, visualizer, blockIndexes);
     }

    @Override
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
//    	double[] expandedLinearX = doExpansion(this.linearX, blocksStart, blocksEnd, this.width);
//    	double[] expandedLinearY = doExpansion(this.linearY, blocksStart, blocksEnd, this.height);
//    	System.out.println("max expandedLinearX: " + max(expandedLinearX) + ", " + "max expandedLinearY: " + max(expandedLinearY));
//    	System.out.println("min expandedLinearX: " + min(expandedLinearX) + ", " + "min expandedLinearY: " + min(expandedLinearY));
//    	String name = "expansion";
//    	this.addVisual(name, expandedLinearX, expandedLinearY);
    	
    	List<LegalizerBlock> blocks = new ArrayList<LegalizerBlock>();  	
    	for(int index = blocksStart; index < blocksEnd; index++) {
    		LegalizerBlock legalizerBlock = new LegalizerBlock(index, this.linearX[index], this.linearY[index]);
    		blocks.add(legalizerBlock);
    	}
    	Collections.sort(blocks, Comparators.HORIZONTAL);

    	System.out.println(blocks.get(blocks.size()/2).x);
    	
    	int numColumn = this.width + 2;
    	int numRow = this.height + 2;
    	int maxCost = this.width + this.height;
    	int centX = (int)Math.ceil(blocks.get(blocks.size()/2).x);
    	int[] rowLeftMost = new int[numColumn];
    	for(int rowIndex = 0; rowIndex < numRow; rowIndex++) {
    		
            rowLeftMost[rowIndex] = centX;
        }
    	int[] rowRightMost = new int[numColumn];
    	for(int rowIndex = 0; rowIndex < numRow; rowIndex++) {
    		
    		rowRightMost[rowIndex] = centX-1;
        }
    	boolean[][] occupiedSite = new boolean[this.width+2][this.height+2];
    	for(int row = 1; row < this.height+1; row++){
    		for(int column = 1; column < numColumn; column++){
    			occupiedSite[column][row] = false;
    		}
    	}
    	
    	//for(LegalizerBlock block: blocks){
    	for(int i = blocks.size()/2 - 1; i >= 0; i--){
    		LegalizerBlock block = blocks.get(i);
    		int bestRow = 1;
    		double bestCost = maxCost;
    		for (int row = 1; row < numRow; row++){
    			rowRightMost[row] = (int)Math.round(Math.min(this.linearX[block.id], rowRightMost[row]));
    			double cost = calculateMovement(this.linearX[block.id], this.linearY[block.id], rowRightMost[row], row);
//    			System.out.println("rowLeftMost is: "+ rowLeftMost +" " + "cost is :" + cost);
    			if(cost < bestCost){
    				bestCost = cost;
    				bestRow = row;
    			}	
    		}
    		this.legalX[block.id] = rowRightMost[bestRow];
        	this.legalY[block.id] = bestRow;
        	occupiedSite[rowRightMost[bestRow]][bestRow] = true;
//    		this.legalX[block.id] = (int)Math.round(this.linearX[block.id]);
//        	this.legalY[block.id] = (int)Math.round(this.linearY[block.id]);;
    		if(rowRightMost[bestRow] > 1){
    			rowRightMost[bestRow]--;
    		} else{
    			rowRightMost[bestRow] = 1;
    		}
    	}
    	for(int i = blocks.size()/2; i < blocks.size(); i++){
    		LegalizerBlock block = blocks.get(i);
    		
    		int bestRow = 1;
    		double bestCost = maxCost;
    		for (int row = 1; row < numRow; row++){
    			rowLeftMost[row] = (int)Math.round(Math.max(this.linearX[block.id], rowLeftMost[row]));
    			double cost = calculateMovement(this.linearX[block.id], this.linearY[block.id], rowLeftMost[row], row);
    			if(cost < bestCost){
    				bestCost = cost;
    				bestRow = row;
    			}	
    		}
    		this.legalX[block.id] = rowLeftMost[bestRow];
        	this.legalY[block.id] = bestRow;
        	occupiedSite[rowLeftMost[bestRow]][bestRow] = true;
        		
    		if(rowLeftMost[bestRow] < this.width){
    			rowLeftMost[bestRow]++;
    		} else{
    			rowLeftMost[bestRow] = this.width;
    		}
    	}
    }
    
    
    private double max(double[] array){
    	double max = 0;
    	for (int index = 0; index < array.length; index++){
    		if (array[index] > max)
    			max = array[index];
    	}
    	return max;
    }
    
    private double min(double[] array){
    	double min = array[0];
    	for (int index= 0; index < array.length; index++){
    		if (array[index] < min)
    			min = array[index];
    	}
    	return min;
    }
	private double[] doExpansion(double[] array, int blocksStart, int blocksEnd, int maxEdge){
		int numBlocks = blocksEnd - blocksStart;
    	double[] tmpArray = new double[numBlocks];
    	System.arraycopy( array, blocksStart, tmpArray, 0, numBlocks );

    	double maxX = max(tmpArray);
    	double minX = min(tmpArray);
    	double centerX = (maxX + minX)/2;
    	double scaleXRight = Math.abs((maxEdge - centerX)/(maxX - centerX));
    	double scaleXLeft = Math.abs((centerX - 1)/(centerX - minX));
    	
    	for(int index = 0; index < numBlocks; index++){
    		if (tmpArray[index] > centerX){ 
    			tmpArray[index] = centerX + scaleXRight * (tmpArray[index] - centerX);
    		}
    		else {
    			tmpArray[index] = centerX - scaleXLeft * (centerX - tmpArray[index]);
    		}
    	}
    	double[] expandedArray = new double[array.length];
    	for(int index = 0; index < blocksStart; index++){
    		expandedArray[index] = array[index];
    	}
    	for(int index = 0; index < numBlocks; index++){
    		expandedArray[index + blocksStart] = tmpArray[index];
    	}
    	for(int index = blocksEnd; index < array.length; index++){
    		expandedArray[index] = array[index];
    	}
//    	System.out.println("blocksStart: " + blocksStart + " blocksEnd: " + blocksEnd + " numBlocks: " + numBlocks);
		return expandedArray;
    }    

	private double calculateMovement( double x1, double y1, int x, int y ){
    	double movement = Math.abs(x1 - x) + Math.abs(y1 - y);
    	return movement;
		  	
    }
    private class LegalizerBlock {
    	int id;
    	double x;
    	double y;
    	
    	LegalizerBlock(int id, double x, double y){
    		this.id = id;
    		this.x = x;
    		this.y = y;
    	}
       		    	
        boolean inRow(Row row){
        	return (this.x >= row.left && this.x < row.right && this.y >= row.bottom && this.y < row.top);
        }
        
        @Override
        public String toString() {
            return String.format("[%.2f, %.2f]", this.x, this.y);
        }
    }
    
    private class Row {
    	int left, right, bottom, top;
        List<LegalizerBlock> blocks;
        

        Row(int left, int right, int bottom, int top, List<LegalizerBlock> blocks) {
            this.left = left;
            this.right = right;
            this.bottom = bottom;
            this.top = this.bottom + 1;
            
            this.blocks = new ArrayList<LegalizerBlock>();
            
            for(LegalizerBlock block:blocks){
            	if(block.inRow(this)){
            		this.blocks.add(block);
            	}
            }
        }
        
       
        
        
        LegalizerBlock getFirstBlock(Comparator<LegalizerBlock> comparator){
        	LegalizerBlock firstBlock = this.blocks.get(0);
        	for(LegalizerBlock block: this.blocks){
        		if(comparator.compare(block, firstBlock) < 0){
        			firstBlock = block;
        		}
        	}
        	return firstBlock;
        }
        LegalizerBlock getLastBlock(Comparator<LegalizerBlock> comparator){
        	LegalizerBlock lastBlock = this.blocks.get(0);
        	for(LegalizerBlock block: this.blocks){
        		if(comparator.compare(block, lastBlock) > 0){
        			lastBlock = block;
        		}
        	}
        	return lastBlock;
        }
        
        
        int capacity(){
        	return this.area();
        }
        
        int occupation(){
        	return this.blocks.size();
        }
        
        double utilisation(){
        	return (double)this.occupation() / (double)this.capacity();
        }

        int width(){
        	return this.right - this.left;
        }
        int height(){
        	return this.top - this.bottom;
        }
        int area(){
        	return this.width() * this.height();
        }
        
        @Override
        public String toString() {
            return String.format("h: [%d, %d],\tv: [%d, %d]\t%d\t%d blocks", this.left, this.right, this.bottom, this.top, this.area(), this.blocks.size());
        }
    }
    
    public static class Comparators {
        public static Comparator<LegalizerBlock> HORIZONTAL = new Comparator<LegalizerBlock>() {
            @Override
            public int compare(LegalizerBlock b1, LegalizerBlock b2) {
                return Double.compare(b1.x, b2.x);
            }
        };
        public static Comparator<LegalizerBlock> VERTICAL = new Comparator<LegalizerBlock>() {
            @Override
            public int compare(LegalizerBlock b1, LegalizerBlock b2) {
                return Double.compare(b1.y, b2.y);
            }
        };
    }
    
    @Override
    protected void initializeLegalizationAreas(){
    	return;
    }

    @Override
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}