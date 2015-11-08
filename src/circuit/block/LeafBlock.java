package circuit.block;

import java.util.ArrayList;
import java.util.List;

import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.architecture.DelayTables;

public class LeafBlock extends IntermediateBlock {

    private GlobalBlock globalParent;

    private ArrayList<LeafBlock> sourceBlocks = new ArrayList<LeafBlock>();
    private ArrayList<LeafBlock> sinkBlocks = new ArrayList<LeafBlock>();
    private ArrayList<TimingEdge> sourceEdges = new ArrayList<TimingEdge>();
    private ArrayList<TimingEdge> sinkEdges = new ArrayList<TimingEdge>();
    private int numSources = 0, numSinks = 0;

    private double arrivalTime, requiredTime;
    private int numProcessedSources, numProcessedSinks;


    public LeafBlock(String name, BlockType type, int index, AbstractBlock parent, GlobalBlock globalParent) {
        super(name, type, index, parent);

        this.globalParent = globalParent;
        this.globalParent.addLeaf(this);
    }

    @Override
    public void compact() {
        this.sourceBlocks.trimToSize();
        this.sinkBlocks.trimToSize();
        this.sourceEdges.trimToSize();
        this.sinkEdges.trimToSize();
    }

    public GlobalBlock getGlobalParent() {
        return this.globalParent;
    }

    public int getX() {
        return this.globalParent.getX();
    }
    public int getY() {
        return this.globalParent.getY();
    }


    void addSink(LeafBlock sink, double fixedDelay) {
        int sinkIndex = this.sinkBlocks.indexOf(sink);

        if(sinkIndex == -1) {
            TimingEdge edge = new TimingEdge(fixedDelay);
            sink.addSource(this, edge);

            this.sinkBlocks.add(sink);
            this.sinkEdges.add(edge);
            this.numSinks++;

        } else {
            TimingEdge edge = this.sinkEdges.get(sinkIndex);
            if(fixedDelay > edge.getFixedDelay()) {
                edge.setFixedDelay(fixedDelay);
            }
        }
    }

    void addSource(LeafBlock source, TimingEdge edge) {
        this.sourceBlocks.add(source);
        this.sourceEdges.add(edge);
        this.numSources++;
    }


    int getNumSources() {
        return this.numSources;
    }
    int getNumSinks() {
        return this.numSinks;
    }

    LeafBlock getSource(int i) {
        return this.sourceBlocks.get(i);
    }
    LeafBlock getSink(int i) {
        return this.sinkBlocks.get(i);
    }

    List<LeafBlock> getSources() {
        return this.sourceBlocks;
    }
    List<TimingEdge> getSourceEdges() {
        return this.sourceEdges;
    }

    TimingEdge getSourceEdge(int i) {
        return this.sourceEdges.get(i);
    }
    TimingEdge getSinkEdge(int i) {
        return this.sinkEdges.get(i);
    }

    List<LeafBlock> getSinks() {
        return this.sinkBlocks;
    }
    List<TimingEdge> getSinkEdges() {
        return this.sinkEdges;
    }


    double calculateArrivalTime() {
        for(int sourceIndex = 0; sourceIndex < this.numSources; sourceIndex++) {
            LeafBlock source = this.sourceBlocks.get(sourceIndex);
            TimingEdge edge = this.sourceEdges.get(sourceIndex);

            double sourceArrivalTime = source.isClocked() ? 0 : source.arrivalTime;
            double delay = edge.getTotalDelay();

            double arrivalTime = sourceArrivalTime + delay;
            if(arrivalTime > this.arrivalTime) {
                this.arrivalTime = arrivalTime;
            }
        }

        return this.arrivalTime;
    }

    double getArrivalTime() {
        return this.arrivalTime;
    }

    void setRequiredTime(double requiredTime) {
        this.requiredTime = requiredTime;
    }

    double calculateRequiredTime(double maxDelay) {
        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            LeafBlock sink = this.sinkBlocks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            double sinkRequiredTime = sink.isClocked() ? maxDelay : sink.requiredTime;
            double delay = edge.getTotalDelay();

            double requiredTime = sinkRequiredTime - delay;
            if(requiredTime < this.requiredTime) {
                this.requiredTime = requiredTime;
            }
        }

        return this.requiredTime;
    }


    void resetTiming() {
        this.arrivalTime = 0;
        this.requiredTime = Double.MAX_VALUE;
        this.numProcessedSources = 0;
        this.numProcessedSinks = 0;
    }

    void incrementProcessedSources() {
        this.numProcessedSources++;
    }
    void incrementProcessedSinks() {
        this.numProcessedSinks++;
    }
    boolean allSourcesProcessed() {
        return this.numSources == this.numProcessedSources;
    }
    boolean allSinksProcessed() {
        return this.numSinks == this.numProcessedSinks;
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
        return this.calculateWireDelay(otherBlock, this.getX(), this.getY());
    }

    private double calculateWireDelay(LeafBlock otherBlock, int newX, int newY) {
        int deltaX = Math.abs(newX - otherBlock.getX());
        int deltaY = Math.abs(newY - otherBlock.getY());

        BlockCategory fromCategory = this.globalParent.getCategory();
        BlockCategory toCategory = otherBlock.globalParent.getCategory();

        return DelayTables.getInstance().getDelay(fromCategory, toCategory, deltaX, deltaY);
    }



    void calculateCriticalities(double maxArrivalTime, double criticalityExponent) {
        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            LeafBlock sink = this.sinkBlocks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            double slack = sink.requiredTime - this.arrivalTime - edge.getTotalDelay();
            double criticality = 1 - slack / maxArrivalTime;
            edge.setCriticality(Math.pow(criticality, criticalityExponent));
        }
    }



    double calculateCost() {
        double cost = 0;

        for(TimingEdge edge : this.sinkEdges) {
            cost += edge.getCriticality() * edge.getTotalDelay();
        }

        return cost;
    }

    double calculateDeltaCost(int newX, int newY) {
        double cost = 0;

        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            LeafBlock sink = this.sinkBlocks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            cost += this.calculateDeltaCost(newX, newY, sink, edge);
        }

        for(int sourceIndex = 0; sourceIndex < this.numSources; sourceIndex++) {
            // Only calculate the delta cost if the source is not in the block where we would swap to
            // This is necessary to avoid double counting: the other swap block also calculates delta
            // costs of all sink edges
            LeafBlock source = this.sourceBlocks.get(sourceIndex);
            if(!(source.getX() == newX && source.getY() == newY)) {
                TimingEdge edge = this.sourceEdges.get(sourceIndex);

                cost += this.calculateDeltaCost(newX, newY, source, edge);
            }
        }

        return cost;
    }

    private double calculateDeltaCost(int newX, int newY, LeafBlock otherBlock, TimingEdge edge) {
        double wireDelay = this.calculateWireDelay(otherBlock, newX, newY);
        edge.setStagedWireDelay(wireDelay);
        return edge.getCriticality() * (edge.getStagedTotalDelay() - edge.getTotalDelay());
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
