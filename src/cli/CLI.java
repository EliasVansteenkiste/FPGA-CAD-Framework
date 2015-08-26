package cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import placers.Placer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import placers.random.RandomPlacer;
import timinganalysis.TimingGraph;

import architecture.Architecture;
import architecture.FourLutSanitized;
import architecture.HeterogeneousArchitecture;

import circuit.PackedCircuit;
import circuit.PrePackedCircuit;
import circuit.parser.net.NetReader;
import cli.Options;


public class CLI {
	
	private static long timerBegin, timerEnd;
	
	public static void main(String[] args) {
		
		// The Options class can parse the command line options
		Options options = new Options();
		options.parseArguments(args);
		
		
		// Read the net file
		NetReader netReader = new NetReader();
		
		try {
			netReader.readNetlist(options.netFile.toString(), 6);
		} catch(IOException e) {
			error("Failed to read net file: " + options.netFile.toString());
		}
		
		PrePackedCircuit prePackedCircuit = netReader.getPrePackedCircuit();
		PackedCircuit packedCircuit = netReader.getPackedCircuit();
		
		
		
		// Set the architecture
		// Currently only the heterogeneous architecture is supported
		Architecture architecture = null; // Needed to suppress "variable may not be initialized" errors
		switch(options.architecture) {
			case "4lut":
			case "4LUT":
				architecture = new FourLutSanitized(packedCircuit);
				break;
				
			case "heterogeneous":
				architecture = new HeterogeneousArchitecture(packedCircuit);
				break;
			
			default:
				error("Architecture type not recognized: " + options.architecture);
		}
		
		
		
		// If a random initialization is required: do it
		if(options.random) {
			Random rand = new Random(1);
			RandomPlacer.placeCLBsandFixedIOs(packedCircuit, (FourLutSanitized) architecture, rand);
		}
		
		
		// Loop through the placers
		for(String placerName : options.placers.keySet()) {
			System.out.println("Placing with " + placerName + "...");
			
			HashMap<String, String> placerOptions = options.placers.get(placerName);
			
			// Create the placer and place the circuit
			CLI.startTimer();
			Placer placer = Placer.newPlacer(placerName, architecture, packedCircuit, placerOptions);
			placer.place();
			CLI.stopTimer();
			
			CLI.printStatistics(placerName, prePackedCircuit, packedCircuit);
		}
		
		
		
		// Print out the place file
		try {
			packedCircuit.dumpPlacement(options.placeFile.toString());
		} catch (FileNotFoundException e) {
			error("Place file not found: " + options.placeFile);
		}
	}
	
	private static void startTimer() {
		CLI.timerBegin = System.nanoTime();
	}
	private static void stopTimer() {
		CLI.timerEnd = System.nanoTime();
	}
	private static double getTimer() {
		return (CLI.timerEnd - CLI.timerBegin) * 1e-12;
	}
	
	private static void printStatistics(String prefix, PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit) {
		
		System.out.println();
		double placeTime = CLI.getTimer();
		System.out.format("%s %15s: %fs\n", prefix, "place time", placeTime);
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		double totalCost = effcc.calculateTotalCost();
		System.out.format("%s %15s: %f\n", prefix, "total cost", totalCost);
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.format("%s %15s: %f\n", prefix, "max delay", maxDelay);
	}
	
	
	private static void error(String error) {
		System.err.println(error);
		System.exit(1);
	}

}
