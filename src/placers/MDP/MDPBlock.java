package placers.MDP;

import mathtools.QuickSelect;
import circuit.Block;

public class MDPBlock {
	
	private Block originalBlock;
	private MDPNet[] nets;
	private int numNets = 0;
	private int maxNets;
	
	public MDPPoint coor;
	public int[] optimalInterval = new int[2];
	
	
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
		int[] minBounds = new int[numNets];
		int[] maxBounds = new int[numNets];
		
		
		for(int i = 0; i < numNets; i++) {
			tmpBounds = nets[i].getExBounds(axis, this);
			minBounds[i] = tmpBounds[0];
			maxBounds[i] = tmpBounds[1];
		}
		
		if(this.numNets == 1) {
			this.optimalInterval[0] = minBounds[0];
			this.optimalInterval[1] = maxBounds[0];
		} else {
			this.optimalInterval[0] = QuickSelect.select(minBounds, minBounds.length / 2);
			this.optimalInterval[1] = QuickSelect.select(maxBounds, (int) Math.ceil(maxBounds.length / 2.));
		}
		if(this.optimalInterval[1] < this.optimalInterval[0]) {
			System.out.println("ok");
		}
	}
	
	public String toString() {
		return this.originalBlock.toString();
	}

	public Block getOriginalBlock() {
		return this.originalBlock;
	}
}
