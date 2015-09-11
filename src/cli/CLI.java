package cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import packers.BlePacker;
import packers.ClbPacker;
import placers.Placer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import placers.random.RandomPlacer;
import timinganalysis.TimingGraph;

import architecture.Architecture;
import circuit.BlePackedCircuit;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;
import circuit.parser.blif.BlifReader;
import circuit.parser.net.NetReader;
import cli.Options;
import flexible_architecture.Circuit;
import flexible_architecture.architecture.FlexibleArchitecture;


public class CLI {
	
	private static long timerBegin, timerEnd;
	
	public static void main(String[] args) {
		
		// The Options class can parse the command line options
		Options options = new Options();
		options.parseArguments(args);
		
		FlexibleArchitecture architecture = new FlexibleArchitecture(options.architecture);
		architecture.parse();
		
		Circuit circuit = new Circuit(architecture, options.netFile.toString());
		circuit.parse();
		
		
		
		
		
		// If a random initialization is required: do it
		/*if(options.random) {
			Random rand = new Random(1);
			RandomPlacer.placeCLBsandFixedIOs(packedCircuit, architecture, rand);
		}*/
		
		
		//CLI.printStatistics("initial", prePackedCircuit, packedCircuit, false);
		
		// Loop through the placers
		for(String placerName : options.placers.keySet()) {
			System.out.println("Placing with " + placerName + "...");
			
			HashMap<String, String> placerOptions = options.placers.get(placerName);
			
			// Create the placer and place the circuit
			CLI.startTimer();
			//Placer placer = Placer.newPlacer(placerName, architecture, prePackedCircuit, packedCircuit, placerOptions);
			//placer.place();
			CLI.stopTimer();
			
			//CLI.printStatistics(placerName, prePackedCircuit, packedCircuit);
		}
		
		
		
		// Print out the place file
		/*try {
			packedCircuit.dumpPlacement(options.placeFile.toString());
		} catch (FileNotFoundException e) {
			error("Place file not found: " + options.placeFile);
		}*/
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
	
	private static void printStatistics(String prefix, PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit) {
		CLI.printStatistics(prefix, prePackedCircuit, packedCircuit, true);
	}
	
	private static void printStatistics(String prefix, PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit, boolean printTime) {
		
		System.out.println();
		
		if(printTime) {	
			double placeTime = CLI.getTimer();
			System.out.format("%s %15s: %f s\n", prefix, "place time", placeTime);
		}
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		double totalCost = effcc.calculateTotalCost();
		System.out.format("%s %15s: %f\n", prefix, "total cost", totalCost);
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.format("%s %15s: %f\n", prefix, "max delay", maxDelay);
		
		System.out.println();
	}
	
	
	private static void error(String error) {
		System.err.println(error);
		System.exit(1);
	}

}
