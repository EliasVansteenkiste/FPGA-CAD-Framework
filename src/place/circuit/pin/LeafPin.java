package place.circuit.pin;

import place.circuit.architecture.PortType;
import place.circuit.block.LeafBlock;
import place.circuit.timing.TimingNode;

public class LeafPin extends LocalPin {

    public LeafPin(LeafBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public LeafBlock getOwner() {
        return (LeafBlock) this.owner;
    }
}
