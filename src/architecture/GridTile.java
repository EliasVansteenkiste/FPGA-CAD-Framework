package architecture;

public class GridTile
{

	private int x;
	private int y;
	private SiteType type;
	private Site[] sites;
	
	private GridTile(SiteType type, int capacity, int x, int y, String hbTypeName)
	{
		this.x = x;
		this.y = y;
		this.type = type;
		sites = new Site[capacity];
		for(int i = 0; i < capacity; i++)
		{
			if(type == SiteType.CLB)
			{
				sites[i] = new ClbSite(i, this);
			}
			else
			{
				if(type == SiteType.HARDBLOCK)
				{
					sites[i] = new HardBlockSite(hbTypeName, i, this);
				}
				else //Type must be IO
				{
					sites[i] = new IoSite(i, this);
				}
			}
		}
	}
	
	public static GridTile constructClbGridTile(int x, int y)
	{
		return new GridTile(SiteType.CLB, 1, x, y, null);
	}
	
	public static GridTile constructIOGridTile(int x, int y, int capacity)
	{
		return new GridTile(SiteType.IO, capacity, x, y, null);
	}
	
	public static GridTile constructHardBlockGridTile(int x, int y, String hbTypeName)
	{
		return new GridTile(SiteType.HARDBLOCK, 1, x, y, hbTypeName);
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
	
	public Site getSite(int z)
	{
		return sites[z];
	}
	
	public int getCapacity()
	{
		return sites.length;
	}
	
}
