package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Option;
import interfaces.Options;

import java.util.List;
import java.util.Random;

import placers.analyticalplacer.linear_solver.LinearSolver;
import placers.analyticalplacer.linear_solver.LinearSolverGradient;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.BlockType;

public class GradientPlacer extends AnalyticalPlacer {

    public static void initOptions(Options options) {
        options.add(new Option("anchor weight", "starting anchor weight", new Double(0)));
        options.add(new Option("anchor weight increase", "value that is added to the anchor weight in each iteration", new Double(0.2)));

        options.add(new Option("gradient step size", "ratio of distance to optimal position that is moved", new Double(0.4)));
        options.add(new Option("gradient effort level", "number of gradient steps to take in each outer iteration", new Integer(40)));
    }


    private final int gradientIterations;
    private double gradientSpeed;
    private double anchorWeight, anchorWeightIncrease;

    public GradientPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.anchorWeight = this.options.getDouble("anchor weight");
        this.anchorWeightIncrease = this.options.getDouble("anchor weight increase");

        this.gradientSpeed = this.options.getDouble("gradient step size");
        this.gradientIterations = this.options.getInteger("gradient effort level");
    }


    @Override
    protected Legalizer createLegalizer(List<BlockType> blockTypes, List<Integer> blockTypeIndexStarts) {
        return new HeapLegalizer(this.circuit, null, this.blockIndexes, blockTypes, blockTypeIndexStarts, this.linearX, this.linearY);
    }



    @Override
    protected void solve(int iteration) {

        boolean addPseudoNets = iteration > 0;

        if(addPseudoNets) {
            this.anchorWeight += this.anchorWeightIncrease;
        }

        int innerIterations = iteration == 0 ? 4 * this.gradientIterations : this.gradientIterations;

        for(int i = 0; i < innerIterations; i++) {
            LinearSolver solver = new LinearSolverGradient(this.linearX, this.linearY, this.numIOBlocks, this.anchorWeight, this.gradientSpeed);
            this.solveLinear(solver, addPseudoNets);
        }
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
        this.logger.printf("%-13d%-17f%f\n", iteration, this.anchorWeight, time);
    }

    @Override
    public String getName() {
        return "Wirelength driven gradient descent placer";
    }
}
