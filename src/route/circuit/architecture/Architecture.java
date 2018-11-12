package route.circuit.architecture;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import route.util.Pair;
import route.circuit.exceptions.InvalidFileFormatException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * We make a lot of assumptions while parsing an architecture XML file.
 * I tried to document these assumptions by commenting them with the
 * prefix ASM.
 * First assumption: the architecture file is entirely valid. No checks
 * on duplicate or illegal values are made in this parser.
 *
 * Only a subset of the VPR architecture specs is supported.
 * TODO: document this subset.
 *
 * This parser does not store interconnections. We just assume that all
 * connections in the net file are legal. Should someone ever want to
 * write a packer, then this feature has to be implemented.
 *
 * ASM: block types have unique names. Two blocks with the same type
 * are exactly equal, regardless of their parent block(s)
 */
public class Architecture implements Serializable {
    private static final long serialVersionUID = -5436935126902935000L;
    
    private boolean autoSize;
    private int width, height;
    private double autoRatio;
    
    private final File architectureFile, blifFile, netFile, sdcFile, rrgFile;
    private final String circuitName;
    
    private transient Map<String, Boolean> modelIsClocked = new HashMap<>();
    private transient List<Pair<PortType, Double>> setupTimes = new ArrayList<>();
    
    private transient List<DelayElement> delayElements = new ArrayList<>();
    
    private transient Map<String, Integer> directs = new HashMap<>();//Direct interconnect between logic blocks
    
    private DelayTables delayTables;
    
    private int ioCapacity;
    
    public Architecture(
            String circuitName,
            File architectureFile,
            File blifFile,
            File netFile,
            File sdcFile,
            File rrgFile) {

        this.architectureFile = architectureFile;

        this.blifFile = blifFile;
        this.netFile = netFile;
        this.sdcFile = sdcFile;
        this.rrgFile = rrgFile;

        this.circuitName = circuitName;
    }

    public void parse() throws IOException, InvalidFileFormatException, InterruptedException, ParserConfigurationException, SAXException {

        // Build a XML root
        DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document xmlDocument = xmlBuilder.parse(this.architectureFile);
        Element root = xmlDocument.getDocumentElement();

        // Get the architecture size (fixed or automatic)
        this.processLayout(root);

        // Store the models to know which ones are clocked
        this.processModels(root);

        // The device, switchlist and segmentlist tags are ignored: we leave these complex calculations to VPR
        // Proceed with directlist
        this.processDirects(root);

        // Get all the complex block types and their ports and delays
        this.processBlocks(root);

        // Cache some frequently used data
        BlockTypeData.getInstance().postProcess();

        // All delays have been cached in this.delays, process them now
        this.processDelays();

        // Set all delays to 0
        // This is only useful while debugging,
        // because the call to vpr takes a really
        // long time for large circuits
        this.delayTables = new DelayTables();
    }


    private void processLayout(Element root) {
        Element layoutElement = this.getFirstChild(root, "layout");

        this.autoSize = layoutElement.hasAttribute("auto");
        if(this.autoSize) {
            this.autoRatio = Double.parseDouble(layoutElement.getAttribute("auto"));

        } else {
            this.width = Integer.parseInt(layoutElement.getAttribute("width"));
            this.height = Integer.parseInt(layoutElement.getAttribute("height"));
        }
    }



    private void processModels(Element root) {
        Element modelsElement = this.getFirstChild(root, "models");
        List<Element> modelElements = this.getChildElementsByTagName(modelsElement, "model");

        for(Element modelElement : modelElements) {
            this.processModel(modelElement);
        }
    }

    private void processModel(Element modelElement) {
        String modelName = modelElement.getAttribute("name");

        Element inputPortsElement = this.getFirstChild(modelElement, "input_ports");
        List<Element> portElements = this.getChildElementsByTagName(inputPortsElement, "port");

        boolean isClocked = false;
        for(Element portElement : portElements) {
            String isClock = portElement.getAttribute("is_clock");
            if(isClock.equals("1")) {
                isClocked = true;
            }
        }

        this.modelIsClocked.put(modelName, isClocked);
    }



