package placers.analyticalplacer;

import java.util.Map;

import timing_graph.TimingGraph;

import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;


public class TD_CostCalculator extends CostCalculator {
	
	private WLD_CostCalculator wldCostCalculator;
	private TimingGraph timingGraph;
	private double tradeOff;
	private double initialBBCost, initialTDCost;
	
	TD_CostCalculator(Circuit circuit, Map<GlobalBlock, Integer> blockIndexes, TimingGraph timingGraph, double tradeOff) {
		this.wldCostCalculator = new WLD_CostCalculator(circuit, blockIndexes);
		this.timingGraph = timingGraph;
		this.tradeOff = tradeOff;
	}
	
	@Override
	boolean requiresCircuitUpdate() {
		return true;
	}
	
	@Override
	protected double calculate() {
		
		this.timingGraph.recalculateAllSlackCriticalities();
		double TDCost = this.timingGraph.calculateTotalCost();
		
		double BBCost;
		if(this.ints) {
			BBCost = this.wldCostCalculator.calculate(this.intX, this.intY);
		} else {
			BBCost = this.wldCostCalculator.calculate(this.doubleX, this.doubleY);
		}
		
		if(this.initialBBCost == 0) {
			this.initialBBCost = BBCost;
			this.initialTDCost = TDCost;
			return Double.MAX_VALUE / 10;
		
		} else {
			return 
					this.tradeOff         * TDCost / this.initialTDCost
					+ (1 - this.tradeOff) * BBCost / this.initialBBCost;
		}
	}
}
