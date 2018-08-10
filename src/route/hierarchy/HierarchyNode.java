package route.hierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	public void getConnections(Set<Connection> connections) {
		if(this.isLeafNode()) {
			connections.addAll(((LeafNode) this).getConnections());
		}
		for(HierarchyNode child : this.children) {
			child.getConnections(connections);
		}
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
