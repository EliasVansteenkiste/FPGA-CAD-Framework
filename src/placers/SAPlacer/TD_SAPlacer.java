package placers.SAPlacer;

import interfaces.Logger;
import interfaces.Option;
import interfaces.OptionList;

import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;
import circuit.block.TimingGraph;




public class TD_SAPlacer extends SAPlacer {

    public static void initOptions(OptionList options) {
        SAPlacer.initOptions(options);

        options.add(new Option("trade off", "Trade off between wirelength and timing cost optimization: 0 is pure WLD, 1 is pure TD", new Double(0.5)));
        options.add(new Option("criticality exponent", "Exponent to calculate cost of critical connections", new Double(1)));
        options.add(new Option("recalculate", "Number of swap iterations before a recalculate of the timing graph", new Integer(50000)));
    }

    private EfficientBoundingBoxNetCC calculator;
    private final TimingGraph timingGraph;
    private double cachedBBCost, cachedTDCost, previousBBCost, previousTDCost;

    private final double tradeOffFactor;
    private final int iterationsBeforeRecalculate;

    public TD_SAPlacer(Circuit circuit, OptionList options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.calculator = new EfficientBoundingBoxNetCC(circuit);
        this.timingGraph = circuit.getTimingGraph();


        this.tradeOffFactor = this.options.getDouble("trade off");
        this.iterationsBeforeRecalculate = this.options.getInteger("iterations_before_recalculate");

        double criticalityExponent = this.options.getDouble("criticality_exponent");
        this.timingGraph.setCriticalityExponent(criticalityExponent);
    }

    @Override
    public String getName() {
        return "TD simulated annealing placer";
    }


    @Override
    protected void initializePlace() {
        this.calculator.recalculateFromScratch();
    }

    @Override
    protected void initializeSwapIteration() {
        this.timingGraph.recalculateAllSlackCriticalities();

        this.updatePreviousCosts();
    }

    private void updatePreviousCosts() {
        this.getCost();

        this.previousBBCost = this.cachedBBCost;
        this.previousTDCost = this.cachedTDCost;
    }


    @Override
    protected String getStatistics() {
        this.getCost();
        return "WL cost = " + this.cachedBBCost
                + ", T cost = " + this.cachedTDCost
                + ", delay = " + this.timingGraph.getMaxDelay();
    }


    @Override
    protected double getCost() {
        if(this.circuitChanged) {
            this.circuitChanged = false;
            this.cachedBBCost = this.calculator.calculateTotalCost();
            this.cachedTDCost = this.timingGraph.calculateTotalCost();
        }

        return this.balancedCost(this.cachedBBCost, this.cachedTDCost);
    }

    @Override
    protected double getDeltaCost(Swap swap) {
        double deltaBBCost = this.calculator.calculateDeltaCost(swap);
        double deltaTDCost = this.timingGraph.calculateDeltaCost(swap);

        return this.balancedCost(deltaBBCost, deltaTDCost);
    }

    private double balancedCost(double BBCost, double TDCost) {
        return
                this.tradeOffFactor         * TDCost / this.previousTDCost
                + (1 - this.tradeOffFactor) * BBCost / this.previousBBCost;
    }


    @Override
    protected void pushThrough(int iteration) {
        this.calculator.pushThrough();
        this.timingGraph.pushThrough();

        if(iteration % this.iterationsBeforeRecalculate == 0 && iteration > 0) {
            this.updatePreviousCosts();
        }
    }

    @Override
    protected void revert(int iteration) {
        this.calculator.revert();
        this.timingGraph.revert();

        if(iteration % this.iterationsBeforeRecalculate == 0 && iteration > 0) {
            this.updatePreviousCosts();
        }
    }
}
