package placers;

import interfaces.Logger;
import interfaces.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    protected List<String> statTitles = new ArrayList<>();
    private List<Integer> statLengths = new ArrayList<>();
    private int numStats;
    private static int statSpaces = 3;


    protected Placer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        this.circuit = circuit;
        this.options = options;
        this.random = random;
        this.logger = logger;
        this.visualizer = visualizer;

        this.printOptions();


        this.addStatTitles(this.statTitles);
        this.numStats = this.statTitles.size();

        if(this.numStats > 0) {
            this.printStatsHeader();
        }
    }


    public abstract String getName();
    public abstract void initializeData();
    public abstract void place() throws PlacementException;

    protected abstract void addStatTitles(List<String> titles);


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

    protected void startTimer(String name) {
        if(!this.timers.containsKey(name)) {
            this.timers.put(name, new Timer());

            if(name.length() > this.maxTimerNameLength) {
                this.maxTimerNameLength = name.length();
            }
        }

        try {
            this.timers.get(name).start();
        } catch(IllegalStateException error) {
            this.logger.raise("There was a problem with timer \"" + name + "\":", error);
        }
    }
    protected void stopTimer(String name) {
        if(this.timers.containsKey(name)) {

            try {
                this.timers.get(name).stop();
            } catch(IllegalStateException error) {
                this.logger.raise("There was a problem with timer \"" + name + "\":", error);
            }

        } else {
            this.logger.raise("Timer hasn't been initialized: " + name);
        }
    }

    private void printStatsHeader() {
        StringBuilder header = new StringBuilder();
        StringBuilder underlines = new StringBuilder();

        for(String title : this.statTitles) {
            int length = title.length();
            this.statLengths.add(length);

            String format = "%-" + (length + Placer.statSpaces) + "s";

            char[] underline = new char[length];
            Arrays.fill(underline, '-');

            header.append(String.format(format, title));
            underlines.append(String.format(format, new String(underline)));
        }


        this.logger.println(header.toString());
        this.logger.println(underlines.toString());
    }

    protected void printStats(String... stats) {
        StringBuilder line = new StringBuilder();

        for(int i = 0; i < this.numStats; i++) {
            int length = this.statLengths.get(i);
            String format = "%-" + (length + Placer.statSpaces) + "s";
            String stat = String.format(format, stats[i]);

            line.append(String.format("%-"+length+"s", stat));
        }

        this.logger.println(line.toString());
    }

    public void printRuntimeBreakdown() {
        if(this.timers.size() > 0) {
            this.logger.printf("%s runtime breakdown:\n", this.getName());

            String totalName = "total";
            int maxLength = Math.max(this.maxTimerNameLength, totalName.length());
            String format = String.format("%%-%ds| %%f\n", maxLength + 1);

            double totalTime = 0;
            for(Map.Entry<String, Timer> timerEntry : this.timers.entrySet()) {
                String name = timerEntry.getKey();

                double time = 0;
                try {
                    time = timerEntry.getValue().getTime();
                } catch(IllegalStateException error) {
                    this.logger.raise("There was a problem with timer \"" + name + "\":", error);
                }
                totalTime += time;

                this.logger.printf(format, name, time);
            }

            this.logger.printf(format, totalName, totalTime);
            this.logger.println();
        }
    }
}
