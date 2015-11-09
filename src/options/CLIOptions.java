package options;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class CLIOptions {

    @Option(name="-a", aliases="--architecture", metaVar="NAME", required=true, usage="the architecture on which the circuit is placed; supported values: heterogeneous, 4LUT")
    public String architecturePath = "heterogeneous";


    @Option(name="-p", aliases="--placer", metaVar="NAME", usage="the placer that should be used; can be multi-valued using a comma separated list")
    private String placersString;

    @Option(name="-n", aliases="--net_file", required=true, metaVar="PATH", usage="path to net file")
    private String netPath;

    @Option(name="-i", aliases="--input", metaVar="PATH", usage="folder that contains the input place file")
    private String inputPlacePath;

    @Option(name="-o", aliases="--output", metaVar="PATH", usage="folder that will contain the output place file")
    private String outputPlacePath;

    @Option(name="-v", aliases="--visual", metaVar="BOOLEAN", usage="show the placement in a visual window")
    private boolean visual = false;


    @Argument(multiValued=true, metaVar="placer-options", usage="a whitespace-separated list of key=value pairs")
    private List<String> arguments = new ArrayList<String>();


    @Option(name="-h", aliases="--help", usage="show this help list", help=true)
    public boolean showHelp = false;



    private CmdLineParser parser;


    public CLIOptions() {
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


        Options options = Options.getInstance();

        options.setVisual(this.visual);

        // Set the architecture file
        options.setArchitectureFile(this.architecturePath);

        // Set the blif and net file
        String blifPath = this.netPath.replaceFirst("(.net)$", ".blif");
        options.setBlifFile(blifPath);

        options.setNetFile(this.netPath);
        options.guessArchitectureFileVPR();


        // Set the circuit name
        String circuitName = options.getNetFile().getAbsolutePath().replaceFirst(".*/(.*)\\..+", "$1");
        options.setCircuitName(circuitName);

        // Set the input place file and starting stage
        if(this.inputPlacePath != null) {
            File inputPlaceFolderOrFile = new File(this.inputPlacePath);
            File inputPlaceFile;

            if(inputPlaceFolderOrFile.isDirectory()) {
                inputPlaceFile = new File(inputPlaceFolderOrFile, options.getCircuitName() + ".place");
            } else {
                inputPlaceFile = inputPlaceFolderOrFile;
            }

            options.setInputPlaceFile(inputPlaceFile);
        }

        // Set the output place file
        File outputPlaceFolder;
        if(this.outputPlacePath == null) {
            outputPlaceFolder = options.getNetFile().getParentFile();

        } else {
            outputPlaceFolder = new File(this.outputPlacePath);
            outputPlaceFolder.mkdir();
        }

        File outputPlaceFile = new File(outputPlaceFolder, options.getCircuitName() + ".place");
        options.setOutputPlaceFile(outputPlaceFile);



        // Parse the extra placer options
        List<String> placers = new ArrayList<String>();
        List<Map<String, String>> placerOptions = new ArrayList<Map<String, String>>();
        if(this.placersString != null) {
            placers = Arrays.asList(this.placersString.split(";"));
            int numPlacers = placers.size();

            for(int i = 0; i < numPlacers; i++) {
                placers.set(i, placers.get(i).toLowerCase());
                placerOptions.add(new HashMap<String, String>());
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
        }

        options.setPlacers(placers, placerOptions);
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
}