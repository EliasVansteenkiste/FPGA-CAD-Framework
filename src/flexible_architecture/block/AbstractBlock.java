package flexible_architecture.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.Logger;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.PortType;
import flexible_architecture.pin.AbstractPin;

public abstract class AbstractBlock {
	
	private String name;
	private BlockType type;
	private int index;
	
	protected Map<String, LocalBlock[]> children;
	
	private List<Map<String, AbstractPin[]>> pins;
	
	
	public AbstractBlock(String name, BlockType type, int index) {
		this.name = name;
		this.type = type;
		this.index = index;
		
		
		Map<String, Integer> numChildren = type.getChildren();
		int capacity = (int) Math.ceil(numChildren.size() * 1.33);
		this.children = new HashMap<String, LocalBlock[]>(capacity);
		
		for(Map.Entry<String, Integer> childEntry : numChildren.entrySet()) {
			this.children.put(childEntry.getKey(), new LocalBlock[childEntry.getValue()]);
		}
		
		
		int numPortTypes = PortType.values().length;
		this.pins = new ArrayList<Map<String, AbstractPin[]>>(numPortTypes);
		
		for(PortType portType : PortType.values()) {
			Map<String, Integer> pinCounts = type.getPorts(portType); 
			
			Map<String, AbstractPin[]> pins = this.createPins(portType, pinCounts);
			this.pins.add(pins);
		}
	}
	
	private Map<String, AbstractPin[]> createPins(PortType portType, Map<String, Integer> pinCounts) {
		int capacity = (int) Math.ceil(pinCounts.size() * 1.33);
		Map<String, AbstractPin[]> pins = new HashMap<String, AbstractPin[]>(capacity);
		
		for(Map.Entry<String, Integer> pinsEntry : pinCounts.entrySet()) {
			
			String portName = pinsEntry.getKey();
			int numPins = pinsEntry.getValue();
			
			AbstractPin[] newPins = new AbstractPin[numPins];
			
			for(int index = 0; index < numPins; index++) {
				newPins[index] = createPin(portType, portName, index);
			}
			
			pins.put(portName, newPins);
		}
		
		return pins;
	}
	
	public abstract AbstractPin createPin(PortType portType, String portName, int index);
	
	
	public String getName() {
		return this.name;
	}
	public BlockType getType() {
		return this.type;
	}
	public int getIndex() {
		return this.index;
	}
	
	public boolean isFixed() {
		return false;
	}
	
	public boolean isGlobal() {
		return this.getType().isGlobal();
	}
	public abstract AbstractBlock getParent();
	
	
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
	
	
	
	public Map<String, AbstractPin[]> getInputPins() {
		return this.getPins(PortType.INPUT);
	}
	public AbstractPin[] getInputPins(String portName) {
		return this.getPins(PortType.INPUT, portName);
	}
	public AbstractPin getInputPin(String portName, int index) {
		return this.getPin(PortType.INPUT, portName, index);
	}
	
	public Map<String, AbstractPin[]> getOutputPins() {
		return this.getPins(PortType.OUTPUT);
	}
	public AbstractPin[] getOutputPins(String portName) {
		return this.getPins(PortType.OUTPUT, portName);
	}
	public AbstractPin getOutputPin(String portName, int index) {
		return this.getPin(PortType.OUTPUT, portName, index);
	}
	
	public Map<String, AbstractPin[]> getPins(PortType portType) {
		return this.pins.get(portType.ordinal());
	}	
	private AbstractPin[] getPins(PortType portType, String portName) {
		Map<String, AbstractPin[]> pinsOfType = this.getPins(portType);
		if(pinsOfType.containsKey(portName)) {
			return pinsOfType.get(portName);
		} else {
			Logger.raise("Unknown port name: " + portName);
			return null;
		}
	}
	private AbstractPin getPin(PortType portType, String portName, int index) {
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
		
		AbstractPin sourcePin = this.getPin(portType, portName, portIndex);
		AbstractPin sinkPin = sink.getPin(sinkPortType, sinkPortName, sinkPortIndex);
		
		sourcePin.addSink(sinkPin);
		sinkPin.setSource(sourcePin);
	}
	
	
	public String toString() {
		return this.type.toString() + ":" + this.getName();
	}
}
