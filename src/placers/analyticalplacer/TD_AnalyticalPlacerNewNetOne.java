package placers.analyticalplacer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.Rplace;

import timinganalysis.TimingGraph;

import architecture.FourLutSanitized;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;
import circuit.PrePackedCircuit;

/**
 * TODO To watch out for: pseudoweights and Cost calculation: criticalities
 */
public class TD_AnalyticalPlacerNewNetOne
{

	private final double ALPHA = 0.3;
	
	private FourLutSanitized architecture;
	private PackedCircuit circuit;
	private Map<Clb,Integer> indexMap; //Maps an index to a Clb
	private int minimalX;
	private int maximalX;
	private int minimalY;
	private int maximalY;
	private double[] linearX;
	private double[] linearY;
	private int[] anchorPointsX;
	private int[] anchorPointsY;
	private TD_LegalizerOne legalizer;
	private TimingGraph timingGraph;
	
	public TD_AnalyticalPlacerNewNetOne(FourLutSanitized architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.minimalX = 1;
		this.maximalX = architecture.width;
		this.minimalY = 1;
		this.maximalY = architecture.height;
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.setCriticalityExponent(1.0);
		this.legalizer = new TD_LegalizerOne(minimalX, maximalX, minimalY, maximalY, circuit.clbs.values().size(), timingGraph, 
												circuit, architecture);
	}
	
	public void place()
	{
		Rplace.placeCLBsandFixedIOs(circuit, architecture, new Random(1));
		initializeDataStructures();
		
		timingGraph.buildTimingGraph();
		timingGraph.mapClbsToTimingGraph(circuit);
		
		//Initial linear solves, should normally be done 5-7 times		
		for(int i = 0; i < 15; i++)
		{
			solveLinear(true, 0.0);
		}
		
		//Initial legalization
		legalizer.legalize(linearX, linearY, indexMap);		
		
//		for(int i = 0; i < linearX.length; i++)
//		{
//			System.out.printf("%d: (%.2f-%.2f)\n", i, linearX[i], linearY[i]);
//		}
				
		for(int i = 0; i < 30; i++)
		{
			solveLinear(false, (i+1)*ALPHA);
			legalizer.legalize(linearX, linearY, indexMap);
		}
	}
	
