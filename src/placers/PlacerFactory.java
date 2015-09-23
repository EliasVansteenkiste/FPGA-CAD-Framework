package placers;

import java.util.HashMap;
import java.util.Map;

import placers.MDP.MDPPlacer;
import placers.SAPlacer.TD_SAPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;
import placers.random.RandomPlacer;
import util.Logger;
import architecture.Architecture;
import architecture.FourLutSanitized;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;
import flexible_architecture.Circuit;

public class PlacerFactory {
	
	private static Map<String, Boolean> needsInitialPlacement = new HashMap<String, Boolean>();
	static {
		needsInitialPlacement.put("random", false);
		needsInitialPlacement.put("wld_sa", true);
		needsInitialPlacement.put("td_sa", true);
		needsInitialPlacement.put("ap", false);
	}
	
	public static boolean needsInitialPlacement(String type) {
		if(PlacerFactory.needsInitialPlacement.containsKey(type)) {
			return PlacerFactory.needsInitialPlacement.get(type);
		
		} else {
			Logger.raise("Unknown placer type: " + type);
			return false;
		}
	}
	
	public static Placer getPlacer(String type, Circuit circuit, Map<String, String> options) {
		switch(type) {
		
		case "random":
			return new RandomPlacer(circuit, options);
			
		case "wld_sa":
			return new WLD_SAPlacer(circuit, options);
			
		case "td_sa":
			return new TD_SAPlacer(circuit, options);
		
		case "ap":
			//return new HeteroAnalyticalPlacerTwo(circuit, options);
			
		case "mdp":
			//return new MDPPlacer(circuit, options);
			
		default:
			Logger.raise("Unknown placer type: " + type);
			return null;
		}
	}	
}
