package placers;

import interfaces.Logger;
import interfaces.Options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import util.Timer;
import visual.PlacementVisualizer;

import circuit.Circuit;
import circuit.exceptions.PlacementException;



public abstract class Placer {

    protected Logger logger;
    protected PlacementVisualizer visualizer;
    protected Circuit circuit;
    protected Options options;
    protected Random random;

    private Map<String, Timer> timers = new LinkedHashMap<>();
    private int maxTimerNameLength = 0;


    protected Placer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        this.circuit = circuit;
        this.options = options;
        this.random = random;
        this.logger = logger;
        this.visualizer = visualizer;

        this.printOptions();
    }


    public abstract String getName();
    public abstract void initializeData();
    public abstract void place() throws PlacementException;


    private final void printOptions() {
        int maxLength = this.options.getMaxNameLength();

        this.logger.printf("%s options:\n", this.getName());

        String format = String.format("%%-%ds| %%s\n", maxLength + 1);
        for(Map.Entry<String, Object> optionEntry : this.options.entrySet()) {
            String optionName = optionEntry.getKey();
            Object optionValue = optionEntry.getValue();

            this.logger.printf(format, optionName, optionValue);
        }

        this.logger.println();
    }


    protected void addTimer(String name) {
        if(this.timers.get(name) != null) {
            this.logger.raise("Timer has already been added: " + name);

        } else {
            this.timers.put(name, new Timer(this.logger));

            if(name.length() > this.maxTimerNameLength) {
                this.maxTimerNameLength = name.length();
            }
        }
    }

    protected void startTimer(String name) {
        this.timers.get(name).start();
    }
    protected void stopTimer(String name) {
        this.timers.get(name).stop();
    }

    public void printRuntimeBreakdown() {
        if(this.timers.size() > 0) {
            this.logger.printf("%s runtime breakdown:\n", this.getName());

            String totalName = "total";
            double totalTime = 0;
            int maxLength = Math.max(this.maxTimerNameLength, totalName.length());


            String format = String.format("%%-%ds| %%f\n", maxLength + 1);
            for(Map.Entry<String, Timer> timerEntry : this.timers.entrySet()) {
                String name = timerEntry.getKey();
                Double time = timerEntry.getValue().getTime();
                totalTime += time;

                this.logger.printf(format, name, time);
            }

            this.logger.printf(format, totalName, totalTime);
            this.logger.println();
        }
    }
}
