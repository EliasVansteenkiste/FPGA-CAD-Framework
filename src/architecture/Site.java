package architecture;

import circuit.Block;

public abstract class Site
{
	
	private int z;
	private GridTile tile;
	
	public Site(int z, GridTile tyle)
	{
		this.z = z;
		this.tile = tyle;
	}
	
	public GridTile getTyle()
	{
		return tile;
	}
	
	public int getX()
	{
		return tile.getX();
	}
	
	public int getY()
	{
		return tile.getY();
	}
	
	public int getZ()
	{
		return z;
	}
	
	public SiteType getType()
	{
		return tile.getType();
	}
	
	public abstract void setBlock(Block block);
	
	public abstract Block getBlock();
	
	@Override
	public String toString()
	{
		return "Site_" + tile.getX() + "_" + tile.getY() + "_" + z;
	}
	
}
