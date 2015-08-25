package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.Placer;
import placers.random.RandomPlacer;

import architecture.HeterogeneousArchitecture;
import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.HardBlock;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;

public class HeteroAnalyticalPlacerTwo extends Placer
{

	private final double ALPHA = 0.3;
	
	private HeterogeneousArchitecture architecture;
	private PackedCircuit circuit;
	private Map<Block, Integer> indexMap; // Maps a block (CLB or hardblock) to its integer index
	private int[] typeStartIndices;
	private String[] typeNames;
	private double[] linearX;
	private double[] linearY;
	private HeteroLegalizerTwo legalizer;
	private Collection<Net> nets;
	private Collection<Clb> clbs;
	
	private boolean doneMemoryUse;
	private int totalMatrixBytes;
	
	public HeteroAnalyticalPlacerTwo(HeterogeneousArchitecture architecture, PackedCircuit circuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.nets = circuit.nets.values();
		this.clbs = circuit.clbs.values();
		initializeDataStructures();
		this.legalizer = new HeteroLegalizerTwo(architecture, typeStartIndices, typeNames, linearX.length);
		this.doneMemoryUse = false;
		this.totalMatrixBytes = 0;
	}
	
	public int place()
	{
		int solveMode = 0; //0 = solve all, 1 = solve CLBs only, 2 = solve hb1 type only, 3 = solve hb2 type only,...
		double[] maxUtilizationLegalizerArray = new double[] {4.0,3.0,2.0,1.5,0.9};
		//double[] maxUtilizationLegalizerArray = new double[] {4.5,3.0,2.0,1.35,0.9};
		//double[] maxUtilizationLegalizerArray = new double[] {0.9}; //No partial overlap solves...
		double maxUtilizationLegalizer = maxUtilizationLegalizerArray[0];
		
		RandomPlacer.placeFixedIOs(circuit, architecture);
		
		//Initial linear solves, should normally be done 5-7 times		
		for(int i = 0; i < 7; i++)
		{
			solveLinear(true, solveMode, 0.0);
		}
		
		double initialLinearCost = calculateTotalCost(linearX, linearY);
		System.out.println("Linear cost after initial solves:" + initialLinearCost);
		
		//Initial legalization
		double totalLegalizeTime = 0.0;
		long legalizeStartTime = System.nanoTime();
		legalizer.legalize(linearX, linearY, nets, indexMap, solveMode, maxUtilizationLegalizer);
		long legalizeStopTime = System.nanoTime();
		totalLegalizeTime += (double)(legalizeStopTime - legalizeStartTime) / 1000000000.0;
		
		double pseudoWeightFactor = 0.0;
		for(int i = 0; i < 30; i++)
		{
			solveMode = (solveMode + 1) % (typeNames.length + 1);
			if(solveMode <= 1)
			{
				pseudoWeightFactor += ALPHA;
				int index = Math.min((int)Math.round(pseudoWeightFactor / ALPHA), maxUtilizationLegalizerArray.length - 1);
				maxUtilizationLegalizer = maxUtilizationLegalizerArray[index];
			}
			solveLinear(false, solveMode, pseudoWeightFactor);
			double costLinear = calculateTotalCost(linearX, linearY);
			legalizeStartTime = System.nanoTime();
			legalizer.legalize(linearX, linearY, nets, indexMap, solveMode, maxUtilizationLegalizer);
			legalizeStopTime = System.nanoTime();
			totalLegalizeTime += (double)(legalizeStopTime - legalizeStartTime) / 1000000000.0;
			double costLegal = legalizer.calculateBestLegalCost(nets, indexMap);
			//System.out.println("Linear cost iteration " + i + ": " + costLinear);
			//System.out.println("Legal cost iteration " + i + ": " + costLegal);
			if(costLinear / costLegal > 0.80)
			{
				break;
			}
		}
		
		System.out.printf("Total time spent in legalization = %.2f\n", totalLegalizeTime);
		
		updateCircuit();
		
		return totalMatrixBytes;
	}
	
