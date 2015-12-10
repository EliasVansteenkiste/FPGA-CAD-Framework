package placers.SAPlacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;
import circuit.block.TimingGraph;




public class SAPlacerTD extends SAPlacer {

    public static void initOptions(Options options) {
        SAPlacer.initOptions(options);

        options.add("trade off", "trade off between wirelength and timing cost optimization: 0 is pure WLD, 1 is pure TD", new Double(0.5));
        options.add("criticality exponent start", "exponent to calculate criticality of connections at start of anneal", new Double(1));
        options.add("criticality exponent end", "exponent to calculate criticality of connections at end of anneal", new Double(8));
        options.add("recalculate", "number of swap iterations before a recalculate of the timing graph", new Integer(50000));
    }

    private EfficientBoundingBoxNetCC calculator;
    private final TimingGraph timingGraph;
    private final double criticalityExponentStart, criticalityExponentEnd;
    private double cachedBBCost, cachedTDCost, previousBBCost, previousTDCost;

    private final double tradeOffFactor;
    private final int iterationsBeforeRecalculate;

    public SAPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.calculator = new EfficientBoundingBoxNetCC(circuit);
        this.timingGraph = circuit.getTimingGraph();


        this.tradeOffFactor = this.options.getDouble("trade off");
        this.iterationsBeforeRecalculate = this.options.getInteger("recalculate");

        this.criticalityExponentStart = this.options.getDouble("criticality exponent start");
        this.criticalityExponentEnd = this.options.getDouble("criticality exponent end");
    }

    @Override
    public void initializeData() {
        super.initializeData();
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
        double criticalityExponent = this.criticalityExponentStart +
                (1 - (this.Rlimd - 1) / (this.initialRlim - 1))
                * (this.criticalityExponentEnd - this.criticalityExponentStart);

        this.timingGraph.setCriticalityExponent(criticalityExponent);
        this.timingGraph.recalculateAllSlacksCriticalities(true);

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
            this.timingGraph.recalculateAllSlacksCriticalities(false);
            this.updatePreviousCosts();
        }
    }

    @Override
    protected void revert(int iteration) {
        this.calculator.revert();
        this.timingGraph.revert();

        if(iteration % this.iterationsBeforeRecalculate == 0 && iteration > 0) {
            this.timingGraph.recalculateAllSlacksCriticalities(false);
            this.updatePreviousCosts();
        }
    }
}
