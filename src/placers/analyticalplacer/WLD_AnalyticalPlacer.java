package placers.analyticalplacer;

import java.util.Map;

import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;


public class WLD_AnalyticalPlacer extends AnalyticalPlacer {
	
	public WLD_AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
	}
    
    @Override
    protected void createCostCalculator(Map<GlobalBlock, Integer> blockIndexes) {
        this.costCalculator = new WLD_CostCalculator(this.circuit, blockIndexes);
    }

	@Override
	protected void initializePlacement() {
		// Do nothing
	}
}
