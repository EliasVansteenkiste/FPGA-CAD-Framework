package circuit.block;

import circuit.architecture.BlockType;
import circuit.architecture.PortType;
import circuit.pin.LocalPin;


public class IntermediateBlock extends AbstractBlock {

    private AbstractBlock parent;

    public IntermediateBlock(String name, BlockType type, int index, AbstractBlock parent) {
        super(name, type, index);

        this.parent = parent;
        this.parent.setChild(this, index);
    }

    @Override
    public AbstractBlock getParent() {
        return this.parent;
    }

    @Override
    protected LocalPin createPin(PortType portType, int index) {
        return new LocalPin(this, portType, index);
    }
}
