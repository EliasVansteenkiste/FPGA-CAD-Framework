package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Arrays;
import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.exceptions.PlacementException;

public abstract class GradientPlacer extends AnalyticalAndGradientPlacer {

    private static final String
        O_ANCHOR_WEIGHT_START = "anchor weight start",
        O_ANCHOR_WEIGHT_STEP = "anchor weight step",
        O_ANCHOR_WEIGHT_STOP = "anchor weight stop",
        O_STEP_SIZE = "step size",
        O_EFFORT_LEVEL = "effort level";

    public static void initOptions(Options options) {
        options.add(
                O_ANCHOR_WEIGHT_START,
                "starting anchor weight",
                new Double(0));

        options.add(
                O_ANCHOR_WEIGHT_STEP,
                "value that is added to the anchor weight in each iteration",
                new Double(0.01));

        options.add(
                O_ANCHOR_WEIGHT_STOP,
                "anchor weight at which the placement is finished",
                new Double(0.9));


        options.add(
                O_STEP_SIZE,
                "ratio of distance to optimal position that is moved",
                new Double(0.4));

        options.add(
                O_EFFORT_LEVEL,
                "number of gradient steps to take in each outer iteration",
                new Integer(40));
    }


    protected double anchorWeight;
    protected double anchorWeightStart, anchorWeightStop, anchorWeightStep;
    private double stepSize;
    private final int gradientIterations;
    protected double maxUtilization;

    private double[] netCriticalities;

    protected HeapLegalizer legalizer;
    private LinearSolverGradient solver;


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
        this.gradientIterations = this.options.getInteger(O_EFFORT_LEVEL);
    }

    protected abstract void updateLegalIfNeeded(int iteration);


    @Override
    public void initializeData() {
        super.initializeData();

        this.startTimer(T_INITIALIZE_DATA);

        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.stepSize);

        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes,
                this.blockTypeIndexStarts,
                this.linearX,
                this.linearY,
                this.heights);

        int numNets = this.nets.size();
        this.netCriticalities = new double[numNets];
        Arrays.fill(this.netCriticalities, 1);

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
        int numNets = this.nets.size();
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
        int numNets = this.nets.size();
        boolean timingDriven = this.isTimingDriven();

        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            Net net = this.nets.get(netIndex);
            double criticality = timingDriven ? this.netCriticalities[netIndex] : 1;

            this.solver.processNet(net, criticality);
        }
    }


    @Override
    protected void solveLegal(int iteration) {

        // TODO: optimize this
        //this.maxUtilization = Math.min(this.numBlocks, Math.max(1, 0.8 / this.anchorWeight));
        this.maxUtilization = 1;

        this.startTimer(T_LEGALIZE);
        try {
            this.legalizer.legalize(this.maxUtilization);
        } catch(PlacementException error) {
            this.logger.raise(error);
        }
        this.stopTimer(T_LEGALIZE);

        this.startTimer(T_UPDATE_CIRCUIT);
        this.updateLegalIfNeeded(iteration);
        this.stopTimer(T_UPDATE_CIRCUIT);
    }


    @Override
    protected boolean stopCondition() {
        return this.anchorWeight > this.anchorWeightStop;
    }
}
