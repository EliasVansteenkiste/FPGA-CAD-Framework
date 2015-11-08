package main;

import java.util.HashMap;
import java.util.Map;

import options.Options;
import options.Options.StartingStage;
import placers.Placer;
import placers.PlacerFactory;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import util.Logger;
import circuit.Circuit;
import circuit.architecture.Architecture;
import circuit.parser.NetParser;
import circuit.parser.PlaceDumper;
import circuit.parser.PlaceParser;

public class Main {

    private static Main instance = new Main();
    public static Main getInstance() {
        return Main.instance;
    }


    private Options options;
    private Map<String, Timer> timers = new HashMap<String, Timer>();
    private String mostRecentTimerName;
    private Circuit circuit;



    public void runPlacement() {
        String totalString = "Total flow took";
        this.startTimer(totalString);

        this.options = Options.getInstance();

        this.loadCircuit();


        // Read the place file
        if(this.options.getStartingStage() == StartingStage.PLACE) {
            this.startTimer("parser");
            PlaceParser placeParser = new PlaceParser(this.circuit, this.options.getInputPlaceFile());
            placeParser.parse();
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
            System.out.println("Placing with " + placerName + "...");
            this.timePlacement(placerName, placerOptions);
        }


        if(this.options.getNumPlacers() > 0) {
            PlaceDumper placeDumper = new PlaceDumper(
                    this.circuit,
                    this.options.getNetFile(),
                    this.options.getOutputPlaceFile(),
                    this.options.getArchitectureFileVPR());

            placeDumper.dump();
        }

        this.stopAndPrintTimer(totalString);
    }




    private void loadCircuit() {
        CircuitCacher circuitCacher = new CircuitCacher(this.options.getCircuitName(), this.options.getNetFile());
        boolean isCached = circuitCacher.isCached();

        if(isCached) {
            this.startTimer("Loading cached circuit");
            this.circuit = circuitCacher.load();
            this.stopAndPrintTimer();

        } else {
            this.initializeCircuit();
        }

        // Parse net file
        this.startTimer("Net file parsing");

        NetParser netParser = new NetParser(this.circuit, this.options.getNetFile());
        netParser.parse();

        this.stopAndPrintTimer("Net file parsing");


        // Build timing graphs
        this.startTimer("Timing graph building");
        this.circuit.buildTimingGraph();
        this.stopAndPrintTimer();


        // Cache the circuit for future use
        if(!isCached) {
            this.startTimer("Circuit caching");
            circuitCacher.store(this.circuit);
            this.stopAndPrintTimer();
        }
    }


    private void initializeCircuit() {
        // Get architecture
        this.startTimer("Architecture parsing");

        Architecture architecture = new Architecture();
        architecture.parse();
        this.stopAndPrintTimer();

        this.startTimer("Delay matrix building");
        architecture.buildDelayMatrixes();
        this.stopAndPrintTimer();

        this.circuit = new Circuit(this.options.getCircuitName(), architecture);
    }




    private void timePlacement(String placerName) {
        this.timePlacement(placerName, new HashMap<String, String>());
    }

    private void timePlacement(String placerName, Map<String, String> options) {
        this.startTimer(placerName);

        Placer placer = PlacerFactory.newPlacer(placerName, this.circuit, options);
        placer.initializeData();
        placer.place();

        this.stopTimer();
        this.printStatistics();
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
            Logger.raise("Timer hasn't been initialized: " + name);
        } else {
            timer.stop();
        }
    }

    private double getTime(String name) {
        Timer timer = this.timers.get(name);

        if(timer == null) {
            Logger.raise("Timer hasn't been initialized: " + name);
            return -1;
        } else {
            return timer.getTime();
        }
    }

    private void printStatistics() {
        this.printStatistics(this.mostRecentTimerName, true);
    }
    private void printStatistics(String prefix, boolean printTime) {

        System.out.println();

        if(printTime) {
            double placeTime = this.getTime(prefix);
            System.out.format("%s %15s: %f s\n", prefix, "time", placeTime);
        }

        // Calculate BB cost
        EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
        double totalWLCost = effcc.calculateTotalCost();
        System.out.format("%s %15s: %f\n", prefix, "BB cost", totalWLCost);

        // Calculate timing cost
        this.circuit.recalculateTimingGraph();
        double totalTimingCost = this.circuit.calculateTimingCost();
        double maxDelay = this.circuit.getMaxDelay();

        System.out.format("%s %15s: %e\n", prefix, "timing cost", totalTimingCost);
        System.out.format("%s %15s: %f ns\n", prefix, "max delay", maxDelay);

        System.out.println();
    }


    private void stopAndPrintTimer() {
        this.stopAndPrintTimer(this.mostRecentTimerName);
    }
    private void stopAndPrintTimer(String timerName) {
        this.stopTimer(timerName);
        this.printTimer(timerName);
    }

    private void printTimer(String timerName) {
        System.out.println();
        double placeTime = this.getTime(timerName);
        Logger.log(timerName + ": " + placeTime + " s");
    }
}
