package placers;

import java.util.HashMap;
import java.util.Map;

import architecture.circuit.Circuit;

import placers.SAPlacer.TD_SAPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.TD_AnalyticalPlacer;
import placers.analyticalplacer.WLD_AnalyticalPlacer;
import placers.random.RandomPlacer;
import util.Logger;

public class PlacerFactory {
	
	public static Placer newPlacer(String type, Circuit circuit) {
		return PlacerFactory.newPlacer(type, circuit, new HashMap<String, String>());
	}
	
	public static Placer newPlacer(String type, Circuit circuit, Map<String, String> options) {
		switch(type) {
		
		case "random":
			return new RandomPlacer(circuit, options);
		
		case "greedy_wld_sa":
			options.put("greedy", "1");
		case "detailed_wld_sa":
			options.put("detailed", "1");
		case "wld_sa":
			return new WLD_SAPlacer(circuit, options);
		
		case "greedy_td_sa":
			options.put("greedy", "1");
		case "detailed_td_sa":
			options.put("detailed", "1");
		case "td_sa":
			return new TD_SAPlacer(circuit, options);
		
		case "wld_ap":
			return new WLD_AnalyticalPlacer(circuit, options);
		
		case "td_ap":
			return new TD_AnalyticalPlacer(circuit, options);
			
		case "mdp":
			//return new MDPPlacer(circuit, options);
			
		default:
			Logger.raise("Unknown placer type: " + type);
			return null;
		}
	}	
}
