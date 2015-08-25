package placers.analyticalplacer.old;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.analyticalplacer.Hetero_TD_LegalizerOne;
import placers.random.RandomPlacer;
import timinganalysis.TimingGraph;

import architecture.HeterogeneousArchitecture;
import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.HardBlock;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;
import circuit.PrePackedCircuit;

public class Hetero_TD_AnalyticalPlacerNewNetTwo
{

	private final double ALPHA = 0.3;
	private final double TG_CRITICALITY_EXPONENT = 1.0;
	
	private HeterogeneousArchitecture architecture;
	private PackedCircuit circuit;
	private Map<Block, Integer> indexMap; // Maps a block (CLB or hardblock) to its integer index
	private int[] typeStartIndices;
	private String[] typeNames;
	private double[] linearX;
	private double[] linearY;
	private Hetero_TD_LegalizerOne legalizer;
	private TimingGraph timingGraph;
	
	public Hetero_TD_AnalyticalPlacerNewNetTwo(HeterogeneousArchitecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		RandomPlacer.placeCLBsandFixedIOs(circuit, architecture, new Random(1));
		initializeDataStructures();
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.setCriticalityExponent(TG_CRITICALITY_EXPONENT);
		this.legalizer = new Hetero_TD_LegalizerOne(architecture, typeStartIndices, typeNames, linearX.length, timingGraph, circuit);
	}
	
