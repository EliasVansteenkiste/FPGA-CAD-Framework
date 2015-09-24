package flexible_architecture.pin;

import flexible_architecture.architecture.PortType;
import flexible_architecture.block.GlobalBlock;

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
