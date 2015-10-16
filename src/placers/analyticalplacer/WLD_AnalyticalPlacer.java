package placers.analyticalplacer;

import java.util.Map;

import architecture.BlockType;
import architecture.circuit.Circuit;


public class WLD_AnalyticalPlacer extends AnalyticalPlacer {
	
	public WLD_AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
	}
	
	@Override
	protected CostCalculator createCostCalculator() {
		return new WLD_CostCalculator(this.circuit, this.blockIndexes);
	}

	@Override
	protected void initializePlacement() {
		// Do nothing
	}

	@Override
	protected void processNets(BlockType blockType, int startIndex, boolean firstSolve) {
		this.processNetsB2B(blockType, startIndex);
	}
}
