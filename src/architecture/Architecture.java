package architecture;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.Vector;

import architecture.old.RouteNode;

public abstract class Architecture {
	
	protected Random rand;
	protected Vector<Site> siteVector;
	protected Vector<RouteNode> routeNodeVector;
	
	protected Site[][][] siteArray;
	
	protected int width, height;
	
	public Architecture()
	{
		super();
		rand= new Random();
		siteVector = new Vector<>();
		routeNodeVector = new Vector<>();
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
	public Site getSite(int x, int y, int n)
	{
		return siteArray[x][y][n];
	}
	
	protected void addSite(Site site, int x, int y, int n)
	{
		siteArray[x][y][n] = site;
		siteVector.add(site);
	}
	
	public abstract Site randomClbSite(int Rlim, Site pl1);
	
	
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
