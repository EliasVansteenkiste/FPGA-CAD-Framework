package placers.analyticalplacer.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.Rplace;
import placers.SAPlacer.Swap;
import placers.old.CostCalculator;
import placers.old.PlacementManipulator;
import placers.old.PlacementManipulatorIOCLB;
import architecture.FourLutSanitized;
import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;

/*
 * When expanding area of a cluster adds blocks in expanded area
 * Doesn't accept overlap at all in intermediate results
 */
public class AnalyticalPlacerTwo 
{
	
	private FourLutSanitized architecture;
	private PackedCircuit circuit;
	private Map<Clb,Integer> indexMap; //Maps an index to a Clb
	private double[] linearX;
	private double[] linearY;
	private int[] legalX;
	private int[] legalY;
	private int[] bestLegalX;
	private int[] bestLegalY;
	private double utilizationFactor;
	private int minimalX;
	private int maximalX;
	private int minimalY;
	private int maximalY;
	private CostCalculator calculator;
	private double currentCost;
	
	private final double ALPHA = 0.3;
	
	public AnalyticalPlacerTwo(FourLutSanitized architecture, PackedCircuit circuit, CostCalculator calculator)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.calculator = calculator;
		this.utilizationFactor = 0.90;
		this.currentCost = Double.MAX_VALUE;
		minimalX = 1;
		maximalX = architecture.getWidth();
		minimalY = 1;
		maximalY = architecture.getHeight();
	}
	
	public void place()
	{
		Rplace.placeCLBsandFixedIOs(circuit, architecture, new Random(1));
		initializeDataStructures();
		
		//Initial linear solves, should normally be done 5-7 times
		for(int i = 0; i < 20; i++)
		{
			solveLinear(true, 0.0);
		}
		
		//Initial legalization
//		System.out.println("Legalizing...");
		clusterCutSpreadRecursive();
		updateBestLegal();
		
		//Iterative solves with pseudonets
		for(int i = 0; i < 30; i++)
		{
//			System.out.println("SOLVE " + i);
			solveLinear(false, (i+1)*ALPHA);
			clusterCutSpreadRecursive();
			updateBestLegal();
		}
		
		updateCircuit(true);
		
		int nbAttempts = 5000;
		iterativeRefinement(nbAttempts);
		
		double cost = calculateTotalCost(bestLegalX, bestLegalY);
		System.out.println("COST BEFORE REFINEMENT = " + cost);
	}
	
	private double calculateTotalCost(int[] xArray, int[] yArray)
	{
		double cost = 0.0;
		for(Net net:circuit.nets.values())
		{
			int minX;
			int maxX;
			int minY;
			int maxY;
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
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 */
	private void solveLinear(boolean firstSolve, double pseudoWeight)
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
			for(int i = 0; i < dimension; i++)
			{
				xMatrix.setElement(i, i, xMatrix.getElement(i, i) + pseudoWeight);
				xVector[i] += pseudoWeight * bestLegalX[i];
				yMatrix.setElement(i, i, yMatrix.getElement(i, i) + pseudoWeight);
				yVector[i] += pseudoWeight*bestLegalY[i];
			}
		}
		
		//Build the linear systems (x and y are solved separately)
		for(Net net:circuit.getNets().values())
		{
			ArrayList<Integer> netClbIndices = new ArrayList<>();
			ArrayList<Double> fixedXPositions = new ArrayList<>();
			ArrayList<Double> fixedYPositions = new ArrayList<>();
			int nbPins = 1 + net.sinks.size();
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
				if(delta == 0.0)
				{
					delta = 0.001;
				}
				double weight = ((double)2/(nbPins-1)) * (1/delta);
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
				if(delta == 0.0)
				{
					delta = 0.001;
				}
				double weight = ((double)2/(nbPins-1)) * (1/delta);
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
					if(deltaMaxX == 0.0)
					{
						deltaMaxX = 0.001;
					}
					double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
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
					if(deltaMinX == 0.0)
					{
						//System.out.println("Problem 4");
						deltaMinX = 0.001;
					}
					double weightMinX = ((double)2/(nbPins-1)) * (1/deltaMinX);
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
					if(deltaMaxY == 0.0)
					{
						deltaMaxY = 0.001;
					}
					double weightMaxY = ((double)2/(nbPins-1)) * (1/deltaMaxY);
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
					if(deltaMinY == 0.0)
					{
						deltaMinY = 0.001;
					}
					double weightMinY = ((double)2/(nbPins-1)) * (1/deltaMinY);
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
						if(deltaMaxX == 0.0)
						{
							deltaMaxX = 0.001;
						}
						double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
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
						if(deltaMinX == 0.0)
						{
							deltaMinX = 0.001;
						}
						double weightMinX = ((double)2/(nbPins-1)) * (1/deltaMinX);
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
						if(deltaMaxY == 0.0)
						{
							deltaMaxY = 0.001;
						}
						double weightMaxY = ((double)2/(nbPins-1)) * (1/deltaMaxY);
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
						if(deltaMinY == 0.0)
						{
							deltaMinY = 0.001;
						}
						double weightMinY = ((double)2/(nbPins-1)) * (1/deltaMinY);
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
		
		linearX = xSolution;
		linearY = ySolution;
	}
	
	private void clusterCutSpreadRecursive()
	{
		int[] semiLegalXPositions = new int[linearX.length];
		int[] semiLegalYPositions = new int[linearX.length];
		List<Integer> todo = new ArrayList<>();
		for(int i = 0; i < linearX.length; i++)
		{
			todo.add(i);
		}
		while(todo.size() != 0)
		{
			//Cluster
			int areaXUpBound = 0;
			int areaXDownBound = 0;
			int areaYUpBound = 0;
			int areaYDownBound = 0;
			List<Integer> indices = new ArrayList<>();
			List<Double> positionsX = new ArrayList<>();
			List<Double> positionsY = new ArrayList<>();
			
			//Find a starting point for the cluster
			int startIndex = todo.get(0);
			areaXUpBound = (int)Math.floor(linearX[startIndex] + 1.0);
			areaXDownBound = (int)Math.floor(linearX[startIndex]);
			areaYUpBound = (int)Math.floor(linearY[startIndex] + 1.0);
			areaYDownBound = (int)Math.floor(linearY[startIndex]);
			indices.add(startIndex);
			positionsX.add(linearX[startIndex]);
			positionsY.add(linearY[startIndex]);
			todo.remove(0);
			for(int i = 0; i < todo.size(); i++)
			{
				int currentIndex = todo.get(i);
				if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
				{
					indices.add(currentIndex);
					positionsX.add(linearX[currentIndex]);
					positionsY.add(linearY[currentIndex]);
					todo.remove(i);
					i--;
				}
			}

			//Grow cluster until it is surrounded by non overutilized areas
			boolean expanded = false;
			if(indices.size() > 1)
			{
				expanded = true;
			}
			while(expanded)
			{
				expanded = false;
				//Check if need to grow to the right
				boolean addRight = false;
				for(int y = areaYDownBound; y < areaYUpBound; y++)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXUpBound && linearX[currentIndex] < areaXUpBound+1 && linearY[currentIndex] >= y && linearY[currentIndex] < y+1)
						{
							nbCells++;
							if(nbCells >= 2)
							{
								addRight = true;
								break;
							}
						}
					}
					if(addRight)
					{
						break;
					}
				}
				if(addRight)
				{
					areaXUpBound += 1;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}
				
				//Check if need to grow to the top
				boolean addTop = false;
				for(int x = areaXDownBound; x < areaXUpBound; x++)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= x && linearX[currentIndex] < x+1 && linearY[currentIndex] >= areaYDownBound-1 && linearY[currentIndex] < areaYDownBound)
						{
							nbCells++;
							if(nbCells >= 2)
							{
								addTop = true;
								break;
							}
						}
					}
					if(addTop)
					{
						break;
					}
				}
				if(addTop)
				{
					areaYDownBound -= 1;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}
				
				//Check if need to grow to the left
				boolean addLeft = false;
				for(int y = areaYDownBound; y < areaYUpBound; y++)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBound-1 && linearX[currentIndex] < areaXDownBound && linearY[currentIndex] >= y && linearY[currentIndex] < y+1)
						{
							nbCells++;
							if(nbCells >= 2)
							{
								addLeft = true;
								break;
							}
						}
					}
					if(addLeft)
					{
						break;
					}
				}
				if(addLeft)
				{
					areaXDownBound -= 1;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}
				
				//Check if need to grow to the bottom
				boolean addBottom = false;
				for(int x = areaXDownBound; x < areaXUpBound; x++)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= x && linearX[currentIndex] < x+1 && linearY[currentIndex] >= areaYUpBound && linearY[currentIndex] < areaYUpBound+1)
						{
							nbCells++;
							if(nbCells >= 2)
							{
								addBottom = true;
								break;
							}
						}
					}
					if(addBottom)
					{
						break;
					}
				}
				if(addBottom)
				{
					areaYUpBound += 1;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}
				
			}
			