    private void processDirects(Element root) {
        Element directsElement = this.getFirstChild(root, "directlist");
        if(directsElement == null) {
            return;
        }

        List<Element> directElements = this.getChildElementsByTagName(directsElement, "direct");
        for(Element directElement : directElements) {

            String[] fromPort = directElement.getAttribute("from_pin").split("\\.");
            String[] toPort = directElement.getAttribute("to_pin").split("\\.");

            int offsetX = Integer.parseInt(directElement.getAttribute("x_offset"));
            int offsetY = Integer.parseInt(directElement.getAttribute("y_offset"));
            int offsetZ = Integer.parseInt(directElement.getAttribute("z_offset"));

            // Directs can only go between blocks of the same type
            assert(fromPort[0].equals(toPort[0]));

            // ASM: macro's can only extend in the y direction
            // if offsetX is different from 0, this is a direct
            // connection that is not used as a carry chain
            if(offsetX == 0 && offsetZ == 0) {
                String id = this.getDirectId(fromPort[0], fromPort[1], toPort[1]);
                this.directs.put(id, offsetY);
            }
        }
    }

    private String getDirectId(String blockName, String portNameFrom, String portNameTo) {
        return String.format("%s.%s-%s", blockName, portNameFrom, portNameTo);
    }



    private void processBlocks(Element root) {
        Element blockListElement = this.getFirstChild(root, "complexblocklist");
        List<Element> blockElements = this.getChildElementsByTagName(blockListElement, "pb_type");

        for(Element blockElement : blockElements) {
            this.processBlockElement(null, blockElement);
        }
    }

    private BlockType processBlockElement(BlockType parentBlockType, Element blockElement) {
        String blockName = blockElement.getAttribute("name");
        int num_pb = -1;
        
        if(blockElement.getAttribute("num_pb").length() > 0) {
        	num_pb = Integer.parseInt(blockElement.getAttribute("num_pb"));
        }

        boolean isGlobal = this.isGlobal(blockElement);
        boolean isLeaf = this.isLeaf(blockElement);
        boolean isClocked = this.isClocked(blockElement);


        // Set block category, and some related properties
        BlockCategory blockCategory;

        // ASM: IO blocks are around the perimeter, HARDBLOCKs are in columns,
        // CLB blocks fill the rest of the FPGA
        int start = -1, repeat = -1, height = -1, priority = -1;
        double fcIn = -1.0;
        double fcOut = -1.0;

        if(isLeaf) {
            blockCategory = BlockCategory.LEAF;

        } else if(!isGlobal) {
            blockCategory = BlockCategory.INTERMEDIATE;

        } else {
            // Get some extra properties that relate to the placement
            // of global blocks

            blockCategory = this.getGlobalBlockCategory(blockElement);
            
            Element fcElement = this.getFirstChild(blockElement, "fc");
            fcIn = Double.parseDouble(fcElement.getAttribute("default_in_val"));
            fcOut = Double.parseDouble(fcElement.getAttribute("default_out_val"));
            
            if(blockCategory == BlockCategory.IO) {
                // ASM: io blocks are always placed around the perimeter of the
                // architecture, ie. "<loc type="perimeter">" is set.
                // This assumption is used throughout this entire placement suite.
                // Many changes will have to be made in the different algorithms
                // in order to get rid of this assumption.
                this.ioCapacity = Integer.parseInt(blockElement.getAttribute("capacity"));

            } else {

                if(blockElement.hasAttribute("height")) {
                    height = Integer.parseInt(blockElement.getAttribute("height"));
                } else {
                    height = 1;
                }

                Element gridLocationsElement = this.getFirstChild(blockElement, "gridlocations");
                Element locElement = this.getFirstChild(gridLocationsElement, "loc");
                priority = Integer.parseInt(locElement.getAttribute("priority"));

                // ASM: the loc type "rel" is not used
                String type = locElement.getAttribute("type");
                assert(!type.equals("rel"));

                if(type.equals("col")) {
                    start = Integer.parseInt(locElement.getAttribute("start"));

                    if(locElement.hasAttribute("repeat")) {
                        repeat = Integer.parseInt(locElement.getAttribute("repeat"));
                    } else {
                        repeat = -1;

                        // If start is 0 and repeat is -1, the column calculation in Circuit.java fails
                        assert(start != 0);
                    }



                } else if(type.equals("fill")) {
                    start = 0;
                    repeat = 1;

                } else {
                    assert(false);
                }
            }

        }

        // Build maps of inputs and outputs
        Map<String, Pair<Integer, Boolean>> inputs = this.getPorts(blockElement, "input");
        Map<String, Pair<Integer, Boolean>> outputs = this.getPorts(blockElement, "output");
        Map<String, Pair<Integer, Boolean>> clockPorts = this.getPorts(blockElement, "clock");

        BlockType blockType = BlockTypeData.getInstance().addType(
                parentBlockType,
                blockName,
                blockCategory,
                num_pb,
                height,
                start,
                repeat,
                priority,
                fcIn,
                fcOut,
                isClocked,
                inputs,
                outputs,
                clockPorts);

        // If block type is null, this means that there are two block
        // types in the architecture file that we cannot differentiate.
        // This should of course never happen.
        assert(blockType != null);

        // Set the carry chain, if there is one
        Pair<PortType, PortType> direct = this.getDirect(blockType, blockElement);

        if(direct != null) {
            PortType carryFromPort = direct.getFirst();
            PortType carryToPort = direct.getSecond();

            String directId = this.getDirectId(
                    blockType.getName(),
                    carryFromPort.getName(),
                    carryToPort.getName());
            int offsetY = this.directs.get(directId);

            PortTypeData.getInstance().setCarryPorts(carryFromPort, carryToPort, offsetY);
        }

        // Add the different modes and process the children for that mode
        /* Ugly data structure, but otherwise we would have to split it up
         * in multiple structures. Each pair in the list represents a mode
         * of this block type and its properties.
         *   - the first part is a pair that contains a mode name and the
         *     corresponding mode element
         *   - the second part is a list of child types. For each child
         *     type the number of children and the corresponding child
         *     Element are stored.
         */
        List<Pair<Pair<BlockType, Element>, List<Pair<Integer, BlockType>>>> modesAndChildren = this.getModesAndChildren(blockType, blockElement);

        for(Pair<Pair<BlockType, Element>, List<Pair<Integer, BlockType>>> modeAndChildren : modesAndChildren) {

            Pair<BlockType, Element> mode = modeAndChildren.getFirst();
            BlockType blockTypeWithMode = mode.getFirst();
            Element modeElement = mode.getSecond();

            List<BlockType> blockTypes = new ArrayList<>();
            blockTypes.add(blockType);

            // Add all the child types
            for(Pair<Integer, BlockType> child : modeAndChildren.getSecond()) {
                Integer numChildren = child.getFirst();
                BlockType childBlockType = child.getSecond();
                blockTypes.add(childBlockType);

                BlockTypeData.getInstance().addChild(blockTypeWithMode, childBlockType, numChildren);
            }

            // Cache delays to and from this element
            // We can't store them in PortTypeData until all blocks have been stored
            this.cacheDelays(blockTypes, modeElement);
        }

        // Cache setup times (time from input to clock, and from clock to output)
        this.cacheSetupTimes(blockType, blockElement);


        return blockType;
    }



