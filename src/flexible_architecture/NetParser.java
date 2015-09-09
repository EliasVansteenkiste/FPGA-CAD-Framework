package flexible_architecture;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Logger;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.block.LocalBlock;

public class NetParser {
	
	private String filename;
	private BufferedReader reader;
	
	private List<AbstractBlock> blocks;
	private Stack<AbstractBlock> blockStack;
	
	private List<HashMap<String, String>> inputs;
	private List<HashMap<String, String>> outputs;
	private enum PortType {INPUT, OUTPUT};
	private PortType currentPortType;
	
	
	private static Pattern portPattern = Pattern.compile("name=\"(?<name>[^\"]*)\">(?<ports>.*)</port>");
	private static Pattern blockPattern = Pattern.compile("name=\"(?<name>[^\"]*).*instance=\"(?<instance>[^[]*)");
	
	
	public NetParser(String filename) {
		this.filename = filename;
		
		try {
			this.reader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException exception) {
			Logger.raise("Could not find the net file: " + filename, exception);
		}
	}
	
	
	public List<AbstractBlock> parse() {
		
		// These arrays and stack will be built by the "process....Line()" methods
		
		// A list of all the blocks in the circuit
		this.blocks = new ArrayList<AbstractBlock>();
		
		// A list that contains the inputs and outputs for all the blocks.
		// We can't build any nets before all the blocks are known, so we
		// temporarily store the ports as strings (like they appear in the
		// net file).
		this.inputs = new ArrayList<HashMap<String, String>>();
		this.outputs = new ArrayList<HashMap<String, String>>();
		
		// A stack that contains the current block hierarchy. Used to find
		// the parent of a block;
		this.blockStack = new Stack<AbstractBlock>();
		
		
		String line;
	    try {
			while ((line = this.reader.readLine()) != null) {
				String trimmedLine = line.trim();
				String lineStart = trimmedLine.substring(0, 5);
				
				switch(lineStart) {
				case "<inpu":
					this.processInputLine(trimmedLine);
					break;
					
					
				case "<outp":
					this.processOutputLine(trimmedLine);
					break;
					
					
				case "<port":
					this.processPortLine(trimmedLine);					
					break;
					
					
				case "<bloc":
					this.processBlockLine(trimmedLine);
					break;
					
					
				case "</blo":
					this.processBlockEndLine();
					break;
				}
			}
		} catch (IOException exception) {
			Logger.raise("Failed to read from the net file: " + this.filename, exception);
		}
	    
	    //TODO: create nets
		
		return this.blocks;
	}
	
	
	
	private void processInputLine(String line) {
		this.currentPortType = PortType.INPUT;
	}
	
	private void processOutputLine(String line) {
		this.currentPortType = PortType.OUTPUT;
	}
	
	
	private void processPortLine(String line) {
		Matcher matcher = portPattern.matcher(line);
		String name = matcher.group("name");
		String ports = matcher.group("ports");
		
		switch(this.currentPortType) {
		case INPUT:
			this.inputs.get(this.inputs.size() - 1).put(name, ports);
			break;
			
		case OUTPUT:
			this.outputs.get(this.outputs.size() - 1).put(name, ports);
			break;
			
		default:
			Logger.raise("Port type not initialized");
		}
	}
	
	
	private void processBlockLine(String line) {
		
		Matcher matcher = blockPattern.matcher(line);
		String name = matcher.group("name");
		String instance = matcher.group("instance");
		
		BlockType type = new BlockType(instance);
		if(type == null) {
			Logger.raise("Invalid block type: " + instance);
		}
		
		
		AbstractBlock newBlock;
		if(this.blockStack.size() == 0) {
			newBlock = new GlobalBlock(name, type);
		
		} else {
			AbstractBlock parent = this.blockStack.peek();
			newBlock = new LocalBlock(name, type, parent);
			parent.addChild((LocalBlock) newBlock);
		}
		
		
		this.blockStack.push(newBlock);
		
		this.blocks.add(newBlock);
		this.inputs.add(new HashMap<String, String>());
		this.outputs.add(new HashMap<String, String>());
	}
	
	private void processBlockEndLine() {
		this.blockStack.pop();
	}
}
