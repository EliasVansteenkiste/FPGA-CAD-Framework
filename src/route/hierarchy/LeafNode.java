package route.hierarchy;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import route.route.Connection;

public class LeafNode extends HierarchyNode{
	private final int index;
	private final Color color;
	
	protected final Set<Connection> connections;
	
	private final boolean floating;
	
	public LeafNode(int index, String identifier, Color color){
		super(identifier);
		
		this.index = index;
		this.color = color;
		
		this.connections = new HashSet<>();
		
		if(this.identifier.equals("floating")) this.floating = true;
		else this.floating = false;
	}
	public void addConnection(Connection connection) {
		this.connections.add(connection);
	}
	
	public int getIndex(){
		return this.index;
	}
	public Color getColor(){
		return this.color;
	}
	public Set<Connection> getConnections() {
		return this.connections;
	}
	
	//Distance metric
	public int totalDistance(LeafNode otherNode){
		if(this.identifier.equals(otherNode.identifier)) return 0;
		
		int index = 0;
		while(this.identifier.charAt(index) == otherNode.identifier.charAt(index)){
			index++;
		}

		int totalDistance = this.identifier.length() - index + otherNode.identifier.length() - index;

		return totalDistance;
	}
	public int distanceToCommonRoot(LeafNode otherNode){
		if(this.identifier.equals(otherNode.identifier)) return 0;
		
		int index = 0;
		while(this.identifier.charAt(index) == otherNode.identifier.charAt(index)){
			index++;
		}
		return this.identifier.length() - index;
	}
	public int cutLevel(LeafNode otherNode){
		if(this.isFloating()) return 1;
		if(otherNode.isFloating()) return 1;
		if(this.identifier.equals(otherNode.identifier)) return 100;//TODO

		int index = 0;
		while(this.identifier.charAt(index) == otherNode.identifier.charAt(index)){
			index++;
		}

		int cutLevel = index + 1;

		return cutLevel;
	}
	public int cutSeparation(LeafNode otherNode){
		return Math.max(this.distanceToCommonRoot(otherNode), otherNode.distanceToCommonRoot(this));
	}
	public boolean isFloating() {
		return floating;
	}
	
	public int cost() {
		int cost = 0;
		for(Connection con : this.connections) {
			cost += con.boundingBox;
		}
		return cost;
	}
	
	@Override
	public int hashCode() {
		return this.index;
	}
	
	@Override
	public String toString() {
		return "ID: " + this.index + " Num connections: " + this.connections.size();
	}
}
