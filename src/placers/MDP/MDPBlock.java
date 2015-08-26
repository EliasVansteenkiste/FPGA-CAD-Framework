package placers.MDP;

import circuit.Block;

public class MDPBlock {
	
	private Block originalBlock;
	private MDPNet[] nets;
	private int numNets = 0;
	
	public MDPBlock(Block originalBlock) {
		this.originalBlock = originalBlock;
		
		int maxNets = originalBlock.maxNets();
		nets = new MDPNet[maxNets];
	}
	
	public void addNet(MDPNet net) {
		nets[numNets] = net;
		numNets += 1;
	}
	
}
