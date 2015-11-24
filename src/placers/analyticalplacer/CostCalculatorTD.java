package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.DelayTables;
import circuit.block.GlobalBlock;
import circuit.block.TimingEdge;
import circuit.block.TimingGraph;

import util.Pair;

class CostCalculatorTD extends CostCalculator {

    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;

    private List<List<Pair<Integer, TimingEdge>>> timingNets;

    CostCalculatorTD(Circuit circuit, Map<GlobalBlock, Integer> blockIndexes, List<List<Pair<Integer, TimingEdge>>> timingNets) {
        this.timingGraph = circuit.getTimingGraph();
        this.delayTables = circuit.getArchitecture().getDelayTables();

        this.blockCategories = new BlockCategory[blockIndexes.size()];
        for(Map.Entry<GlobalBlock, Integer> blockEntry : blockIndexes.entrySet()) {
            BlockCategory category = blockEntry.getKey().getCategory();
            int index = blockEntry.getValue();

            this.blockCategories[index] = category;
        }

        this.timingNets = timingNets;
    }

    @Override
    protected double calculate() {
        for(List<Pair<Integer, TimingEdge>> net : this.timingNets) {
            int sourceIndex = net.get(0).getFirst();
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];
            double sourceX = this.getX(sourceIndex);
            double sourceY = this.getY(sourceIndex);

            int numPins = net.size();
            for(int i = 1; i < numPins; i++) {
                Pair<Integer, TimingEdge> entry = net.get(i);

                int sinkIndex = entry.getFirst();
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                double sinkX = this.getX(sinkIndex);
                double sinkY = this.getY(sinkIndex);
                int deltaX = (int) Math.abs(sinkX - sourceX);
                int deltaY = (int) Math.abs(sinkY - sourceY);
                double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);

                TimingEdge timingEdge = entry.getSecond();
                timingEdge.setWireDelay(wireDelay);
            }
        }

        this.timingGraph.calculateArrivalTimes(false);
        return this.timingGraph.getMaxDelay();
    }
}
