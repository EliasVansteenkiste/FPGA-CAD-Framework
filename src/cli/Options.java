package cli;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Options {
	@Argument(metaVar="circuit", usage="input circuit, can be the circuit name of the location of a blif file")
	public String circuit;
	
	
	@Option(name="-i", aliases="--input", metaVar="FOLDER", usage="input folder")
	public String inputPath;
	
	@Option(name="-o", aliases="--output", metaVar="FOLDER", usage="output folder, defaults to the folder of the input file")
	public String outputPath;
	
	@Option(name="--architecture", metaVar="NAME", usage="the architecture on which the circuit is placed; supported values: heterogeneous, 4LUT")
	public String architecture = "heterogeneous";
	
	@Option(name="--placer", metaVar="NAME", usage="the placer that should be used; supported values: random, SA, TDSA, AP, MDP, CA")
	public String placer = "SA";
	
	@Option(name="--options", metaVar="OPTIONS", usage="a comma separated list of placer options and optionally their values")
	private String optionsString = "";
	public HashMap<String, String> options = new HashMap<String, String>();
	
	
	
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
			System.err.println("Output folder not found: " + this.outputFolder);
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
			System.err.println("Input net file not found: " + this.netFile);
			System.exit(1);
		} else if(this.netFile.isDirectory()) {
			System.err.println("Input net file is a directory:" + this.netFile);
			System.exit(1);
		}
		
		
		
		// Set the circuit name
		String fileName = this.netFile.getName();
		this.circuitName = fileName.substring(0, fileName.length() - 4);
		
		
		// Set the placement file location
		this.placeFile = new File(outputFolder, circuitName + ".place");
		
		
		// Parse the extra options
		String[] optionsSplitted = optionsString.split(",");
		for(int i = 0; i < optionsSplitted.length; i++) {
			String option = optionsSplitted[i].trim();
			int firstSpace = option.indexOf(' ');
			
			if(firstSpace == -1) {
				options.put(option, "");
			} else {
				String optionKey = option.substring(0, firstSpace);
				String optionValue = option.substring(firstSpace + 1); 
				options.put(optionKey, optionValue);
			}
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