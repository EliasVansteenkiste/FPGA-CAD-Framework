package placers.analyticalplacer;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.TimingGraph;
import interfaces.Logger;
import interfaces.Options;

public class AnalyticalPlacerTD extends AnalyticalPlacer {

    public static void initOptions(Options options) {
        AnalyticalPlacer.initOptions(options);

        options.add("criticality exponent", "criticality exponent of connections", new Double(1));
        options.add("criticality threshold", "minimal criticality for adding extra constraints", new Double(0.8));
    }


    private double criticalityExponent;
    private TimingGraph timingGraph;

    public AnalyticalPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble("criticality exponent");
        this.criticalityThreshold = options.getDouble("criticality threshold");

        this.timingGraph = this.circuit.getTimingGraph();
        this.timingGraph.setCriticalityExponent(this.criticalityExponent);
        this.timingGraph.recalculateAllSlacksCriticalities(true);
    }


    @Override
    protected CostCalculator createCostCalculator() {
        return new CostCalculatorTD(this.circuit, this.blockIndexes, this.timingNets);
    }


    @Override
    protected boolean isTimingDriven() {
        return true;
    }

    @Override
    public String getName() {
        return "Timing driven analytical placer";
    }
}
