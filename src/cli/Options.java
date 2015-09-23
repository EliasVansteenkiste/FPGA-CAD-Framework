package cli;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Options {
	
	@Option(name="-c", aliases="--circuit", metaVar="NAME", required=true, usage="input circuit, can be the circuit name or the location of a net file")
	public String circuit;
	
	@Option(name="-s", aliases="--start", metaVar="NAME", usage="starting stage, either blif, net or place, default is net")
	public String startingStage = "net";
	
	
	
	@Option(name="-p", aliases="--placer", metaVar="NAME", required=true, usage="the placer that should be used; can be multi-valued using a comma separated list")
	private String placersString;
	
	@Option(name="-a", aliases="--architecture", metaVar="NAME", usage="the architecture on which the circuit is placed; supported values: heterogeneous, 4LUT")
	public String architecture = "heterogeneous";
	
	
	
	@Option(name="-i", aliases="--input", metaVar="FOLDER", usage="input folder")
	public String inputPath;
	
	@Option(name="-o", aliases="--output", metaVar="FOLDER", usage="output folder, defaults to the folder of the input file")
	public String outputPath;
	
	
	@Argument(multiValued=true)
	private List<String> arguments = new ArrayList<String>();
	public List<String> placers;
	public List<HashMap<String, String>> placerOptions;
	
	
	
	@Option(name="-h", aliases="--help", usage="show this help list", help=true)
	public boolean showHelp = false;
	
	
	public File inputFolder, outputFolder, blifFile, netFile, placeFile, outputFile;
	
	
	private CmdLineParser parser;
	
	
	public Options() {
		this.parser = new CmdLineParser(this);
	}
	
	public void parseArguments(String[] args) {
		try {
			this.parser.parseArgument(args);
			
		} catch(CmdLineException e) {
			this.printUsage(System.err);
			System.exit(1);
		}
		
		
		// Show the help and exit
		if(this.showHelp) {
			this.printUsage();
			System.exit(0);
		}
		
		// Set the input folder
		File workingFolder = new File(System.getProperty("user.dir"));
		if(this.inputPath != null) {
			this.inputFolder = new File(workingFolder, this.inputPath);
			this.testFolder("Input folder", this.inputFolder);
		
		} else {
			this.inputFolder = workingFolder;
		}
		
		
		
		// Check the starting stage
		String[] possibleStages = {"blif", "net", "place"};
		if(!Arrays.asList(possibleStages).contains(this.startingStage)) {
			this.error("Invalid starting stage: " + this.startingStage + ". Choose either blif, net or place");
		}
		
		
		// Get the input extension
		if(this.startingStage.equals("blif")) {
			this.blifFile = new File(this.inputFolder, this.circuit + ".blif");
			this.testFile("Input file", this.blifFile);
		} else {
			this.netFile = new File(this.inputFolder, this.circuit + ".net");
			this.testFile("Input file", this.netFile);
			
			if(this.startingStage.equals("place")) {
				this.placeFile = new File(this.inputFolder, this.circuit + ".place");
				this.testFile("Input file", this.placeFile);
			}
		}
		
		
		
		// Set the output folder
		if(this.outputPath != null) {
			this.outputFolder = new File(workingFolder, this.outputPath);
			this.testFolder("Output folder", this.outputFolder);
		
		} else {
			this.outputFolder = this.inputFolder;
		}
		
		
		// Set the output file
		this.outputFile = new File(this.outputFolder, this.circuit + ".place");
		
		
		
		// Parse the extra placer options
		this.placers = Arrays.asList(this.placersString.split(";"));
		int numPlacers = this.placers.size();
		for(int i = 0; i < numPlacers; i++) {
			this.placers.set(i, this.placers.get(i).toLowerCase());
		}
		
		
		// For each placer: create an options HashMap
		this.placerOptions = new ArrayList<HashMap<String, String>>(numPlacers);
		for(int i = 0; i < numPlacers; i++) {
			this.placerOptions.add(new HashMap<String, String>());
		}
		
		
		// Loop through all the extra placer options
		for(String option : this.arguments) {
			
			int splitPos = option.indexOf('=');
			String optionKey;
			String[] optionValues;
			if(splitPos == -1) {
				optionKey = option;
				optionValues = new String[1];
				optionValues[0] = "";

			} else {
				optionKey = option.substring(0, splitPos);
				optionValues = option.substring(splitPos + 1).split(";"); 
			}
			
			
			// If there is no separate value for each placer
			if(optionValues.length == 1) {
				for(int i = 0; i < numPlacers; i++) {
					this.placerOptions.get(i).put(optionKey, optionValues[0]);
				}
			
			// If there is a separate value for each placer
			} else {
				for(int i = 0; i < numPlacers; i++) {
					String optionValue = null;
					
					try {
						optionValue = optionValues[i];
					} catch(IndexOutOfBoundsException e) {
						System.err.println("Option " + optionKey + " doesn't have a value for each placer: " + option);
						e.printStackTrace();
						System.exit(1);
					}
					
					this.placerOptions.get(i).put(optionKey, optionValue);
				}
			}
		}
	}
	
	
	private void testFolder(String name, File file) {
		// Test if a given folder exists
		if(!file.exists()) {
			this.error(name + " not found: " + file);
		
		} else if(!file.isDirectory()) {
			this.error(name + " is not a directory: " + file);
		}
	}
	
	private void testFile(String name, File file) {
		// Test if a given file exists
		if(!file.exists()) {
			this.error(name + " not found: " + file);
			
		} else if(file.isDirectory()) {
			this.error(name + " is a directory:" + file);
		}
	}
	
	
	
	private void printUsage() {
		this.printUsage(System.out);
	}
	
	private void printUsage(PrintStream stream) {
		
		stream.println("Usage: java cli --placer NAME --circuit NAME [options] [placer-options]");
		
		CmdLineParser parser = new CmdLineParser(this);		
		parser.printUsage(stream);
		
		stream.println();
	}
	
	
	private void error(String message) {
		System.err.println(message);
		System.exit(1);
	}
}