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
                "anchor weight start",
                "starting anchor weight",
                new Double(0));

        options.add(
                "anchor weight step",
                "value that is added to the anchor weight in each iteration",
                new Double(0.01));

        options.add("anchor weight stop",
                "anchor weight at which the placement is finished",
                new Double(0.9));


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
    protected double anchorWeightStart, anchorWeightStop, anchorWeightStep;
    private double stepSize;
    private final int gradientIterations;
    protected double maxUtilization;

    private double[] netCriticalities;
    private double[][] debugCriticalities;

    protected HeapLegalizer legalizer;
    private LinearSolverGradient solver;


    public GradientPlacer(
            Circuit circuit,
            Options options,
            Random random,
            Logger logger,
            PlacementVisualizer visualizer) {

        super(circuit, options, random, logger, visualizer);

        this.anchorWeightStart = this.options.getDouble("anchor weight start");
        this.anchorWeightStep = this.options.getDouble("anchor weight step");
        this.anchorWeightStop = this.options.getDouble("anchor weight stop");
        this.anchorWeight = this.anchorWeightStart;

        this.stepSize = this.options.getDouble("step size");
        this.gradientIterations = this.options.getInteger("effort level");

        this.debugCriticalities = new double[91][];
    }

    protected abstract void updateLegalIfNeeded(int iteration);


    @Override
    public void initializeData() {
        super.initializeData();


        this.solver = new LinearSolverGradient(this.linearX, this.linearY, this.stepSize);

        this.legalizer = new HeapLegalizer(
                this.circuit,
                this.blockTypes, this.blockTypeIndexStarts,
                this.linearX, this.linearY);

        int numNets = this.netUniqueBlockIndexes.size();
        this.netCriticalities = new double[numNets];
        Arrays.fill(this.netCriticalities, 1);
    }



    @Override
    protected void solveLinear(int iteration) {
        if(iteration > 0) {
            this.anchorWeight += this.anchorWeightStep;
        }

        // Cache the max criticality of each net
        if(this.isTimingDriven() && iteration % 1 == 0) {
            this.updateNetCriticalities();

            // DEBUG
            this.debugCriticalities[iteration] = new double[this.netCriticalities.length];
            System.arraycopy(this.netCriticalities, 0, this.debugCriticalities[iteration], 0, this.netCriticalities.length);
        }

        int innerIterations = iteration == 0 ? 4 * this.gradientIterations : this.gradientIterations;

        for(int i = 0; i < innerIterations; i++) {
            this.solveLinearIteration(iteration);
        }
    }


    private void updateNetCriticalities() {
        int numNets = this.netUniqueBlockIndexes.size();

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

            this.netCriticalities[netIndex] = maxCriticality;
        }
    }


    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(int iteration) {

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

        // Solve and save result
        this.solver.solve();
    }

    private void processNets() {
        int numNets = this.netUniqueBlockIndexes.size();
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            int[] blockIndexes = this.netUniqueBlockIndexes.get(netIndex);
            double criticality = this.netCriticalities[netIndex];

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

        this.updateLegalIfNeeded(iteration);
    }


    @Override
    protected boolean stopCondition() {
        // DEBUG
        /*if(this.anchorWeight > this.anchorWeightStop) {
            for(int i = 0; i < this.netCriticalities.length; i++) {
                StringBuilder line = new StringBuilder();
                String prefix = "";
                for(int iteration = 0; iteration < 91; iteration++) {
                    line.append(prefix);
                    prefix = ",";
                    line.append(this.debugCriticalities[iteration][i]);
                }

                this.logger.println(line.toString());
            }
        }*/

        return this.anchorWeight > this.anchorWeightStop;
    }
}
