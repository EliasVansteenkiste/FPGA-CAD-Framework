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
import flexible_architecture.site.AbstractSite;

public class PlaceParser {
	
	private static Pattern sizePattern = Pattern.compile("Array size: (?<width>\\d+) x (?<height>\\d+).*");
	private static Pattern sitePattern = Pattern.compile("(?<block>\\S+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)\\s+(?<z>\\d+).*");
	
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
				AbstractSite site = this.circuit.getSite(x, y);
				((GlobalBlock) block).setSite(site);
			}
		}
	}
	
	
	private void processLine(String line) {
		Matcher sizeMatcher = sizePattern.matcher(line);
		boolean sizeMatches = sizeMatcher.matches();
		
		Matcher siteMatcher = sitePattern.matcher(line);
		boolean siteMatches = siteMatcher.matches();
		
		
		if(sizeMatches) {
			int width = Integer.parseInt(sizeMatcher.group("width"));
			int height = Integer.parseInt(sizeMatcher.group("height"));
			
			this.circuit.setSize(width + 2, height + 2);
		
		} else if(siteMatches) {
			String blockName = siteMatcher.group("block");
			int x = Integer.parseInt(siteMatcher.group("x"));
			int y = Integer.parseInt(siteMatcher.group("y"));
			int z = Integer.parseInt(siteMatcher.group("z"));
			
			int[] coordinate = {x, y, z};
			this.coordinates.put(blockName, coordinate);
		}
	}
}
