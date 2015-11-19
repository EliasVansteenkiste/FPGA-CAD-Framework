package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;

import placers.analyticalplacer.linear_solver.LinearSolver;
import placers.analyticalplacer.linear_solver.LinearSolverComplete;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.BlockType;

public abstract class AnalyticalPlacer extends AnalyticalAndGradientPlacer {

    private static final double EPSILON = 0.005;

    public static void initOptions(Options options) {
        options.add("stop ratio", "ratio between linear and legal cost above which placement should be stopped", new Double(0.9));
        options.add("anchor weight", "starting anchor weight", new Double(0));
        options.add("anchor weight multiplier", "multiplier to increase the anchor weight in each iteration", new Double(0.2));
    }

    private double stopRatio, anchorWeight, anchorWeightIncrease;
    private CostCalculator costCalculator;
    private double linearCost, legalCost;

    public AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.stopRatio = options.getDouble("stop ratio");

        this.anchorWeight = options.getDouble("anchor weight");
        this.anchorWeightIncrease = options.getDouble("anchor weight increase");
    }

    protected abstract CostCalculator createCostCalculator();

    @Override
    protected Legalizer createLegalizer(List<BlockType> blockTypes, List<Integer> blockTypeIndexStarts) {
        this.costCalculator = this.createCostCalculator();
        return new HeapLegalizer(this.circuit, this.costCalculator, this.blockIndexes, blockTypes, blockTypeIndexStarts, this.linearX, this.linearY);
    }

    @Override
    protected void solveLinear(int iteration) {
        boolean firstSolve = (iteration == 0);

        if(!firstSolve) {
            this.anchorWeight *= this.anchorWeightIncrease;
        }

        int innerIterations = firstSolve ? 5 : 1;
        for(int i = 0; i < innerIterations; i++) {
            LinearSolver solver = new LinearSolverComplete(this.linearX, this.linearY, this.numIOBlocks, this.anchorWeight, AnalyticalPlacer.EPSILON);
            this.solveLinearIteration(solver, !firstSolve);
        }
    }

    @Override
    protected boolean stopCondition() {
        this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
        this.legalCost = this.legalizer.getCost();

        return this.linearCost / this.legalCost > this.stopRatio;
    }

    @Override
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    anchor weight    linear cost    legal cost    time");
        this.logger.println("---------    -------------    -----------    ----------    ----");

    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.logger.printf("%-13d%-17f%-19f%-15f%-14f%f\n", iteration, this.anchorWeight, this.linearCost, this.legalCost, time);
    }
}
