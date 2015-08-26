package placers.MDP;

import java.util.ArrayList;
import java.util.Vector;

import circuit.Block;
import circuit.Clb;
import circuit.Input;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;
import architecture.Architecture;
import architecture.Site;

public class MDPPlacement {
	
	private Architecture architecture;
	private PackedCircuit circuit;
	
	private int width, height, n;
	private MDPBlock[][][] blocks;
	
	
	public MDPPlacement(Architecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
		
		this.width = architecture.getWidth();
		this.height = architecture.getHeight();
		this.n = architecture.getN();
		
		// + 2 is for 2 rows and 2 colums of IO blocks
		this.blocks = new MDPBlock[this.width + 2][this.height + 2][this.n];
		
		this.loadBlocks();
	}
	
	private void loadBlocks() {
		
		for(Net originalNet : this.circuit.getNets().values()) {
			ArrayList<MDPBlock> blocks = new ArrayList<>();
			
			blocks.add(this.getMDPBlock(originalNet.source.owner));
			for(Pin sink : originalNet.sinks) {
				blocks.add(this.getMDPBlock(sink.owner));
			}
			
			
			MDPNet net = new MDPNet(originalNet, blocks);
			
			
			for(MDPBlock block : blocks) {
				block.addNet(net);
			}
		}
	}
	
	
	private MDPBlock getMDPBlock(Block block) {
		Site site = block.getSite();
		int x = site.x;
		int y = site.y;
		int n = site.n;
		
		if(this.blocks[x][y][n] == null) {
			this.blocks[x][y][n] = new MDPBlock(block);
		}
		
		return this.blocks[x][y][n];
	}
}