	/*
	 * If it are initial solves: 
	 * 	- Don't add pseudoConnections
	 *  - Don't add timing factor in weights
	 */
	public void solveLinear(boolean firstSolve, double pseudoWeightFactor)
	{
		int dimension = linearX.length;
		Crs xMatrix = new Crs(dimension);
		double[] xVector = new double[dimension];
		Crs yMatrix = new Crs(dimension);
		double[] yVector = new double[dimension];
		
		//Add pseudo connections
		if(!firstSolve)
		{
			//Process pseudonets
			legalizer.getAnchorPoints(anchorPointsX, anchorPointsY);
			
			for(int i = 0; i < dimension; i++)
			{
				double deltaX = Math.abs(anchorPointsX[i] - linearX[i]);
				if(deltaX < 0.001)
				{
					deltaX = 0.001;
				}
				double pseudoWeightX = 2*pseudoWeightFactor*(1/deltaX);
				xMatrix.setElement(i, i, xMatrix.getElement(i, i) + pseudoWeightX);
				xVector[i] += pseudoWeightX * anchorPointsX[i];
				double deltaY = Math.abs(anchorPointsY[i] - linearY[i]);
				if(deltaY < 0.001)
				{
					deltaY = 0.001;
				}
				double pseudoWeightY = 2*pseudoWeightFactor*(1/deltaY);
				yMatrix.setElement(i, i, yMatrix.getElement(i, i) + pseudoWeightY);
				yVector[i] += pseudoWeightY*anchorPointsY[i];
			}
		}
		
		//Build the linear systems (x and y are solved separately)
		for(Net net:circuit.getNets().values())
		{
			Block sourceOwner = net.source.owner;
			int nbPins = 1 + net.sinks.size();
			boolean isSourceFixed;
			double sourceX;
			double sourceY;
			if(sourceOwner.type == BlockType.CLB)
			{
				isSourceFixed = false;
				int sourceIndex = indexMap.get(sourceOwner);
				sourceX = linearX[sourceIndex];
				sourceY = linearY[sourceIndex];
			}
			else
			{
				isSourceFixed = true;
				sourceX = sourceOwner.getSite().x;
				sourceY = sourceOwner.getSite().y;
			}
			for(Pin sinkPin: net.sinks)
			{
				Block sinkOwner = sinkPin.owner;
				boolean isSinkFixed;
				double sinkX;
				double sinkY;
				if(sinkOwner.type == BlockType.CLB)
				{
					isSinkFixed = false;
					int sinkIndex = indexMap.get(sinkOwner);
					sinkX = linearX[sinkIndex];
					sinkY = linearY[sinkIndex];
				}
				else
				{
					isSinkFixed = true;
					sinkX = sinkOwner.getSite().x;
					sinkY = sinkOwner.getSite().y;
				}
				if(!(isSourceFixed && isSinkFixed)) //Not both fixed
				{
					double deltaX = Math.abs(sinkX - sourceX);
					if(deltaX < 0.001)
					{
						deltaX = 0.001;
					}
					double weightX = (double)2/((nbPins - 1)*deltaX);
					double deltaY = Math.abs(sinkY - sourceY);
					if(deltaY < 0.001)
					{
						deltaY = 0.001;
					}
					double weightY = (double)2/((nbPins - 1)*deltaY);
					if(!firstSolve) //Include timing factor
					{
						//Search for connection in timing graph
						double criticality = timingGraph.getConnectionCriticalityWithExponent(net.source, sinkPin);
						weightX *= criticality;
						weightY *= criticality;
					}
					
					if(isSourceFixed) //Source is fixed, sink is free
					{
						int sinkIndex = indexMap.get(sinkOwner);
						xMatrix.setElement(sinkIndex, sinkIndex, xMatrix.getElement(sinkIndex, sinkIndex) + weightX);
						xVector[sinkIndex] += weightX * sourceX;
						yMatrix.setElement(sinkIndex, sinkIndex, yMatrix.getElement(sinkIndex, sinkIndex) + weightY);
						yVector[sinkIndex] += weightY * sourceY;
					}
					else //Source is free
					{
						if(isSinkFixed) //Sink is fixed, source is free
						{
							int sourceIndex = indexMap.get(sourceOwner);
							xMatrix.setElement(sourceIndex, sourceIndex, xMatrix.getElement(sourceIndex, sourceIndex) + weightX);
							xVector[sourceIndex] += weightX * sinkX;
							yMatrix.setElement(sourceIndex, sourceIndex, yMatrix.getElement(sourceIndex, sourceIndex) + weightY);
							yVector[sourceIndex] += weightY * sinkY;
						}
						else //Both are free
						{
							int sourceIndex = indexMap.get(sourceOwner);
							int sinkIndex = indexMap.get(sinkOwner);
							xMatrix.setElement(sourceIndex, sourceIndex, xMatrix.getElement(sourceIndex, sourceIndex) + weightX);
							xMatrix.setElement(sourceIndex, sinkIndex, xMatrix.getElement(sourceIndex, sinkIndex) - weightX);
							xMatrix.setElement(sinkIndex, sourceIndex, xMatrix.getElement(sinkIndex, sourceIndex) - weightX);
							xMatrix.setElement(sinkIndex, sinkIndex, xMatrix.getElement(sinkIndex, sinkIndex) + weightX);
							yMatrix.setElement(sourceIndex, sourceIndex, yMatrix.getElement(sourceIndex, sourceIndex) + weightY);
							yMatrix.setElement(sourceIndex, sinkIndex, yMatrix.getElement(sourceIndex, sinkIndex) - weightY);
							yMatrix.setElement(sinkIndex, sourceIndex, yMatrix.getElement(sinkIndex, sourceIndex) - weightY);
							yMatrix.setElement(sinkIndex, sinkIndex, yMatrix.getElement(sinkIndex, sinkIndex) + weightY);
						}
					}
				}
			}
		}
		
		if(!xMatrix.isSymmetrical())
		{
			System.err.println("ERROR: X-Matrix is assymmetrical: there must be a bug in the code!");
		}
		if(!yMatrix.isSymmetrical())
		{
			System.err.println("ERROR: Y-Matrix is assymmetrical: there must be a bug in the code!");
		}
		
		double epselon = 0.0001;
		//Solve x problem
		CGSolver xSolver = new CGSolver(xMatrix, xVector);
		double[] xSolution = xSolver.solve(epselon);
		//Solve y problem
		CGSolver ySolver = new CGSolver(yMatrix, yVector);
		double[] ySolution = ySolver.solve(epselon);
		
		linearX = xSolution;
		linearY = ySolution;
	}
	
	private void initializeDataStructures()
	{
		int dimensions = circuit.clbs.values().size();
		indexMap = new HashMap<>();;
		linearX = new double[dimensions];
		linearY = new double[dimensions];
		anchorPointsX = new int[dimensions];
		anchorPointsY = new int[dimensions];
		int index = 0;
		Random random = new Random();
		for(Clb clb:circuit.clbs.values())
		{
			indexMap.put(clb, index);
			linearX[index] = minimalX + (maximalX - minimalX)*random.nextDouble();
			linearY[index] = minimalY + (maximalY - minimalY)*random.nextDouble();
			index++;
		}
	}
	
}
