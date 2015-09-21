package flexible_architecture.block;

import flexible_architecture.architecture.BlockType;

import flexible_architecture.architecture.PortType;
import flexible_architecture.pin.LocalPin;

public class LocalBlock extends AbstractBlock {
	
	
	private AbstractBlock parent;
	
	public LocalBlock(String name, BlockType type, int index, AbstractBlock parent) {
		super(name, type, index);
		
		this.parent = parent;
	}
	
	@Override
	public AbstractBlock getParent() {
		return this.parent;
	}
	
	@Override
	public LocalPin createPin(PortType portType, String portName, int index) {
		return new LocalPin(this, portType, portName, index);
	}
}
