package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import mathtools.CGSolver;
import mathtools.Crs;
import placers.Rplace;
import timinganalysis.TimingEdge;
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

public class Hetero_TD_AnalyticalPlacerCombinedNetThree
{

	private final double ALPHA = 0.3;
	private final double TG_CRITICALITY_EXPONENT = 1.0;
	private final double MAX_CRITICALITY_THRESHOLD = 0.8;
	
	private HeterogeneousArchitecture architecture;
	private PackedCircuit circuit;
	private Map<Block, Integer> indexMap; // Maps a block (CLB or hardblock) to its integer index
	private int[] typeStartIndices;
	private String[] typeNames;
	private double[] linearX;
	private double[] linearY;
	private Hetero_TD_LegalizerOne legalizer;
	private TimingGraph timingGraph;
	
	public Hetero_TD_AnalyticalPlacerCombinedNetThree(HeterogeneousArchitecture architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
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
		System.out.println("Placement algorithm: Hetero_TD_AnalyticalPlacerCombinedNetThree");
		
		int solveMode = 0; //0 = solve all, 1 = solve CLBs only, 2 = solve hb1 type only, 3 = solve hb2 type only,...

		timingGraph.buildTimingGraph();
		timingGraph.mapTopLevelPinsToTimingGraph(circuit);
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
		
		for(Net net: circuit.getNets().values())
		{
			int nbPins = 1 + net.sinks.size();
			if(nbPins < 2)
			{
				continue;
			}
			
			//We always add all bound to bound connections, just as if we were doing wire length driven placement
			addBoundToBoundNet(net, solveMode, startIndex, true, xMatrix, xVector, yMatrix, yVector); 
			
			if(!firstSolve)
			{
				//Loop over all source sink connections. 
				//If they have a criticality larger than MAX_CRITICALITY_THRESHOLD an 
				//additional source sink connection is added to the linear system
				ArrayList<TimingEdge> netEdges = timingGraph.getNetEdges(net);
				if(netEdges != null)
				{
					for(TimingEdge edge: netEdges)
					{
						if(edge.getCriticality() > MAX_CRITICALITY_THRESHOLD) //Add additional source sink connection to the linear system
						{
							addSourceSinkConnection(net, edge, edge.getCriticality(), solveMode, startIndex, xMatrix, xVector, yMatrix, yVector);
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
	
	private void addBoundToBoundNet(Net net, int solveMode, int startIndex, boolean addQn, Crs xMatrix, double[] xVector, Crs yMatrix, double[] yVector)
	{
		int nbPins = 1 + net.sinks.size();
		ArrayList<Integer> netMovableBlockIndices = new ArrayList<>();
		ArrayList<Double> fixedXPositions = new ArrayList<>();
		ArrayList<Double> fixedYPositions = new ArrayList<>();
		double minX = Double.MAX_VALUE;
		int minXIndex = -1; //Index = -1 means fixed block
		double maxX = -Double.MAX_VALUE;
		int maxXIndex = -1;
		double minY = Double.MAX_VALUE;
		int minYIndex = -1;
		double maxY = -Double.MAX_VALUE;
		int maxYIndex = -1;
		double Qn = getWeight(nbPins);
		
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
			if(addQn)
			{
				weight *= Qn;
			}
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
			if(addQn)
			{
				weight *= Qn;
			}
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
				if(addQn)
				{
					weightMaxX *= Qn;
				}
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
				if(addQn)
				{
					weightMinX *= Qn;
				}
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
				if(addQn)
				{
					weightMaxY *= Qn;
				}
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
				if(addQn)
				{
					weightMinY *= Qn;
				}
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
					if(addQn)
					{
						weightMaxX *= Qn;
					}
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
					if(addQn)
					{
						weightMinX *= Qn;
					}
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
					if(addQn)
					{
						weightMaxY *= Qn;
					}
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
					if(addQn)
					{
						weightMinY *= Qn;
					}
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
	
	private void addSourceSinkConnection(Net net, TimingEdge edge, double criticality, int solveMode, int startIndex, 
																Crs xMatrix, double[] xVector, Crs yMatrix, double[] yVector)
	{
		int nbPins = 1 + net.sinks.size();
		
		Pin sourcePin = net.source;
		Block sourceOwner = sourcePin.owner;
		boolean isSourceFixed = isFixedPin(sourcePin, solveMode);
		double sourceX;
		double sourceY;
		if(!isSourceFixed)
		{
			int sourceIndex = indexMap.get(sourceOwner);
			sourceX = linearX[sourceIndex];
			sourceY = linearY[sourceIndex];
		}
		else
		{
			if(sourceOwner.type == BlockType.INPUT || sourceOwner.type == BlockType.OUTPUT)
			{
				sourceX = sourceOwner.getSite().x;
				sourceY = sourceOwner.getSite().y;
			}
			else //Must be a hard block
			{
				int sourceIndex = indexMap.get(sourceOwner);
				sourceX = legalizer.getBestLegalX()[sourceIndex];
				sourceY = legalizer.getBestLegalY()[sourceIndex];
			}
		}
		
		//Check
		if(sourceOwner.type == BlockType.CLB)
		{
			Clb sourceOwnerClb = (Clb)sourceOwner;
			if(sourceOwnerClb.getBle().getFlipflop() != null)
			{
				if(sourceOwnerClb.getBle().getFlipflop() != edge.getInput().getPin().owner)
				{
					System.out.println("Trouble");
				}
			}
			else
			{
				if(sourceOwnerClb.getBle().getLut() != edge.getInput().getPin().owner)
				{
					System.out.println("Trouble");
				}
			}
		}
		else
		{
			if(sourceOwner != edge.getInput().getPin().owner)
			{
				System.out.println("Trouble 1");
			}
		}
		
		//Search for the sink block and get its position
		Pin sinkPin = null;
		Block sinkOwner = null;
		double sinkX = -1.0;
		double sinkY = -1.0;
		for(Pin sink: net.sinks)
		{
			if(sink.owner.type == BlockType.CLB)
			{
				Clb sinkOwnerClb = (Clb)sink.owner;
				if(sinkOwnerClb.getBle().getLut() != null)
				{
					if(sinkOwnerClb.getBle().getLut() == edge.getOutput().getPin().owner)
					{
						sinkPin = sink;
						sinkOwner = sinkPin.owner;
						int sinkIndex = indexMap.get(sinkOwner);
						if(isFixedPin(sinkPin, solveMode))
						{
							sinkX = legalizer.getBestLegalX()[sinkIndex];
							sinkY = legalizer.getBestLegalY()[sinkIndex];
						}
						else
						{
							sinkX = linearX[sinkIndex];
							sinkY = linearY[sinkIndex];
						}
						break;
					}
				}
				else
				{
					if(sinkOwnerClb.getBle().getFlipflop() == edge.getOutput().getPin().owner)
					{
						sinkPin = sink;
						sinkOwner = sinkPin.owner;
						int sinkIndex = indexMap.get(sinkOwner);
						if(isFixedPin(sinkPin, solveMode))
						{
							sinkX = legalizer.getBestLegalX()[sinkIndex];
							sinkY = legalizer.getBestLegalY()[sinkIndex];
						}
						else
						{
							sinkX = linearX[sinkIndex];
							sinkY = linearY[sinkIndex];
						}
						break;
					}
				}
			}
			else //It's not a CLB
			{
				if(sink.owner == edge.getOutput().getPin().owner)
				{
					sinkPin = sink;
					sinkOwner = sinkPin.owner;
					if(sink.owner.type == BlockType.INPUT || sink.owner.type == BlockType.OUTPUT)
					{
						sinkX = sink.owner.getSite().x;
						sinkY = sink.owner.getSite().y;
					}
					else //Must be a hard block
					{
						int sinkIndex = indexMap.get(sink.owner);
						if(isFixedPin(sinkPin, solveMode))
						{
							sinkX = legalizer.getBestLegalX()[sinkIndex];
							sinkY = legalizer.getBestLegalY()[sinkIndex];
						}
						else
						{
							sinkX = linearX[sinkIndex];
							sinkY = linearY[sinkIndex];
						}
						break;
					}
				}
			}
		}
		if(sinkPin == null) //Check
		{
			System.out.println("Trouble 2");
		}
		boolean isSinkFixed = isFixedPin(sinkPin, solveMode);
			
		if(!(isSourceFixed && isSinkFixed)) //Not both fixed
		{
			//Calculate weight
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
			
			//Search for connection in timing graph
			//We don't have to check if this is a firstSolve or not because we never use the source-sink net model for firstSolves
			weightX *= criticality;
			weightY *= criticality;

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
