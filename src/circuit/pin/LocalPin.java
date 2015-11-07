package circuit.pin;

import circuit.architecture.PortType;
import circuit.block.AbstractBlock;
import circuit.block.LocalBlock;

public class LocalPin extends AbstractPin {

    public LocalPin(AbstractBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    public LocalBlock getOwner() {
        return (LocalBlock) super.getOwner();
    }
}
