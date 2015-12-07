package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

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
    protected void printStatisticsHeader() {
        this.logger.println("Iteration    anchor weight    max utilization    time");
        this.logger.println("---------    -------------    ---------------    ----");
    }

    @Override
    protected void printStatistics(int iteration, double time) {
        this.logger.printf("%-9d    %-13f    %-15f    %f\n", iteration, this.anchorWeight, this.maxUtilization, time);
    }


    @Override
    public String getName() {
        return "Wirelength driven gradient descent placer";
    }
}
