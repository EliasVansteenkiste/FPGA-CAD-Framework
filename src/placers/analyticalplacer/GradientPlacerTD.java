package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.TimingGraph;

public class GradientPlacerTD extends GradientPlacer {

    public static void initOptions(Options options) {
        GradientPlacer.initOptions(options);

        options.add("criticality exponent", "criticality exponent of connections", new Double(3));
        options.add("criticality threshold", "minimal criticality for adding extra constraints", new Double(0.5));
    }

    private double criticalityExponent;
    private TimingGraph timingGraph;
    private CostCalculatorTD costCalculator;
    private double latestCost, minCost;

    public GradientPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble("criticality exponent");
        this.criticalityThreshold = options.getDouble("criticality threshold");

        this.timingGraph = this.circuit.getTimingGraph();

        this.minCost = Double.MAX_VALUE;
    }


    @Override
    public void initializeData() {
        super.initializeData();

        this.timingGraph.reset();
        this.timingGraph.setCriticalityExponent(this.criticalityExponent);
        this.timingGraph.recalculateAllSlacksCriticalities(false);

        this.costCalculator = new CostCalculatorTD(this.circuit, this.blockIndexes, this.timingNets);
    }

    @Override
    protected boolean isTimingDriven() {
        return true;
    }


    @Override
    protected void updateLegal() {
        this.latestCost = this.costCalculator.calculate(this.legalizer.getLegalX(), this.legalizer.getLegalY());
        if(this.maxUtilization == 1 && this.latestCost < this.minCost) {
            this.minCost = this.latestCost;
            super.updateLegal();
        }
    }


    @Override
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    max delay    anchor weight    time");
        this.logger.println("---------    ---------    -------------    ----");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.logger.printf("%-9d    %-13f    %-9f    %f\n", iteration, this.anchorWeight, this.latestCost, time);
    }


    @Override
    public String getName() {
        return "Timing driven gradient placer";
    }
}
