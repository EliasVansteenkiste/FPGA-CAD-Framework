package architecture;

import java.util.Collection;
import java.util.Random;
import java.util.Vector;

public abstract class Architecture {
	
	protected Random rand;
	protected Vector<Site> siteVector;
	protected Site[][] siteArray;
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
	
	public Collection<Site> getSites()
	{
		return siteVector;
	}
	
	public Site getSite(int x, int y)
	{
		return siteArray[x][y];
	}
	
	protected void addSite(Site site, int x, int y)
	{
		siteArray[x][y] = site;
		siteVector.add(site);
	}
	
	public abstract Site randomClbSite(int Rlim, Site pl1);
	public abstract Site randomHardBlockSite(int Rlim, HardBlockSite pl1);
	public abstract Site randomIOSite(int Rlim, Site pl1);
	
	public void setRand(Random rand)
	{
		this.rand = rand;
	}
}
