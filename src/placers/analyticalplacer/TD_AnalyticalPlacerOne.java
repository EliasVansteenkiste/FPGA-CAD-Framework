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
		
		double costLinear = calculateTotalCost(linearX, linearY);
		System.out.println("Linear cost before legalizing = " + costLinear);
		
		//Initial legalization
		legalizer.legalize(linearX, linearY, circuit.getNets().values(), indexMap);
		
		int nbIterations = 0;
		for(int i = 0; i < 30; i++)
		{
			nbIterations++;
			solveLinear(false, (i+1)*ALPHA);
			legalizer.legalize(linearX, linearY, circuit.getNets().values(), indexMap);
			costLinear = calculateTotalCost(linearX, linearY);
			double costLegal = legalizer.calculateBestLegalCost(circuit.getNets().values(), indexMap);
			if(costLinear / costLegal > 0.70)
			{
				break;
			}
		}
		
		System.out.println("Nb of iterations: " + nbIterations);
		
		updateCircuit();
		
		double cost = legalizer.calculateBestLegalCost(circuit.getNets().values(), indexMap);
		System.out.println("COST BEFORE REFINEMENT = " + cost);
	}
	
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
			int sourceX = sourceOwner.getSite().x;
			int sourceY = sourceOwner.getSite().y;
			boolean isSourceFixed;
			if(sourceOwner.type == BlockType.CLB)
			{
				isSourceFixed = false;
			}
			else
			{
				isSourceFixed = true;
			}
			for(Pin sinkPin: net.sinks)
			{
				Block sinkOwner = sinkPin.owner;
				boolean isSinkFixed;
				if(sinkOwner.type == BlockType.CLB)
				{
					isSinkFixed = false;
				}
				else
				{
					isSinkFixed = true;
				}
				if(!(isSourceFixed && isSinkFixed)) //Not both fixed
				{
					
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
	
	Need to add criticalities to cost
	private double calculateTotalCost(double[] xArray, double[] yArray)
	{
		double cost = 0.0;
		for(Net net:circuit.nets.values())
		{
			double minX;
			double maxX;
			double minY;
			double maxY;
			Block sourceBlock = net.source.owner;
			if(sourceBlock.type == BlockType.INPUT || sourceBlock.type == BlockType.OUTPUT)
			{
				minX = sourceBlock.getSite().x;
				maxX = sourceBlock.getSite().x;
				minY = sourceBlock.getSite().y;
				maxY = sourceBlock.getSite().y;
			}
			else
			{
				int index = indexMap.get((Clb)sourceBlock);
				minX = xArray[index];
				maxX = xArray[index];
				minY = yArray[index];
				maxY = yArray[index];
			}
			
			for(Pin pin:net.sinks)
			{
				Block sinkOwner = pin.owner;
				if(sinkOwner.type == BlockType.INPUT || sinkOwner.type == BlockType.OUTPUT)
				{
					Site sinkOwnerSite = sinkOwner.getSite();
					if(sinkOwnerSite.x < minX)
					{
						minX = sinkOwnerSite.x;
					}
					if(sinkOwnerSite.x > maxX)
					{
						maxX = sinkOwnerSite.x;
					}
					if(sinkOwnerSite.y < minY)
					{
						minY = sinkOwnerSite.y;
					}
					if(sinkOwnerSite.y > maxY)
					{
						maxY = sinkOwnerSite.y;
					}
				}
				else
				{
					int index = indexMap.get((Clb)sinkOwner);
					if(xArray[index] < minX)
					{
						minX = xArray[index];
					}
					if(xArray[index] > maxX)
					{
						maxX = xArray[index];
					}
					if(yArray[index] < minY)
					{
						minY = yArray[index];
					}
					if(yArray[index] > maxY)
					{
						maxY = yArray[index];
					}
				}
			}
			Set<Block> blocks = new HashSet<>();
			blocks.addAll(net.blocks());
			double weight = getWeight(blocks.size());
			cost += ((maxX - minX) + (maxY - minY) + 2) * weight;
			
		}
		return cost;
	}
	
	private double getWeight(int size)
	{
		double weight = 0.0;
		switch (size) {
			case 1:  weight=1; break;
			case 2:  weight=1; break;
			case 3:  weight=1; break;
			case 4:  weight=1.0828; break;
			case 5:  weight=1.1536; break;
			case 6:  weight=1.2206; break;
			case 7:  weight=1.2823; break;
			case 8:  weight=1.3385; break;
			case 9:  weight=1.3991; break;
			case 10: weight=1.4493; break;
			case 11:
			case 12:
			case 13:
			case 14:
			case 15: weight=(size-10)*(1.6899-1.4493)/5+1.4493;break;				
			case 16:
			case 17:
			case 18:
			case 19:
			case 20: weight=(size-15)*(1.8924-1.6899)/5+1.6899;break;
			case 21:
			case 22:
			case 23:
			case 24:
			case 25: weight=(size-20)*(2.0743-1.8924)/5+1.8924;break;		
			case 26:
			case 27:
			case 28:
			case 29:
			case 30: weight=(size-25)*(2.2334-2.0743)/5+2.0743;break;		
			case 31:
			case 32:
			case 33:
			case 34:
			case 35: weight=(size-30)*(2.3895-2.2334)/5+2.2334;break;		
			case 36:
			case 37:
			case 38:
			case 39:
			case 40: weight=(size-35)*(2.5356-2.3895)/5+2.3895;break;		
			case 41:
			case 42:
			case 43:
			case 44:
			case 45: weight=(size-40)*(2.6625-2.5356)/5+2.5356;break;		
			case 46:
			case 47:
			case 48:
			case 49:
			case 50: weight=(size-45)*(2.7933-2.6625)/5+2.6625;break;
			default: weight=(size-50)*0.02616+2.7933;break;
		}
		return weight;
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
