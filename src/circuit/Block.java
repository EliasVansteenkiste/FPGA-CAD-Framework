package circuit;

import architecture.Site;

public class Block {
	public String name;
	public BlockType type;
	
	public Site site;
	public boolean fixed;

	public Block(String name, BlockType type) {
		super();
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode()^type.hashCode();
	}
	
	
	
}
