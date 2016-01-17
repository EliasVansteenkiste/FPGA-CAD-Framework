package circuit.timing;

import java.util.ArrayList;
import java.util.List;

import circuit.architecture.BlockCategory;
import circuit.architecture.DelayTables;
import circuit.block.GlobalBlock;
import circuit.block.LeafBlock;
import circuit.pin.LeafPin;

public class TimingNode {

    public enum Position {ROOT, INTERMEDIATE, LEAF};

    private LeafBlock block;
    private GlobalBlock globalBlock;
    private LeafPin pin;

    private DelayTables delayTables;

    private Position position;

    private int numClockDomains;
    private int clockDomain = -1;


    private ArrayList<TimingNode> sources = new ArrayList<>();
    private ArrayList<TimingEdge> sourceEdges = new ArrayList<>();
    private int numSources = 0;

    private ArrayList<TimingNode> sinks = new ArrayList<>();
    private ArrayList<TimingEdge> sinkEdges = new ArrayList<>();
    private int numSinks = 0;


    private int[] clockDomainNumSources;

    private ArrayList<ArrayList<TimingNode>> clockDomainSinks = new ArrayList<>();
    private ArrayList<ArrayList<TimingEdge>> clockDomainSinkEdges = new ArrayList<>();
    private int[] clockDomainNumSinks;


    private double arrivalTime, requiredTime;


    private int numUnprocessedSources = 0;


    TimingNode(LeafBlock block, LeafPin pin, Position position, int clockDomain, DelayTables delayTables) {
        this.block = block;
        this.globalBlock = block.getGlobalParent();

        this.pin = pin;
        this.position = position;
        this.clockDomain = clockDomain;

        this.delayTables = delayTables;
    }

