package flexible_architecture.site;

import flexible_architecture.block.AbstractBlock;

public abstract class AbstractSite {
	
	private int x, y;
	private int height;
	
	public AbstractSite(int x, int y, int height) {
		this.x = x;
		this.y = y;
		this.height = height;
	}
	
	
	public void setBlock(AbstractBlock block) {
		
	}
}
