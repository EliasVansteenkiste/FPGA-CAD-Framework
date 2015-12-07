package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.DelayTables;
import circuit.block.GlobalBlock;
import circuit.block.TimingEdge;
import circuit.block.TimingGraph;

class CostCalculatorTD extends CostCalculator {

    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;

    private List<int[]> netBlockIndexes;
    private List<TimingEdge[]> netTimingEdges;


    CostCalculatorTD(
            Circuit circuit,
            Map<GlobalBlock, Integer> blockIndexes,
            List<int[]> netBlockIndexes,
            List<TimingEdge[]> netTimingEdges) {

        this.timingGraph = circuit.getTimingGraph();
        this.delayTables = circuit.getArchitecture().getDelayTables();

        this.blockCategories = new BlockCategory[blockIndexes.size()];
        for(Map.Entry<GlobalBlock, Integer> blockEntry : blockIndexes.entrySet()) {
            BlockCategory category = blockEntry.getKey().getCategory();
            int index = blockEntry.getValue();

            this.blockCategories[index] = category;
        }

        this.netBlockIndexes = netBlockIndexes;
        this.netTimingEdges = netTimingEdges;
    }


    @Override
    protected double calculate(boolean recalculateCriticalities) {
        this.updateDelays();

        this.timingGraph.calculateArrivalTimes(false);

        // If the provided solution is legal: update the criticalities in the timing graph
        if(this.isInts()) {
            this.timingGraph.calculateRequiredTimes();
            if(recalculateCriticalities) {
                this.timingGraph.calculateCriticalities();
            }
        }

        return this.timingGraph.getMaxDelay();
    }


    private void updateDelays() {
        int numNets = this.netBlockIndexes.size();
        for(int netIndex = 0; netIndex < numNets; netIndex ++) {

            int[] blockIndexes = this.netBlockIndexes.get(netIndex);
            TimingEdge[] timingEdges = this.netTimingEdges.get(netIndex);

            int sourceIndex = blockIndexes[0];
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];
            double sourceX = this.getX(sourceIndex);
            double sourceY = this.getY(sourceIndex);

            int numPins = timingEdges.length;
            for(int i = 0; i < numPins; i++) {
                int sinkIndex = blockIndexes[i + 1];
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                double sinkX = this.getX(sinkIndex);
                double sinkY = this.getY(sinkIndex);
                int deltaX = (int) Math.abs(sinkX - sourceX);
                int deltaY = (int) Math.abs(sinkY - sourceY);
                double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);

                TimingEdge timingEdge = timingEdges[i];
                timingEdge.setWireDelay(wireDelay);
            }
        }
    }
}
