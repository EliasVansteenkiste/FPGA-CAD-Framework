package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.TimingGraph;

public class GradientPlacerTD extends GradientPlacer {

    public static void initOptions(Options options) {
        GradientPlacer.initOptions(options);

        options.add("criticality exponent", "criticality exponent of connections", new Double(3));
        options.add("criticality threshold", "minimal criticality for adding extra constraints", new Double(0.5));
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

        this.costCalculator = new CostCalculatorTD(
                this.circuit,
                this.netBlocks,
                this.timingNets);


        int numIterations = (int) ((this.anchorWeightStop - this.anchorWeightStart) / this.anchorWeightStep);
        this.recalculate = new boolean[numIterations];
        double nextFunctionValue = 0;

        StringBuilder recalculationsString = new StringBuilder();
        for(int i = 0; i < numIterations; i++) {
            double functionValue = Math.pow((1. * i) / numIterations, 1. / this.recalculatePriority);
            if(functionValue >= nextFunctionValue) {
                this.recalculate[i] = true;
                nextFunctionValue += 1 / (this.recalculateCriticalities * numIterations);
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
