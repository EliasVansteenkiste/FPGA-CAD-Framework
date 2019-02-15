package route.circuit.architecture;

public class DelayElement {
    public final PortType sourcePortType;
    public final PortType sinkPortType;
    public final int sourcePortTypeIndex;
    public final int sinkPortTypeIndex;
    public final int sourceBlockIndex;
    public final int sinkBlockIndex;
    public final int sourcePortIndex;
    public final int sinkPortIndex;
    public final float delay;
    
    public DelayElement(int sourceBlockIndex, PortType sourcePortType, int sourcePortIndex, int sinkBlockIndex, PortType sinkPortType, int sinkPortIndex, float delay) {
        this.sourcePortType = sourcePortType;
        this.sinkPortType = sinkPortType;
        this.sourcePortTypeIndex = this.sourcePortType.getPortTypeIndex();
        this.sinkPortTypeIndex = this.sinkPortType.getPortTypeIndex();
        this.sourceBlockIndex = sourceBlockIndex;
        this.sinkBlockIndex = sinkBlockIndex;
        this.sourcePortIndex = sourcePortIndex;
        this.sinkPortIndex = sinkPortIndex;
        this.delay = delay;
    }
}
