package flexible_architecture.block;

import java.util.Collection;
import java.util.Random;

import flexible_architecture.architecture.BlockType;

public abstract class AbstractSite {
	
	private int x, y;
	private BlockType blockType;
	
	public AbstractSite(int x, int y, BlockType blockType) {
		this.x = x;
		this.y = y;
		this.blockType = blockType;
	}
	
	public int getX() {
		return this.x;
	}
	public int getY() {
		return this.y;
	}
	public BlockType getType() {
		return this.blockType;
	}
	
	
	public abstract GlobalBlock getRandomBlock(Random random);
	abstract void addBlock(GlobalBlock block);
	public abstract void removeBlock(GlobalBlock block);
	public abstract void clear();
	
	public abstract Collection<GlobalBlock> getBlocks();
	
	
	
	public String toString() {
		return "[" + this.x + ", " + this.y + "]";
	}
}
