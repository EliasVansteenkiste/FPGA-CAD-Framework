package circuit.timing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import placers.simulatedannealing.Swap;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.architecture.DelayTables;
import circuit.architecture.PortType;
import circuit.block.AbstractBlock;
import circuit.block.GlobalBlock;
import circuit.block.LeafBlock;
import circuit.exceptions.PlacementException;
import circuit.pin.AbstractPin;
import circuit.pin.LeafPin;
import circuit.timing.TimingNode.Position;

import util.Pair;
import util.Triple;

public class TimingGraph implements Iterable<TimingGraph.TimingGraphEntry> {

    private static String VIRTUAL_IO_CLOCK = "virtual-io-clock";

    private Circuit circuit;
    private DelayTables delayTables;

    // A map of clock domains, along with their unique id
    private Map<String, Integer> clockNamesToDomains = new HashMap<>();
    private int numClockDomains = 0;
    private int virtualIoClockDomain;

    private List<TimingNode> timingNodes = new ArrayList<>(),
                             startNodes = new ArrayList<>(),
                             affectedNodes = new ArrayList<>();

    private List<TimingEdge> timingEdges  = new ArrayList<>();
    private List<List<TimingEdge>> timingNets = new ArrayList<>();

    private List<Triple<Integer, Integer, List<TimingNode>>> traversals  = new ArrayList<>();

    private double globalMaxDelay;

    private double[] criticalityLookupTable = new double[21];

    public TimingGraph(Circuit circuit) {
        this.circuit = circuit;
        this.delayTables = this.circuit.getArchitecture().getDelayTables();

        // Create the virtual io clock domain
        this.virtualIoClockDomain = 0;
        this.clockNamesToDomains.put(VIRTUAL_IO_CLOCK, this.virtualIoClockDomain);
        this.numClockDomains = 1;

        this.setCriticalityExponent(1);
    }




    /******************************************
     * These functions build the timing graph *
     ******************************************/

    public void build() {

        this.buildGraph();

        this.setClockDomains();

        this.buildTraversals();
    }

    private void buildGraph() {

        List<Double> clockDelays = new ArrayList<Double>();

        // Create all timing nodes
        for(BlockType leafBlockType : BlockType.getLeafBlockTypes()) {
            boolean isClocked = leafBlockType.isClocked();

            for(AbstractBlock abstractBlock : this.circuit.getBlocks(leafBlockType)) {
                LeafBlock block = (LeafBlock) abstractBlock;
                boolean isConstantGenerator = isClocked ? false : this.isConstantGenerator(block);

                // Get the clock domain and clock setup time
                int clockDomain = -1;
                double clockDelay = 0;
                if(isClocked) {
                    Pair<Integer, Double> clockDomainAndDelay = this.getClockDomainAndDelay(block);
                    clockDomain = clockDomainAndDelay.getFirst();

                    // We don't include the clock setup time in the critical path delay,
                    // because VPR also doesn't does this for the critical path. When you
                    // let VPR print out the critical path the clock setup time IS included,
                    // but it isn't included in the stdout estimation.
                    //clockDelay = clockDomainAndDelay.getSecond();
                } else if(isConstantGenerator) {
                    clockDomain = this.virtualIoClockDomain;
                }

                // If the block is clocked: create TimingNodes for
                // the input pins
                if(isClocked) {
                    for(AbstractPin abstractPin : block.getInputPins()) {
                        if(abstractPin.getSource() != null) {
                            LeafPin inputPin = (LeafPin) abstractPin;
                            TimingNode node = new TimingNode(block, inputPin, Position.LEAF, clockDomain, this.delayTables);
                            inputPin.setTimingNode(node);

                            clockDelays.add(0.0);
                            this.timingNodes.add(node);
                        }
                    }
                }


                // Add the output pins of this timing graph root or intermediate node
                Position position = (isClocked || isConstantGenerator) ? Position.ROOT : Position.INTERMEDIATE;
                for(AbstractPin abstractPin : block.getOutputPins()) {
                    LeafPin outputPin = (LeafPin) abstractPin;

                    TimingNode node = new TimingNode(block, outputPin, position, clockDomain, this.delayTables);
                    outputPin.setTimingNode(node);

                    this.timingNodes.add(node);

                    if(position == Position.ROOT) {
                        clockDelays.add(clockDelay + outputPin.getPortType().getSetupTime());
                        this.startNodes.add(node);
                    } else {
                        clockDelays.add(0.0);
                    }
                }
            }
        }

        int numNodes = this.timingNodes.size();
        for(int i = 0; i < numNodes; i++) {
            TimingNode node = this.timingNodes.get(i);
            if(node.getPosition() != Position.LEAF && !this.clockNamesToDomains.containsKey(node.getPin().getOwner().getName())) {
                this.traverseFromSource(node, clockDelays.get(i));
            }
        }
    }

