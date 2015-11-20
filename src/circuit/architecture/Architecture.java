package circuit.architecture;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.Pair;
import util.Triplet;

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
 *
 * This parser does not store interconnections. We just assume that all
 * connections in the net file are legal. Should someonse ever want to
 * write a packer, then this feature has to be implemented.
 *
 * ASM: block types have unique names. There is no need to check parent blocks
 * to find out the exact type of the block.
 *
 */
public class Architecture implements Serializable {

    private static final long serialVersionUID = -5436935126902935000L;

    private static final double FILL_GRADE = 1;

    private File architectureFile, blifFile, netFile;
    private String circuitName;
    private transient String vprCommand;

    private transient Map<String, Boolean> models = new HashMap<>();
    private transient List<Pair<PortType, Double>> setupTimes = new ArrayList<>();
    private transient List<Triplet<PortType, PortType, Double>> delays = new ArrayList<>();

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

        // Store the models to know which ones are clocked
        this.processModels(root);

        // The device, switchlist and segmentlist tags are ignored: we leave these complex calculations to VPR
        this.processBlocks(root);
        BlockTypeData.getInstance().postProcess();

        // All delays have been cached in this.delays, process them now
        this.processDelays();

        // Build the delay matrixes
        this.buildDelayMatrixes();
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

        this.models.put(modelName, isClocked);
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
        int start = 1, repeat = 1, height = 1;


        if(isLeaf) {
            blockCategory = BlockCategory.LEAF;

        } else {
            if(isGlobal) {
                blockCategory = this.getGlobalBlockCategory(blockElement);

                if(blockCategory == BlockCategory.IO) {
                    this.ioCapacity = Integer.parseInt(blockElement.getAttribute("capacity"));

                } else if(blockCategory == BlockCategory.HARDBLOCK) {
                    height = Integer.parseInt(blockElement.getAttribute("height"));

                    Element gridLocationsElement = this.getFirstChild(blockElement, "gridlocations");
                    Element locElement = this.getFirstChild(gridLocationsElement, "loc");

                    start = Integer.parseInt(locElement.getAttribute("start"));
                    repeat = Integer.parseInt(locElement.getAttribute("repeat"));
                }

            } else {
                blockCategory = BlockCategory.INTERMEDIATE;
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


        boolean success = BlockTypeData.getInstance().addType(blockName, blockCategory, height, start, repeat, isClocked, inputs, outputs);

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

    private boolean isGlobal(Element blockElement) {
        Element parentElement = (Element) blockElement.getParentNode();
        return parentElement.getTagName().equals("complexblocklist");
    }

    private boolean isLeaf(Element blockElement) {
        return blockElement.hasAttribute("blif_model");
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
            return this.models.get(modelName);
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

        // Leafs have 1 unnamed mode without children
        // Luts have two hardcoded modes that are not defined in the arch file:
        //  - one with the same name as the block type (e.g. lut5 or lut6). In this mode
        //    it has one child "lut", but we ignore this child.
        //  - "wire": no children
        if(isLeaf(blockElement)) {
            if(blockElement.getAttribute("blif_model").equals(".names")) {
                returnModes.add(blockElement.getAttribute("name"));
                returnChildren.add(new HashMap<String, Integer>());

                returnModes.add("wire");
                returnChildren.add(new HashMap<String, Integer>());

            } else {
                returnModes.add("");
                returnChildren.add(new HashMap<String, Integer>());
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

        // Process delay times for unclocked leaf blocks
        Element delayMatrixElement = this.getFirstChild(blockElement, "delay_matrix");
        if(delayMatrixElement != null) {
            // ASM: all delays are equal and type is "max"
            PortType sourcePortType = this.getPortType(delayMatrixElement.getAttribute("in_port"));
            PortType sinkPortType = this.getPortType(delayMatrixElement.getAttribute("out_port"));

            String[] delays = delayMatrixElement.getTextContent().split("\\s+");
            int index = delays[0].length() > 0 ? 0 : 1;
            double delay = Double.parseDouble(delays[index]);

            this.delays.add(new Triplet<PortType, PortType, Double>(sourcePortType, sinkPortType, delay));
        }
    }


    private void cacheDelays(Element modeElement) {
        Element interconnectElement = this.getFirstChild(modeElement, "interconnect");

        NodeList delayConstantNodes = interconnectElement.getElementsByTagName("delay_constant");
        for(int i = 0; i < delayConstantNodes.getLength(); i++) {
            Element delayConstantElement = (Element) delayConstantNodes.item(i);

            PortType sourcePortType = this.getPortType(delayConstantElement.getAttribute("in_port"));
            PortType sinkPortType = this.getPortType(delayConstantElement.getAttribute("out_port"));

            double delay = Double.parseDouble(delayConstantElement.getAttribute("max"));

            this.delays.add(new Triplet<PortType, PortType, Double>(sourcePortType, sinkPortType, delay));
        }
    }


    private void processDelays() {
        for(Pair<PortType, Double> setupTimeEntry : this.setupTimes) {
            PortType portType = setupTimeEntry.getFirst();
            double delay = setupTimeEntry.getSecond();

            portType.setSetupTime(delay);
        }

        for(Triplet<PortType, PortType, Double> delayEntry : this.delays) {
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


    public DelayTables getDelayTables() {
        return this.delayTables;
    }



    public int getIoCapacity() {
        return this.ioCapacity;
    }

    public double getFillGrade() {
        return Architecture.FILL_GRADE;
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
