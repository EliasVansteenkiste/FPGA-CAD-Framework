package placers.analyticalplacer;

import java.util.Map;

import timing_graph.TimingGraph;
import timing_graph.TimingGraphEntry;

import architecture.BlockType;
import architecture.BlockType.BlockCategory;
import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;

import mathtools.Crs;


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
		this.timingGraph.setCriticalityExponent(this.criticalityExponent);
	}
	
	@Override
	protected CostCalculator createCostCalculator() {
		this.tradeOff = this.parseDoubleOption("trade_off");
		
		this.timingGraph = new TimingGraph(this.circuit);
		this.timingGraph.build();
		
		return new TD_CostCalculator(this.circuit, this.blockIndexes, this.timingGraph, this.tradeOff);
	}
	
	@Override
	protected void initializePlacement() {
		this.timingGraph.build();
	}
	
	@Override
	protected void addExtraConnections(BlockType blockType, int startIndex, boolean firstSolve) {
		if(!firstSolve) {
			for(TimingGraphEntry entry : this.timingGraph) {
				double criticality = entry.getCriticality();
				
				if(criticality > this.maxCriticalityThreshold) {
					GlobalBlock source = entry.getSource();
					GlobalBlock sink = entry.getSink();
					
					double weightMultiplier = ((double) 2) / entry.getNetSize() * criticality;
					this.processTimingDrivenConnection(blockType, source, sink, startIndex, weightMultiplier);
				}
			}
		}
	}
	
	
	private void processTimingDrivenConnection(BlockType blockType, GlobalBlock source, GlobalBlock sink, int startIndex, double weightMultiplier) {
		boolean sourceIsFixed = isFixed(source, blockType);
		boolean sinkIsFixed = isFixed(sink, blockType);
		
		// If at least one of the two blocks is movable
		if(!sourceIsFixed || !sinkIsFixed) {
			int sourceIndex = -1, sinkIndex = -1;
			double sourceX, sourceY, sinkX, sinkY;
			
			if(sourceIsFixed) {
				if(source.getCategory() != BlockCategory.IO) {
					sourceX = source.getX();
					sourceY = source.getY();
				
				} else {
					// Don't update sourceIndex
					int index = this.blockIndexes.get(source);
					sourceX = this.legalizer.getBestLegalX()[index];
					sourceY = this.legalizer.getBestLegalY()[index];
				}
			
			} else {
				sourceIndex = this.blockIndexes.get(source);
				sourceX = this.linearX[sourceIndex];
				sourceY = this.linearY[sourceIndex];
			}
			
			
			if(sinkIsFixed) {
				if(sink.getCategory() == BlockCategory.IO) {
					sinkX = sink.getX();
					sinkY = sink.getY();
				
				} else {
					// Don't update sinkIndex
					int index = this.blockIndexes.get(sink);
					sinkX = this.legalizer.getBestLegalX()[index];
					sinkY = this.legalizer.getBestLegalY()[index];
				}
			
			} else {
				sinkIndex = this.blockIndexes.get(sink);
				sinkX = this.linearX[sinkIndex];
				sinkY = this.linearY[sinkIndex];
			} 
			
			
			this.addSourceSinkConnection(
					sourceIndex - startIndex, sourceX,
					sinkIndex - startIndex, sinkX,
					weightMultiplier, this.xMatrix, this.xVector);
			
			this.addSourceSinkConnection(
					sourceIndex - startIndex, sourceY,
					sinkIndex - startIndex, sinkY,
					weightMultiplier, this.yMatrix, this.yVector);
		}
	}	

	
	private void addSourceSinkConnection(
			int index1, double coordinate1,
			int index2, double coordinate2,
			double weightMultiplier, Crs matrix, double[] vector) {
		
		double delta = Math.max(Math.abs(coordinate2 - coordinate1), 0.001);
		double weight = weightMultiplier / delta;
		
		// Only second block is free
		if(index1 < 0) {
			matrix.setElement(index2, index2, matrix.getElement(index2, index2) + weight);
			vector[index2] += weight * coordinate1;
		
		// Only first block is free
		} else if(index2 < 0) {
			matrix.setElement(index1, index1, matrix.getElement(index1, index1) + weight);
			vector[index1] += weight * coordinate2;
		
		// Both blocks are free
		} else {
			matrix.setElement(index1, index1, matrix.getElement(index1, index1) + weight);
			matrix.setElement(index1, index2, matrix.getElement(index1, index2) - weight);
			matrix.setElement(index2, index1, matrix.getElement(index2, index1) - weight);
			matrix.setElement(index2, index2, matrix.getElement(index2, index2) + weight);
		}
	}
}
