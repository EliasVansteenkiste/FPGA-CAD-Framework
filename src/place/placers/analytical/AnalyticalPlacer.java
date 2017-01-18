package place.placers.analytical;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class AnalyticalPlacer extends AnalyticalAndGradientPlacer {

    private static final double EPSILON = 0.005;

    private double ratio;

    private static final String
        O_STOP_RATIO = "stop ratio",
        O_ANCHOR_WEIGHT = "anchor weight",
        O_ANCHOR_WEIGHT_MULTIPLIER = "anchor weight multiplier";

    public static void initOptions(Options options) {
        AnalyticalAndGradientPlacer.initOptions(options);

        options.add(
                O_STOP_RATIO,
                "ratio between linear and legal cost to stop placement",
                new Double(0.9));

        options.add(
                O_ANCHOR_WEIGHT,
                "starting anchor weight",
                new Double(0.2));

        options.add(
                O_ANCHOR_WEIGHT_MULTIPLIER,
                "anchor weight multiplier in each iteration",
                new Double(1.1));
    }


    private double stopRatio, anchorWeight, anchorWeightMultiplier;
    protected double criticalityThreshold, tradeOff; // This is only used by AnalyticalPlacerTD

    private double linearCost;
    protected double legalCost = Double.MAX_VALUE;

    private Legalizer legalizer;
    protected CostCalculator costCalculator;


    public AnalyticalPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.stopRatio = options.getDouble(O_STOP_RATIO);

        this.anchorWeight = options.getDouble(O_ANCHOR_WEIGHT);
        this.anchorWeightMultiplier = options.getDouble(O_ANCHOR_WEIGHT_MULTIPLIER);
    }


    protected abstract CostCalculator createCostCalculator();
    protected abstract void updateLegalIfNeeded(int[] x, int[] y);


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
                this.legalX,
                this.legalY,
                this.heights);

        this.costCalculator = this.createCostCalculator();

        this.stopTimer(T_INITIALIZE_DATA);
    }


    @Override
    protected void solveLinear(int iteration, BlockType movableBlockType) {
    	
    	/*TODO Implement the functionality to allow only a few blocktypes to move in analytical placement.	*
    	 *Current this functionality is only implemented in the gradient descent placer						*/

        if(iteration > 0) {
            this.anchorWeight *= this.anchorWeightMultiplier;
        }

        int innerIterations = iteration == 0 ? 5 : 1;
        for(int i = 0; i < innerIterations; i++) {

            LinearSolverAnalytical solver = new LinearSolverAnalytical(
                    this.linearX,
                    this.linearY,
                    this.numIOBlocks,
                    this.anchorWeight,
                    this.criticalityThreshold,
                    this.tradeOff,
                    AnalyticalPlacer.EPSILON);
            this.solveLinearIteration(solver, iteration);
        }

        this.startTimer(T_CALCULATE_COST);
        this.linearCost = this.costCalculator.calculate(this.linearX, this.linearY);
        this.stopTimer(T_CALCULATE_COST);
    }

    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(LinearSolverAnalytical solver, int iteration) {

        this.startTimer(T_BUILD_LINEAR);

        // Add connections between blocks that are connected by a net
        this.processNetsWLD(solver);

        this.processNetsTD(solver);

        // Add pseudo connections
        if(iteration > 0) {
            // this.legalX and this.legalY store the solution with the lowest cost
            // For anchors, the last (possibly suboptimal) solution usually works better
            solver.addPseudoConnections(this.legalizer.getLegalX(), this.legalizer.getLegalY());
        }

        this.stopTimer(T_BUILD_LINEAR);


        // Solve and save result
        this.startTimer(T_SOLVE_LINEAR);
        solver.solve();
        this.stopTimer(T_SOLVE_LINEAR);
    }

    protected void processNetsWLD(LinearSolverAnalytical solver) {
        for(Net net : this.nets) {
            solver.processNetWLD(net);
        }
    }

    protected void processNetsTD(LinearSolverAnalytical solver) {
        // If the Placer is not timing driven, this.timingNets is empty
        int numNets = this.timingNets.size();
        for(int netIndex = 0; netIndex < numNets; netIndex++) {
            solver.processNetTD(this.timingNets.get(netIndex));
        }
    }

    @Override
    protected void solveLegal(int iteration,  BlockType movableBlockType) {
        this.startTimer(T_LEGALIZE);
        this.legalizer.legalize(1, movableBlockType);
        this.stopTimer(T_LEGALIZE);

        int[] newLegalX = this.legalizer.getLegalX();
        int[] newLegalY = this.legalizer.getLegalY();
        this.updateLegalIfNeeded(newLegalX, newLegalY);
        this.ratio = this.linearCost / this.legalCost;
    }
    
    @Override
    protected void initializeIteration(int iteration){
    	this.legalizer.initializeLegalizationAreas();
    }
    
    @Override
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return this.legalizer.getLegalizationAreas();
    }

    @Override
    protected boolean stopCondition(int iteration) {
        return this.ratio > this.stopRatio;
    }



    @Override
    protected void addStatTitles(List<String> titles) {
        titles.add("iteration");
        titles.add("anchor weight");
        titles.add("linear cost");
        titles.add("legal cost");
        titles.add("time");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.printStats(
                Integer.toString(iteration),
                String.format("%.2f", this.anchorWeight),
                String.format("%.5g", this.linearCost),
                String.format("%.5g", this.legalCost),
                String.format("%.3g", time));
    }
}
