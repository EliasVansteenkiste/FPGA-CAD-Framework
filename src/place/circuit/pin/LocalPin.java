package place.circuit.pin;

import place.circuit.architecture.PortType;
import place.circuit.block.LocalBlock;

public class LocalPin extends AbstractPin {

    public LocalPin(LocalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public LocalBlock getOwner() {
        return (LocalBlock) this.owner;
    }
}
