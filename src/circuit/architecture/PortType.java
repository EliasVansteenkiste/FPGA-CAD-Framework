package circuit.architecture;

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

    void setDelay(PortType sinkType, double delay) {
        PortTypeData.getInstance().setDelay(this.typeIndex, sinkType.typeIndex, delay);
    }
    public double getDelay(PortType sinkType) {
        return PortTypeData.getInstance().getDelay(this.typeIndex, sinkType.typeIndex);
    }


    public String getName() {
        return PortTypeData.getInstance().getName(this.typeIndex);
    }

    public int[] getRange() {
        return PortTypeData.getInstance().getRange(this.typeIndex);
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



    @Override
    public int compareTo(PortType otherPortType) {
        return Integer.compare(this.typeIndex, otherPortType.typeIndex);
    }

    @Override
    public String toString() {
        return String.format("%s.%s", new BlockType(this.getBlockTypeIndex()).toString(), this.getName());
    }
}
