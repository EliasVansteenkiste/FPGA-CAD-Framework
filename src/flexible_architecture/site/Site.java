package flexible_architecture.site;

import java.util.Random;

import util.Logger;
import flexible_architecture.architecture.BlockType;
import flexible_architecture.block.GlobalBlock;

public class Site extends AbstractSite {
	
	private GlobalBlock block;
	
	public Site(int x, int y, BlockType blockType) {
		super(x, y, blockType);
	}
	
	
	
	public GlobalBlock getRandomBlock(Random random) {
		return this.block;
	}
	
	public void addBlock(GlobalBlock block) {
		if(this.block == null) {
			this.block = block;
		} else {
			Logger.raise("Trying to set the block on a non-empty site");
		}
	}
	
	private GlobalBlock removeBlock() {
		GlobalBlock oldBlock = this.block;
		this.block = null;
		return oldBlock;
	}
	public void removeBlock(GlobalBlock block) {
		if(block == this.block) {
			this.removeBlock();
		} else {
			Logger.raise("Trying to remove a block that is not present in site");
		}
	}
}
