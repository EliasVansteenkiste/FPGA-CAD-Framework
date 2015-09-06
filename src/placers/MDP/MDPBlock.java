package placers.MDP;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import mathtools.QuickSelect;
import circuit.Block;

public class MDPBlock {
	
	public Block originalBlock;
	private MDPNet[] nets;
	private int numNets = 0;
	private int maxNets;
	
	public MDPPoint coor;
	public int[] optimalInterval = new int[2];
	public int optimalPosition;
	
	
	public MDPBlock(Block originalBlock) {
		this.originalBlock = originalBlock;
		this.coor = new MDPPoint(originalBlock.getSite().getTile().getX(), originalBlock.getSite().getTile().getY());
		
		this.maxNets = originalBlock.maxNets();
		this.nets = new MDPNet[this.maxNets];
	}
	
	public void addNet(MDPNet net) {
		this.nets[this.numNets] = net;
		this.numNets += 1;
	}
	
	public void move(Axis axis, int position) {
		int oldPosition = this.coor.get(axis);
		this.coor.set(axis, position);
		
		for(int i = 0; i < this.numNets; i++) {
			this.nets[i].updateBounds(axis, oldPosition, position);
			assert(this.nets[i].getMin(Axis.X) == this.nets[i].getBounds(Axis.X)[0]);
		}
	}
	
	public void calculateOptimalInterval(Axis axis) {
		
		int[] tmpBounds = new int[2];
		int[] allBounds = new int[this.numNets * 2];
		
		
		for(int i = 0; i < this.numNets; i++) {
			tmpBounds = this.nets[i].getExBounds(axis, this);
			System.arraycopy(tmpBounds, 0, allBounds, 2*i, 2);
		}
		
		Arrays.sort(allBounds);
		this.optimalInterval[0] = allBounds[this.numNets - 1];
		this.optimalInterval[1] = allBounds[this.numNets];
	}
	
	
	public String toString() {
		return this.originalBlock.toString();
	}

	public Block getOriginalBlock() {
		return this.originalBlock;
	}
}
