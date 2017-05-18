package place.placers.analytical;

//import java.util.ArrayList;
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
    private TimingGraph timingGraph;
    private DelayTables delayTables;
    private BlockCategory[] blockCategories;

    private List<TimingNet> nets;

    CriticalityCalculator(
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

    public double calculate(int[] x, int[] y) {
        this.updateDelays(x, y);
        this.timingGraph.calculateCriticalities(false);
        
        return this.timingGraph.getMaxDelay();
    }

    private void updateDelays(int[] x, int[] y) {
        for(TimingNet net : this.nets) {
            int sourceIndex = net.source.blockIndex;
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];

            int sourceX = x[sourceIndex];
            int sourceY = y[sourceIndex];

            for(TimingNetBlock sink : net.sinks) {
                int sinkIndex = sink.blockIndex;
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                int sinkX = x[sinkIndex];
                int sinkY = y[sinkIndex];

                int deltaX = Math.abs(sinkX - sourceX);
                int deltaY = Math.abs(sinkY - sourceY);

                double wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, deltaX, deltaY);
                sink.timingEdge.setWireDelay(wireDelay);
            }
        }
    }
}
