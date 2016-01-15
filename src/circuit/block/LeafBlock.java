package circuit.block;

import java.util.ArrayList;
import java.util.List;

import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.architecture.DelayTables;

public class LeafBlock extends IntermediateBlock {

    private DelayTables delayTables;
    private GlobalBlock globalParent;

    private int clockDomain = -1;
    private int numClockDomains;

    private ArrayList<LeafBlock> sourceBlocks = new ArrayList<>();
    private ArrayList<TimingEdge> sourceEdges = new ArrayList<>();
    private int numSources = 0;
    private int[] clockDomainNumSources;

    private ArrayList<LeafBlock> sinkBlocks = new ArrayList<>();
    private ArrayList<TimingEdge> sinkEdges = new ArrayList<>();
    private int numSinks = 0;

    private ArrayList<ArrayList<LeafBlock>> clockDomainSinkBlocks = new ArrayList<>();
    private ArrayList<ArrayList<TimingEdge>> clockDomainSinkEdges = new ArrayList<>();
    private int[] clockDomainNumSinks;

    private int[] sinkEdgesPinStarts;
    private int currentPinIndex;

    private double arrivalTime, requiredTime;
    private int numUnprocessedSources;


    public LeafBlock(DelayTables delayTables, String name, BlockType type, int index, AbstractBlock parent, GlobalBlock globalParent) {
        super(name, type, index, parent);

        this.delayTables = delayTables;
        this.globalParent = globalParent;
        this.globalParent.addLeaf(this);

        this.sinkEdgesPinStarts = new int[this.numOutputPins() + 1];
        this.sinkEdgesPinStarts[0] = 0;
        this.currentPinIndex = 0;
    }

    @Override
    public void compact() {
        this.sinkBlocks.trimToSize();
        this.sinkEdges.trimToSize();

        this.clockDomainSinkBlocks.trimToSize();
        this.clockDomainSinkEdges.trimToSize();

        for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
            this.clockDomainSinkBlocks.get(clockDomain).trimToSize();
            this.clockDomainSinkEdges.get(clockDomain).trimToSize();
        }

