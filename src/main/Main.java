package main;

import interfaces.Logger;
import interfaces.Option.Required;
import interfaces.OptionList;
import interfaces.Options;
import interfaces.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import placers.Placer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.Architecture;
import circuit.architecture.ArchitectureCacher;
import circuit.exceptions.InvalidFileFormatException;
import circuit.exceptions.PlacementException;
import circuit.parser.BlockNotFoundException;
import circuit.parser.NetParser;
import circuit.parser.PlaceDumper;
import circuit.parser.PlaceParser;

public class Main {

    private long randomSeed;

    private String circuitName;
    private File blifFile, netFile, inputPlaceFile, outputPlaceFile;
    private File architectureFile, architectureFileVPR;
    private boolean visual;

    private Logger logger;
    private Options options;
    private PlacementVisualizer visualizer;


    private Map<String, Timer> timers = new HashMap<String, Timer>();
    private String mostRecentTimerName;
    private Circuit circuit;


    public static void initOptionList(OptionList options) {
        options.add(new Option("architecture", "The JSON architecture file", File.class));
        options.add(new Option("blif file", "The blif file", File.class));

        options.add(new Option("net file", "The net file. Default: based on the blif file.", File.class, Required.FALSE));
        options.add(new Option("input place file", "The input place file. If omitted the initial placement is random.", File.class, Required.FALSE));
        options.add(new Option("output place file", "The output place file", File.class, Required.FALSE));

        options.add(new Option("vpr architecture", "The XML architecture file", File.class, Required.FALSE));

        options.add(new Option("visual", "Show the placed circuit in a GUI", Boolean.FALSE));
        options.add(new Option("random seed", "Seed for randomization", new Long(1)));
    }


    public Main(Options options) {
        this.options = options;
        this.logger = options.getLogger();

        this.parseOptions(options.getMainOptions());
    }

    private void parseOptions(OptionList options) {

        this.randomSeed = options.getLong("random seed");

        this.inputPlaceFile = options.getFile("input place file");

        this.blifFile = options.getFile("blif file");
        this.netFile = options.getFile("net file");
        this.outputPlaceFile = options.getFile("output place file");

        File inputFolder = this.blifFile.getParentFile();
        this.circuitName = this.blifFile.getName().replaceFirst("(.+)\\.blif", "$1");

        if(this.netFile == null) {
            this.netFile = new File(inputFolder, this.circuitName + ".net");
        }
        if(this.outputPlaceFile == null) {
            this.outputPlaceFile = new File(inputFolder, this.circuitName + ".place");
        }

        this.architectureFile = options.getFile("architecture");
        this.architectureFileVPR = options.getFile("vpr architecture");
        if(this.architectureFileVPR == null) {
            this.architectureFileVPR = this.guessArchitectureFileVPR();
        }

        this.visual = (Boolean) options.get("visual");


        // Check if all input files exist
        this.checkFileExistence("Blif file", this.blifFile);
        this.checkFileExistence("Net file", this.netFile);
        this.checkFileExistence("Input place file", this.inputPlaceFile);

        this.checkFileExistence("Architecture file", this.architectureFile);
        this.checkFileExistence("VPR architecture file", this.architectureFileVPR);
    }

    private File guessArchitectureFileVPR() {
        File[] files = this.netFile.getParentFile().listFiles();
        File architectureFile = null;

        for(File file : files) {
            String path = file.getAbsolutePath();
            if(path.substring(path.length() - 4).equals(".xml")) {
                if(architectureFile != null) {
                    this.logger.raise("Multiple architecture files found in the input folder");
                }
                architectureFile = file;
            }
        }

        if(architectureFile == null) {
            this.logger.raise("No architecture file found in the inputfolder");
        }

        return architectureFile;
    }

    protected void checkFileExistence(String prefix, File file) {
        if(file == null) {
            return;
        }

        if(!file.exists()) {
            this.logger.raise(new FileNotFoundException(prefix + " " + file));

        } else if(file.isDirectory()) {
            this.logger.raise(new FileNotFoundException(prefix + " " + file + " is a director"));
        }
    }



