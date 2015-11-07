package circuit.pin;

import circuit.architecture.PortType;
import circuit.block.GlobalBlock;

public class GlobalPin extends AbstractPin {

    public GlobalPin(GlobalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }

    public GlobalBlock getOwner() {
        return (GlobalBlock) super.getOwner();
    }

    @Override
    public GlobalPin getSink(int index) {
        return (GlobalPin) super.getSink(index);
    }
}
