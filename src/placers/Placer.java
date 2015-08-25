package placers;

import java.util.ArrayList;
import java.util.HashMap;

import placers.MDP.MDPPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

import architecture.Architecture;
import architecture.HeterogeneousArchitecture;
import circuit.PackedCircuit;

public abstract class Placer {
	
	/**
	 * Place the circuit with all default options.
	 * Equivalent to calling place() with an empty HashMap as parameter.
	 */
	public int place() {
		return this.place(new HashMap<String, String>());
	}
	
	/**
	 * Place the circuit that was given in the constructor.
	 * @param options	A hashmap containing the options for the placer. The accepted options are different for each placer.
	 */
	public abstract int place(HashMap<String, String> options);
	
	
	
	public static Placer newPlacer(String type, Architecture architecture, PackedCircuit circuit) {
		switch(type) {
		
		case "AP":
		case "ap":
			return new HeteroAnalyticalPlacerTwo((HeterogeneousArchitecture) architecture, circuit);
			
		case "MDP":
		case "mdp":
			return new MDPPlacer((HeterogeneousArchitecture) architecture, circuit);
			
		default:
			System.err.println("Unknown placer type: " + type);
			System.err.println(Thread.currentThread().getStackTrace());
			System.err.println("ok");
			System.exit(1);
		}
		
		return null;
	}	
}