    void compact() {
        this.sinks.trimToSize();
        this.sinkEdges.trimToSize();

        this.clockDomainSinks.trimToSize();
        this.clockDomainSinkEdges.trimToSize();

        for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
            this.clockDomainSinks.get(clockDomain).trimToSize();
            this.clockDomainSinkEdges.get(clockDomain).trimToSize();
        }
    }

    public LeafBlock getBlock() {
        return this.block;
    }
    public GlobalBlock getGlobalBlock() {
        return this.globalBlock;
    }
    public LeafPin getPin() {
        return this.pin;
    }
    public Position getPosition() {
        return this.position;
    }


    private void addSource(TimingNode source, TimingEdge edge) {
        this.sources.add(source);
        this.sourceEdges.add(edge);
        this.numSources++;
    }
    TimingEdge addSink(TimingNode sink, double delay) {
        TimingEdge edge = new TimingEdge(delay);

        this.sinks.add(sink);
        this.sinkEdges.add(edge);
        this.numSinks++;

        sink.addSource(this, edge);

        return edge;
    }

    public List<TimingNode> getSources() {
        return this.sources;
    }
    public List<TimingNode> getSinks() {
        return this.sinks;
    }

    List<TimingNode> getSinks(int clockDomain) {
        return this.clockDomainSinks.get(clockDomain);
    }

    public int getNumSources() {
        return this.numSources;
    }
    public int getNumSinks() {
        return this.numSinks;
    }

    public TimingNode getSource(int sourceIndex) {
        return this.sources.get(sourceIndex);
    }
    public TimingNode getSink(int sinkIndex) {
        return this.sinks.get(sinkIndex);
    }

    public TimingEdge getSourceEdge(int sourceIndex) {
        return this.sourceEdges.get(sourceIndex);
    }
    public TimingEdge getSinkEdge(int sinkIndex) {
        return this.sinkEdges.get(sinkIndex);
    }


    void addClockDomainSource(List<Integer> clockDomains) {
        for(int clockDomain : clockDomains) {
            this.clockDomainNumSources[clockDomain]++;
        }
    }
    void addClockDomainSink(int clockDomain, TimingNode sink, TimingEdge edge) {
        this.clockDomainSinks.get(clockDomain).add(sink);
        this.clockDomainSinkEdges.get(clockDomain).add(edge);
        this.clockDomainNumSinks[clockDomain]++;
    }


    void setNumClockDomains(int numClockDomains) {
        this.numClockDomains = numClockDomains;

        this.clockDomainNumSources = new int[numClockDomains];
        this.clockDomainNumSinks = new int[numClockDomains];

        for(int clockDomain = 0; clockDomain < numClockDomains; clockDomain++) {
            this.clockDomainSinks.add(clockDomain, new ArrayList<TimingNode>());
            this.clockDomainSinkEdges.add(clockDomain, new ArrayList<TimingEdge>());
        }
    }

    int getClockDomain() {
        return this.clockDomain;
    }

    int[] getClockDomainNumSources() {
        return this.clockDomainNumSources;
    }
    int[] getClockDomainNumSinks() {
        return this.clockDomainNumSinks;
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



    void resetArrivalTime() {
        this.arrivalTime = 0;
    }
    double getArrivalTime() {
        return this.arrivalTime;
    }

    double updateSinkArrivalTimes(int clockDomain) {
        double maxArrivalTime = 0;

        List<TimingNode> sinks = this.clockDomainSinks.get(clockDomain);
        List<TimingEdge> edges = this.clockDomainSinkEdges.get(clockDomain);
        int numSinks = this.clockDomainNumSinks[clockDomain];

        for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
            TimingNode sink = sinks.get(sinkIndex);
            TimingEdge edge = edges.get(sinkIndex);

            double sinkArrivalTime = this.arrivalTime + edge.getTotalDelay();
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

        List<TimingNode> sinks = this.clockDomainSinks.get(sinkClockDomain);
        List<TimingEdge> edges = this.clockDomainSinkEdges.get(sinkClockDomain);
        int numSinks = this.clockDomainNumSinks[sinkClockDomain];

        for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
            TimingNode sink = sinks.get(sinkIndex);

            double thisRequiredTime = sink.requiredTime - edges.get(sinkIndex).getTotalDelay();
            if(thisRequiredTime < this.requiredTime) {
                this.requiredTime = thisRequiredTime;
            }

            TimingEdge edge = edges.get(sinkIndex);
            double slack = sink.requiredTime - this.arrivalTime - edge.getTotalDelay();
            if(slack < edge.getSlack()) {
                edge.setSlack(slack);
            }
        }
    }




    void calculateSinkWireDelays() {
        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            TimingNode sink = this.sinks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            double wireDelay = this.calculateWireDelay(sink);
            edge.setWireDelay(wireDelay);
        }
    }

    private double calculateWireDelay(TimingNode node) {
        int deltaX = Math.abs(this.globalBlock.getColumn() - node.globalBlock.getColumn());
        int deltaY = Math.abs(this.globalBlock.getRow() - node.globalBlock.getRow());

        BlockCategory fromCategory = this.globalBlock.getCategory();
        BlockCategory toCategory = node.globalBlock.getCategory();

        return this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
    }



    /*************************************************
     * Functions that facilitate simulated annealing *
     *************************************************/

    double calculateDeltaCost(GlobalBlock otherBlock) {
        /*
         * When this method is called, we assume that this block and
         * the block with which this block will be swapped, already
         * have their positions updated (temporarily).
         */
        double cost = 0;

        for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
            TimingNode sink = this.sinks.get(sinkIndex);
            TimingEdge edge = this.sinkEdges.get(sinkIndex);

            cost += this.calculateDeltaCost(sink, edge);
        }

        for(int sourceIndex = 0; sourceIndex < this.numSources; sourceIndex++) {
            TimingNode source = this.sources.get(sourceIndex);

            if(source.globalBlock != otherBlock) {
                TimingEdge edge = this.sourceEdges.get(sourceIndex);
                cost += this.calculateDeltaCost(source, edge);
            }
        }

        return cost;
    }

    private double calculateDeltaCost(TimingNode otherNode, TimingEdge edge) {
        if(otherNode.globalBlock == this.globalBlock) {
            edge.resetStagedDelay();
            return 0;

        } else {
            double wireDelay = this.calculateWireDelay(otherNode);
            edge.setStagedWireDelay(wireDelay);
            return edge.getDeltaCost();
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



    @Override
    public String toString() {
        return this.pin.toString();
    }
}
