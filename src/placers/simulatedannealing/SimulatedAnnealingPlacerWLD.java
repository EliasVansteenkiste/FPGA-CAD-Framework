package placers.simulatedannealing;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class SimulatedAnnealingPlacerWLD extends SimulatedAnnealingPlacer {

    public static void initOptions(Options options) {
        SimulatedAnnealingPlacer.initOptions(options);
    }

    private EfficientBoundingBoxNetCC calculator;
    private double cachedCost;

    public SimulatedAnnealingPlacerWLD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
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
        // Do nothing
    }

    @Override
    protected void addStatisticsTitlesSA(List<String> titles) {
        titles.add("cost");
    }

    @Override
    protected void addStats(List<String> stats) {
        stats.add(String.format("%.5g", this.getCost()));
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