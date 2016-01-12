package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.List;
import java.util.Random;

import visual.PlacementVisualizer;
import circuit.Circuit;

public class GradientPlacerWLD extends GradientPlacer {

    public GradientPlacerWLD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    protected boolean isTimingDriven() {
        return false;
    }

    @Override
    protected void updateLegalIfNeeded(int iteration) {
        // This placer always accepts the latest solution.
        // No cost has to be calculated, so this is faster.
        this.updateLegal(this.legalizer.getLegalX(), this.legalizer.getLegalY());
    }


    @Override
    protected void addStatTitlesGP(List<String> titles) {
        // Do nothing
    }

    @Override
    protected void addStats(List<String> stats) {
        // Do nothing
    }


    @Override
    public String getName() {
        return "Wirelength driven gradient descent placer";
    }
}
