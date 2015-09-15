package flexible_architecture;

import java.util.List;
import java.util.Map;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.block.AbstractBlock;

public class Circuit {
	
	private NetParser netparser;
	private Map<BlockType, List<AbstractBlock>> blocks;
	private FlexibleArchitecture architecture;
	
	
	public Circuit(FlexibleArchitecture architecture, String filename) {
		this.netparser = new NetParser(filename);
		this.architecture = architecture;
	}
	
	public void parse() {
		this.blocks = netparser.parse();
		this.architecture.loadBlocks(this.blocks);
	}
}
