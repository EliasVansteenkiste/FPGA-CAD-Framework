package placers.analyticalplacer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mathtools.Crs;

import placers.Rplace;

import timinganalysis.TimingGraph;

import architecture.FourLutSanitized;
import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;

/**
 * TODO To watch out for: pseudoweights and Cost calculation: criticalities
 */
public class TD_AnalyticalPlacerOne
{

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
	private Legalizer legalizer;
	private TimingGraph timingGraph;
	
	private final double ALPHA = 0.3;
	
	public TD_AnalyticalPlacerOne(FourLutSanitized architecture, PackedCircuit circuit, int legalizer, TimingGraph timingGraph)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.timingGraph = timingGraph;
		this.minimalX = 1;
		this.maximalX = architecture.width;
		this.minimalY = 1;
		this.maximalY = architecture.height;
		switch(legalizer)
		{
			case 1:
				this.legalizer = new LegalizerOne(minimalX, maximalX, minimalY, maximalY, circuit.clbs.values().size());
				break;
			case 2:
				this.legalizer = new LegalizerTwo(minimalX, maximalX, minimalY, maximalY, circuit.clbs.values().size());
				break;
			default:
				this.legalizer = new LegalizerThree(minimalX, maximalX, minimalY, maximalY, circuit.clbs.values().size());
				break;
		}
	}
	
	public void place()
	{
		Rplace.placeCLBsandFixedIOs(circuit, architecture, new Random(1));
		initializeDataStructures();
		
		//Initial linear solves, should normally be done 5-7 times		
		for(int i = 0; i < 7; i++)
		{
			solveLinear(true, 0.0);
		}
		
		//Initial legalization
		legalizer.legalize(linearX, linearY, circuit.getNets().values(), indexMap);
		
		for(int i = 0; i < 30; i++)
		{
			solveLinear(false, (i+1)*ALPHA);
			legalizer.legalize(linearX, linearY, circuit.getNets().values(), indexMap);
		}
		
		updateCircuit();
		
		double cost = legalizer.calculateBestLegalCost(circuit.getNets().values(), indexMap);
		System.out.println("COST BEFORE REFINEMENT = " + cost);
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
				if(deltaX < 0.005)
				{
					deltaX = 0.005;
				}
				double pseudoWeightX = 2*pseudoWeightFactor*(1/deltaX);
				xMatrix.setElement(i, i, xMatrix.getElement(i, i) + pseudoWeightX);
				xVector[i] += pseudoWeightX * anchorPointsX[i];
				double deltaY = Math.abs(anchorPointsY[i] - linearY[i]);
				if(deltaY < 0.005)
				{
					deltaY = 0.005;
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
				sourceX = linearX[indexMap.get(sourceOwner)];
				sourceY = linearY[indexMap.get(sourceOwner)];
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
					sinkX = linearX[indexMap.get(sinkOwner)];
					sinkY = linearY[indexMap.get(sinkOwner)];
				}
				else
				{
					isSinkFixed = true;
					sinkX = sinkOwner.getSite().x;
					sinkY = sinkOwner.getSite().y;
				}
				if(!(isSourceFixed && isSinkFixed)) //Not both fixed
				{
					double weightX;
					double weightY;
					if(firstSolve) //Don't include timing factor
					{
						weightX = (double)2/((nbPins - 1)*Math.abs(sinkX - sourceX));
						weightY = (double)2/((nbPins - 1)*Math.abs(sinkY - sourceY));
					}
					else //Include timing factor
					{
						jkgh
					}
					
					if(isSourceFixed) //Source is fixed, sink is free
					{
						
					}
					else //Source is free
					{
						if(isSinkFixed) //Sink is fixed, source is free
						{
							
						}
						else //Both are free
						{
							
						}
					}
				}
			}
		}
	}
	
	private void updateCircuit()
	{
		int[] bestLegalX = new int[linearX.length];
		int[] bestLegalY = new int[linearY.length];
		
		legalizer.getBestLegal(bestLegalX, bestLegalY);
		
		//Clear all previous locations
		for(int i = minimalX; i <= maximalX; i++)
		{
			for(int j = minimalY; j <= maximalY; j++)
			{
				if(architecture.getSite(i, j, 0).block != null)
				{
					architecture.getSite(i, j, 0).block.setSite(null);
				}
				architecture.getSite(i, j, 0).block = null;
			}
		}
		
		//Update locations
		for(Clb clb:circuit.clbs.values())
		{
			int index = indexMap.get(clb);
			Site site = architecture.getSite(bestLegalX[index], bestLegalY[index], 0);
			site.block = clb;
			clb.setSite(site);
		}
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
