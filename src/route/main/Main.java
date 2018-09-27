package route.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import route.circuit.Circuit;
import route.circuit.architecture.Architecture;
import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.BlockType;
import route.circuit.exceptions.InvalidFileFormatException;
import route.circuit.io.NetParser;

import route.circuit.block.GlobalBlock;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import route.circuit.exceptions.PlacementException;
import route.circuit.io.BlockNotFoundException;
import route.circuit.io.HierarchyParser;
import route.circuit.io.IllegalSizeException;
import route.circuit.io.PlaceParser;
import route.circuit.pin.AbstractPin;
import route.circuit.pin.Pin;
import route.hierarchy.HierarchyNode;
import route.hierarchy.LeafNode;
import route.route.Connection;
import route.route.ConnectionRouter;

public class Main {
	
	private Logger logger;
	
	private String circuitName;
	private File architectureFile, blifFile, netFile, placeFile, hierarchyFile, lookupDumpFile, sdcFile, rrgFile;
	
	private boolean parallel_routing;
	private int num_route_nodes;
	
	private boolean useVprTiming;
	
	private Circuit circuit;
	private HierarchyNode rootNode;

	public Main(Logger logger, String[] arguments) {
		for(int i = 0; i < arguments.length; i++) {
			if(arguments[i].contains("architecture_file")) {
				this.architectureFile = new File(arguments[++i]);
			} if(arguments[i].contains("blif_file")) {
				this.blifFile = new File(arguments[++i]);
			} else if(arguments[i].contains("sdc_file")) {
				this.sdcFile = new File(arguments[++i]);
			} else if(arguments[i].contains("net_file")) {
				this.netFile = new File(arguments[++i]);
			} else if(arguments[i].contains("place_file")) {
				this.placeFile = new File(arguments[++i]);
			} else if(arguments[i].contains("hierarchy_file")) {
				this.hierarchyFile = new File(arguments[++i]);
			} else if(arguments[i].contains("lookup_dump_file")) {
				this.lookupDumpFile = new File(arguments[++i]);
			} else if(arguments[i].contains("rr_graph_file")) {
				this.rrgFile = new File(arguments[++i]);
			} else if(arguments[i].contains("parallel_routing")) {
				this.parallel_routing = Boolean.parseBoolean(arguments[++i]);
			} else if(arguments[i].contains("num_route_nodes")) {
				this.num_route_nodes = Integer.parseInt(arguments[++i]);
			}
		}
		
		this.logger = logger;
		
		this.useVprTiming = true;
		
		this.circuitName = this.blifFile.getName().replaceFirst("(.+)\\.blif", "$1");
		this.logger.println("Circuit : " + this.circuitName);
		
		this.checkFileExistence("Architecture file", this.architectureFile);
		this.checkFileExistence("Blif file", this.blifFile);
		this.checkFileExistence("Net file", this.netFile);
		this.checkFileExistence("Place file", this.placeFile);
		this.checkFileExistence("Hierarchy file", this.hierarchyFile);
		this.checkFileExistence("SDC file", this.sdcFile);
		this.checkFileExistence("RRG file", this.rrgFile);
	
		this.loadCircuit();
		this.detaildGlobalBlockInformation();
		
		this.printNumBlocks();
		
		this.readPlaceFile();
		this.processHierarchy();
		
		this.sanityCheck();

					
		long start = System.nanoTime();
		ConnectionRouter route = new ConnectionRouter(this.circuit.getResourceGraph(), this.circuit);
		int numIterations = route.route(this.rootNode, this.parallel_routing, this.num_route_nodes);
		long end = System.nanoTime();
		
		System.out.printf("Routing took %.2fs\n",((end - start) * Math.pow(10, -9)));
		
		System.out.println("Routing succeeded after " + numIterations + " iterations");
		System.out.println("The total wirelength is equal to " + this.circuit.getResourceGraph().totalWireLengt());
		System.out.println("The total congested wirelength is equal to " + this.circuit.getResourceGraph().congestedTotalWireLengt());
		System.out.println("The number of used wire segments is equal to " + this.circuit.getResourceGraph().wireSegmentsUsed());
		System.out.println("Maximum net length " + this.circuit.maximumNetLength());
	}
    private void loadCircuit() {
    	//Process the architecture file
    	Architecture architecture = new Architecture(
    			this.circuitName,
    			this.architectureFile,
    			this.blifFile,
    			this.netFile,
    			this.sdcFile,
    			this.rrgFile);
    	try {
    		architecture.parse();
    	} catch(IOException | InvalidFileFormatException | InterruptedException | ParserConfigurationException | SAXException error) {
    		this.logger.raise("Failed to parse architecture file or delay tables", error);
    	}

    	if(this.useVprTiming) {
    		try {
    			if(this.lookupDumpFile == null) {
    				this.logger.raise("Failed to find lookupDumpFile");
    			} else {
    				architecture.getVprTiming(this.lookupDumpFile);
    			}
    		} catch(IOException | InvalidFileFormatException error) {
    			this.logger.raise("Failed to get vpr delays", error);
    		}
    	}

    	// Parse net file
    	try {
    		NetParser netParser = new NetParser(architecture, this.circuitName, this.netFile);
    		this.circuit = netParser.parse();
    		this.logger.println(this.circuit.stats());
    	} catch(IOException error) {
    		this.logger.raise("Failed to read net file", error);
    	}
    }
    private void readPlaceFile() {
        // Read the place file
        if(this.placeFile != null){
            PlaceParser placeParser = new PlaceParser(this.circuit, this.placeFile);
            try {
                placeParser.parse();
            } catch(IOException | BlockNotFoundException | PlacementException | IllegalSizeException error) {
                this.logger.raise("Something went wrong while parsing the place file", error);
            }
        }else{
        	this.logger.raise("No valid place file");
        }
        this.circuit.loadNetsAndConnections();
    }
    private void processHierarchy() {
    	this.readHierarchyFile();
		
    	for(Connection con : this.circuit.getConnections()) {
    		con.setLeafNode();
    	}
		
    	Map<LeafNode, Set<Connection>> clusterConnections = new HashMap<>();
    	for(Connection connection : this.circuit.getConnections()) {
    		LeafNode ln = connection.leafNode;
			if(!clusterConnections.containsKey(ln)) {
				Set<Connection> temp = new HashSet<>();
				clusterConnections.put(ln, temp);
			}
			clusterConnections.get(ln).add(connection);
     	}
    	
    	for(LeafNode ln : clusterConnections.keySet()) {
    		for(Connection connection : clusterConnections.get(ln)) {
    			ln.addConnection(connection);
    		}
    	}
    }
    private void readHierarchyFile() {
        if(this.hierarchyFile != null){
        	HierarchyParser hierarchyParser = new HierarchyParser(this.circuit, this.hierarchyFile);
        	try {
        		this.rootNode = hierarchyParser.parse();
        	} catch(IOException error) {
                 this.logger.raise("Something went wrong while parsing the hierarchy file", error);
			}
        } else {
        	this.logger.raise("No valid hierarchy file");
        }
    }

	
    private void checkFileExistence(String prefix, File file) {
        if(file == null) {
        	this.logger.raise(new FileNotFoundException(prefix + " not given as an argument"));
        }

        if(!file.exists()) {
        	this.logger.raise(new FileNotFoundException(prefix + " " + file));

        } else if(file.isDirectory()) {
        	this.logger.raise(new FileNotFoundException(prefix + " " + file + " is a directory"));
        }
    }
    
