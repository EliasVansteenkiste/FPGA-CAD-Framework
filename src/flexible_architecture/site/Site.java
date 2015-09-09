package flexible_architecture.site;

import util.Logger;
import flexible_architecture.block.GlobalBlock;

public class Site extends AbstractSite {
	
	private GlobalBlock block;
	
	public Site(int x, int y) {
		super(x, y);
	}
	
	
	public GlobalBlock getBlock() {
		return this.block;
	}
	
	public void setBlock(GlobalBlock block) {
		if(this.block == null) {
			this.block = block;
		
		} else {
			Logger.raise("Tried to set the block on a site that is already occupied");
		}
	}
	
	public GlobalBlock removeBlock() {
		GlobalBlock block = this.block;
		this.block = null;
		return block;
	}
}
