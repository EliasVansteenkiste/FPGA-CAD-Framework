package architecture.circuit.parser;

import java.io.BufferedReader;
import java.io.File;
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

import architecture.BlockType;
import architecture.PortType;
import architecture.circuit.Circuit;
import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.GlobalBlock;
import architecture.circuit.block.LocalBlock;
import architecture.circuit.block.TupleBlockMap;
import architecture.circuit.pin.AbstractPin;

import timing_graph.TimingGraph;
import util.Logger;


public class NetParser {
	
	private Circuit circuit;
	private File file;
	private BufferedReader reader;
	
	private Map<BlockType, List<AbstractBlock>> blocks;
	
	private Stack<AbstractBlock> blockStack;
	private Stack<TupleBlockMap> inputsStack;
	private Stack<HashMap<String, String>> outputsStack;
	
	private Map<String, AbstractPin> sourcePins;
	
	private enum PortDirection {INPUT, OUTPUT};
	private PortDirection currentPortType;
	
	
	private static Pattern portPattern = Pattern.compile(".*name=\"(?<name>[^\"]+)\">(?<ports>.+)</port>.*");
	private static Pattern blockPattern = Pattern.compile(".*name=\"(?<name>[^\"]+)\".+instance=\"(?<type>\\w+)\\[(?<index>\\d+)\\]\"(?:\\s+mode=\"(?<mode>\\w+)?\")?/?>");
	
	private static Pattern internalNetPattern = Pattern.compile("(?<block>\\w+)(?:\\[(?<blockIndex>\\d+)\\])?\\.(?<port>\\w+)\\[(?<portIndex>\\d+)\\]->.*");
	
	
	public NetParser(Circuit circuit, File file) {
		this.circuit = circuit;
		this.file = file;
		
		try {
			this.reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException exception) {
			Logger.raise("Could not find the net file: " + file, exception);
		}
	}
	
	
	public void parse() {
		
		// A list of all the blocks in the circuit
		this.blocks = new HashMap<BlockType, List<AbstractBlock>>();
		
		// blockStack is a stack that contains the current block hierarchy.
		// It is used to find the parent of a block. outputsStack contains
		// the outputs of these blocks. This is necessary because the outputs
		// of a block can only be processed after all the childs have been
		// processed.
		this.blockStack = new Stack<AbstractBlock>();
		this.inputsStack = new Stack<TupleBlockMap>();
		this.outputsStack = new Stack<HashMap<String, String>>();
		
		
		// sourcePins contains the names of the outputs of leaf blocks and
		// the corresponding output pins. It is needed to be able to create
		// global nets: at the time: only the name of the bottom-level source
		// block is given for these nets.
		this.sourcePins = new HashMap<String, AbstractPin>();
		
		
		String line, multiLine = "";
	    try {
			while ((line = this.reader.readLine()) != null) {
				String trimmedLine = line.trim();
				
				
				
				// Add the current line to the multiLine
				if(multiLine.length() > 0) {
					multiLine += " ";
				}
				multiLine += trimmedLine;
				
				if(!this.isCompleteLine(multiLine)) {
					continue;
				}
				
				
				
				String lineStart = multiLine.substring(0, 5);
				
				switch(lineStart) {
				case "<inpu":
					this.processInputLine(multiLine);
					break;
					
					
				case "<outp":
					this.processOutputLine(multiLine);
					break;
				
				case "<cloc":
					this.processClockLine(multiLine);
					break;
					
					
				case "<port":
					this.processPortLine(multiLine);					
					break;
					
					
				case "<bloc":
					if(!multiLine.substring(multiLine.length() - 2).equals("/>")) {
						this.processBlockLine(multiLine);
					}
					break;
					
					
				case "</blo":
					this.processBlockEndLine();
					break;
				}
				
				multiLine = "";
			}
		} catch (IOException exception) {
			Logger.raise("Failed to read from the net file: " + this.file, exception);
		}
		
	    this.circuit.loadBlocks(this.blocks);
	    
	    // Create the timing graph and store it in the circuit
	    TimingGraph timingGraph = new TimingGraph(this.circuit, this.file.getParentFile());
	    timingGraph.build();
	    this.circuit.setTimingGraph(timingGraph);
	}
	
	
	private boolean isCompleteLine(String line) {
		int lineLength = line.length();
		
		// The line is empty
		if(lineLength == 0) {
			return false;
		}
		
		
		// The line doesn't end with a ">" character 
		if(!line.substring(lineLength - 1).equals(">")) {
			return false;
		}
		
		// The line is a port line, but not all ports are on this line
		if(lineLength >= 7 
				&& line.substring(0, 5).equals("<port")
				&& !line.substring(lineLength - 7).equals("</port>")) {
			return false;
		}
		
		return true;
	}
	
	
	
