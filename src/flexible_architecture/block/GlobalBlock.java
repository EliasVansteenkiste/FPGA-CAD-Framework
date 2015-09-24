package flexible_architecture.block;

import util.Logger;
import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.PortType;
import flexible_architecture.pin.GlobalPin;
import flexible_architecture.site.AbstractSite;

public class GlobalBlock extends AbstractBlock {
	
	private AbstractSite site;
	
	public GlobalBlock(String name, BlockType type, int index) {
		super(name, type, index);
	}
	
	public AbstractSite getSite() {
		return this.site;
	}
	
	public void removeSite() {
		if(this.site != null) {
			this.site.removeBlock(this);
			this.site = null;
		} else {
			Logger.raise("Trying to remove the site of an unplaced block");
		}
	}
	public void setSite(AbstractSite site) {
		if(this.site == null) {
			this.site = site;
			this.site.addBlock(this);
		} else {
			Logger.raise("Trying to set the site of a placed block");
		}
	}
	
	@Override
	public AbstractBlock getParent() {
		return null;
	}
	
	@Override
	public GlobalPin createPin(PortType portType, int index) {
		return new GlobalPin(this, portType, index);
	}
}
