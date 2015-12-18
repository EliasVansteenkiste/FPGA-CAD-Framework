package circuit.architecture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // These lists contain one element for each block type
    private List<Map<String, Integer>> ports = new ArrayList<>();
    private List<Integer> firstInputPorts = new ArrayList<>();
    private List<Integer> firstOutputPorts = new ArrayList<>();
    private List<Integer> numInputPorts = new ArrayList<>();
    private List<List<Integer>> portStarts = new ArrayList<>();

    private List<Integer> carryFromPorts = new ArrayList<>();
    private List<Integer> carryToPorts = new ArrayList<>();
    private List<Integer> carryOffsetsY = new ArrayList<>();

    private List<List<Double>> delays = new ArrayList<>();
    private double inputSetupTime;



    void addPorts(int blockTypeIndex, Map<String, Integer> inputPorts, Map<String, Integer> outputPorts) {

        int currentNumBlocks = this.ports.size();
        for(int i = currentNumBlocks; i <= blockTypeIndex; i++) {
            this.addBlockType();
        }

        assert(this.firstInputPorts.get(blockTypeIndex) == null);
        assert(this.firstOutputPorts.get(blockTypeIndex) == null);

        this.numInputPorts.set(blockTypeIndex, inputPorts.size());

        this.firstInputPorts.set(blockTypeIndex, this.portTypes.size());
        this.addPorts(blockTypeIndex, inputPorts);

        this.firstOutputPorts.set(blockTypeIndex, this.portTypes.size());
        this.addPorts(blockTypeIndex, outputPorts);
    }

    void addBlockType() {
        this.ports.add(new HashMap<String, Integer>());
        this.firstInputPorts.add(null);
        this.firstOutputPorts.add(null);
        this.numInputPorts.add(null);

        ArrayList<Integer> portStart = new ArrayList<Integer>();
        portStart.add(0);
        this.portStarts.add(portStart);

        this.carryFromPorts.add(null);
        this.carryToPorts.add(null);
        this.carryOffsetsY.add(null);
    }

    private void addPorts(int blockTypeIndex, Map<String, Integer> ports) {

        Map<String, Integer> blockTypePorts = this.ports.get(blockTypeIndex);
        List<Integer> portStart = this.portStarts.get(blockTypeIndex);

        int totalNumPins = portStart.get(portStart.size() - 1);

        for(Map.Entry<String, Integer> port : ports.entrySet()) {
            String portName = port.getKey();
            Integer numPins = port.getValue();

            this.portNames.add(portName);
            totalNumPins += numPins;
            portStart.add(totalNumPins);


            blockTypePorts.put(portName, this.portTypes.size());

            this.blockTypeIndexes.add(blockTypeIndex);
            this.portTypes.add(new PortType(blockTypeIndex, portName));
        }
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



    void postProcess() {
        this.firstInputPorts.add(this.portTypes.size());

        int numPorts = this.portNames.size();
        for(int i = 0; i < numPorts; i++) {
            this.delays.add(new ArrayList<Double>(Collections.nCopies(numPorts, 0.0)));
        }
    }



    int getTypeIndex(int blockTypeIndex, String portName) {
        return this.ports.get(blockTypeIndex).get(portName);
    }
    int getBlockTypeIndex(int portTypeIndex) {
        return this.blockTypeIndexes.get(portTypeIndex);
    }


    List<PortType> getPortTypes(int blockTypeIndex) {
        return this.portTypes.subList(
                this.firstInputPorts.get(blockTypeIndex),
                this.firstInputPorts.get(blockTypeIndex + 1));
    }

    int getNumPins(int blockTypeIndex) {
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);
        return typePortStarts.get(typePortStarts.size() - 1);
    }

    int[] getInputPortRange(int blockTypeIndex) {
        int numInputPorts = this.numInputPorts.get(blockTypeIndex);
        int numInputPins = this.portStarts.get(blockTypeIndex).get(numInputPorts);

        int[] portRange = {0, numInputPins};
        return portRange;
    }
    int[] getOutputPortRange(int blockTypeIndex) {
        int numInputPorts = this.numInputPorts.get(blockTypeIndex);

        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);
        int numInputPins = typePortStarts.get(numInputPorts);
        int numPins = typePortStarts.get(typePortStarts.size() - 1);

        int[] portRange = {numInputPins, numPins};
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

        this.delays.get(portTypeIndex).set(portTypeIndex, delay);
    }
    double getSetupTime(int portTypeIndex) {
        return this.delays.get(portTypeIndex).get(portTypeIndex);
    }

    void setClockSetupTime(double delay) {
        this.inputSetupTime = delay;
    }
    double getClockSetupTime() {
        return this.inputSetupTime;
    }

    void setDelay(int sourcePortTypeIndex, int sinkPortTypeIndex, double delay) {
        this.delays.get(sourcePortTypeIndex).set(sinkPortTypeIndex, delay);
    }
    public double getDelay(int sourcePortTypeIndex, int sinkPortTypeIndex) {
        return this.delays.get(sourcePortTypeIndex).get(sinkPortTypeIndex);
    }



    String getName(int portTypeIndex) {
        return this.portNames.get(portTypeIndex);
    }

    int[] getRange(int portTypeIndex) {
        int blockTypeIndex = this.blockTypeIndexes.get(portTypeIndex);
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);

        int relativePortTypeIndex = portTypeIndex - this.firstInputPorts.get(blockTypeIndex);
        int portStart = typePortStarts.get(relativePortTypeIndex);
        int portEnd = typePortStarts.get(relativePortTypeIndex + 1);

        int[] portRange = {portStart, portEnd};
        return portRange;
    }

    int getNumInputPorts(int blockTypeIndex) {
        return this.numInputPorts.get(blockTypeIndex);
    }

    boolean isInput(int portTypeIndex) {
        int blockTypeIndex = this.blockTypeIndexes.get(portTypeIndex);
        return portTypeIndex < this.firstOutputPorts.get(blockTypeIndex);
    }
}
