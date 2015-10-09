package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.List;

public class LegalizerArea {
	
	int top, bottom, left, right;
	
	private boolean absorbed = false;
	
	private double tileCapacity;
	private int numTiles = 0;
	private List<Integer> blockIndexes = new ArrayList<Integer>();
	
	LegalizerArea(LegalizerArea a) {
		this.top = a.top;
		this.bottom = a.bottom;
		this.left = a.left;
		this.right = a.right;
		
		this.tileCapacity = a.tileCapacity;
	}
	
	LegalizerArea(int x, int y, double tileCapacity) {
		this.left = x;
		this.right = x;
		this.top = y;
		this.bottom = y;
		
		this.tileCapacity = tileCapacity;
	}
	
	void grow(int[] direction) {
		if(direction[0] == -1) {
			this.left--;
		
		} else if(direction[0] == 1) {
			this.right++;
		
		} else if(direction[1] == -1) {
			this.top--;
		
		} else if(direction[1] == 1) {
			this.bottom++;
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
