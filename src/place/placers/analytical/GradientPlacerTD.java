package place.placers.analytical;

import place.circuit.Circuit;
import place.circuit.timing.TimingGraph;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.visual.PlacementVisualizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GradientPlacerTD extends GradientPlacer {

    private static final String
        O_CRITICALITY_EXPONENT = "criticality exponent",
        O_CRITICALITY_THRESHOLD = "criticality threshold",
        O_RECALCULATE_CRITICALITIES = "recalculate criticalities",
        O_RECALCULATE_PRIORITY = "recalculate priority",
        O_TRADE_OFF = "trade off",
        O_ALWAYS_UPDATE = "always update legal solution";

    public static void initOptions(Options options) {
        GradientPlacer.initOptions(options);

        options.add(
                O_CRITICALITY_EXPONENT,
                "criticality exponent of connections",
                new Double(4));

        options.add(
                O_CRITICALITY_THRESHOLD,
                "minimal criticality for adding TD constraints",
                new Double(0.8));

        options.add(
                O_RECALCULATE_CRITICALITIES,
                "frequency of criticalities recalculation; 0 = never, 1 = every iteration",
                new Double(1));

        options.add(
                O_RECALCULATE_PRIORITY,
                "controls the spreading of recalculations; 1 = evenly spread, higher = less recalculations near the end",
                new Double(1));

        options.add(
                O_TRADE_OFF,
                "0 = purely wirelength driven, higher = more timing driven",
                new Double(10));
        
        options.add(
                O_ALWAYS_UPDATE,
                "always update the legal solution, even if it leads to worse quality than the best legal solution",
                new Boolean(true));
    }


    private static String
        T_UPDATE_CRIT_CON = "update critical connections";
    
    private List<CriticalConnection> criticalConnections = new ArrayList<CriticalConnection>();

    private double criticalityExponent, criticalityThreshold;
    private TimingGraph timingGraph;
    private CriticalityCalculator criticalityCalculator;

    private double recalculateCriticalities, recalculatePriority;
    private boolean[] recalculate;

    public GradientPlacerTD(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.criticalityExponent = options.getDouble(O_CRITICALITY_EXPONENT);
        this.criticalityThreshold = options.getDouble(O_CRITICALITY_THRESHOLD);

        this.recalculateCriticalities = options.getDouble(O_RECALCULATE_CRITICALITIES);
        this.recalculatePriority = options.getDouble(O_RECALCULATE_PRIORITY);
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

        // Determine the iterations in which the connection
        // criticalities should be recalculated
        this.recalculate = new boolean[this.numIterations];
        double nextFunctionValue = 0;

        StringBuilder recalculationsString = new StringBuilder();
        for(int i = 0; i < this.numIterations; i++) {
            double functionValue = Math.pow((1. * i) / this.numIterations, 1. / this.recalculatePriority);
            if(functionValue >= nextFunctionValue) {
                this.recalculate[i] = true;
                nextFunctionValue += 1 / (this.recalculateCriticalities * this.numIterations);
                recalculationsString.append("|");
            } else {
                this.recalculate[i] = false;
                recalculationsString.append(".");
            }
        }

        // Print these iterations
        this.logger.println("Criticalities recalculations:");
        this.logger.println(recalculationsString.toString());
        this.logger.println();
    }

    @Override
    protected boolean isTimingDriven() {
        return true;
    }

    @Override
    protected void initializeIteration(int iteration) {
    	if(this.recalculate[iteration]) {
    		this.startTimer(T_UPDATE_CRIT_CON);
    		this.updateCriticalConnections();
    		this.stopTimer(T_UPDATE_CRIT_CON);
        }
        if(iteration > 0) {
            this.anchorWeight += this.anchorWeightStep;
        }
    }
    private void updateCriticalConnections() {
    	this.criticalityCalculator.calculate(this.legalizer.getLegalX(), this.legalizer.getLegalY());

        this.criticalConnections.clear();

        for(TimingNet net : this.timingNets) {
            NetBlock source = net.source;

            for(TimingNetBlock sink : net.sinks) {
                double criticality = sink.timingEdge.getCriticality();
                if(criticality > this.criticalityThreshold) {

                    if(source.blockIndex != sink.blockIndex) {
                    	CriticalConnection c = new CriticalConnection(source.blockIndex, sink.blockIndex, sink.offset - source.offset, (float)(this.tradeOff * criticality));
                    	this.criticalConnections.add(c);
                    }
                }
            }
        }
    }
    public List<CriticalConnection> getCriticalConnections(){
    	return this.criticalConnections;
    }

    @Override
    protected void processNets() {
        // Process all nets wirelength driven
        super.processNets();
        
        // Process the most critical source-sink connections
        for(CriticalConnection critConn:this.criticalConnections){
        	this.solver.processConnection(critConn.sourceIndex, critConn.sinkIndex, critConn.offset, critConn.weight);
        }
    }


    @Override
    protected void updateLegalIfNeeded(int iteration) {
        int[] newLegalX = this.legalizer.getLegalX();
        int[] newLegalY = this.legalizer.getLegalY();

        //Always update legal cost
        this.updateLegal(newLegalX, newLegalY);
    }

    @Override
    public String getName() {
        return "Timing driven gradient placer";
    }
    
    public class CriticalConnection{
    	final int sourceIndex, sinkIndex;
    	final float offset, weight;
    	
    	CriticalConnection(int sourceIndex, int sinkIndex, float offset, float weight){
    		this.sourceIndex = sourceIndex;
    		this.sinkIndex = sinkIndex;
    		this.offset = offset;
    		this.weight = weight;
    	}
    }
}
