package route.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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
import route.circuit.io.IllegalSizeException;
import route.circuit.io.PlaceParser;
import route.circuit.pin.AbstractPin;
import route.circuit.pin.GlobalPin;
import route.route.ConnectionRouter;

public class Main {
	
	private Logger logger;
	
	private String circuitName;
	private File architectureFile, blifFile, netFile, placeFile, lookupDumpFile, sdcFile, rrgFile;
	
	private float alphaWLD = 0, alphaTD = 0, pres_fac_mult = 0, share_multiplier = 0;
	
	private boolean td;
	
	private Circuit circuit;

	public Main(Logger logger, String[] arguments) {
		
		this.td = false;
		
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
			} else if(arguments[i].contains("lookup_dump_file")) {
 				this.lookupDumpFile = new File(arguments[++i]);
			} else if(arguments[i].contains("rr_graph_file")) {
				this.rrgFile = new File(arguments[++i]);

			} else if(arguments[i].contains("td_hroute")) {
				this.td = true;
			
			} else if(arguments[i].contains("alphaWLD")) {
				this.alphaWLD = Float.parseFloat(arguments[++i]);
			} else if(arguments[i].contains("alphaTD")) {
				this.alphaTD = Float.parseFloat(arguments[++i]);

			} else if(arguments[i].contains("pres_fac_mult")) {
				this.pres_fac_mult = Float.parseFloat(arguments[++i]);
			} else if(arguments[i].contains("share_multiplier")) {
				this.share_multiplier = Float.parseFloat(arguments[++i]);
			}
		}
		
		this.logger = logger;
		
		this.circuitName = this.blifFile.getName().replaceFirst("(.+)\\.blif", "$1");
		this.logger.println("Circuit : " + this.circuitName);
		
		this.checkFileExistence("Architecture file", this.architectureFile);
		this.checkFileExistence("Blif file", this.blifFile);
		this.checkFileExistence("Net file", this.netFile);
		this.checkFileExistence("Place file", this.placeFile);
		this.checkFileExistence("Lookup dump file", this.lookupDumpFile);
		this.checkFileExistence("SDC file", this.sdcFile);
		
		this.loadCircuit();
		
		this.readPlaceFile();
		
		this.circuit.getTimingGraph().initializeTiming();
		
		this.sanityCheck();
		
		System.gc();
		
		ConnectionRouter route = new ConnectionRouter(this.circuit.getResourceGraph(), this.circuit, this.td);
		int timeMilliseconds = route.route(this.alphaWLD, this.alphaTD, this.pres_fac_mult, this.share_multiplier);
		
		System.out.printf("Routing took %.2fs\n\n", (timeMilliseconds * Math.pow(10, -3)));
		System.out.println();
		
		this.circuit.getTimingGraph().calculateActualWireDelay();
		this.circuit.getTimingGraph().calculateArrivalRequiredAndCriticality(1, 1);
		
		System.out.println("-------------------------------------------------------------------------------");
		System.out.println("|               Timing information (based on actual wire delay)               |");
		System.out.println("-------------------------------------------------------------------------------");
		this.circuit.getTimingGraph().printDelays();
		System.out.println();
		System.out.println();
		
		this.circuit.getResourceGraph().printWireUsage();
		System.out.println();
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
    		architecture.getVprTiming(this.lookupDumpFile);
    	} catch(IOException | InvalidFileFormatException | InterruptedException | ParserConfigurationException | SAXException error) {
    		this.logger.raise("Failed to parse architecture file or delay tables", error);
    	}

    	// Parse net file
    	try {
    		NetParser netParser = new NetParser(architecture, this.circuitName, this.netFile);
    		this.circuit = netParser.parse();
    		this.logger.println(this.circuit.stats());
    	} catch(IOException error) {
    		this.logger.raise("Failed to read net file", error);
    	}
    	
    	this.detaildGlobalBlockInformation();
    	this.printNumBlocks();
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
        this.logger.print("   Num pins: " + numPins + "\n\n");
    }
    
    private void sanityCheck() {
    	this.circuit.getResourceGraph().sanityCheck();
    }
    private void detaildGlobalBlockInformation() {
		for(GlobalBlock block : this.circuit.getGlobalBlocks()) {
			for(AbstractPin abstractPin : block.getOutputPins()) {
				GlobalPin pin = (GlobalPin) abstractPin;
				
				if(pin.getNetName() == null && pin.getNumSinks() > 0) {
					System.err.println("\t" + "Block: " + block.getName() + " " + "Net: " + pin.getNetName() + " " + pin.getNumSinks() + " " + pin.getPortName() + "[" + pin.getIndex() + "] " + pin.getPortType().isEquivalent());
				}
			}
		}
    }
}
