package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.block.GlobalBlock;
import circuit.block.LeafBlock;
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


    private final int gradientIterations;
    private double gradientSpeed;
    protected double anchorWeight;
    private double anchorWeightIncrease;
    protected double maxUtilization;

    protected HeapLegalizer legalizer;
    private LinearSolverGradient solver;

    protected List<int[]> netBlockIndexes, netUniqueBlockIndexes;
    protected List<TimingEdge[]> netTimingEdges;

    public GradientPlacer(
            Circuit circuit,
            Options options,
            Random random,
            Logger logger,
            PlacementVisualizer visualizer) {

        super(circuit, options, random, logger, visualizer);

        this.anchorWeight = this.options.getDouble("anchor weight");
        this.anchorWeightIncrease = this.options.getDouble("anchor weight increase");

        this.gradientSpeed = this.options.getDouble("step size");
        this.gradientIterations = this.options.getInteger("effort level");
    }

    protected abstract boolean isTimingDriven();
    protected abstract void updateLegalIfNeeded();


    @Override
    public void initializeData() {
        super.initializeData();

        this.netBlockIndexes = new ArrayList<int[]>();
        this.netUniqueBlockIndexes = new ArrayList<int[]>();
        this.netTimingEdges = new ArrayList<TimingEdge[]>();

        // Add all nets
        // If the algorithm is timing driven: also store all the TimingEdges
        // in each net.
        boolean timingDriven = this.isTimingDriven();

        for(GlobalBlock sourceGlobalBlock : this.circuit.getGlobalBlocks()) {
            int sourceBlockIndex = this.blockIndexes.get(sourceGlobalBlock);

            for(LeafBlock sourceLeafBlock : sourceGlobalBlock.getLeafBlocks()) {
                int numSinks = sourceLeafBlock.getNumSinks();

                Set<Integer> blockIndexesSet = new HashSet<>();
                blockIndexesSet.add(sourceBlockIndex);

                int[] blockIndexes = new int[numSinks + 1];
                blockIndexes[0] = sourceBlockIndex;

                TimingEdge[] timingEdges = new TimingEdge[numSinks];

                for(int i = 0; i < numSinks; i++) {
                    GlobalBlock sinkGlobalBlock = sourceLeafBlock.getSink(i).getGlobalParent();
                    int sinkBlockIndex = this.blockIndexes.get(sinkGlobalBlock);

                    blockIndexesSet.add(sinkBlockIndex);
                    blockIndexes[i + 1] = sinkBlockIndex;

                    TimingEdge timingEdge = sourceLeafBlock.getSinkEdge(i);
                    timingEdges[i] = timingEdge;
                }


                /* Don't add nets which connect only one global block.
                 * Due to this, the WLD costcalculator is not entirely
                 * accurate, but that doesn't matter, because we use
                 * the same (inaccurate) costcalculator to calculate
                 * both the linear and legal cost, so the deviation
                 * cancels out.
                 */
                int numUniqueBlocks = blockIndexesSet.size();
                if(numUniqueBlocks > 1) {
                    int[] uniqueBlockIndexes = new int[numUniqueBlocks];
                    int i = 0;
                    for(Integer blockIndex : blockIndexesSet) {
                        uniqueBlockIndexes[i] = blockIndex;
                        i++;
                    }

                    this.netUniqueBlockIndexes.add(uniqueBlockIndexes);

                    // We only need all the blocks and their timing edges if
                    // the algorithm is timing driven
                    if(timingDriven) {
                        this.netTimingEdges.add(timingEdges);
                        this.netBlockIndexes.add(blockIndexes);
                    }
                }
            }
        }

        this.solver = new LinearSolverGradient(
                this.linearX, this.linearY,
                this.numIOBlocks, this.gradientSpeed);

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
