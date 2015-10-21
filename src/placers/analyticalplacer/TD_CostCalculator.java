package placers.analyticalplacer;

import java.util.Map;

import timing_graph.TimingGraph;

import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;
import java.util.List;


public class TD_CostCalculator extends WLD_CostCalculator {
	
	private TimingGraph timingGraph;
	private double tradeOff;
	private double initialBBCost, initialTDCost;
	
	TD_CostCalculator(List<int[]> nets, TimingGraph timingGraph, double tradeOff) {
		super(nets);
        
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
		
		double BBCost = super.calculate();
		
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
