package flexible_architecture.block;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;

public class LocalBlock extends AbstractBlock {
	
	
	private AbstractBlock parent;
	
	public LocalBlock(String name, BlockType type, AbstractBlock parent) {
		super(name, type);
		
		this.parent = parent;
	}
	
	public AbstractBlock getParent() {
		return this.parent;
	}
}
