package placers;

import java.util.ArrayList;
import java.util.HashMap;

import placers.MDP.MDPPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

import architecture.Architecture;
import architecture.FourLutSanitized;
import architecture.HeterogeneousArchitecture;
import circuit.PackedCircuit;

public abstract class Placer {
	
	protected static HashMap<String, String> defaultOptions;
	protected static ArrayList<String> requiredOptions;
	
	protected Architecture architecture;
	protected PackedCircuit circuit;
	protected HashMap<String, String> options;
	
	protected Placer(Architecture architecture, PackedCircuit circuit, HashMap<String, String> options) {
		this.architecture = architecture;
		this.circuit = circuit;
		this.options = options;
		
		this.parseOptions();
	}
	
	protected void parseOptions() {
		for(String option : defaultOptions.keySet()) {
			if(!this.options.containsKey(option)) {
				this.options.put(option,  defaultOptions.get(option));
			}
		}
	}
	
	
	public abstract void place();
	
	
	
	public static Placer newPlacer(String type, Architecture architecture, PackedCircuit circuit, HashMap<String, String> options) {
		switch(type) {
		
		case "AP":
		case "ap":
			return new HeteroAnalyticalPlacerTwo((HeterogeneousArchitecture) architecture, circuit);
			
		case "MDP":
		case "mdp":
			return new MDPPlacer((FourLutSanitized) architecture, circuit, options);
			
		default:
			System.err.println("Unknown placer type: " + type);
			System.err.println(Thread.currentThread().getStackTrace());
			System.err.println("ok");
			System.exit(1);
		}
		
		return null;
	}	
}
