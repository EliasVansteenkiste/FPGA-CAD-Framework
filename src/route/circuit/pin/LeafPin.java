package route.circuit.pin;

import route.circuit.architecture.PortType;
import route.circuit.block.LeafBlock;
import route.circuit.timing.TimingNode;

public class LeafPin extends LocalPin {

    private TimingNode timingNode;

    public LeafPin(LeafBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public LeafBlock getOwner() {
        return (LeafBlock) this.owner;
    }

    public void setTimingNode(TimingNode timingNode) {
        this.timingNode = timingNode;
    }
    public TimingNode getTimingNode() {
        return this.timingNode;
    }
}
