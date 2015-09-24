package flexible_architecture.architecture;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import flexible_architecture.block.AbstractBlock;
import flexible_architecture.pin.AbstractPin;

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
	
	private static final double FILL_GRADE = 0.85;
	
	private String filename;
	private JSONObject blockDefinitions;
	private Map<String, Double> delays;
	
	public FlexibleArchitecture(String filename) {
		this.filename = filename;
	}
	
	@SuppressWarnings("unchecked")
	public void parse() {
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(this.filename));
		} catch (FileNotFoundException exception) {
			Logger.raise("Could not find the architecture file: " + this.filename, exception);
		}
		
		
		// Read the entire file
		String content = "", line;
		try {
			while((line = reader.readLine()) != null) {
				content += line;
			}
		} catch (IOException exception) {
			Logger.raise("Failed to read from the architecture file: " + this.filename, exception);
		}
		
		// Parse the JSONObject
		JSONObject jsonContent = (JSONObject) JSONValue.parse(content);
		this.blockDefinitions = (JSONObject) jsonContent.get("blocks");
		this.delays = (Map<String, Double>) jsonContent.get("delays");
		
		
		// Set the IO capacity
		int capacity = (int) (long) this.getDefinition("io").get("capacity");
		BlockType.setIoCapacity(capacity);
		
		// Add all the block types
		this.addBlockTypes();
	}
	
	
	private void addBlockTypes() {
		
		@SuppressWarnings("unchecked")
		Set<String> blockTypes = this.blockDefinitions.keySet();
		
		for(String typeName : blockTypes) {
			JSONObject definition = this.getDefinition(typeName);
			
			// Get some general info
			boolean isGlobal = definition.containsKey("globalCategory");
			boolean isLeaf = (boolean) definition.get("leaf");
			
			String category;
			if(isGlobal) {
				category = (String) definition.get("globalCategory");
			} else if(isLeaf) {
				category = "leaf";
			} else {
				category = "local";
			}
			
			boolean isClocked = false;
			if(isLeaf) {
				isClocked = (boolean) definition.get("clocked");
			}
			
			int height = 1, start = 0, repeat = 0;
			if(category.equals("hardblock")) {
				height = (int) (long) definition.get("height");
				start = (int) (long) definition.get("start");
				repeat = (int) (long) definition.get("repeat");
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
				modes.add(typeName);
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
			
			BlockType.addType(typeName, category, height, start, repeat, isClocked, inputs, outputs);
			
			for(int i = 0; i < modes.size(); i++) {
				BlockType.addMode(typeName, modes.get(i), children.get(i));
			}
		}
		
		BlockType.finishAdding();
	}
	
	private JSONObject getDefinition(String blockType) {
		return (JSONObject) this.blockDefinitions.get(blockType);
	}
	
	private Map<String, Integer> getChildren(JSONObject subDefinition) {
		return castIntegers((JSONObject) subDefinition.get("children"));
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
	
	
	public double getDelay(AbstractPin fromPin, AbstractPin toPin) {
		String key = String.format("%s.%s-%s.%s",
				fromPin.getOwner().getType().getName(), fromPin.getPortName(),
				toPin.getOwner().getType().getName(), toPin.getPortName());
		
		return this.getDelay(key);
	}
	public double getDelay(AbstractBlock fromBlock, AbstractPin toPin) {
		String key = String.format("%s-%s.%s",
				fromBlock.getType().getName(),
				toPin.getOwner().getType().getName(), toPin.getPortName());
		return this.getDelay(key);
	}
	public double getDelay(AbstractPin fromPin, AbstractBlock toBlock) {
		String key = String.format("%s.%s-%s",
				fromPin.getOwner().getType().getName(), fromPin.getPortName(),
				toBlock.getType().getName());
		return this.getDelay(key);
	}
	
	public double getDelay(String key) {
		if(this.delays.containsKey(key)) {
			return this.delays.get(key);
		} else {
			//Logger.log(Logger.Stream.ERR, "Delay key not found: " + key);
			return 0;
		}
	}
	
	
	public double getFillGrade() {
		return FlexibleArchitecture.FILL_GRADE;
	}
}
