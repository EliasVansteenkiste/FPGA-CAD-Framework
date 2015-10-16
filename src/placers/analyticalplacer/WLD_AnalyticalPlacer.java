package placers.analyticalplacer;

import java.util.Map;

import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;


public class WLD_AnalyticalPlacer extends AnalyticalPlacer {
	
	public WLD_AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
	}
	
	@Override
	protected CostCalculator createCostCalculator(Circuit circuit, Map<GlobalBlock, Integer> blockIndexes) {
		return new WLD_CostCalculator(circuit, blockIndexes);
	}

	@Override
	protected void initializePlacement() {
		// Do nothing
	}

	@Override
	protected void processNets(boolean firstSolve) {
		this.processNetsB2B();
	}
}
