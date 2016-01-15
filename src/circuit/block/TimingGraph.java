package circuit.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.exceptions.PlacementException;
import circuit.pin.AbstractPin;

import placers.SAPlacer.Swap;
import util.Triple;

public class TimingGraph implements Iterable<TimingGraph.TimingGraphEntry> {

    private static String VIRTUAL_IO_CLOCK = "virtual-io-clock";

    private Circuit circuit;

    // A list of clock domains, along with their unique id
    private Map<String, Integer> clocks = new HashMap<>();
    private int virtualIoClockDomain;
    private int numClockDomains = 0;

    private List<LeafBlock> endPointBlocks  = new ArrayList<>();
    private List<LeafBlock> affectedBlocks  = new ArrayList<>();

    private List<TimingEdge> timingEdges  = new ArrayList<>();

    private List<Triple<Integer, Integer, List<LeafBlock>>> traversals  = new ArrayList<>();

    private double criticalityExponent = 1;
    private double globalMaxDelay;


    public TimingGraph(Circuit circuit) {
        this.circuit = circuit;

        this.virtualIoClockDomain = 0;
        this.clocks.put(VIRTUAL_IO_CLOCK, this.virtualIoClockDomain);
        this.numClockDomains = 1;
    }




    /******************************************
     * These functions build the timing graph *
     ******************************************/

    public void build() {

        this.buildEndPoints();

        this.buildTimingEdges();

        this.setClockDomains();

        this.buildTraversals();
    }

