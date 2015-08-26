package architecture;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import circuit.Block;


public abstract class Architecture {
	
	protected Random rand;
	
	protected Site[][][] siteArray;
	
	protected HashMap<SiteType, Vector<Site>> siteVectors;
	protected Vector<RouteNode> routeNodeVector;
	
	protected int width, height, n;
	
	public Architecture()
	{
		super();
		rand= new Random();
		routeNodeVector = new Vector<>();
		
		siteVectors = new HashMap<SiteType, Vector<Site>>();
		for(SiteType siteType : SiteType.values()) {
			siteVectors.put(siteType, new Vector<Site>());
		}
	}
	
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public int getN() {
		return n;
	}
	
	
	
	public Vector<Site> getSites(SiteType type)
	{
		return siteVectors.get(type);
	}
	public Vector<Site> getSites(SiteType... types)
	{
		Vector<Site> sites = new Vector<Site>();
		for(SiteType type : types) {
			sites.addAll(getSites(type));
		}
		return sites;
	}
	
	public Site getSite(int x, int y, int n)
	{
		return siteArray[x][y][n];
	}
	
	protected void addSite(Site site, int x, int y, int n)
	{
		siteArray[x][y][n] = site;
		siteVectors.get(site.type).add(site);
	}
	
	
	public Block placeBlock(int x, int y, int n, Block block) {
		Site site = this.getSite(x, y, n);
		return this.placeBlock(site, block);
	}
	public Block placeBlock(Site site, Block block) {
		block.setSite(site);
		return site.setBlock(block);
	}
	
	
	public abstract Site randomClbSite(int Rlim, Site pl1);
	public abstract Site randomHardBlockSite(int Rlim, HardBlockSite pl1);
	public abstract Site randomISite(int Rlim, Site pl1);
	public abstract Site randomOSite(int Rlim, Site pl1);
	
	
	public Collection<RouteNode> getRouteNodes()
	{
		return routeNodeVector;
	}
	
	public void setRand(Random rand)
	{
		this.rand = rand;
	}
	
	
	
	public void printRoutingGraph(PrintStream stream)
	{
		for(RouteNode node : routeNodeVector)
		{
			stream.println("Node "+node.name);
			for(RouteNode child : node.children)
			{
				stream.print(child.name+ " ");
			}
			stream.println();
			stream.println();
		}
	}
}
