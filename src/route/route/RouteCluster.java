package route.route;

import java.util.ArrayList;
import java.util.List;

import route.hierarchy.HierarchyNode;
import route.hierarchy.LeafNode;

public class RouteCluster {
	private final HierarchyNode hierarchyNode;
	private final List<LeafNode> leafNodes;
	
	private final List<Connection> connections;
	private final List<Connection> localConnections;
	private final List<Connection> globalConnections;
	
	private final int cost;
	
	public RouteCluster(HierarchyNode hierarchyNode) {
		this.hierarchyNode = hierarchyNode;
		
		this.leafNodes = new ArrayList<>(this.hierarchyNode.getLeafNodes());
		this.connections = new ArrayList<>(this.hierarchyNode.getConnections());
		
		this.localConnections = new ArrayList<>();
		this.globalConnections = new ArrayList<>();
		
		for(Connection conn : this.connections) {
			LeafNode sourceLeafNode = conn.source.getOwner().getLeafNode();
			LeafNode sinkLeafNode = conn.sink.getOwner().getLeafNode();
			
			if(this.leafNodes.contains(sourceLeafNode) && this.leafNodes.contains(sinkLeafNode)) {
				this.localConnections.add(conn);
			} else {
				this.globalConnections.add(conn);
			}
		}
		
		int tempCost = 0;
		for(LeafNode leafNode : this.leafNodes) {
			tempCost += leafNode.cost();
		}
		this.cost = tempCost;
	}
	
	public List<Connection> getLocalConnections() {
		return this.localConnections;
	}
	public List<Connection> getGlobalConnections() {
		return this.globalConnections;
	}
	public int getCost() {
		return this.cost;
	}
	
	@Override
	public String toString() {
		return this.hierarchyNode.getIdentifier();
	}
}
