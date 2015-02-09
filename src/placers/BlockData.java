package placers;

import java.util.HashSet;
import java.util.Set;

import circuit.Block;
import circuit.Clb;
import circuit.Input;
import circuit.Output;
import circuit.Pin;

public class BlockData {
	Block block;
	public Set<Block> blocks;
	
	public boolean fixed;
	
	public BlockData(){
		blocks = new HashSet<Block>();
	}

	public BlockData(Block block) {
		super();
		this.block = block;
		
		blocks = new HashSet<Block>();
		
		switch (block.type) {
		case INPUT:
			Input input=(Input)block;
			blocks.add(input.output.routingBlock);
			break;
		case OUTPUT:
			Output output=(Output)block;
			blocks.add(output.input.routingBlock);
			break;
		case CLB:
			Clb clb=(Clb)block;
			for (Pin p:clb.input) {
				if(p.routingBlock!=null)blocks.add(p.routingBlock);
			}
			for (Pin p:clb.output) {
				if(p.routingBlock!=null)blocks.add(p.routingBlock);
			}
			if(clb.clock.routingBlock!=null)blocks.add(clb.clock.routingBlock);

			break;
		default:
			System.out.println("Error: wrong block type");
		}
	}
}
