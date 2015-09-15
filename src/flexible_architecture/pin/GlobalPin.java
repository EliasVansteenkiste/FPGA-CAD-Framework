package flexible_architecture.pin;

import flexible_architecture.architecture.PortType;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.cost_calculator.CostCalculator;
import flexible_architecture.site.Site;

public class GlobalPin extends AbstractPin {
	
	private double cost;
	private double calculatedCost;
	private Object cache;
	
	public GlobalPin(GlobalBlock owner, PortType portType, String portName, int index) {
		super(owner, portType, portName, index);
	}
	
	public GlobalBlock getOwner() {
		return (GlobalBlock) super.getOwner();
	}
	
	
	
	public double getCost() {
		return this.cost;
	}
	
	public double calculateCost(CostCalculator calculator, Site site) {
		return this.calculateCost(calculator, site, false);
	}
	public double calculateCost(CostCalculator calculator, Site site, boolean apply) {
		this.calculatedCost = calculator.calculate(this, site);
		if(apply) {
			return this.applyCost();
		} else {
			return this.calculatedCost;
		}
	}
	
	public double applyCost() {
		this.cost = this.calculatedCost;
		return this.cost;
	}
	
	
	public Object getCache() {
		return this.cache;
	}
	public void setCache(Object cache) {
		this.cache = cache;
	}
}