        int numOutputPins = this.numOutputPins();
        for(; this.currentPinIndex < numOutputPins; this.currentPinIndex++) {
            this.sinkEdgesPinStarts[this.currentPinIndex + 1] = this.numSinks;
        }
    }

    public GlobalBlock getGlobalParent() {
        return this.globalParent;
    }

    public int getColumn() {
        return this.globalParent.getColumn();
    }
    public int getY() {
        return this.globalParent.getRow();
    }

    void setNumClockDomains(int numClockDomains) {
        this.numClockDomains = numClockDomains;
        this.clockDomainNumSources = new int[numClockDomains];
        this.clockDomainNumSinks = new int[numClockDomains];

        for(int clockDomain = 0; clockDomain < numClockDomains; clockDomain++) {
            this.clockDomainSinkBlocks.add(clockDomain, new ArrayList<LeafBlock>());
            this.clockDomainSinkEdges.add(clockDomain, new ArrayList<TimingEdge>());
        }
    }

    void setClockDomain(int clockDomain) {
        this.clockDomain = clockDomain;
    }
    int getClockDomain() {
        return this.clockDomain;
    }

    /*void addSinkClockDomains(int[] sinkNumSinksPerClockDomain) {
        for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
            if(sinkNumSinksPerClockDomain[clockDomain] > 0) {
                this.clockDomainNumSinks[clockDomain]++;
            }
        }
    }*/

    int[] getClockDomainNumSources() {
        return this.clockDomainNumSources;
    }
    int[] getClockDomainNumSinks() {
        return this.clockDomainNumSinks;
    }

    void addClockDomainSink(int clockDomain, LeafBlock sink, TimingEdge edge) {
        this.clockDomainSinkBlocks.get(clockDomain).add(sink);
        this.clockDomainSinkEdges.get(clockDomain).add(edge);
        this.clockDomainNumSinks[clockDomain]++;
    }





    public int[] getSinkRange(int pinIndex) {
        int[] range = new int[2];
        range[0] = this.sinkEdgesPinStarts[pinIndex];
        range[1] = this.sinkEdgesPinStarts[pinIndex + 1];
        return range;
    }


    TimingEdge addSink(int pinIndex, LeafBlock sink, double fixedDelay) throws IllegalArgumentException {
        /*
         * This method assumes that pinIndex is always smaller than or
         * equal to the smallest pinIndex encountered so far. In other
         * words: sinks must be added in ascending order of output pin
         * index.
         */
        if(pinIndex < this.currentPinIndex) {
            throw new IllegalArgumentException("sink was not added in ascending pin index order");

        } else {
            for(; this.currentPinIndex < pinIndex; this.currentPinIndex++) {
                this.sinkEdgesPinStarts[this.currentPinIndex + 1] = this.numSinks;
            }
        }


        TimingEdge edge = new TimingEdge(fixedDelay);
        sink.addSource(this, edge);

        this.sinkBlocks.add(sink);
        this.sinkEdges.add(edge);
        this.numSinks++;

        return edge;
    }

    void addSource(LeafBlock block, TimingEdge edge) {
        this.sourceBlocks.add(block);
        this.sourceEdges.add(edge);
        this.numSources++;
    }
    void addClockDomainSource(List<Integer> sourceClockDomains) {
        for(int clockDomain : sourceClockDomains) {
            this.clockDomainNumSources[clockDomain]++;
        }
    }



    List<LeafBlock> getSinks() {
        return this.sinkBlocks;
    }
    public List<LeafBlock> getSinks(int pinIndex) {
        int[] sinkRange = this.getSinkRange(pinIndex);
        return this.sinkBlocks.subList(sinkRange[0], sinkRange[1]);
    }

    int getNumSources() {
        return this.numSources;
    }
    public int getNumSinks() {
        return this.numSinks;
    }
    public int getNumSinks(int pinIndex) {
        int[] sinkRange = this.getSinkRange(pinIndex);
        return sinkRange[1] - sinkRange[0];
    }

    public LeafBlock getSink(int index) {
        return this.sinkBlocks.get(index);
    }

    public TimingEdge getSinkEdge(int index) {
        return this.sinkEdges.get(index);
    }

    double updateSinkArrivalTimes(int clockDomain) {
        double maxArrivalTime = 0;

        List<LeafBlock> sinks = this.clockDomainSinkBlocks.get(clockDomain);
        List<TimingEdge> edges = this.clockDomainSinkEdges.get(clockDomain);
        int numSinks = this.clockDomainNumSinks[clockDomain];

        for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
            LeafBlock sink = sinks.get(sinkIndex);
            TimingEdge edge = edges.get(sinkIndex);

            double thisArrivalTime = this.isClocked() ? 0 : this.arrivalTime;
            double sinkArrivalTime = thisArrivalTime + edge.getTotalDelay();
            if(sinkArrivalTime > sink.arrivalTime) {
                sink.arrivalTime = sinkArrivalTime;

                if(sinkArrivalTime > maxArrivalTime) {
                    maxArrivalTime = sinkArrivalTime;
                }
            }
        }

        return maxArrivalTime;
    }

    void updateSlacks(int sinkClockDomain) {

        this.requiredTime = Double.MAX_VALUE;

        List<LeafBlock> sinks = this.clockDomainSinkBlocks.get(sinkClockDomain);
        List<TimingEdge> edges = this.clockDomainSinkEdges.get(sinkClockDomain);
        int numSinks = this.clockDomainNumSinks[sinkClockDomain];

        double thisArrivalTime = this.isClocked() ? 0 : this.arrivalTime;

        for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
            LeafBlock sink = sinks.get(sinkIndex);
            double sinkRequiredTime = sink.isClocked() ? 0 : sink.requiredTime;

            double thisRequiredTime = sinkRequiredTime - edges.get(sinkIndex).getTotalDelay();
            if(thisRequiredTime < this.requiredTime) {
                this.requiredTime = thisRequiredTime;
            }

            TimingEdge edge = edges.get(sinkIndex);
            double slack = sinkRequiredTime - thisArrivalTime - edge.getTotalDelay();
            if(slack < edge.getSlack()) {
                edge.setSlack(slack);
            }
        }
    }


    /*double calculateArrivalTime() {

        double maxArrivalTime = this.arrivalTime;

        for(int sourceIndex = 0; sourceIndex < this.numSources; sourceIndex++) {
            LeafBlock source = this.sourceBlocks.get(sourceIndex);
            TimingEdge edge = this.sourceEdges.get(sourceIndex);

            double sourceArrivalTime = source.arrivalTime;
            double delay = edge.getTotalDelay();

            double arrivalTime = sourceArrivalTime + delay;

            if(arrivalTime > maxArrivalTime) {
                maxArrivalTime = arrivalTime;
            }
        }

        // We don't store the arrival time for clocked blocks.
        // When using the clocked block as a sink, we never need to
        // know the arrival time. When using the block as a source,
        // the arrival time is 0. So we just leave it to 0.
        if(!this.isClocked()) {
            this.arrivalTime = maxArrivalTime;
        }

        return maxArrivalTime;
    }*/

    double getArrivalTime() {
        return this.arrivalTime;
    }

    /*void setRequiredTime(double requiredTime) {
        this.requiredTime = requiredTime;
    }

    double calculateRequiredTime() {

        double minRequiredTime = Double.MAX_VALUE;

        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            LeafBlock sink = this.sinkBlocks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            double sinkRequiredTime = sink.requiredTime;
            double delay = edge.getTotalDelay();

            double requiredTime = sinkRequiredTime - delay;
            if(requiredTime < minRequiredTime) {
                minRequiredTime = requiredTime;
            }
        }

        // We don't store the arrival time for clocked blocks.
        // When using the clocked block as a source, we never need to
        // know the required time. When using the block as a sink,
        // the arrival time is equal to maxDelay, and has been set
        // manually by the timing graph. So we just leave it to that
        // value.
        if(!this.isClocked()) {
            this.requiredTime = minRequiredTime;
        }

        return minRequiredTime;
    }*/


    void resetArrivalTime() {
        this.arrivalTime = 0;
    }

    void resetProcessedSources() {
        this.numUnprocessedSources = this.numSources;
    }
    void resetProcessedSources(int clockDomain) {
        this.numUnprocessedSources = this.clockDomainNumSources[clockDomain];
    }
    void setTraversalRoot() {
        this.numUnprocessedSources = -1;
    }

    void incrementProcessedSources() {
        this.numUnprocessedSources--;
    }
    boolean allSourcesProcessed() {
        return this.numUnprocessedSources == 0;
    }



    void calculateSinkWireDelays() {
        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            LeafBlock sink = this.sinkBlocks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            double wireDelay = this.calculateWireDelay(sink);
            edge.setWireDelay(wireDelay);
        }
    }

    private double calculateWireDelay(LeafBlock otherBlock) {
        int deltaX = Math.abs(this.getColumn() - otherBlock.getColumn());
        int deltaY = Math.abs(this.getY() - otherBlock.getY());

        BlockCategory fromCategory = this.globalParent.getCategory();
        BlockCategory toCategory = otherBlock.globalParent.getCategory();

        return this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
    }



    /*void calculateCriticalities(double maxArrivalTime, double criticalityExponent) {
        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            LeafBlock sink = this.sinkBlocks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            double slack = sink.requiredTime - this.arrivalTime - edge.getTotalDelay();
            double criticality = 1 - slack / maxArrivalTime;
            edge.setCriticality(Math.pow(criticality, criticalityExponent));
        }
    }*/



    double calculateCost() {
        double cost = 0;

        for(TimingEdge edge : this.sinkEdges) {
            cost += edge.getCriticality() * edge.getTotalDelay();
        }

        return cost;
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
