package interfaces;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import circuit.Circuit;

import main.Main;

import placers.Placer;
import visual.PlacementVisualizer;


public abstract class Options {

    public enum StartingStage {NET, PLACE};

    protected PlacerFactory placerFactory;

    protected Logger logger;

    private OptionList mainOptions;
    private List<String> placerNames = new ArrayList<String>();
    private List<OptionList> placerOptions = new ArrayList<OptionList>();


    protected Options(Logger logger) {
        this.logger = logger;

        this.placerFactory = new PlacerFactory(this.logger);

        this.mainOptions = new OptionList(this.logger);
        Main.initOptionList(this.mainOptions);
    }

    public Logger getLogger() {
        return this.logger;
    }

    public OptionList getMainOptions() {
        return this.mainOptions;
    }

    public int getNumPlacers() {
        return this.placerNames.size();
    }
    public Placer getPlacer(int placerIndex, Circuit circuit, Random random, PlacementVisualizer visualizer) {
        String placerName = this.placerNames.get(placerIndex);
        OptionList options = this.placerOptions.get(placerIndex);

        return this.placerFactory.newPlacer(placerName, circuit, options, random, visualizer);
    }


    protected OptionList getDefaultOptions(String placerName) {
        return this.placerFactory.initOptions(placerName);
    }



    public void insertRandomPlacer() {
        String placerName = "random";
        this.addPlacer(0, placerName, this.getDefaultOptions(placerName));
    }

    public void addPlacer(String placerName, OptionList options) {
        this.addPlacer(this.getNumPlacers(), placerName, options);
    }

    private void addPlacer(int index, String placerName, OptionList options) {
        this.placerNames.add(index, placerName);
        this.placerOptions.add(index, options);
    }
}
