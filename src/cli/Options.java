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
	
	@Option(name="--pack", usage="start from a blif file and pack the circuit before placing")
	public boolean pack;
	
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
	public File inputFolder, outputFolder, blifFile, netFile, placeFile;
	
	
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
		
		
		
		// Get the input file
		String inputExtension;
		if(this.pack) {
			inputExtension = ".blif";
		} else {
			inputExtension = ".net";
		}
		
		
		// If the given circuit is a file matching the input extension
		File inputFile;
		if(this.circuit.length() > inputExtension.length() && this.circuit.substring(this.circuit.length() - inputExtension.length()).equals(inputExtension)) {
			if(this.inputPath != null) {
				inputFile = new File(this.inputFolder, this.circuit);
			} else {
				inputFile = new File(this.circuit);
			}
			
		// Else: the given circuit is just the circuit name
		} else {
			inputFile = new File(this.inputFolder, this.circuit + inputExtension);
		}
		
		// Test if the input file exists and is not a directory
		if(!inputFile.exists()) {
			this.error("Input file not found: " + inputFile);
			
		} else if(inputFile.isDirectory()) {
			this.error("Input file is a directory:" + inputFile);
		}
		
		
		// Set the blif or net file
		if(pack) {
			this.blifFile = inputFile;
		
		} else {
			this.netFile = inputFile;
		}
		
		
		
		
		
		
		// Set the circuit name
		String fileName = inputFile.getName();
		this.circuitName = fileName.substring(0, fileName.length() - inputExtension.length());
		
		
		// Set the placement file location
		this.placeFile = new File(outputFolder, circuitName + ".place");
		
		
		// Set the architecture
		this.architecture = this.architecture.toLowerCase();
		
		
		// Parse the extra placer options
		String[] placerNames = placersString.split(",");
		int numPlacers = placerNames.length;
		ArrayList<HashMap<String, String>> placerOptions = new ArrayList<HashMap<String, String>>();
		
		
		
		// For each placer: create an options HashMap
		for(int i = 0; i < numPlacers; i++) {
			placerOptions.add(new HashMap<String, String>());
		}
		
		
		// Loop through all the extra placer options
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