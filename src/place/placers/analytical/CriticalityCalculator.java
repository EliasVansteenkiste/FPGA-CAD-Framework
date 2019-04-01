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

    public double calculate(double[] x, double[] y) {
        this.updateDelays(x, y);
        this.timingGraph.calculateCriticalities(false);
        
        return this.timingGraph.getMaxDelay();
    }

    private void updateDelays(double[] x, double[] y) {
        for(TimingNet net : this.nets) {
            int sourceIndex = net.source.blockIndex;
            BlockCategory sourceCategory = this.blockCategories[sourceIndex];

            double sourceX = x[sourceIndex];
            double sourceY = y[sourceIndex];

            for(TimingNetBlock sink : net.sinks) {
                int sinkIndex = sink.blockIndex;
                BlockCategory sinkCategory = this.blockCategories[sinkIndex];

                double sinkX = x[sinkIndex];
                double sinkY = y[sinkIndex];

                double deltaX = Math.abs(sinkX - sourceX);
                double deltaY = Math.abs(sinkY - sourceY);
                
                int intDeltaX = (int) Math.round(deltaX);
                int intDeltaY = (int) Math.round(deltaY);

                float wireDelay = this.delayTables.getDelay(sourceCategory, sinkCategory, intDeltaX, intDeltaY);
                sink.timingEdge.setWireDelay(wireDelay);
            }
        }
    }
}
