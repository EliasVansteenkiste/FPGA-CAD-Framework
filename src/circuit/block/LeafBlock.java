package circuit.block;

import java.util.ArrayList;
import java.util.List;

import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.architecture.DelayTables;
import circuit.architecture.PortType;
import circuit.pin.LeafPin;

public class LeafBlock extends LocalBlock {

    private GlobalBlock globalParent;


    public LeafBlock(String name, BlockType type, int index, AbstractBlock parent, GlobalBlock globalParent) {
        super(name, type, index, parent);

        this.globalParent = globalParent;
        this.globalParent.addLeaf(this);
    }

    @Override
    protected LeafPin createPin(PortType portType, int index) {
        return new LeafPin(this, portType, index);
    }

    public GlobalBlock getGlobalParent() {
        return this.globalParent;
    }






    /**********************************************
     * Functions that support simulated annealing *
     **********************************************/

    double calculateDeltaCost(GlobalBlock otherBlock) {
        /*
         * When this method is called, we assume that this block and
         * the block with which this block will be swapped, already
         * have their positions updated (temporarily).
         */
        double cost = 0;

        int sinkIndex = 0;
        for(LeafBlock sink : this.sinkBlocks) {
            TimingEdge edge = this.sinkEdges.get(sinkIndex);
            cost += this.calculateDeltaCost(sink, edge);

            sinkIndex++;
        }

        int sourceIndex = 0;
        for(LeafBlock source : this.sourceBlocks) {
            // Only calculate the delta cost if the source is not in the block where we would swap to
            // This is necessary to avoid double counting: the other swap block also calculates delta
            // costs of all sink edges
            if(source.getGlobalParent() != otherBlock) {
                TimingEdge edge = this.sourceEdges.get(sourceIndex);
                cost += this.calculateDeltaCost(source, edge);
            }

            sourceIndex++;
        }

        return cost;
    }

    private double calculateDeltaCost(LeafBlock otherBlock, TimingEdge edge) {
        if(otherBlock.globalParent == this.globalParent) {
            edge.resetStagedDelay();
            return 0;

        } else {
            double wireDelay = this.calculateWireDelay(otherBlock);
            edge.setStagedWireDelay(wireDelay);
            return edge.getCriticality() * (edge.getStagedTotalDelay() - edge.getTotalDelay());
        }
    }


    void pushThrough() {
        for(TimingEdge edge : this.sinkEdges) {
            edge.pushThrough();
        }
        for(TimingEdge edge : this.sourceEdges) {
            edge.pushThrough();
        }
    }
}
