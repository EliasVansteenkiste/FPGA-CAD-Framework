package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.Rplace;
import timinganalysis.TimingEdge;
import timinganalysis.TimingGraph;

import architecture.FourLutSanitized;
import circuit.BlockType;
import circuit.Clb;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;
import circuit.PrePackedCircuit;

public class TD_AnalyticalPlacerOldNetOne
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
	
	public TD_AnalyticalPlacerOldNetOne(FourLutSanitized architecture, PackedCircuit circuit, PrePackedCircuit prePackedCircuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.minimalX = 1;
		this.maximalX = architecture.getWidth();
		this.minimalY = 1;
		this.maximalY = architecture.getHeight();
		this.timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.setCriticalityExponent(1.0);
		legalizer = new TD_LegalizerOne(minimalX, maximalX, minimalY, maximalY, circuit.clbs.values().size(), 
												timingGraph, circuit, architecture);
	}
	
	public void place()
	{
		Rplace.placeCLBsandFixedIOs(circuit, architecture, new Random(1));
		initializeDataStructures();
		
		timingGraph.buildTimingGraph();
		timingGraph.mapNetsToEdges(circuit);
		
		//Initial linear solves, should normally be done 5-7 times		
		for(int i = 0; i < 7; i++)
		{
			solveLinear(true, 0.0);
		}
		
		//Initial legalization
		legalizer.legalize(linearX, linearY, indexMap);
		
//		for(int i = 0; i < linearX.length; i++)
//		{
//			System.out.printf("%d: (%.2f-%.2f)\n", i, linearX[i], linearY[i]);
//		}
		
		//Iterative solves with pseudonets
		for(int i = 0; i < 30; i++)
		{
			solveLinear(false, (i+1)*ALPHA);
			legalizer.legalize(linearX, linearY, indexMap);
		}
	}
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 */
	private void solveLinear(boolean firstSolve, double pseudoWeightFactor)
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
			ArrayList<Integer> netClbIndices = new ArrayList<>();
			ArrayList<Double> fixedXPositions = new ArrayList<>();
			ArrayList<Double> fixedYPositions = new ArrayList<>();
			int nbPins = 1 + net.sinks.size();
			double timingWeightFactor = 0.0;
			ArrayList<TimingEdge> netEdges = timingGraph.getNetEdges(net);
			if(netEdges != null)
			{
				for(TimingEdge edge: netEdges)
				{
					timingWeightFactor += edge.getCost();
				}
			}
			else
			{
				timingWeightFactor = 1.0;
			}
			double minX = Double.MAX_VALUE;
			int minXIndex = -1; //Index = -1 means fixed block
			double maxX = -Double.MAX_VALUE;
			int maxXIndex = -1;
			double minY = Double.MAX_VALUE;
			int minYIndex = -1;
			double maxY = -Double.MAX_VALUE;
			int maxYIndex = -1;
			if(net.source.owner.type == BlockType.CLB)
			{
				int index = indexMap.get((Clb)net.source.owner);
				netClbIndices.add(index);
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
				double xPosition = net.source.owner.getSite().x;
				double yPosition = net.source.owner.getSite().y;
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
			for(Pin pin:net.sinks)
			{
				if(pin.owner.type == BlockType.CLB)
				{
					int index = indexMap.get((Clb)pin.owner);
					netClbIndices.add(index);
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
					double xPosition = pin.owner.getSite().x;
					double yPosition = pin.owner.getSite().y;
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
			
			//Add connection beween min and max
			if(!(minXIndex == -1 && maxXIndex == -1))
			{
				double delta = maxX - minX;
				if(delta < 0.005)
				{
					delta = 0.005;
				}
				double weight = ((double)2/(nbPins-1)) * (1/delta);
				if(!firstSolve)
				{
					weight *= timingWeightFactor;
				}
				if(maxXIndex == -1)
				{
					//maxX fixed but minX not
					xMatrix.setElement(minXIndex, minXIndex, xMatrix.getElement(minXIndex, minXIndex) + weight);
					xVector[minXIndex] = xVector[minXIndex] + weight*maxX;
				}
				else
				{
					if(minXIndex == -1)
					{
						//minX fixed but maxX not
						xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weight);
						xVector[maxXIndex] = xVector[maxXIndex] + weight*minX;
					}
					else
					{
						//neither of both fixed
						xMatrix.setElement(minXIndex, minXIndex, xMatrix.getElement(minXIndex, minXIndex) + weight);
						xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weight);
						xMatrix.setElement(minXIndex, maxXIndex, xMatrix.getElement(minXIndex, maxXIndex) - weight);
						xMatrix.setElement(maxXIndex, minXIndex, xMatrix.getElement(maxXIndex, minXIndex) - weight);
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
				if(!firstSolve)
				{
					weight *= timingWeightFactor;
				}
				if(maxYIndex == -1)
				{
					//maxX fixed but minX not
					yMatrix.setElement(minYIndex, minYIndex, yMatrix.getElement(minYIndex, minYIndex) + weight);
					yVector[minYIndex] = yVector[minYIndex] + weight*maxY;
				}
				else
				{
					if(minYIndex == -1)
					{
						//minX fixed but maxX not
						yMatrix.setElement(maxYIndex, maxYIndex, yMatrix.getElement(maxYIndex, maxYIndex) + weight);
						yVector[maxYIndex] = yVector[maxYIndex] + weight*minY;
					}
					else
					{
						//neither of both fixed
						yMatrix.setElement(minYIndex, minYIndex, yMatrix.getElement(minYIndex, minYIndex) + weight);
						yMatrix.setElement(maxYIndex, maxYIndex, yMatrix.getElement(maxYIndex, maxYIndex) + weight);
						yMatrix.setElement(minYIndex, maxYIndex, yMatrix.getElement(minYIndex, maxYIndex) - weight);
						yMatrix.setElement(maxYIndex, minYIndex, yMatrix.getElement(maxYIndex, minYIndex) - weight);
					}
				}
			}
			
			//Add connections internal to max and min
			for(Integer index:netClbIndices)
			{
				if(index != minXIndex)
				{
					double deltaMaxX = Math.abs(linearX[index] - maxX);
					if(deltaMaxX < 0.005)
					{
						deltaMaxX = 0.005;
					}
					double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
					if(!firstSolve)
					{
						weightMaxX *= timingWeightFactor;
					}
					if(maxXIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMaxX);
						xVector[index] = xVector[index] + weightMaxX*maxX;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(maxXIndex == index))
						{
							xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMaxX);
							xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weightMaxX);
							xMatrix.setElement(index, maxXIndex, xMatrix.getElement(index, maxXIndex) - weightMaxX); //This line contains the problem
							xMatrix.setElement(maxXIndex, index, xMatrix.getElement(maxXIndex, index) - weightMaxX);
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
					if(!firstSolve)
					{
						weightMinX *= timingWeightFactor;
					}
					if(minXIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMinX);
						xVector[index] = xVector[index] + weightMinX*minX;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(minXIndex == index))
						{
							xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMinX);
							xMatrix.setElement(minXIndex, minXIndex, xMatrix.getElement(minXIndex, minXIndex) + weightMinX);
							xMatrix.setElement(index, minXIndex, xMatrix.getElement(index, minXIndex) - weightMinX);
							xMatrix.setElement(minXIndex, index, xMatrix.getElement(minXIndex, index) - weightMinX);
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
					if(!firstSolve)
					{
						weightMaxY *= timingWeightFactor;
					}
					if(maxYIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						yMatrix.setElement(index, index, yMatrix.getElement(index, index) + weightMaxY);
						yVector[index] = yVector[index] + weightMaxY*maxY;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(maxYIndex == index))
						{
							yMatrix.setElement(index, index, yMatrix.getElement(index, index) + weightMaxY);
							yMatrix.setElement(maxYIndex, maxYIndex, yMatrix.getElement(maxYIndex, maxYIndex) + weightMaxY);
							yMatrix.setElement(index, maxYIndex, yMatrix.getElement(index, maxYIndex) - weightMaxY);
							yMatrix.setElement(maxYIndex, index, yMatrix.getElement(maxYIndex, index) - weightMaxY);
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
					if(!firstSolve)
					{
						weightMinY *= timingWeightFactor;
					}
					if(minYIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						yMatrix.setElement(index, index, yMatrix.getElement(index, index) + weightMinY);
						yVector[index] = yVector[index] + weightMinY*minY;
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(minYIndex == index))
						{
							yMatrix.setElement(index, index, yMatrix.getElement(index, index) + weightMinY);
							yMatrix.setElement(minYIndex, minYIndex, yMatrix.getElement(minYIndex, minYIndex) + weightMinY);
							yMatrix.setElement(index, minYIndex, yMatrix.getElement(index, minYIndex) - weightMinY);
							yMatrix.setElement(minYIndex, index, yMatrix.getElement(minYIndex, index) - weightMinY);
						}
					}
				}
			}
			
			boolean firstMax = true;
			boolean firstMin = true;
			for(double fixedXPosition:fixedXPositions)
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
						if(!firstSolve)
						{
							weightMaxX *= timingWeightFactor;
						}
						//Connection between fixed and non fixed block
						xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weightMaxX);
						xVector[maxXIndex] = xVector[maxXIndex] + weightMaxX*fixedXPosition;
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
						if(!firstSolve)
						{
							weightMinX *= timingWeightFactor;
						}
						//Connection between fixed and non fixed block
						xMatrix.setElement(minXIndex, minXIndex, xMatrix.getElement(minXIndex, minXIndex) + weightMinX);
						xVector[minXIndex] = xVector[minXIndex] + weightMinX*fixedXPosition;
					}
				}
				else
				{
					firstMin = false;
				}
			}
			
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
						if(!firstSolve)
						{
							weightMaxY *= timingWeightFactor;
						}
						//Connection between fixed and non fixed block
						yMatrix.setElement(maxYIndex, maxYIndex, yMatrix.getElement(maxYIndex, maxYIndex) + weightMaxY);
						yVector[maxYIndex] = yVector[maxYIndex] + weightMaxY*fixedYPosition;
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
						if(!firstSolve)
						{
							weightMinY *= timingWeightFactor;
						}
						//Connection between fixed and non fixed block
						yMatrix.setElement(minYIndex, minYIndex, yMatrix.getElement(minYIndex, minYIndex) + weightMinY);
						yVector[minYIndex] = yVector[minYIndex] + weightMinY*fixedYPosition;
					}
				}
				else
				{
					firstMin = false;
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
