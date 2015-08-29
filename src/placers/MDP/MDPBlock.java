package placers.MDP;

import mathtools.QuickSelect;
import circuit.Block;

public class MDPBlock {
	
	private Block originalBlock;
	private MDPNet[] nets;
	private int numNets = 0;
	
	public MDPPoint coor;
	public int[] optimalInterval = new int[2];
	
	public MDPBlock(Block originalBlock) {
		this.originalBlock = originalBlock;
		this.coor = new MDPPoint(originalBlock.getSite().getTile().getX(), originalBlock.getSite().getTile().getY());
		
		int maxNets = originalBlock.maxNets();
		this.nets = new MDPNet[maxNets];
	}
	
	public void addNet(MDPNet net) {
		this.nets[this.numNets] = net;
		this.numNets += 1;
	}
	
	public void move(Axis axis, int position) {
		int oldPosition = this.coor.set(axis, position);
		
		for(int i = 0; i < this.numNets; i++) {
			this.nets[i].updateBounds(axis, oldPosition);
		}
	}
	
	public void calculateOptimalInterval(Axis axis) {
		
		int[] tmpBounds = new int[2];
		int[] leftBounds = new int[numNets];
		int[] rightBounds = new int[numNets];
		
		
		for(int i = 0; i < numNets; i++) {
			tmpBounds = nets[i].getExBounds(axis, this);
			leftBounds[i] = tmpBounds[0];
			rightBounds[i] = tmpBounds[1];
		}
		
		this.optimalInterval[0] = QuickSelect.select(leftBounds, leftBounds.length / 2);
		this.optimalInterval[1] = QuickSelect.select(rightBounds, (int) Math.ceil(rightBounds.length / 2.));
	}
}
