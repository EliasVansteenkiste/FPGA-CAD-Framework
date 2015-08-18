package circuit;

import java.util.HashSet;
import java.util.Set;

import architecture.old.RouteNode;

public class NetInst {
	public Net net;
	public Set<RouteNode> routeNodes;
		
	public NetInst(Net net) {
		super();
		this.net = net;
		routeNodes = new HashSet<RouteNode>();
	}

	@Override
	public String toString() {
		return net.name;
	}
	
}
