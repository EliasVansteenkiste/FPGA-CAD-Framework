package placers;

import java.util.ArrayList;
import java.util.HashMap;

import placers.MDP.MDPPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

import architecture.Architecture;
import architecture.FourLutSanitized;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public abstract class Placer {
	
	protected static HashMap<String, String> defaultOptions;
	protected static ArrayList<String> requiredOptions;
	
	static {
		defaultOptions = new HashMap<String, String>();
		requiredOptions = new ArrayList<String>();
	}
	
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
	
	
	
	public static Placer newPlacer(String type, Architecture architecture, PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit, HashMap<String, String> options) {
		switch(type) {
		
		case "SA":
		case "sa":
			return new WLD_SAPlacer(architecture, packedCircuit, options);
		
		case "AP":
		case "ap":
			return new HeteroAnalyticalPlacerTwo(architecture, packedCircuit, options);
			
		case "MDP":
		case "mdp":
			return new MDPPlacer((FourLutSanitized) architecture, packedCircuit, options);
			
		default:
			System.err.println("Unknown placer type: " + type);
			System.err.println(Thread.currentThread().getStackTrace());
			System.err.println("ok");
			System.exit(1);
		}
		
		return null;
	}	
}
