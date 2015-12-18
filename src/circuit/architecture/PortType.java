package circuit.architecture;

import java.io.Serializable;


public class PortType implements Serializable, Comparable<PortType> {

    private static final long serialVersionUID = 8336333602345751378L;

    private int blockTypeIndex, portTypeIndex;


    public PortType(BlockType blockType, String portName) {
        this(blockType.getTypeIndex(), portName);
    }
    PortType(int blockTypeIndex, String portName) {
        this.blockTypeIndex = blockTypeIndex;
        this.portTypeIndex = PortTypeData.getInstance().getTypeIndex(this.blockTypeIndex, portName);
    }

    BlockType getBlockType() {
        return new BlockType(this.blockTypeIndex);
    }
    int getBlockTypeIndex() {
        return this.blockTypeIndex;
    }
    int getPortTypeIndex() {
        return this.portTypeIndex;
    }

    void setSetupTime(double delay) {
        PortTypeData.getInstance().setSetupTime(this.blockTypeIndex, this.portTypeIndex, delay);
    }
    public double getSetupTime() {
        return PortTypeData.getInstance().getSetupTime(this.blockTypeIndex, this.portTypeIndex);
    }

    public static double getClockSetupTime() {
        return PortTypeData.getInstance().getClockSetupTime();
    }

    void setDelay(PortType sinkType, double delay) {
        PortTypeData.getInstance().setDelay(
                this.blockTypeIndex, this.portTypeIndex,
                sinkType.blockTypeIndex, sinkType.portTypeIndex,
                delay);
    }
    public double getDelay(PortType sinkType) {
        return PortTypeData.getInstance().getDelay(
                this.blockTypeIndex, this.portTypeIndex,
                sinkType.blockTypeIndex, sinkType.portTypeIndex);
    }


    public String getName() {
        return PortTypeData.getInstance().getName(this.blockTypeIndex, this.portTypeIndex);
    }

    public int[] getRange() {
        return PortTypeData.getInstance().getRange(this.blockTypeIndex, this.portTypeIndex);
    }


    public boolean isOutput() {
        return !this.isInput();
    }
    public boolean isInput() {
        return this.portTypeIndex < PortTypeData.getInstance().getNumInputPorts(this.blockTypeIndex);
    }


    private int uniqueId() {
        return PortTypeData.getInstance().uniqueId(this.blockTypeIndex, this.portTypeIndex);
    }

    @Override
    public int compareTo(PortType otherPortType) {
        return Integer.compare(this.uniqueId(), otherPortType.uniqueId());
    }

    @Override
    public String toString() {
        return String.format("%s.%s", new BlockType(this.blockTypeIndex).toString(), this.getName());
    }
}
