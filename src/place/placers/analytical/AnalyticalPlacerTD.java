package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import place.circuit.Circuit;
import place.circuit.timing.TimingGraph;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

public class AnalyticalPlacerTD extends AnalyticalPlacer {

    private static final String
        O_CRITICALITY_EXPONENT = "criticality exponent",
        O_CRITICALITY_THRESHOLD = "criticality threshold",
        O_MAX_PER_CRIT_EDGE = "max per crit edge",
        O_TRADE_OFF = "trade off";

    public static void initOptions(Options options) {
        AnalyticalPlacer.initOptions(options);

        options.add(
                O_CRITICALITY_EXPONENT,
                "criticality exponent of connections",
                new Double(3));

        options.add(
                O_CRITICALITY_THRESHOLD,
                "minimal criticality for adding TD constraints",
                new Double(0.6));

        options.add(
                O_MAX_PER_CRIT_EDGE,
                "the maximum number of critical edges compared to the total number of edges",
                new Double(3));

        options.add(
                O_TRADE_OFF,
                "trade_off between timing driven and wirelength driven (0 = not timing driven)",
                new Double(20));
    }

    private static String 
    	T_UPDATE_CRIT_CON = "update critical connections";

    private double criticalityExponent, criticalityThreshold, maxPerCritEdge;
    private TimingGraph timingGraph;

    private CriticalityCalculator criticalityCalculator;

    public AnalyticalPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble(O_CRITICALITY_EXPONENT);
        this.criticalityThreshold = options.getDouble(O_CRITICALITY_THRESHOLD);
        this.maxPerCritEdge = options.getDouble(O_MAX_PER_CRIT_EDGE);

        this.tradeOff = options.getDouble(O_TRADE_OFF);

        this.timingGraph = this.circuit.getTimingGraph();
    }

    @Override
    public void initializeData() {
        super.initializeData();

        this.timingGraph.setCriticalityExponent(this.criticalityExponent);
        this.timingGraph.calculateCriticalities(true);

        this.criticalityCalculator = new CriticalityCalculator(
                this.circuit,
                this.netBlocks,
                this.timingNets);
    }

    @Override
    protected boolean isTimingDriven() {
        return true;
    }

    @Override
    protected void initializeIteration(int iteration) {

    	this.startTimer(T_UPDATE_CRIT_CON);
    	this.updateCriticalConnections();
    	this.stopTimer(T_UPDATE_CRIT_CON);

        if(iteration > 0) {
            this.anchorWeight *= this.anchorWeightMultiplier;
            this.legalizer.multiplySettings();
        }
    }

    private void updateCriticalConnections() {

        for(TimingNet net : this.timingNets) {
        	if(net.include()) {
                for(TimingNetBlock sink : net.sinks) {
                	sink.updateCriticality();
                }
        	}
        }

        List<Double> criticalities = new ArrayList<>();
        for(TimingNet net : this.timingNets) {
        	if(net.include()) {
                NetBlock source = net.source;
                for(TimingNetBlock sink : net.sinks) {
                	if(sink.criticality > this.criticalityThreshold) {
                		if(source.blockIndex != sink.blockIndex) {
                			criticalities.add(sink.criticality);
                		}
                	}
                }
        	}
        }
        double minimumCriticality = this.criticalityThreshold;
        int maxNumCritConn = (int) Math.round(this.numRealConn * this.maxPerCritEdge / 100);
        if(criticalities.size() > maxNumCritConn){
        	Collections.sort(criticalities);
        	minimumCriticality = criticalities.get(criticalities.size() - 1 - maxNumCritConn);
        }

        this.criticalConnections.clear();
        for(TimingNet net : this.timingNets) {
        	if(net.include()) {
                NetBlock source = net.source;
                for(TimingNetBlock sink : net.sinks) {
                	if(sink.criticality > minimumCriticality) {
                		if(source.blockIndex != sink.blockIndex) {
                			CritConn c = new CritConn(source.blockIndex, sink.blockIndex, source.offset, sink.offset, (float)(this.tradeOff * sink.criticality));
                			this.criticalConnections.add(c);
                		}
                	}
                }
        	}
        }

        this.legalizer.updateCriticalConnections(this.criticalConnections);
    }

    @Override
    protected void calculateTimingCost() {
        this.timingCost = this.criticalityCalculator.calculate(this.legalX, this.legalY);
    }

    @Override
    public String getName() {
        return "Timing driven analytical placer";
    }
}
