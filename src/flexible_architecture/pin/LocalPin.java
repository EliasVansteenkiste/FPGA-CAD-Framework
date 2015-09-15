package flexible_architecture.pin;

import flexible_architecture.architecture.PortType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.LocalBlock;

public class LocalPin extends AbstractPin {
	
	public LocalPin(AbstractBlock owner, PortType portType, String portName, int index) {
		super(owner, portType, portName, index);
	}
	
	public LocalBlock getOwner() {
		return (LocalBlock) super.getOwner();
	}
}
