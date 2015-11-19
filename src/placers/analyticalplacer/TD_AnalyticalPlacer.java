package placers.analyticalplacer;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.TimingGraph;
import interfaces.Logger;
import interfaces.Options;

public class TD_AnalyticalPlacer extends AnalyticalPlacer {

    public static void initOptions(Options options) {
        AnalyticalPlacer.initOptions(options);

        options.add("trade off", "trade of between wirelength and timing cost (0 = wirelength driven)", new Double(0.5));
        options.add("criticality exponent", "criticality exponent of connections", new Double(1));
        options.add("criticality threshold", "minimal criticality for adding extra constraints", new Double(0.8));
    }


    private double tradeOff, criticalityExponent, criticalityThreshold;

    public TD_AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.tradeOff = options.getDouble("trade off");
        this.criticalityExponent = options.getDouble("criticality exponent");
        this.criticalityThreshold = options.getDouble("criticality threshold");
    }

    @Override
    protected CostCalculator createCostCalculator() {
        TimingGraph timingGraph = this.circuit.getTimingGraph();
        timingGraph.setCriticalityExponent(this.criticalityExponent);

        return new TD_CostCalculator(this.nets, timingGraph, this.tradeOff);
    }

    @Override
    public String getName() {
        return "Timing driven analytical placer";
    }
}
