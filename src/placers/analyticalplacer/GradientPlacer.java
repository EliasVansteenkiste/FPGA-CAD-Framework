package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Arrays;
import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.TimingEdge;
import circuit.exceptions.PlacementException;

public abstract class GradientPlacer extends AnalyticalAndGradientPlacer {

    public static void initOptions(Options options) {
        options.add(
                "anchor weight",
                "starting anchor weight",
                new Double(0));

        options.add(
                "anchor weight increase",
                "value that is added to the anchor weight in each iteration",
                new Double(0.01));


        options.add(
                "step size",
                "ratio of distance to optimal position that is moved",
                new Double(0.4));

        options.add(
                "effort level",
                "number of gradient steps to take in each outer iteration",
                new Integer(40));
    }



    protected double anchorWeight;
    private double anchorWeightIncrease;
    private double stepSize;
    private final int gradientIterations;
    protected double maxUtilization;

    protected HeapLegalizer legalizer;
    private LinearSolverGradient solver;


    public GradientPlacer(
            Circuit circuit,
            Options options,
            Random random,
            Logger logger,
            PlacementVisualizer visualizer) {

        super(circuit, options, random, logger, visualizer);

        this.anchorWeight = this.options.getDouble("anchor weight");
        this.anchorWeightIncrease = this.options.getDouble("anchor weight increase");

        this.stepSize = this.options.getDouble("step size");
        this.gradientIterations = this.options.getInteger("effort level");
    }

    protected abstract void updateLegalIfNeeded();


    @Override
    public void initializeData() {
        super.initializeData();


        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.stepSize);

        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes, this.blockTypeIndexStarts,
                this.linearX, this.linearY);
    }



    @Override
    protected void solveLinear(int iteration) {
        if(iteration > 0) {
            this.anchorWeight += this.anchorWeightIncrease;
        }

        // Cache the max criticality of each net
        double[] netCriticalities = this.getNetCriticalities();

        int innerIterations = iteration == 0 ? 4 * this.gradientIterations : this.gradientIterations;

        for(int i = 0; i < innerIterations; i++) {
            this.solveLinearIteration(iteration, netCriticalities);
        }
    }


    private double[] getNetCriticalities() {
        int numNets = this.netUniqueBlockIndexes.size();
        double[] netCriticalities = new double[numNets];

        if(this.isTimingDriven()) {
            for(int netIndex = 0; netIndex < numNets; netIndex++) {
                TimingEdge[] edges = this.netTimingEdges.get(netIndex);
                int numEdges = edges.length;

                double maxCriticality = edges[0].getCriticality();
                for(int edgeIndex = 1; edgeIndex < numEdges; edgeIndex++) {
                    double criticality = edges[edgeIndex].getCriticality();
                    if(criticality > maxCriticality) {
                        maxCriticality = criticality;
                    }
                }

                netCriticalities[netIndex] = maxCriticality;
            }

        } else {
            Arrays.fill(netCriticalities, 1);
        }

        return netCriticalities;
    }


    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(int iteration, double[] netCriticalities) {

        // Reset the solver
        this.solver.initializeIteration(this.anchorWeight);

        // Process nets
        this.processNets(netCriticalities);

        // Add pseudo connections
        if(iteration > 0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            this.solver.addPseudoConnections(this.legalizer.getLegalX(), this.legalizer.getLegalY());
        }

        // Solve and save result
        this.solver.solve();
    }

    private void processNets(double[] netCriticalities) {
        int numNets = this.netUniqueBlockIndexes.size();
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            int[] blockIndexes = this.netUniqueBlockIndexes.get(netIndex);
            double criticality = netCriticalities[netIndex];

            this.solver.processNet(blockIndexes, criticality);
        }
    }


    @Override
    protected void solveLegal(int iteration) {
        // TODO: optimize this
        this.maxUtilization = Math.min(this.numBlocks, Math.max(1, 0.8 / this.anchorWeight));
        this.maxUtilization = 1;

        try {
            this.legalizer.legalize(this.maxUtilization);
        } catch(PlacementException error) {
            this.logger.raise(error);
        }

        this.updateLegalIfNeeded();
    }


    @Override
    protected boolean stopCondition() {
        return this.anchorWeight > 0.9;
    }




    @Override
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    anchor weight    max utilization    time");
        this.logger.println("---------    -------------    ---------------    ----");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.logger.printf("%-9d    %-13f    %-15f    %f\n", iteration, this.anchorWeight, this.maxUtilization, time);
    }
}
