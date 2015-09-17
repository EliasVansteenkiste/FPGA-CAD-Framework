package placers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import placers.MDP.MDPPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;
import placers.parser.PlaceParser;

import architecture.Architecture;
import architecture.FourLutSanitized;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public abstract class Placer {
	
	protected static Map<String, String> defaultOptions = new HashMap<String, String>();
	protected static List<String> requiredOptions = new ArrayList<String>();
	
	protected Architecture architecture;
	protected PackedCircuit circuit;
	protected Map<String, String> options;
	
	protected Placer(Architecture architecture, PackedCircuit circuit, Map<String, String> options) {
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
	
	
	protected boolean hasOption(String optionName) {
		return this.options.containsKey(optionName);
	}
	protected String getOption(String optionName) {
		return this.options.get(optionName);
	}
	
	
	public static Placer newPlacer(String type, Architecture architecture, PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit, HashMap<String, String> options) {
		switch(type) {
		
		case "parser":
			return new PlaceParser(architecture, packedCircuit, options);
		
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
