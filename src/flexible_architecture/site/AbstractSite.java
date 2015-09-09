package flexible_architecture.site;

import flexible_architecture.block.AbstractBlock;

public abstract class AbstractSite {
	
	private int x, y;
	
	public AbstractSite(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	
	public void setBlock(AbstractBlock block) {
		
	}
}
