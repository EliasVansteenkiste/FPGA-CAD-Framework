package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;

public abstract class GradientPlacer extends AnalyticalAndGradientPlacer {

    private static final String
        O_ANCHOR_WEIGHT_START = "anchor weight start",
        O_ANCHOR_WEIGHT_STEP = "anchor weight step",
        O_ANCHOR_WEIGHT_STOP = "anchor weight stop",
        O_STEP_SIZE = "step size",
        O_MAX_CONNECTION_LENGTH = "max connection length",
        O_SPEED_AVERAGING = "speed averaging",
        O_EFFORT_LEVEL = "effort level";

    public static void initOptions(Options options) {
        AnalyticalAndGradientPlacer.initOptions(options);

        options.add(
                O_ANCHOR_WEIGHT_START,
                "starting anchor weight",
                new Double(0.01));

        options.add(
                O_ANCHOR_WEIGHT_STEP,
                "value that is added to the anchor weight in each iteration",
                new Double(0.01));

        options.add(
                O_ANCHOR_WEIGHT_STOP,
                "anchor weight at which the placement is finished (max: 1)",
                new Double(0.9));


        options.add(
                O_STEP_SIZE,
                "ratio of distance to optimal position that is moved",
                new Double(0.4));

        options.add(
                O_MAX_CONNECTION_LENGTH,
                "length to which connection lengths are platformed",
                new Double(20));

        options.add(
                O_SPEED_AVERAGING,
                "averaging factor for block speeds",
                new Double(0.3));


        options.add(
                O_EFFORT_LEVEL,
                "number of gradient steps to take in each outer iteration",
                new Integer(40));
    }


    protected double anchorWeight;
    protected double anchorWeightStart, anchorWeightStop, anchorWeightStep;
    private double stepSize, maxConnectionLength, speedAveraging;
    private final int gradientIterations;
    protected double utilization;

    private double[] netCriticalities;

    protected HeapLegalizer legalizer;
    private LinearSolverGradient solver;


    private int[] netEnds;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;


    protected abstract void addStatTitlesGP(List<String> titles);
    protected abstract void addStats(List<String> stats);


    public GradientPlacer(
            Circuit circuit,
            Options options,
            Random random,
            Logger logger,
            PlacementVisualizer visualizer) {

        super(circuit, options, random, logger, visualizer);

        this.anchorWeightStart = this.options.getDouble(O_ANCHOR_WEIGHT_START);
        this.anchorWeightStep = this.options.getDouble(O_ANCHOR_WEIGHT_STEP);
        this.anchorWeightStop = this.options.getDouble(O_ANCHOR_WEIGHT_STOP);
        this.anchorWeight = this.anchorWeightStart;

        this.stepSize = this.options.getDouble(O_STEP_SIZE);
        this.maxConnectionLength = this.options.getDouble(O_MAX_CONNECTION_LENGTH);
        this.speedAveraging = this.options.getDouble(O_SPEED_AVERAGING);
        this.gradientIterations = this.options.getInteger(O_EFFORT_LEVEL);
    }

    protected abstract void updateLegalIfNeeded(int iteration);


    @Override
    public void initializeData() {
        super.initializeData();

        this.startTimer(T_INITIALIZE_DATA);

        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes,
                this.blockTypeIndexStarts,
                this.linearX,
                this.linearY,
                this.heights);

        // There can be more WLD nets than this, if there
        // are blocks that are only connected to themselves.
        // But we don't need these dummy nets, and they are
        // always at the back of the list, so that doesn't matter.
        this.netCriticalities = new double[this.numRealNets];


        // Juggling with objects is too slow (I profiled this,
        // the speedup is around 40%)
        // Build some good ol' arrays of primitive types
        int netBlockSize = 0;
        for(int i = 0; i < this.numRealNets; i++) {
            netBlockSize += this.nets.get(i).blocks.length;
        }

