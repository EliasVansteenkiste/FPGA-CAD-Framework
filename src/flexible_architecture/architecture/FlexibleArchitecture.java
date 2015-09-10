package flexible_architecture.architecture;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FlexibleArchitecture {
	
	private int x, y;
	
	private int ioCapacity = 8;
	private JSONObject blockDefinitions;
	
	//public enum PortType {INPUT, OUTPUT};
	
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
	
	public FlexibleArchitecture(String filename) {
		this.parse(filename);
	}
	
	public FlexibleArchitecture(String filename, int x, int y) {
		this(filename);
		this.x = x;
		this.y = y;
	}
	
	private void parse(String filename) {
		File file = new File(filename);
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException exception) {
			Logger.raise("Architecture file not found: " + filename, exception);
		}
		
		String content = scanner.useDelimiter("\\Z").next();
		
		this.blockDefinitions = (JSONObject) JSONValue.parse(content);
	}
	
	
	
	private JSONObject getDefinition(String blockType) {
		return (JSONObject) this.blockDefinitions.get(blockType);
	}
	
	/*public Map<String, Integer> getChildren(String blockType) {
		return this.getChildren(blockType, null);
	}
	public Map<String, Integer> getChildren(String blockType, String mode) {
		JSONObject definition = this.getDefinition(blockType);
		
		if(mode != null) {
			definition = (JSONObject) ((JSONObject) definition.get("modes")).get(mode);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Integer> children = (Map<String, Integer>) definition.get("children");
		
		return children;
	}
	
	
	public Map<String, Integer> getInputs(String blockType) {
		return this.getPorts(blockType, "input");
	}
	public Map<String, Integer> getOutputs(String blockType) {
		return this.getPorts(blockType, "output");
	}
	public Map<String, Integer> getPorts(String blockType, PortType portType) {
		String portTypeString = null;
		
		switch(portType) {
		case INPUT:
			portTypeString = "input";
			break;
		case OUTPUT:
			portTypeString = "output";
			break;
		}
		
		return this.getPorts(blockType, portTypeString);
	}
	private Map<String, Integer> getPorts(String blockType, String portType) {
		Map<String, Map<String, Integer>> ports = this.getPorts(blockType);
		return ports.get(portType);
	}
	public Map<String, Map<String, Integer>> getPorts(String blockType) {
		JSONObject definition = this.getDefinition(blockType);
		
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Integer>> ports = (Map<String, Map<String, Integer>>) definition.get("ports");
		
		return ports;
	}*/
}
