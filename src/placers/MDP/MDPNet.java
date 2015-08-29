package placers.MDP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import circuit.Net;
import circuit.Block;

public class MDPNet {
	
	private Net originalNet;
	private ArrayList<MDPBlock> blocks;
	private MDPPoint[] bounds = new MDPPoint[2];
	
	private MDPBlockComparatorX comparatorX = new MDPBlockComparatorX();
	private MDPBlockComparatorY comparatorY = new MDPBlockComparatorY();
	
	
	public MDPNet(Net originalNet, ArrayList<MDPBlock> blocks) {
		this.originalNet = originalNet;
		this.blocks = blocks;
		
		this.updateBounds();
	}
	
	public void updateBounds() {
		this.updateBounds(Axis.X);
		this.updateBounds(Axis.Y);
	}
	
	public void updateBounds(Axis axis) {
		this.updateMin(axis);
		this.updateMax(axis);
	}
	
	public void updateMin(Axis axis) {
		if(axis == Axis.X) {
			updateMinX();
		} else {
			updateMinY();
		}
	}
	
	public void updateMax(Axis axis) {
		if(axis == Axis.X) {
			updateMaxX();
		} else {
			updateMaxY();
		}
	}
	
	public void updateMinX() {
		this.bounds[0].x = Collections.min(this.blocks, comparatorX).coor.x;
	}
	
	public void updateMaxX() {
		this.bounds[1].x = Collections.max(this.blocks, comparatorX).coor.x;
	}
	
	public void updateMinY() {
		this.bounds[0].y = Collections.min(this.blocks, comparatorY).coor.y;
	}
	
	public void updateMaxY() {
		this.bounds[1].y = Collections.max(this.blocks, comparatorY).coor.y;
	}
	
	
	
	public void updateBounds(Axis axis, int oldPosition) {
		if(oldPosition == this.bounds[0].get(axis)) {
			this.updateMin(axis);
		} else if(oldPosition == this.bounds[1].get(axis)) {
			this.updateMax(axis);
		}
	}
	
	
	public int[] getBounds(Axis axis) {
		int[] bounds = {this.bounds[0].get(axis), this.bounds[1].get(axis)};
		return bounds;
	}
	
	
	public double getCenter(Axis axis) {
		int sum = 0;
		for(MDPBlock block : this.blocks) {
			sum += block.coor.get(axis);
		}
		return (1. * sum) / this.blocks.size();
	}
	
	public int[] getExBounds(Axis axis, MDPBlock block) {
		int[] bounds = this.getBounds(axis);
		int position = block.coor.get(axis);
		
		if(position == bounds[0]) {
			bounds[0] = (int) Math.floor(this.getCenter(axis));
		} else if(position == bounds[1]) {
			bounds[1] = (int) Math.ceil(this.getCenter(axis));
		}
		
		return bounds;
	}
}
