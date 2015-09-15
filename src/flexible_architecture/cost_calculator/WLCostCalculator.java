package flexible_architecture.cost_calculator;

import java.util.List;

import flexible_architecture.Circuit;
import flexible_architecture.pin.GlobalPin;
import flexible_architecture.site.AbstractSite;
import flexible_architecture.site.Site;

public class WLCostCalculator extends CostCalculator {
	
	public void initialize(Circuit circuit) {
		
	}
	
	public double calculate(GlobalPin pin, Site site) {
		
		AbstractSite currentSite = pin.getOwner().getSite();
		
		return 0;
	}
	
	private List<int[]> getBoundingBox(GlobalPin pin) {
		
	}
}