    private void printNumBlocks() {
        int numLut = 0,
            numFf = 0,
            numClb = 0,
            numHardBlock = 0,
            numIo = 0;
        
        int numPLL = 0,
        	numM9K = 0, 
        	numM144K = 0, 
        	numDSP = 0;

        int numPins = 0;
        for(GlobalBlock block:this.circuit.getGlobalBlocks()){
        	numPins += block.numClockPins();
        	numPins += block.numInputPins();
        	numPins += block.numOutputPins();
        }
        for(BlockType blockType : BlockType.getBlockTypes()) {

            String name = blockType.getName();
            BlockCategory category = blockType.getCategory();
            int numBlocks = this.circuit.getBlocks(blockType).size();

            if(name.equals("lut")) {
                numLut += numBlocks;

            } else if(name.equals("ff") || name.equals("dff")) {
                numFf += numBlocks;

            } else if(category == BlockCategory.CLB) {
                numClb += numBlocks;

            } else if(category == BlockCategory.HARDBLOCK) {
                numHardBlock += numBlocks;
                
                if(blockType.equals(BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(0))){
                	numPLL += numBlocks;
                }else if(blockType.equals(BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(1))){
                	numDSP += numBlocks;
                }else if(blockType.equals(BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(2))){
                	numM9K += numBlocks;
                }else if(blockType.equals(BlockType.getBlockTypes(BlockCategory.HARDBLOCK).get(3))){
                	numM144K += numBlocks;
                }

            } else if(category == BlockCategory.IO) {
                numIo += numBlocks;
            }
        }

        this.logger.println("Circuit statistics:");
        this.logger.printf("   clb: %d\n      lut: %d\n      ff: %d\n   hardblock: %d\n      PLL: %d\n      DSP: %d\n      M9K: %d\n      M144K: %d\n   io: %d\n\n",
                numClb, numLut, numFf, numHardBlock, numPLL, numDSP, numM9K, numM144K, numIo);
        this.logger.print("   CLB usage ratio: " + String.format("%.3f",this.circuit.ratioUsedCLB())  + "\n");
        this.logger.print("   Dense circuit: " + this.circuit.dense() + "\n");
        this.logger.print("   Num pins: " + numPins + "\n\n");
    }
    
    private void sanityCheck() {
    	this.circuit.getResourceGraph().sanityCheck();
    }
    private void detaildGlobalBlockInformation() {
		for(GlobalBlock block : this.circuit.getGlobalBlocks()) {
			for(AbstractPin abstractPin : block.getOutputPins()) {
				Pin pin = (Pin) abstractPin;
				
				if(pin.getNetName() == null && pin.getNumSinks() > 0) {
					System.err.println("\t" + "Block: " + block.getName() + " " + "Net: " + pin.getNetName() + " " + pin.getNumSinks() + " " + pin.getPortName() + "[" + pin.getIndex() + "] " + pin.getPortType().isEquivalent());
				}
			}
		}
    }
}
