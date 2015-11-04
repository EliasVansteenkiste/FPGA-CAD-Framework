package placers.SAPlacer;

import java.util.Map;

import circuit.Circuit;



public class WLD_SAPlacer extends SAPlacer {    
    
    private EfficientCostCalculator calculator;
    private double cachedCost;
    
    public WLD_SAPlacer(Circuit circuit, Map<String, String> options) {
        super(circuit, options);
        
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
}