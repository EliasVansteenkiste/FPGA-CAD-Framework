package route.circuit.pin;

import route.circuit.architecture.PortType;
import route.circuit.block.LeafBlock;

public class LeafPin extends LocalPin {

    public LeafPin(LeafBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public LeafBlock getOwner() {
        return (LeafBlock) this.owner;
    }
}
