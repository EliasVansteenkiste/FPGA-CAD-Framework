package placers.analyticalplacer;

import interfaces.Logger;

import java.util.Map;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class WLD_AnalyticalPlacer extends AnalyticalPlacer {

    private static final String name = "TD Analytical Placer";

    public WLD_AnalyticalPlacer(Logger logger, PlacementVisualizer visualizer, Circuit circuit, Map<String, String> options) {
        super(logger, visualizer, circuit, options);
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

    @Override
    public String getName() {
        return WLD_AnalyticalPlacer.name;
    }
}
