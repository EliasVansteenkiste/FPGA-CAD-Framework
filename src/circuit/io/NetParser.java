package circuit.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import circuit.Circuit;
import circuit.architecture.Architecture;
import circuit.architecture.BlockType;
import circuit.architecture.PortType;
import circuit.block.AbstractBlock;
import circuit.block.GlobalBlock;
import circuit.block.LeafBlock;
import circuit.block.IntermediateBlock;
import circuit.pin.AbstractPin;

public class NetParser {

    //private Circuit circuit;
    private Architecture architecture;
    private String circuitName;
    private BufferedReader reader;

    private Map<BlockType, List<AbstractBlock>> blocks;

    // blockStack is a LinkedList because we want to be able to peekLast()
    private LinkedList<AbstractBlock> blockStack;
    private Stack<TupleBlockMap> inputsStack;
    private Stack<Map<String, String>> outputsStack;

    private Map<String, AbstractPin> sourcePins;

    private enum PortDirection {INPUT, OUTPUT};
    private PortDirection currentPortType;


    public NetParser(Architecture architecture, String circuitName, File file) throws FileNotFoundException {
        this.architecture = architecture;
        this.circuitName = circuitName;
        this.reader = new BufferedReader(new FileReader(file));
    }


    public Circuit parse() throws IOException {
        // A list of all the blocks in the circuit
        this.blocks = new HashMap<BlockType, List<AbstractBlock>>();

        // blockStack is a stack that contains the current block hierarchy.
        // It is used to find the parent of a block. outputsStack contains
        // the outputs of these blocks. This is necessary because the outputs
        // of a block can only be processed after all the childs have been
        // processed.
        this.blockStack = new LinkedList<AbstractBlock>();
        this.inputsStack = new Stack<TupleBlockMap>();
        this.outputsStack = new Stack<Map<String, String>>();


        // sourcePins contains the names of the outputs of leaf blocks and
        // the corresponding output pins. It is needed to be able to create
        // global nets: at the time: only the name of the bottom-level source
        // block is given for these nets.
        this.sourcePins = new HashMap<String, AbstractPin>();


        String line, multiLine = "";

        while ((line = this.reader.readLine()) != null) {
            String trimmedLine = line.trim();

            // Add the current line to the multiLine
            if(multiLine.length() > 0) {
                multiLine += " ";
            }
            multiLine += trimmedLine;

            if(!this.isCompleteLine(multiLine)) {
                continue;
            }



            String lineStart = multiLine.substring(0, 5);

            switch(lineStart) {
            case "<inpu":
                this.processInputLine(multiLine);
                break;


            case "<outp":
                this.processOutputLine(multiLine);
                break;

            case "<cloc":
                this.processClockLine(multiLine);
                break;


            case "<port":
                this.processPortLine(multiLine);
                break;


            case "<bloc":
                if(!multiLine.substring(multiLine.length() - 2).equals("/>")) {
                    this.processBlockLine(multiLine);
                }
                break;


            case "</blo":
                this.processBlockEndLine();
                break;
            }

            multiLine = "";
        }


        for(List<AbstractBlock> blocksOfType : this.blocks.values()) {
            for(AbstractBlock block : blocksOfType) {
                block.compact();
            }
        }

        Circuit circuit = new Circuit(this.circuitName, this.architecture, this.blocks);
        circuit.buildDataStructures();

        return circuit;
    }


    private boolean isCompleteLine(String line) {
        int lineLength = line.length();

        // The line is empty
        if(lineLength == 0) {
            return false;
        }


        // The line doesn't end with a ">" character
        if(!line.substring(lineLength - 1).equals(">")) {
            return false;
        }

        // The line is a port line, but not all ports are on this line
        if(lineLength >= 7
                && line.substring(0, 5).equals("<port")
                && !line.substring(lineLength - 7).equals("</port>")) {
            return false;
        }

        return true;
    }



    @SuppressWarnings("unused")
    private void processInputLine(String line) {
        this.currentPortType = PortDirection.INPUT;
    }

    @SuppressWarnings("unused")
    private void processOutputLine(String line) {
        this.currentPortType = PortDirection.OUTPUT;
    }

    @SuppressWarnings("unused")
    private void processClockLine(String line) {
        this.currentPortType = null;
    }


