package architecture;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.Vector;

public class Architecture {
	
	protected Random rand;
	protected Vector<Site> siteVector;
	protected Vector<RouteNode> routeNodeVector;
	
	public Architecture()
	{
		super();
		rand= new Random();
		siteVector = new Vector<>();
		routeNodeVector = new Vector<>();
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
	
	public Collection<Site> getSites()
	{
		return siteVector;
	}
	
	public Collection<RouteNode> getRouteNodes()
	{
		return routeNodeVector;
	}
	
	public void setRand(Random rand)
	{
		this.rand = rand;
	}

}