	private void processInputLine(String line) {
		this.currentPortType = PortDirection.INPUT;
	}
	
	private void processOutputLine(String line) {
		this.currentPortType = PortDirection.OUTPUT;
	}
	
	private void processClockLine(String line) {
		this.currentPortType = null;
	}
	
	
	private void processPortLine(String line) {
		
		// This is a clock port
		if(this.currentPortType == null) {
			return;
		}
		
		Matcher matcher = portPattern.matcher(line);
		matcher.matches();
		String name = matcher.group("name");
		String ports = matcher.group("ports");
		
		switch(this.currentPortType) {
		case INPUT:
			this.inputsStack.peek().getMap().put(name, ports);
			//this.addNets(this.blockStack.peek(), PortType.INPUT, name, ports);
			break;
			
		case OUTPUT:
			this.outputsStack.peek().put(name, ports);
			break;
			
		default:
			Logger.raise("Port type not set");
		}
	}
	
	
	private void processBlockLine(String line) {
		Matcher matcher = blockPattern.matcher(line);
		matcher.matches();
		
		String name = matcher.group("name");
		String type = matcher.group("type");
		int index = Integer.parseInt(matcher.group("index"));
		String mode = matcher.group("mode");
		
		// Ignore the top-level block
		if(type.equals("FPGA_packed_netlist")) {
			return;
		}
		
		BlockType blockType = new BlockType(type, mode);
		
		
		AbstractBlock newBlock;
		if(this.blockStack.size() == 0) {
			newBlock = new GlobalBlock(name, blockType, index);
		
		} else {
			AbstractBlock parent = this.blockStack.peek();
			newBlock = new LocalBlock(name, blockType, index, parent);
			parent.setChild((LocalBlock) newBlock, index);
		}
		
		
		this.blockStack.push(newBlock);
		this.inputsStack.push(new TupleBlockMap(newBlock));
		this.outputsStack.push(new HashMap<String, String>());
		
		
		if(!this.blocks.containsKey(blockType)) {
			BlockType emptyModeType = new BlockType(blockType.getName());
			this.blocks.put(emptyModeType, new ArrayList<AbstractBlock>());
		}
		this.blocks.get(blockType).add(newBlock);
	}
	
	private void processBlockEndLine() {
		// If the stack is empty: this is the top-level block
		// All that is left to do is process all the inputs of
		// the global blocks
		if(this.blockStack.size() == 0) {
			while(this.inputsStack.size() > 0) {
				TupleBlockMap globalTuple = this.inputsStack.pop();
				AbstractBlock globalBlock = globalTuple.getBlock();
				Map<String, String> inputs = globalTuple.getMap();
				
				processPortsHashMap(globalBlock, inputs);
			}
		
		// This is 
		} else {
			// Remove this block and its outputs from the stacks
			AbstractBlock block = this.blockStack.pop();
			
			HashMap<String, String> outputs = this.outputsStack.pop();
			processPortsHashMap(block, outputs);
			
			// Process the inputs of all the children of this block, but
			// not of this block itself. This is because the inputs may
			// come from sibling blocks that haven't been parsed yet.
			while(this.inputsStack.peek().getBlock() != block) {
				TupleBlockMap childTuple = this.inputsStack.pop();
				AbstractBlock childBlock = childTuple.getBlock();
				Map<String, String> inputs = childTuple.getMap();
				
				processPortsHashMap(childBlock, inputs);
			}
		}
	}
	
