package place.circuit.block;

import place.circuit.architecture.BlockType;
import place.circuit.architecture.PortType;
import place.circuit.pin.LocalPin;


public class LocalBlock extends AbstractBlock {

    private AbstractBlock parent;

    public LocalBlock(String name, BlockType type, int index, AbstractBlock parent) {
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
