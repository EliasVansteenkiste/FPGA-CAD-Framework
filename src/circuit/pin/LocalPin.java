package circuit.pin;

import circuit.architecture.PortType;
import circuit.block.LocalBlock;

public class LocalPin extends AbstractPin {

    public LocalPin(LocalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public LocalBlock getOwner() {
        return (LocalBlock) this.owner;
    }
}