	private void processPortsHashMap(AbstractBlock block, Map<String, String> ports) {
		for(Map.Entry<String, String> portEntry : ports.entrySet()) {
			String portName = portEntry.getKey();
			PortType portType = new PortType(block.getType(), portName);
			List<AbstractPin> pins = block.getPins(portType);
			
			String nets = portEntry.getValue();
			
			this.addNets(pins, nets);
		}
	}
	
	
	private void addNets(List<AbstractPin> sinkPins, String netsString) {
		String[] nets = netsString.trim().split("\\s+");
		
		for(int sinkPinIndex = 0; sinkPinIndex < nets.length; sinkPinIndex++) {
			AbstractPin sinkPin = sinkPins.get(sinkPinIndex);
			String net = nets[sinkPinIndex];
			
			this.addNet(sinkPin, net);	
		}
	}
	
	
	private void addNet(AbstractPin sinkPin, String net) {
		if(net.equals("open")) {
			return;
		}
		
		AbstractBlock sinkBlock = sinkPin.getOwner();
		
		Matcher matcher = internalNetPattern.matcher(net);
		boolean matches = matcher.matches();
		
		
		if(matches) {
			String sourceBlockTypeString = matcher.group("block");
			BlockType sourceBlockType = new BlockType(sourceBlockTypeString);
			
			String sourcePortName = matcher.group("port");
			PortType sourcePortType = new PortType(sourceBlockType, sourcePortName);
			
			
			String sourceBlockIndexString = matcher.group("blockIndex");
			int sourcePinIndex = Integer.parseInt(matcher.group("portIndex"));
			
			
			// The hardest part: determine the source block
			AbstractBlock sourceBlock;
			
			// The net is incident to an input port. It has an input port of the parent block as source.
			if(sourceBlockIndexString == null) {
				sourceBlock = ((LocalBlock) sinkBlock).getParent();
				
			
			} else {
				int sourceBlockIndex = Integer.parseInt(sourceBlockIndexString);
				
				// The net is incident to an input port. It has a sibling output port as source.
				if(sinkPin.isInput()) {
					AbstractBlock parent = ((LocalBlock) sinkBlock).getParent();
					sourceBlock = parent.getChild(sourceBlockType, sourceBlockIndex);
					
				
				// The net is incident to an output port. It has either a child output port as source
				// or an input port of itself.
				} else {
					if(sinkBlock.getType().equals(sourceBlockType)) {
						sourceBlock = sinkBlock;
					} else {
						sourceBlock = sinkBlock.getChild(sourceBlockType, sourceBlockIndex);
					}
				}
			}
			
			AbstractPin sourcePin = sourceBlock.getPin(sourcePortType, sourcePinIndex);
			sourcePin.addSink(sinkPin);
			sinkPin.setSource(sourcePin);
		
			
		// The current block is a leaf block. We can add a reference from the net name to
		// the correct pin in this block, so that we can add the todo-nets later.
		} else if(sinkPin.isOutput()) {
			this.sourcePins.put(net, sinkPin);
		
			
		// The input net that we want to add has a leaf block as its (indirect) source.
		// Finding the source block for the net is a bit tricky, because we have to trickle
		// up through the hierarchy from the referenced block.
		} else {
			String sourceName = net;
			
	    	AbstractPin sourcePin = this.sourcePins.get(sourceName);
	    	AbstractBlock parent = sourcePin.getOwner().getParent();
	    	
	    	if(!sinkPin.getOwner().isGlobal()) {
	    		Logger.raise("Found a global net with a sink that is not in a global block");
	    	}
	    	
	    	while(parent != null) {
	    		int numPins = sourcePin.getNumSinks();
	    		AbstractPin nextSourcePin = null;
	    		
	    		for(int i = 0; i < numPins; i++) {
	    			AbstractPin pin = sourcePin.getSink(i); 
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
	    	sinkPin.setSource(sourcePin);
		}
	}
}
