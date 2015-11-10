package placers;

import interfaces.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import visual.PlacementVisualizer;

import circuit.Circuit;



public abstract class Placer {

    protected final static Map<String, String> defaultOptions = new HashMap<>();
    protected static List<String> requiredOptions = new ArrayList<>();

    protected Logger logger;
    protected PlacementVisualizer visualizer;
    protected Circuit circuit;
    protected Map<String, String> options;


    protected Placer(Logger logger, PlacementVisualizer visualizer, Circuit circuit, Map<String, String> options) {
        this.logger = logger;
        this.visualizer = visualizer;
        this.circuit = circuit;
        this.options = options;

        this.parseOptions();
    }

    protected final void parseOptions() {
        int maxLength = 0;
        for(Map.Entry<String, String> optionEntry : Placer.defaultOptions.entrySet()) {
            String key = optionEntry.getKey();
            String defaultValue = optionEntry.getValue();

            String value = this.options.get(key);
            if(value == null) {
                value = defaultValue;
                this.options.put(key,  value);
            }

            int length = key.length();
            if(length > maxLength) {
                maxLength = length;
            }
        }

        this.logger.logf("%s options:\n", this.getName());
        String format = String.format("%%-%ds| %%s\n", maxLength + 1);
        for(String key : Placer.defaultOptions.keySet()) {
            String value = this.options.get(key);
            this.logger.logf(format, key, value);
        }

        this.logger.logln();
    }

    protected String parseStringOption(String option) {
        return this.options.get(option);
    }

    protected boolean parseBooleanOption(String option) {
        try {
            return (Integer.parseInt(this.options.get(option)) > 0);

        } catch(NumberFormatException e) {
            return Boolean.parseBoolean(this.options.get(option));
        }
    }

    protected double parseDoubleOption(String option) {
        return Double.parseDouble(this.options.get(option));
    }
    protected int parseIntegerOption(String option) {
        return Integer.parseInt(this.options.get(option));
    }

    protected int parseIntegerOptionWithDefault(String option, int defaultValue) {
        int value = Integer.parseInt(this.options.get(option));
        if(value == -1) {
            value = defaultValue;
        }

        return value;
    }

    public abstract String getName();
    public abstract void initializeData();
    public abstract void place();


    protected boolean hasOption(String optionName) {
        return this.options.containsKey(optionName);
    }
    protected String getOption(String optionName) {
        return this.options.get(optionName);
    }
}