    private void buildEndPoints() {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            boolean isClocked = block.isClocked();

            if(isClocked) {
                this.endPointBlocks.add(block);

            // I don't think constant generators should be in the
            // endPoints collection.
            } /*else {
                boolean isConstantGenerator = true;
                for(AbstractPin inputPin : block.getInputPins()) {
                    if(inputPin.getSource() != null) {
                        isConstantGenerator = false;
                        break;
                    }
                }


                if(isConstantGenerator) {
                    this.endPointBlocks.add(block);
                }
            }*/
        }
    }


    private void buildTimingEdges() {
        /* Build the basic structure of the timing graph:
         * each leaf block has direct connections to sink
         * leaf blocks, with timing information
         */

        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            if(block.toString().equals("lcell_comb<>:subckt34659~shareout~unconn")) {
                int d = 0;
            }
            this.traverseFromSource(block);
        }
    }

    private void traverseFromSource(LeafBlock pathSource) {


        LinkedList<Triple<Integer, AbstractPin, Double>> stack = new LinkedList<>();

        if(pathSource.isClocked()) {
            int sourcePinIndex = 0;
            double clockDelay = this.getSetupTimeAndSetClockDomain(pathSource);

            for(AbstractPin outputPin : pathSource.getOutputPins()) {
                double setupDelay = clockDelay + outputPin.getPortType().getSetupTime();

                // Insert elements at the bottom of the stack, so that the output pins of
                // the source block will be processed in ascending order. This is necessary
                // for the method addSink().
                stack.addLast(new Triple<Integer, AbstractPin, Double>(sourcePinIndex, outputPin, setupDelay));

                sourcePinIndex++;
        }

        for(AbstractPin outputPin : pathSource.getOutputPins()) {
            double setupDelay = clockDelay;
            if(pathSource.isClocked()) {
                setupDelay += outputPin.getPortType().getSetupTime();
            }

            // Insert elements at the bottom of the stack, so that the output pins of
            // the source block will be processed in ascending order. This is necessary
            // for the method addSink().
            stack.addLast(new Triple<Integer, AbstractPin, Double>(sourcePinIndex, outputPin, setupDelay));

            sourcePinIndex++;
        }

        while(stack.size() > 0) {
            Triple<Integer, AbstractPin, Double> entry = stack.pop();
            int pathSourcePinIndex = entry.getFirst();
            AbstractPin currentPin = entry.getSecond();
            double currentDelay = entry.getThird();


            AbstractBlock owner = currentPin.getOwner();

            // The pin is the input of a leaf block, so a timing graph node
            if(currentPin.isInput() && owner.isLeaf()) {
                LeafBlock pathSink = ((LeafBlock) owner);

                double endDelay;
                if(owner.isClocked()) {
                    endDelay = currentPin.getPortType().getSetupTime();

                } else {
                    List<AbstractPin> outputPins = owner.getOutputPins();
                    // TODO: this is wrong
                    endDelay = currentPin.getPortType().getDelay(outputPins.get(0).getPortType());
                }

                TimingEdge edge = pathSource.addSink(pathSourcePinIndex, pathSink, currentDelay + endDelay);
                this.timingEdges.add(edge);

            // The block has children: proceed with the sinks of the current pin
            } else {
                for(AbstractPin sinkPin : currentPin.getSinks()) {
                    if(sinkPin != null) {
                        double sourceSinkDelay = currentPin.getPortType().getDelay(sinkPin.getPortType());
                        double totalDelay = currentDelay + sourceSinkDelay;

                        stack.push(new Triple<Integer, AbstractPin, Double>(pathSourcePinIndex, sinkPin, totalDelay));
                    }
                }
            }
        }
    }

    private double getSetupTimeAndSetClockDomain(LeafBlock block) {

        if(!block.isClocked()) {
            return 0;
        }

        String clockName;
        double clockDelay = 0;

        if(block.getGlobalParent().getCategory() == BlockCategory.IO) {
            clockName = VIRTUAL_IO_CLOCK;

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

            if(!this.clocks.containsKey(clockName)) {
                this.clocks.put(clockName, this.numClockDomains);
                this.numClockDomains++;
            }
        }

        block.setClockDomain(this.clocks.get(clockName));

        return clockDelay;
    }


    private void setClockDomains() {

        // Initialize all blocks
        List<LeafBlock> leafBlocks = this.circuit.getLeafBlocks();
        for(LeafBlock block : leafBlocks) {
            block.resetProcessedSources();
            block.setNumClockDomains(this.numClockDomains);
        }

        /* Initialize the todo stack
         * block.setTraversalRoot() makes sure no block
         * is processed twice
         * */
        Stack<LeafBlock> todo = new Stack<>();
        for(LeafBlock block : this.endPointBlocks) {
            block.setTraversalRoot();
            todo.add(block);
        }


        /* For each block and clock domain: store how many sources
         * that block has that have a reverse path to a block that
         * is clocked on that clock domain.
         *
         * At the same time, build a traversal regardless of clock
         * domains. The traversal is a regular list. The order of
         * blocks is chosen such that for each connection between
         * two blocks, the source blocks always comes before the
         * sink block. Connections to a clocked block are not taken
         * into account: a clocked block may come before its source
         * blocks.
         */
        List<LeafBlock> traversal = new ArrayList<>(leafBlocks.size());
        while(!todo.empty()) {
            LeafBlock block = todo.pop();
            traversal.add(block);

            List<Integer> clockDomains = this.getSourceClockDomains(block);
            for(LeafBlock sink : block.getSinks()) {
                sink.addClockDomainSource(clockDomains);

                sink.incrementProcessedSources();
                if(sink.allSourcesProcessed()) {
                    todo.add(sink);
                }
            }
        }

        /* For each block and clock domain: store how many sinks
         * that block has that have a path to a block that is
         * clocked on that clock domain.
         */
        for(int travIndex = traversal.size() - 1; travIndex >= 0; travIndex--) {
            LeafBlock block = traversal.get(travIndex);

            int numSinks = block.getNumSinks();
            for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
                LeafBlock sink = block.getSink(sinkIndex);
                TimingEdge edge = block.getSinkEdge(sinkIndex);

                List<Integer> clockDomains = this.getSinkClockDomains(sink);
                for(int clockDomain : clockDomains) {
                    block.addClockDomainSink(clockDomain, sink, edge);
                }
            }
        }
    }

    private List<Integer> getSourceClockDomains(LeafBlock block) {
        List<Integer> clockDomains = new ArrayList<>(this.numClockDomains);
        if(block.isClocked()) {
            clockDomains.add(block.getClockDomain());

        } else {
            int[] clockDomainNumSources = block.getClockDomainNumSources();
            for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
                if(clockDomainNumSources[clockDomain] > 0) {
                    clockDomains.add(clockDomain);
                }
            }
        }

        return clockDomains;
    }

    private List<Integer> getSinkClockDomains(LeafBlock block) {
        List<Integer> clockDomains = new ArrayList<>(this.numClockDomains);
        if(block.isClocked()) {
            clockDomains.add(block.getClockDomain());

        } else {
            int[] clockDomainNumSinks = block.getClockDomainNumSinks();
            for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
                if(clockDomainNumSinks[clockDomain] > 0) {
                    clockDomains.add(clockDomain);
                }
            }
        }

        return clockDomains;
    }



    private void buildTraversals() {
        for(int sourceClockDomain = 0; sourceClockDomain < this.numClockDomains; sourceClockDomain++) {
            for(int sinkClockDomain = 0; sinkClockDomain < this.numClockDomains; sinkClockDomain++) {
                List<LeafBlock> traversal = this.buildTraversal(sourceClockDomain, sinkClockDomain);

                if(traversal.size() > 0) {
                    Triple<Integer, Integer, List<LeafBlock>> traversalEntry =
                        new Triple<>(sourceClockDomain, sinkClockDomain, traversal);
                    this.traversals.add(traversalEntry);
                }
            }
        }
    }

    private List<LeafBlock> buildTraversal(int sourceClockDomain, int sinkClockDomain) {

        List<LeafBlock> traversal = new ArrayList<>();

        // Check if the circuit should analyze paths between the
        // two given clock domains
        if(!this.includePaths(sourceClockDomain, sinkClockDomain)) {
            return traversal;
        }

        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            block.resetProcessedSources(sourceClockDomain);
        }

        Stack<LeafBlock> todo = new Stack<>();
        for(LeafBlock block : this.endPointBlocks) {
            if(block.getClockDomain() == sourceClockDomain
                    && block.getClockDomainNumSinks()[sinkClockDomain] > 0) {
                block.setTraversalRoot();
                todo.add(block);
            }
        }

        /* These traversals only contain blocks that lie between
         * two clocked blocks on the given clock domain
         */
        while(!todo.empty()) {
            LeafBlock source = todo.pop();
            traversal.add(source);

            for(LeafBlock sink : source.getSinks()) {
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
     ******************************************************List<LeafBlock>**********/

    public void setCriticalityExponent(double criticalityExponent) {
        this.criticalityExponent = criticalityExponent;
        this.calculateCriticalities(false);
    }


    public void reset() {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            int numSinks = block.getNumSinks();
            for(int i = 0; i < numSinks; i++) {
                block.getSinkEdge(i).setWireDelay(0);
            }
        }
    }


    public double getMaxDelay() {
        return this.globalMaxDelay;
    }
    public double calculateMaxDelay(boolean calculateWireDelays) {
        if(calculateWireDelays) {
            this.calculateWireDelays();
        }

        this.calculateArrivalTimesAndCriticalities(true);
        return this.globalMaxDelay;
    }
    public void calculateCriticalities(boolean calculateWireDelays) {
        if(calculateWireDelays) {
            this.calculateWireDelays();
        }

        // TODO: DEBUG
        this.calculateArrivalTimesAndCriticalities(true);
        //this.calculateArrivalTimesAndCriticalities(false);
    }

    private void calculateArrivalTimesAndCriticalities(boolean calculateCriticalities) {

        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            block.resetArrivalTime();
        }

        if(calculateCriticalities) {
            for(TimingEdge edge : this.timingEdges) {
                edge.resetSlack();
            }
        }

        this.globalMaxDelay = 0;
        for(Triple<Integer, Integer, List<LeafBlock>> traversalEntry : this.traversals) {
            int sinkClockDomain = traversalEntry.getSecond();
            List<LeafBlock> traversal = traversalEntry.getThird();

            // Delays are calculated during a forward traversal
            double maxDelay = this.calculateArrivalTimes(sinkClockDomain, traversal);

            for(LeafBlock block : this.endPointBlocks) {
                if(block.getName().equals("top^FF_NODE~382")) {
                    int d = 0;
                }
            }

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

             if(traversalEntry.getFirst() == 1 && sinkClockDomain == 1) {
                 break;
             }
        }


        if(calculateCriticalities) {
            for(TimingEdge edge : this.timingEdges) {
                edge.calculateCriticality(this.globalMaxDelay, this.criticalityExponent);
            }
        }

        for(LeafBlock block : this.endPointBlocks) {
            if(block.toString().equals("memory_slice<>:subckt8317~eccstatus[0]~unconn")) {
                int d = 0;
            }
        }

        LeafBlock prevCritBlock = null;
        for(LeafBlock block : this.endPointBlocks) {
            int numSinks = block.getNumSinks();
            for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
                TimingEdge edge = block.getSinkEdge(sinkIndex);
                if(edge.getCriticality() > 0.99999) {
                    prevCritBlock = block;
                    break;
                }
            }
            if(prevCritBlock != null) {
                break;
            }
        }

        System.out.printf("%-72s%g\n", prevCritBlock.toString(), prevCritBlock.getArrivalTime());
        do {
            int numSinks = prevCritBlock.getNumSinks();
            LeafBlock critBlock = null;
            for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
                TimingEdge edge = prevCritBlock.getSinkEdge(sinkIndex);
                if(edge.getCriticality() > 0.99999) {
                    critBlock = prevCritBlock.getSink(sinkIndex);
                    break;
                }
            }

            prevCritBlock = critBlock;
            System.out.printf("%-72s%g\n", prevCritBlock.toString(), prevCritBlock.getArrivalTime());
        } while(!prevCritBlock.isClocked());



        /*for(LeafBlock block : this.endPointBlocks) {
            if(block.toString().equals("dff<>:sparc_mul_top:mul|sparc_mul_dp:dpath|mul64:mulcore|myout_a1[105]")) {
                LeafBlock sourceBlock = block;
                int pathLength = 0;
                do {
                    pathLength++;
                    int sourceIndex = -1;
                    for(int i = 0; i < sourceBlock.getNumSources(); i++) {
                        if(sourceBlock.getSourceEdge(i).getCriticality() > 0.9999) {
                            sourceIndex = i;
                            break;
                        }
                    }

                    sourceBlock = sourceBlock.getSource(sourceIndex);
                } while(!sourceBlock.isClocked());
                int d = 0;
            }
        }*/
    }

    public void calculateWireDelays() {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            block.calculateSinkWireDelays();
        }
    }

    private double calculateArrivalTimes(int sinkClockDomain, List<LeafBlock> traversal) {
        double maxDelay = 0;

        for(LeafBlock block : traversal) {
            block.resetArrivalTime();
        }

        for(LeafBlock source : traversal) {
            if(source.toString().equals("dff<>:sparc_mul_top:mul|sparc_mul_dp:dpath|mul64:mulcore|myout_a1[92]")) {
                int d = 0;
            }
            double maxArrivalTime = source.updateSinkArrivalTimes(sinkClockDomain);

            if(maxArrivalTime > maxDelay) {
                maxDelay = maxArrivalTime;
            }
        }

        return maxDelay;
    }

    private void calculateCriticalities(int sinkClockDomain, List<LeafBlock> traversal) {
        for(int travIndex = traversal.size() - 1; travIndex >= 0; travIndex--) {
            LeafBlock block = traversal.get(travIndex);
            if(block.toString().equals("dff<>:sparc_mul_top:mul|sparc_mul_dp:dpath|mul64:mulcore|myout_a1[92]")) {
                int d = 0;
            }
            block.updateSlacks(sinkClockDomain);
        }
    }

    /*public void calculateArrivalTimes() {

        Stack<LeafBlock> todo = new Stack<LeafBlock>();

        for(LeafBlock startBlock : this.endPointBlocks) {
            for(LeafBlock sink : startBlock.getSinks()) {
                sink.incrementProcessedSources();
                if(sink.allSourcesProcessed()) {
                    todo.add(sink);
                }
            }
        }


        this.maxDelay = 0;
        while(todo.size() > 0) {
            LeafBlock currentBlock = todo.pop();

            double arrivalTime = currentBlock.calculateArrivalTime();

            if(currentBlock.isClocked()) {
                if(arrivalTime > this.maxDelay) {
                    this.maxDelay = arrivalTime;
                }

            } else {
                for(LeafBlock sink : currentBlock.getSinks()) {
                    sink.incrementProcessedSources();
                    if(sink.allSourcesProcessed()) {
                        todo.add(sink);
                    }
                }
            }
        }
    }

    public void calculateRequiredTimes() {
        Stack<LeafBlock> todo = new Stack<LeafBlock>();

        for(LeafBlock endBlock : this.endPointBlocks) {
            endBlock.setRequiredTime(this.maxDelay);

            for(LeafBlock source : endBlock.getSources()) {
                source.incrementProcessedSinks();
                if(source.allSinksProcessed()) {
                    todo.add(source);
                }
            }
        }

        while(todo.size() > 0) {
            LeafBlock currentBlock = todo.pop();

            if(!currentBlock.isClocked()) {
                currentBlock.calculateRequiredTime();

                for(LeafBlock source : currentBlock.getSources()) {
                    source.incrementProcessedSinks();
                    if(source.allSinksProcessed()) {
                        todo.add(source);
                    }
                }
            }
        }
    }

    public void calculateCriticalities() {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            if(block.toString().equals("lcell_comb<>:subckt77899~shareout~unconn")) {
                int d = 0;
            }
            block.calculateCriticalities(this.maxDelay, this.criticalityExponent);
        }
    }

    public double getMaxDelay() {
        return this.maxDelay * Math.pow(10, 9);
    }*/

    public double calculateTotalCost() {
        double totalCost = 0;

        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            totalCost += block.calculateCost();
        }

        return totalCost;
    }


    public double calculateDeltaCost(Swap swap) {
        double cost = 0;

        this.affectedBlocks.clear();


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
        List<LeafBlock> nodes1 = block1.getLeafBlocks();
        this.affectedBlocks.addAll(nodes1);

        double cost = 0;
        for(LeafBlock node : nodes1) {
            cost += node.calculateDeltaCost(block2);
        }

        return cost;
    }

    public void pushThrough() {
        for(LeafBlock block : this.affectedBlocks) {
            block.pushThrough();
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
