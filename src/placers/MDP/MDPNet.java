package placers.MDP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import circuit.Net;
import circuit.Block;

public class MDPNet {
	
	Net originalNet;
	private ArrayList<MDPBlock> blocks;
	private MDPPoint[] bounds = new MDPPoint[2];
	
	private MDPBlockComparatorX comparatorX = new MDPBlockComparatorX();
	private MDPBlockComparatorY comparatorY = new MDPBlockComparatorY();
	
	
	public MDPNet(Net originalNet, ArrayList<MDPBlock> blocks) {
		this.originalNet = originalNet;
		this.blocks = blocks;
		
		this.bounds[0] = new MDPPoint();
		this.bounds[1] = new MDPPoint();
		this.updateBounds();
	}
	
	
	
	
	private void updateBounds() {
		updateBounds(Axis.X);
		updateBounds(Axis.Y);
	}
	
	private void updateBounds(Axis axis) {
		this.updateMin(axis);
		this.updateMax(axis);
	}
	
	private void updateMin(Axis axis) {
		this.bounds[0].set(axis, getMin(axis));
	}
	private void updateMax(Axis axis) {
		this.bounds[1].set(axis, getMax(axis));
	}
	
	public int getMin(Axis axis) {
		if(axis == Axis.X) {
			return Collections.min(this.blocks, comparatorX).coor.x;
		} else {
			return Collections.min(this.blocks, comparatorY).coor.y;
		}
	}
	public int getMax(Axis axis) {
		if(axis == Axis.X) {
			return Collections.max(this.blocks, comparatorX).coor.x;
		} else {
			return Collections.max(this.blocks, comparatorY).coor.y;
		}
	}
	
	
	
	public void updateBounds(Axis axis, int oldPosition, int newPosition) {
		/*if(oldPosition == this.bounds[0].get(axis)) {
			this.updateMin(axis);
		} else if(oldPosition == this.bounds[1].get(axis)) {
			this.updateMax(axis);
		}
		
		if(newPosition < this.bounds[0].get(axis)) {
			this.bounds[0].set(axis, newPosition);
		} else if(newPosition > this.bounds[1].get(axis)) {
			this.bounds[1].set(axis, newPosition);
		}*/
		
		this.updateBounds(axis);
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
			int center = (int) Math.floor(this.getCenter(axis));
			
			block.coor.set(axis, center);
			bounds[0] = getMin(axis);
			block.coor.set(axis, position);
			
		} else if(position == bounds[1]) {
			int center = (int) Math.ceil(this.getCenter(axis));
			
			block.coor.set(axis, center);
			bounds[1] = getMax(axis);
			block.coor.set(axis, position);
		}
		
		return bounds;
	}
	
	
	public String toString() {
		return this.originalNet.toString();
	}
}