//			System.out.println("\n\nINDICES:");
//			for(int index:indices)
//			{
//				System.out.print(index + " ");
//			}
//			System.out.println("\nX_LOCATIONS:");
//			for(double x:positionsX)
//			{
//				System.out.printf("%.3f ", x);
//			}
//			System.out.println("\nYLOCATIONS:");
//			for(double y:positionsY)
//			{
//				System.out.printf("%.3f ", y);
//			}
//			System.out.println("\nXDown: " + areaXDownBound + ", XUp: " + areaXUpBound + ", YDown: " + areaYDownBound + ", YUp: " + areaYUpBound);
			
			
			
			
			if(indices.size() == 1)
			{
				int x = (int)Math.round(positionsX.get(0));
				int y = (int)Math.round(positionsY.get(0));
				int index = indices.get(0);
				semiLegalXPositions[index] = x;
				semiLegalYPositions[index] = y;
				//System.out.printf("Index: %d, X: %d, Y: %d\n", indices.get(0), x, y);
				//System.out.println();
				continue;
			}
			
			
			
			
			//Grow area until not overutilized
			double curUtilization = (double)positionsX.size() / (double)((areaXUpBound - areaXDownBound) * (areaYUpBound - areaYDownBound));
			//System.out.printf("Utilization: %.3f\n", curUtilization);
			int curDirection = 0; //0 = right, 1 = top, 2 = left, 3 = bottom
			while(curUtilization >= utilizationFactor)
			{
				switch(curDirection)
				{
					case 0: //Grow to the right if possible
						if(areaXUpBound <= maximalX)
						{
							areaXUpBound += 1;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
								{
									indices.add(currentIndex);
									positionsX.add(linearX[currentIndex]);
									positionsY.add(linearY[currentIndex]);
									todo.remove(i);
									i--;
								}
							}
						}
						break;
					case 1: //Grow to the top if possible
						if(areaYDownBound >= minimalY)
						{
							areaYDownBound -= 1;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
								{
									indices.add(currentIndex);
									positionsX.add(linearX[currentIndex]);
									positionsY.add(linearY[currentIndex]);
									todo.remove(i);
									i--;
								}
							}
						}
						break;
					case 2: //Grow to the left if possible
						if(areaXDownBound >= minimalX)
						{
							areaXDownBound -= 1;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
								{
									indices.add(currentIndex);
									positionsX.add(linearX[currentIndex]);
									positionsY.add(linearY[currentIndex]);
									todo.remove(i);
									i--;
								}
							}
						}
						break;
					default: //Grow to the bottom if possible
						if(areaYUpBound <= maximalY)
						{
							areaYUpBound += 1;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBound && linearX[currentIndex] < areaXUpBound && linearY[currentIndex] >= areaYDownBound && linearY[currentIndex] < areaYUpBound)
								{
									indices.add(currentIndex);
									positionsX.add(linearX[currentIndex]);
									positionsY.add(linearY[currentIndex]);
									todo.remove(i);
									i--;
								}
							}
						}
						break;
				}
				curUtilization = (double)positionsX.size() / (double)((areaXUpBound - areaXDownBound) * (areaYUpBound - areaYDownBound));
				curDirection++;
				curDirection %= 4;
			}
			//System.out.println("New limits: XDown: " + areaXDownBound + ", XUp: " + areaXUpBound + ", YDown: " + areaYDownBound + ", YUp: " + areaYUpBound);
			//System.out.printf("New utilization: %.3f\n", curUtilization);
			
			
			
			
			//Cut and spread
			boolean cutDir = true; //Initial cut is horizontally
			if(areaYUpBound - areaYDownBound <= 1) //Check if it is possible to cut horizontally
			{
				cutDir = false; //Cut vertically if not possible to cut horizontally
			}
			cutAndSpread(cutDir, indices, positionsX, positionsY, areaXDownBound, areaXUpBound, areaYDownBound, areaYUpBound, 
							semiLegalXPositions, semiLegalYPositions); 

			//System.out.println();
		}
		
		//System.out.println("Started final legalization...");
		finalLegalization(semiLegalXPositions, semiLegalYPositions);
		
	}
	
	/*
	 * Cut and spread recursively
	 * horCutDirection = true ==> cut horizontally, horCutDirection = false ==> cut vertically
	 */
	private void cutAndSpread(boolean horCutDirection, List<Integer> indices, List<Double> positionsX, List<Double> positionsY, 
								int areaXDownBound, int areaXUpBound, int areaYDownBound, int areaYUpBound, 
								int[] semiLegalXPositions, int[] semiLegalYPositions)
	{
		//Bug checks
		if(areaXDownBound >= areaXUpBound || areaYDownBound >= areaYUpBound)
		{
			System.err.println("ERROR: A DOWNBOUND IS BIGGER THAN OR EQUAL TO AN UPBOUND");
		}
		
		//Sort
		if(horCutDirection) //Cut horizontally => sort in i
		{
			sort(false, indices, positionsX, positionsY);
		}
		else //Cut vertically
		{
			sort(true, indices, positionsX, positionsY);
		}
		
		//Source cut
		int endIndex = indices.size();
		int middleIndex = endIndex / 2; //If odd ==> one more in last set than in first set
		List<Integer> indices1 = new ArrayList<>();
		List<Double> positionsX1 = new ArrayList<>();
		List<Double> positionsY1 = new ArrayList<>();
		for(int i = 0; i < middleIndex; i++)
		{
			indices1.add(indices.get(i));
			positionsX1.add(positionsX.get(i));
			positionsY1.add(positionsY.get(i));
		}
		List<Integer> indices2 = new ArrayList<>();
		List<Double> positionsX2 = new ArrayList<>();
		List<Double> positionsY2 = new ArrayList<>();
		for(int i = middleIndex; i < endIndex; i++)
		{
			indices2.add(indices.get(i));
			positionsX2.add(positionsX.get(i));
			positionsY2.add(positionsY.get(i));
		}
		
		//Target cut
		int cutPosition;
		if(horCutDirection) //Cut horizontally
		{
			cutPosition = (areaYDownBound + areaYUpBound) / 2;
			double utilizationTop = (double)positionsY1.size() / (double)((areaXUpBound - areaXDownBound) * (cutPosition - areaYDownBound));
			while(utilizationTop > 1.0) //Move blocks from top to bottom
			{
				int indexToShift = indices1.remove(indices1.size() - 1);
				indices2.add(0, indexToShift);
				double xPosToShift = positionsX1.remove(positionsX1.size() - 1);
				positionsX2.add(0, xPosToShift);
				double yPosToShift = positionsY1.remove(positionsY1.size() - 1);
				positionsY2.add(0, yPosToShift);
				utilizationTop = (double)positionsY1.size() / (double)((areaXUpBound - areaXDownBound) * (cutPosition - areaYDownBound));
			}
			double utilizationBottom = (double)positionsY2.size() / (double)((areaXUpBound - areaXDownBound) * (areaYUpBound - cutPosition));
			if(utilizationBottom > 1.0 || utilizationTop > 1.0)
			{
				System.err.println("AN ERROR OCCURRED WHILE CUTTING!");
			}
			else
			{
				//System.out.println("Horizontal cut y-position: " + cutPosition);
				//System.out.printf("Top utilization: %.3f\n", utilizationTop);
				//System.out.printf("Bottom utilization: %.3f\n", utilizationBottom);
			}
		}
		else //Cut vertically
		{
			cutPosition = (areaXDownBound + areaXUpBound) / 2;
			double utilizationLeft = (double)positionsX1.size() / (double)((cutPosition - areaXDownBound) * (areaYUpBound - areaYDownBound));
			while(utilizationLeft > 1.0) //Move blocks from left to right
			{
				int indexToShift = indices1.remove(indices1.size() - 1);
				indices2.add(0, indexToShift);
				double xPosToShift = positionsX1.remove(positionsX1.size() - 1);
				positionsX2.add(0, xPosToShift);
				double yPosToShift = positionsY1.remove(positionsY1.size() - 1);
				positionsY2.add(0, yPosToShift);
				utilizationLeft = (double)positionsX1.size() / (double)((cutPosition - areaXDownBound) * (areaYUpBound - areaYDownBound));
			}
			double utilizationRight = (double)positionsX2.size() / (double)((areaXUpBound - cutPosition) * (areaYUpBound - areaYDownBound));
			if(utilizationRight > 1.0 || utilizationLeft > 1.0)
			{
				System.err.println("AN ERROR OCCURRED WHILE CUTTING!");
			}
			else
			{
				//System.out.println("Vertical cut x-position: " + cutPosition);
				//System.out.printf("Left utilization: %.3f\n", utilizationLeft);
				//System.out.printf("Right utilization: %.3f\n", utilizationRight);
			}
		}
		
		//Recursive calls if necessary (check for base cases)
		if(indices1.size() > 1) //Do recursive call
		{
			if(horCutDirection)
			{
				boolean nextCut = !horCutDirection; //Next cut will be vertical
				if(areaXUpBound - areaXDownBound <= 1) //Next cut will be vertical ==> check if it will be possible to cut vertically
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices1, positionsX1, positionsY1, areaXDownBound, areaXUpBound, areaYDownBound, cutPosition, 
								semiLegalXPositions, semiLegalYPositions);
			}
			else
			{
				boolean nextCut = !horCutDirection; //Next cut will be horizontal
				if(areaYUpBound - areaYDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices1, positionsX1, positionsY1, areaXDownBound, cutPosition, areaYDownBound, areaYUpBound, 
								semiLegalXPositions, semiLegalYPositions);
			}
		}
		else //Snap to grid
		{

			int x;
			int y;
			if(positionsX1.get(0) <= areaXDownBound)
			{
				x = areaXDownBound;
			}
			else
			{
				if(horCutDirection)
				{
					if(positionsX1.get(0) >= areaXUpBound - 1)
					{
						x = areaXUpBound - 1;
					}
					else
					{
						x = (int)Math.round(positionsX1.get(0));
					}
				}
				else
				{
					if(positionsX1.get(0) >= cutPosition - 1)
					{
						x = cutPosition - 1;
					}
					else
					{
						x = (int)Math.round(cutPosition - 1);
					}
				}
			}
			if(positionsY2.get(0) <= areaYDownBound)
			{
				y = areaYDownBound;
			}
			else
			{
				if(horCutDirection)
				{
					if(positionsY2.get(0) >= cutPosition - 1)
					{	
						y = cutPosition - 1;
					}
					else
					{
						y = (int)Math.round(positionsY2.get(0));
					}
				}
				else
				{
					if(positionsY2.get(0) >= areaYUpBound - 1)
					{	
						y = areaYUpBound - 1;
					}
					else
					{
						y = (int)Math.round(positionsY2.get(0));
					}
				}
			}
			int index = indices1.get(0);
			//System.out.printf("Index: %d, X: %d, Y: %d\n", index, x, y);
			semiLegalXPositions[index] = x;
			semiLegalYPositions[index] = y;
		}
		if(indices2.size() > 1) //Do recursive call
		{
			if(horCutDirection)
			{
				boolean nextCut = !horCutDirection; //Next cut will be vertical
				if(areaXUpBound - areaXDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices2, positionsX2, positionsY2, areaXDownBound, areaXUpBound, cutPosition, areaYUpBound, 
								semiLegalXPositions, semiLegalYPositions);
			}
			else
			{
				boolean nextCut = !horCutDirection; //Next cut will be horizontal
				if(areaYUpBound - areaYDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices2, positionsX2, positionsY2, cutPosition, areaXUpBound, areaYDownBound, areaYUpBound, 
								semiLegalXPositions, semiLegalYPositions);
			}
		}
		else //Snap to grid
		{
			int x;
			int y;
			if(positionsX2.get(0) >= areaXUpBound - 1)
			{
				x = areaXUpBound - 1;
			}
			else
			{
				if(horCutDirection)
				{
					if(positionsX2.get(0) <= areaXDownBound)
					{
						x = areaXDownBound;
					}
					else
					{
						x = (int)Math.round(positionsX2.get(0));
					}
				}
				else
				{
					if(positionsX2.get(0) <= cutPosition)
					{
						x = cutPosition;
					}
					else
					{
						x = (int)Math.round(positionsX2.get(0));
					}
				}
			}
			if(positionsY2.get(0) >= areaYUpBound - 1)
			{
				y = areaYUpBound - 1;
			}
			else
			{
				if(horCutDirection)
				{
					if(positionsY2.get(0) <= cutPosition)
					{
						y = cutPosition;
					}
					else
					{
						y = (int)Math.round(positionsY2.get(0));
					}
				}
				else
				{
					if(positionsY2.get(0) <= areaYDownBound)
					{
						y = areaYDownBound;
					}
					else
					{
						y = (int)Math.round(positionsY2.get(0));
					}
				}
			}
			int index = indices2.get(0);
			//System.out.printf("Index: %d, X: %d, Y: %d\n", index, x, y);
			semiLegalXPositions[index] = x;
			semiLegalYPositions[index] = y;
		}
	}
	
	/*
	 * Eliminates the final overlaps (between different clusters)
	 * Works from left to right
	 */
	private void finalLegalization(int[] semiLegalXPositions, int[] semiLegalYPositions)
	{
		int[] semiLegalIndices = new int[semiLegalXPositions.length];
		for(int i = 0; i < semiLegalIndices.length; i++)
		{
			semiLegalIndices[i] = i;
		}
		
		//System.out.println("BEFORE FINAL LEGALIZATION:");
		//for(int i = 0; i < semiLegalIndices.length; i++)
		//{
			//System.out.printf("Index: %d, X: %d, Y: %d\n", semiLegalIndices[i], semiLegalXPositions[i], semiLegalYPositions[i]);
		//}
		
		sort(true, semiLegalIndices, semiLegalXPositions, semiLegalYPositions); //Sort in x direction
		
		int ySize = maximalY - minimalY + 1;
		int xSize = maximalX - minimalX + 1;
		boolean[][] occupied = new boolean[ySize][xSize]; //True if CLB site is occupied, false if not
		for(int i = 0; i < ySize; i++)
		{
			for(int j = 0; j < xSize; j++)
			{
				occupied[i][j] = false;
			}
		}
		
		for(int i = 0; i < semiLegalIndices.length; i++)
		{
			int index = semiLegalIndices[i];
			int x = semiLegalXPositions[i];
			int y = semiLegalYPositions[i];
			
			//Shift to legal zone
			while(x < minimalX)
			{
				x++;
			}
			while(x > maximalX)
			{
				x--;
			}
			while(y < minimalY)
			{
				y++;
			}
			while(y > maximalY)
			{
				y--;
			}
			
			//Check if there's overlap
			if(!occupied[y-minimalY][x-minimalX])
			{
				occupied[y-minimalY][x-minimalX] = true;
				legalX[index] = x;
				legalY[index] = y;
			}
			else //Eliminate overlap
			{
				//Look around for free spot ==> counterclockwise with increasing boxSize till we find available position
				int currentX = x;
				int currentY = y-1;
				int curBoxSize = 1;
				boolean xDir = true; //true = x-direction, false = y-direction
				int moveSpeed = -1; //Always +1 or -1
				//System.out.println("Need to search around X = " + x + " and Y = " + y);
				while(currentX < minimalX || currentX > maximalX || currentY < minimalY || currentY > maximalY || occupied[currentY-minimalY][currentX-minimalX])
				{
					//System.out.println("CurBoxSize: " + curBoxSize);
					//System.out.println("X = " + currentX + ", Y = " + currentY + " is not free");
					if(xDir && currentX == x-curBoxSize) //Check if we reached top left corner
					{
						//System.out.println("Here 1");
						xDir = false;
						moveSpeed = 1;
						currentY = y - curBoxSize + 1;
					}
					else
					{
						if(!xDir && currentY == y+curBoxSize) //Check if we reached bottom left corner
						{
							//System.out.println("Here 2");
							xDir = true;
							moveSpeed = 1;
							currentX = x - curBoxSize + 1;
						}
						else
						{
							if(xDir && currentX == x+curBoxSize) //Check if we reached bottom right corner
							{
								//System.out.println("Here 3");
								xDir = false;
								moveSpeed = -1;
								currentY = y + curBoxSize -1;
							}
							else
							{
								if(!xDir && currentY == y-curBoxSize) //Check if we reached top right corner
								{
									//System.out.println("Here 4");
									xDir = true;
									moveSpeed = -1;
									currentX = x + curBoxSize - 1;
									if(currentX == x && currentY == y - curBoxSize) //We've went completely around the box and didn't find an available position ==> increas box size
									{
										curBoxSize++;
										currentX = x;
										currentY = y-curBoxSize;
										xDir = true;
										moveSpeed = -1;
									}
								}
								else // We didn't reach a corner and just have to keep moving
								{
									if(xDir) //Move in x-direction
									{
										currentX += moveSpeed;
									}
									else //Move in y-direction
									{
										currentY += moveSpeed;
									}
									if(currentX == x && currentY == y - curBoxSize) //We've went completely around the box and didn't find an available position ==> increas box size
									{
										curBoxSize++;
										currentX = x;
										currentY = y-curBoxSize;
										xDir = true;
										moveSpeed = -1;
									}
								}
							}
						}
					}
				}
				occupied[currentY-minimalY][currentX-minimalX] = true;
				legalX[index] = currentX;
				legalY[index] = currentY;
			}
		}
	}
	
	/*
	 * Eliminates the final overlaps (between different clusters)
	 */
	private void finalLegalizationTwo(int[] semiLegalXPositions, int[] semiLegalYPositions)
	{
		List<Integer> remainingIndices = new ArrayList<>();
		int ySize = maximalY - minimalY + 1;
		int xSize = maximalX - minimalX + 1;
		boolean[][] occupied = new boolean[ySize][xSize]; //True if CLB site is occupied, false if not
		
		for(int i = 0; i < ySize; i++)
		{
			for(int j = 0; j < xSize; j++)
			{
				occupied[i][j] = false;
			}
		}
		
		for(int i = 0; i < semiLegalXPositions.length; i++)
		{
			int x = semiLegalXPositions[i];
			int y = semiLegalYPositions[i];
			
			//Shift to legal zone
			while(x < minimalX)
			{
				x++;
			}
			while(x > maximalX)
			{
				x--;
			}
			while(y < minimalY)
			{
				y++;
			}
			while(y > maximalY)
			{
				y--;
			}
			
			//Check if there's overlap
			if(!occupied[y-minimalY][x-minimalX])
			{
				occupied[y-minimalY][x-minimalX] = true;
				legalX[i] = x;
				legalY[i] = y;
			}
			else //Add to remaining indices list
			{
				remainingIndices.add(i);
			}
		}
		
		for(int index:remainingIndices)
		{
			//Look around for free spot ==> counterclockwise with increasing boxSize till we find available position
			int x = semiLegalXPositions[index];
			int y = semiLegalYPositions[index];
			int currentX = x;
			int currentY = y-1;
			int curBoxSize = 1;
			boolean xDir = true; //true = x-direction, false = y-direction
			int moveSpeed = -1; //Always +1 or -1
			//System.out.println("Need to search around X = " + x + " and Y = " + y);
			while(currentX < minimalX || currentX > maximalX || currentY < minimalY || currentY > maximalY || occupied[currentY-minimalY][currentX-minimalX])
			{
				//System.out.println("CurBoxSize: " + curBoxSize);
				//System.out.println("X = " + currentX + ", Y = " + currentY + " is not free");
				if(xDir && currentX == x-curBoxSize) //Check if we reached top left corner
				{
					//System.out.println("Here 1");
					xDir = false;
					moveSpeed = 1;
					currentY = y - curBoxSize + 1;
				}
				else
				{
					if(!xDir && currentY == y+curBoxSize) //Check if we reached bottom left corner
					{
						//System.out.println("Here 2");
						xDir = true;
						moveSpeed = 1;
						currentX = x - curBoxSize + 1;
					}
					else
					{
						if(xDir && currentX == x+curBoxSize) //Check if we reached bottom right corner
						{
							//System.out.println("Here 3");
							xDir = false;
							moveSpeed = -1;
							currentY = y + curBoxSize -1;
						}
						else
						{
							if(!xDir && currentY == y-curBoxSize) //Check if we reached top right corner
							{
								//System.out.println("Here 4");
								xDir = true;
								moveSpeed = -1;
								currentX = x + curBoxSize - 1;
								if(currentX == x && currentY == y - curBoxSize) //We've went completely around the box and didn't find an available position ==> increas box size
								{
									curBoxSize++;
									currentX = x;
									currentY = y-curBoxSize;
									xDir = true;
									moveSpeed = -1;
								}
							}
							else // We didn't reach a corner and just have to keep moving
							{
								if(xDir) //Move in x-direction
								{
									currentX += moveSpeed;
								}
								else //Move in y-direction
								{
									currentY += moveSpeed;
								}
								if(currentX == x && currentY == y - curBoxSize) //We've went completely around the box and didn't find an available position ==> increas box size
								{
									curBoxSize++;
									currentX = x;
									currentY = y-curBoxSize;
									xDir = true;
									moveSpeed = -1;
								}
							}
						}
					}
				}
			}
			occupied[currentY-minimalY][currentX-minimalX] = true;
			legalX[index] = currentX;
			legalY[index] = currentY;
		}
	}
	
	private void updateBestLegal()
	{
		if(bestLegalX == null) //This is the first time ==> current legal placement is best
		{
			bestLegalX = new int[legalX.length];
			bestLegalY = new int[legalY.length];
			for(int i = 0; i < legalX.length; i++)
			{
				bestLegalX[i] = legalX[i];
				bestLegalY[i] = legalY[i];
			}
		}
		else
		{
			//Calculate cost current placement
			updateCircuit(false);
			double newCost = calculator.calculateTotalCost();
			if(newCost < currentCost)
			{
				currentCost = newCost;
				for(int i = 0; i < legalX.length; i++)
				{
					bestLegalX[i] = legalX[i];
					bestLegalY[i] = legalY[i];
				}
			}
		}
	}
	
	private void updateCircuit(boolean finalTime)
	{
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
			Site site;
			if(finalTime)
			{
				site = architecture.getSite(bestLegalX[index], bestLegalY[index], 0);
			}
			else
			{
				site = architecture.getSite(legalX[index], legalY[index], 0);
			}
			site.block = clb;
			clb.setSite(site);
		}
	}
	
	private void iterativeRefinement(int nbAttempts)
	{
		PlacementManipulator manipulator = new PlacementManipulatorIOCLB(architecture, circuit);
		for(int i = 0; i < nbAttempts; i++)
		{
			Swap swap;
			swap=manipulator.findSwap(3);
			if((swap.pl1.block == null || (!swap.pl1.block.fixed)) && (swap.pl2.block == null || (!swap.pl2.block.fixed)))
			{
				double deltaCost = calculator.calculateDeltaCost(swap);
				if(deltaCost<=0) //Only accept the move if it improves the total cost
				{
					swap.apply();
				}
			}
		}
	}
	
	/*
	 * Sort in increasing order of X or Y position
	 * xDir = true ==> sort in x direction, xDir = false ==> sort in Y direction
	 */
	private void sort(boolean xDir, int[] indices, int[] positionsX, int[] positionsY)
	{
		for(int i = 0; i < indices.length; i++)
		{
			int minIndex = i;
			int minValue;
			if(xDir)
			{
				minValue = positionsX[i];
			}
			else
			{
				minValue = positionsY[i];
			}
			for(int j = i+1; j < indices.length; j++)
			{
				if(xDir && positionsX[j] < minValue)
				{
					minIndex = j;
					minValue = positionsX[j];
				}
				if(!xDir && positionsY[j] < minValue)
				{
					minIndex = j;
					minValue = positionsY[j];
				}
			}
			if(minIndex != i)
			{
				int previousIIndex = indices[i];
				int previousIX = positionsX[i];
				int previousIY = positionsY[i];
				indices[i] = indices[minIndex];
				positionsX[i] = positionsX[minIndex];
				positionsY[i] = positionsY[minIndex];
				indices[minIndex] = previousIIndex;
				positionsX[minIndex] = previousIX;
				positionsY[minIndex] = previousIY;
			}
		}
	}
	
	/*
	 * Sort in increasing order of X or Y position
	 * xDir = true ==> sort in x direction, xDir = false ==> sort in Y direction
	 */
	private void sort(boolean xDir, List<Integer> indices, List<Double> positionsX, List<Double> positionsY)
	{
		for(int i = 0; i < indices.size(); i++)
		{
			int minIndex = i;
			double minValue;
			if(xDir)
			{
				minValue = positionsX.get(i);
			}
			else
			{
				minValue = positionsY.get(i);
			}
			for(int j = i+1; j < indices.size(); j++)
			{
				if(xDir && positionsX.get(j) < minValue)
				{
					minIndex = j;
					minValue = positionsX.get(j);
				}
				if(!xDir && positionsY.get(j) < minValue)
				{
					minIndex = j;
					minValue = positionsY.get(j);
				}
			}
			if(minIndex != i) //Switch index, X-position and Y-position between i and minIndex
			{
				int previousIIndex = indices.get(i);
				double previousIX = positionsX.get(i);
				double previousIY = positionsY.get(i);
				indices.set(i, indices.get(minIndex));
				positionsX.set(i, positionsX.get(minIndex));
				positionsY.set(i, positionsY.get(minIndex));
				indices.set(minIndex, previousIIndex);
				positionsX.set(minIndex, previousIX);
				positionsY.set(minIndex, previousIY);
			}
		}
	}
	
	private void initializeDataStructures()
	{
		int dimensions = circuit.clbs.values().size();
		indexMap = new HashMap<>();;
		linearX = new double[dimensions];
		linearY = new double[dimensions];
		legalX = new int[dimensions];
		legalY = new int[dimensions];
		int index = 0;
		double xPos = 0.09;
		double yPos = 0.09;
		for(Clb clb:circuit.clbs.values())
		{
			indexMap.put(clb, index);
			//linearX[index] = clb.getSite().x;
			//linearY[index] = clb.getSite().y;
			linearX[index] = xPos;
			linearY[index] = yPos;
			xPos += 0.09;
			yPos += 0.09;
			index++;
		}
	}
	
}
