package main;

import interfaces.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import options.Options;
import options.Options.StartingStage;
import placers.Placer;
import placers.PlacerFactory;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import visual.PlacementVisualizer;
import circuit.Circuit;
import circuit.architecture.Architecture;
import circuit.architecture.ArchitectureCacher;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.InvalidFileFormatException;
import circuit.exceptions.PlacedBlockException;
import circuit.parser.BlockNotFoundException;
import circuit.parser.NetParser;
import circuit.parser.PlaceDumper;
import circuit.parser.PlaceParser;

public class Main {

    private Logger logger;
    private PlacementVisualizer visualizer;
    private Options options;

    private Map<String, Timer> timers = new HashMap<String, Timer>();
    private String mostRecentTimerName;
    private Circuit circuit;


    public Main(Logger logger, Options options) {
        this.logger = logger;
        this.options = options;
    }

    public void runPlacement() {
        String totalString = "Total flow took";
        this.startTimer(totalString);

        this.loadCircuit();
        this.logger.logln();


        // Enable the visualizer
        this.visualizer = new PlacementVisualizer(this.logger);
        if(this.options.getVisual()) {
            this.visualizer.setCircuit(this.circuit);
        }


        // Read the place file
        if(this.options.getStartingStage() == StartingStage.PLACE) {
            this.startTimer("parser");
            PlaceParser placeParser = new PlaceParser(this.circuit, this.options.getInputPlaceFile());

            try {
                placeParser.parse();
            } catch(BlockNotFoundException | IOException | PlacedBlockException | FullSiteException error) {
                this.logger.raise("Something went wrong while parsing the place file", error);
            }

            this.stopTimer();
            this.printStatistics();
        }


        // Loop through the placers
        for(int placerIndex = 0; placerIndex < this.options.getNumPlacers(); placerIndex++) {
            String placerName = this.options.getPlacer(placerIndex);
            Map<String, String> placerOptions = this.options.getPlacerOptions(placerIndex);

            // Do a random placement if an initial placement is required
            if(this.options.getStartingStage() == StartingStage.NET && placerIndex == 0) {
                this.timePlacement("random");
            }

            // Create the placer and place the circuit
            this.timePlacement(placerName, placerOptions);
        }


        if(this.options.getNumPlacers() > 0) {
            PlaceDumper placeDumper = new PlaceDumper(
                    this.circuit,
                    this.options.getNetFile(),
                    this.options.getOutputPlaceFile(),
                    this.options.getArchitectureFileVPR());

            try {
                placeDumper.dump();
            } catch(IOException error) {
                this.logger.raise("Failed to write to place file: " + this.options.getOutputPlaceFile(), error);
            }
        }

        this.stopAndPrintTimer(totalString);

        this.visualizer.createAndDrawGUI();
    }




    private void loadCircuit() {
        ArchitectureCacher architectureCacher = new ArchitectureCacher(this.options.getCircuitName(), this.options.getNetFile());
        Architecture architecture = architectureCacher.loadIfCached();
        boolean isCached = (architecture != null);

        // Parse the architecture file if necessary
        if(!isCached) {
            this.startTimer("Architecture parsing");
            architecture = new Architecture(
                    this.options.getCircuitName(),
                    this.options.getArchitectureFile(),
                    this.options.getArchitectureFileVPR(),
                    this.options.getBlifFile(),
                    this.options.getNetFile());

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
            NetParser netParser = new NetParser(architecture, this.options.getNetFile(), this.options.getCircuitName());
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



    private void timePlacement(String placerName) {
        this.timePlacement(placerName, new HashMap<String, String>());
    }

    private void timePlacement(String placerName, Map<String, String> options) {
        this.startTimer(placerName);

        Placer placer = PlacerFactory.newPlacer(placerName, this.logger, this.visualizer, this.circuit, options);
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
            this.logger.logf("%s %15s: %f s\n", prefix, "time", placeTime);
        }

        // Calculate BB cost
        EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
        double totalWLCost = effcc.calculateTotalCost();
        this.logger.logf("%s %15s: %f\n", prefix, "BB cost", totalWLCost);

        // Calculate timing cost
        this.circuit.recalculateTimingGraph();
        double totalTimingCost = this.circuit.calculateTimingCost();
        double maxDelay = this.circuit.getMaxDelay();

        this.logger.logf("%s %15s: %e\n", prefix, "timing cost", totalTimingCost);
        this.logger.logf("%s %15s: %f ns\n", prefix, "max delay", maxDelay);

        this.logger.logln();
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
        this.logger.logf("%s: %f s\n", timerName, placeTime);
    }
}
