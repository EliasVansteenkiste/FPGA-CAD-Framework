package placers.SAPlacer;

import java.util.Map;

import flexible_architecture.Circuit;

public class WLD_SAPlacer extends SAPlacer {	
	
	static {
		defaultOptions.put("T_multiplier", "20");
	}
	
	private EfficientCostCalculator calculator;
	private double cachedCost;
	
	public WLD_SAPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		this.calculator = new EfficientBoundingBoxNetCC(circuit);
	}
	
	
	protected void initializePlace() {
		this.calculator.recalculateFromScratch();
	}
	
	protected void initializeSwapIteration() {
	}
	
	protected String getStatistics() {
		return "cost = " + this.getCost();
	}
	
	protected double getCost() {
		if(this.circuitChanged) {
			this.circuitChanged = false;
			this.cachedCost = this.calculator.calculateTotalCost();
		}
		
		return this.cachedCost;
	}
	
	protected double getDeltaCost(Swap swap) {
		return this.calculator.calculateDeltaCost(swap);
	}
	
	protected void pushThrough(int iteration) {
		this.calculator.pushThrough();
	}
	
	protected void revert(int iteration) {
		this.calculator.revert();
	}
}