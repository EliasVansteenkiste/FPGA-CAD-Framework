package architecture;

public class Site {
	private int x;
	private int y;
	private SiteType type;
	
	public Site(int x, int y, SiteType type)
	{
		super();	
		this.x=x;
		this.y=y;
		this.type=type;
	}
	
	public SiteType getType()
	{
		return type;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	@Override
	public String toString()
	{
		return "Site_" + x + "_" + y;
	}
	
}
