package flexible_architecture.architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Logger;

public class BlockType {
	
	public static enum BlockCategory {IO, CLB, HARDBLOCK, LOCAL, LEAF};
	
	
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
	
	private static List<Boolean> clocked = new ArrayList<Boolean>();
	//private static List<Boolean> hasClockedChild = new ArrayList<Boolean>();
	
	
	private static List<Map<String, Integer>> modes = new ArrayList<Map<String, Integer>>();
	private static List<List<String>> modeNames = new ArrayList<List<String>>();
	private static List<List<Map<String, Integer>>> children = new ArrayList<List<Map<String, Integer>>>();
	
	private static List<List<List<Integer>>> childStarts = new ArrayList<List<List<Integer>>>();
	private static List<List<List<Integer>>> childEnds = new ArrayList<List<List<Integer>>>();
	private static List<List<Integer>> numChildren = new ArrayList<List<Integer>>();
	
	private static List<List<PortType>> portTypes = new ArrayList<List<PortType>>();
	
	
	
	private Integer typeIndex, modeIndex;
	
	
	static void addType(String typeName, String categoryName, int height, int start, int repeat, boolean clocked, Map<String, Integer> inputs, Map<String, Integer> outputs) {
		
		int typeIndex = BlockType.typeNames.size();
		BlockType.typeNames.add(typeName);
		BlockType.types.put(typeName, typeIndex);
		
		BlockCategory category = BlockType.getCategoryFromString(categoryName);
		BlockType.category.add(category);
		BlockType.blockTypesPerCategory.get(category.ordinal()).add(new BlockType(typeName));
		
		BlockType.height.add(height);
		BlockType.start.add(start);
		BlockType.repeat.add(repeat);
		
		BlockType.clocked.add(clocked);
		//BlockType.hasClockedChild.add(null);
		
		BlockType.modeNames.add(new ArrayList<String>());
		BlockType.modes.add(new HashMap<String, Integer>());
		BlockType.children.add(new ArrayList<Map<String, Integer>>());
		
		PortType.setNumInputPorts(typeIndex, inputs.size());
		PortType.addPorts(typeIndex, inputs);
		PortType.addPorts(typeIndex, outputs);
		
		// getPortTypes is a heavy operation, and it has to be performed loads of times during
		// netparsing, so we cache it
		BlockType.portTypes.add(PortType.getPortTypes(typeIndex));
	}
	
	
	public static void postProcess() {
		//BlockType.findBlocksWithClockedChild();
		BlockType.cacheChildren();
		PortType.postProcess();
	}
	
	
	/*private static void findBlocksWithClockedChild() {
		for(int typeIndex = 0; typeIndex < BlockType.types.size(); typeIndex++) {
			BlockType.setAndGetHasClockedChild(typeIndex);
		}
	}
	
	private static boolean setAndGetHasClockedChild(int typeIndex) {
		boolean hasClockedChild = false;
		if(BlockType.hasClockedChild.get(typeIndex) != null) {
			hasClockedChild = BlockType.hasClockedChild.get(typeIndex);
		
		} else if(BlockType.clocked.get(typeIndex) == true) {
			hasClockedChild = true;
		
		} else {
			for(int modeIndex = 0; modeIndex < BlockType.modes.get(typeIndex).size(); modeIndex++) {
				for(String childTypeName : BlockType.children.get(typeIndex).get(modeIndex).keySet()) {
					int childTypeIndex = BlockType.types.get(childTypeName);
					boolean childIsClocked = BlockType.setAndGetHasClockedChild(childTypeIndex);
					if(childIsClocked) {
						hasClockedChild = true;
						break;
					}
				}
				
				if(hasClockedChild) {
					break;
				}
			}
		}
		
		BlockType.hasClockedChild.set(typeIndex, hasClockedChild);
		return hasClockedChild;
	}*/
	
	private static void cacheChildren() {
		
		int numTypes = BlockType.types.size();
		for(int typeIndex = 0; typeIndex < numTypes; typeIndex++) {
			
			BlockType.childStarts.add(new ArrayList<List<Integer>>());
			BlockType.childEnds.add(new ArrayList<List<Integer>>());
			BlockType.numChildren.add(new ArrayList<Integer>());
			
			int numModes = BlockType.modeNames.get(typeIndex).size();
			for(int modeIndex = 0; modeIndex < numModes; modeIndex++) {
				
				BlockType.childStarts.get(typeIndex).add(new ArrayList<Integer>(Collections.nCopies(numTypes, (Integer) null)));
				BlockType.childEnds.get(typeIndex).add(new ArrayList<Integer>(Collections.nCopies(numTypes, (Integer) null)));
				int numChildren = 0;
				
				for(Map.Entry<String, Integer> childEntry : BlockType.children.get(typeIndex).get(modeIndex).entrySet()) {
					String childName = childEntry.getKey();
					int childTypeIndex = BlockType.types.get(childName);
					int childCount = childEntry.getValue();
					
					BlockType.childStarts.get(typeIndex).get(modeIndex).set(childTypeIndex, numChildren);
					numChildren += childCount;
					BlockType.childEnds.get(typeIndex).get(modeIndex).set(childTypeIndex, numChildren);
				}
				
				BlockType.numChildren.get(typeIndex).add(numChildren);
			}
		}
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
	
	
	
	
	public BlockType(String typeName) {
		this.typeIndex = BlockType.getTypeIndex(typeName);
		this.modeIndex = null;
	}
	public BlockType(String typeName, String modeName) {
		this.typeIndex = BlockType.getTypeIndex(typeName);
		this.modeIndex = this.getModeIndex(modeName);
	}
	
	private static int getTypeIndex(String typeName) {
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
	
	
	int getIndex() {
		return this.typeIndex;
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
	
	public boolean isClocked() {
		return BlockType.clocked.get(this.typeIndex);
	}
	/*public boolean hasClockedChild() {
		return BlockType.hasClockedChild.get(this.typeIndex);
	}*/
	
	
	public int getNumChildren() {
		return BlockType.numChildren.get(this.typeIndex).get(this.modeIndex);
	}
	public int[] getChildRange(BlockType blockType) {
		int childStart = BlockType.childStarts.get(this.typeIndex).get(this.modeIndex).get(blockType.getIndex());
		int childEnd = BlockType.childEnds.get(this.typeIndex).get(this.modeIndex).get(blockType.getIndex());
		
		int[] childRange = {childStart, childEnd};
		return childRange;
	}
	
	
	public int getNumPins() {
		return PortType.getNumPins(this.typeIndex);
	}
	public int[] getInputPortRange() {
		return PortType.getInputPortRange(this.typeIndex);
	}
	public int[] getOutputPortRange() {
		return PortType.getOutputPortRange(this.typeIndex);
	}
	
	public List<PortType> getPortTypes() {
		return BlockType.portTypes.get(this.typeIndex);
	}
	
	
	/*public Map<String, Integer> getPorts(PortType type) {
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
	}*/
	
	
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
