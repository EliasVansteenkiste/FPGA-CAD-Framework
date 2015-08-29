package architecture;

import circuit.Block;

public abstract class Site
{
	
	private int z;
	private GridTile tile;
	private Block block = null;
	
	public Site(int z, GridTile tile)
	{
		this.z = z;
		this.tile = tile;
	}
	

	
	public Block getBlock() {
		return this.block;
	}
	public Block setBlock(Block block) {
		Block oldBlock = this.block;
		this.block = block;
		return oldBlock;
	}
	
	
	public GridTile getTile() {
		return this.tile;
	}
	public GridTile setTile(GridTile tile) {
		GridTile oldTile = this.tile;
		this.tile = tile;
		return oldTile;
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
	
	
	@Override
	public String toString()
	{
		return "Site_" + tile.getX() + "_" + tile.getY() + "_" + z;
	}
	
}