	public void place()
	{
		int solveMode = 0; //0 = solve all, 1 = solve CLBs only, 2 = solve hb1 type only, 3 = solve hb2 type only,...

		timingGraph.buildTimingGraph();
		timingGraph.mapTopLevelPinsToTimingGraph(circuit);
		
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
		int dimensions;
		int startIndex;
		if(solveMode == 0)
		{
			dimensions = linearX.length;
			startIndex = 0;
		}
		else
		{
			startIndex = typeStartIndices[solveMode - 1];
			if(solveMode == typeNames.length)
			{
				dimensions = linearX.length - typeStartIndices[typeStartIndices.length - 1];
			}
			else
			{
				dimensions = typeStartIndices[solveMode] - typeStartIndices[solveMode - 1];
			}
		}
		Crs xMatrix = new Crs(dimensions);
		double[] xVector = new double[dimensions];
		Crs yMatrix = new Crs(dimensions);
		double[] yVector = new double[dimensions];
		
		//Add pseudo connections
		if(!firstSolve)
		{
			//Process pseudonets
			int[] anchorPointsX = legalizer.getAnchorPointsX();
			int[] anchorPointsY = legalizer.getAnchorPointsY();
			for(int i = 0; i < dimensions; i++)
			{
				double deltaX = Math.abs(anchorPointsX[i + startIndex] - linearX[i + startIndex]);
				if(deltaX < 0.005)
				{
					deltaX = 0.005;
				}
				double pseudoWeightX = 2*pseudoWeightFactor*(1/deltaX);
				xMatrix.setElement(i, i, xMatrix.getElement(i, i) + pseudoWeightX);
				xVector[i] += pseudoWeightX * anchorPointsX[i + startIndex];
				double deltaY = Math.abs(anchorPointsY[i + startIndex] - linearY[i + startIndex]);
				if(deltaY < 0.005)
				{
					deltaY = 0.005;
				}
				double pseudoWeightY = 2*pseudoWeightFactor*(1/deltaY);
				yMatrix.setElement(i, i, yMatrix.getElement(i, i) + pseudoWeightY);
				yVector[i] += pseudoWeightY*anchorPointsY[i + startIndex];
			}
		}
		
		//Build the linear systems (x and y are solved separately)
		for(Net net:circuit.getNets().values())
		{
			int nbPins = 1 + net.sinks.size();
			if(nbPins < 2)
			{
				continue;
			}
			
			double Qn = getWeight(nbPins);
			
			//Process net source pin
			Block sourceOwner = net.source.owner;
			boolean isSourceFixed;
			double sourceX;
			double sourceY;
			if(!isFixedPin(net.source, solveMode))
			{
				isSourceFixed = false;
				int sourceIndex = indexMap.get(sourceOwner);
				sourceX = linearX[sourceIndex];
				sourceY = linearY[sourceIndex];
			}
			else
			{
				isSourceFixed = true;
				if(sourceOwner.type == BlockType.INPUT || sourceOwner.type == BlockType.OUTPUT)
				{
					sourceX = sourceOwner.getSite().x;
					sourceY = sourceOwner.getSite().y;
				}
				else
				{
					int sourceIndex = indexMap.get(sourceOwner);
					sourceX = legalizer.getBestLegalX()[sourceIndex];
					sourceY = legalizer.getBestLegalY()[sourceIndex];
				}
			}
			
			//Find bounding box
			double minX = sourceX;
			double maxX = sourceX;
			double minY = sourceY;
			double maxY = sourceY;
			for(Pin sinkPin: net.sinks)
			{
				double sinkX;
				double sinkY;
				if(!isFixedPin(sinkPin, solveMode))
				{
					int sinkIndex = indexMap.get(sinkPin.owner);
					sinkX = linearX[sinkIndex];
					sinkY = linearY[sinkIndex];
				}
				else
				{
					if(sinkPin.owner.type == BlockType.INPUT || sinkPin.owner.type == BlockType.OUTPUT)
					{
						sinkX = sinkPin.owner.getSite().x;
						sinkY = sinkPin.owner.getSite().y;
					}
					else
					{
						int sinkIndex = indexMap.get(sinkPin.owner);
						sinkX = legalizer.getBestLegalX()[sinkIndex];
						sinkY = legalizer.getBestLegalY()[sinkIndex];
					}
				}
				
				if(sinkX > maxX)
				{
					maxX = sinkX;
				}
				if(sinkX < minX)
				{
					minX = sinkX;
				}
				if(sinkY > maxY)
				{
					maxY = sinkY;
				}
				if(sinkY < minY)
				{
					minY = sinkY;
				}
			}
			
			//Process net sink pins
			boolean doneMaxX = false;
			boolean doneMinX = false;
			boolean doneMaxY = false;
			boolean doneMinY = false;
			for(Pin sinkPin: net.sinks)
			{
				Block sinkOwner = sinkPin.owner;
				boolean isSinkFixed;
				double sinkX;
				double sinkY;
				if(!isFixedPin(sinkPin, solveMode))
				{
					isSinkFixed = false;
					int sinkIndex = indexMap.get(sinkOwner);
					sinkX = linearX[sinkIndex];
					sinkY = linearY[sinkIndex];
				}
				else
				{
					isSinkFixed = true;
					if(sinkOwner.type == BlockType.INPUT || sinkOwner.type == BlockType.OUTPUT)
					{
						sinkX = sinkOwner.getSite().x;
						sinkY = sinkOwner.getSite().y;
					}
					else
					{
						int sinkIndex = indexMap.get(sinkOwner);
						sinkX = legalizer.getBestLegalX()[sinkIndex];
						sinkY = legalizer.getBestLegalY()[sinkIndex];
					}
				}
				if(!(isSourceFixed && isSinkFixed)) //Not both fixed
				{
					//Calculate weight
					double deltaX = Math.abs(sinkX - sourceX);
					if(deltaX < 0.001)
					{
						deltaX = 0.001;
					}
					double weightX = (double)2/((nbPins - 1)*deltaX);
//					double weightX;
//					if(sinkX == minX && !doneMinX) //Don't have to square it
//					{
//						weightX = (double)2/deltaX;
//						doneMinX = true;
//					}
//					else
//					{
//						if(sinkX == maxX && !doneMaxX) //Don't have to square it
//						{
//							weightX = (double)2/deltaX;
//							doneMaxX = true;
//						}
//						else //Have to square it
//						{
//							//weightX = (double)2/(deltaX*deltaX);
//							weightX = (double)2/deltaX;
//						}
//					}
					double deltaY = Math.abs(sinkY - sourceY);
					if(deltaY < 0.001)
					{
						deltaY = 0.001;
					}
					double weightY = (double)2/((nbPins - 1)*deltaY);
//					double weightY;
//					if(sinkY == minY && !doneMinY)
//					{
//						weightY = (double)2/deltaY;
//						doneMinY = true;
//					}
//					else
//					{
//						if(sinkY == maxY && !doneMaxY)
//						{
//							weightY = (double)2/deltaY;
//							doneMaxY = true;
//						}
//						else
//						{
//							//weightY = (double)2/(deltaY*deltaY);
//							weightY = (double)2/deltaY;
//						}
//					}
					
					//weightX *= Qn;
					//weightY *= Qn;
					
					if(!firstSolve)
					{
						//Search for connection in timing graph
						double criticality = timingGraph.getConnectionCriticalityWithExponent(net.source, sinkPin);
						weightX *= criticality;
						weightY *= criticality;
					}
					
					//Add to linear system
					if(isSourceFixed) //Source is fixed, sink is free
					{
						int sinkIndex = indexMap.get(sinkOwner);
						xMatrix.setElement(sinkIndex - startIndex, sinkIndex - startIndex, 
										xMatrix.getElement(sinkIndex - startIndex, sinkIndex - startIndex) + weightX);
						xVector[sinkIndex - startIndex] += weightX * sourceX;
						yMatrix.setElement(sinkIndex - startIndex, sinkIndex - startIndex, 
										yMatrix.getElement(sinkIndex - startIndex, sinkIndex - startIndex) + weightY);
						yVector[sinkIndex - startIndex] += weightY * sourceY;
					}
					else //Source is free
					{
						if(isSinkFixed) //Sink is fixed, source is free
						{
							int sourceIndex = indexMap.get(sourceOwner);
							xMatrix.setElement(sourceIndex - startIndex, sourceIndex - startIndex, 
										xMatrix.getElement(sourceIndex - startIndex, sourceIndex - startIndex) + weightX);
							xVector[sourceIndex - startIndex] += weightX * sinkX;
							yMatrix.setElement(sourceIndex - startIndex, sourceIndex - startIndex, 
										yMatrix.getElement(sourceIndex - startIndex, sourceIndex - startIndex) + weightY);
							yVector[sourceIndex - startIndex] += weightY * sinkY;
						}
						else //Both are free
						{
							int sourceIndex = indexMap.get(sourceOwner);
							int sinkIndex = indexMap.get(sinkOwner);
							xMatrix.setElement(sourceIndex - startIndex, sourceIndex - startIndex, 
										xMatrix.getElement(sourceIndex - startIndex, sourceIndex - startIndex) + weightX);
							xMatrix.setElement(sourceIndex - startIndex, sinkIndex - startIndex, 
										xMatrix.getElement(sourceIndex - startIndex, sinkIndex - startIndex) - weightX);
							xMatrix.setElement(sinkIndex - startIndex, sourceIndex - startIndex, 
										xMatrix.getElement(sinkIndex - startIndex, sourceIndex - startIndex) - weightX);
							xMatrix.setElement(sinkIndex - startIndex, sinkIndex - startIndex, 
										xMatrix.getElement(sinkIndex - startIndex, sinkIndex - startIndex) + weightX);
							yMatrix.setElement(sourceIndex - startIndex, sourceIndex - startIndex, 
										yMatrix.getElement(sourceIndex - startIndex, sourceIndex - startIndex) + weightY);
							yMatrix.setElement(sourceIndex - startIndex, sinkIndex - startIndex, 
										yMatrix.getElement(sourceIndex - startIndex, sinkIndex - startIndex) - weightY);
							yMatrix.setElement(sinkIndex - startIndex, sourceIndex - startIndex, 
										yMatrix.getElement(sinkIndex - startIndex, sourceIndex - startIndex) - weightY);
							yMatrix.setElement(sinkIndex - startIndex, sinkIndex - startIndex, 
										yMatrix.getElement(sinkIndex - startIndex, sinkIndex - startIndex) + weightY);
						}
					}
				}
			}
		}
		
		if(!xMatrix.isSymmetricalAndFinite())
		{
			System.err.println("ERROR: X-Matrix is assymmetrical: there must be a bug in the code!");
		}
		if(!yMatrix.isSymmetricalAndFinite())
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
		
		//Save results
		for(int i = 0; i < dimensions; i++)
		{
			linearX[startIndex + i] = xSolution[i];
			linearY[startIndex + i] = ySolution[i];
		}
		
	}
	
