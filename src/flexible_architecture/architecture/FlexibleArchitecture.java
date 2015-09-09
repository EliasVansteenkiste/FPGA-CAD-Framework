package flexible_architecture.architecture;

import java.util.HashMap;
import java.util.Map;

public class FlexibleArchitecture {
	
	private int x, y;
	
	private int ioCapacity = 8;
	
	/*private static Map<BlockType, Map<String, Integer>> inputs = new HashMap<BlockType, Map<String, Integer>>();
	private static Map<BlockType, Map<String, Integer>> outputs = new HashMap<BlockType, Map<String, Integer>>();
	static {
		for(BlockType type : BlockType.values()) {
			inputs.put(type, new HashMap<String, Integer>());
			outputs.put(type, new HashMap<String, Integer>());
		}
		
		inputs.get(BlockType.IO).put("outpad", 1);
		outputs.get(BlockType.IO).put("inpad", 1);
		
		inputs.get(BlockType.CLB).put("I", 40);
		outputs.get(BlockType.CLB).put("O", 40);
	}*/
	
	public FlexibleArchitecture() {
		
	}
	
	public FlexibleArchitecture(int x, int y) {
		this.x = x;
		this.y = y;
	}
}
