package placers.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import architecture.Architecture;
import architecture.Site;

import circuit.Block;
import circuit.PackedCircuit;

import placers.Placer;

public class PlaceParser extends Placer {
	
	private static Pattern pattern = Pattern.compile("(?<block>\\S+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)\\s+(?<z>\\d+)");
	
	private Map<String, int[]> coordinates;
	
	public PlaceParser(Architecture architecture, PackedCircuit circuit, Map<String, String> options) {
		super(architecture, circuit, options);
	}
	
	
	public void place() {
		
		// Open the place file that should have been provided in the options
		if(!this.hasOption("place_file")) {
			System.err.println("PlaceParser requires a place_file value");
			System.exit(1);
		}
		
		String placeFile = this.getOption("place_file");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(placeFile));
		} catch (FileNotFoundException e) {
			System.err.println("Place file not found: " + placeFile);
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
			System.err.println("Failed to read from place file: " + placeFile);
			e.printStackTrace();
			System.exit(1);
		}
		
		
		// Loop over all the blocks in the circuit
		this.circuit.fillVector();
		
		for(Block block : this.circuit.vBlocks) {
			
			// Get the coordinate of the block
			String blockName = block.name;
			if(!this.coordinates.containsKey(blockName)) {
				System.err.println("Block not found in placement file: " + blockName);
				System.exit(1);
			}
			
			int[] coordinate = this.coordinates.get(blockName);
			int x = coordinate[0], y = coordinate[1], z = coordinate[2];
			
			
			// Bind the site and block to each other
			Site site = this.architecture.getSite(x, y, z);
			block.setSite(site);
			site.setBlock(block);
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
