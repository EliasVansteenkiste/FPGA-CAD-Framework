package placers.SAPlacer;

import java.util.Map;

import timinganalysis.TimingGraph;
import architecture.Architecture;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public class TD_SAPlacer extends SAPlacer {
	
	
	static {
		//Timing trade off factor: 0.0 = wire-length-driven
		defaultOptions.put("trade_off_factor", "0.5");
		
		//Starting criticality exponent
		defaultOptions.put("criticality_exponent", "8");
		
		// Number of iterations before recalculating timing information
		defaultOptions.put("iterations_before_recalculate", "50000");
	}
	
	private EfficientCostCalculator calculator;
	private TimingGraph timingGraph;
	private double cachedBBCost, cachedTDCost, previousBBCost, previousTDCost;
	
	private double tradeOffFactor;
	private int iterationsBeforeRecalculate;
	
	public TD_SAPlacer(Architecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit, Map<String, String> options) {
		super(architecture, circuit, options);
		
		this.calculator = new EfficientBoundingBoxNetCC(circuit);
		this.timingGraph = new TimingGraph(prePackedCircuit);
		this.timingGraph.buildTimingGraph();
		
		
		this.tradeOffFactor = Double.parseDouble(this.options.get("trade_off_factor"));
		this.iterationsBeforeRecalculate = Integer.parseInt(this.options.get("iterations_before_recalculate"));
		
		double criticalityExponent = Double.parseDouble(this.options.get("criticality_exponent"));
		this.timingGraph.setCriticalityExponent(criticalityExponent);
	}
	
	
	
	protected void initializePlace() {
		this.calculator.recalculateFromScratch();
	}
	
	protected void initializeSwapIteration() {
		this.timingGraph.recalculateAllSlacksCriticalities();
		
		this.updatePreviousCosts();
	}
	
	private void updatePreviousCosts() {
		this.getCost();
		
		this.previousBBCost = this.cachedBBCost;
		this.previousTDCost = this.cachedTDCost;
	}
	
	
	
	protected String getStatistics() {
		this.getCost();
		return "WL cost = " + this.cachedBBCost
				+ ", T cost = " + this.cachedTDCost
				+ ", delay = " + this.timingGraph.calculateMaximalDelay();
	}
	
	
		
	protected double getCost() {
		if(this.circuitChanged) {
			this.circuitChanged = false;
			this.cachedBBCost = this.calculator.calculateTotalCost();
			this.cachedTDCost = this.timingGraph.calculateTotalCost();
		}
		
		//TODO: is this ok?
		return this.balancedCost(this.cachedBBCost, this.cachedTDCost);
	}
	
	protected double getDeltaCost(Swap swap) {
		double deltaBBCost = this.calculator.calculateDeltaCost(swap);
		double deltaTDCost = this.timingGraph.calculateDeltaCost(swap);
		
		return this.balancedCost(deltaBBCost, deltaTDCost);
	}
	
	private double balancedCost(double BBCost, double TDCost) {
		return
				this.tradeOffFactor         * TDCost / this.previousTDCost
				+ (1 - this.tradeOffFactor) * BBCost / this.previousBBCost;
	}
	
	
	
	protected void pushThrough(int iteration) {
		this.calculator.pushThrough();
		this.timingGraph.pushThrough();
		
		if(iteration % this.iterationsBeforeRecalculate == 0 && iteration > 0) {
			this.updatePreviousCosts();
		}
	}
	
	protected void revert(int iteration) {
		this.calculator.revert();
		this.timingGraph.revert();
		
		if(iteration % this.iterationsBeforeRecalculate == 0) {
			this.updatePreviousCosts();
		}
	}
}
