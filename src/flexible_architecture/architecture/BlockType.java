package flexible_architecture.architecture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Logger;

public class BlockType {
	
	private static List<String> id = new ArrayList<String>();
	private static List<String> name = new ArrayList<String>();
	private static List<String> mode = new ArrayList<String>();
	
	private static List<Boolean> isGlobal = new ArrayList<Boolean>();
	private static List<Boolean> isLeaf = new ArrayList<Boolean>();
	private static List<Integer> height = new ArrayList<Integer>();
	
	private static List<Map<String, Integer>> children = new ArrayList<Map<String, Integer>>();
	private static List<Map<String, Integer>> inputs = new ArrayList<Map<String, Integer>>();
	private static List<Map<String, Integer>> outputs = new ArrayList<Map<String, Integer>>();
	
	
	private static HashMap<String, Integer> typeIndex;
	
	
	
	private int index;
	
	
	
	private static String getId(String name, String mode) {
		return name + "<" + mode + ">";
	}
	
	public static void addType(String name, boolean isGlobal, boolean isLeaf, int height, HashMap<String, Integer> inputs, HashMap<String, Integer> outputs, HashMap<String, Integer> children) {
		addType(name, "", isGlobal, isLeaf, height, inputs, outputs, children);
	}
	public static void addType(String name, String mode, boolean isGlobal, boolean isLeaf, int height, HashMap<String, Integer> inputs, HashMap<String, Integer> outputs, HashMap<String, Integer> children) {
		String id = BlockType.getId(name, mode);
		
		BlockType.id.add(id);
		BlockType.name.add(name);
		BlockType.mode.add(mode);
		
		BlockType.isGlobal.add(isGlobal);
		BlockType.isLeaf.add(isLeaf);
		BlockType.height.add(height);
		
		BlockType.children.add(children);
		BlockType.inputs.add(inputs);
		BlockType.outputs.add(outputs);
		
		BlockType.typeIndex.put(id, BlockType.id.size());
	}
	
	
	
	
	public BlockType(String type, String mode) {
		String id = BlockType.getId(type, mode);
		if(!typeIndex.containsKey(type)) {
			Logger.raise("Invalid block type: " + id);
		}
		
		this.index = typeIndex.get(type);
	}
	
	
	public String getName() {
		return id.get(this.index);
	}
	public boolean isGlobal() {
		return isGlobal.get(this.index);
	}
	public boolean isLeaf() {
		return isLeaf.get(this.index);
	}
	public int getHeight() {
		return height.get(this.index);
	}
	
	public Map<String, Integer> getChildren() {
		return children.get(this.index);
	}
	
	
	public Map<String, Integer> getPorts(PortType type) {
		switch(type) {
		case INPUT:
			return this.getInputs();
			
		case OUTPUT:
			return this.getOutputs();
		
		default:
			Logger.raise("Unknown port type: " + type);
			return null;
		}
	}
	public Map<String, Integer> getInputs() {
		return inputs.get(this.index);
	}
	public Map<String, Integer> getOutputs() {
		return outputs.get(this.index);
	}
	
	
	
	public boolean equals(BlockType otherBlockType) {
		return this.index == otherBlockType.index;
	}
	
	public String toString() {
		return this.getName();
	}
}
