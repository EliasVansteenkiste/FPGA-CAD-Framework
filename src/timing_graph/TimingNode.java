package timing_graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import architecture.circuit.block.GlobalBlock;
import architecture.circuit.block.BlockType.BlockCategory;

public class TimingNode implements Comparable<TimingNode> {

    private DelayTables delayTables;
    private GlobalBlock owner;
    private String name;
    private boolean isClocked;

    /*
     * For every source and sink, 5 values are stored: - the fixed delay,
     * assuming the source and sink are in the same block - the total delay,
     * also counting the delay that arises between global blocks - the
     * criticality of the connection, including criticality exponent - a new
     * total delay that takes effect when a staged swap is applied - a new
     * criticality that takes effect when a staged swap is applied
     */
    Map<TimingNode, TimingEdge> sources = new HashMap<TimingNode, TimingEdge>();
    Map<TimingNode, TimingEdge> sinks = new HashMap<TimingNode, TimingEdge>();

    private double arrivalTime, requiredTime;
    private int numProcessedSources, numProcessedSinks;


    TimingNode(DelayTables delayTables, GlobalBlock owner, String name, boolean isClocked) {
        this.delayTables = delayTables;
        this.owner = owner;
        this.name = name;
        this.isClocked = isClocked;
    }

    GlobalBlock getOwner() {
        return this.owner;
    }

    boolean isClocked() {
        return this.isClocked;
    }

    void addSink(TimingNode sink, double fixedDelay) {
        TimingEdge timingEdge = this.sinks.get(sink);
        if(timingEdge == null) {
            timingEdge = new TimingEdge(fixedDelay);
            this.sinks.put(sink, timingEdge);
            sink.addSource(this, timingEdge);

        } else {
            if(timingEdge.getFixedDelay() < fixedDelay) {
                timingEdge.setFixedDelay(fixedDelay);
            }
        }
    }

    private void addSource(TimingNode source, TimingEdge timingEdge) {
        this.sources.put(source, timingEdge);
    }

    Set<TimingNode> getSources() {
        return this.sources.keySet();
    }

    Set<TimingNode> getSinks() {
        return this.sinks.keySet();
    }

    Map<TimingNode, TimingEdge> getSinksAndEdges() {
        return this.sinks;
    }

    double calculateArrivalTime() {
        for(Map.Entry<TimingNode, TimingEdge> sourceEntry : this.sources.entrySet()) {
            TimingNode source = sourceEntry.getKey();
            Double sourceArrivalTime = source.isClocked ? 0 : source.arrivalTime;
            Double delay = sourceEntry.getValue().getTotalDelay();

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
        for(Map.Entry<TimingNode, TimingEdge> sinkEntry : this.sinks.entrySet()) {
            TimingNode sink = sinkEntry.getKey();
            Double sinkRequiredTime = sink.isClocked ? maxDelay : sink.requiredTime;
            Double delay = sinkEntry.getValue().getTotalDelay();

            double requiredTime = sinkRequiredTime - delay;
            if(requiredTime < this.requiredTime) {
                this.requiredTime = requiredTime;
            }
        }

        return this.requiredTime;
    }


    void reset() {
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
        return this.sources.size() == this.numProcessedSources;
    }
    boolean allSinksProcessed() {
        return this.sinks.size() == this.numProcessedSinks;
    }



    void calculateSinkWireDelays() {
        for(Map.Entry<TimingNode, TimingEdge> sinkEntry : this.sinks.entrySet()) {
            TimingNode sink = sinkEntry.getKey();
            TimingEdge edge = sinkEntry.getValue();

            double wireDelay = this.calculateWireDelay(sink);
            edge.setWireDelay(wireDelay);
        }
    }

    private double calculateWireDelay(TimingNode otherNode) {
        return this.calculateWireDelay(otherNode, this.owner.getX(), this.owner.getY());
    }

    private double calculateWireDelay(TimingNode otherNode, int newX, int newY) {
        int deltaX = Math.abs(newX - otherNode.owner.getX());
        int deltaY = Math.abs(newY - otherNode.owner.getY());
        BlockCategory fromCategory = this.owner.getCategory();
        BlockCategory toCategory = otherNode.owner.getCategory();

        return this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
    }



    void calculateCriticalities(double maxArrivalTime, double criticalityExponent) {
        for(Map.Entry<TimingNode, TimingEdge> sinkEntry : this.sinks.entrySet()) {
            TimingNode sink = sinkEntry.getKey();
            TimingEdge edge = sinkEntry.getValue();

            double slack = sink.requiredTime - this.arrivalTime - edge.getTotalDelay();
            double criticality = 1 - slack / maxArrivalTime;
            edge.setCriticality(Math.pow(criticality, criticalityExponent));
        }
    }



    double calculateCost() {
        double cost = 0;

        for(TimingEdge edge : this.sinks.values()) {
            cost += edge.getCriticality() * edge.getTotalDelay();
        }

        return cost;
    }

    double calculateDeltaCost(int newX, int newY) {
        double cost = 0;

        for(Map.Entry<TimingNode, TimingEdge> sinkEntry : this.sinks.entrySet()) {
            TimingNode sink = sinkEntry.getKey();
            TimingEdge edge = sinkEntry.getValue();

            cost += this.calculateDeltaCost(newX, newY, sink, edge);
        }

        for(Map.Entry<TimingNode, TimingEdge> sourceEntry : this.sources.entrySet()) {
            TimingNode source = sourceEntry.getKey();

            // Only calculate the delta cost if the source is not in the block where we would swap to
            // This is necessary to avoid double counting: the other swap block also calculates delta
            // costs of all sink edges
            if(!(source.owner.getX() == newX && source.owner.getY() == newY)) {
                TimingEdge edge = sourceEntry.getValue();

                cost += this.calculateDeltaCost(newX, newY, source, edge);
            }
        }

        return cost;
    }

    private double calculateDeltaCost(int newX, int newY, TimingNode otherNode, TimingEdge edge) {
        double wireDelay = this.calculateWireDelay(otherNode, newX, newY);
        edge.setStagedWireDelay(wireDelay);
        return edge.getCriticality() * (edge.getStagedTotalDelay() - edge.getTotalDelay());
    }


    public void pushThrough() {
        for(TimingEdge edge : this.sinks.values()) {
            edge.pushThrough();
        }
        for(TimingEdge edge : this.sources.values()) {
            edge.pushThrough();
        }
    }


    public String getName() {
        return this.name;
    }


    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public int compareTo(TimingNode otherNode) {
        return this.name.compareTo(otherNode.name);
    }
}
