package flexible_architecture.architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Logger;

public class BlockType {
	
	public enum BlockCategory {IO, CLB, HARDBLOCK, LOCAL, LEAF};
	
	private static Map<String, Integer> types = new HashMap<String, Integer>();
	private static List<String> typeNames = new ArrayList<String>();
	
	private static List<BlockCategory> category = new ArrayList<BlockCategory>();
	private static List<List<BlockType>> blockTypesPerCategory = new ArrayList<List<BlockType>>();
	static {
		for(int i = 0; i < BlockCategory.values().length; i++) {
			blockTypesPerCategory.add(new ArrayList<BlockType>());
		}
	}
	
	private static List<Integer> height = new ArrayList<Integer>();
	private static List<Integer> start = new ArrayList<Integer>();
	private static List<Integer> repeat = new ArrayList<Integer>();
	
	private static List<Map<String, Integer>> inputs = new ArrayList<Map<String, Integer>>();
	private static List<Map<String, Integer>> outputs = new ArrayList<Map<String, Integer>>();
	
	private static int ioCapacity;
	
	
	
	private static List<Map<String, Integer>> modes = new ArrayList<Map<String, Integer>>();
	private static List<List<String>> modeNames = new ArrayList<List<String>>();
	private static List<List<Map<String, Integer>>> children = new ArrayList<List<Map<String, Integer>>>();
	
	
	private Integer typeIndex, modeIndex;
	
	
	
	public static void setIoCapacity(int capacity) {
		BlockType.ioCapacity = capacity;
	}
	public static int getIoCapacity() {
		return BlockType.ioCapacity;
	}
	
	
	public static void addType(String typeName, String categoryName, int height, int start, int repeat, Map<String, Integer> inputs, Map<String, Integer> outputs) {
		
		int typeIndex = BlockType.typeNames.size();
		BlockType.typeNames.add(typeName);
		BlockType.types.put(typeName, typeIndex);
		
		BlockCategory category = BlockType.getCategoryFromString(categoryName);
		BlockType.category.add(category);
		BlockType.blockTypesPerCategory.get(category.ordinal()).add(new BlockType(typeName));
		
		BlockType.height.add(height);
		BlockType.start.add(start);
		BlockType.repeat.add(repeat);
		
		BlockType.inputs.add(inputs);
		BlockType.outputs.add(outputs);
		
		
		BlockType.modeNames.add(new ArrayList<String>());
		BlockType.modes.add(new HashMap<String, Integer>());
		BlockType.children.add(new ArrayList<Map<String, Integer>>());
	}
	
	private static BlockCategory getCategoryFromString(String categoryName) {
		switch(categoryName) {
		case "io":
			return BlockCategory.IO;
			
		case "clb":
			return BlockCategory.CLB;
			
		case "hardblock":
			return BlockCategory.HARDBLOCK;
			
		case "local":
			return BlockCategory.LOCAL;
			
		case "leaf":
			return BlockCategory.LEAF;
			
		default:
			return null;
		}
	}
	
	public static void addMode(String typeName, String modeName, Map<String, Integer> children) {
		int typeIndex = BlockType.types.get(typeName);
		
		int modeIndex = BlockType.modeNames.get(typeIndex).size();
		BlockType.modeNames.get(typeIndex).add(modeName);
		BlockType.modes.get(typeIndex).put(modeName, modeIndex);
		
		BlockType.children.get(typeIndex).add(children);
	}
	
	
	
	public BlockType(String typeName) {
		this.typeIndex = this.getTypeIndex(typeName);
		this.modeIndex = null;
	}
	public BlockType(String typeName, String modeName) {
		this.typeIndex = this.getTypeIndex(typeName);
		this.modeIndex = this.getModeIndex(modeName);
	}
	
	
	private int getTypeIndex(String typeName) {
		if(!BlockType.types.containsKey(typeName)) {
			Logger.raise("Invalid block type: " + typeName);
		}
		return BlockType.types.get(typeName);
	}
	
	private int getModeIndex(String argumentModeName) {
		String modeName = (argumentModeName == null) ? "" : argumentModeName;
		if(!BlockType.modes.get(this.typeIndex).containsKey(modeName)) {
			Logger.raise("Invalid mode type for block " + this.getName() + ": " + modeName);
		}
		return BlockType.modes.get(this.typeIndex).get(modeName);
	}
	
	
	public static List<BlockType> getGlobalBlockTypes() {
		List<BlockType> types = new ArrayList<BlockType>();
		types.addAll(BlockType.getBlockTypes(BlockCategory.IO));
		types.addAll(BlockType.getBlockTypes(BlockCategory.CLB));
		types.addAll(BlockType.getBlockTypes(BlockCategory.HARDBLOCK));
		
		return types;
	}
	
	public static List<BlockType> getBlockTypes(BlockCategory category) {
		return BlockType.blockTypesPerCategory.get(category.ordinal());
	}
	
	
	
	public String getName() {
		return BlockType.typeNames.get(this.typeIndex);
	}
	public BlockCategory getCategory() {
		return BlockType.category.get(this.typeIndex);
	}
	public boolean isGlobal() {
		BlockCategory category = this.getCategory();
		return category != BlockCategory.LOCAL && category != BlockCategory.LEAF;
	}
	public boolean isLeaf() {
		BlockCategory category = this.getCategory();
		return category == BlockCategory.LEAF;
	}
	
	public int getHeight() {
		return BlockType.height.get(this.typeIndex);
	}
	public int getStart() {
		return BlockType.start.get(this.typeIndex);
	}
	public int getRepeat() {
		return BlockType.repeat.get(this.typeIndex);
	}
	
	public String getMode() {
		return BlockType.modeNames.get(this.typeIndex).get(this.modeIndex);
	}
	public Map<String, Integer> getChildren() {
		if(this.modeIndex == null) {
			Logger.raise("Cannot get children of a block type without mode");
		}
		return BlockType.children.get(this.typeIndex).get(this.modeIndex);
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
		return BlockType.inputs.get(this.typeIndex);
	}
	public Map<String, Integer> getOutputs() {
		return BlockType.outputs.get(this.typeIndex);
	}
	
	
	@Override
	public boolean equals(Object otherObject) {
		if(otherObject instanceof BlockType) {
			return this.equals((BlockType) otherObject);
		} else {
			return false;
		}
	}
	
	public boolean equals(BlockType otherBlockType) {
		return this.typeIndex == otherBlockType.typeIndex;
	}
	
	@Override
	public int hashCode() {
		return this.typeIndex;
	}
	
	@Override
	public String toString() {
		if(this.modeIndex == null) {
			return this.getName();
		} else {
			return this.getName() + "<" + this.getMode() + ">";
		}
	}
}
