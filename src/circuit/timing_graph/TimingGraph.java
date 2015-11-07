package circuit.timing_graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import options.Options;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.architecture.PortType;
import circuit.architecture.BlockCategory;
import circuit.block.AbstractBlock;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.block.LocalBlock;
import circuit.pin.AbstractPin;

import placers.SAPlacer.Swap;
import util.Logger;

public class TimingGraph implements Iterable<TimingGraphEntry>, Serializable {

    private static final long serialVersionUID = 5056732903629079227L;

    private Circuit circuit;

    private DelayTables delayTables;

    // These fields lead to StackOverflowErrors. The parent circuit is responsible
    // for rebuilding the Timing Graph after deserialization.
    private transient Map<LocalBlock, TimingNode> nodes;
    private transient Map<GlobalBlock, List<TimingNode>> nodesInGlobalBlocks;
    private transient ArrayList<TimingNode> endPointNodes;
    private transient List<TimingNode> affectedNodes;

    private double criticalityExponent = 8;
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
        this.nodes = new HashMap<LocalBlock, TimingNode>();
        this.nodesInGlobalBlocks = new HashMap<GlobalBlock, List<TimingNode>>();
        this.endPointNodes = new ArrayList<TimingNode>();
        this.affectedNodes = new ArrayList<TimingNode>();
    }


    public void buildDelayMatrixes() {
        String circuitName = this.circuit.getName();

        File blifFile = Options.getInstance().getBlifFile();
        File netFile = Options.getInstance().getNetFile();
        File architectureFileVPR = Options.getInstance().getArchitectureFileVPR();

        // Run vpr
        String command = String.format(
                "./vpr %s %s --blif_file %s --net_file %s --place_file vpr_tmp --place --init_t 1 --exit_t 1",
                architectureFileVPR, circuitName, blifFile, netFile);

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch(IOException error) {
            Logger.raise("Failed to execute vpr: " + command, error);
        }

        // Read output to avoid buffer overflow and deadlock
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            while ((reader.readLine()) != null) {}
        } catch(IOException error) {
            Logger.raise("Failed to read from vpr output", error);
        }

        // Finish execution
        try {
            process.waitFor();
        } catch(InterruptedException error) {
            Logger.raise("vpr was interrupted", error);
        }

        // Parse the delay tables
        File delaysFile = new File("lookup_dump.echo");
        this.delayTables = new DelayTables(delaysFile);
        this.delayTables.parse();

        // Clean up
        this.deleteFile("vpr_tmp");
        this.deleteFile("vpr_stdout.log");
        this.deleteFile("lookup_dump.echo");
    }

    private void deleteFile(String path) {
        try {
            Files.deleteIfExists(new File(path).toPath());
        } catch(IOException error) {

            Logger.raise("File not found: " + path);
        }
    }


    public void build() {
        // Get all leaf nodes
        for(BlockType blockType : this.circuit.getBlockTypes()) {
            if(!blockType.isLeaf()) {
                continue;
            }

            boolean typeIsClocked = blockType.isClocked();


            for(AbstractBlock block : this.circuit.getBlocks(blockType)) {
                AbstractBlock parent = block;
                while(!parent.isGlobal()) {
                    parent = parent.getParent();
                }
                TimingNode node = new TimingNode(this.delayTables, (GlobalBlock) parent, block.toString(), typeIsClocked);

                this.nodes.put((LocalBlock) block, node);

                if(this.nodesInGlobalBlocks.get(parent) == null) {
                    this.nodesInGlobalBlocks.put((GlobalBlock) parent, new ArrayList<TimingNode>());
                }
                this.nodesInGlobalBlocks.get(parent).add(node);

                if(typeIsClocked) {
                    this.endPointNodes.add(node);
                } else {
                    boolean isConstantGenerator = true;
                    for(AbstractPin inputPin : block.getInputPins()) {
                        if(inputPin.getSource() != null) {
                            isConstantGenerator = false;
                            break;
                        }
                    }

                    if(isConstantGenerator) {
                        this.endPointNodes.add(node);
                    }
                }
            }
        }

        for(Map.Entry<LocalBlock, TimingNode> nodeEntry : this.nodes.entrySet()) {
            LocalBlock block = nodeEntry.getKey();
            TimingNode node = nodeEntry.getValue();

            this.traverseFromSource(block, node);
        }
    }

    private void traverseFromSource(LocalBlock block, TimingNode pathSource) {
        Stack<AbstractPin> pinStack = new Stack<AbstractPin>();
        Stack<Double> delayStack = new Stack<Double>();

        for(AbstractPin outputPin : block.getOutputPins()) {
            double setupDelay = 0;
            if(block.isClocked()) {
                setupDelay = outputPin.getPortType().getSetupTime();

                if(block.getParent().getCategory() != BlockCategory.IO) {
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
                TimingNode pathSink = this.nodes.get(owner);

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

    public void recalculateAllSlackCriticalities() {
        this.maxDelay = this.calculateArrivalTimes();
        this.calculateRequiredTimes();

        for(TimingNode node : this.nodes.values()) {
            node.calculateCriticalities(this.maxDelay, this.criticalityExponent);
        }
    }

    private double calculateArrivalTimes() {
        for(TimingNode node : this.nodes.values()) {
            node.reset();
            node.calculateSinkWireDelays();
        }


        Stack<TimingNode> todo = new Stack<TimingNode>();

        for(TimingNode startNode : this.endPointNodes) {
            for(TimingNode sink : startNode.getSinks()) {
                sink.incrementProcessedSources();
                if(sink.allSourcesProcessed()) {
                    todo.add(sink);
                }
            }
        }


        double maxDelay = 0;
        while(todo.size() > 0) {
            TimingNode currentNode = todo.pop();

            double arrivalTime = currentNode.calculateArrivalTime();
            if(arrivalTime > maxDelay) {
                maxDelay = arrivalTime;
            }

            if(!currentNode.isClocked()) {
                for(TimingNode sink : currentNode.getSinks()) {
                    sink.incrementProcessedSources();
                    if(sink.allSourcesProcessed()) {
                        todo.add(sink);
                    }
                }
            }
        }

        return maxDelay;
    }

    private void calculateRequiredTimes() {
        Stack<TimingNode> todo = new Stack<TimingNode>();

        for(TimingNode endNode : this.endPointNodes) {
            endNode.setRequiredTime(this.maxDelay);

            for(TimingNode source : endNode.getSources()) {
                source.incrementProcessedSinks();
                if(source.allSinksProcessed()) {
                    todo.add(source);
                }
            }
        }

        while(todo.size() > 0) {
            TimingNode currentNode = todo.pop();

            if(!currentNode.isClocked()) {
                currentNode.calculateRequiredTime(this.maxDelay);

                for(TimingNode source : currentNode.getSources()) {
                    source.incrementProcessedSinks();
                    if(source.allSinksProcessed()) {
                        todo.add(source);
                    }
                }
            }
        }
    }

    public double getMaxDelay() {
        return this.maxDelay * Math.pow(10, 9);
    }

    public double calculateTotalCost() {
        double totalCost = 0;

        for(TimingNode node : this.nodes.values()) {
            totalCost += node.calculateCost();
        }

        return totalCost;
    }

    public double calculateDeltaCost(Swap swap) {
        double cost = 0;

        this.affectedNodes.clear();

        List<TimingNode> nodes = this.nodesInGlobalBlocks.get(swap.getBlock1());
        this.affectedNodes.addAll(nodes);
        cost += this.calculateDeltaCost(nodes, swap.getSite2());

        if(swap.getBlock2() != null) {
            nodes = this.nodesInGlobalBlocks.get(swap.getBlock2());
            this.affectedNodes.addAll(nodes);
            cost += this.calculateDeltaCost(nodes, swap.getSite1());
        }

        return cost;
    }

    private double calculateDeltaCost(List<TimingNode> nodes, AbstractSite site) {
        double cost = 0;

        for(TimingNode node : nodes) {
            cost += node.calculateDeltaCost(site.getX(), site.getY());
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


    double calculateWireDelay(BlockCategory fromCategory, BlockCategory toCategory, int deltaX, int deltaY) {
        if(fromCategory == BlockCategory.IO) {
            if(toCategory == BlockCategory.IO) {
                return this.delayTables.getIoToIo(deltaX, deltaY);
            } else {
                return this.delayTables.getIoToClb(deltaX, deltaY);
            }
        } else {
            if(toCategory == BlockCategory.IO) {
                return this.delayTables.getClbToIo(deltaX, deltaY);
            } else {
                return this.delayTables.getClbToClb(deltaX, deltaY);
            }
        }
    }


    // Iterator methods
    // When iterating over a TimingGraph object, you will get a TimingGraphEntry
    // object
    // for each connection in the timinggraph. Each of those objects contains a
    // source
    // block, a sink block and the criticality of the connection.
    public Iterator<TimingGraphEntry> iterator() {
        return new TimingGraphIterator(this.nodesInGlobalBlocks);
    }
}
