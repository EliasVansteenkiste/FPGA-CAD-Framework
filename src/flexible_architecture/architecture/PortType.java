package flexible_architecture.architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortType {
	
	private static List<List<String>> portNames = new ArrayList<List<String>>();
	private static List<List<Integer>> portStarts = new ArrayList<List<Integer>>();
	private static List<Map<String, Integer>> ports = new ArrayList<Map<String, Integer>>();
	private static List<Integer> numInputPorts = new ArrayList<Integer>();
	
	private static List<List<Double>> delays = new ArrayList<List<Double>>();
	
	private static int maxPortIndex;
	
	private int blockTypeIndex, portIndex;
	
	static void addBlockType() {
		PortType.portNames.add(new ArrayList<String>());
		PortType.ports.add(new HashMap<String, Integer>());
		PortType.numInputPorts.add(null);
		
		List<Integer> portStart = new ArrayList<Integer>();
		portStart.add(0);
		PortType.portStarts.add(portStart);
	}
	
	static void setNumInputPorts(int blockTypeIndex, int numInputPorts) {
		while(blockTypeIndex >= PortType.ports.size()) {
			PortType.addBlockType();
		}
		
		PortType.numInputPorts.set(blockTypeIndex, numInputPorts);
	}
	
	
	
	static void addPorts(int blockTypeIndex, Map<String, Integer> portsMap) {
		List<String> portNames = PortType.portNames.get(blockTypeIndex);
		List<Integer> portStarts = PortType.portStarts.get(blockTypeIndex);
		Map<String, Integer> ports = PortType.ports.get(blockTypeIndex);
		
		int portIndex = portNames.size();
		int portStart = portStarts.get(portStarts.size() - 1);
		
		for(Map.Entry<String, Integer> inputEntry : portsMap.entrySet()) {
			
			String portName = inputEntry.getKey();
			int numPins = inputEntry.getValue();
			
			portStart += numPins;
			portStarts.add(portStart);
			
			portNames.add(portName);
			ports.put(portName, portIndex);
			
			portIndex++;
		}
		
		if(portIndex > PortType.maxPortIndex) {
			PortType.maxPortIndex = portIndex;
		}
	}
	
	static List<PortType> getPortTypes(int blockTypeIndex) {
		List<String> portNames = PortType.portNames.get(blockTypeIndex);
		List<PortType> portTypes = new ArrayList<PortType>();
		
		for(String portName : portNames) {
			portTypes.add(new PortType(blockTypeIndex, portName));
		}
		
		return portTypes;
	}
	
	static int getNumPins(int blockTypeIndex) {
		List<Integer> portStarts = PortType.portStarts.get(blockTypeIndex);
		return portStarts.get(portStarts.size() - 1);
	}
	
	static int[] getInputPortRange(int blockTypeIndex) {
		int numInputPorts = PortType.numInputPorts.get(blockTypeIndex);
		
		int start = 0;
		int end = PortType.portStarts.get(blockTypeIndex).get(numInputPorts);
		
		int[] portRange = {start, end};
		return portRange;
	}
	static int[] getOutputPortRange(int blockTypeIndex) {
		int numInputPorts = PortType.numInputPorts.get(blockTypeIndex);
		List<Integer> portStarts = PortType.portStarts.get(blockTypeIndex);
		
		int start = portStarts.get(numInputPorts);
		int end = portStarts.get(portStarts.size() - 1);
		
		int[] portRange = {start, end};
		return portRange;
	}
	
	
	public PortType(BlockType blockType, String portName) {
		this(blockType.getIndex(), portName);
	}
	PortType(int blockTypeIndex, String portName) {
		this.blockTypeIndex = blockTypeIndex;
		this.portIndex = PortType.ports.get(this.blockTypeIndex).get(portName);
	}
	
	public String getName() {
		return PortType.portNames.get(this.blockTypeIndex).get(this.portIndex);
	}
	
	public int[] getRange() {
		List<Integer> portStarts = PortType.portStarts.get(this.blockTypeIndex);
		int portStart = portStarts.get(this.portIndex);
		int portEnd = portStarts.get(this.portIndex + 1);
		
		int[] portRange = {portStart, portEnd};
		return portRange;
	}
	
	
	public boolean isOutput() {
		return !this.isInput();
	}
	public boolean isInput() {
		return this.portIndex < PortType.numInputPorts.get(this.blockTypeIndex);
	}
	
	
	private int uniqueId() {
		return this.blockTypeIndex * PortType.maxPortIndex + this.portIndex;
	}
}
