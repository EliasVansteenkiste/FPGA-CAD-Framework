package circuit.pin;

import circuit.architecture.PortType;
import circuit.block.GlobalBlock;

public class GlobalPin extends AbstractPin {

    public GlobalPin(GlobalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    @Override
    public GlobalBlock getOwner() {
        return (GlobalBlock) this.owner;
    }

    @Override
    public GlobalPin getSink(int index) {
        return (GlobalPin) super.getSink(index);
    }
}
