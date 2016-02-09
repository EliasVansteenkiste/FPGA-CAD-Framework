package placers.analytical;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.timing.TimingGraph;

public class GradientPlacerTD extends GradientPlacer {

    public static void initOptions(Options options) {
        GradientPlacer.initOptions(options);

        options.add("criticality exponent", "criticality exponent of connections", new Double(3));
        options.add("recalculate criticalities", "frequency of criticalities recalculation; 0 = never, 1 = every iteration", new Double(0.5));
        options.add("recalculate priority", "controls the spreading of recalculations; 1 = evenly spread, higher = less recalculations near the end", new Double(1));
    }

    private double criticalityExponent;
    private TimingGraph timingGraph;
    private CostCalculatorTD costCalculator;
    private double latestCost, minCost;

    private double recalculateCriticalities, recalculatePriority;
    private boolean[] recalculate;

    public GradientPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble("criticality exponent");
        this.recalculateCriticalities = options.getDouble("recalculate criticalities");
        this.recalculatePriority = options.getDouble("recalculate priority");

        this.timingGraph = this.circuit.getTimingGraph();

        this.minCost = Double.MAX_VALUE;
    }


    @Override
    public void initializeData() {
        super.initializeData();

        this.timingGraph.setCriticalityExponent(this.criticalityExponent);
        this.timingGraph.calculateCriticalities(true);

        this.costCalculator = new CostCalculatorTD(
                this.circuit,
                this.netBlocks,
                this.timingNets);


        this.recalculate = new boolean[this.numIterations];
        double nextFunctionValue = 0;

        StringBuilder recalculationsString = new StringBuilder();
        for(int i = 0; i < this.numIterations; i++) {
            double functionValue = Math.pow((1. * i) / this.numIterations, 1. / this.recalculatePriority);
            if(functionValue >= nextFunctionValue) {
                this.recalculate[i] = true;
                nextFunctionValue += 1 / (this.recalculateCriticalities * this.numIterations);
                recalculationsString.append("|");

            } else {
                recalculationsString.append(".");
            }
        }

        this.logger.println("Criticalities recalculations:");
        this.logger.println(recalculationsString.toString());
        this.logger.println();
    }

    @Override
    protected boolean isTimingDriven() {
        return true;
    }


    @Override
    protected void updateLegalIfNeeded(int iteration) {
        int[] newLegalX = this.legalizer.getLegalX();
        int[] newLegalY = this.legalizer.getLegalY();

        this.latestCost = this.costCalculator.calculate(newLegalX, newLegalY, this.recalculate[iteration]);

        if(this.utilization == 1 && this.latestCost < this.minCost) {
            this.minCost = this.latestCost;
            this.updateLegal(newLegalX, newLegalY);
        }
    }


    @Override
    protected void addStatTitlesGP(List<String> titles) {
        titles.add("max delay");
        titles.add("best iteration");
    }

    @Override
    protected void addStats(List<String> stats) {
        stats.add(String.format("%.4g", this.latestCost));
        stats.add(this.latestCost == this.minCost ? "yes" : "");
    }


    @Override
    public String getName() {
        return "Timing driven gradient placer";
    }
}
