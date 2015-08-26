package cli;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Options {
	
	@Option(name="-c", aliases="--circuit", metaVar="NAME", required=true, usage="input circuit, can be the circuit name or the location of a net file")
	public String circuit;
	
	@Option(name="-p", aliases="--placer", metaVar="NAME", required=true, usage="the placer that should be used; can be multi-valued using a comma separated list")
	private String placersString;
	
	@Option(name="-r", aliases="--random", usage="perform a random placement as initialization")
	public boolean random;
	
	@Option(name="-a", aliases="--architecture", metaVar="NAME", usage="the architecture on which the circuit is placed; supported values: heterogeneous, 4LUT")
	public String architecture = "heterogeneous";
	
	
	
	@Option(name="-i", aliases="--input", metaVar="FOLDER", usage="input folder")
	public String inputPath;
	
	@Option(name="-o", aliases="--output", metaVar="FOLDER", usage="output folder, defaults to the folder of the input file")
	public String outputPath;
	
	
	
	@Argument(multiValued=true)
	private List<String> arguments = new ArrayList<String>();
	public LinkedHashMap<String, HashMap<String, String>> placers;
	
	
	
	@Option(name="-h", aliases="--help", usage="show this help list", help=true)
	public boolean showHelp = false;
	
	
	
	public String circuitName;
	public File inputFolder, outputFolder, netFile, placeFile;
	
	
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
		} else {
			this.inputFolder = workingFolder;
		}
		
		// Test if the input folder exists
		if(!this.inputFolder.exists()) {
			this.error("Input folder not found: " + this.inputFolder);
		}
		
		
		
		// Set the output folder
		if(this.outputPath != null) {
			this.outputFolder = new File(workingFolder, this.outputPath);
		} else {
			this.outputFolder = this.inputFolder;
		}
		
		// Test if the output folder exists
		if(!this.outputFolder.exists()) {
			this.error("Output folder not found: " + this.outputFolder);
		}
		
		
		
		
		// Set the net file
		
		// If the given circuit is a net file
		if(this.circuit.length() > 4 && this.circuit.substring(this.circuit.length() - 4).equals(".net")) {
			if(this.inputPath != null) {
				this.netFile = new File(this.inputFolder, this.circuit);
			} else {
				this.netFile = new File(this.circuit);
			}
			
		// Else: the given circuit is just the circuit name
		} else {
			this.netFile = new File(this.inputFolder, this.circuit + ".net");
		}
		
		
		// Test if the input net file exists and is not a directory
		if(!this.netFile.exists()) {
			this.error("Input net file not found: " + this.netFile);
			
		} else if(this.netFile.isDirectory()) {
			this.error("Input net file is a directory:" + this.netFile);
		}
		
		
		
		// Set the circuit name
		String fileName = this.netFile.getName();
		this.circuitName = fileName.substring(0, fileName.length() - 4);
		
		
		// Set the placement file location
		this.placeFile = new File(outputFolder, circuitName + ".place");
		
		
		
		// Parse the extra placer options
		String[] placerNames = placersString.split(",");
		int numPlacers = placerNames.length;
		ArrayList<HashMap<String, String>> placerOptions = new ArrayList<HashMap<String, String>>();
		
		
		
		// For each placer: create an options HashMap
		for(int i = 0; i < numPlacers; i++) {
			placerOptions.add(new HashMap<String, String>());
		}
		
		// Loop through all the given options except "placers"
		for(String option : arguments) {
			
			int splitPos = option.indexOf('=');
			String optionKey;
			String[] optionValues;
			if(splitPos == -1) {
				optionKey = option;
				optionValues = new String[1];
				optionValues[0] = "";

			} else {
				optionKey = option.substring(0, splitPos);
				optionValues = option.substring(splitPos + 1).split(","); 
			}
			
			
			// If there is no separate value for each placer
			if(optionValues.length == 1) {
				for(int i = 0; i < numPlacers; i++) {
					placerOptions.get(i).put(optionKey, optionValues[0]);
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
					
					placerOptions.get(i).put(optionKey, optionValue);
				}
			}
		}
		
		this.placers = new LinkedHashMap<String, HashMap<String, String>>();
		for(int i = 0; i < numPlacers; i++) {
			placers.put(placerNames[i], placerOptions.get(i));
		}
	}
	
	
	
	private void printUsage() {
		this.printUsage(System.out);
	}
	
	private void printUsage(PrintStream stream) {
		
		stream.println("Usage: java cli [options] circuit");
		
		CmdLineParser parser = new CmdLineParser(this);		
		parser.printUsage(stream);
		
		stream.println();
	}
	
	
	private void error(String message) {
		System.err.println(message);
		System.exit(1);
	}
}