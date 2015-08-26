package placers.MDP;

import java.util.Collection;

import circuit.Net;
import circuit.Block;

public class MDPNet {
	
	private Net originalNet;
	private MDPBlock[] blocks;
	
	public MDPNet(Net originalNet, Collection<MDPBlock> blocks) {
		this.originalNet = originalNet;
		this.blocks = blocks.toArray(new MDPBlock[blocks.size()]);
	}
}
