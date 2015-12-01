package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class AnalyticalPlacerWLD extends AnalyticalPlacer {

    public AnalyticalPlacerWLD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    protected boolean isTimingDriven() {
        return false;
    }

    @Override
    public String getName() {
        return "Wirelength driven analytical placer";
    }

    @Override
    protected CostCalculator createCostCalculator() {
        return new CostCalculatorWLD(this.netUniqueBlockIndexes);
    }
}
