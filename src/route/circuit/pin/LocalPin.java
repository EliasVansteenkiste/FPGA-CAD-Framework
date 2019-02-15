package route.circuit.pin;

import route.circuit.architecture.PortType;
import route.circuit.block.LocalBlock;

public class LocalPin extends AbstractPin {

    public LocalPin(LocalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public LocalBlock getOwner() {
        return (LocalBlock) this.owner;
    }
}
