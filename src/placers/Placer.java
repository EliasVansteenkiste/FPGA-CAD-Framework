package placers;

import interfaces.Logger;
import interfaces.OptionList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import visual.PlacementVisualizer;

import circuit.Circuit;



public abstract class Placer {

    protected final static Map<String, String> defaultOptions = new HashMap<>();
    protected static List<String> requiredOptions = new ArrayList<>();

    protected Logger logger;
    protected PlacementVisualizer visualizer;
    protected Circuit circuit;
    protected OptionList options;
    protected Random random;



    protected Placer(Circuit circuit, OptionList options, Random random, Logger logger, PlacementVisualizer visualizer) {
        this.circuit = circuit;
        this.options = options;
        this.random = random;
        this.logger = logger;
        this.visualizer = visualizer;

        this.printOptions();
    }

    private final void printOptions() {
        int maxLength = this.options.getMaxNameLength();

        this.logger.logf("%s options:\n", this.getName());
        String format = String.format("%%-%ds| %%s\n", maxLength + 1);
        for(Map.Entry<String, Object> optionEntry : this.options.entrySet()) {
            String optionName = optionEntry.getKey();
            Object optionValue = optionEntry.getValue();

            this.logger.logf(format, optionName, optionValue);
        }
    }

    public abstract String getName();
    public abstract void initializeData();
    public abstract void place();
}
