package route.circuit.architecture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import route.util.Pair;

public class PortTypeData implements Serializable {
    /**
     * This is a singleton class.
     */

    private static final long serialVersionUID = -7976796410883815420L;


    // Stuff that provides singleton functionality
    private static PortTypeData instance = new PortTypeData();
    static PortTypeData getInstance() {
        return PortTypeData.instance;
    }
    static void setInstance(PortTypeData instance) {
        PortTypeData.instance = instance;
    }

    // These lists contain one element for each port type
    private List<String> portNames = new ArrayList<>();
    private List<Integer> blockTypeIndexes = new ArrayList<>();
    private List<PortType> portTypes = new ArrayList<>();
    //private List<Integer> portEnds = new ArrayList<>();
    private List<int[]> portRanges = new ArrayList<>();
    private List<Boolean> portEquivalence = new ArrayList<>();

    private Map<Long, Double> delays = new HashMap<>();
    private Map<Long, DelayMap> delayMaps = new HashMap<>();

    // These lists contain one element for each block type
    private List<Map<String, Integer>> ports = new ArrayList<>();
    private List<Integer> lastInputPorts = new ArrayList<>();
    private List<Integer> lastOutputPorts = new ArrayList<>();
    private List<Integer> lastClockPorts = new ArrayList<>();

    private List<Integer> carryFromPorts = new ArrayList<>();
    private List<Integer> carryToPorts = new ArrayList<>();
    private List<Integer> carryOffsetsY = new ArrayList<>();
    
    private int numPortTypes;

    private PortTypeData() {
        this.lastInputPorts.add(-1);
        this.lastOutputPorts.add(-1);
        this.lastClockPorts.add(-1);
    }

    void addPorts(
            int blockTypeIndex,
            Map<String, Pair<Integer, Boolean>> inputPorts,
            Map<String, Pair<Integer, Boolean>> outputPorts,
            Map<String, Pair<Integer, Boolean>> clockPorts) {

        int currentNumBlocks = this.ports.size();
        for(int i = currentNumBlocks; i <= blockTypeIndex; i++) {
            this.addBlockType();
        }

        assert(this.lastInputPorts.get(blockTypeIndex + 1) == null);
        assert(this.lastOutputPorts.get(blockTypeIndex + 1) == null);
        assert(this.lastClockPorts.get(blockTypeIndex + 1) == null);


        int numPins = this.addPorts(blockTypeIndex, inputPorts, 0);
        this.lastInputPorts.set(blockTypeIndex + 1, this.portTypes.size() - 1);

        numPins = this.addPorts(blockTypeIndex, outputPorts, numPins);
        this.lastOutputPorts.set(blockTypeIndex + 1, this.portTypes.size() - 1);

        this.addPorts(blockTypeIndex, clockPorts, numPins);
        this.lastClockPorts.set(blockTypeIndex + 1, this.portTypes.size() - 1);
    }

    void addBlockType() {
        this.ports.add(new HashMap<String, Integer>());
        this.lastInputPorts.add(null);
        this.lastOutputPorts.add(null);
        this.lastClockPorts.add(null);

        this.carryFromPorts.add(null);
        this.carryToPorts.add(null);
        this.carryOffsetsY.add(null);
    }
    
    void postProcess() {
    	this.numPortTypes = this.portTypes.size();
    }

    private int addPorts(int blockTypeIndex, Map<String, Pair<Integer, Boolean>> ports, int numPins) {

        Map<String, Integer> blockTypePorts = this.ports.get(blockTypeIndex);

        for(Map.Entry<String, Pair<Integer, Boolean>> port : ports.entrySet()) {
            String portName = port.getKey();
            Pair<Integer, Boolean> numPortPinsAndEquivalance = port.getValue();

            Integer numPortPins = numPortPinsAndEquivalance.getFirst();
            Boolean equivalence = numPortPinsAndEquivalance.getSecond();
            
            this.portNames.add(portName);

            int[] portRange = {numPins, numPins + numPortPins};
            numPins += numPortPins;
            this.portRanges.add(portRange);
            this.portEquivalence.add(equivalence);

            blockTypePorts.put(portName, this.portTypes.size());

            this.blockTypeIndexes.add(blockTypeIndex);
            this.portTypes.add(new PortType(blockTypeIndex, portName));
        }

        return numPins;
    }


    void setCarryPorts(PortType carryFromPort, PortType carryToPort, int carryOffsetY) {
        int fromBlockTypeIndex = carryFromPort.getBlockTypeIndex();
        int toBlockTypeIndex = carryToPort.getBlockTypeIndex();
        assert(fromBlockTypeIndex == toBlockTypeIndex);

        int fromPortTypeIndex = carryFromPort.getPortTypeIndex();
        int toPortTypeIndex = carryToPort.getPortTypeIndex();

        this.carryFromPorts.set(fromBlockTypeIndex, fromPortTypeIndex);
        this.carryToPorts.set(fromBlockTypeIndex, toPortTypeIndex);
        this.carryOffsetsY.set(fromBlockTypeIndex, carryOffsetY);
    }

    int getTypeIndex(int blockTypeIndex, String portName) {
        return this.ports.get(blockTypeIndex).get(portName);
    }
    int getBlockTypeIndex(int portTypeIndex) {
        return this.blockTypeIndexes.get(portTypeIndex);
    }


    List<PortType> getPortTypes(int blockTypeIndex) {
        return this.portTypes.subList(
                this.lastClockPorts.get(blockTypeIndex) + 1,
                this.lastClockPorts.get(blockTypeIndex + 1) + 1);
    }

