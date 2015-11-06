package interfaces;


import java.util.HashMap;
import java.util.Map;

import options.CLIOptions;
import options.Options;
import options.Options.StartingStage;


import placers.Placer;
import placers.PlacerFactory;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import util.Logger;

import circuit.Circuit;
import circuit.architecture.FlexibleArchitecture;
import circuit.parser.NetParser;
import circuit.parser.PlaceDumper;
import circuit.parser.PlaceParser;


public class CLI {

    private static long timerBegin, timerEnd;

    public static void main(String[] args) {

        // The Options class can parse the command line options
        CLIOptions cliOptions = new CLIOptions();
        cliOptions.parseArguments(args);

        Options options = Options.getInstance();

        // Get architecture
        FlexibleArchitecture architecture = new FlexibleArchitecture(options.getArchitectureFile());
        architecture.parse();

        // Create a circuit
        Circuit circuit = new Circuit(options.getCircuitName(), architecture);

        // Parse net file
        NetParser netParser = new NetParser(circuit, options.getNetFile());
        CLI.startTimer();
        netParser.parse();
        CLI.stopTimer();
        CLI.printTimer("Circuit parsing: ");


        // Read the place file
        if(options.getStartingStage() == StartingStage.PLACE) {
            CLI.startTimer();
            PlaceParser placeParser = new PlaceParser(circuit, options.getInputPlaceFile());
            placeParser.parse();
            CLI.stopTimer();

            CLI.printStatistics("parser", circuit);
        }


        // Loop through the placers
        for(int placerIndex = 0; placerIndex < options.getNumPlacers(); placerIndex++) {
            String placerName = options.getPlacer(placerIndex);
            Map<String, String> placerOptions = options.getPlacerOptions(placerIndex);

            // Do a random placement if an initial placement is required
            if(options.getStartingStage() == StartingStage.NET && placerIndex == 0) {
                CLI.timePlacement("random", circuit);
            }

            // Create the placer and place the circuit
            System.out.println("Placing with " + placerName + "...");
            CLI.timePlacement(placerName, circuit, placerOptions);
        }


        if(options.getNumPlacers() > 0) {
            PlaceDumper placeDumper = new PlaceDumper(
                    circuit,
                    options.getNetFile(),
                    options.getOutputPlaceFile(),
                    options.getArchitectureFileVPR());

            placeDumper.dump();
        }
    }


    private static void timePlacement(String placerName, Circuit circuit) {
        CLI.timePlacement(placerName, circuit, new HashMap<String, String>());
    }

    private static void timePlacement(String placerName, Circuit circuit, Map<String, String> options) {
        CLI.startTimer();
        Placer placer = PlacerFactory.newPlacer(placerName, circuit, options);
        placer.initializeData();
        placer.place();
        CLI.stopTimer();

        CLI.printStatistics(placerName, circuit);
    }


    private static void startTimer() {
        CLI.timerBegin = System.nanoTime();
    }
    private static void stopTimer() {
        CLI.timerEnd = System.nanoTime();
    }
    private static double getTimer() {
        return (CLI.timerEnd - CLI.timerBegin) * 1e-9;
    }

    private static void printStatistics(String prefix, Circuit circuit) {
        CLI.printStatistics(prefix, circuit, true);
    }

    private static void printStatistics(String prefix, Circuit circuit, boolean printTime) {

        System.out.println();

        if(printTime) {
            double placeTime = CLI.getTimer();
            System.out.format("%s %15s: %f s\n", prefix, "time", placeTime);
        }

        // Calculate BB cost
        EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(circuit);
        double totalWLCost = effcc.calculateTotalCost();
        System.out.format("%s %15s: %f\n", prefix, "BB cost", totalWLCost);

        // Calculate timing cost
        circuit.recalculateTimingGraph();
        double totalTimingCost = circuit.calculateTimingCost();
        double maxDelay = circuit.getMaxDelay();

        System.out.format("%s %15s: %e\n", prefix, "timing cost", totalTimingCost);
        System.out.format("%s %15s: %f ns\n", prefix, "max delay", maxDelay);

        System.out.println();
    }

    private static void printTimer(String prefix) {
        System.out.println();
        double placeTime = CLI.getTimer();
        Logger.log(prefix + ": " + placeTime + "s");
    }
}
