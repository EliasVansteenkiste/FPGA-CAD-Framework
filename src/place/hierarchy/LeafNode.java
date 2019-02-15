package place.hierarchy;

import java.awt.Color;

public class LeafNode extends HierarchyNode{
	private final int index;
	private final Color color;

	private final boolean floating;

	public LeafNode(int index, String identifier, Color color){
		super(identifier);
		
		this.index = index;
		this.color = color;
		
		if(this.identifier.equals("floating")) this.floating = true;
		else this.floating = false;
	}
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
}
