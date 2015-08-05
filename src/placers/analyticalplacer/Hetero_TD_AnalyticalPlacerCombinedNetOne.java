package placers.analyticalplacer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import placers.Rplace;

import timinganalysis.TimingGraph;
import architecture.HeterogeneousArchitecture;
import circuit.Block;
import circuit.Clb;
import circuit.HardBlock;
import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public class Hetero_TD_AnalyticalPlacerCombinedNetOne
{

	private final double ALPHA = 0.3;
	private final double TG_CRITICALITY_EXPONENT = 1.0;
	private final double AVG_CRITICALITY_THRESHOLD = 0.5;
	
	private HeterogeneousArchitecture architecture;
	private PackedCircuit circuit;
	private Map<Block, Integer> indexMap; // Maps a block (CLB or hardblock) to its integer index
	private int[] typeStartIndices;
	private String[] typeNames;
	private double[] linearX;
	private double[] linearY;
	private Hetero_TD_LegalizerOne legalizer;
	private TimingGraph timingGraph;
	
	public Hetero_TD_AnalyticalPlacerCombinedNetOne(HeterogeneousArchitecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		Rplace.placeCLBsandFixedIOs(circuit, architecture, new Random(1));
		initializeDataStructures();
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.setCriticalityExponent(TG_CRITICALITY_EXPONENT);
		this.legalizer = new Hetero_TD_LegalizerOne(architecture, typeStartIndices, typeNames, linearX.length, timingGraph, circuit);
	}
	
	public void place()
	{
		System.out.println("Placement algorithm: Hetero_TD_AnalyticalPlacerNewNetFour");
		
		int solveMode = 0; //0 = solve all, 1 = solve CLBs only, 2 = solve hb1 type only, 3 = solve hb2 type only,...

		timingGraph.buildTimingGraph();
		timingGraph.mapTopLevelPinsToTimingGraph(circuit);
		timingGraph.mapAnalyticalPlacerIndicesToEdges(indexMap, circuit);
		timingGraph.mapNetsToEdges(circuit);
		
		//Initial linear solves, should normally be done 5-7 times		
		for(int i = 0; i < 7; i++)
		{
			solveLinear(true, solveMode, 0.0);
		}
		
		//Initial legalization
		legalizer.legalize(linearX, linearY, indexMap, solveMode);
		
		double pseudoWeightFactor = 0.0;
		for(int i = 0; i < 30; i++)
		{
			solveMode = (solveMode + 1) % (typeNames.length + 1);
			if(solveMode <= 1)
			{
				pseudoWeightFactor += ALPHA;
			}
			solveLinear(false, solveMode, pseudoWeightFactor);
			double costLinear = calculateTotalCost(linearX, linearY);
			legalizer.legalize(linearX, linearY, indexMap, solveMode);
			double costLegal = legalizer.calculateTotalBBCost(legalizer.getBestLegalX(), legalizer.getBestLegalY(), indexMap);
			if(costLinear / costLegal > 0.85)
			{
				break;
			}
		}
	}
	
	private void solveLinear(boolean firstSolve, int solveMode, double pseudoWeightFactor)
	{
		
	}
	
	private void initializeDataStructures()
	{
		int nbClbs = circuit.clbs.size();
		int nbHardBlockTypes = circuit.getHardBlocks().size();
		int nbHardBlocks = 0;
		for (Vector<HardBlock> hbVector : circuit.getHardBlocks())
		{
			nbHardBlocks += hbVector.size();
		}
		int dimensions = nbClbs + nbHardBlocks;
		indexMap = new HashMap<>();
		typeStartIndices = new int[nbHardBlockTypes + 1];
		typeNames = new String[nbHardBlockTypes + 1];
		linearX = new double[dimensions];
		linearY = new double[dimensions];
		int maximalX = architecture.getWidth();
		int maximalY = architecture.getHeight();
		Random random = new Random();
		typeStartIndices[0] = 0;
		typeNames[0] = "CLB";
		int index = 0;
		for(Clb clb: circuit.clbs.values())
		{
			indexMap.put(clb, index);
			linearX[index] = 1 + (maximalX - 1) * random.nextDouble();
			linearY[index] = 1 + (maximalY - 1) * random.nextDouble();
			index++;
		}
		int hardBlockTypeIndex = 0;
		for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
		{
			typeStartIndices[hardBlockTypeIndex + 1] = index;
			typeNames[hardBlockTypeIndex + 1] = hbVector.get(0).getTypeName();
			for(HardBlock hb: hbVector)
			{
				indexMap.put(hb, index);
				linearX[index] = 1 + (maximalX - 1) * random.nextDouble();
				linearY[index] = 1 + (maximalY - 1) * random.nextDouble();
				index++;
			}
			hardBlockTypeIndex++;
		}
	}
	
}