    private boolean isLeaf(Element blockElement) {
        if(this.hasImplicitChildren(blockElement)) {
            return false;

        } else {
            return blockElement.hasAttribute("blif_model");
        }
    }
    private boolean hasImplicitChildren(Element blockElement) {
        if(blockElement.hasAttribute("class")) {
            String blockClass = blockElement.getAttribute("class");
            return blockClass.equals("lut") || blockClass.equals("memory");

        } else {
            return false;
        }
    }

    private boolean isGlobal(Element blockElement) {
        Element parentElement = (Element) blockElement.getParentNode();
        return parentElement.getTagName().equals("complexblocklist");
    }

    private boolean isClocked(Element blockElement) {
        if(!isLeaf(blockElement)) {
            return false;
        }

        String blifModel = blockElement.getAttribute("blif_model");
        switch(blifModel) {
            case ".names":
                return false;

            case ".latch":
            case ".input":
            case ".output":
                return true;

            default:
                // blifModel starts with .subckt, we are interested in the second part
                String modelName = blifModel.substring(8);
                return this.modelIsClocked.get(modelName);
        }
    }


    private BlockCategory getGlobalBlockCategory(Element blockElement) {
        // Check if this block type should fill the FPGA
        // If it does, we call this the CLB type (there can
        // only be one clb type in an architecture)
        Element locElement = this.getFirstChild(this.getFirstChild(blockElement, "gridlocations"), "loc");
        String type = locElement.getAttribute("type");

        if(type.equals("fill")) {
            return BlockCategory.CLB;
        }

        // Descend down until a leaf block is found
        // If the leaf block has has blif_model .input or
        // .output, this is the io block type (there can
        // only be one io type in an architecture)
        Stack<Element> elements = new Stack<Element>();
        elements.add(blockElement);

        while(elements.size() > 0) {
            Element element = elements.pop();

            String blifModel = element.getAttribute("blif_model");
            if(blifModel.equals(".input") || blifModel.equals(".output")) {
                return BlockCategory.IO;
            }


            List<Element> modeElements = this.getChildElementsByTagName(element, "mode");
            modeElements.add(element);

            for(Element modeElement : modeElements) {
                elements.addAll(this.getChildElementsByTagName(modeElement, "pb_type"));
            }
        }

        return BlockCategory.HARDBLOCK;
    }