        this.netEnds = new int[this.numRealNets];
        this.netBlockIndexes = new int[netBlockSize];
        this.netBlockOffsets = new float[netBlockSize];

        int netBlockCounter = 0;
        for(int netCounter = 0; netCounter < this.numRealNets; netCounter++) {
            Net net = this.nets.get(netCounter);

            for(NetBlock block : net.blocks) {
                this.netBlockIndexes[netBlockCounter] = block.blockIndex;
                this.netBlockOffsets[netBlockCounter] = block.offset;

                netBlockCounter++;
            }

            this.netEnds[netCounter] = netBlockCounter;
        }


        this.solver = new LinearSolverGradient(
                this.linearX,
                this.linearY,
                this.netBlockIndexes,
                this.netBlockOffsets,
                this.stepSize,
                this.maxConnectionLength,
                this.speedAveraging);

        this.stopTimer(T_INITIALIZE_DATA);
    }



    @Override
    protected void solveLinear(int iteration) {
        if(iteration > 0) {
            this.anchorWeight += this.anchorWeightStep;
        }

        // Cache the max criticality of each net
        this.startTimer(T_BUILD_LINEAR);
        if(this.isTimingDriven() && iteration % 1 == 0) {
            this.updateNetCriticalities();
        }
        this.stopTimer(T_BUILD_LINEAR);

        int innerIterations = iteration == 0 ? 4 * this.gradientIterations : this.gradientIterations;

        for(int i = 0; i < innerIterations; i++) {
            this.solveLinearIteration(iteration);
        }
    }


    private void updateNetCriticalities() {
        int numNets = this.timingNets.size();
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            TimingNet net = this.timingNets.get(netIndex);
            this.netCriticalities[netIndex] = net.getCriticality();
        }
    }


    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(int iteration) {

        this.startTimer(T_BUILD_LINEAR);

        // Reset the solver
        this.solver.initializeIteration(this.anchorWeight);

        // Process nets
        this.processNets();

        // Add pseudo connections
        if(iteration > 0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            this.solver.addPseudoConnections(this.legalizer.getLegalX(), this.legalizer.getLegalY());
        }

        this.stopTimer(T_BUILD_LINEAR);

        // Solve and save result
        this.startTimer(T_SOLVE_LINEAR);
        this.solver.solve();
        this.stopTimer(T_SOLVE_LINEAR);
    }

    private void processNets() {
        int numNets = this.netEnds.length;
        boolean timingDriven = this.isTimingDriven();

        int netStart, netEnd = 0;
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            double criticality = timingDriven ? this.netCriticalities[netIndex] : 1;

            netStart = netEnd;
            netEnd = this.netEnds[netIndex];

            this.solver.processNet(netStart, netEnd, criticality);
        }
    }


    @Override
    protected void solveLegal(int iteration) {
        double slope = (this.startUtilization - 1) / (this.anchorWeightStart - this.anchorWeightStop);
        this.utilization = Math.max(1, 1 + slope * (this.anchorWeight - this.anchorWeightStop));

        this.startTimer(T_LEGALIZE);
        this.legalizer.legalize(this.utilization);
        this.stopTimer(T_LEGALIZE);

        this.startTimer(T_UPDATE_CIRCUIT);
        this.updateLegalIfNeeded(iteration);
        this.stopTimer(T_UPDATE_CIRCUIT);
    }


    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("iteration");
        titles.add("anchor weight");
        titles.add("utilization");

        this.addStatTitlesGP(titles);

        titles.add("time");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        List<String> stats = new ArrayList<>();

        stats.add(Integer.toString(iteration));
        stats.add(String.format("%.2f", this.anchorWeight));
        stats.add(String.format("%.3g", this.utilization));
        this.addStats(stats);
        stats.add(String.format("%.3g", time));

        this.printStats(stats.toArray(new String[0]));
    }


    @Override
    protected boolean stopCondition() {
        return this.anchorWeight > this.anchorWeightStop;
    }
}
