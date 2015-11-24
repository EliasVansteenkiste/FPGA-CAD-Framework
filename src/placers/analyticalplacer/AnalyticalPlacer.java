package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;


import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.exceptions.PlacementException;

public abstract class AnalyticalPlacer extends AnalyticalAndGradientPlacer {

    private static final double EPSILON = 0.005;

    public static void initOptions(Options options) {
        options.add("stop ratio", "ratio between linear and legal cost above which placement should be stopped", new Double(0.9));
        options.add("anchor weight", "starting anchor weight", new Double(0.3));
        options.add("anchor weight multiplier", "multiplier to increase the anchor weight in each iteration", new Double(1.1));
    }


    private double stopRatio, anchorWeight, anchorWeightMultiplier;
    private CostCalculator costCalculator;
    private double linearCost, legalCost = Double.MAX_VALUE;
    private int numMovableBlocks;

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

        this.numMovableBlocks = this.numBlocks - this.numIOBlocks;

        this.costCalculator = this.createCostCalculator();
    }

    @Override
    protected Legalizer createLegalizer(List<BlockType> blockTypes, List<Integer> blockTypeIndexStarts) {
        return new HeapLegalizer(this.circuit, blockTypes, blockTypeIndexStarts, this.linearX, this.linearY);
    }

    @Override
    protected void solveLinear(int iteration) {
        boolean firstSolve = (iteration == 0);

        if(!firstSolve) {
            this.anchorWeight *= this.anchorWeightMultiplier;
        }

        int innerIterations = firstSolve ? 5 : 1;
        for(int i = 0; i < innerIterations; i++) {
            LinearSolver solver = new LinearSolverAnalytical(this.linearX, this.linearY, this.numIOBlocks, this.anchorWeight, AnalyticalPlacer.EPSILON);
            this.solveLinearIteration(solver, !firstSolve);
        }

        this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
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

        int[] tmpLegalX = this.legalizer.getLegalX();
        int[] tmpLegalY = this.legalizer.getLegalY();
        double tmpLegalCost = this.costCalculator.calculate(tmpLegalX, tmpLegalY);

        if(tmpLegalCost < this.legalCost) {
            this.legalCost = tmpLegalCost;

            System.arraycopy(tmpLegalX, this.numIOBlocks, this.legalX, this.numIOBlocks, this.numMovableBlocks);
            System.arraycopy(tmpLegalY, this.numIOBlocks, this.legalY, this.numIOBlocks, this.numMovableBlocks);
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