    private Map<String, Pair<Integer, Boolean>> getPorts(Element blockElement, String portType) {
        Map<String, Pair<Integer, Boolean>> ports = new HashMap<>();

        List<Element> portElements = this.getChildElementsByTagName(blockElement, portType);
        for(Element portElement : portElements) {
            String portName = portElement.getAttribute("name");

            int numPins = Integer.parseInt(portElement.getAttribute("num_pins"));
            boolean equivalent = Boolean.parseBoolean(portElement.getAttribute("equivalent"));
            
            ports.put(portName, new Pair<Integer, Boolean>(numPins, equivalent));
        }

        return ports;
    }


    private Pair<PortType, PortType> getDirect(BlockType blockType, Element blockElement) {

        PortType carryFromPort = null, carryToPort = null;

        Element fcElement = this.getFirstChild(blockElement, "fc");
        if(fcElement == null) {
            return null;
        }

        List<Element> pinElements = this.getChildElementsByTagName(fcElement, "pin");
        for(Element pinElement : pinElements) {
            double fcVal = Double.parseDouble(pinElement.getAttribute("fc_val"));

            if(fcVal == 0) {
                String portName = pinElement.getAttribute("name");
                PortType portType = new PortType(blockType, portName);

                if(portType.isInput()) {
                    assert(carryToPort == null);
                    carryToPort = portType;

                } else {
                    assert(carryFromPort == null);
                    carryFromPort = portType;
                }
            }
        }

        // carryInput and carryOutput must simultaneously be set or be null
        assert(!(carryFromPort == null ^ carryToPort == null));
        if(carryFromPort == null) {
            return null;
        } else {
            return new Pair<PortType, PortType>(carryFromPort, carryToPort);
        }
    }


    private List<Pair<Pair<BlockType, Element>, List<Pair<Integer, BlockType>>>> getModesAndChildren(BlockType blockType, Element blockElement) {

        List<Pair<Pair<BlockType, Element>, List<Pair<Integer, BlockType>>>> modesAndChildren = new ArrayList<>();

        String blockName = blockElement.getAttribute("name");

        if(this.hasImplicitChildren(blockElement)) {

            switch(blockElement.getAttribute("class")) {
                case "lut":
                {
                    // ASM: luts have two hardcoded modes that are not defined in the arch file:
                    //  - one with the same name as the block type (e.g. lut5 or lut6). In this mode
                    //    it has one child "lut".
                    //  - "wire": no children

                    BlockType blockTypeWithMode = BlockTypeData.getInstance().addMode(blockType, blockName);
                    Pair<BlockType, Element> mode = new Pair<>(blockTypeWithMode, blockElement);

                    BlockType childBlockType = this.addImplicitLut(blockTypeWithMode, blockElement);
                    List<Pair<Integer, BlockType>> modeChildren = new ArrayList<>();
                    modeChildren.add(new Pair<Integer, BlockType>(1, childBlockType));
                    modesAndChildren.add(new Pair<>(mode, modeChildren));


                    blockTypeWithMode = BlockTypeData.getInstance().addMode(blockType, "wire");
                    mode = new Pair<>(blockTypeWithMode, blockElement);

                    modeChildren = new ArrayList<>();
                    modesAndChildren.add(new Pair<>(mode, modeChildren));

                    break;
                }

                case "memory":
                {
                    // ASM: blocks of class "memory" have one mode "memory_slice" and x children of type
                    // "memory_slice", where x can be determined from the number of output pins in an
                    // output port with a class that begins with "data_out"

                    // Determine the number of children
                    int numChildren = -1;
                    for(Element outputElement : this.getChildElementsByTagName(blockElement, "output")) {
                        String portClass = outputElement.getAttribute("port_class");
                        if(portClass.startsWith("data_out")) {
                            int numPins = Integer.parseInt(outputElement.getAttribute("num_pins"));

                            if(numChildren == -1) {
                                numChildren = numPins;

                            } else if(numChildren != numPins) {
                                throw new ArchitectureException(String.format(
                                        "inconsistend number of data output pins in memory: %d and %d",
                                        numChildren, numPins));
                            }
                        }
                    }

                    BlockType blockTypeWithMode = BlockTypeData.getInstance().addMode(blockType, "memory_slice");
                    Pair<BlockType, Element> mode = new Pair<>(blockTypeWithMode, blockElement);

                    BlockType childBlockType = this.addImplicitMemorySlice(blockTypeWithMode, blockElement);
                    List<Pair<Integer, BlockType>> modeChildren = new ArrayList<>();
                    modeChildren.add(new Pair<Integer, BlockType>(numChildren, childBlockType));
                    modesAndChildren.add(new Pair<>(mode, modeChildren));

                    break;
                }

                default:
                    throw new ArchitectureException("Unknown block type with implicit children: " + blockName);
            }

        } else if(this.isLeaf(blockElement)) {
            // ASM: Leafs have 1 unnamed mode without children
            BlockType blockTypeWithMode = BlockTypeData.getInstance().addMode(blockType, "");
            Pair<BlockType, Element >mode = new Pair<>(blockTypeWithMode, blockElement);
            List<Pair<Integer, BlockType>> modeChildren = new ArrayList<>();

            modesAndChildren.add(new Pair<>(mode, modeChildren));

        } else {
            List<Element> modeElements = new ArrayList<>();
            modeElements.addAll(this.getChildElementsByTagName(blockElement, "mode"));

            // There is 1 mode with the same name as the block
            // There is 1 child block type
            if(modeElements.size() == 0) {
                modeElements.add(blockElement);
            }

            // Add the actual modes and their children
            for(Element modeElement : modeElements) {
                String modeName = modeElement.getAttribute("name");
                BlockType blockTypeWithMode = BlockTypeData.getInstance().addMode(blockType, modeName);
                Pair<BlockType, Element> mode = new Pair<>(blockTypeWithMode, modeElement);

                List<Pair<Integer, BlockType>> modeChildren = new ArrayList<>();
                List<Element> childElements = this.getChildElementsByTagName(modeElement, "pb_type");
                for(Element childElement : childElements) {
                    int numChildren = Integer.parseInt(childElement.getAttribute("num_pb"));
                    BlockType childBlockType = this.processBlockElement(blockTypeWithMode, childElement);
                    modeChildren.add(new Pair<>(numChildren, childBlockType));
                }

                modesAndChildren.add(new Pair<>(mode, modeChildren));
            }
        }

        return modesAndChildren;
    }


