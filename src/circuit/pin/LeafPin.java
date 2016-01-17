package circuit.pin;

import circuit.architecture.PortType;
import circuit.block.LeafBlock;
import circuit.block.TimingNode;

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
