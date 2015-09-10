package flexible_architecture.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.ArrayUtil;
import util.Logger;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.PortType;
import flexible_architecture.net.AbstractNet;

public abstract class AbstractBlock {
	
	private String name;
	private BlockType type;
	
	protected Map<String, LocalBlock[]> children;
	
	private List<Map<String, Pin[]>> pins;
	
	//private Map<String, Pin[]> inputs;
	//private Map<String, Pin[]> outputs;
	
	
	public AbstractBlock(String name, BlockType type) {
		this.name = name;
		this.type = type;
		
		
		this.children = new HashMap<String, LocalBlock[]>();
		Map<String, Integer> numChildren = type.getChildren();
		
		for(Map.Entry<String, Integer> numChild : numChildren.entrySet()) {
			this.children.put(numChild.getKey(), new LocalBlock[numChild.getValue()]);
		}
		
		
		int numPortTypes = PortType.values().length;
		this.pins = new ArrayList<Map<String, Pin[]>>(numPortTypes);
		
		for(PortType portType : PortType.values()) {
			this.createPins(this.pins.get(portType.ordinal()), type.getPorts(portType));
		}
	}
	
	private void createPins(Map<String, Pin[]> pins, Map<String, Integer> numPins) {
		pins = new HashMap<String, Pin[]>();
		
		for(Map.Entry<String, Integer> numPin : numPins.entrySet()) {
			int num = numPin.getValue();
			Pin[] newPins = new Pin[num];
			
			for(int i = 0; i < num; i++) {
				newPins[i] = new Pin(this);
			}
			
			pins.put(numPin.getKey(), newPins);
		}
	}
	
	
	
	
	public String getName() {
		return this.name;
	}
	public BlockType getType() {
		return this.type;
	}
	
	
	
	public Map<String, LocalBlock[]> getChildren() {
		return this.children;
	}
	public LocalBlock[] getChildren(String type) { 
		return this.children.get(type);
	}
	public LocalBlock getChild(String type, int index) {
		return this.getChildren(type)[index];
	}
	public LocalBlock setChild(LocalBlock child, int index) {
		String type = child.getType().getName();
		LocalBlock oldChild = this.children.get(type)[index];
		this.children.get(type)[index] = child;
		return oldChild;
	}
	
	
	
	public Map<String, Pin[]> getInputPins() {
		return this.getPins(PortType.INPUT);
	}
	public Pin[] getInputPins(String portName) {
		return this.getPins(PortType.INPUT, portName);
	}
	public Pin getInputPin(String portName, int index) {
		return this.getPin(PortType.INPUT, portName, index);
	}
	
	public Map<String, Pin[]> getOutputPins() {
		return this.getPins(PortType.OUTPUT);
	}
	public Pin[] getOutputPins(String portName) {
		return this.getPins(PortType.OUTPUT, portName);
	}
	public Pin getOutputPin(String portName, int index) {
		return this.getPin(PortType.OUTPUT, portName, index);
	}
	
	public Map<String, Pin[]> getPins(PortType portType) {
		return this.pins.get(portType.ordinal());
	}	
	private Pin[] getPins(PortType portType, String portName) {
		Map<String, Pin[]> pinsOfType = this.getPins(portType);
		if(pinsOfType.containsKey(portName)) {
			return pinsOfType.get(portName);
		} else {
			Logger.raise("Unknown port name: " + portName);
			return null;
		}
	}
	private Pin getPin(PortType portType, String portName, int index) {
		return this.getPins(portType, portName)[index];
	}
	
	
	
	public void addSink(
			PortType portType, String portName, int portIndex,
			AbstractBlock sink, PortType sinkPortType, String sinkPortName, int sinkPortIndex) {
		/* Source pin input, sink pin input: parent -> child.
		 * Source pin input, sink pin output: impossible.
		 * Source pin output, sink pin input: sibling -> sibling.
		 * Source pin output, sink pin output: child -> parent.
		 */
		
		Pin sourcePin = this.getPin(portType, portName, portIndex);
		Pin sinkPin = sink.getPin(sinkPortType, sinkPortName, sinkPortIndex);
		
		sourcePin.addSink(sinkPin);
		sinkPin.setSource(sourcePin);
	}
	
	public abstract AbstractBlock getParent();
}