    private BlockType addImplicitLut(BlockType parentBlockType, Element parentBlockElement) {
        // A lut has exactly the same ports as its parent lut5/lut6/... block
        Map<String, Pair<Integer, Boolean>> inputPorts = this.getPorts(parentBlockElement, "input");
        Map<String, Pair<Integer, Boolean>> outputPorts = this.getPorts(parentBlockElement, "output");
        Map<String, Pair<Integer, Boolean>> clockPorts = this.getPorts(parentBlockElement, "clock");
        String lutName = "lut";

        BlockType lutBlockType = BlockTypeData.getInstance().addType(
                parentBlockType,
                lutName,
                BlockCategory.LEAF,
                1,
                -1,
                -1,
                -1,
                -1,
                -1.0,
                -1.0,
                false,
                inputPorts,
                outputPorts,
                clockPorts);

        // A lut has one unnamed mode without children
        BlockTypeData.getInstance().addMode(lutBlockType, "");

        // Process delays
        Element delayMatrixElement = this.getFirstChild(parentBlockElement, "delay_matrix");

        // ASM: all delays are equal and type is "max"
        String sourcePortName = delayMatrixElement.getAttribute("in_port").split("\\.")[1];
        PortType sourcePortType = new PortType(lutBlockType, sourcePortName);
        String sinkPortName = delayMatrixElement.getAttribute("out_port").split("\\.")[1];
        PortType sinkPortType = new PortType(lutBlockType, sinkPortName);

        String[] delays = delayMatrixElement.getTextContent().split("\\s+");
        int index = delays[0].length() > 0 ? 0 : 1;
        double delay = Double.parseDouble(delays[index]);

        this.delayElements.add(new DelayElement(-1, sourcePortType, -1 , -1, sinkPortType, -1, delay));

        return lutBlockType;
    }


