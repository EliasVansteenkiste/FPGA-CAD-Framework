package circuit.block;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import circuit.Circuit;
import circuit.architecture.PortType;
import circuit.architecture.BlockCategory;
import circuit.pin.AbstractPin;

import placers.SAPlacer.Swap;

public class TimingGraph implements Iterable<TimingGraphEntry>, Serializable {

    private static final long serialVersionUID = 5056732903629079227L;

    private Circuit circuit;

    private transient ArrayList<LeafBlock> endPointBlocks;
    private transient List<LeafBlock> affectedBlocks;

    private double criticalityExponent;
    private transient double maxDelay;


    public TimingGraph(Circuit circuit) {
        this.circuit = circuit;
        this.initializeData();
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.initializeData();
    }

    private void initializeData() {
        this.endPointBlocks = new ArrayList<LeafBlock>();
        this.affectedBlocks = new ArrayList<LeafBlock>();
    }


    public void build() {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            boolean isClocked = block.isClocked();

            if(isClocked) {
                this.endPointBlocks.add(block);

            } else {
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
            }
        }

        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            this.traverseFromSource(block);
        }
    }

    private void traverseFromSource(LeafBlock pathSource) {

        Stack<AbstractPin> pinStack = new Stack<AbstractPin>();
        Stack<Double> delayStack = new Stack<Double>();

        for(AbstractPin outputPin : pathSource.getOutputPins()) {
            double setupDelay = 0;
            if(pathSource.isClocked()) {
                setupDelay = outputPin.getPortType().getSetupTime();

                if(pathSource.getGlobalParent().getCategory() != BlockCategory.IO) {
                    setupDelay += PortType.getClockSetupTime();
                }
            }

            pinStack.push(outputPin);
            delayStack.push(setupDelay);
        }

        while(pinStack.size() > 0) {
            AbstractPin currentPin = pinStack.pop();
            double currentDelay = delayStack.pop();

            AbstractBlock owner = currentPin.getOwner();

            if(currentPin.isInput() && owner.isLeaf()) {
                LeafBlock pathSink = ((LeafBlock) owner);

                double endDelay;
                if(owner.isClocked()) {
                    endDelay = currentPin.getPortType().getSetupTime();

                } else {
                    List<AbstractPin> outputPins = owner.getOutputPins();
                    endDelay = currentPin.getPortType().getDelay(outputPins.get(0).getPortType());
                }

                pathSource.addSink(pathSink, currentDelay + endDelay);

                // The block has children: proceed with the sinks of the current
                // pin
            } else {
                for(AbstractPin sinkPin : currentPin.getSinks()) {
                    if(sinkPin != null) {
                        double sourceSinkDelay = currentPin.getPortType().getDelay(sinkPin.getPortType());
                        double totalDelay = currentDelay + sourceSinkDelay;

                        pinStack.push(sinkPin);
                        delayStack.push(totalDelay);
                    }
                }
            }
        }
    }


    public void setCriticalityExponent(double criticalityExponent) {
        this.criticalityExponent = criticalityExponent;
    }


    public void reset() {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            int numSinks = block.getNumSinks();
            for(int i = 0; i < numSinks; i++) {
                block.getSinkEdge(i).setWireDelay(0);
            }
        }
    }


    public void recalculateAllSlacksCriticalities(boolean recalculateWireDelays) {
        this.calculateArrivalTimes(recalculateWireDelays);
        this.calculateRequiredTimes();
    }

    public void calculateArrivalTimes(boolean recalculateWireDelays) {
        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            block.resetTiming();
            if(recalculateWireDelays) {
                block.calculateSinkWireDelays();
            }
        }

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
            if(arrivalTime > this.maxDelay) {
                this.maxDelay = arrivalTime;
            }

            if(!currentBlock.isClocked()) {
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
                currentBlock.calculateRequiredTime(this.maxDelay);

                for(LeafBlock source : currentBlock.getSources()) {
                    source.incrementProcessedSinks();
                    if(source.allSinksProcessed()) {
                        todo.add(source);
                    }
                }
            }
        }


        for(LeafBlock block : this.circuit.getLeafBlocks()) {
            block.calculateCriticalities(this.maxDelay, this.criticalityExponent);
        }
    }

    public double getMaxDelay() {
        return this.maxDelay * Math.pow(10, 9);
    }

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

        List<LeafBlock> nodes1 = swap.getBlock1().getLeafBlocks();
        this.affectedBlocks.addAll(nodes1);
        cost += this.calculateDeltaCost(nodes1, swap.getSite2());

        if(swap.getBlock2() != null) {
            List<LeafBlock> nodes2 = swap.getBlock2().getLeafBlocks();
            this.affectedBlocks.addAll(nodes2);
            cost += this.calculateDeltaCost(nodes2, swap.getSite1());
        }

        return cost;
    }

    private double calculateDeltaCost(List<LeafBlock> blocks, AbstractSite site) {
        double cost = 0;

        for(LeafBlock block : blocks) {
            cost += block.calculateDeltaCost(site.getX(), site.getY());
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
}