    private void processPortLine(String line) {

        // This is a clock port
        if(this.currentPortType == null) {
            return;
        }

        int nameStart = 12;
        int nameEnd = line.indexOf("\"", 12);
        String name = line.substring(nameStart, nameEnd);

        int portsStart = nameEnd + 2;
        int portsEnd = line.length() - 7;
        String ports = line.substring(portsStart, portsEnd);

        switch(this.currentPortType) {
            case INPUT:
                this.inputsStack.peek().getMap().put(name, ports);
                break;

            case OUTPUT:
                this.outputsStack.peek().put(name, ports);
                break;
        }
    }


    private void processBlockLine(String line) {

        int nameStart = 13;
        int nameEnd = line.indexOf("\"", nameStart);
        String name = line.substring(nameStart, nameEnd);

        int typeStart = nameEnd + 12;
        int typeEnd = line.indexOf("[", typeStart);
        String type = line.substring(typeStart, typeEnd);

        // Ignore the top-level block
        if(type.equals("FPGA_packed_netlist")) {
            return;
        }


        int indexStart = typeEnd + 1;
        int indexEnd = line.indexOf("]", indexStart);
        int index = Integer.parseInt(line.substring(indexStart, indexEnd));


        int modeStart = indexEnd + 9;
        int modeEnd = line.length() - 2;
        String mode = modeStart < modeEnd ? line.substring(modeStart, modeEnd) : null;



        if(this.architecture.isImplicitBlock(type)) {
            String parentBlockTypeName = this.blockStack.getFirst().getType().getName();
            type = this.architecture.getImplicitBlockName(parentBlockTypeName, type);
        }
        BlockType blockType = new BlockType(type, mode);


        AbstractBlock newBlock;
        if(blockType.isGlobal()) {
            newBlock = new GlobalBlock(name, blockType, index);

        } else {
            AbstractBlock parent = this.blockStack.peek();

            if(blockType.isLeaf()) {
                GlobalBlock globalParent = (GlobalBlock) this.blockStack.peekLast();
                newBlock = new LeafBlock(this.architecture.getDelayTables(), name, blockType, index, parent, globalParent);

            } else {
                newBlock = new IntermediateBlock(name, blockType, index, parent);
            }
        }


        this.blockStack.push(newBlock);
        this.inputsStack.push(new TupleBlockMap(newBlock));
        this.outputsStack.push(new HashMap<String, String>());


        if(!this.blocks.containsKey(blockType)) {
            BlockType emptyModeType = new BlockType(blockType.getName());
            this.blocks.put(emptyModeType, new ArrayList<AbstractBlock>());
        }
        this.blocks.get(blockType).add(newBlock);
    }

    private void processBlockEndLine() {
        // If the stack is empty: this is the top-level block
        // All that is left to do is process all the inputs of
        // the global blocks
        if(this.blockStack.size() == 0) {
            while(this.inputsStack.size() > 0) {
                TupleBlockMap globalTuple = this.inputsStack.pop();
                AbstractBlock globalBlock = globalTuple.getBlock();
                Map<String, String> inputs = globalTuple.getMap();

                processPortsHashMap(globalBlock, inputs);
            }

        // This is
        } else {
            // Remove this block and its outputs from the stacks
            AbstractBlock block = this.blockStack.pop();

            Map<String, String> outputs = this.outputsStack.pop();
            processPortsHashMap(block, outputs);

            // Process the inputs of all the children of this block, but
            // not of this block itself. This is because the inputs may
            // come from sibling blocks that haven't been parsed yet.
            while(this.inputsStack.peek().getBlock() != block) {
                TupleBlockMap childTuple = this.inputsStack.pop();
                AbstractBlock childBlock = childTuple.getBlock();
                Map<String, String> inputs = childTuple.getMap();

                processPortsHashMap(childBlock, inputs);
            }
        }
    }

    private void processPortsHashMap(AbstractBlock block, Map<String, String> ports) {
        for(Map.Entry<String, String> portEntry : ports.entrySet()) {
            String portName = portEntry.getKey();
            PortType portType = new PortType(block.getType(), portName);
            List<AbstractPin> pins = block.getPins(portType);

            String nets = portEntry.getValue();

            this.addNets(pins, nets);
        }
    }