    public void runPlacement() {
        String totalString = "Total flow took";
        this.startTimer(totalString);

        this.loadCircuit();
        this.logger.println();


        // Enable the visualizer
        this.visualizer = new PlacementVisualizer(this.logger);
        if(this.visual) {
            this.visualizer.setCircuit(this.circuit);
        }


        // Read the place file
        boolean startFromPlaceFile = (this.inputPlaceFile != null);
        if(startFromPlaceFile) {
            this.startTimer("parser");
            PlaceParser placeParser = new PlaceParser(this.circuit, this.inputPlaceFile);

            try {
                placeParser.parse();
            } catch(IOException | BlockNotFoundException | PlacementException error) {
                this.logger.raise("Something went wrong while parsing the place file", error);
            }

            this.stopTimer();
            this.printStatistics();

        // Add a random placer with default options at the beginning
        } else {
            this.options.insertRandomPlacer();
        }


        // Loop through the placers
        int numPlacers = this.options.getNumPlacers();
        for(int placerIndex = 0; placerIndex < numPlacers; placerIndex++) {
            this.timePlacement(placerIndex);
        }


        if(numPlacers > 0) {
            PlaceDumper placeDumper = new PlaceDumper(
                    this.circuit,
                    this.netFile,
                    this.outputPlaceFile,
                    this.architectureFileVPR);

            try {
                placeDumper.dump();
            } catch(IOException error) {
                this.logger.raise("Failed to write to place file: " + this.outputPlaceFile, error);
            }
        }

        this.stopAndPrintTimer(totalString);

        this.visualizer.createAndDrawGUI();
    }




    private void loadCircuit() {
        ArchitectureCacher architectureCacher = new ArchitectureCacher(this.circuitName, this.netFile);
        Architecture architecture = architectureCacher.loadIfCached();
        boolean isCached = (architecture != null);

        // Parse the architecture file if necessary
        if(!isCached) {
            this.startTimer("Architecture parsing");
            architecture = new Architecture(
                    this.circuitName,
                    this.architectureFile,
                    this.architectureFileVPR,
                    this.blifFile,
                    this.netFile);

            try {
                architecture.parse();
            } catch(IOException | InvalidFileFormatException | InterruptedException error) {
                this.logger.raise("Failed to parse architecture file or delay tables", error);
            }

            this.stopAndPrintTimer();
        }


        // Parse net file
        this.startTimer("Net file parsing");
        try {
            NetParser netParser = new NetParser(architecture, this.circuitName, this.netFile);
            this.circuit = netParser.parse();

        } catch(IOException error) {
            this.logger.raise("Failed to read net file", error);
        }
        this.stopAndPrintTimer();


        // Cache the circuit for future use
        if(!isCached) {
            this.startTimer("Circuit caching");
            architectureCacher.store(architecture);
            this.stopAndPrintTimer();
        }
    }


    private void timePlacement(int placerIndex) {
        String timerName = "placer" + placerIndex;
        this.startTimer(timerName);

        long seed = this.randomSeed;
        Random random = new Random(seed);

        Placer placer = this.options.getPlacer(placerIndex, this.circuit, random, this.visualizer);
        placer.initializeData();
        placer.place();

        this.stopTimer();
        this.printStatistics();
    }


    private void startTimer(String name) {
        this.mostRecentTimerName = name;

        Timer timer = new Timer(this.logger);
        this.timers.put(name, timer);
        timer.start();
    }

    private void stopTimer() {
        this.stopTimer(this.mostRecentTimerName);
    }
    private void stopTimer(String name) {
        Timer timer = this.timers.get(name);

        if(timer == null) {
            this.logger.raise("Timer hasn't been initialized: " + name);
        } else {
            timer.stop();
        }
    }

    private double getTime(String name) {
        Timer timer = this.timers.get(name);

        if(timer == null) {
            this.logger.raise("Timer hasn't been initialized: " + name);
            return -1;
        } else {
            return timer.getTime();
        }
    }

    private void printStatistics() {
        this.printStatistics(this.mostRecentTimerName, true);
    }
    private void printStatistics(String prefix, boolean printTime) {

        if(printTime) {
            double placeTime = this.getTime(prefix);
            this.logger.printf("%s %15s: %f s\n", prefix, "time", placeTime);
        }

        // Calculate BB cost
        EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
        double totalWLCost = effcc.calculateTotalCost();
        this.logger.printf("%s %15s: %f\n", prefix, "BB cost", totalWLCost);

        // Calculate timing cost
        this.circuit.recalculateTimingGraph();
        double totalTimingCost = this.circuit.calculateTimingCost();
        double maxDelay = this.circuit.getMaxDelay();

        this.logger.printf("%s %15s: %e\n", prefix, "timing cost", totalTimingCost);
        this.logger.printf("%s %15s: %f ns\n", prefix, "max delay", maxDelay);

        this.logger.println();
    }


    private void stopAndPrintTimer() {
        this.stopAndPrintTimer(this.mostRecentTimerName);
    }
    private void stopAndPrintTimer(String timerName) {
        this.stopTimer(timerName);
        this.printTimer(timerName);
    }

    private void printTimer(String timerName) {
        double placeTime = this.getTime(timerName);
        this.logger.printf("%s: %f s\n", timerName, placeTime);
    }
}
