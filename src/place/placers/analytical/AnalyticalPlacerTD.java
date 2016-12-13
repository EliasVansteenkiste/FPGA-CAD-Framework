package place.placers.analytical;

import java.util.Random;

import place.circuit.Circuit;
import place.circuit.timing.TimingGraph;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

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
                new Double(3));
    }


    private double criticalityExponent;
    private TimingGraph timingGraph;

    private double legalCostTD = Double.MAX_VALUE;
    private CostCalculatorTD costCalculatorTD;

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
        this.costCalculatorTD = new CostCalculatorTD(this.circuit, this.netBlocks, this.timingNets);
        return new CostCalculatorWLD(this.nets);
    }


    @Override
    protected boolean isTimingDriven() {
        return true;
    }

    @Override
    public String getName() {
        return "Timing driven analytical placer";
    }


    @Override
    protected void updateLegalIfNeeded(int[] x, int[] y) {
        this.startTimer(T_CALCULATE_COST);
        double costBB = this.costCalculator.calculate(x, y);
        double costTD = this.costCalculatorTD.calculate(x, y);
        this.stopTimer(T_CALCULATE_COST);

        this.startTimer(T_UPDATE_CIRCUIT);
        if(costTD < this.legalCostTD) {
            this.legalCost = costBB;
            this.legalCostTD = costTD;
            this.updateLegal(x, y);
        }

        this.stopTimer(T_UPDATE_CIRCUIT);
    }
}
