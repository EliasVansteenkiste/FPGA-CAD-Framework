package architecture.circuit.pin;

import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.LocalBlock;
import architecture.circuit.block.PortType;

public class LocalPin extends AbstractPin {
    
    public LocalPin(AbstractBlock owner, PortType portType, int index) {
        super(owner, portType, index);
    }
    
    public LocalBlock getOwner() {
        return (LocalBlock) super.getOwner();
    }
}
