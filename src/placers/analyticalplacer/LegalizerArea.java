package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.List;

import architecture.BlockType;


public class LegalizerArea {
	
	int top, bottom, left, right;
	
	private boolean absorbed = false;
	
	private double tileCapacity;
	private int blockHeight, blockRepeat;
	
	private int numTiles = 0;
	private List<Integer> blockIndexes = new ArrayList<Integer>();
	
	LegalizerArea(LegalizerArea a) {
		this.top = a.top;
		this.bottom = a.bottom;
		this.left = a.left;
		this.right = a.right;
		
		this.tileCapacity = a.tileCapacity;
		this.blockHeight = a.blockHeight;
		this.blockRepeat = a.blockRepeat;
	}
	
	LegalizerArea(int x, int y, double tileCapacity, BlockType blockType) {
		this.left = x;
		this.right = x;
		this.top = y;
		this.bottom = y;
		
		this.tileCapacity = tileCapacity;
		this.blockHeight = blockType.getHeight();
		this.blockRepeat = blockType.getRepeat();
	}
	
	
	int getBlockHeight() {
		return this.blockHeight;
	}
	int getBlockRepeat() {
		return this.blockRepeat;
	}
	
	
	void grow(int[] direction) {
		this.grow(direction[0], direction[1]);
	}
	void grow(int horizontal, int vertical) {
		if(horizontal == -1) {
			this.left -= this.blockRepeat;
		
		} else if(horizontal == 1) {
			this.right += this.blockRepeat;
		
		} else if(vertical == -1) {
			this.top -= this.blockHeight;
		
		} else if(vertical == 1) {
			this.bottom += this.blockHeight;
		}
	}
	
	void absorb() {
		this.absorbed = true;
	}
	
	boolean isAbsorbed() {
		return this.absorbed;
	}
	
	
	void incrementTiles() {
		this.numTiles++;
	}
	double getCapacity() {
		return this.numTiles * this.tileCapacity;
	}
	
	
	void addBlockIndexes(List<Integer> blockIndexes) {
		this.blockIndexes.addAll(blockIndexes);
	}
	List<Integer> getBlockIndexes() {
		return this.blockIndexes;
	}
	int getOccupation() {
		return this.blockIndexes.size();
	}
}
