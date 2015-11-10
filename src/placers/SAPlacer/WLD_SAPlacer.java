package placers.SAPlacer;

import interfaces.Logger;

import java.util.Map;

import visual.PlacementVisualizer;

import circuit.Circuit;



public class WLD_SAPlacer extends SAPlacer {

    private static final String name = "WLD Simulated Annealing Placer";

    private EfficientBoundingBoxNetCC calculator;
    private double cachedCost;

    public WLD_SAPlacer(Logger logger, PlacementVisualizer visualizer, Circuit circuit, Map<String, String> options) {
        super(logger, visualizer, circuit, options);

        this.calculator = new EfficientBoundingBoxNetCC(circuit);
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

    @Override
    public String getName() {
        return WLD_SAPlacer.name;
    }
}