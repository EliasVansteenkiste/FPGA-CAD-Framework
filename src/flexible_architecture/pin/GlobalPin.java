package flexible_architecture.pin;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import flexible_architecture.architecture.PortType;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.cost_calculator.CostCalculator;
import flexible_architecture.site.Site;

public class GlobalPin extends AbstractPin {
	
	public GlobalPin(GlobalBlock owner, PortType portType, String portName, int index) {
		super(owner, portType, portName, index);
	}
	
	public GlobalBlock getOwner() {
		return (GlobalBlock) super.getOwner();
	}
	
	@Override
	public GlobalPin getSink(int index) {
		return (GlobalPin) super.getSink(index);
	}
}
