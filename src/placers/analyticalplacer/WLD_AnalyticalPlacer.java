package placers.analyticalplacer;

import java.util.Map;

import flexible_architecture.Circuit;
import flexible_architecture.architecture.BlockType;

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
	protected void addExtraConnections(BlockType blockType, int startIndex, boolean firstSolve) {
		//Do nothing
	}
	
	
}
