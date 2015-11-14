package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class WLD_AnalyticalPlacer extends AnalyticalPlacer {

    public static void initOptions(Options options) {
        AnalyticalPlacer.initOptions(options);
    }

    @Override
    public String getName() {
        return "WLD Analytical Placer";
    }

    public WLD_AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
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
