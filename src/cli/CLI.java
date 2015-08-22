package cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import packers.BlePacker;
import packers.ClbPacker;
import placers.Placer;
import placers.MDP.MDPBasedPlacer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import timinganalysis.TimingGraph;

import architecture.Architecture;
import architecture.HeterogeneousArchitecture;

import circuit.BlePackedCircuit;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;
import circuit.parser.blif.BlifReader;
import circuit.parser.net.NetReader;
import cli.Options;


public class CLI {
	
	public static void main(String[] args) {
		
		// The Options class can parse the command line options
		Options options = new Options();
		options.parseArguments(args);
		
		
		// Read the net file
		NetReader netReader = new NetReader();
		PrePackedCircuit prePackedCircuit = null;
		
		try {
			netReader.readNetlist(options.netFile.toString(), 6);
		} catch(IOException e) {
			error("Failed to read net file");
		}
		
		PackedCircuit packedCircuit = netReader.getPackedCircuit();
		
		
		
		// Set the architecture
		// Currently only the heterogeneous architecture is supported
		Architecture architecture = null; // Needed to suppress "variable may not be initialized" errors
		switch(options.architecture) {
			case "heterogeneous":
				architecture = new HeterogeneousArchitecture(packedCircuit);
				break;
			
			default:
				error("Architecture type not recognized: " + options.architecture);
		}
		
		
		
		// Place the circuit
		Placer placer = null; // Needed to suppress "variable may not be initialized" errors
		switch(options.placer) {
			case "MDP":
				if(!architecture.getClass().equals(HeterogeneousArchitecture.class)) {
					error("MDP currently only supports the architecture \"heterogeneous\"");
				}
				
				placer = new MDPBasedPlacer((HeterogeneousArchitecture) architecture, packedCircuit);
				break;
			
			case "analytical":
				
			case "random":
				
			case "SA":
				
			case "TDSA":
				error("Placer not yet implemented: " + options.placer);
				
			default:
				error("Placer type not recognized: " + options.placer);
		}
		
		long timeStartPlace = System.nanoTime();
		placer.place();
		long timeStopPlace = System.nanoTime();
		
		
		
		// Analyze the circuit and print statistics
		System.out.println();
		double placeTime = (timeStopPlace - timeStartPlace) * 1e-12;
		System.out.format("%15s: %fs\n", "Place time", placeTime);
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		double totalCost = effcc.calculateTotalCost();
		System.out.format("%15s: %f\n", "Total cost", totalCost);
		
		// TODO: why does this only work with a pre-packed circuit?
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.format("%15s: %f\n", "Max delay", maxDelay);
		
		
		
		// Print out the place file
		try {
			packedCircuit.dumpPlacement(options.placeFile.toString());
		} catch (FileNotFoundException e) {
			error("Place file not found: " + options.placeFile);
		}
	}
	
	
	private static void error(String error) {
		System.err.println(error);
		System.exit(1);
	}

}
