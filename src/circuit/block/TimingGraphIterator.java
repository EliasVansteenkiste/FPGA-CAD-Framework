package circuit.block;

import java.util.Iterator;
import java.util.List;

class TimingGraphIterator implements Iterator<TimingGraphEntry> {

    private GlobalBlock sourceGlobalBlock;
    private Iterator<GlobalBlock> sourceGlobalBlockIterator;

    private LeafBlock sourceBlock;
    private Iterator<LeafBlock> sourceBlockIterator;

    private int maxSinkIndex, sinkIndex;

    private TimingGraphEntry cachedEntry = null;


    TimingGraphIterator(List<GlobalBlock> globalBlocks) {
        this.sourceGlobalBlockIterator = globalBlocks.iterator();

        this.sourceGlobalBlock = this.sourceGlobalBlockIterator.next();
        this.sourceBlockIterator = this.sourceGlobalBlock.getLeafBlocks().iterator();

        this.sourceBlock = this.sourceBlockIterator.next();
        this.maxSinkIndex = this.sourceBlock.getNumSinks();
        this.sinkIndex = 0;
    }

    @Override
    public boolean hasNext() {
        if(this.cachedEntry != null) {
            return true;
        }

        while(this.sinkIndex < this.maxSinkIndex) {

            while(!this.sourceBlockIterator.hasNext()) {
                if(!this.sourceGlobalBlockIterator.hasNext()) {
                    return false;
                }

                this.sourceGlobalBlock = this.sourceGlobalBlockIterator.next();
                this.sourceBlockIterator = this.sourceGlobalBlock.getLeafBlocks().iterator();
            }

            this.sourceBlock = this.sourceBlockIterator.next();
            this.maxSinkIndex = this.sourceBlock.getNumSinks();
            this.sinkIndex = 0;
        }

        LeafBlock sink = this.sourceBlock.getSink(this.sinkIndex);
        TimingEdge edge = this.sourceBlock.getSinkEdge(this.sinkIndex);
        this.sinkIndex++;

        this.cachedEntry = new TimingGraphEntry(
                this.sourceGlobalBlock,
                sink.getGlobalParent(),
                edge.getCriticality(),
                this.sourceBlock.getNumSinks());

        return true;
    }

    @Override
    public TimingGraphEntry next() {
        TimingGraphEntry entry = this.cachedEntry;
        this.cachedEntry = null;
        return entry;
    }

    @Override
    public void remove() {
        // Not supported
    }
}
