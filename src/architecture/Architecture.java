package architecture;

import java.util.Random;
import java.util.Vector;

public abstract class Architecture {
	
	protected Random rand;
	protected GridTile[][] tileArray;
	private Vector<Site> siteVector;
	protected int width;
	protected int height;
	
	public Architecture()
	{
		super();
		rand= new Random();
		siteVector = new Vector<>();
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public Site getSite(int x, int y, int z)
	{
		return tileArray[x][y].getSite(z);
	}
	
	public GridTile getTile(int x, int y)
	{
		return tileArray[x][y];
	}
	
	protected void addTile(GridTile tile)
	{
		tileArray[tile.getX()][tile.getY()] = tile;
		for(int i = 0; i < tile.getCapacity(); i++)
		{
			siteVector.add(tile.getSite(i));
		}
	}
	
	public Vector<Site> getSites()
	{
		return siteVector;
	}
	
	public abstract Site randomClbSite(int Rlim, Site pl1);
	public abstract Site randomHardBlockSite(int Rlim, HardBlockSite pl1);
	public abstract Site randomIOSite(int Rlim, Site pl1);
	
	public void setRand(Random rand)
	{
		this.rand = rand;
	}
}
