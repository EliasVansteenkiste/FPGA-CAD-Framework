package flexible_architecture.site;

import flexible_architecture.block.AbstractBlock;

public abstract class AbstractSite {
	
	private int x, y;
	private int height;
	private AbstractBlock block;
	
	public AbstractSite(int x, int y, int height) {
		this.x = x;
		this.y = y;
		this.height = height;
	}
	
	public int getX() {
		return this.x;
	}
	public int getY() {
		return this.y;
	}
	
	
	public AbstractBlock getBlock() {
		return this.block;
	}
	public void setBlock(AbstractBlock block) {
		this.block = block;
	}
}