	public int place(HashMap<String,String> dictionary)
	{
		//startingStage = 0 ==> start with initial solves (no anchors)
		//startingStage = 1 ==> start from existing placement that is incorporated in the packedCircuit passed with the constructor
		int startingStage = 0; //Default	
		if(dictionary.get("starting_stage") != null)
		{
			startingStage = Integer.parseInt(dictionary.get("starting_stage"));
			if(startingStage < 0 || startingStage > 1) //startingStage can only be 0 or 1
			{
				startingStage = 0;
			}
		}
		
		//initialize maxUtilizationSequence used by the legalizer
		double[] maxUtilizationSequenceArray = new double[] {0.9}; //Default
		if(dictionary.get("max_utilization_sequence") != null)
		{
			String maxUtilizationSequenceString = dictionary.get("max_utilization_sequence");
			String[] maxUtilizationSequenceStringArray = maxUtilizationSequenceString.split(";");
			maxUtilizationSequenceArray = new double[maxUtilizationSequenceStringArray.length];
			int i = 0;
			for(String maxUtilization: maxUtilizationSequenceStringArray)
			{
				maxUtilizationSequenceArray[i] = Double.parseDouble(maxUtilization);
				i++;
			}
		}
		
		//The first anchorWeight factor that will be used in the main solve loop
		double startingAnchorWeight = 0.3; //Default
		if(dictionary.get("starting_anchor_weight") != null)
		{
			startingAnchorWeight = Double.parseDouble(dictionary.get("starting_anchor_weight"));
		}
		
		//The amount with whom the anchorWeight factor will be increased each iteration
		double anchorWeightIncrease = 0.3; //Default
		if(dictionary.get("anchor_weight_increase") != null)
		{
			anchorWeightIncrease = Double.parseDouble("anchor_weight_increase");
		}
		
		//The ratio of linear solutions cost to legal solution cost at which we stop the algorithm
		double stopRatioLinearLegal = 0.8; //Default
		if(dictionary.get("stop_ratio_linear_legal") != null)
		{
			stopRatioLinearLegal = Double.parseDouble(dictionary.get("stop_ratio_linear_legal"));
		}
		
		int maxMemoryUse = placementOuterLoop(startingStage, maxUtilizationSequenceArray, 
							startingAnchorWeight, anchorWeightIncrease, stopRatioLinearLegal);
		
		return maxMemoryUse;
	}
	
