package placers.SAPlacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class WLD_SAPlacer extends SAPlacer {

    public static void initOptions(Options options) {
        SAPlacer.initOptions(options);
    }

    private EfficientBoundingBoxNetCC calculator;
    private double cachedCost;

    public WLD_SAPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.calculator = new EfficientBoundingBoxNetCC(circuit);
    }

    @Override
    public String getName() {
        return "WLD simulated annealing placer";
    }

    @Override
    protected void initializePlace() {
        this.calculator.recalculateFromScratch();
    }

    @Override
    protected void initializeSwapIteration() {
    }

    @Override
    protected String getStatistics() {
        return "cost = " + this.getCost();
    }

    @Override
    protected double getCost() {
        if(this.circuitChanged) {
            this.circuitChanged = false;
            this.cachedCost = this.calculator.calculateTotalCost();
        }

        return this.cachedCost;
    }

    @Override
    protected double getDeltaCost(Swap swap) {
        return this.calculator.calculateDeltaCost(swap);
    }

    @Override
    protected void pushThrough(int iteration) {
        this.calculator.pushThrough();
    }

    @Override
    protected void revert(int iteration) {
        this.calculator.revert();
    }
}