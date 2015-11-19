package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class WLD_AnalyticalPlacer extends AnalyticalPlacer {

    public WLD_AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    protected CostCalculator createCostCalculator() {
        return new WLD_CostCalculator(this.nets);
    }

    @Override
    public String getName() {
        return "Wirelength driven analytical placer";
    }
}
