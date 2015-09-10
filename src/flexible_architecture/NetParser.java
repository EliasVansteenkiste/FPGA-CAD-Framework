package flexible_architecture;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Logger;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.architecture.PortType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.block.LocalBlock;
import flexible_architecture.block.Pin;
import flexible_architecture.net.AbstractNet;

public class NetParser {
	
	private String filename;
	private BufferedReader reader;
	
	private List<AbstractBlock> blocks;
	private Stack<AbstractBlock> blockStack;
	private Stack<HashMap<String, String>> outputsStack;
	
	private Map<String, Pin> sourcePins;
	private Map<String, Pin> sinkPins;
	
	private PortType currentPortType;
	
	
	private static Pattern portPattern = Pattern.compile("name=\"(?<name>[^\"]+)\">(?<ports>.+)</port>");
	private static Pattern blockPattern = Pattern.compile("name=\"(?<name>[^\"]+).*instance=\"(?<type>\\w+)\\[(?<index>\\d+)\\].*(?:mode=\"(?<mode>\\w+)\")");
	
	private static Pattern internalNetPattern = Pattern.compile("(?<block>\\w+)(?:\\[(?<blockIndex>\\d+)\\])?\\.(?<port>\\w+)\\[(?<portIndex>\\d+)\\]");
	
	
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
		
		// blockStack is a stack that contains the current block hierarchy.
		// It is used to find the parent of a block. outputsStack contains
		// the corresponding outputs. This is necessary because the outputs
		// of a block can only be processed after all the childs have been
		// processed.
		this.blockStack = new Stack<AbstractBlock>();
		this.outputsStack = new Stack<HashMap<String, String>>();
		
		// sourcePins contains the names of the outputs of leaf blocks and
		// the corresponding output pins. sinkPins contains input pins of
		// global blocks and the name of the leaf block output pin that 
		// should have a net to that input pin.
		// Both Maps are needed to be able to create global nets: at the time
		// of parsing a sink block, the source block may not have been parsed
		// yet. Both are added to these lists and processed after reading the
		// entire file.
		this.sourcePins = new HashMap<String, Pin>();
		this.sinkPins = new HashMap<String, Pin>();
		
		
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
	    
	    
	    
	    // Add the global nets that could not be processed earlier
	    for(Map.Entry<String, Pin> pinEntry : this.sinkPins.entrySet()) {
	    	Pin sinkPin = pinEntry.getValue();
	    	Pin sourcePin = this.sourcePins.get(pinEntry.getKey());
	    	AbstractBlock parent = sourcePin.getOwner().getParent();
	    	
	    	if(sinkPin.getOwner().getParent() == null) {
	    		Logger.raise("The sink of a global net is not in a global block");
	    	}
	    	
	    	while(parent != null) {
	    		List<Pin> nextSourcePins = sourcePin.getSinks();
	    		Pin nextSourcePin = null;
	    		
	    		for(Pin pin : nextSourcePins) {
	    			if(pin.getOwner() == parent) {
	    				nextSourcePin = pin;
	    				break;
	    			}
	    		}
	    		
	    		if(nextSourcePin == null) {
	    			Logger.raise("No net to parent block found");
	    		}
	    		
	    		sourcePin = nextSourcePin;
	    		parent = sourcePin.getOwner().getParent();
	    	}
	    	
	    	sourcePin.addSink(sinkPin);
	    }
		
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
			this.addNets(this.blockStack.peek(), PortType.INPUT, name, ports);
			break;
			
		case OUTPUT:
			this.outputsStack.peek().put(name, ports);
			break;
			
		default:
			Logger.raise("Port type not initialized");
		}
	}
	
	
	private void processBlockLine(String line) {
		
		Matcher matcher = blockPattern.matcher(line);
		String name = matcher.group("name");
		int index = Integer.parseInt(matcher.group("index"));
		
		String typeString = matcher.group("type");
		String mode = matcher.group("mode");
		BlockType type = new BlockType(typeString, mode);
		
		
		AbstractBlock newBlock;
		if(this.blockStack.size() == 0) {
			newBlock = new GlobalBlock(name, type);
		
		} else {
			AbstractBlock parent = this.blockStack.peek();
			newBlock = new LocalBlock(name, type, parent);
			parent.setChild((LocalBlock) newBlock, index);
		}
		
		
		this.blockStack.push(newBlock);
		this.outputsStack.push(new HashMap<String, String>());
		
		this.blocks.add(newBlock);
	}
	
	private void processBlockEndLine() {
		// Remove this block and its outputs from the stacks
		AbstractBlock block = this.blockStack.pop();
		HashMap<String, String> outputs = this.outputsStack.pop();
		
		// Process the outputs of the block
		for(Map.Entry<String, String> output : outputs.entrySet()) {
			this.addNets(block, PortType.OUTPUT, output.getKey(), output.getValue());
		}
	}
	
	
	private void addNets(AbstractBlock sinkBlock, PortType sinkPortType, String sinkPortName, String netsString) {
		String[] nets = netsString.split("\\s+");
		
		for(int sinkPortIndex = 0; sinkPortIndex < nets.length; sinkPortIndex++) {
			String net = nets[sinkPortIndex];
			
			if(net.equals("open")) {
				continue;
			}
			
			
			Matcher matcher = internalNetPattern.matcher(net);
			
			
			if(matcher.matches()) {
				AbstractBlock sourceBlock;
				PortType sourcePortType;
				
				String sourceBlockType = matcher.group("block");
				String sourceBlockIndexString = matcher.group("blockIndex");
				String sourcePortName = matcher.group("port");
				int sourcePortIndex = Integer.parseInt(matcher.group("portIndex"));
				
				// The net is incident to an input port. It has an input port of the parent block as source.
				if(sourceBlockIndexString == null) {
					sourceBlock = ((LocalBlock) sinkBlock).getParent();
					sourcePortType = PortType.INPUT;
					
				
				} else {
					int sourceBlockIndex = Integer.parseInt(sourceBlockIndexString);
					
					// The net is incident to an input port. It has a sibling output port as source.
					if(sinkPortType == PortType.INPUT) {
						AbstractBlock parent = ((LocalBlock) sinkBlock).getParent();
						sourceBlock = parent.getChild(sourceBlockType, sourceBlockIndex);
						sourcePortType = PortType.OUTPUT;
						
					
					// The net is incident to an output port. It has a child output port as source.
					} else {
						sourceBlock = sinkBlock.getChild(sourceBlockType, sourceBlockIndex);
						sourcePortType = PortType.OUTPUT;
					}
					
				}
				
				sourceBlock.addSink(
						sourcePortType, sourcePortName, sourcePortIndex,
						sinkBlock,
						sinkPortType, sinkPortName, sinkPortIndex);
			
				
			// The input net that we want to add has a leaf block as its (indirect) source.
			// We can't add this net yet, because it is possible that the source block has
			// not been parsed yet. Store it in a todo list.
			} else if(sinkPortType == PortType.INPUT) {
				Pin sinkPin = sinkBlock.getInputPin(sinkPortName, sinkPortIndex);
				this.sinkPins.put(net, sinkPin);
			
			// The current block is a leaf block. We can add a reference from the net name to
			// the correct pin in this block, so that we can add the todo-nets later.
			} else {
				Pin sourcePin = sinkBlock.getOutputPin(sinkPortName, sinkPortIndex);
				this.sourcePins.put(net, sourcePin);
			}
		}
	}
}
