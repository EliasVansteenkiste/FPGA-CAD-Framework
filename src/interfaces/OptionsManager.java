package interfaces;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import circuit.Circuit;

import main.Main;

import placers.Placer;
import visual.PlacementVisualizer;


public abstract class OptionsManager {

    public enum StartingStage {NET, PLACE};

    protected PlacerFactory placerFactory;

    protected Logger logger;

    private Options mainOptions;
    private List<String> placerNames = new ArrayList<String>();
    private List<Options> placerOptions = new ArrayList<Options>();


    protected OptionsManager(Logger logger) {
        this.logger = logger;

        this.placerFactory = new PlacerFactory(this.logger);

        this.mainOptions = new Options(this.logger);
        Main.initOptionList(this.mainOptions);
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Options getMainOptions() {
        return this.mainOptions;
    }

    public int getNumPlacers() {
        return this.placerNames.size();
    }
    public Placer getPlacer(int placerIndex, Circuit circuit, Random random, PlacementVisualizer visualizer) {
        String placerName = this.placerNames.get(placerIndex);
        Options options = this.placerOptions.get(placerIndex);

        return this.placerFactory.newPlacer(placerName, circuit, options, random, visualizer);
    }


    protected Options getDefaultOptions(String placerName) {
        return this.placerFactory.initOptions(placerName);
    }



    public void insertRandomPlacer() {
        String placerName = "random";
        this.addPlacer(0, placerName, this.getDefaultOptions(placerName));
    }

    public void addPlacer(String placerName, Options options) {
        this.addPlacer(this.getNumPlacers(), placerName, options);
    }

    private void addPlacer(int index, String placerName, Options options) {
        this.placerNames.add(index, placerName);
        this.placerOptions.add(index, options);
    }
}
