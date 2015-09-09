package flexible_architecture.architecture;

import java.util.Arrays;
import java.util.HashMap;

import util.Logger;

public class BlockType {
	//IO("io", true, 1, 1),
	//CLB("clb", true, );
	
	//, CLB, MULT_36, MEMORY, FLE, LUT5_INTER, BLE6, BLE5, LUT6, LUT5, FF;
	
	private static String[] type = {"io", "clb", "mult_36", "memory", "fle", "lut5_inter", "ble6", "ble5", "lut6", "lut5", "ff"};
	private static boolean[] isGlobal = {true, true, true, true, false, false, false, false, false, false};
	private static int[] maxChildren = {1, 10, 0, 0, 2, 2, 2, 2, 0, 0, 0};
	private static int[] height = {1, 1, 4, 6};
	
	private static HashMap<String, Integer> typeIndex;
	static {
		for(int i = 0; i < type.length; i++) {
			typeIndex.put(type[i], i);
		}
	}
	
	
	private int index;
	
	public BlockType(String type) {
		if(!typeIndex.containsKey(type)) {
			Logger.raise("Invalid block type: " + type);
		}
		
		int index = typeIndex.get(type);
	}
	
	
	public String getType() {
		return type[this.index];
	}
	public boolean isGlobal() {
		return isGlobal[this.index];
	}
	public int getMaxChildren() {
		return maxChildren[this.index];
	}
	public int getHeight() {
		return height[this.index];
	}
	
	public String toString() {
		return this.getType();
	}
}
