package place.circuit.block;

import place.circuit.architecture.BlockType;
import place.circuit.architecture.PortType;
import place.circuit.pin.LeafPin;

public class LeafBlock extends LocalBlock {

    private GlobalBlock globalParent;


    public LeafBlock(String name, BlockType type, int index, AbstractBlock parent, GlobalBlock globalParent) {
        super(name, type, index, parent);

        this.globalParent = globalParent;
    }

    @Override
    protected LeafPin createPin(PortType portType, int index) {
        return new LeafPin(this, portType, index);
    }

    public GlobalBlock getGlobalParent() {
        return this.globalParent;
    }
}
