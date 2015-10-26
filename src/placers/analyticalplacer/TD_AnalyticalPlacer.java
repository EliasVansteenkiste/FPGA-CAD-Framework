package placers.analyticalplacer;

import java.util.Map;

import timing_graph.TimingGraph;
import timing_graph.TimingGraphEntry;

import architecture.BlockType;
import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;


public class TD_AnalyticalPlacer extends AnalyticalPlacer {
	
	private double criticalityExponent;
	private double maxCriticalityThreshold;
	private TimingGraph timingGraph;
	private double tradeOff;
	
	static {
		// The criticality exponent of connections
		defaultOptions.put("criticality_exponent", "8");
		
		// Extra constraints will be added to the linear system if a connection criticality is above this threshold
		defaultOptions.put("max_criticality_threshold", "0.8");
		
		// Tradeoff between WL and TD, this is only used to calculate the cost of the circuit
		// 0 = WLD
		defaultOptions.put("trade_off", "0.5");
	}
	
	public TD_AnalyticalPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
		
		// Timing driven options
		this.maxCriticalityThreshold = this.parseDoubleOption("max_criticality_threshold");
		
		this.criticalityExponent = this.parseDoubleOption("criticality_exponent");
        this.tradeOff = this.parseDoubleOption("trade_off");
	}
    
    @Override
    public void initializeData() {
        super.initializeData();
        
        this.timingGraph = this.circuit.getTimingGraph();
    }
    
    @Override
    protected CostCalculator createCostCalculator() {
        return new TD_CostCalculator(this.nets, this.timingGraph, this.tradeOff);
    }
	
    @Override
    protected void initializePlacementIteration() {
        this.timingGraph.recalculateAllSlackCriticalities();
    }
	
	
	
	protected void processNetsSourceSink(BlockType blockType, int startIndex) {
		for(TimingGraphEntry entry : this.timingGraph) {
			double criticality = entry.getCriticality();
			
			if(criticality > this.maxCriticalityThreshold) {
				GlobalBlock source = entry.getSource();
				GlobalBlock sink = entry.getSink();
				double weightMultiplier = ((double) 2) / entry.getNumSinks() * criticality;
				
				//this.processTimingDrivenConnection(blockType, source, sink, startIndex, weightMultiplier);
			}
		}
	}
	
	
	/*private void processTimingDrivenConnection(BlockType blockType, GlobalBlock source, GlobalBlock sink, int startIndex, double weightMultiplier) {
		boolean sourceFixed = isFixed(source, blockType);
		boolean sinkFixed = isFixed(sink, blockType);
		
		// If at least one of the two blocks is movable
		if(!sourceFixed || !sinkFixed) {
			int sourceIndex = -1, sinkIndex = -1;
			double sourceX, sourceY, sinkX, sinkY;
			
			if(sourceFixed) {
				if(source.getCategory() == BlockCategory.IO) {
					sourceX = source.getX();
					sourceY = source.getY();
				
				} else {
					// Don't update sourceIndex
					sourceIndex = this.blockIndexes.get(source);
					sourceX = this.legalizer.getBestLegalX()[sourceIndex];
					sourceY = this.legalizer.getBestLegalY()[sourceIndex];
				}
			
			} else {
				sourceIndex = this.blockIndexes.get(source);
				sourceX = this.linearX[sourceIndex];
				sourceY = this.linearY[sourceIndex];
			}
			
			
			if(sinkFixed) {
				if(sink.getCategory() == BlockCategory.IO) {
					sinkX = sink.getX();
					sinkY = sink.getY();
				
				} else {
					// Don't update sinkIndex
					sinkIndex = this.blockIndexes.get(sink);
					sinkX = this.legalizer.getBestLegalX()[sinkIndex];
					sinkY = this.legalizer.getBestLegalY()[sinkIndex];
				}
			
			} else {
				sinkIndex = this.blockIndexes.get(sink);
				sinkX = this.linearX[sinkIndex];
				sinkY = this.linearY[sinkIndex];
			} 
			
			
			this.addConnection(
					sourceX, sourceFixed, sourceIndex - startIndex,
					sinkX, sinkFixed, sinkIndex - startIndex,
					weightMultiplier, this.xMatrix, this.xVector);
			
			this.addConnection(
					sourceY, sourceFixed, sourceIndex - startIndex,
					sinkY, sinkFixed, sinkIndex - startIndex,
					weightMultiplier, this.yMatrix, this.yVector);
		}
	}*/
}
