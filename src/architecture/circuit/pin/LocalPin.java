package architecture.circuit.pin;

import architecture.PortType;
import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.LocalBlock;

public class LocalPin extends AbstractPin {
	
	public LocalPin(AbstractBlock owner, PortType portType, int index) {
		super(owner, portType, index);
	}
	
	public LocalBlock getOwner() {
		return (LocalBlock) super.getOwner();
	}
}
