package placers;

import interfaces.Logger;

import java.util.HashMap;
import java.util.Map;

import circuit.Circuit;

import placers.SAPlacer.TD_SAPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.TD_AnalyticalPlacer;
import placers.analyticalplacer.WLD_AnalyticalPlacer;
import placers.random.RandomPlacer;
import visual.PlacementVisualizer;

public class PlacerFactory {

    public static Placer newPlacer(String type, Logger logger, PlacementVisualizer visualizer, Circuit circuit) {
        return PlacerFactory.newPlacer(type, logger, visualizer, circuit, new HashMap<String, String>());
    }

    public static Placer newPlacer(String type, Logger logger, PlacementVisualizer visualizer, Circuit circuit, Map<String, String> options) {
        switch(type) {
            case "random":
                return new RandomPlacer(logger, visualizer, circuit, options);

            case "greedy_wld_sa":
                options.put("greedy", "1");
            case "detailed_wld_sa":
                options.put("detailed", "1");
            case "wld_sa":
                return new WLD_SAPlacer(logger, visualizer, circuit, options);

            case "greedy_td_sa":
                options.put("greedy", "1");
            case "detailed_td_sa":
                options.put("detailed", "1");
            case "td_sa":
                return new TD_SAPlacer(logger, visualizer, circuit, options);

            case "wld_ap":
                return new WLD_AnalyticalPlacer(logger, visualizer, circuit, options);

            case "td_ap":
                return new TD_AnalyticalPlacer(logger, visualizer, circuit, options);

            default:
                throw new IllegalArgumentException("Unknown placer type \"" + type + "\"");
        }
    }
}
