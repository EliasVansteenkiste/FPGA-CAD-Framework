package circuit;

import architecture.Site;

public abstract class Block {
	public String name;
	public BlockType type;
	
	private Site site;
	public boolean fixed;

	public Block(String name, BlockType type) {
		super();
		this.name = name;
		this.type = type;
	}
	
	public abstract int maxNets();

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode()^type.hashCode();
	}
	
	public Site getSite()
	{
		return this.site;
	}
	
	public void setSite(Site site)
	{
		this.site = site;
	}
	
}
