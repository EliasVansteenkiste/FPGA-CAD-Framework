package architecture;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class Architecture {
	public Random rand;
	public Map<String,Site> siteMap;
	public Map<String,RouteNode> routeNodeMap;
	public Site getPlace(String name) {
		return siteMap.get(name);
	}
	
	public Architecture() {
		super();

		rand= new Random();

		siteMap = new HashMap<String,Site>();
		
		routeNodeMap = new HashMap<String, RouteNode>();

		
	}
	
	public RouteNode getOrMakeRouteNode(String name) {
		RouteNode result=routeNodeMap.get(name);
		if (result==null) {
			result=new RouteNode(name);
			putRouteNode(result);
		}
		return result;
	}

	public void putRouteNode(RouteNode node) {
		routeNodeMap.put(node.name, node);
	}

	public void printRoutingGraph(PrintStream stream) {
		for(RouteNode node : routeNodeMap.values()) {
			stream.println("Node "+node.name);
			for (RouteNode child : node.children) {
				stream.print(child.name+ " ");
			}
			stream.println();
			stream.println();
		}
	}
	
	public Collection<Site> sites() {
		return siteMap.values();
	}

}
