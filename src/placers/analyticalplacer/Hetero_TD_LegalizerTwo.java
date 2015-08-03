package placers.analyticalplacer;

import timinganalysis.TimingGraph;
import circuit.PackedCircuit;
import architecture.HeterogeneousArchitecture;

public class Hetero_TD_LegalizerTwo
{

	private static final double UTILIZATION_FACTOR = 0.9;
	private static final double TRADE_OFF_FACTOR = 0.5;
	
	private int[] bestLegalX;
	private int[] bestLegalY;
	private int[] partialLegalX;
	private int[] partialLegalY;
	private HeterogeneousArchitecture architecture;
	private int[] typeStartIndices;
	private String[] typeNames;
	private boolean lastMaxUtilizationSmallerThanOne;
	private boolean firstLegalizationDone;
	private TimingGraph timingGraph;
	private PackedCircuit circuit;
	private boolean firstUpdateLegalDone;
	private double previousMaxDelay;
	private double previousBBCost;
	
	public Hetero_TD_LegalizerTwo(HeterogeneousArchitecture architecture, int[] typeStartIndices, String[] typeNames, 
													int nbMovableBlocks, TimingGraph timingGraph, PackedCircuit circuit)
	{
		this.bestLegalX = new int[nbMovableBlocks];
		this.bestLegalY = new int[nbMovableBlocks];
		this.partialLegalX = new int[nbMovableBlocks];
		this.partialLegalY = new int[nbMovableBlocks];
		this.architecture = architecture;
		this.typeStartIndices = typeStartIndices;
		this.typeNames = typeNames;
		this.lastMaxUtilizationSmallerThanOne = false;
		this.firstLegalizationDone = false;
		this.timingGraph = timingGraph;
		this.circuit = circuit;
		this.firstUpdateLegalDone = false;
		this.previousMaxDelay = Double.MAX_VALUE;
		this.previousBBCost = Double.MAX_VALUE;
	}
	
}
