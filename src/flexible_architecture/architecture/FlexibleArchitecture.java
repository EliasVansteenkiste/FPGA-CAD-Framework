package flexible_architecture.architecture;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import util.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlexibleArchitecture {
	
	private int x, y;
	
	private String filename;
	private BufferedReader reader;
	
	private JSONObject blockDefinitions;
	
	public FlexibleArchitecture(String filename) {
		this.filename = filename;
		
		try {
			this.reader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException exception) {
			Logger.raise("Could not find the architecture file: " + filename, exception);
		}
	}
	
	public FlexibleArchitecture(String filename, int x, int y) {
		this(filename);
		this.x = x;
		this.y = y;
	}
	
	public void parse() {
		
		// Read the entire file
		String content = "", line;
		try {
			while((line = this.reader.readLine()) != null) {
				content += line;
			}
		} catch (IOException exception) {
			Logger.raise("Failed to read from the architecture file: " + this.filename, exception);
		}
		
		// Parse the JSONObject
		this.blockDefinitions = (JSONObject) JSONValue.parse(content);
		
		
		// Set the IO capacity
		int capacity = (int) (long) this.getDefinition("io").get("capacity");
		BlockType.setIoCapacity(capacity);
		
		// Add all the block types
		this.addBlockTypes();
	}
	
	private void addBlockTypes() {
		
		@SuppressWarnings("unchecked")
		Set<String> blockTypes = this.blockDefinitions.keySet();
		
		for(String blockType : blockTypes) {
			JSONObject definition = this.getDefinition(blockType);
			
			// Get some general info
			boolean isGlobal = (boolean) definition.get("global");
			boolean isLeaf = (boolean) definition.get("leaf");
			
			int height;
			if(isGlobal) {
				height = (int) (long) definition.get("height");
			} else {
				height = 0;
			}
			
			
			// Get the port counts
			@SuppressWarnings("unchecked")
			Map<String, JSONObject> ports = (Map<String, JSONObject>) definition.get("ports");
			
			Map<String, Integer> inputs = castIntegers(ports.get("input"));
			Map<String, Integer> outputs = castIntegers(ports.get("output"));
			
			
			// Get the modes and children
			List<String> modes = new ArrayList<String>();
			List<Map<String, Integer>> children = new ArrayList<Map<String, Integer>>();
			
			// If the block is a leaf: there are no modes, the only mode is unnamed
			if(isLeaf) {
				modes.add("");
				children.add(this.getChildren(definition));
			
			
			// There is only one mode, but we have to name it like the block for some reason
			} else if(!definition.containsKey("modes")) {
				modes.add(blockType);
				children.add(this.getChildren(definition));
			
				
			// There are multiple modes
			} else {
				JSONObject modeDefinitions = (JSONObject) definition.get("modes");
				
				@SuppressWarnings("unchecked")
				Set<String> modeNames = modeDefinitions.keySet();
				
				for(String mode : modeNames) {
					modes.add(mode);
					children.add(this.getChildren((JSONObject) modeDefinitions.get(mode)));
				}
			}
			
			
			for(int i = 0; i < modes.size(); i++) {
				BlockType.addType(blockType, modes.get(i), isGlobal, isLeaf, height, inputs, outputs, children.get(i));
			}
		}
	}
	
	
	private Map<String, Integer> getChildren(JSONObject subDefinition) {
		Map<String, Integer> children = castIntegers((JSONObject) subDefinition.get("children"));;
		
		return children;
	}
	
	
	private Map<String, Integer> castIntegers(JSONObject subDefinition) {
		@SuppressWarnings("unchecked")
		Set<String> keys = (Set<String>) subDefinition.keySet();
		
		Map<String, Integer> newSubDefinition = new HashMap<String, Integer>();
		
		for(String key : keys) {
			int value = (int) (long) subDefinition.get(key);
			newSubDefinition.put(key, value);
		}
		
		return newSubDefinition;
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
