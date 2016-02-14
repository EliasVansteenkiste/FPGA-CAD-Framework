package placers.analytical;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.timing.TimingGraph;
import interfaces.Logger;
import interfaces.Options;

public class AnalyticalPlacerTD extends AnalyticalPlacer {

    private static final String
        O_CRITICALITY_EXPONENT = "criticality exponent",
        O_CRITICALITY_THRESHOLD = "criticality threshold",
        O_TRADE_OFF = "trade off";

    public static void initOptions(Options options) {
        AnalyticalPlacer.initOptions(options);

        options.add(
                O_CRITICALITY_EXPONENT,
                "criticality exponent of connections",
                new Double(1));

        options.add(
                O_CRITICALITY_THRESHOLD,
                "minimal criticality for adding TD constraints",
                new Double(0.8));

        options.add(
                O_TRADE_OFF,
                "trade_off between timing driven and wirelength driven (0 = not timing driven)",
                new Double(2));
    }


    private double criticalityExponent;
    private TimingGraph timingGraph;

    public AnalyticalPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble(O_CRITICALITY_EXPONENT);
        this.criticalityThreshold = options.getDouble(O_CRITICALITY_THRESHOLD);
        this.tradeOff = options.getDouble(O_TRADE_OFF);

        this.timingGraph = this.circuit.getTimingGraph();
        this.timingGraph.setCriticalityExponent(this.criticalityExponent);
        this.timingGraph.calculateCriticalities(true);
    }


    @Override
    protected CostCalculator createCostCalculator() {
        return new CostCalculatorTD(this.circuit, this.netBlocks, this.timingNets);
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
