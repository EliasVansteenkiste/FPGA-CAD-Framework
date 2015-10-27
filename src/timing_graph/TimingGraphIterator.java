package timing_graph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import architecture.circuit.block.GlobalBlock;


public class TimingGraphIterator implements Iterator<TimingGraphEntry> {

    private GlobalBlock sourceBlock;
    private Iterator<Map.Entry<GlobalBlock, List<TimingNode>>> sourceBlockIterator;

    private TimingNode sourceNode;
    private Iterator<TimingNode> sourceNodeIterator;

    private Iterator<Map.Entry<TimingNode, TimingEdge>> sinkNodeIterator;

    private TimingGraphEntry cachedEntry = null;


    TimingGraphIterator(Map<GlobalBlock, List<TimingNode>> nodesInGlobalBlocks) {
        this.sourceBlockIterator = nodesInGlobalBlocks.entrySet().iterator();

        Map.Entry<GlobalBlock, List<TimingNode>> sourceBlockEntry = this.sourceBlockIterator.next();
        this.sourceBlock = sourceBlockEntry.getKey();
        this.sourceNodeIterator = sourceBlockEntry.getValue().iterator();

        this.sourceNode = this.sourceNodeIterator.next();
        this.sinkNodeIterator = this.sourceNode.sinks.entrySet().iterator();
    }

    public boolean hasNext() {
        if(this.cachedEntry != null) {
            return true;
        }

        while(!this.sinkNodeIterator.hasNext()) {

            while(!this.sourceNodeIterator.hasNext()) {
                if(!this.sourceBlockIterator.hasNext()) {
                    return false;
                }

                Map.Entry<GlobalBlock, List<TimingNode>> sourceBlockEntry = this.sourceBlockIterator.next();
                this.sourceBlock = sourceBlockEntry.getKey();
                this.sourceNodeIterator = sourceBlockEntry.getValue().iterator();
            }

            this.sourceNode = this.sourceNodeIterator.next();
            this.sinkNodeIterator = this.sourceNode.sinks.entrySet().iterator();
        }

        Map.Entry<TimingNode, TimingEdge> sinkNodeEntry = this.sinkNodeIterator.next();

        this.cachedEntry = new TimingGraphEntry(this.sourceBlock,
                sinkNodeEntry.getKey().getOwner(),
                sinkNodeEntry.getValue().getCriticality(),
                this.sourceNode.sinks.size());

        return true;
    }

    public TimingGraphEntry next() {
        TimingGraphEntry entry = this.cachedEntry;
        this.cachedEntry = null;
        return entry;
    }

    public void remove() {
        // Not supported
    }
}
