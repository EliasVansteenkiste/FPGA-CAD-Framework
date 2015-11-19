package placers.analyticalplacer;


import java.util.List;

import circuit.block.TimingGraph;


public class CostCalculatorTD extends CostCalculatorWLD {

    private TimingGraph timingGraph;
    private double tradeOff;
    private double initialBBCost, initialTDCost;

    CostCalculatorTD(List<int[]> nets, TimingGraph timingGraph, double tradeOff) {
        super(nets);

        this.timingGraph = timingGraph;
        this.tradeOff = tradeOff;
    }

    @Override
    boolean requiresCircuitUpdate() {
        return true;
    }

    @Override
    protected double calculate() {

        this.timingGraph.recalculateAllSlackCriticalities();
        double TDCost = this.timingGraph.calculateTotalCost();

        double BBCost = super.calculate();

        if(this.initialBBCost == 0) {
            this.initialBBCost = BBCost;
            this.initialTDCost = TDCost;
            return Double.MAX_VALUE / 10;

        } else {
            return
                    this.tradeOff         * TDCost / this.initialTDCost
                    + (1 - this.tradeOff) * BBCost / this.initialBBCost;
        }
    }
}
