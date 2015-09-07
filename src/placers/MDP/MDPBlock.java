package placers.MDP;

import java.util.Arrays;
import circuit.Block;

public class MDPBlock {
	
	public Block originalBlock;
	private MDPNet[] nets;
	private int numNets = 0;
	private int maxNets;
	
	public MDPPoint coor;
	private int[] exBounds;
	private int[] optimalInterval = new int[2];
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
	
	public void setCoor(Axis axis, int position) {
		int oldPosition = this.coor.get(axis);
		this.coor.set(axis, position);
		
		for(int i = 0; i < this.numNets; i++) {
			this.nets[i].updateBounds(axis, oldPosition, position);
			assert(this.nets[i].getMin(Axis.X) == this.nets[i].getBounds(Axis.X)[0]);
		}
	}
	
	private void calculateExBounds(Axis axis) {
		if(this.exBounds == null) {
			this.exBounds = new int[this.numNets * 2];
		}
		int[] tmpBounds = new int[2];
		for(int i = 0; i < this.numNets; i++) {
			tmpBounds = this.nets[i].getExBounds(axis, this);
			System.arraycopy(tmpBounds, 0, this.exBounds, 2*i, 2);
		}
	}
	
	public void calculateOptimalInterval(Axis axis) {
		
		this.calculateExBounds(axis);
		
		Arrays.sort(this.exBounds);
		this.optimalInterval[0] = this.exBounds[this.numNets - 1];
		this.optimalInterval[1] = this.exBounds[this.numNets];
	}
	
	
	public int[] getOptimalInterval() {
		return this.optimalInterval;
	}
	
	
	public double[] getCostInPartition(int[] partition) {
		int left = partition[0];
		int right = partition[1];
		int partitionSize = right - left + 1;
		
		
		double[] costs = new double[partitionSize];
		
		for(int direction = -1; direction <= 1; direction += 2) {
			int endIndex, boundIndex, endBoundIndex, cost = 0;
			if(direction == -1) {
				endIndex = left - 1;
				boundIndex = this.numNets - 1; 
				endBoundIndex = -1;
			} else {
				endIndex = right + 1;
				boundIndex = this.numNets;
				endBoundIndex = this.numNets * 2;
			}
			
			for(int index = this.optimalPosition; index != endIndex; index += direction) {
				costs[index - left] = cost;
				
				while(boundIndex != endBoundIndex && this.exBounds[boundIndex] == index) {
					cost += 1;
					boundIndex += direction;
				}
			}
		}
		
		
		return costs;
	}
	
	
	public String toString() {
		return this.originalBlock.toString();
	}

	public Block getOriginalBlock() {
		return this.originalBlock;
	}
}
