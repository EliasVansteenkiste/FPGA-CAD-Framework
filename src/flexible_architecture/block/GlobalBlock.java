package flexible_architecture.block;

import java.util.Map;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.PortType;
import flexible_architecture.cost_calculator.CostCalculator;
import flexible_architecture.pin.AbstractPin;
import flexible_architecture.pin.GlobalPin;
import flexible_architecture.site.AbstractSite;
import flexible_architecture.site.Site;

public class GlobalBlock extends AbstractBlock {
	
	private AbstractSite site;
	
	public GlobalBlock(String name, BlockType type) {
		super(name, type);
	}
	
	public AbstractSite getSite() {
		return this.site;
	}
	public void setSite(AbstractSite site) {
		this.site = site;
	}
	
	@Override
	public AbstractBlock getParent() {
		return null;
	}
	
	@Override
	public GlobalPin createPin(PortType portType, String portName, int index) {
		return new GlobalPin(this, portType, portName, index);
	}
}
