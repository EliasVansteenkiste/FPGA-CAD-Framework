package placers.analytical;

import java.util.List;
import java.util.Map;

import placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import placers.analytical.AnalyticalAndGradientPlacer.TimingNetBlock;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.DelayTables;
import circuit.block.GlobalBlock;
import circuit.timing.TimingGraph;

class CostCalculatorTD extends CostCalculator {

    private int maxDeltaX, maxDeltaY;

    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;

    private List<TimingNet> nets;


    CostCalculatorTD(
            Circuit circuit,
            Map<GlobalBlock, NetBlock> netBlocks,
            List<TimingNet> nets) {

        this.maxDeltaX = circuit.getWidth() - 1;
        this.maxDeltaY = circuit.getHeight() - 1;

        this.timingGraph = circuit.getTimingGraph();
        this.delayTables = circuit.getArchitecture().getDelayTables();

        this.blockCategories = new BlockCategory[netBlocks.size()];
        for(Map.Entry<GlobalBlock, NetBlock> blockEntry : netBlocks.entrySet()) {
            BlockCategory category = blockEntry.getKey().getCategory();
            int index = blockEntry.getValue().blockIndex;

            this.blockCategories[index] = category;
        }

        this.nets = nets;
    }


    @Override
    protected double calculate(boolean recalculateCriticalities) {
        this.updateDelays();

        // If the provided solution is legal: update the criticalities in the timing graph
        if(this.isInts() && recalculateCriticalities) {
            this.timingGraph.calculateCriticalities(false);

        // Else: just get the max delay
        } else {
            this.timingGraph.calculateMaxDelay(false);
        }

        return this.timingGraph.getMaxDelay();
    }


    private void updateDelays() {
        for(TimingNet net : this.nets) {
            int sourceIndex = net.source.blockIndex;
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];

            double sourceX = this.getX(sourceIndex);
            double sourceY = this.getY(sourceIndex);

            for(TimingNetBlock sink : net.sinks) {
                int sinkIndex = sink.blockIndex;
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                double sinkX = this.getX(sinkIndex);
                double sinkY = this.getY(sinkIndex);

                int deltaX = Math.min(this.maxDeltaX, (int) Math.abs(sinkX - sourceX));
                int deltaY = Math.min(this.maxDeltaY, (int) Math.abs(sinkY - sourceY));
                double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);

                sink.timingEdge.setWireDelay(wireDelay);
            }
        }
    }
}
