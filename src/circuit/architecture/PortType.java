package circuit.architecture;

import java.io.Serializable;


public class PortType implements Serializable {

    private static final long serialVersionUID = 8336333602345751378L;

    private int blockTypeIndex, portTypeIndex;


    public PortType(String blockTypeName, String portName) {
        this(new BlockType(blockTypeName), portName);
    }
    public PortType(BlockType blockType, String portName) {
        this(blockType.getIndex(), portName);
    }
    PortType(int blockTypeIndex, String portName) {
        this.blockTypeIndex = blockTypeIndex;
        this.portTypeIndex = PortTypeData.getInstance().getTypeIndex(this.blockTypeIndex, portName);
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
}