    private boolean isConstantGenerator(LeafBlock block) {
        for(AbstractPin inputPin : block.getInputPins()) {
            if(inputPin.getSource() != null) {
                return false;
            }
        }

        return true;
    }

    private Pair<Integer, Double> getClockDomainAndDelay(LeafBlock block) {
        /**
         * This method should only be called for clocked blocks.
         */

        String clockName;
        double clockDelay = 0;

        // If the block is an input
        if(block.getGlobalParent().getCategory() == BlockCategory.IO) {
            clockName = VIRTUAL_IO_CLOCK;

        // If the block is a regular block
        } else{
            // A clocked leaf block has exactly 1 clock pin
            List<AbstractPin> clockPins = block.getClockPins();
            assert(clockPins.size() == 1);
            AbstractPin clockPin = clockPins.get(0);

            while(true) {
                AbstractPin sourcePin = clockPin.getSource();
                if(sourcePin == null) {
                    break;
                }

                clockDelay += sourcePin.getPortType().getDelay(clockPin.getPortType());
                clockPin = sourcePin;
            }

            clockName = clockPin.getOwner().getName();
            assert(!clockName.equals(VIRTUAL_IO_CLOCK));

            if(!this.clockNamesToDomains.containsKey(clockName)) {
                this.clockNamesToDomains.put(clockName, this.numClockDomains);
                this.numClockDomains++;
            }
        }

        return new Pair<Integer, Double>(this.clockNamesToDomains.get(clockName), clockDelay);
    }

    private void traverseFromSource(TimingNode pathSourceNode, double clockDelay) {
        GlobalBlock pathSourceBlock = pathSourceNode.getGlobalBlock();
        LeafPin pathSourcePin = pathSourceNode.getPin();

        Map<GlobalBlock, List<TimingEdge>> sourceTimingNets = new HashMap<>();

        Stack<TraversePair> todo = new Stack<>();
        todo.push(new TraversePair(pathSourcePin, clockDelay));

        while(!todo.empty()) {
            TraversePair traverseEntry = todo.pop();
            AbstractPin sourcePin = traverseEntry.pin;
            double delay = traverseEntry.delay;

            AbstractBlock sourceBlock = sourcePin.getOwner();
            PortType sourcePortType = sourcePin.getPortType();

            if(this.isEndpin(sourceBlock, sourcePin, pathSourcePin)) {
                delay += sourcePortType.getSetupTime();
                TimingNode pathSinkNode = ((LeafPin) sourcePin).getTimingNode();

                // If pathSinkNode is null, this sinkPin doesn't have any sinks
                // so isn't used in the timing graph
                if(pathSinkNode != null) {
                    TimingEdge edge = pathSourceNode.addSink(pathSinkNode, delay);
                    this.timingEdges.add(edge);

                    GlobalBlock pathSinkBlock = pathSinkNode.getGlobalBlock();
                    if(pathSinkBlock != pathSourceBlock) {
                        if(!sourceTimingNets.containsKey(pathSinkBlock)) {
                            sourceTimingNets.put(pathSinkBlock, new ArrayList<TimingEdge>());
                        }
                        sourceTimingNets.get(pathSinkBlock).add(edge);
                    }
                }

            } else {
                List<AbstractPin> sinkPins;
                if(sourceBlock.isLeaf() && sourcePin != pathSourcePin) {
                    sinkPins = sourceBlock.getOutputPins();
                } else {
                    sinkPins = sourcePin.getSinks();
                }

                for(AbstractPin sinkPin : sinkPins) {
                    if(sinkPin != null) {
                        double sourceSinkDelay = sourcePortType.getDelay(sinkPin.getPortType());
                        double totalDelay = delay + sourceSinkDelay;

                        // Loops around an unclocked block are rare, but not inexistent.
                        // The proper way to handle these is probably to add the loop
                        // delay to all other output pins of this block.
                        if(sinkPin != pathSourcePin) {
                            todo.push(new TraversePair(sinkPin, totalDelay));
                        }
                    }
                }
            }
        }

        for(List<TimingEdge> timingNet : sourceTimingNets.values()) {
            this.timingNets.add(timingNet);
        }
    }

    private boolean isEndpin(AbstractBlock block, AbstractPin pin, AbstractPin pathSourcePin) {
        return block.isLeaf() && (pin != pathSourcePin) && (block.isClocked() && pin.isInput() || pin.isOutput());
    }

