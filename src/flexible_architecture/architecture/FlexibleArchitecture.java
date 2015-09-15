package flexible_architecture.architecture;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import flexible_architecture.architecture.BlockType.BlockCategory;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.site.AbstractSite;
import flexible_architecture.site.IOSite;
import flexible_architecture.site.Site;

import util.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FlexibleArchitecture {
	
	private static final double FILL_GRADE = 0.85;
	
	private int width, height;
	private AbstractSite[][] sites;
	
	private Map<BlockType, List<AbstractBlock>> blocks = new HashMap<BlockType, List<AbstractBlock>>(4);
	private Map<BlockCategory, List<BlockType>> blockTypesPerCategory = new HashMap<BlockCategory, List<BlockType>>();
	private Map<BlockType, List<Integer>> columnsPerBlockType = new HashMap<BlockType, List<Integer>>();
	
	
	private String filename;
	private JSONObject blockDefinitions;
	
	public FlexibleArchitecture(String filename) {
		this.filename = filename;
		
		this.blockTypesPerCategory.put(BlockCategory.IO, new ArrayList<BlockType>());
		this.blockTypesPerCategory.put(BlockCategory.CLB, new ArrayList<BlockType>());
		this.blockTypesPerCategory.put(BlockCategory.HARDBLOCK, new ArrayList<BlockType>());
	}
	
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
			
			BlockType.addType(typeName, category, height, start, repeat, inputs, outputs);
			
			for(int i = 0; i < modes.size(); i++) {
				BlockType.addMode(typeName, modes.get(i), children.get(i));
			}
			
			
			
			if(isGlobal) {
				BlockType blockType = new BlockType(typeName);
				BlockCategory blockCategory = blockType.getCategory();
				
				this.blocks.put(blockType, new ArrayList<AbstractBlock>());
				this.columnsPerBlockType.put(blockType, new ArrayList<Integer>());
				this.blockTypesPerCategory.get(blockCategory).add(blockType);
			}
		}
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
	
	
	
	
	
	
	
	public void loadBlocks(Map<BlockType, List<AbstractBlock>> allBlocks) {
		this.addBlocks(allBlocks);
		this.calculateSizeAndColumns();
		this.createSites();
	}
	
	private void addBlocks(Map<BlockType, List<AbstractBlock>> allBlocks) {
		for(Entry<BlockType, List<AbstractBlock>> blockEntry : this.blocks.entrySet()) {
			BlockType blockType = blockEntry.getKey();
			if(allBlocks.containsKey(blockType)) {
				this.blocks.put(blockType, allBlocks.get(blockType));
			}
		}
	}
	
	private void calculateSizeAndColumns() {
		BlockType ioType = this.blockTypesPerCategory.get(BlockCategory.IO).get(0);
		BlockType clbType = this.blockTypesPerCategory.get(BlockCategory.CLB).get(0);
		List<BlockType> hardBlockTypes = this.blockTypesPerCategory.get(BlockCategory.HARDBLOCK);
		
		
		int numClbColumns = 0;
		int[] numHardBlockColumns = new int[hardBlockTypes.size()];
		for(int i = 0; i < hardBlockTypes.size(); i++) {
			numHardBlockColumns[i] = 0;
		}
		
		List<BlockType> columns = new ArrayList<BlockType>();
		columns.add(ioType);
		int size = 2;
		
		boolean tooSmall = true;
		while(tooSmall) {
			for(int i = 0; i < hardBlockTypes.size(); i++) {
				BlockType hardBlockType = hardBlockTypes.get(i);
				int start = hardBlockType.getStart();
				int repeat = hardBlockType.getRepeat();
				
				if((size - 1 - start) % repeat == 0) {
					columns.add(hardBlockType);
					numHardBlockColumns[i]++;
					break;
				}
			}
			
			if(columns.size() < size) {
				columns.add(clbType);
				numClbColumns++;
			}
			
			size++;
			tooSmall = false;
			
			int clbCapacity = (int) ((size - 2) * numClbColumns * FlexibleArchitecture.FILL_GRADE);
			int ioCapacity = (size - 1) * 4 * BlockType.getIoCapacity();
			if(clbCapacity < this.blocks.get(clbType).size() || ioCapacity < this.blocks.get(ioType).size()) { 
				tooSmall = true;
				continue;
			}
			
			for(int i = 0; i < hardBlockTypes.size(); i++) {
				BlockType hardBlockType = hardBlockTypes.get(i);
				
				int heightPerBlock = hardBlockType.getHeight();
				int blocksPerColumn = (size - 2) / heightPerBlock;
				int capacity = numHardBlockColumns[i] * blocksPerColumn;
				
				if(capacity < this.blocks.get(hardBlockType).size()) {
					tooSmall = true;
					break;
				}
			}
		}
		
		
		columns.add(ioType);
		this.width = size;
		this.height = size;
		
		for(int i = 0; i < columns.size(); i++) {
			this.columnsPerBlockType.get(columns.get(i)).add(i);
		}
	}
	
	private void createSites() {
		this.sites = new AbstractSite[this.width][this.height];
		
		BlockType ioType = this.blockTypesPerCategory.get(BlockCategory.IO).get(0);
		int ioCapacity = BlockType.getIoCapacity();
		
		int size = this.width;
		for(int i = 0; i < size - 1; i++) {
			this.sites[0][i] = new IOSite(0, i, ioCapacity);
			this.sites[size-1][size-1-i] = new IOSite(size-1, size-1-i, ioCapacity);
			this.sites[i][0] = new IOSite(i, 0, ioCapacity);
			this.sites[size-1-i][size-1] = new IOSite(size-1-i, size-1, ioCapacity);
		}
		
		for(Entry<BlockType, List<Integer>> columnEntry : this.columnsPerBlockType.entrySet()) {
			BlockType blockType = columnEntry.getKey();
			if(blockType.equals(ioType)) {
				continue;
			}
			
			int height = blockType.getHeight();
			List<Integer> columns = columnEntry.getValue();
			
			for(int x : columns) {
				for(int y = 1; y < size - 1; y++) {
					this.sites[x][y] = new Site(x, y, height);
				}
			}
		}
	}
	
	
	
	public AbstractSite getSite(int x, int y) {
		return this.sites[x][y];
	}
}