    private BlockType addImplicitMemorySlice(BlockType parentBlockType, Element parentBlockElement) {
        // The memory slice has exactly the same ports as its parent,
        // except there is only one data_in and data_out

        Map<String, Pair<Integer, Boolean>> inputPorts = this.getPorts(parentBlockElement, "input");
        for(Element inputElement : this.getChildElementsByTagName(parentBlockElement, "input")) {
            if(inputElement.getAttribute("port_class").startsWith("data_in")) {
                inputPorts.put(inputElement.getAttribute("name"), new Pair<Integer, Boolean>(1, false));
            }
        }

        Map<String, Pair<Integer, Boolean>> outputPorts = this.getPorts(parentBlockElement, "output");
        for(Element outputElement : this.getChildElementsByTagName(parentBlockElement, "output")) {
            if(outputElement.getAttribute("port_class").startsWith("data_out")) {
                inputPorts.put(outputElement.getAttribute("name"), new Pair<Integer, Boolean>(1, false));
            }
        }

        Map<String, Pair<Integer, Boolean>> clockPorts = this.getPorts(parentBlockElement, "clock");

        String memorySliceName = "memory_slice";

        // ASM: memory slices are clocked
        BlockType memoryBlockType = BlockTypeData.getInstance().addType(
                parentBlockType,
                memorySliceName,
                BlockCategory.LEAF,
                1,
                -1,
                -1,
                -1,
                -1,
                -1.0,
                -1.0,
                true,
                inputPorts,
                outputPorts,
                clockPorts);

        // Add the new memory type as a child of the parent type
        BlockTypeData.getInstance().addChild(parentBlockType, memoryBlockType, 1);

        // A memory_slice has one unnamed mode without children
        BlockTypeData.getInstance().addMode(memoryBlockType, "");


        // Process setup times
        // These are added as delays between the parent and child element
        List<Element> setupElements = this.getChildElementsByTagName(parentBlockElement, "T_setup");
        for(Element setupElement : setupElements) {
            String sourcePortName = setupElement.getAttribute("port").split("\\.")[1];
            PortType sourcePortType = new PortType(parentBlockType, sourcePortName);
            PortType sinkPortType = new PortType(memoryBlockType, sourcePortName);

            double delay = Double.parseDouble(setupElement.getAttribute("value"));
            
            this.delayElements.add(new DelayElement(-1, sourcePortType, -1 , -1, sinkPortType, -1, delay));
        }

        // Process clock to port times
        // These are added as delays between the child and parent element
        List<Element> clockToPortElements = this.getChildElementsByTagName(parentBlockElement, "T_clock_to_Q");
        for(Element clockToPortElement : clockToPortElements) {
            String sinkPortName = clockToPortElement.getAttribute("port").split("\\.")[1];
            PortType sourcePortType = new PortType(memoryBlockType, sinkPortName);
            PortType sinkPortType = new PortType(parentBlockType, sinkPortName);
            
            double delay = Double.parseDouble(clockToPortElement.getAttribute("max"));
            
            this.delayElements.add(new DelayElement(-1, sourcePortType, -1 , -1, sinkPortType, -1, delay));
        }

        return memoryBlockType;
    }


    private void cacheSetupTimes(BlockType blockType, Element blockElement) {
        // Process setup times
        List<Element> setupElements = this.getChildElementsByTagName(blockElement, "T_setup");
        for(Element setupElement : setupElements) {
            String portName = setupElement.getAttribute("port").split("\\.")[1];
            PortType portType = new PortType(blockType, portName);
            double delay = Double.parseDouble(setupElement.getAttribute("value"));

            this.setupTimes.add(new Pair<PortType, Double>(portType, delay));
        }

        // Process clock to port times
        List<Element> clockToPortElements = this.getChildElementsByTagName(blockElement, "T_clock_to_Q");
        for(Element clockToPortElement : clockToPortElements) {
            String portName = clockToPortElement.getAttribute("port").split("\\.")[1];
            PortType portType = new PortType(blockType, portName);
            double delay = Double.parseDouble(clockToPortElement.getAttribute("max"));

            this.setupTimes.add(new Pair<PortType, Double>(portType, delay));
        }

        // ASM: delay_matrix elements only occur in blocks of class "lut"
        // These are processed in addImplicitLut()
    }


