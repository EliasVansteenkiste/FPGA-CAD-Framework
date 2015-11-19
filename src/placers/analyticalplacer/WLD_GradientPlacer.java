package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;

public class WLD_GradientPlacer extends GradientPlacer {

    public WLD_GradientPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    public String getName() {
        return "Wirelength driven gradient descent placer";
    }
}
