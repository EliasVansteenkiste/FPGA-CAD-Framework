package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;


import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.exceptions.PlacementException;

public abstract class GradientPlacer extends AnalyticalAndGradientPlacer {

    public static void initOptions(Options options) {
        options.add("anchor weight", "starting anchor weight", new Double(0));
        options.add("anchor weight increase", "value that is added to the anchor weight in each iteration", new Double(0.01));

        options.add("gradient step size", "ratio of distance to optimal position that is moved", new Double(0.4));
        options.add("gradient effort level", "number of gradient steps to take in each outer iteration", new Integer(40));
    }


    private final int gradientIterations;
    private double gradientSpeed;
    protected double criticalityThreshold; // Only used by GradientPlacerTD
    protected double anchorWeight;
    private double anchorWeightIncrease;
    protected double maxUtilization;
    private LinearSolverGradient solver;

    public GradientPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.anchorWeight = this.options.getDouble("anchor weight");
        this.anchorWeightIncrease = this.options.getDouble("anchor weight increase");

        this.gradientSpeed = this.options.getDouble("gradient step size");
        this.gradientIterations = this.options.getInteger("gradient effort level");
    }


    @Override
    protected Legalizer createLegalizer(List<BlockType> blockTypes, List<Integer> blockTypeIndexStarts) {
        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.numIOBlocks, this.anchorWeight, this.criticalityThreshold, this.gradientSpeed);
        return new HeapLegalizer(this.circuit, blockTypes, blockTypeIndexStarts, this.linearX, this.linearY);
    }



    @Override
    protected void solveLinear(int iteration) {
        if(iteration > 0) {
            this.anchorWeight += this.anchorWeightIncrease;
        }

        int innerIterations = iteration == 0 ? 4 * this.gradientIterations : this.gradientIterations;

        //this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.numIOBlocks, this.anchorWeight, this.criticalityThreshold, this.gradientSpeed);
        for(int i = 0; i < innerIterations; i++) {
            solver.reset(this.anchorWeight);
            this.solveLinearIteration(solver, iteration);
        }
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(LinearSolver solver, int iteration) {

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

    @Override
    protected void solveLegal(int iteration) {
        // This is fixed, because making it dynamic doesn't improve results
        // But HeapLegalizer still supports other values for maxUtilization
        //this.maxUtilization = Math.max(4.5 - 5 * this.anchorWeight, 1);
        this.maxUtilization = Math.min(this.numBlocks, Math.max(1, 0.8 / this.anchorWeight));
        System.out.println(this.maxUtilization);

        try {
            this.legalizer.legalize(this.maxUtilization);
        } catch(PlacementException error) {
            this.logger.raise(error);
        }

        this.updateLegal();
    }


    @Override
    protected boolean stopCondition() {
        return this.anchorWeight > 0.9;
    }




    @Override
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    anchor weight    time");
        this.logger.println("---------    -------------    ----");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.logger.printf("%-9d    %-13f    %f\n", iteration, this.anchorWeight, time);
    }
}
