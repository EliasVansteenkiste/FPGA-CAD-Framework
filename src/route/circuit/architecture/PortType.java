package route.circuit.architecture;

import java.io.Serializable;


public class PortType implements Serializable, Comparable<PortType> {

    private static final long serialVersionUID = 8336333602345751378L;

    private int typeIndex;


    public PortType(BlockType blockType, String portName) {
        this(blockType.getTypeIndex(), portName);
    }
    PortType(int blockTypeIndex, String portName) {
        this.typeIndex = PortTypeData.getInstance().getTypeIndex(blockTypeIndex, portName);
    }

    int getBlockTypeIndex() {
        return PortTypeData.getInstance().getBlockTypeIndex(this.typeIndex);
    }
    int getPortTypeIndex() {
        return this.typeIndex;
    }

    void setSetupTime(double delay) {
        PortTypeData.getInstance().setSetupTime(this.typeIndex, delay);
    }
    public double getSetupTime() {
        return PortTypeData.getInstance().getSetupTime(this.typeIndex);
    }

    public void setDelay(DelayElement delayElement) {
        PortTypeData.getInstance().setDelay(delayElement);
    }
    public double getDelay(PortType sinkType, int sourceBlockIndex, int sinkBlockIndex, int sourcePortIndex, int sinkPortIndex) {
        return PortTypeData.getInstance().getDelay(sourceBlockIndex, this.typeIndex, sourcePortIndex, sinkBlockIndex, sinkType.typeIndex, sinkPortIndex);
    }


    public String getName() {
        return PortTypeData.getInstance().getName(this.typeIndex);
    }

    public int[] getRange() {
        return PortTypeData.getInstance().getRange(this.typeIndex);
    }
    public int numPins(){
    	int[] range = this.getRange();
    	return range[1] - range[0];
    }

    public boolean isInput() {
        return PortTypeData.getInstance().isInput(this.typeIndex);
    }
    public boolean isOutput() {
        return PortTypeData.getInstance().isOutput(this.typeIndex);
    }
    public boolean isClock() {
        return PortTypeData.getInstance().isClock(this.typeIndex);
    }
    
    public boolean isEquivalent() {
    	return PortTypeData.getInstance().isEquivalent(this.typeIndex);
    }

    @Override
    public int compareTo(PortType otherPortType) {
        return Integer.compare(this.typeIndex, otherPortType.typeIndex);
    }

    @Override
    public String toString() {
        return String.format("%s.%s", new BlockType(this.getBlockTypeIndex()).toString(), this.getName());
    }
}
