package flexible_architecture.cost_calculator;

import flexible_architecture.pin.GlobalPin;
import flexible_architecture.site.Site;

public abstract class CostCalculator {
	
	public CostCalculator() {
	}
	
	public abstract double calculate(GlobalPin pin, Site site);
}