    private void cacheDelays(List<BlockType> blockTypes, Element modeElement) {

        /* ASM: delays are defined in a delay_constant tag. These tags are children of
         * one of the next elements:
         *   - modeElement
         *   - modeElement - <interconnect> - <direct>
         *   - modeElement - <interconnect> - <complete>
         *   - modeElement - <interconnect> - <mux>
         *   - child <direct> elements of modeElement
         * One exception are luts: the lut delays are defined in a delay_matrix element.
         * See addImplicitLut().
         */

        List<Element> elements = new ArrayList<Element>();
        elements.add(modeElement);
        Element interconnectElement = this.getFirstChild(modeElement, "interconnect");
        if(interconnectElement != null) {
            elements.addAll(this.getChildElementsByTagName(interconnectElement, "direct"));
            elements.addAll(this.getChildElementsByTagName(interconnectElement, "complete"));
            elements.addAll(this.getChildElementsByTagName(interconnectElement, "mux"));
        }

        for(Element element : elements) {
            for(Element delayConstantElement : this.getChildElementsByTagName(element, "delay_constant")) {

                double delay = Double.parseDouble(delayConstantElement.getAttribute("max"));
                String[] sourcePorts = delayConstantElement.getAttribute("in_port").split("\\s+");
                String[] sinkPorts = delayConstantElement.getAttribute("out_port").split("\\s+");

                for(String sourcePort : sourcePorts) {
                    for(String sinkPort : sinkPorts) {
                        if(!(sourcePort.length() == 0 || sinkPort.length() == 0)) {
                            this.cacheDelay(blockTypes, sourcePort, sinkPort, delay);
                        }
                    }
                }
            }
        }
    }

    private void cacheDelay(List<BlockType> blockTypes, String sourcePort, String sinkPort, double delay) {
    	//SOURCE PORT
    	List<Integer> sourceBlockIndexes = new ArrayList<>();
    	List<Integer> sourcePortIndexes = new ArrayList<>();
    	
    	BlockType sourcePortBlockType = this.getPortBlockType(sourcePort, blockTypes);
    	PortType sourcePortType = getPortType(sourcePort, sourcePortBlockType);
    	this.processPort(sourcePort, sourceBlockIndexes, sourcePortIndexes, sourcePortType, sourcePortBlockType);
        
    	//SINK PORT
        List<Integer> sinkBlockIndexes = new ArrayList<>();
        List<Integer> sinkPortIndexes = new ArrayList<>();
        
    	BlockType sinkPortBlockType = this.getPortBlockType(sinkPort, blockTypes);
    	PortType sinkPortType = getPortType(sinkPort, sinkPortBlockType);
    	this.processPort(sinkPort, sinkBlockIndexes, sinkPortIndexes, sinkPortType, sinkPortBlockType);
        
        for(int sourceBlockIndex : sourceBlockIndexes) {
            for(int sinkBlockIndex : sinkBlockIndexes) {
                for(int sourcePortIndex : sourcePortIndexes) {
                    for(int sinkPortIndex : sinkPortIndexes) {
                        this.delayElements.add(new DelayElement(sourceBlockIndex, sourcePortType, sourcePortIndex, sinkBlockIndex, sinkPortType, sinkPortIndex, delay));
                    }
                }
            }
        }
    }
    private BlockType getPortBlockType(String port, List<BlockType> blockTypes) {
    	String[] portParts = port.split("\\.");
        String blockName = portParts[0].split("\\[")[0];
        
        for(BlockType blockType : blockTypes) {
            if(blockName.equals(blockType.getName())) {
                return blockType;
            }
        }
        return null;
    }
    private PortType getPortType(String port, BlockType portBlockType) {
    	String[] portParts = port.split("\\.");
    	String portName = portParts[1].split("\\[")[0];
    	
    	return new PortType(portBlockType, portName);
    }
    private void processPort(String port, List<Integer> blockIndexes, List<Integer> portIndexes, PortType portType, BlockType blockType) {
        String[] portParts = port.split("\\.");
        
        //Block
        String blockName = portParts[0];
        if (blockName.contains("[") || blockName.contains("]")) {
        	String blockNumbers = blockName.split("\\[")[1].split("\\]")[0];
        	
        	if(blockNumbers.contains(":")) {
        		int end = Integer.parseInt(blockNumbers.split(":")[0]);
        		int start = Integer.parseInt(blockNumbers.split(":")[1]);
           		
        		if(start > end) {
        			int temp = end;
        			end = start;
        			start = temp;
        		}
           		
           		if( (end - start + 1) != blockType.num_pb()) {
            		for(int p = start; p <= end; p++) {
            			blockIndexes.add(p);
            		}
           		}
        	} else {
        		blockIndexes.add(Integer.parseInt(blockNumbers));
        	}
        }
        blockIndexes.add(-1);
        
        //Port
        String portName = portParts[1];
        if (portName.contains("[") || portName.contains("]")) {
        	String portNumbers = portName.split("\\[")[1].split("\\]")[0];
        	
        	if(portNumbers.contains(":")) {
        		int end = Integer.parseInt(portNumbers.split(":")[0]);
        		int start = Integer.parseInt(portNumbers.split(":")[1]);
        		
        		if(start > end) {
        			int temp = end;
        			end = start;
        			start = temp;
        		}
        		
        		if( (end - start + 1) != portType.numPins()) {
            		for(int p = start; p <= end; p++) {
            			portIndexes.add(p);
            		}
           		}
        	} else {
        		portIndexes.add(Integer.parseInt(portNumbers));
        	}	
        }
        portIndexes.add(-1);
        
    }
    
