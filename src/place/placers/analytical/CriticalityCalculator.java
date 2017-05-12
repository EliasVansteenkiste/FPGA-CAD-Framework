package place.placers.analytical;

import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.DelayTables;
import place.circuit.block.GlobalBlock;
import place.circuit.timing.TimingGraph;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNetBlock;

class CriticalityCalculator{

    private int width, height;

    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;

    private List<TimingNet> nets;

    CriticalityCalculator(
            Circuit circuit,
            Map<GlobalBlock, NetBlock> netBlocks,
            List<TimingNet> nets) {

        this.width = circuit.getWidth();
        this.height = circuit.getHeight();

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

    public void calculate(int[] x, int[] y) {
        this.updateDelays(x, y);
        this.timingGraph.calculateCriticalities(false);
    }


    private void updateDelays(int[] x, int[] y) {
        for(TimingNet net : this.nets) {
            int sourceIndex = net.source.blockIndex;
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];

            int maxDeltaX = sourceCategory == BlockCategory.IO ? this.width + 1 : this.width;
            int maxDeltaY = sourceCategory == BlockCategory.IO ? this.height + 1 : this.height;

            double sourceX = x[sourceIndex];
            double sourceY = y[sourceIndex];

            for(TimingNetBlock sink : net.sinks) {
                int sinkIndex = sink.blockIndex;
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                double sinkX = x[sinkIndex];
                double sinkY = y[sinkIndex];

                int deltaX = Math.min((int) Math.abs(sinkX - sourceX), sinkCategory == BlockCategory.IO ? maxDeltaX + 1: maxDeltaX);
                int deltaY = Math.min((int) Math.abs(sinkY - sourceY), sinkCategory == BlockCategory.IO ? maxDeltaY + 1: maxDeltaY);

                double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);
                sink.timingEdge.setWireDelay(wireDelay);
            }
        }
    }
}
