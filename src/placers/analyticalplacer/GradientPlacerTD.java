package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.TimingGraph;
import circuit.exceptions.PlacementException;

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

        this.timingGraph = this.circuit.getTimingGraph();

        this.minCost = Double.MAX_VALUE;
    }


    @Override
    public void initializeData() {
        super.initializeData();

        this.timingGraph.setCriticalityExponent(this.criticalityExponent);

        this.costCalculator = new CostCalculatorTD(
                this.circuit,
                this.blockIndexes,
                this.netBlockIndexes,
                this.netTimingEdges);

        // DEBUG
        /*int[] x = new int[this.blockIndexes.size()];
        int[] y = new int[this.blockIndexes.size()];

        this.costCalculator.calculate(x, y);
        this.timingGraph.calculateCriticalities();*/
    }

    @Override
    protected boolean isTimingDriven() {
        return true;
    }


    @Override
    protected void updateLegalIfNeeded(int iteration) {
        // TODO: this shouldn't happen every iteration, maybe near the
        // end of the algorithm we can skip some iterations?
        if(iteration % 1 == 0) {
            int[] newLegalX = this.legalizer.getLegalX();
            int[] newLegalY = this.legalizer.getLegalY();

            this.latestCost = this.costCalculator.calculate(newLegalX, newLegalY, iteration == 0);
            if(this.maxUtilization == 1 && this.latestCost < this.minCost) {
                this.minCost = this.latestCost;
                this.updateLegal(newLegalX, newLegalY);
            }
        }
    }


    @Override
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    anchor weight    max delay    time");
        this.logger.println("---------    -------------    ---------    ----");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        String iterationString = iteration + (this.latestCost == this.minCost ? "+" : " ");
        this.logger.printf("%-9s    %-13f    %-9f    %f\n", iterationString, this.anchorWeight, this.latestCost, time);
    }


    @Override
    public String getName() {
        return "Timing driven gradient placer";
    }
}
