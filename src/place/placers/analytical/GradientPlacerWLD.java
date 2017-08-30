package place.placers.analytical;

import place.circuit.Circuit;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.Random;

public class GradientPlacerWLD extends GradientPlacer {

    public GradientPlacerWLD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    protected boolean isTimingDriven() {
        return false;
    }

    @Override
    protected void initializeIteration(int iteration) {
        if(iteration > 0) {
            this.anchorWeight = Math.pow((double)iteration / (this.numIterations - 1.0), this.anchorWeightExponent) * this.anchorWeightStop;
            this.learningRate *= this.learningRateMultiplier;
            this.legalizer.increaseAnnealQualityAndGridForce();
            this.effortLevel = Math.max(this.effortLevelStop, (int)Math.round(this.effortLevel*0.5));
        }
    }

    @Override
    public String getName() {
        return "Wirelength driven gradient descent placer";
    }

	@Override
	protected void calculateTimingCost() {
		this.timingCost = 0;
	}
}
