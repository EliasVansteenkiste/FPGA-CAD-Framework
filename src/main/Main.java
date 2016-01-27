package main;

import interfaces.Logger;
import interfaces.Logger.Stream;
import interfaces.Options;
import interfaces.Options.Required;
import interfaces.OptionsManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import placers.Placer;
import placers.simulatedannealing.EfficientBoundingBoxNetCC;
import util.Timer;
import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.Architecture;
import circuit.architecture.ArchitectureCacher;
import circuit.architecture.ParseException;
import circuit.exceptions.InvalidFileFormatException;
import circuit.exceptions.PlacementException;
import circuit.io.BlockNotFoundException;
import circuit.io.IllegalSizeException;
import circuit.io.NetParser;
import circuit.io.PlaceDumper;
import circuit.io.PlaceParser;

public class Main {

    private long randomSeed;

    private String circuitName;
    private File blifFile, netFile, inputPlaceFile, outputPlaceFile;
    private File architectureFile;

    private boolean useVprTiming;
    private String vprCommand;

    private boolean visual;

    private Logger logger;
    private OptionsManager options;
    private PlacementVisualizer visualizer;


    private Map<String, Timer> timers = new HashMap<String, Timer>();
    private String mostRecentTimerName;
    private Circuit circuit;


    private static final String
        O_ARCHITECTURE = "architecture.xml",
        O_BLIF_FILE = "blif file",
        O_NET_FILE = "net file",
        O_INPUT_PLACE_FILE = "input place file",
        O_OUTPUT_PLACE_FILE = "output place file",
        O_VPR_TIMING = "vpr timing",
        O_VPR_COMMAND = "vpr command",
        O_VISUAL = "visual",
        O_RANDOM_SEED = "random seed";


    public static void initOptionList(Options options) {
        options.add(O_ARCHITECTURE, "", File.class);
        options.add(O_BLIF_FILE, "", File.class);

        options.add(O_NET_FILE, "(default: based on the blif file)", File.class, Required.FALSE);
        options.add(O_INPUT_PLACE_FILE, "if omitted the initial placement is random", File.class, Required.FALSE);
        options.add(O_OUTPUT_PLACE_FILE, "(default: based on the blif file)", File.class, Required.FALSE);

        options.add(O_VPR_TIMING, "Use vpr timing information", Boolean.TRUE);
        options.add(O_VPR_COMMAND, "Path to vpr executable", "./vpr");

        options.add(O_VISUAL, "show the placed circuit in a GUI", Boolean.FALSE);
        options.add(O_RANDOM_SEED, "seed for randomization", new Long(1));
    }


    public Main(OptionsManager options) {
        this.options = options;
        this.logger = options.getLogger();

        this.parseOptions(options.getMainOptions());
    }

    private void parseOptions(Options options) {

        this.randomSeed = options.getLong(O_RANDOM_SEED);

        this.inputPlaceFile = options.getFile(O_INPUT_PLACE_FILE);

        this.blifFile = options.getFile(O_BLIF_FILE);
        this.netFile = options.getFile(O_NET_FILE);
        this.outputPlaceFile = options.getFile(O_OUTPUT_PLACE_FILE);

        File inputFolder = this.blifFile.getParentFile();
        this.circuitName = this.blifFile.getName().replaceFirst("(.+)\\.blif", "$1");

        if(this.netFile == null) {
            this.netFile = new File(inputFolder, this.circuitName + ".net");
        }
        if(this.outputPlaceFile == null) {
            this.outputPlaceFile = new File(inputFolder, this.circuitName + ".place");
        }

        this.architectureFile = options.getFile(O_ARCHITECTURE);

        this.useVprTiming = options.getBoolean(O_VPR_TIMING);
        this.vprCommand = options.getString(O_VPR_COMMAND);
        this.visual = options.getBoolean(O_VISUAL);


        // Check if all input files exist
        this.checkFileExistence("Blif file", this.blifFile);
        this.checkFileExistence("Net file", this.netFile);
        this.checkFileExistence("Input place file", this.inputPlaceFile);

        this.checkFileExistence("Architecture file", this.architectureFile);
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
            this.startTimer("Placement parser");
            PlaceParser placeParser = new PlaceParser(this.circuit, this.inputPlaceFile);

            try {
                placeParser.parse();

            } catch(IOException | BlockNotFoundException | PlacementException | IllegalSizeException error) {
                this.logger.raise("Something went wrong while parsing the place file", error);
            }

            this.stopTimer();
            this.printStatistics("Placement parser", false);

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
                    this.architectureFile);

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
        ArchitectureCacher architectureCacher = new ArchitectureCacher(
                this.circuitName,
                this.netFile,
                this.architectureFile,
                this.useVprTiming);
        Architecture architecture = architectureCacher.loadIfCached();
        boolean isCached = (architecture != null);

        // Parse the architecture file if necessary
        if(!isCached) {
            this.startTimer("Architecture parsing");
            architecture = new Architecture(
                    this.circuitName,
                    this.architectureFile,
                    this.vprCommand,
                    this.blifFile,
                    this.netFile,
                    this.useVprTiming);

            try {
                architecture.parse();
            } catch(IOException | InvalidFileFormatException | InterruptedException | ParseException | ParserConfigurationException | SAXException error) {
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
            boolean success = architectureCacher.store(architecture);
            this.stopAndPrintTimer();

            if(!success) {
                this.logger.print(Stream.ERR, "Something went wrong while caching the architecture");
            }
        }
    }


    private void timePlacement(int placerIndex) {
        long seed = this.randomSeed;
        Random random = new Random(seed);

        Placer placer = this.options.getPlacer(placerIndex, this.circuit, random, this.visualizer);
        String placerName = placer.getName();

        this.startTimer(placerName);
        placer.initializeData();
        try {
            placer.place();
        } catch(PlacementException error) {
            this.logger.raise(error);
        }
        this.stopTimer();

        placer.printRuntimeBreakdown();

        this.printStatistics(placerName, true);
    }


    private void startTimer(String name) {
        this.mostRecentTimerName = name;

        Timer timer = new Timer();
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
            try {
                timer.stop();
            } catch(IllegalStateException error) {
                this.logger.raise("There was a problem with timer \"" + name + "\":", error);
            }
        }
    }

    private double getTime(String name) {
        Timer timer = this.timers.get(name);

        if(timer == null) {
            this.logger.raise("Timer hasn't been initialized: " + name);
            return -1;

        } else {
            double time = 0;
            try {
                time = timer.getTime();

            } catch(IllegalStateException error) {
                this.logger.raise("There was a problem with timer \"" + name + "\":", error);
            }

            return time;
        }
    }


    private void printStatistics(String prefix, boolean printTime) {

        this.logger.println(prefix + " results:");
        String format = "%-11s | %g%s\n";

        if(printTime) {
            double placeTime = this.getTime(prefix);
            this.logger.printf(format, "runtime", placeTime, " s");
        }

        // Calculate BB cost
        EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
        double totalWLCost = effcc.calculateTotalCost();
        this.logger.printf(format, "BB cost", totalWLCost, "");

        // Calculate timing cost
        this.circuit.recalculateTimingGraph();
        double totalTimingCost = this.circuit.calculateTimingCost();
        double maxDelay = this.circuit.getMaxDelay();

        this.logger.printf(format, "timing cost", totalTimingCost, "");
        this.logger.printf(format, "max delay", maxDelay, " ns");

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
