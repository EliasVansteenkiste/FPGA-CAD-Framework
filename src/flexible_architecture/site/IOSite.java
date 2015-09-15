package flexible_architecture.site;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.Logger;

import flexible_architecture.block.GlobalBlock;

public class IOSite extends AbstractSite {
	
	private int capacity;
	private Set<GlobalBlock> blocks;
	
	public IOSite(int x, int y, int capacity) {
		super(x, y, 1);
		this.capacity = capacity;
		this.blocks = new HashSet<GlobalBlock>(capacity);
	}
	
	
	public Set<GlobalBlock> getBlocks() {
		return this.blocks;
	}
	
	public void addBlock(GlobalBlock block) {
		if(this.blocks.size() == this.capacity) {
			Logger.raise("Tried to add a block to a full IO site");
		} else {
			this.blocks.add(block);
		}
	}
	
	public boolean removeBlock(GlobalBlock block) {
		return this.blocks.remove(block);
	}
}
