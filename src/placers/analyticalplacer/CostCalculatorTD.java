package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import placers.analyticalplacer.AnalyticalAndGradientPlacer.NetBlock;
import placers.analyticalplacer.AnalyticalAndGradientPlacer.TimingNet;
import placers.analyticalplacer.AnalyticalAndGradientPlacer.TimingNetBlock;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.DelayTables;
import circuit.block.GlobalBlock;
import circuit.block.TimingGraph;

class CostCalculatorTD extends CostCalculator {

    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;

    private List<TimingNet> nets;


    CostCalculatorTD(
            Circuit circuit,
            Map<GlobalBlock, NetBlock> netBlocks,
            List<TimingNet> nets) {

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

                int deltaX = (int) Math.abs(sinkX - sourceX);
                int deltaY = (int) Math.abs(sinkY - sourceY);
                double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);

                sink.timingEdge.setWireDelay(wireDelay);
            }
        }
    }
}