	private int placementOuterLoop(int startingStage, double[] maxUtilizationSequence, 
			double startingAnchorWeight, double anchorWeightIncrease, double stopRatioLinearLegal)
	{
		double pseudoWeightFactor;
		int itNumber;
		if(startingStage == 0)
		{
			RandomPlacer.placeFixedIOs(circuit, architecture);
			initializeArraysRandom();
			
			int solveMode = 0;
			for(int i = 0; i < 7; i++) //Initial linear solves, should normally be done 5-7 times
			{
				solveLinear(true, solveMode, 0.0);
			}
			//Initial legalization
			legalizer.legalize(linearX, linearY, nets, indexMap, solveMode, maxUtilizationSequence[0]);
			
			pseudoWeightFactor = anchorWeightIncrease;
			itNumber = 1;
		}
		else //startingStage == 1
		{
			initializeArraysInitialPlacement();
			legalizer.initializeArrays(linearX, linearY, nets, indexMap);
			pseudoWeightFactor = startingAnchorWeight;
			itNumber = 0;
		}
		
		int solveMode = 0;
		double costLinear;
		double costLegal;
		do
		{
			System.out.println("Iteration " + (itNumber + 1) + ": pseudoWeightFactor = " + pseudoWeightFactor + 
					", solveMode = " + solveMode);
			int index = Math.min(itNumber, maxUtilizationSequence.length - 1);
			double maxUtilizationLegalizer = maxUtilizationSequence[index];
			for(int i = 0; i < 3; i++)
			{
				solveLinear(false, solveMode, pseudoWeightFactor);
			}
			costLinear = calculateTotalCost(linearX, linearY);
			legalizer.legalize(linearX, linearY, nets, indexMap, solveMode, maxUtilizationLegalizer);
			costLegal = legalizer.calculateBestLegalCost(nets, indexMap);
			System.out.println("Linear cost iteration " + itNumber + ": " + costLinear);
			System.out.println("Legal cost iteration " + itNumber + ": " + costLegal);
			solveMode = (solveMode + 1) % (typeNames.length + 1);
			if(solveMode <= 1)
			{
				pseudoWeightFactor += anchorWeightIncrease;
				itNumber++;
			}
		}while(costLinear / costLegal < stopRatioLinearLegal);
		
		updateCircuit();
		
		return totalMatrixBytes;
	}
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 * SolveMode: 0 = solve all, 1 = solve CLBs only, 2 = solve hb1 type only, 3 = solve hb2 type only,...
	 */
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
		for(Net net: nets)
		{
			ArrayList<Integer> netMovableBlockIndices = new ArrayList<>();
			ArrayList<Double> fixedXPositions = new ArrayList<>();
			ArrayList<Double> fixedYPositions = new ArrayList<>();
			int nbPins = 1 + net.sinks.size();
			if(nbPins < 2)
			{
				continue;
			}
			double minX = Double.MAX_VALUE;
			int minXIndex = -1; //Index = -1 means fixed block
			double maxX = -Double.MAX_VALUE;
			int maxXIndex = -1;
			double minY = Double.MAX_VALUE;
			int minYIndex = -1;
			double maxY = -Double.MAX_VALUE;
			int maxYIndex = -1;
			double Qn = getWeight(net.sinks.size() + 1);
			
			//Search bounding box boundaries
			//Handle net source pin
			if(!isFixedPin(net.source, solveMode)) //The considered pin is not fixed
			{
				int index = indexMap.get(net.source.owner);
				netMovableBlockIndices.add(index);
				if(linearX[index] > maxX)
				{
					maxX = linearX[index];
					maxXIndex = index;
				}
				if(linearX[index] < minX)
				{
					minX = linearX[index];
					minXIndex = index;
				}
				if(linearY[index] > maxY)
				{
					maxY = linearY[index];
					maxYIndex = index;
				}
				if(linearY[index] < minY)
				{
					minY = linearY[index];
					minYIndex = index;
				}
			}
			else //The considered pin is fixed
			{
				double xPosition;
				double yPosition;
				if(net.source.owner.type == BlockType.INPUT || net.source.owner.type == BlockType.OUTPUT) //IOs are always fixed
				{
					xPosition = net.source.owner.getSite().x;
					yPosition = net.source.owner.getSite().y;
				}
				else //This is a movable block which is not moved in this iteration
				{
					int index = indexMap.get(net.source.owner);
					xPosition = legalizer.getAnchorPointsX()[index];
					yPosition = legalizer.getAnchorPointsY()[index];
				}
				fixedXPositions.add(xPosition);
				fixedYPositions.add(yPosition);
				if(xPosition > maxX)
				{
					maxX = xPosition;
					maxXIndex = -1;
				}
				if(xPosition < minX)
				{
					minX = xPosition;
					minXIndex = -1;
				}
				if(yPosition > maxY)
				{
					maxY = yPosition;
					maxYIndex = -1;
				}
				if(yPosition < minY)
				{
					minY = yPosition;
					minYIndex = -1;
				}
			}
			
			//Handle net sink pins
			for(Pin sinkPin:net.sinks)
			{
				if(!isFixedPin(sinkPin, solveMode))
				{
					int index = indexMap.get(sinkPin.owner);
					netMovableBlockIndices.add(index);
					if(linearX[index] > maxX)
					{
						maxX = linearX[index];
						maxXIndex = index;
					}
					if(linearX[index] < minX)
					{
						minX = linearX[index];
						minXIndex = index;
					}
					if(linearY[index] > maxY)
					{
						maxY = linearY[index];
						maxYIndex = index;
					}
					if(linearY[index] < minY)
					{
						minY = linearY[index];
						minYIndex = index;
					}
				}
				else
				{
					double xPosition;
					double yPosition;
					if(sinkPin.owner.type == BlockType.INPUT || sinkPin.owner.type == BlockType.OUTPUT) //IOs are always fixed
					{
						xPosition = sinkPin.owner.getSite().x;
						yPosition = sinkPin.owner.getSite().y;
					}
					else //This is a movable block which is not moved in this iteration
					{
						int index = indexMap.get(sinkPin.owner);
						xPosition = legalizer.getAnchorPointsX()[index];
						yPosition = legalizer.getAnchorPointsY()[index];
					}
					fixedXPositions.add(xPosition);
					fixedYPositions.add(yPosition);
					if(xPosition > maxX)
					{
						maxX = xPosition;
						maxXIndex = -1;
					}
					if(xPosition < minX)
					{
						minX = xPosition;
						minXIndex = -1;
					}
					if(yPosition > maxY)
					{
						maxY = yPosition;
						maxYIndex = -1;
					}
					if(yPosition < minY)
					{
						minY = yPosition;
						minYIndex = -1;
					}
				}
			}
			

			
			
			
			
			
			
			//Add connection between min and max
			if(!(minXIndex == -1 && maxXIndex == -1))
			{
				double delta = maxX - minX;
				if(delta < 0.005)
				{
					delta = 0.005;
				}
				double weight = ((double)2/(nbPins-1)) * (1/delta);
				weight *= Qn;
				if(maxXIndex == -1)
				{
					//maxX fixed but minX not
					xMatrix.setElement(minXIndex - startIndex, minXIndex - startIndex, 
															xMatrix.getElement(minXIndex - startIndex, minXIndex - startIndex) + weight);
					xVector[minXIndex - startIndex] = xVector[minXIndex - startIndex] + weight*maxX;
				}
				else
				{
					if(minXIndex == -1)
					{
						//minX fixed but maxX not
						xMatrix.setElement(maxXIndex - startIndex, maxXIndex - startIndex, 
															xMatrix.getElement(maxXIndex - startIndex, maxXIndex - startIndex) + weight);
						xVector[maxXIndex - startIndex] = xVector[maxXIndex - startIndex] + weight*minX;
					}
					else
					{
						//neither of both fixed
						xMatrix.setElement(minXIndex - startIndex, minXIndex - startIndex, 
															xMatrix.getElement(minXIndex - startIndex, minXIndex - startIndex) + weight);
						xMatrix.setElement(maxXIndex - startIndex, maxXIndex - startIndex, 
															xMatrix.getElement(maxXIndex - startIndex, maxXIndex - startIndex) + weight);
						xMatrix.setElement(minXIndex - startIndex, maxXIndex - startIndex, 
															xMatrix.getElement(minXIndex - startIndex, maxXIndex - startIndex) - weight);
						xMatrix.setElement(maxXIndex - startIndex, minXIndex - startIndex, 
															xMatrix.getElement(maxXIndex - startIndex, minXIndex - startIndex) - weight);
					}
				}
			}
			if(!(minYIndex == -1 && maxYIndex == -1))
			{
				double delta = maxY - minY;
				if(delta < 0.005)
				{
					delta = 0.005;
				}
				double weight = ((double)2/(nbPins-1)) * (1/delta);
				weight *= Qn;
				if(maxYIndex == -1)
				{
					//maxX fixed but minX not
					yMatrix.setElement(minYIndex - startIndex, minYIndex - startIndex, 
															yMatrix.getElement(minYIndex - startIndex, minYIndex - startIndex) + weight);
					yVector[minYIndex - startIndex] = yVector[minYIndex - startIndex] + weight*maxY;
				}
				else
				{
					if(minYIndex == -1)
					{
						//minX fixed but maxX not
						yMatrix.setElement(maxYIndex - startIndex, maxYIndex - startIndex, 
															yMatrix.getElement(maxYIndex - startIndex, maxYIndex - startIndex) + weight);
						yVector[maxYIndex - startIndex] = yVector[maxYIndex - startIndex] + weight*minY;
					}
					else
					{
						//neither of both fixed
						yMatrix.setElement(minYIndex - startIndex, minYIndex - startIndex, 
															yMatrix.getElement(minYIndex - startIndex, minYIndex - startIndex) + weight);
						yMatrix.setElement(maxYIndex - startIndex, maxYIndex - startIndex, 
															yMatrix.getElement(maxYIndex - startIndex, maxYIndex - startIndex) + weight);
						yMatrix.setElement(minYIndex - startIndex, maxYIndex - startIndex, 
															yMatrix.getElement(minYIndex - startIndex, maxYIndex - startIndex) - weight);
						yMatrix.setElement(maxYIndex - startIndex, minYIndex - startIndex, 
															yMatrix.getElement(maxYIndex - startIndex, minYIndex - startIndex) - weight);
					}
				}
			}
			
			//Add movable internal connections to min and max
			for(Integer index: netMovableBlockIndices)
			{
				if(index != minXIndex)
				{
					double deltaMaxX = Math.abs(linearX[index] - maxX);
					if(deltaMaxX < 0.005)
					{
						deltaMaxX = 0.005;
					}
					double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
					weightMaxX *= Qn;
					if(maxXIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						xMatrix.setElement(index - startIndex, index - startIndex, 
															xMatrix.getElement(index - startIndex, index - startIndex) + weightMaxX);
						xVector[index - startIndex] = xVector[index - startIndex] + weightMaxX*maxX;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(maxXIndex == index))
						{
							xMatrix.setElement(index - startIndex, index - startIndex, 
															xMatrix.getElement(index - startIndex, index - startIndex) + weightMaxX);
							xMatrix.setElement(maxXIndex - startIndex, maxXIndex - startIndex, 
															xMatrix.getElement(maxXIndex - startIndex, maxXIndex - startIndex) + weightMaxX);
							xMatrix.setElement(index - startIndex, maxXIndex - startIndex, 
															xMatrix.getElement(index - startIndex, maxXIndex - startIndex) - weightMaxX);
							xMatrix.setElement(maxXIndex - startIndex, index - startIndex, 
															xMatrix.getElement(maxXIndex - startIndex, index - startIndex) - weightMaxX);
						}
					}
				}
				if(index != maxXIndex)
				{
					double deltaMinX = Math.abs(linearX[index] - minX);
					if(deltaMinX < 0.005)
					{
						//System.out.println("Problem 4");
						deltaMinX = 0.005;
					}
					double weightMinX = ((double)2/(nbPins-1)) * (1/deltaMinX);
					weightMinX *= Qn;
					if(minXIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						xMatrix.setElement(index - startIndex, index - startIndex, 
															xMatrix.getElement(index - startIndex, index - startIndex) + weightMinX);
						xVector[index - startIndex] = xVector[index - startIndex] + weightMinX*minX;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(minXIndex == index))
						{
							xMatrix.setElement(index - startIndex, index - startIndex, 
															xMatrix.getElement(index - startIndex, index - startIndex) + weightMinX);
							xMatrix.setElement(minXIndex - startIndex, minXIndex - startIndex, 
															xMatrix.getElement(minXIndex - startIndex, minXIndex - startIndex) + weightMinX);
							xMatrix.setElement(index - startIndex, minXIndex - startIndex, 
															xMatrix.getElement(index - startIndex, minXIndex - startIndex) - weightMinX);
							xMatrix.setElement(minXIndex - startIndex, index - startIndex, 
															xMatrix.getElement(minXIndex - startIndex, index - startIndex) - weightMinX);
						}
					}
				}
				if(index != minYIndex)
				{
					double deltaMaxY = Math.abs(linearY[index] - maxY);
					if(deltaMaxY < 0.005)
					{
						deltaMaxY = 0.005;
					}
					double weightMaxY = ((double)2/(nbPins-1)) * (1/deltaMaxY);
					weightMaxY *= Qn;
					if(maxYIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						yMatrix.setElement(index - startIndex, index - startIndex, 
															yMatrix.getElement(index - startIndex, index - startIndex) + weightMaxY);
						yVector[index - startIndex] = yVector[index - startIndex] + weightMaxY*maxY;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(maxYIndex == index))
						{
							yMatrix.setElement(index - startIndex, index - startIndex, 
															yMatrix.getElement(index - startIndex, index - startIndex) + weightMaxY);
							yMatrix.setElement(maxYIndex - startIndex, maxYIndex - startIndex, 
															yMatrix.getElement(maxYIndex - startIndex, maxYIndex - startIndex) + weightMaxY);
							yMatrix.setElement(index - startIndex, maxYIndex - startIndex, 
															yMatrix.getElement(index - startIndex, maxYIndex - startIndex) - weightMaxY);
							yMatrix.setElement(maxYIndex - startIndex, index - startIndex, 
															yMatrix.getElement(maxYIndex - startIndex, index - startIndex) - weightMaxY);
						}
					}
				}
				if(index != maxYIndex)
				{
					double deltaMinY = Math.abs(linearY[index] - minY);
					if(deltaMinY < 0.005)
					{
						deltaMinY = 0.005;
					}
					double weightMinY = ((double)2/(nbPins-1)) * (1/deltaMinY);
					weightMinY *= Qn;
					if(minYIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						yMatrix.setElement(index - startIndex, index - startIndex, 
															yMatrix.getElement(index - startIndex, index - startIndex) + weightMinY);
						yVector[index - startIndex] = yVector[index - startIndex] + weightMinY*minY;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(minYIndex == index))
						{
							yMatrix.setElement(index - startIndex, index - startIndex, 
															yMatrix.getElement(index - startIndex, index - startIndex) + weightMinY);
							yMatrix.setElement(minYIndex - startIndex, minYIndex - startIndex, 
															yMatrix.getElement(minYIndex - startIndex, minYIndex - startIndex) + weightMinY);
							yMatrix.setElement(index - startIndex, minYIndex - startIndex, 
															yMatrix.getElement(index - startIndex, minYIndex - startIndex) - weightMinY);
							yMatrix.setElement(minYIndex - startIndex, index - startIndex, 
															yMatrix.getElement(minYIndex - startIndex, index - startIndex) - weightMinY);
						}
					}
				}
			}
			
			//Add fixed internal connections to min and max for X-problem
			boolean firstMax = true;
			boolean firstMin = true;
			for(double fixedXPosition: fixedXPositions)
			{
				if(!(fixedXPosition == minX && minXIndex == -1 && firstMax)) 
				{
					if(!(maxXIndex == -1))
					{
						double deltaMaxX = Math.abs(fixedXPosition - maxX);
						if(deltaMaxX < 0.005)
						{
							deltaMaxX = 0.005;
						}
						double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
						weightMaxX *= Qn;
						//Connection between fixed and non fixed block
						xMatrix.setElement(maxXIndex - startIndex, maxXIndex - startIndex, 
															xMatrix.getElement(maxXIndex - startIndex, maxXIndex - startIndex) + weightMaxX);
						xVector[maxXIndex - startIndex] = xVector[maxXIndex - startIndex] + weightMaxX*fixedXPosition;
					}
				}
				else
				{
					firstMax = false;
				}
				
				if(!(fixedXPosition == maxX && maxXIndex == -1 && firstMin))
				{
					if(!(minXIndex == -1))
					{
						double deltaMinX = Math.abs(fixedXPosition - minX);
						if(deltaMinX < 0.005)
						{
							deltaMinX = 0.005;
						}
						double weightMinX = ((double)2/(nbPins-1)) * (1/deltaMinX);
						weightMinX *= Qn;
						//Connection between fixed and non fixed block
						xMatrix.setElement(minXIndex - startIndex, minXIndex - startIndex, 
															xMatrix.getElement(minXIndex - startIndex, minXIndex - startIndex) + weightMinX);
						xVector[minXIndex - startIndex] = xVector[minXIndex - startIndex] + weightMinX*fixedXPosition;
					}
				}
				else
				{
					firstMin = false;
				}
			}
			
			//Add fixed internal connections to min and max for Y-problem
			firstMax = true;
			firstMin = true;
			for(double fixedYPosition:fixedYPositions)
			{
				if(!(fixedYPosition == minY && minYIndex == -1 && firstMax))
				{
					if(!(maxYIndex == -1))
					{
						double deltaMaxY = Math.abs(fixedYPosition - maxY);
						if(deltaMaxY < 0.005)
						{
							deltaMaxY = 0.005;
						}
						double weightMaxY = ((double)2/(nbPins-1)) * (1/deltaMaxY);
						weightMaxY *= Qn;
						//Connection between fixed and non fixed block
						yMatrix.setElement(maxYIndex - startIndex, maxYIndex - startIndex, 
															yMatrix.getElement(maxYIndex - startIndex, maxYIndex - startIndex) + weightMaxY);
						yVector[maxYIndex - startIndex] = yVector[maxYIndex - startIndex] + weightMaxY*fixedYPosition;
					}
				}
				else
				{
					firstMax = false;
				}
				
				if(!(fixedYPosition == maxY && maxYIndex == -1 && firstMin))
				{
					if(!(minYIndex == -1))
					{
						double deltaMinY = Math.abs(fixedYPosition - minY);
						if(deltaMinY < 0.005)
						{
							deltaMinY = 0.005;
						}
						double weightMinY = ((double)2/(nbPins-1)) * (1/deltaMinY);
						weightMinY *= Qn;
						//Connection between fixed and non fixed block
						yMatrix.setElement(minYIndex - startIndex, minYIndex - startIndex, 
															yMatrix.getElement(minYIndex - startIndex, minYIndex - startIndex) + weightMinY);
						yVector[minYIndex - startIndex] = yVector[minYIndex - startIndex] + weightMinY*fixedYPosition;
					}
				}
				else
				{
					firstMin = false;
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
		
//		if(firstSolve)
//		{
//			System.out.println("Memory usage of xMatrix (doubles): " + xMatrix.getMemoryUsageDouble());
//			System.out.println("Memory usage of xMatrix (floats): " + xMatrix.getMemoryUsageFloat());
//			System.out.println("Memory usage of yMatrix (doubles): " + yMatrix.getMemoryUsageDouble());
//			System.out.println("Memory usage of yMatrix (floats): " + yMatrix.getMemoryUsageFloat());
//				
//			//Wait for enter to start (necessary for easy profiling)
//			System.out.println("Hit any key to continue...");
//			try
//			{
//				System.in.read();
//			}
//			catch(Exception ioe)
//			{
//				System.out.println("Something went wrong");
//			}
//		}
		
		if(!doneMemoryUse)
		{
			doneMemoryUse = true;
			totalMatrixBytes = xMatrix.getMemoryUsageDouble() + yMatrix.getMemoryUsageDouble();
		}
		
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
		for(Clb clb: clbs)
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
		for(Net net: nets)
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
	
	private void initializeArraysRandom()
	{
		int maximalX = architecture.getWidth();
		int maximalY = architecture.getHeight();
		Random random = new Random();
		for(Clb clb: clbs)
		{
			int index = indexMap.get(clb);
			linearX[index] = 1 + (maximalX - 1) * random.nextDouble();
			linearY[index] = 1 + (maximalY - 1) * random.nextDouble();
		}
		for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
		{
			for(HardBlock hb: hbVector)
			{
				int index = indexMap.get(hb);
				linearX[index] = 1 + (maximalX - 1) * random.nextDouble();
				linearY[index] = 1 + (maximalY - 1) * random.nextDouble();
			}
		}
	}
	
	private void initializeArraysInitialPlacement()
	{
		for(Clb clb: clbs)
		{
			int index = indexMap.get(clb);
			linearX[index] = clb.getSite().x;
			linearY[index] = clb.getSite().y;
		}
		for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
		{
			for(HardBlock hb: hbVector)
			{
				int index = indexMap.get(hb);
				linearX[index] = hb.getSite().x;
				linearY[index] = hb.getSite().y;
			}
		}
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
		
		typeStartIndices[0] = 0;
		typeNames[0] = "CLB";
		int index = 0;
		for(Clb clb: clbs)
		{
			indexMap.put(clb, index);
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
				index++;
			}
			hardBlockTypeIndex++;
		}
	}
	
}
