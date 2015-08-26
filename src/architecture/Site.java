package architecture;

import architecture.old.RouteNode;
import circuit.Block;

public class Site {
	public int x;
	public int y;
	public int n;
	public SiteType type;
	public Block block;
	private String naam;
	public RouteNode source;
	public RouteNode sink;
	
	public Site(int x, int y, int n, SiteType t, String naam)
	{
		super();	
		this.x=x;
		this.y=y;
		this.n=n;
		this.type=t;
		this.naam=naam;
	}
	
	public Block setBlock(Block block) {
		Block oldBlock = this.block;
		this.block = block;
		return oldBlock;
	}
	public Block getBlock() {
		return this.block;
	}
	
	double afstand(Site p)
	{
		return Math.abs(x-p.x)+Math.abs(y-p.y);
	}

	@Override
	public String toString()
	{
		return naam;
	}
	
}
