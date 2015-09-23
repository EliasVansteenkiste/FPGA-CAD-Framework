package cli;

import java.util.HashMap;
import java.util.Map;

import placers.Placer;
import placers.PlacerFactory;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import util.Logger;

import cli.Options;
import flexible_architecture.Circuit;
import flexible_architecture.NetParser;
import flexible_architecture.PlaceDumper;
import flexible_architecture.PlaceParser;
import flexible_architecture.architecture.FlexibleArchitecture;


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
		Circuit circuit = new Circuit(options.circuit, architecture);
		
		// Parse net file
		NetParser netParser = new NetParser(circuit, options.netFile);
		CLI.startTimer();
		netParser.parse();
		CLI.stopTimer();
		CLI.printTimer("Circuit parsing: ");
		
		
		// Read the place file
		if(options.startingStage.equals("place")) {
			PlaceParser placeParser = new PlaceParser(circuit, options.placeFile);
			placeParser.parse();
			
			CLI.printStatistics("parser", circuit);
		}
		
		
		// Loop through the placers
		for(int i = 0; i < options.placers.size(); i++) {
			String placerName = options.placers.get(i);
			HashMap<String, String> placerOptions = options.placerOptions.get(i);
			
			// Do a random placement if an initial placement is required
			if(i == 0 && PlacerFactory.needsInitialPlacement(placerName)) {
				CLI.timePlacement("random", circuit);
			}
			
			// Create the placer and place the circuit
			System.out.println("Placing with " + placerName + "...");
			CLI.timePlacement(placerName, circuit, placerOptions);
		}
		
		
		
		PlaceDumper placeDumper = new PlaceDumper(circuit, options.outputFile);
		placeDumper.dump();
		
		// Print out the place file
		/*try {
			circuit.dumpPlacement(options.placeFile.toString());
		} catch (FileNotFoundException e) {
			error("Place file not found: " + options.placeFile);
		}*/
	}
	
	
	private static void timePlacement(String placerName, Circuit circuit) {
		CLI.timePlacement(placerName, circuit, new HashMap<String, String>());
	}
	
	private static void timePlacement(String placerName, Circuit circuit, Map<String, String> options) {
		CLI.startTimer();
		Placer placer = PlacerFactory.getPlacer(placerName, circuit, options);
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
		double totalCost = effcc.calculateTotalCost();
		System.out.format("%s %15s: %f\n", prefix, "BB cost", totalCost);
		
		//TODO: timingGraph
		/*TimingGraph timingGraph = new TimingGraph(circuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.format("%s %15s: %f\n", prefix, "timing cost", timingGraph.calculateTotalCost());
		System.out.format("%s %15s: %f\n", prefix, "max delay", maxDelay);*/
		
		System.out.println();
	}
	
	private static void printTimer(String prefix) {
		System.out.println();
		double placeTime = CLI.getTimer();
		Logger.log(prefix + ": " + placeTime + "s");
	}
}
