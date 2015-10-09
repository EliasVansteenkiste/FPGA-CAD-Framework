package flexible_architecture.block;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import util.Logger;

import flexible_architecture.architecture.BlockType;

public class IOSite extends AbstractSite {
	
	private int capacity;
	private Set<GlobalBlock> blocks;
	
	public IOSite(int x, int y, BlockType blockType, int capacity) {
		super(x, y, blockType);
		this.capacity = capacity;
		this.blocks = new HashSet<GlobalBlock>(capacity);
	}
	
	
	
	public GlobalBlock getRandomBlock(Random random) {
		int size = this.blocks.size();
		if(size == 0) {
			return null;
		}
		
		int index = random.nextInt(size);
		Iterator<GlobalBlock> iter = this.blocks.iterator();
		for(int i = 0; i < index; i++) {
		    iter.next();
		}
		
		return iter.next();		
	}
	
	void addBlock(GlobalBlock block) {
		if(this.blocks.size() == this.capacity) {
			Logger.raise("Tried to add a block to a full IO site");
		} else {
			this.blocks.add(block);
		}
	}
	
	public void removeBlock(GlobalBlock block) {
		boolean success = this.blocks.remove(block);
		if(!success) {
			Logger.raise("Trying to remove a block that is not present in site");
		}
	}
	
	public void clear() {
		this.blocks.clear();
	}
	
	
	public Collection<GlobalBlock> getBlocks() {
		return this.blocks;
	}
}
