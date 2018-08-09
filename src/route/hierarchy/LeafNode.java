package route.hierarchy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import route.circuit.resource.Sink;
import route.circuit.resource.Source;
import route.route.Connection;

public class LeafNode extends HierarchyNode{
	private final int index;
	private final Color color;

	private final boolean floating;
	
	private Set<Connection> connections;
	
	private short x_min;
	private short x_max;
	private short y_min;
	private short y_max;
	
	private short boundingBoxRange;
	
	public LeafNode(int index, String identifier, Color color){
		super(identifier);
		
		this.index = index;
		this.color = color;
		
		if(this.identifier.equals("floating")) this.floating = true;
		else this.floating = false;
	}
	
	public void initializeConnections(Set<Connection> connections, short boundingBoxRange) {
		this.connections = connections;
		this.boundingBoxRange = boundingBoxRange;
		
		List<Short> xCoordinatesBB = new ArrayList<>();
		List<Short> yCoordinatesBB = new ArrayList<>();
		
		for(Connection connection : this.connections) {
			Source source = (Source) connection.sourceRouteNode;
			Sink sink = (Sink) connection.sinkRouteNode;
			
			if(!connection.isGlobal) {
				xCoordinatesBB.add(source.xlow);
				xCoordinatesBB.add(source.xhigh);
				
				xCoordinatesBB.add(sink.xlow);
				xCoordinatesBB.add(sink.xhigh);
				
				yCoordinatesBB.add(source.ylow);
				yCoordinatesBB.add(source.yhigh);
				
				yCoordinatesBB.add(sink.ylow);
				yCoordinatesBB.add(sink.yhigh);
			}
		}
		
		short x_min_temp = Short.MAX_VALUE;
		short y_min_temp = Short.MAX_VALUE;
		
		short x_max_temp = Short.MIN_VALUE;
		short y_max_temp = Short.MIN_VALUE;
		
		for(short x : xCoordinatesBB) {
			if(x < x_min_temp) {
				x_min_temp = x;
			}
			if(x > x_max_temp) {
				x_max_temp = x;
			}
		}
		for(short y : yCoordinatesBB) {
			if(y < y_min_temp) {
				y_min_temp = y;
			}
			if(y > y_max_temp) {
				y_max_temp = y;
			}
		}
		
		this.x_min = x_min_temp;
		this.y_min = y_min_temp;
		
		this.x_max = x_max_temp;
		this.y_max = y_max_temp;
	}
	public Set<Connection> getConnections() {
		return this.connections;
	}
	public int numConnections() {
		return this.connections.size();
	}
//	public boolean isInBoundingBoxLimit(RouteNode node) {
//		if(
//			node.xlow < (this.x_max + this.boundingBoxRange) 
//			&& node.xhigh > (this.x_min - this.boundingBoxRange) 
//			&& node.ylow < (this.y_max + this.boundingBoxRange) 
//			&& node.yhigh > (this.y_min - this.boundingBoxRange)
//		) {
//			return true;
//		}
//		
//		return false;
//	}
	
	public int getIndex(){
		return this.index;
	}
	public Color getColor(){
		return this.color;
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
	
	@Override
	public int hashCode() {
		return this.index;
	}
	public int cost() {
		int cost = 0;
		for(Connection con : this.connections) {
			cost += con.boundingBox;
		}
		return cost;
	}
	
	@Override
	public String toString() {
		return "(" + this.x_min + "," + this.y_min + ") => (" + this.x_max + "," + this.y_max + ")" + " Num connections: " + this.connections.size();
	}
}
