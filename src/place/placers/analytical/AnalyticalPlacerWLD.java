package place.placers.analytical;

import place.circuit.Circuit;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.Random;



public class AnalyticalPlacerWLD extends AnalyticalPlacer {

    public AnalyticalPlacerWLD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    protected boolean isTimingDriven() {
        return false;
    }

    @Override
    public String getName() {
        return "Wirelength driven analytical placer";
    }

    @Override
    protected CostCalculator createCostCalculator() {
        return new CostCalculatorWLD(this.nets);
    }

    @Override
    protected void updateLegalIfNeeded(int[] x, int[] y) {
        this.startTimer(T_CALCULATE_COST);
        double tmpLegalCost = this.costCalculator.calculate(x, y);
        this.stopTimer(T_CALCULATE_COST);

        this.startTimer(T_UPDATE_CIRCUIT);
        if(tmpLegalCost < this.legalCost) {
            this.legalCost = tmpLegalCost;
            this.updateLegal(x, y);
        }

        this.stopTimer(T_UPDATE_CIRCUIT);
    }
}