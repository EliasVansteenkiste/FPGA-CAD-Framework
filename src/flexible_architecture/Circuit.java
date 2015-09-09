package flexible_architecture;

import java.util.List;

import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.block.AbstractBlock;

public class Circuit {
	
	private NetParser netparser;
	private List<AbstractBlock> blocks;
	private FlexibleArchitecture architecture;
	
	
	public Circuit(String filename) {
		this.netparser = new NetParser(filename);
		this.architecture = new FlexibleArchitecture();
	}
	
	public void build() {
		this.blocks = netparser.parse();
	}
}
