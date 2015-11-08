package circuit.pin;

import circuit.architecture.PortType;
import circuit.block.AbstractBlock;
import circuit.block.IntermediateBlock;

public class LocalPin extends AbstractPin {

    public LocalPin(AbstractBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public IntermediateBlock getOwner() {
        return (IntermediateBlock) super.getOwner();
    }
}
