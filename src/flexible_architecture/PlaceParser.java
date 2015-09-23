package flexible_architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;

public class PlaceParser {
	
	private static Pattern pattern = Pattern.compile("(?<block>\\S+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)\\s+(?<z>\\d+)");
	
	private Map<String, int[]> coordinates;
	private Circuit circuit;
	private File file;
	
	public PlaceParser(Circuit circuit, File file) {
		this.circuit = circuit;
		this.file = file;
	}
	
	
	public void parse() {
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(this.file));
		} catch (FileNotFoundException e) {
			System.err.println("Place file not found: " + this.file);
			e.printStackTrace();
			System.exit(1);
		}
		
		
		// Read all the coordinates and store them
		this.coordinates = new HashMap<String, int[]>();
		
		String line;
		try {
			while((line = reader.readLine()) != null) {
				this.processLine(line);
			}
		} catch (IOException e) {
			System.err.println("Failed to read from place file: " + this.file);
			e.printStackTrace();
			System.exit(1);
		}
		
		
		// Loop over all the blocks in the circuit
		Collection<BlockType> blockTypes = this.circuit.getGlobalBlockTypes();
		
		for(BlockType blockType : blockTypes) {
			for(AbstractBlock block : this.circuit.getBlocks(blockType)) {
				
				// Get the coordinate of the block
				String blockName = block.getName();
				if(!this.coordinates.containsKey(blockName)) {
					System.err.println("Block not found in placement file: " + blockName);
					System.exit(1);
				}
				
				int[] coordinate = this.coordinates.get(blockName);
				int x = coordinate[0], y = coordinate[1];
				
				// Bind the site and block to each other
				this.circuit.putBlock((GlobalBlock) block, x, y);
			}
		}
	}
	
	
	private void processLine(String line) {
		Matcher matcher = pattern.matcher(line);
		boolean matches = matcher.matches();
		
		if(matches) {
			String blockName = matcher.group("block");
			int x = Integer.parseInt(matcher.group("x"));
			int y = Integer.parseInt(matcher.group("y"));
			int z = Integer.parseInt(matcher.group("z"));
			
			int[] coordinate = {x, y, z};
			this.coordinates.put(blockName, coordinate);
		}
	}
}
