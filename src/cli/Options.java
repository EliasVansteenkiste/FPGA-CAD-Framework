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
	
	public enum StartingStage {NET, PLACE};
	
	@Option(name="-a", aliases="--architecture", metaVar="NAME", required=true, usage="the architecture on which the circuit is placed; supported values: heterogeneous, 4LUT")
	public String architecture = "heterogeneous";
	
    
    @Option(name="-p", aliases="--placer", metaVar="NAME", usage="the placer that should be used; can be multi-valued using a comma separated list")
	private String placersString;
	
	@Option(name="-n", aliases="--net_file", required=true, metaVar="PATH", usage="path to net file")
	private String netPath;
	
	@Option(name="-i", aliases="--input", metaVar="PATH", usage="folder that contains the input place file")
	private String inputPlacePath;
	
	@Option(name="-o", aliases="--output", metaVar="PATH", usage="folder that will contain the output place file")
	private String outputPlacePath;
	
	
	public String circuitName;
	public File netFile, inputPlaceFile, outputPlaceFile;
	public StartingStage startingStage;
	
	
	@Argument(multiValued=true, metaVar="placer-options", usage="a whitespace-separated list of key=value pairs")
	private List<String> arguments = new ArrayList<String>();
	public List<String> placers;
	public List<HashMap<String, String>> placerOptions;
	
	
	
	@Option(name="-h", aliases="--help", usage="show this help list", help=true)
	public boolean showHelp = false;
	
	
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
		
		// Set the net file
		File workingDir = new File(System.getProperty("user.dir"));
		this.netFile = new File(workingDir, this.netPath);
		this.testFile("Net file", this.netFile);
		
		// Set the circuit name
		this.circuitName = this.netFile.getAbsolutePath().replaceFirst(".*/(.*)\\..+", "$1");
		
		// Set the input place file and starting stage
		if(this.inputPlacePath == null) {
			this.startingStage = StartingStage.NET;
		
		} else {
			this.startingStage = StartingStage.PLACE;
			
			File inputPlaceFolder = new File(workingDir, this.inputPlacePath);
			this.inputPlaceFile = new File(inputPlaceFolder, this.circuitName + ".place");
			
			this.testFile("Input place file", this.inputPlaceFile);
		}
		
		// Set the output place file
		if(this.outputPlacePath == null) {
			this.outputPlaceFile = new File(this.netFile.getParent(), this.circuitName + ".place");
		
		} else {
			File outputPlaceFolder = new File(workingDir, this.outputPlacePath);
			outputPlaceFolder.mkdir();
			this.outputPlaceFile = new File(outputPlaceFolder, this.circuitName + ".place");
		}
		
		
		
		// Parse the extra placer options
        if(this.placersString == null) {
            this.placers = new ArrayList<>();
        
        } else {
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
		
		stream.println("Usage: java cli --placer NAME --net_file PATH [options] [placer-options]");
		
		CmdLineParser parser = new CmdLineParser(this);		
		parser.printUsage(stream);
		
		stream.println();
	}
	
	
	private void error(String message) {
		System.err.println(message);
		System.exit(1);
	}
}