    int getNumPins(int blockTypeIndex) {
        return this.portRanges.get(this.lastClockPorts.get(blockTypeIndex + 1))[1];
    }

    int[] getInputPortRange(int blockTypeIndex) {
        int firstInputPort = this.lastClockPorts.get(blockTypeIndex) + 1;
        int lastInputPort = this.lastInputPorts.get(blockTypeIndex + 1);

        return getPortRange(firstInputPort, lastInputPort);
    }
    int[] getOutputPortRange(int blockTypeIndex) {
        int firstOutputPort = this.lastInputPorts.get(blockTypeIndex + 1) + 1;
        int lastOutputPort = this.lastOutputPorts.get(blockTypeIndex + 1);

        return getPortRange(firstOutputPort, lastOutputPort);
    }
    int[] getClockPortRange(int blockTypeIndex) {
        int firstClockPort = this.lastOutputPorts.get(blockTypeIndex + 1) + 1;
        int lastClockPort = this.lastClockPorts.get(blockTypeIndex + 1);

        return getPortRange(firstClockPort, lastClockPort);
    }

    private int[] getPortRange(int firstPort, int lastPort) {
        int[] portRange = {0, 0};

        if(firstPort <= lastPort) {
            portRange[0] = this.portRanges.get(firstPort)[0];
            portRange[1] = this.portRanges.get(lastPort)[1];
        }

        return portRange;
    }

    PortType getCarryFromPort(int blockTypeIndex) {
        Integer portTypeIndex = this.carryFromPorts.get(blockTypeIndex);
        return portTypeIndex == null ? null : this.portTypes.get(portTypeIndex);
    }
    PortType getCarryToPort(int blockTypeIndex) {
        Integer portTypeIndex = this.carryToPorts.get(blockTypeIndex);
        return portTypeIndex == null ? null : this.portTypes.get(portTypeIndex);
    }
    Integer getCarryOffsetY(int blockTypeIndex) {
        return this.carryOffsetsY.get(blockTypeIndex);
    }

    void setSetupTime(int portTypeIndex, double delay) {
    	// This method can be used both to set the setup time ("T_setup" in architecture files)
    	// and clock to port time ("T_clock_to_Q")
    	// Which of the two it is, depends on whether the port is an input or output
    	
    	long delayId = this.delayId(portTypeIndex, portTypeIndex);
    	this.setDelay(delayId, delay);
    }
    double getSetupTime(int portTypeIndex) {
    	long delayId = this.delayId(portTypeIndex, portTypeIndex);
    	return this.getDelay(delayId);
    }
    
    /**************
     *  Set Delay *
     **************/
    public void setDelay(DelayElement d) {
    	long delayId = this.delayId(d.sourcePortTypeIndex, d.sinkPortTypeIndex);
    	if(d.sourceBlockIndex == -1 && d.sourcePortIndex == -1 && d.sinkBlockIndex == -1 && d.sinkPortIndex == -1) {
    		this.setDelay(delayId, d.delay);
    	} else {
    		DelayMap delayMap = this.delayMaps.get(delayId);
    		if(delayMap == null) {
    			delayMap = new DelayMap();
    			this.delayMaps.put(delayId, delayMap);
    		}
    		delayMap.add(d);
    	}
    }
    private void setDelay(long delayId, double delay) {
    	this.delays.put(delayId, delay);
    }
    
    /**************
     *  Get Delay * 
     **************/
    public Double getDelay(int sourceBlockIndex, int sourcePortTypeIndex, int sourcePortIndex, int sinkBlockIndex, int sinkPortTypeIndex, int sinkPortIndex) {
    	long delayId = this.delayId(sourcePortTypeIndex, sinkPortTypeIndex);
    	
    	Double delay = null;
    	DelayMap delayMap = this.delayMaps.get(delayId);
    	
    	if(delayMap != null) delay = delayMap.get(sourceBlockIndex, sourcePortIndex, sinkBlockIndex, sinkPortIndex);
    	
    	return delay == null ? this.getDelay(delayId) : delay;
    }
    
    private double getDelay(long delayId) {
        Double delay = this.delays.get(delayId);
        return delay == null ? 0 : delay;
    }

    private long delayId(int sourcePortTypeIndex, int sinkPortTypeIndex) {
    	return this.numPortTypes * sourcePortTypeIndex + sinkPortTypeIndex;
    }

    
    
    String getName(int portTypeIndex) {
        return this.portNames.get(portTypeIndex);
    }

    int[] getRange(int portTypeIndex) {
        return this.portRanges.get(portTypeIndex);
    }

    boolean isInput(int portTypeIndex) {
        int blockTypeIndex = this.blockTypeIndexes.get(portTypeIndex);
        return portTypeIndex <= this.lastInputPorts.get(blockTypeIndex + 1);
    }
    boolean isOutput(int portTypeIndex) {
        int blockTypeIndex = this.blockTypeIndexes.get(portTypeIndex);
        return
                portTypeIndex > this.lastInputPorts.get(blockTypeIndex + 1)
                && portTypeIndex <= this.lastOutputPorts.get(blockTypeIndex + 1);
    }
    boolean isClock(int portTypeIndex) {
        int blockTypeIndex = this.blockTypeIndexes.get(portTypeIndex);
        return portTypeIndex > this.lastOutputPorts.get(blockTypeIndex + 1);
    }
    
    boolean isEquivalent(int portTypeIndex) {
    	return this.portEquivalence.get(portTypeIndex);
    }
}