	private boolean isFixedPin(Pin pin, int solveMode)
	{
		boolean isFixed;
		if(pin.owner.type == BlockType.INPUT || pin.owner.type == BlockType.OUTPUT)
		{
			isFixed = true; //IOs are always considered fixed
		}
		else
		{
			if(solveMode == 0) //If solving all ==> we are sure the pin is not fixed because it's not an IO pin
			{
				isFixed = false;
			}
			else //We are only solving a specific type of blocks
			{
				if(solveMode == 1) //We are solving CLBs ==> easy to check
				{
					if(pin.owner.type == BlockType.CLB)
					{
						isFixed = false;
					}
					else
					{
						isFixed = true;
					}
				}
				else //We are solving a specific type of hard blocks
				{
					if(pin.owner.type == BlockType.HARDBLOCK_CLOCKED || pin.owner.type == BlockType.HARDBLOCK_UNCLOCKED)
					{
						if(((HardBlock)pin.owner).getTypeName().equals(typeNames[solveMode - 1]))
						{
							isFixed = false;
						}
						else
						{
							isFixed = true;
						}
					}
					else
					{
						isFixed = true;
					}
				}
			}
		}
		return isFixed;
	}
	
	private void updateCircuit()
	{
		int[] bestLegalX = legalizer.getBestLegalX();
		int[] bestLegalY = legalizer.getBestLegalY();
		
		//Clear all previous locations
		int maximalX = architecture.getWidth();
		int maximalY = architecture.getHeight();
		for(int i = 1; i <= maximalX; i++)
		{
			for(int j = 1; j <= maximalY; j++)
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
		for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
		{
			for(HardBlock hb: hbVector)
			{
				int index = indexMap.get(hb);
				Site site = architecture.getSite(bestLegalX[index], bestLegalY[index], 0);
				site.block = hb;
				hb.setSite(site);
			}
		}
	}
	
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
				int index = indexMap.get(sourceBlock);
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
					int index = indexMap.get(sinkOwner);
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
