package architecture.circuit.pin;

import architecture.circuit.block.GlobalBlock;
import architecture.circuit.block.PortType;

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
