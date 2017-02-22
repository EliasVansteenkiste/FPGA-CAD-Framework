package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.placers.analytical.GradientLegalizer.LegalizerBlock;

class DetailedLegalizer {

	private final int width, height;
    private final boolean[][] isLegal;
    private final LegalizerBlock[][] legalMap;
    private final ArrayList<LegalizerBlock> unplacedBlocks;
    private final ArrayList<LegalizerBlock> moveBlocks;

    DetailedLegalizer(int width, int height){
    	this.width = width;
    	this.height = height;
    	
    	this.isLegal = new boolean[this.width + 2][this.height + 2];
    	this.legalMap = new LegalizerBlock[this.width + 2][this.height + 2];
    	this.unplacedBlocks = new ArrayList<LegalizerBlock>();
    	this.moveBlocks = new ArrayList<LegalizerBlock>();
    }
  
    public void shiftLegal(LegalizerBlock[] blocks, BlockType blockType, Circuit circuit, double maxIllegalRatio){//TODO ADD BLOCK HEIGHT
    	
    	this.resetLegalMap();
    	this.setLegal(blockType, circuit);
    	
    	for(LegalizerBlock block:blocks){
    		int x = block.legalHorizontal();
    		int y = block.legalVertical();

    		int height = block.height();

    		boolean legal = true;
    		for(int h = 0; h < height; h++){
    			if(!this.legalPostion(x, y + h)){
    				legal = false;
    			}
    		}
    		if(legal){
    			for(int h = 0; h < height; h++){
    				this.legalMap[x][y + h] = block;
    			}
    		}else{
    			this.unplacedBlocks.add(block);
    		}
    	}
    	if(this.unplacedBlocks.size() < blocks.length * maxIllegalRatio){
    		
    		while(!this.unplacedBlocks.isEmpty()){
    			
    			//Find block that leads to minimal displacement
    			LegalizerBlock overlappingBlock = this.unplacedBlocks.get(0);
    			int minimalDisplacement = this.legalizationDisplacement(overlappingBlock);
    			
    			for(LegalizerBlock candidateBlock:this.unplacedBlocks){
    				int legalizeDisplacement = this.legalizationDisplacement(candidateBlock);
    				if(legalizeDisplacement < minimalDisplacement){
    					overlappingBlock = candidateBlock;
    					minimalDisplacement = legalizeDisplacement;
    				}
    			}
    			
    			this.unplacedBlocks.remove(overlappingBlock);
        		
        		Direction movingDirection = bestMovingDirection(overlappingBlock);

        		int x = overlappingBlock.legalHorizontal();
        		int y = overlappingBlock.legalVertical();

        		this.moveBlocks.clear();
        		
        		if(movingDirection.equals(Direction.LEFT)){
        			while(!this.legalPostion(x, y)){
        				if(this.isLegal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			x--;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock moveBlock:this.moveBlocks){
        				x = moveBlock.legalHorizontal();
        				y = moveBlock.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					x--;
        				}
        				this.legalMap[x][y] = moveBlock;
        				this.legalMap[moveBlock.legalHorizontal()][moveBlock.legalVertical()] = null;
        				moveBlock.setHorizontal(x);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			x--;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setHorizontal(x);
        		}else if(movingDirection.equals(Direction.RIGHT)){
        			while(!this.legalPostion(x, y)){
        				if(this.isLegal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			x++;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock moveBlock:this.moveBlocks){
        				x = moveBlock.legalHorizontal();
        				y = moveBlock.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					x++;
        				}
        				
        				this.legalMap[x][y] = moveBlock;
        				this.legalMap[moveBlock.legalHorizontal()][moveBlock.legalVertical()] = null;
        				moveBlock.setHorizontal(x);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			x++;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setHorizontal(x);
        		}else if(movingDirection.equals(Direction.DOWN)){
        			while(!this.legalPostion(x, y)){
        				if(this.isLegal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			y--;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock moveBlock:this.moveBlocks){
        				x = moveBlock.legalHorizontal();
        				y = moveBlock.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					y--;
        				}
        				
        				this.legalMap[x][y] = moveBlock;
        				this.legalMap[moveBlock.legalHorizontal()][moveBlock.legalVertical()] = null;
        				moveBlock.setVertical(y);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			y--;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setVertical(y);
        		}else if(movingDirection.equals(Direction.UP)){
        			while(!this.legalPostion(x, y)){
        				if(this.isLegal[x][y]){
        					this.moveBlocks.add(this.legalMap[x][y]);
        				}
            			y++;
            		}
        			Collections.reverse(this.moveBlocks);
        			for(LegalizerBlock block:this.moveBlocks){
        				x = block.legalHorizontal();
        				y = block.legalVertical();
        				
        				while(!this.legalPostion(x, y)){
        					y++;
        				}
        				
        				this.legalMap[x][y] = block;
                		
        				this.legalMap[block.legalHorizontal()][block.legalVertical()] = null;
        				block.setVertical(y);
        			}
        			
        			//Set the coordinate of the overlapping block
            		x = overlappingBlock.legalHorizontal();
            		y = overlappingBlock.legalVertical();
            		while(!this.legalPostion(x, y)){
            			y++;
            		}
        			this.legalMap[x][y] = overlappingBlock;
        			overlappingBlock.setVertical(y);
        		}
        	}
    	}
    }
    private void resetLegalMap(){
    	int width = this.legalMap.length;
    	int height = this.legalMap[0].length;
    	for(int x = 0; x < width; x++){
    		for(int y = 0; y < height; y++){
    			this.legalMap[x][y] = null;
    		}
    	}
    	
    	this.unplacedBlocks.clear();
    	this.moveBlocks.clear();
    }
    private void setLegal(BlockType blockType, Circuit circuit){
    	for(int x = 0; x < this.width + 2; x++){
    		for(int y = 0; y < this.height + 2; y++){
    			this.isLegal[x][y] = false;
    		}
    	}
    	for(int x = 1; x < this.width + 1; x++){
    		if(circuit.getColumnType(x).equals(blockType)){
    			for(int y = 1; y < this.height + 1; y++){
	    			this.isLegal[x][y] = true;
	    		}
    		}
    	}
    }
    private boolean legalPostion(int x, int y){
    	if(this.isLegal[x][y] && this.legalMap[x][y] == null){
    		return true;
    	}else{
    		return false;
    	}
    }
    private int legalizationDisplacement(LegalizerBlock block){
		int left = this.displacement(block, Direction.LEFT);
		int right = this.displacement(block, Direction.RIGHT);
		int down = this.displacement(block, Direction.DOWN);
		int up = this.displacement(block, Direction.UP);
		
		return Math.min(Math.min(left, right), Math.min(down, up));
    }
    private int displacement(LegalizerBlock block, Direction direction){
    	int x = block.legalHorizontal();
		int y = block.legalVertical();
		
		if(direction.equals(Direction.LEFT)){
			int left = 0;
			while(!this.legalPostion(x, y)){
				left++;
				x--;
				
				if(x == 0){
					return Integer.MAX_VALUE;
				}
			}
			return left;
		}else if(direction.equals(Direction.RIGHT)){
			int right = 0;
			while(!this.legalPostion(x, y)){
				right++;
				x++;
				
				if(x == this.width + 1){
					return Integer.MAX_VALUE;
				}
			}
			return right;
		}else if(direction.equals(Direction.DOWN)){
			int down = 0;
			while(!this.legalPostion(x,y)){
				down++;
				y--;
				
				if(y == 0){
					return Integer.MAX_VALUE;
				}
			}
			return down;
		}else if(direction.equals(Direction.UP)){
			int up = 0;
			while(!this.legalPostion(x, y)){
				up++;
				y++;
				
				if(y == this.height + 1){
					return Integer.MAX_VALUE;
				}
			}
			return up;
		}
		return 0;
    }
    private Direction bestMovingDirection(LegalizerBlock block){
		int left = this.displacement(block, Direction.LEFT);
		int right = this.displacement(block, Direction.RIGHT);
		int down = this.displacement(block, Direction.DOWN);
		int up = this.displacement(block, Direction.UP);

		int min = Math.min(Math.min(left, right), Math.min(down, up));
		
		if(min == Integer.MAX_VALUE){
			System.out.println("All FPGA boundaries hit during shifting legalization of the blocks");
			return null;
		}

		if(left == min){
			return Direction.LEFT;
		}else if(right == min){
			return Direction.RIGHT;
		}else if(down == min){
			return Direction.DOWN;
		}else if(up == min){
			return Direction.UP;
		}
		
		System.out.println("Problem in best moving direction => min displacement equals " + min);
		return null;
    }
    
    private enum Direction {
    	LEFT,
    	RIGHT,
    	UP,
    	DOWN
    }
}