    private void addNets(List<AbstractPin> sinkPins, String netsString) {
        //TODO: no regex?
        String[] nets = netsString.trim().split("\\s+");

        for(int sinkPinIndex = 0; sinkPinIndex < nets.length; sinkPinIndex++) {
            AbstractPin sinkPin = sinkPins.get(sinkPinIndex);
            String net = nets[sinkPinIndex];

            this.addNet(sinkPin, net);
        }
    }


    private void addNet(AbstractPin sinkPin, String net) {
        if(net.equals("open")) {
            return;
        }

        AbstractBlock sinkBlock = sinkPin.getOwner();

        int separator = net.lastIndexOf("->");

        if(separator != -1) {
            int pinIndexEnd = separator - 1;
            int pinIndexStart = net.lastIndexOf("[", pinIndexEnd) + 1;
            int sourcePinIndex = Integer.parseInt(net.substring(pinIndexStart, pinIndexEnd));

            int portEnd = pinIndexStart - 1;
            int portStart = net.lastIndexOf(".", portEnd) + 1;
            String sourcePortName = net.substring(portStart, portEnd);


            int blockIndexEnd = portStart - 2;
            int blockIndexStart = portStart;
            int sourceBlockIndex = -1;

            if(net.charAt(blockIndexEnd) == ']') {
                blockIndexStart = net.lastIndexOf("[", blockIndexEnd) + 1;
                sourceBlockIndex = Integer.parseInt(net.substring(blockIndexStart, blockIndexEnd));
            }

            int typeEnd = blockIndexStart - 1;
            int typeStart = 0;
            String sourceBlockName = net.substring(typeStart, typeEnd);

            if(this.architecture.isImplicitBlock(sourceBlockName)) {
                String parentBlockTypeName = sinkBlock.getType().getName();
                sourceBlockName = this.architecture.getImplicitBlockName(parentBlockTypeName, sourceBlockName);
            }

            BlockType sourceBlockType = new BlockType(sourceBlockName);
            PortType sourcePortType = new PortType(sourceBlockType, sourcePortName);

            // The hardest part: determine the source block
            AbstractBlock sourceBlock;

            // The net is incident to an input port. It has an input port of the parent block as source.
            if(sourceBlockIndex == -1) {
                sourceBlock = ((IntermediateBlock) sinkBlock).getParent();


            } else {


                // The net is incident to an input port. It has a sibling output port as source.
                if(sinkPin.isInput()) {
                    AbstractBlock parent = ((IntermediateBlock) sinkBlock).getParent();
                    sourceBlock = parent.getChild(sourceBlockType, sourceBlockIndex);


                // The net is incident to an output port. It has either a child output port as source
                // or an input port of itself.
                } else {
                    if(sinkBlock.getType().equals(sourceBlockType)) {
                        sourceBlock = sinkBlock;
                    } else {
                        sourceBlock = sinkBlock.getChild(sourceBlockType, sourceBlockIndex);
                    }
                }
            }

            AbstractPin sourcePin = sourceBlock.getPin(sourcePortType, sourcePinIndex);
            sourcePin.addSink(sinkPin);
            sinkPin.setSource(sourcePin);


        // The current block is a leaf block. We can add a reference from the net name to
        // the correct pin in this block, so that we can add the todo-nets later.
        } else if(sinkPin.isOutput()) {
            this.sourcePins.put(net, sinkPin);


        // The input net that we want to add has a leaf block as its (indirect) source.
        // Finding the source block for the net is a bit tricky, because we have to trickle
        // up through the hierarchy from the referenced block.
        } else {
            String sourceName = net;

            AbstractPin sourcePin = this.sourcePins.get(sourceName);
            AbstractBlock parent = sourcePin.getOwner().getParent();


            while(parent != null) {
                int numPins = sourcePin.getNumSinks();
                AbstractPin nextSourcePin = null;

                for(int i = 0; i < numPins; i++) {
                    AbstractPin pin = sourcePin.getSink(i);
                    if(pin.getOwner() == parent) {
                        nextSourcePin = pin;
                        break;
                    }
                }

                sourcePin = nextSourcePin;
                parent = sourcePin.getOwner().getParent();
            }

            sourcePin.addSink(sinkPin);
            sinkPin.setSource(sourcePin);
        }
    }
}
