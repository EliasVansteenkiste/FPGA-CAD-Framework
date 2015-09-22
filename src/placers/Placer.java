package placers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import placers.MDP.MDPPlacer;
import placers.SAPlacer.TD_SAPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

import architecture.Architecture;
import architecture.FourLutSanitized;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;
import circuit.parser.placement.PlaceParser;

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
	
	protected boolean parseBooleanOption(String option) {
		try {
			int greedy_int = Integer.parseInt(this.options.get(option));
			return (greedy_int > 0);
		
		} catch(NumberFormatException e) {
			return Boolean.parseBoolean(this.options.get(option));
		}
	}
	
	protected int parseIntegerOptionWithDefault(String option, int defaultValue) {
		int value = Integer.parseInt(this.options.get(option));
		if(value == -1) {
			value = defaultValue;
		}
		
		return value;
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
		
		case "wld_sa":
			return new WLD_SAPlacer(architecture, packedCircuit, options);
			
		case "td_sa":
			return new TD_SAPlacer(architecture, packedCircuit, prePackedCircuit, options);
		
		case "ap":
			return new HeteroAnalyticalPlacerTwo(architecture, packedCircuit, options);
			
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
