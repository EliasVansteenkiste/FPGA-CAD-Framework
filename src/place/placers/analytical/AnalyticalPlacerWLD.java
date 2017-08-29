package place.placers.analytical;

import place.circuit.Circuit;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.Random;

public class AnalyticalPlacerWLD extends AnalyticalPlacer {

    public AnalyticalPlacerWLD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }

    @Override
    protected boolean isTimingDriven() {
        return false;
    }
    
    @Override
    protected void initializeIteration(int iteration){
    	if(iteration > 0) {
    		this.anchorWeight *= this.anchorWeightMultiplier;
    		this.legalizer.increaseAnnealQualityAndGridForce();
    	}
    }

    @Override
    public String getName() {
        return "Wirelength driven analytical placer";
    }

	@Override
	protected void calculateTimingCost() {
		this.timingCost = 0;
	}
}