    private void processDelays() {
        for(Pair<PortType, Double> setupTimeEntry : this.setupTimes) {
            PortType portType = setupTimeEntry.getFirst();
            double delay = setupTimeEntry.getSecond();

            portType.setSetupTime(delay);
        }

        for(DelayElement delayElement : this.delayElements) {
            PortType sourcePortType = delayElement.sourcePortType;
            sourcePortType.setDelay(delayElement);
        }
    }


    private Element getFirstChild(Element blockElement, String tagName) {
        NodeList childNodes = blockElement.getChildNodes();
        for(int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                if(childElement.getTagName().equals(tagName)) {
                    return childElement;
                }
            }
        }

        return null;
    }

    private List<Element> getChildElementsByTagName(Element blockElement, String tagName) {
        List<Element> childElements = new ArrayList<Element>();

        NodeList childNodes = blockElement.getChildNodes();
        for(int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                if(childElement.getTagName().equals(tagName)) {
                    childElements.add(childElement);
                }
            }
        }

        return childElements;
    }





    public void getVprTiming(String vprCommand) throws IOException, InterruptedException, InvalidFileFormatException {
        // For this method to work, the macro PRINT_ARRAYS should be defined
        // in vpr: place/timing_place_lookup.c

        // Run vpr
        String command = String.format(
                "%s %s %s --blif_file %s --net_file %s --place_file vpr_tmp.place --place --init_t 1 --exit_t 1",
                vprCommand, this.architectureFile, this.circuitName, this.blifFile, this.netFile);

        Process process = null;
        process = Runtime.getRuntime().exec(command);


        // Read output to avoid buffer overflow and deadlock
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((reader.readLine()) != null) {}
        process.waitFor();


        // Build delay tables
        String lookupDumpPath = "lookup_dump.echo";
        File lookupDumpFile = new File(lookupDumpPath);
        this.buildDelayTables(lookupDumpFile);

        // Clean up
        this.deleteFile("vpr_tmp.place");
        this.deleteFile("vpr_stdout.log");
        this.deleteFile(lookupDumpPath);
    }

    public void getVprTiming(File lookupDumpFile) throws IOException, InvalidFileFormatException {
        this.buildDelayTables(lookupDumpFile);
    }

    private void buildDelayTables(File lookupDumpFile) throws IOException, InvalidFileFormatException {
        // Parse the delay tables
        this.delayTables = new DelayTables(lookupDumpFile);
        this.delayTables.parse();
    }

    private void deleteFile(String path) throws IOException {
        Files.deleteIfExists(new File(path).toPath());
    }



    public boolean isAutoSized() {
        return this.autoSize;
    }
    public double getAutoRatio() {
        return this.autoRatio;
    }
    public int getWidth() {
        return this.width;
    }
    public int getHeight() {
        return this.height;
    }

    public DelayTables getDelayTables() {
        return this.delayTables;
    }



    public int getIoCapacity() {
        return this.ioCapacity;
    }


    public boolean isImplicitBlock(String blockTypeName) {
        // ASM: lut and memory_slice are the only possible implicit blocks
        return blockTypeName.equals("lut") || blockTypeName.equals("memory_slice");
    }
    public String getImplicitBlockName(String parentBlockTypeName, String blockTypeName) {
        return parentBlockTypeName + "." + blockTypeName;
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(BlockTypeData.getInstance());
        out.writeObject(PortTypeData.getInstance());
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        BlockTypeData.setInstance((BlockTypeData) in.readObject());
        PortTypeData.setInstance((PortTypeData) in.readObject());
    }


    public class ArchitectureException extends RuntimeException {
        private static final long serialVersionUID = 5348447367650914363L;

        ArchitectureException(String message) {
            super(message);
        }
    }
    
    public File getSDCFile() {
    	return this.sdcFile;
    }
    public File getRRGFile() {
    	return this.rrgFile;
    }
}
