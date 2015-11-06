package circuit.architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortTypeData {
    /**
     * This is a singleton class.
     */

    // Stuff that provides singleton functionality
    private static PortTypeData instance = new PortTypeData();
    public static PortTypeData getInstance() {
        return PortTypeData.instance;
    }


    private List<List<String>> portNames = new ArrayList<List<String>>();
    private List<List<PortType>> portTypes = new ArrayList<List<PortType>>();
    private List<List<Integer>> portStarts = new ArrayList<List<Integer>>();
    private List<Map<String, Integer>> ports = new ArrayList<Map<String, Integer>>();
    private List<Integer> numInputPorts = new ArrayList<Integer>();

    private List<List<Double>> delays = new ArrayList<List<Double>>();
    private double inputSetupTime;

    private int maxPortIndex;


    void addBlockType() {
        this.portNames.add(new ArrayList<String>());
        this.portTypes.add(new ArrayList<PortType>());
        this.ports.add(new HashMap<String, Integer>());
        this.numInputPorts.add(null);

        List<Integer> portStart = new ArrayList<Integer>();
        portStart.add(0);
        this.portStarts.add(portStart);
    }

    void setNumInputPorts(int blockTypeIndex, int numInputPorts) {
        int numPorts = this.ports.size();
        for(int i = 0; i <= blockTypeIndex - numPorts; i++) {
            this.addBlockType();
        }

        this.numInputPorts.set(blockTypeIndex, numInputPorts);
    }



    void addPorts(int blockTypeIndex, Map<String, Integer> portsMap) {
        List<String> typePortNames = this.portNames.get(blockTypeIndex);
        List<PortType> typePortTypes = this.portTypes.get(blockTypeIndex);
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);
        Map<String, Integer> typePorts = this.ports.get(blockTypeIndex);

        int portIndex = typePortNames.size();
        int portStart = typePortStarts.get(typePortStarts.size() - 1);

        for(Map.Entry<String, Integer> inputEntry : portsMap.entrySet()) {
            String portName = inputEntry.getKey();
            int numPins = inputEntry.getValue();

            portStart += numPins;
            typePortStarts.add(portStart);

            typePortNames.add(portName);
            typePorts.put(portName, portIndex);

            PortType portType = new PortType(blockTypeIndex, portName);
            typePortTypes.add(portType);

            portIndex++;
        }

        if(portIndex > this.maxPortIndex) {
            this.maxPortIndex = portIndex;
        }
    }



    void postProcess() {
        this.createDelayArrays();
    }

    private void createDelayArrays() {
        int maxId = this.ports.size() * this.maxPortIndex;
        for(int i = 0; i < maxId; i++) {
            this.delays.add(new ArrayList<Double>(Collections.nCopies(maxId, 0.0)));
        }
    }





    int getTypeIndex(int blockTypeIndex, String portName) {
        return this.ports.get(blockTypeIndex).get(portName);
    }


    List<PortType> getPortTypes(int blockTypeIndex) {
        return this.portTypes.get(blockTypeIndex);
    }

    int getNumPins(int blockTypeIndex) {
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);
        return typePortStarts.get(typePortStarts.size() - 1);
    }

    int[] getInputPortRange(int blockTypeIndex) {
        int typeNumInputPorts = this.numInputPorts.get(blockTypeIndex);
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);

        int start = 0;
        int end = typePortStarts.get(typeNumInputPorts);

        int[] portRange = {start, end};
        return portRange;
    }
    int[] getOutputPortRange(int blockTypeIndex) {
        int typeNumInputPorts = this.numInputPorts.get(blockTypeIndex);
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);

        int start = typePortStarts.get(typeNumInputPorts);
        int end = typePortStarts.get(typePortStarts.size() - 1);

        int[] portRange = {start, end};
        return portRange;
    }


    void setSetupTime(int blockTypeIndex, int portTypeIndex, double delay) {
        int id = this.uniqueId(blockTypeIndex, portTypeIndex);
        this.delays.get(id).set(id, delay);
    }
    double getSetupTime(int blockTypeIndex, int portTypeIndex) {
        int id = this.uniqueId(blockTypeIndex, portTypeIndex);
        return this.delays.get(id).get(id);
    }

    void setClockSetupTime(double delay) {
        this.inputSetupTime = delay;
    }
    double getClockSetupTime() {
        return this.inputSetupTime;
    }

    void setDelay(
            int sourceBlockTypeIndex, int sourcePortTypeIndex,
            int sinkBlockTypeIndex, int sinkPortTypeIndex,
            double delay) {
        int sourceId = this.uniqueId(sourceBlockTypeIndex, sourcePortTypeIndex);
        int sinkId = this.uniqueId(sinkBlockTypeIndex, sinkPortTypeIndex);

        this.delays.get(sourceId).set(sinkId, delay);
    }
    public double getDelay(
            int sourceBlockTypeIndex, int sourcePortTypeIndex,
            int sinkBlockTypeIndex, int sinkPortTypeIndex) {
        int sourceId = this.uniqueId(sourceBlockTypeIndex, sourcePortTypeIndex);
        int sinkId = this.uniqueId(sinkBlockTypeIndex, sinkPortTypeIndex);

        return this.delays.get(sourceId).get(sinkId);
    }




    String getName(int blockTypeIndex, int portTypeIndex) {
        return this.portNames.get(blockTypeIndex).get(portTypeIndex);
    }

    int[] getRange(int blockTypeIndex, int portTypeIndex) {
        List<Integer> typePortStarts = this.portStarts.get(blockTypeIndex);
        int portStart = typePortStarts.get(portTypeIndex);
        int portEnd = typePortStarts.get(portTypeIndex + 1);

        int[] portRange = {portStart, portEnd};
        return portRange;
    }

    int getNumInputPorts(int blockTypeIndex) {
        return this.numInputPorts.get(blockTypeIndex);
    }


    private int uniqueId(int blockTypeIndex, int portTypeIndex) {
        return blockTypeIndex * this.maxPortIndex + portTypeIndex;
    }
}
