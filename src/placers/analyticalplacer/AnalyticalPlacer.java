package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.exceptions.PlacementException;

public abstract class AnalyticalPlacer extends AnalyticalAndGradientPlacer {

    private static final double EPSILON = 0.005;

    public static void initOptions(Options options) {
        options.add("stop ratio", "ratio between linear and legal cost above which placement should be stopped", new Double(0.9));
        options.add("anchor weight", "starting anchor weight", new Double(0.3));
        options.add("anchor weight multiplier", "multiplier to increase the anchor weight in each iteration", new Double(1.1));
    }


    private double stopRatio, anchorWeight, anchorWeightMultiplier;
    protected double criticalityThreshold; // This is only used by AnalyticalPlacerTD

    private double linearCost, legalCost = Double.MAX_VALUE;

    private HeapLegalizer legalizer;
    private CostCalculator costCalculator;




    public AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.stopRatio = options.getDouble("stop ratio");

        this.anchorWeight = options.getDouble("anchor weight");
        this.anchorWeightMultiplier = options.getDouble("anchor weight multiplier");
    }


    protected abstract CostCalculator createCostCalculator();

    @Override
    public void initializeData() {
        super.initializeData();

        this.legalizer = new HeapLegalizer(this.circuit, this.blockTypes, this.blockTypeIndexStarts, this.linearX, this.linearY);
        this.costCalculator = this.createCostCalculator();
    }


    @Override
    protected void solveLinear(int iteration) {
        if(iteration > 0) {
            this.anchorWeight *= this.anchorWeightMultiplier;
        }

        int innerIterations = iteration == 0 ? 5 : 1;
        for(int i = 0; i < innerIterations; i++) {
            LinearSolverAnalytical solver = new LinearSolverAnalytical(this.linearX, this.linearY, this.numIOBlocks, this.anchorWeight, this.criticalityThreshold, AnalyticalPlacer.EPSILON);
            this.solveLinearIteration(solver, iteration);
        }

        this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(LinearSolverAnalytical solver, int iteration) {

        // Add connections between blocks that are connected by a net
        this.processNetsWLD(solver);

        this.processNetsTD(solver);

        // Add pseudo connections
        if(iteration > 0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            solver.addPseudoConnections(this.legalizer.getLegalX(), this.legalizer.getLegalY());
        }

        // Solve and save result
        solver.solve();
    }

    protected void processNetsWLD(LinearSolverAnalytical solver) {
        for(int[] net : this.netUniqueBlockIndexes) {
            solver.processNetWLD(net);
        }
    }

    protected void processNetsTD(LinearSolverAnalytical solver) {
        // If the Placer is not timing driven, this.timingNets is empty
        int numNets = this.netBlockIndexes.size();
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            solver.processNetTD(this.netBlockIndexes.get(netIndex), this.netTimingEdges.get(netIndex));
        }
    }

    @Override
    protected void solveLegal(int iteration) {
        // This is fixed, because making it dynamic doesn't improve results
        // But HeapLegalizer still supports other values for maxUtilization
        double maxUtilization = 1;

        try {
            this.legalizer.legalize(maxUtilization);
        } catch(PlacementException error) {
            this.logger.raise(error);
        }

        int[] newLegalX = this.legalizer.getLegalX();
        int[] newLegalY = this.legalizer.getLegalY();
        double tmpLegalCost = this.costCalculator.calculate(newLegalX, newLegalY);

        if(tmpLegalCost < this.legalCost) {
            this.legalCost = tmpLegalCost;
            this.updateLegal(newLegalX, newLegalY);
        }
    }

    @Override
    protected boolean stopCondition() {
        return this.linearCost / this.legalCost > this.stopRatio;
    }

    @Override
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    anchor weight    linear cost    legal cost    time");
        this.logger.println("---------    -------------    -----------    ----------    ----");

    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.logger.printf("%-13d%-17f%-15f%-14f%f\n", iteration, this.anchorWeight, this.linearCost, this.legalCost, time);
    }
}
