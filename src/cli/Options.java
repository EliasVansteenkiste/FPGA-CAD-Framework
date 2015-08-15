package cli;

import java.io.File;
import java.io.PrintStream;

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
	
	@Option(name="--placer", metaVar="NAME", usage="the placer that should be used; supported values: random, SA, TDSA, AP, MDP")
	public String placer = "SA";
	
	
	@Option(name="-h", aliases="--help", usage="show this help list", help=true)
	public boolean showHelp = false;
	
	
	public String circuitName;
	public File inputFolder, outputFolder, blifFile, placeFile;
	
	
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
			System.err.println("Input folder not found: " + this.inputFolder);
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
		
		
		
		
		// Set the blif file
		
		// If the given circuit is a blif file
		if(this.circuit.length() > 5 && this.circuit.substring(this.circuit.length() - 5).equals(".blif")) {
			if(this.inputPath != null) {
				this.blifFile = new File(this.inputFolder, this.circuit);
			} else {
				this.blifFile = new File(this.circuit);
			}
			
		// Else: the given circuit is just the circuit name
		} else {
			this.blifFile = new File(this.inputFolder, this.circuit + ".blif");
		}
		
		
		// Test if the input blif file exists and is not a directory
		if(!this.blifFile.exists()) {
			System.err.println("Input blif file not found: " + this.blifFile);
			System.exit(1);
		} else if(this.blifFile.isDirectory()) {
			System.err.println("Input blif file is a directory:" + this.blifFile);
			System.exit(1);
		}
		
		
		
		// Set the circuit name
		String fileName = this.blifFile.getName();
		this.circuitName = fileName.substring(0, fileName.length() - 5);
		
		
		// Set the placement file location
		this.placeFile = new File(outputFolder, circuitName + ".place");
		
	}
	
	
	
	public void printUsage() {
		this.printUsage(System.out);
	}
	
	public void printUsage(PrintStream stream) {
		
		stream.println("Usage: java cli [options] circuit");
		
		CmdLineParser parser = new CmdLineParser(this);		
		parser.printUsage(stream);
		
		stream.println();
	}
}