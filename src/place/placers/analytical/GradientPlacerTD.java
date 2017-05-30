package place.placers.analytical;

import place.circuit.Circuit;
import place.circuit.timing.TimingGraph;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GradientPlacerTD extends GradientPlacer {

    private static final String
        O_CRITICALITY_EXPONENT = "criticality exponent",
        O_CRITICALITY_THRESHOLD = "criticality threshold",
        O_MAX_PER_CRIT_EDGE = "max per crit edge",
        O_TRADE_OFF = "trade off";

    public static void initOptions(Options options) {
        GradientPlacer.initOptions(options);

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
                "0 = purely wirelength driven, higher = more timing driven",
                new Double(30));
    }


    private static String
        T_UPDATE_CRIT_CON = "update critical connections";
    
    private List<CritConn> criticalConnections = new ArrayList<>();
    private List<Double> criticalities = new ArrayList<>();

    private double criticalityExponent, criticalityThreshold, maxPerCritEdge;
    private TimingGraph timingGraph;
    
    private CriticalityCalculator criticalityCalculator;

    public GradientPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
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
            this.anchorWeight += this.anchorWeightStep;
        }
        this.learningRate *= this.learningRateMultiplier;
    }

    private void updateCriticalConnections() {
    	
        for(TimingNet net : this.timingNets) {
            for(TimingNetBlock sink : net.sinks) {
            	sink.updateCriticality();
            }
        }

        this.criticalities.clear();
        for(TimingNet net : this.timingNets) {
            NetBlock source = net.source;
            for(TimingNetBlock sink : net.sinks) {
            	if(sink.criticality > this.criticalityThreshold) {
            		if(source.blockIndex != sink.blockIndex) {
            			this.criticalities.add(sink.criticality);
            		}
            	}
            }
        }

        double minimumCriticality = this.criticalityThreshold;
        int maxNumCritConn = (int) Math.round(this.numRealConn * this.maxPerCritEdge / 100);
        if(this.criticalities.size() > maxNumCritConn){
        	Collections.sort(this.criticalities);
        	minimumCriticality = this.criticalities.get(this.criticalities.size() - 1 - maxNumCritConn);
        }

        this.criticalConnections.clear();
        for(TimingNet net : this.timingNets) {
            NetBlock source = net.source;
            for(TimingNetBlock sink : net.sinks) {
            	if(sink.criticality > minimumCriticality) {
            		if(source.blockIndex != sink.blockIndex) {
            			CritConn c = new CritConn(source.blockIndex, sink.blockIndex, sink.offset - source.offset, (float)(this.tradeOff * sink.criticality));
            			this.criticalConnections.add(c);
            		}
            	}
            }
        }
    	
        this.legalizer.updateCriticalConnections(this.criticalConnections);
        
        this.critConn = this.criticalConnections.size();
    }

    @Override
    protected void processNets() {
        // Process all nets wirelength driven
        super.processNets();

        // Process the most critical source-sink connections
        for(CritConn critConn:this.criticalConnections) {
        	this.solver.processConnection(critConn.sourceIndex, critConn.sinkIndex, critConn.offset, critConn.weight);
        }
    }

    @Override
    protected void calculateTimingCost() {
        this.timingCost = this.criticalityCalculator.calculate(this.legalizer.getLegalX(), this.legalizer.getLegalY());
    }

    @Override
    public String getName() {
        return "Timing driven gradient placer";
    }

    public class CritConn{
    	final int sourceIndex, sinkIndex;
    	final float offset, weight;
    	
    	CritConn(int sourceIndex, int sinkIndex, float offset, float weight) {
    		this.sourceIndex = sourceIndex;
    		this.sinkIndex = sinkIndex;
    		this.offset = offset;
    		this.weight = weight;
    	}
    }
}