package circuit.architecture;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.Pair;
import util.Triple;

import circuit.exceptions.InvalidFileFormatException;

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

    private File architectureFile, blifFile, netFile;
    private String circuitName;
    private transient String vprCommand;

    private transient Map<String, Boolean> modelIsClocked = new HashMap<>();
    private transient List<Pair<PortType, Double>> setupTimes = new ArrayList<>();
    private transient List<Triple<PortType, PortType, Double>> delays = new ArrayList<>();

    private DelayTables delayTables;

    private int ioCapacity;


    public Architecture(String circuitName, File architectureFile, String vprCommand, File blifFile, File netFile) {
        this.architectureFile = architectureFile;

        this.vprCommand = vprCommand;

        this.blifFile = blifFile;
        this.netFile = netFile;

        this.circuitName = circuitName;
    }

    public void parse() throws parseException, IOException, InvalidFileFormatException, InterruptedException, ParserConfigurationException, SAXException {

        // Build a XML root
        DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document xmlDocument = xmlBuilder.parse(this.architectureFile);
        Element root = xmlDocument.getDocumentElement();

        // Get the architecture size (fixed or automatic)
        this.processLayout(root);

        // Store the models to know which ones are clocked
        this.processModels(root);

        // The device, switchlist and segmentlist tags are ignored: we leave these complex calculations to VPR
        // Proceed with complexblocklist
        this.processBlocks(root);

        // Cache some frequently used data
        BlockTypeData.getInstance().postProcess();

        // All delays have been cached in this.delays, process them now
        this.processDelays();

        // Build the delay matrixes
        this.buildDelayMatrixes();
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


    private void processBlocks(Element root) throws parseException {
        Element blockListElement = this.getFirstChild(root, "complexblocklist");
        List<Element> blockElements = this.getChildElementsByTagName(blockListElement, "pb_type");

        for(Element blockElement : blockElements) {
            this.processBlockElement(blockElement);
        }
    }

    private void processBlockElement(Element blockElement) throws parseException {
        String blockName = blockElement.getAttribute("name");

        boolean isGlobal = this.isGlobal(blockElement);
        boolean isLeaf = this.isLeaf(blockElement);
        boolean isClocked = this.isClocked(blockElement);


        // Set block category, and some related properties
        BlockCategory blockCategory;

        // ASM: IO blocks are around the perimeter, HARDBLOCKs are in columns,
        // CLB blocks fill the rest of the FPGA
        int start = -1, repeat = -1, height = -1, priority = -1;


        if(isLeaf) {
            blockCategory = BlockCategory.LEAF;

        } else if(!isGlobal) {
            blockCategory = BlockCategory.INTERMEDIATE;

        } else {
            // Get some extra properties that relate to the placement
            // of global blocks

            blockCategory = this.getGlobalBlockCategory(blockElement);

            if(blockCategory == BlockCategory.IO) {
                // ASM: io blocks are always placed around the perimeter of the
                // architecture, ie. "<loc type="perimeter">" is set.
                // This assumption is used throughout this entire placement suite.
                // Many changes will have to be made in the different algorithms
                // in order to resolve this assumption.
                this.ioCapacity = Integer.parseInt(blockElement.getAttribute("capacity"));

            } else {
                if(blockElement.hasAttribute("height")) {
                    height = Integer.parseInt(blockElement.getAttribute("height"));
                } else {
                    height = 1;
                }

                Element gridLocationsElement = this.getFirstChild(blockElement, "gridlocations");
                Element locElement = this.getFirstChild(gridLocationsElement, "loc");

                String type = locElement.getAttribute("type");
                if(type.equals("fill")) {
                    start = 1;
                    repeat = 1;

                } else {
                    start = Integer.parseInt(locElement.getAttribute("start"));
                    repeat = Integer.parseInt(locElement.getAttribute("repeat"));
                }

                priority = Integer.parseInt(locElement.getAttribute("priority"));
            }

        }

        // Build maps of inputs and outputs
        Map<String, Integer> inputs = this.getPorts(blockElement, "input");
        Map<String, Integer> outputs = this.getPorts(blockElement, "output");

        // Get the modes and children
        List<Element> modeElements = new ArrayList<>();
        List<Element> childElements = new ArrayList<>();
        List<String> modes = new ArrayList<>();
        List<Map<String, Integer>> children = new ArrayList<>();
        this.getModesAndChildren(blockElement, modeElements, childElements, modes, children);


        boolean success = BlockTypeData.getInstance().addType(
                blockName,
                blockCategory,
                height,
                start,
                repeat,
                priority,
                isClocked,
                inputs,
                outputs);

        // Flipflops are defined twice in some arch files, this is no problem
        // If any other blocks are defined more than once, we'd like to know it
        if(!success) {
            if(!blockName.equals("ff")) {
                System.err.println("Duplicate block type detected in architecture file: " + blockName);
            }

            return;
        }

        for(int i = 0; i < modes.size(); i++) {
            BlockTypeData.getInstance().addMode(blockName, modes.get(i), children.get(i));
        }


        // Process children
        this.addImplicitChildren(blockElement);
        for(Element childElement : childElements) {
            this.processBlockElement(childElement);
        }


        // Cache setup times and delays
        // We can't store them in PortTypeData until all blocks have been stored
        this.cacheSetupTimes(blockElement);
        for(Element modeElement : modeElements) {
            this.cacheDelays(modeElement);
        }
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


    private BlockCategory getGlobalBlockCategory(Element blockElement) throws parseException {
        // Descend down until a leaf block is found
        // Use this leaf block to determine the block category
        Element childElement = blockElement;

        while(!childElement.hasAttribute("blif_model")) {
            Element newChildElement = this.getFirstChild(childElement, "pb_type");
            if(newChildElement == null) {
                childElement = this.getFirstChild(childElement, "mode");
            } else {
                childElement = newChildElement;
            }
        }

        String model = childElement.getAttribute("blif_model").split(" ")[0];

        switch(model) {
            case ".input":
            case ".output":
                return BlockCategory.IO;

            case ".names":
            case ".latch":
                return BlockCategory.CLB;

            case ".subckt":
                return BlockCategory.HARDBLOCK;

            default:
                throw new parseException("Unknown model type: " + model);
        }
    }



    private Map<String, Integer> getPorts(Element blockElement, String portType) {
        Map<String, Integer> ports = new HashMap<>();

        List<Element> portElements = this.getChildElementsByTagName(blockElement, portType);
        for(Element portElement : portElements) {
            String portName = portElement.getAttribute("name");

            // ASM: circuits have only 1 clock, ignore clock inputs
            if(!portName.equals("clk")) {
                int numPins = Integer.parseInt(portElement.getAttribute("num_pins"));
                ports.put(portName, numPins);
            }
        }

        return ports;
    }

    private void getModesAndChildren(
            Element blockElement,
            List<Element> returnModeElements,
            List<Element> returnChildElements,
            List<String> returnModes,
            List<Map<String, Integer>> returnChildren) {

        if(this.hasImplicitChildren(blockElement) || this.isLeaf(blockElement)) {
            String blockName = blockElement.getAttribute("name");

            switch(blockElement.getAttribute("class")) {
                case "lut":
                    // ASM: luts have two hardcoded modes that are not defined in the arch file:
                    //  - one with the same name as the block type (e.g. lut5 or lut6). In this mode
                    //    it has one child "lut".
                    //  - "wire": no children

                    returnModes.add(blockName);


                    String lutChildName = this.getImplicitBlockName(blockName, "lut");
                    int numLutChildren = 1;

                    Map<String, Integer> lutChildren = new HashMap<String, Integer>();
                    lutChildren.put(lutChildName, numLutChildren);
                    returnChildren.add(lutChildren);

                    returnModes.add("wire");
                    returnChildren.add(new HashMap<String, Integer>());
                    break;

                case "memory":
                    // ASM: blocks of class "memory" have one mode "memory_slice" and x children of type
                    // "memory_slice", where x can be determined from the number of output ports
                    returnModes.add("memory_slice");


                    String memoryChildName = this.getImplicitBlockName(blockName, "memory_slice");

                    Element outputElement = this.getFirstChild(blockElement, "output");
                    int numMemoryChildren = Integer.parseInt(outputElement.getAttribute("num_pins"));

                    Map<String, Integer> memoryChildren = new HashMap<String, Integer>();
                    memoryChildren.put(memoryChildName, numMemoryChildren);
                    returnChildren.add(memoryChildren);
                    break;

                default:
                    // ASM: Leafs have 1 unnamed mode without children
                    returnModes.add("");
                    returnChildren.add(new HashMap<String, Integer>());
                    returnModeElements.add(blockElement);
            }

            return;
        }

        returnModeElements.addAll(this.getChildElementsByTagName(blockElement, "mode"));

        // There is 1 mode with the same name as the block
        // There is 1 child block type
        if(returnModeElements.size() == 0) {
            returnModeElements.add(blockElement);
        }

        // Add the actual modes and their children
        for(Element modeElement : returnModeElements) {
            Map<String, Integer> modeChildren = new HashMap<>();

            List<Element> childElements = this.getChildElementsByTagName(modeElement, "pb_type");
            for(Element childElement : childElements) {
                returnChildElements.add(childElement);

                String childName = childElement.getAttribute("name");
                int childNum = Integer.parseInt(childElement.getAttribute("num_pb"));

                modeChildren.put(childName, childNum);
            }

            returnModes.add(modeElement.getAttribute("name"));
            returnChildren.add(modeChildren);
        }
    }


    private void addImplicitChildren(Element blockElement) {
        if(!this.hasImplicitChildren(blockElement)) {
            return;
        }

        switch(blockElement.getAttribute("class")) {
        case "lut":
            this.addImplicitLut(blockElement);
            break;

        case "memory":
            this.addImplicitMemorySlice(blockElement);
            break;

        default:
            System.out.println("Failed to add implicit children for block type: " + blockElement.getAttribute("class"));
            System.exit(1);
        }
    }


    private void addImplicitLut(Element blockElement) {
        // A lut has exactly the same ports as its parent lut5/lut6/... block
        Map<String, Integer> inputPorts = this.getPorts(blockElement, "input");
        Map<String, Integer> outputPorts = this.getPorts(blockElement, "output");
        String lutName = this.getImplicitBlockName(blockElement.getAttribute("name"), "lut");

        BlockTypeData.getInstance().addType(
                lutName,
                BlockCategory.LEAF,
                -1,
                -1,
                -1,
                -1,
                false,
                inputPorts,
                outputPorts);

        BlockTypeData.getInstance().addMode(lutName, "", new HashMap<String, Integer>());

        // Process delays
        Element delayMatrixElement = this.getFirstChild(blockElement, "delay_matrix");

        // ASM: all delays are equal and type is "max"
        PortType sourcePortType = this.getPortType(delayMatrixElement.getAttribute("in_port"));
        PortType sinkPortType = new PortType(lutName, "in");

        String[] delays = delayMatrixElement.getTextContent().split("\\s+");
        int index = delays[0].length() > 0 ? 0 : 1;
        double delay = Double.parseDouble(delays[index]);

        this.delays.add(new Triple<PortType, PortType, Double>(sourcePortType, sinkPortType, delay));
    }


    private void addImplicitMemorySlice(Element blockElement) {
        Map<String, Integer> inputPorts = this.getPorts(blockElement, "input");
        for(Element inputElement : this.getChildElementsByTagName(blockElement, "input")) {
            if(inputElement.getAttribute("port_class").startsWith("data_in")) {
                inputPorts.put(inputElement.getAttribute("name"), 1);
            }
        }

        Map<String, Integer> outputPorts = this.getPorts(blockElement, "output");
        for(Element outputElement : this.getChildElementsByTagName(blockElement, "output")) {
            if(outputElement.getAttribute("port_class").startsWith("data_out")) {
                inputPorts.put(outputElement.getAttribute("name"), 1);
            }
        }

        String memorySliceName = this.getImplicitBlockName(blockElement.getAttribute("name"), "memory_slice");

        // ASM: memory slices are clocked
        BlockTypeData.getInstance().addType(
                memorySliceName,
                BlockCategory.LEAF,
                -1,
                -1,
                -1,
                -1,
                true,
                inputPorts,
                outputPorts);
        BlockTypeData.getInstance().addMode(memorySliceName, "", new HashMap<String, Integer>());


        // Process setup times
        // These are added as delays between the parent and child element
        List<Element> setupElements = this.getChildElementsByTagName(blockElement, "T_setup");
        for(Element setupElement : setupElements) {
            String sourcePort = setupElement.getAttribute("port");
            PortType sourcePortType = this.getPortType(sourcePort);

            String portName = sourcePort.split("\\.")[1];
            PortType sinkPortType = new PortType(memorySliceName, portName);

            double delay = Double.parseDouble(setupElement.getAttribute("value"));

            this.delays.add(new Triple<PortType, PortType, Double>(sourcePortType, sinkPortType, delay));
        }

        // Process clock to port times
        // These are added as delays between the parent and child element
        List<Element> clockToPortElements = this.getChildElementsByTagName(blockElement, "T_clock_to_Q");
        for(Element clockToPortElement : clockToPortElements) {
            String sinkPort = clockToPortElement.getAttribute("port");
            PortType sinkPortType = this.getPortType(sinkPort);

            String portName = sinkPort.split("\\.")[1];
            PortType sourcePortType = new PortType(memorySliceName, portName);

            double delay = Double.parseDouble(clockToPortElement.getAttribute("max"));

            this.delays.add(new Triple<PortType, PortType, Double>(sourcePortType, sinkPortType, delay));
        }
    }




    private void cacheSetupTimes(Element blockElement) {
        // Process setup times
        List<Element> setupElements = this.getChildElementsByTagName(blockElement, "T_setup");
        for(Element setupElement : setupElements) {
            PortType portType = this.getPortType(setupElement.getAttribute("port"));
            double delay = Double.parseDouble(setupElement.getAttribute("value"));

            this.setupTimes.add(new Pair<PortType, Double>(portType, delay));
        }

        // Process clock to port times
        List<Element> clockToPortElements = this.getChildElementsByTagName(blockElement, "T_clock_to_Q");
        for(Element clockToPortElement : clockToPortElements) {
            PortType portType = this.getPortType(clockToPortElement.getAttribute("port"));
            double delay = Double.parseDouble(clockToPortElement.getAttribute("max"));

            this.setupTimes.add(new Pair<PortType, Double>(portType, delay));
        }

        // ASM: delay_matrix elements only occur in blocks of class "lut"
        // These are processed in addImplicitLut()
    }


    private void cacheDelays(Element modeElement) {

        // ASM: delays are defined in a delay_constant tag. These tags are children of
        // one of the next elements:
        //   - modeElement itself
        //   - child <interconnect> elements of modeElement
        //   - child <direct> elements of modeElement
        // One exception are luts: the lut delays are defined in a delay_matrix element.
        // See addImplicitLut().

        List<Element> elements = new ArrayList<Element>();
        elements.add(modeElement);
        elements.addAll(this.getChildElementsByTagName(modeElement, "interconnect"));
        elements.addAll(this.getChildElementsByTagName(modeElement, "direct"));

        for(Element element : elements) {
            NodeList delayConstantNodes = element.getElementsByTagName("delay_constant");
            for(int i = 0; i < delayConstantNodes.getLength(); i++) {
                Element delayConstantElement = (Element) delayConstantNodes.item(i);

                PortType sourcePortType = this.getPortType(delayConstantElement.getAttribute("in_port"));
                PortType sinkPortType = this.getPortType(delayConstantElement.getAttribute("out_port"));

                double delay = Double.parseDouble(delayConstantElement.getAttribute("max"));

                // ASM: the element with blif_model .input is located directly under the
                // global input block type
                // PortTypeData uses this assumption to set the clock setup time
                this.delays.add(new Triple<PortType, PortType, Double>(sourcePortType, sinkPortType, delay));
            }
        }
    }


    private void processDelays() {
        for(Pair<PortType, Double> setupTimeEntry : this.setupTimes) {
            PortType portType = setupTimeEntry.getFirst();
            double delay = setupTimeEntry.getSecond();

            portType.setSetupTime(delay);
        }

        for(Triple<PortType, PortType, Double> delayEntry : this.delays) {
            PortType sourcePortType = delayEntry.getFirst();
            PortType sinkPortType = delayEntry.getSecond();
            double delay = delayEntry.getThird();

            sourcePortType.setDelay(sinkPortType, delay);
        }
    }


    private PortType getPortType(String definition) {
        String[] parts = definition.split("\\.");
        for(int i = 0; i < 2; i++) {
            String part = parts[i];
            int bracketIndex = part.indexOf('[');
            if(bracketIndex > -1) {
                parts[i] = part.substring(0, bracketIndex);
            }
        }

        return new PortType(parts[0], parts[1]);
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




    private void buildDelayMatrixes() throws IOException, InvalidFileFormatException, InterruptedException {
        // For this method to work, the macro PRINT_ARRAYS should be defined
        // in vpr: place/timing_place_lookup.c


        // Run vpr
        String command = String.format(
                "%s %s %s --blif_file %s --net_file %s --place_file vpr_tmp --place --init_t 1 --exit_t 1",
                this.vprCommand, this.architectureFile, this.circuitName, this.blifFile, this.netFile);

        Process process = null;
        process = Runtime.getRuntime().exec(command);


        // Read output to avoid buffer overflow and deadlock
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((reader.readLine()) != null) {}
        process.waitFor();


        // Parse the delay tables
        File delaysFile = new File("lookup_dump.echo");
        this.delayTables = new DelayTables(delaysFile);
        this.delayTables.parse();

        // Clean up
        this.deleteFile("vpr_tmp");
        this.deleteFile("vpr_stdout.log");
        this.deleteFile("lookup_dump.echo");
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
}
