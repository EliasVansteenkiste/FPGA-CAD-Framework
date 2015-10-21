package placers.analyticalplacer;

import java.util.Map;

import architecture.circuit.Circuit;


public class WLD_AnalyticalPlacer extends AnalyticalPlacer {
	
	public WLD_AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
	}
    
    @Override
    public void initializeData() {
        super.initializeData();
    }
    
    @Override
    public CostCalculator createCostCalculator() {
        return new WLD_CostCalculator(this.nets);
    }
    
    @Override
    protected void initializePlacementIteration() {
        // Do nothing
    }
}
