package route.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import route.circuit.block.GlobalBlock;
import route.route.Connection;

public class HierarchyNode {
	protected final int level;
	protected final String identifier;
	
	private HierarchyNode parent;
	private List<HierarchyNode> children;
	
	protected List<GlobalBlock> blocks;
	
	public HierarchyNode(String identifier){
		this.level = identifier.length();
		this.identifier = identifier;
		
		this.blocks = new ArrayList<GlobalBlock>();
		
		this.parent = null;
		this.children = new ArrayList<HierarchyNode>();
	}

	public int numConnections() {
		if(this.isLeafNode()) {
			return ((LeafNode) this).connections.size();
		} else {
			int sum = 0;
			for(HierarchyNode child : this.children) {
				sum += child.numConnections();
			}
			return sum;
		}
	}
	public Collection<Connection> getConnections() {
		Collection<Connection> connections = new HashSet<>();
		this.getConnections(connections);
		return connections;
	}
	private void getConnections(Collection<Connection> connections) {
		if(this.isLeafNode()) {
			connections.addAll(((LeafNode) this).connections);
		} else {
			for(HierarchyNode child : this.children) {
				child.getConnections(connections);
			}
		}
	}
	
	public Collection<LeafNode> getLeafNodes() {
		Collection<LeafNode> result = new HashSet<>();
		
		List<HierarchyNode> work = new LinkedList<>();
		work.add(this);
		
		while(!work.isEmpty()) {
			HierarchyNode node = work.remove(0);
			if(node.isLeafNode()) {
				result.add((LeafNode)node);
			} else {
				for(HierarchyNode child : node.getChildren()) {
					work.add(child);
				}
			}
		}
		
		return result;
	}
	
	public void add(GlobalBlock block){
		this.blocks.add(block);
	}
	public List<GlobalBlock> getBlocks(){
		return this.blocks;
	}
	public String getIdentifier(){
		return this.identifier;
	}
	
	public void setParent(HierarchyNode parent){
		this.parent = parent;
	}
	public void addChild(HierarchyNode child){
		this.children.add(child);
	}
	
	public HierarchyNode getParent(){
		return this.parent;
	}
	public List<HierarchyNode> getChildren(){
		return this.children;
	}
	public boolean isLeafNode() {
		return this.getChildren().isEmpty();
	}
	
	@Override
	public String toString(){
		String result = new String();
		
		result += "Hierarchy Node:\n";
		result += "\tLevel: "+ this.level + "\n";
		result += "\tIdentifier: "+ this.identifier + "\n";
		result += "\n";
		if(this.parent != null) result += "\tParent: " + this.parent.getIdentifier() + "\n";
		if(!this.children.isEmpty()){
			result += "\tChildren: ";
			for(HierarchyNode child:this.children){
				result += child.getIdentifier() + " ";
			}
			result += "\n";
		}
		result += "\n";
		return result;
	}
}
