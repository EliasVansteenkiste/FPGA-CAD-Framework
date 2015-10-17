package cli;

import java.util.HashMap;
import java.util.Map;

import architecture.FlexibleArchitecture;
import architecture.circuit.Circuit;
import architecture.circuit.parser.NetParser;
import architecture.circuit.parser.PlaceDumper;
import architecture.circuit.parser.PlaceParser;

import placers.Placer;
import placers.PlacerFactory;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import timing_graph.TimingGraph;
import util.Logger;

import cli.Options.StartingStage;


public class CLI {
	
	private static long timerBegin, timerEnd;
	
	public static void main(String[] args) {
		
		// The Options class can parse the command line options
		Options options = new Options();
		options.parseArguments(args);
		
		// Get architecture
		FlexibleArchitecture architecture = new FlexibleArchitecture(options.architecture);
		architecture.parse();
		
		// Create a circuit
		Circuit circuit = new Circuit(options.circuitName, architecture);
		
		// Create the placement file dumper
		// It does some checks to find an architecture file, that's why we put it here already
		PlaceDumper placeDumper = new PlaceDumper(circuit, options.netFile, options.outputPlaceFile);
		
		// Parse net file
		NetParser netParser = new NetParser(circuit, options.netFile);
		CLI.startTimer();
		netParser.parse();
		CLI.stopTimer();
		CLI.printTimer("Circuit parsing: ");
		
		
		// Read the place file
		if(options.startingStage == StartingStage.PLACE) {
			PlaceParser placeParser = new PlaceParser(circuit, options.inputPlaceFile);
			placeParser.parse();
			
			CLI.printStatistics("parser", circuit);
		}
		
		
		// Loop through the placers
		for(int i = 0; i < options.placers.size(); i++) {
			String placerName = options.placers.get(i);
			HashMap<String, String> placerOptions = options.placerOptions.get(i);
			
			// Do a random placement if an initial placement is required
			if(options.startingStage == StartingStage.NET && i == 0) {
				CLI.timePlacement("random", circuit);
			}
			
			// Create the placer and place the circuit
			System.out.println("Placing with " + placerName + "...");
			CLI.timePlacement(placerName, circuit, placerOptions);
		}
		
		
		placeDumper.dump();
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
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(circuit);
		double totalWLCost = effcc.calculateTotalCost();
		System.out.format("%s %15s: %f\n", prefix, "BB cost", totalWLCost);
		
		//TODO: timingGraph
		TimingGraph timingGraph = new TimingGraph(circuit);
		timingGraph.build();
		timingGraph.recalculateAllSlackCriticalities();
		
		double totalTimingCost = timingGraph.calculateTotalCost();
		double maxDelay = timingGraph.getMaxDelay();
		System.out.format("%s %15s: %e\n", prefix, "timing cost", totalTimingCost);
		System.out.format("%s %15s: %e\n", prefix, "max delay", maxDelay);
		
		System.out.println();
	}
	
	private static void printTimer(String prefix) {
		System.out.println();
		double placeTime = CLI.getTimer();
		Logger.log(prefix + ": " + placeTime + "s");
	}
}