    private class TraversePair {
        AbstractPin pin;
        double delay;

        TraversePair(AbstractPin pin, double delay) {
            this.pin = pin;
            this.delay = delay;
        }
    }



    private void setClockDomains() {

        // Initialize all blocks
        List<TimingNode> nodes = this.timingNodes;
        for(TimingNode node : nodes) {
            node.resetProcessedSources();
            node.setNumClockDomains(this.numClockDomains);
        }

        /* Initialize the todo stack
         * block.setTraversalRoot() makes sure no block
         * is processed twice
         * */
        Stack<TimingNode> todo = new Stack<>();
        for(TimingNode node : this.startNodes) {
            node.setTraversalRoot();
            todo.add(node);
        }


        /* For each node and clock domain: store how many sources
         * that node has that have a reverse path to a node that
         * is clocked on that clock domain.
         *
         * At the same time, build a traversal regardless of clock
         * domains. The traversal is a regular list. The order of
         * nodes is chosen such that for each connection between
         * two nodes, the source node always comes before the
         * sink node. Endpoint nodes are not added to the list.
         */
        List<TimingNode> traversal = new ArrayList<>();
        while(!todo.empty()) {
            TimingNode node = todo.pop();

            if(node.getPosition() != Position.LEAF) {
                traversal.add(node);

                List<Integer> clockDomains = this.getSourceClockDomains(node);
                for(TimingNode sink : node.getSinks()) {
                    sink.addClockDomainSource(clockDomains);

                    sink.incrementProcessedSources();
                    if(sink.allSourcesProcessed()) {
                        todo.add(sink);
                    }
                }
            }
        }

        /* For each block and clock domain: store how many sinks
         * that block has that have a path to a block that is
         * clocked on that clock domain.
         */
        for(int travIndex = traversal.size() - 1; travIndex >= 0; travIndex--) {
            TimingNode node = traversal.get(travIndex);

            int numSinks = node.getNumSinks();
            for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
                TimingNode sink = node.getSink(sinkIndex);
                TimingEdge edge = node.getSinkEdge(sinkIndex);

                List<Integer> clockDomains = this.getSinkClockDomains(sink);
                for(int clockDomain : clockDomains) {
                    node.addClockDomainSink(clockDomain, sink, edge);
                }
            }
        }
    }

    private List<Integer> getSourceClockDomains(TimingNode node) {
        List<Integer> clockDomains = new ArrayList<>(this.numClockDomains);

        if(node.getPosition() == Position.INTERMEDIATE) {
            int[] clockDomainNumSources = node.getClockDomainNumSources();
            for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
                if(clockDomainNumSources[clockDomain] > 0) {
                    clockDomains.add(clockDomain);
                }
            }

        } else {
            clockDomains.add(node.getClockDomain());
        }

        return clockDomains;
    }

    private List<Integer> getSinkClockDomains(TimingNode node) {
        List<Integer> clockDomains = new ArrayList<>(this.numClockDomains);

        if(node.getPosition() == Position.INTERMEDIATE) {
            int[] clockDomainNumSinks = node.getClockDomainNumSinks();
            for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
                if(clockDomainNumSinks[clockDomain] > 0) {
                    clockDomains.add(clockDomain);
                }
            }

        } else {
            clockDomains.add(node.getClockDomain());

        }

        return clockDomains;
    }



    private void buildTraversals() {
        for(int sourceClockDomain = 0; sourceClockDomain < this.numClockDomains; sourceClockDomain++) {
            for(int sinkClockDomain = 0; sinkClockDomain < this.numClockDomains; sinkClockDomain++) {
                List<TimingNode> traversal = this.buildTraversal(sourceClockDomain, sinkClockDomain);

                if(traversal.size() > 0) {
                    Triple<Integer, Integer, List<TimingNode>> traversalEntry =
                        new Triple<>(sourceClockDomain, sinkClockDomain, traversal);
                    this.traversals.add(traversalEntry);
                }
            }
        }
    }

    private List<TimingNode> buildTraversal(int sourceClockDomain, int sinkClockDomain) {

        List<TimingNode> traversal = new ArrayList<>();

        // Check if the circuit should analyze paths between the
        // two given clock domains
        if(!this.includePaths(sourceClockDomain, sinkClockDomain)) {
            return traversal;
        }

        for(TimingNode node : this.timingNodes) {
            node.resetProcessedSources(sourceClockDomain);
        }

        Stack<TimingNode> todo = new Stack<>();
        for(TimingNode node : this.startNodes) {
            if(node.getClockDomain() == sourceClockDomain
                    && node.getClockDomainNumSinks()[sinkClockDomain] > 0) {
                node.setTraversalRoot();
                todo.add(node);
            }
        }

        /* These traversals only contain blocks that lie between
         * two clocked blocks on the given clock domain
         */
        while(!todo.empty()) {
            TimingNode source = todo.pop();

            if(source.getPosition() != Position.LEAF) {
                traversal.add(source);

                for(TimingNode sink : source.getSinks()) {
                    // If this sink has a path that lead to a block
                    // that is clocked on the correct clock domain
                    if(sink.getClockDomainNumSinks()[sinkClockDomain] > 0) {
                        sink.incrementProcessedSources();
                        if(sink.allSourcesProcessed()) {
                            todo.add(sink);
                        }
                    }
                }
            }
        }

        return traversal;
    }

    private boolean includePaths(int sourceClockDomain, int sinkClockDomain) {
        return
                sourceClockDomain == this.virtualIoClockDomain
                || sinkClockDomain == this.virtualIoClockDomain
                || sourceClockDomain == sinkClockDomain;
    }



    /****************************************************************
     * These functions calculate the criticality of all connections *
     ****************************************************************/

    public void setCriticalityExponent(double criticalityExponent) {
        for(int i = 0; i <= 20; i++) {
            this.criticalityLookupTable[i] = Math.pow(i * 0.1, criticalityExponent);
        }
    }



    public double getMaxDelay() {
        return this.globalMaxDelay * 1e9;
    }
    public double calculateMaxDelay(boolean calculateWireDelays) {
        if(calculateWireDelays) {
            this.calculateWireDelays();
        }

        this.calculateArrivalTimesAndCriticalities(false);
        return this.globalMaxDelay;
    }
    public void calculateCriticalities(boolean calculateWireDelays) {
        if(calculateWireDelays) {
            this.calculateWireDelays();
        }


        this.calculateArrivalTimesAndCriticalities(true);
    }

    private void calculateArrivalTimesAndCriticalities(boolean calculateCriticalities) {

        for(TimingNode node : this.timingNodes) {
            node.resetArrivalTime();
        }

        if(calculateCriticalities) {
            for(TimingEdge edge : this.timingEdges) {
                edge.resetSlack();
            }
        }

        this.globalMaxDelay = 0;
        for(Triple<Integer, Integer, List<TimingNode>> traversalEntry : this.traversals) {
            int sinkClockDomain = traversalEntry.getSecond();
            List<TimingNode> traversal = traversalEntry.getThird();

            // Delays are calculated during a forward traversal
            double maxDelay = this.calculateArrivalTimes(sinkClockDomain, traversal);

            if(maxDelay > this.globalMaxDelay) {
                this.globalMaxDelay = maxDelay;
            }

            /* Slacks and criticalities are calculated during a backward traversal
             * The global max delay is not known yet, so the endpoint required
             * times are set to zero. This means all required times are non-positive,
             * and all the slacks are negative.
             * When calculating the criticalities, the global max delay is added to
             * each slack (see TimingEdge.calculateCriticality().
             */
             if(calculateCriticalities) {
                this.calculateCriticalities(sinkClockDomain, traversal);
             }
        }


        if(calculateCriticalities) {
            for(TimingEdge edge : this.timingEdges) {
                double val = (1 - (this.globalMaxDelay + edge.getSlack()) / this.globalMaxDelay) * 10;
                int i = Math.min(9, (int) val);
                double linearInterpolation = val - i;

                edge.setCriticality(
                        (1 - linearInterpolation) * this.criticalityLookupTable[i]
                        + linearInterpolation * this.criticalityLookupTable[i+1]);
            }
        }
    }

    public void calculateWireDelays() {
        for(TimingNode node : this.timingNodes) {
            node.calculateSinkWireDelays();
        }
    }

    private double calculateArrivalTimes(int sinkClockDomain, List<TimingNode> traversal) {
        double maxDelay = 0;

        for(TimingNode node : traversal) {
            node.resetArrivalTime();
        }

        for(TimingNode source : traversal) {
            double maxArrivalTime = source.updateSinkArrivalTimes(sinkClockDomain);

            if(maxArrivalTime > maxDelay) {
                maxDelay = maxArrivalTime;
            }
        }

        return maxDelay;
    }

    private void calculateCriticalities(int sinkClockDomain, List<TimingNode> traversal) {
        for(int travIndex = traversal.size() - 1; travIndex >= 0; travIndex--) {
            TimingNode block = traversal.get(travIndex);
            block.updateSlacks(sinkClockDomain);
        }
    }

    public double calculateTotalCost() {
        double totalCost = 0;

        for(List<TimingEdge> net : this.timingNets) {
            double netCost = 0;
            for(TimingEdge edge : net) {
                double cost = edge.getCost();
                if(cost > netCost) {
                    netCost = cost;
                }
            }

            totalCost += netCost;
        }


        return totalCost;
    }


    public double calculateDeltaCost(Swap swap) {
        double cost = 0;

        this.affectedNodes.clear();


        // Switch the positions of the blocks
        try {
            swap.apply();
        } catch(PlacementException e) {
            e.printStackTrace();
        }

        int numBlocks = swap.getNumBlocks();
        for(int i = 0; i < numBlocks; i++) {
            GlobalBlock block1 = swap.getBlock1(i);
            GlobalBlock block2 = swap.getBlock2(i);

            if(block1 != null) {
                cost += this.calculateDeltaCost(block1, block2);
            }
            if(block2 != null) {
                cost += this.calculateDeltaCost(block2, block1);
            }
        }


        // Put the blocks back in their original position
        try {
            swap.undoApply();
        } catch(PlacementException e) {
            e.printStackTrace();
        }

        return cost;
    }

    private double calculateDeltaCost(GlobalBlock block1, GlobalBlock block2) {
        List<TimingNode> nodes1 = block1.getTimingNodes();
        this.affectedNodes.addAll(nodes1);

        double cost = 0;
        for(TimingNode node : nodes1) {
            cost += node.calculateDeltaCost(block2);
        }

        return cost;
    }

    public void pushThrough() {
        for(TimingNode node : this.affectedNodes) {
            node.pushThrough();
        }
    }

    public void revert() {
        // Do nothing
    }


    // Iterator methods
    // When iterating over a TimingGraph object, you will get a TimingGraphEntry
    // object for each connection in the timinggraph. Each of those objects contains
    // a source block, a sink block and the criticality of the connection.
    @Override
    public Iterator<TimingGraphEntry> iterator() {
        return new TimingGraphIterator(this.circuit.getGlobalBlocks());
    }

    private class TimingGraphIterator implements Iterator<TimingGraphEntry> {

        private GlobalBlock sourceGlobalBlock;
        private Iterator<GlobalBlock> sourceGlobalBlockIterator;

        private TimingNode sourceNode;
        private Iterator<TimingNode> sourceNodeIterator;

        private int maxSinkIndex, sinkIndex;

        private TimingGraphEntry cachedEntry = null;


        TimingGraphIterator(List<GlobalBlock> globalBlocks) {
            this.sourceGlobalBlockIterator = globalBlocks.iterator();

            this.sourceGlobalBlock = this.sourceGlobalBlockIterator.next();
            this.sourceNodeIterator = this.sourceGlobalBlock.getTimingNodes().iterator();

            this.sourceNode = this.sourceNodeIterator.next();
            this.maxSinkIndex = this.sourceNode.getNumSinks();
            this.sinkIndex = 0;
        }

        @Override
        public boolean hasNext() {
            if(this.cachedEntry != null) {
                return true;
            }

            while(this.sinkIndex < this.maxSinkIndex) {

                while(!this.sourceNodeIterator.hasNext()) {
                    if(!this.sourceGlobalBlockIterator.hasNext()) {
                        return false;
                    }

                    this.sourceGlobalBlock = this.sourceGlobalBlockIterator.next();
                    this.sourceNodeIterator = this.sourceGlobalBlock.getTimingNodes().iterator();
                }

                this.sourceNode = this.sourceNodeIterator.next();
                this.maxSinkIndex = this.sourceNode.getNumSinks();
                this.sinkIndex = 0;
            }

            TimingNode sink = this.sourceNode.getSink(this.sinkIndex);
            TimingEdge edge = this.sourceNode.getSinkEdge(this.sinkIndex);
            this.sinkIndex++;

            this.cachedEntry = new TimingGraphEntry(
                    this.sourceGlobalBlock,
                    sink.getGlobalBlock(),
                    edge.getCriticality(),
                    this.sourceNode.getNumSinks());

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

    class TimingGraphEntry {
        private GlobalBlock source;
        private GlobalBlock sink;
        private double criticality;
        private int numSinks;

        TimingGraphEntry(GlobalBlock source, GlobalBlock sink, double criticality, int numSinks) {
            this.source = source;
            this.sink = sink;
            this.criticality = criticality;
            this.numSinks = numSinks;
        }

        public GlobalBlock getSource() {
            return this.source;
        }
        public GlobalBlock getSink() {
            return this.sink;
        }
        public double getCriticality() {
            return this.criticality;
        }
        public int getNumSinks() {
            return this.numSinks;
        }
    }
}
