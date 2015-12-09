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
        options.add("recalculate criticalities", "period of the iterations in which the criticalities are recalculated", new Integer(1));
    }

    private double criticalityExponent;
    private TimingGraph timingGraph;
    private CostCalculatorTD costCalculator;
    private double latestCost, minCost;
    private int recalculateCriticalitiesPeriod;

    public GradientPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble("criticality exponent");
        this.recalculateCriticalitiesPeriod = options.getInteger("recalculate criticalities");

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
    }

    @Override
    protected boolean isTimingDriven() {
        return true;
    }


    @Override
    protected void updateLegalIfNeeded(int iteration) {
        // TODO: this shouldn't happen every iteration, maybe near the
        // end of the algorithm we can skip some iterations?
        int[] newLegalX = this.legalizer.getLegalX();
        int[] newLegalY = this.legalizer.getLegalY();

        //boolean recalculate = (iteration < 20 && iteration % this.recalculateCriticalitiesPeriod == 0) || (iteration >= 20 && iteration % (this.recalculateCriticalitiesPeriod + 1) == 0);
        int mod = iteration / 20 + 1;
        boolean recalculate = (iteration % mod == 0);
        this.latestCost = this.costCalculator.calculate(newLegalX, newLegalY, recalculate);

        if(this.maxUtilization == 1 && this.latestCost < this.minCost) {
            this.minCost = this.latestCost;
            this.updateLegal(newLegalX, newLegalY);
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
