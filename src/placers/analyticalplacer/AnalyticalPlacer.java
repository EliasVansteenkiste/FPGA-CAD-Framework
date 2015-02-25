package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mathtools.CGSolver;
import mathtools.Crs;

import placers.Rplace;

import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.Input;
import circuit.Output;
import circuit.PackedCircuit;
import circuit.Net;
import circuit.Pin;

import architecture.FourLutSanitized;
import architecture.Site;

public class AnalyticalPlacer 
{
	
	private FourLutSanitized architecture;
	private PackedCircuit circuit;
	private Map<Clb,Integer> indexMap; //Maps an index to a Clb
	private double[] linearX;
	private double[] linearY;
	private double utilizationFactor;
	private int minimalX;
	private int maximalX;
	private int minimalY;
	private int maximalY;
	
	public AnalyticalPlacer(FourLutSanitized architecture, PackedCircuit circuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.utilizationFactor = 0.9;
		minimalX = 1;
		maximalX = 16;
		minimalY = 1;
		maximalY = 16;
	}
	
	public void place()
	{
		randomInitialPlace();
		initializeDataStructures();
		solveLinear(true);
		clusterCutSpreadRecursive();
	}
	
	/*
	 * Build and solve the linear system ==> recalculates linearX and linearY
	 * If it is the first time we solve the linear system ==> don't take pseudonets into account
	 */
	private void solveLinear(boolean firstSolve)
	{
		if(!firstSolve)
		{
			//Process pseudonets into account
			System.out.println("Not first solve is not supported yet!");
		}
		
		//Build the linear systems (x and y are solved separately)
		int dimension = linearX.length;
		Crs xMatrix = new Crs(dimension);
		double[] xVector = new double[dimension];
		Crs yMatrix = new Crs(dimension);
		double[] yVector = new double[dimension];
		for(Net net:circuit.getNets().values())
		{
			ArrayList<Integer> netClbIndices = new ArrayList<>();
			ArrayList<Double> fixedXPositions = new ArrayList<>();
			ArrayList<Double> fixedYPositions = new ArrayList<>();
			int nbPins = 1 + net.sinks.size();
			double minX = Double.MAX_VALUE;
			int minXIndex = -1; //Index = -1 means fixed block
			double maxX = Double.MIN_VALUE;
			int maxXIndex = -1;
			double minY = Double.MAX_VALUE;
			int minYIndex = -1;
			double maxY = Double.MIN_VALUE;
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
			
			
			if(net.name.contains("pv22_5"))
			{
				System.out.println("MAX_X: " + maxX + ",\tIndex: " + maxXIndex);
				System.out.println("MIN_X: " + minX + ",\tIndex: " + minXIndex);
				System.out.println("MAX_Y: " + maxY + ",\tIndex: " + maxYIndex);
				System.out.println("MIN_Y: " + minY + ",\tIndex: " + minYIndex);
			}
			
			
			
			
			
			//Add connection beween min and max
			if(!(minXIndex == -1 && maxXIndex == -1))
			{
				double delta = maxX - minX;
				if(delta == 0)
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
				if(delta == 0)
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
					if(deltaMaxX == 0)
					{
						deltaMaxX = 0.001;
					}
					double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
					if(maxXIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMaxX);
						xVector[index] = xVector[index] + weightMaxX*maxX;
						
						
						if(index == 0)
						{
							System.out.println("New (0,0) value: " + xMatrix.getElement(0, 0));
							if(net.name.contains("pv22_5"))
							{
								System.out.println("HERE 1");
							}
						}
						
					}
					else //maxX is not a fixed block
					{
						//Connection between two non fixed blocks
						if(!(maxXIndex == index))
						{
							xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMaxX);
							xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weightMaxX);
							xMatrix.setElement(index, maxXIndex, xMatrix.getElement(index, maxXIndex) - weightMaxX);
							xMatrix.setElement(maxXIndex, index, xMatrix.getElement(maxXIndex, index) - weightMaxX);
							
							if(index == 0 || maxXIndex == 0)
							{
								System.out.println("New (0,0) value: " + xMatrix.getElement(0, 0));
								if(net.name.contains("pv22_5"))
								{
									System.out.println("HERE 2: weight = " + weightMaxX + ", delta = " + deltaMaxX);
								}
							}
						}
					}
				}
				
				if(index != maxXIndex)
				{
					double deltaMinX = Math.abs(linearX[index] - minX);
					if(deltaMinX == 0)
					{
						deltaMinX = 0.001;
					}
					double weightMinX = ((double)2/(nbPins-1)) * (1/deltaMinX);
					if(minXIndex == -1) //maxX is a fixed block
					{
						//Connection between fixed and non fixed block
						xMatrix.setElement(index, index, xMatrix.getElement(index, index) + weightMinX);
						xVector[index] = xVector[index] + weightMinX*minX;
						
						if(index == 0)
						{
							System.out.println("New (0,0) value: " + xMatrix.getElement(0, 0));
							if(net.name.contains("pv22_5"))
							{
								System.out.println("HERE 3: weight = " + weightMinX + ", delta = " + deltaMinX);
							}
						}
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
							
							if(index == 0 || minXIndex == 0)
							{
								System.out.println("New (0,0) value: " + xMatrix.getElement(0, 0));
								if(net.name.contains("pv22_5"))
								{
									System.out.println("HERE 4");
								}
							}
						}
					}
				}
				
				if(index != minYIndex)
				{
					double deltaMaxY = Math.abs(linearY[index] - maxY);
					if(deltaMaxY == 0)
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
					if(deltaMinY == 0)
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
						if(deltaMaxX == 0)
						{
							deltaMaxX = 0.001;
						}
						double weightMaxX = ((double)2/(nbPins-1)) * (1/deltaMaxX);
						//Connection between fixed and non fixed block
						xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weightMaxX);
						xVector[maxXIndex] = xVector[maxXIndex] + weightMaxX*fixedXPosition;
						
						if(maxXIndex == 0)
						{
							System.out.println("New (0,0) value: " + xMatrix.getElement(0, 0));
							if(net.name.contains("pv22_5"))
							{
								System.out.println("HERE 5");
							}
						}
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
						if(deltaMinX == 0)
						{
							deltaMinX = 0.001;
						}
						double weightMinX = ((double)2/(nbPins-1)) * (1/deltaMinX);
						//Connection between fixed and non fixed block
						xMatrix.setElement(minXIndex, minXIndex, xMatrix.getElement(minXIndex, minXIndex) + weightMinX);
						xVector[minXIndex] = xVector[minXIndex] + weightMinX*fixedXPosition;
						
						if(minXIndex == 0)
						{
							System.out.println("New (0,0) value: " + xMatrix.getElement(0, 0));
							if(net.name.contains("pv22_5"))
							{
								System.out.println("HERE 6");
							}
						}
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
						if(deltaMaxY == 0)
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
						if(deltaMinY == 0)
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
		
		double epselon = 0.000001;
		
		//Solve x problem
		CGSolver xSolver = new CGSolver(xMatrix, xVector);
		double[] xSolution = xSolver.solve(epselon);
		
		//Solve y problem
		CGSolver ySolver = new CGSolver(yMatrix, yVector);
		double[] ySolution = ySolver.solve(epselon);
		
		
		
		
		
		linearX = xSolution;
		linearY = ySolution;
		
		
		
		
		
//		System.out.println("Indices:");
//		for(Clb clb:circuit.clbs.values())
//		{
//			System.out.println(clb.name + ": " + indexMap.get(clb));
//		}
//		System.out.println("\n\n");
//		
//		System.out.println("Nets:");
//		for(Net net:circuit.getNets().values())
//		{
//			System.out.print("Source: " + net.source.owner.name + "(" + net.source.owner.getSite().x + "," + net.source.owner.getSite().y + ") Sinks: ");
//			for(Pin sink:net.sinks)
//			{
//				System.out.print(sink.owner.name + "(" + sink.owner.getSite().x + "," + sink.owner.getSite().y + ") ");
//			}
//			System.out.println();
//		}
//		System.out.println("\n\n");
//		
//		System.out.println("XMatrix:");
//		for(int i = 0; i < xVector.length; i++)
//		{
//			for(int j = 0; j < xVector.length; j++)
//			{
//				System.out.print(xMatrix.getElement(i, j) + "       ");
//			}
//			System.out.println();
//		}
//		System.out.println("\n\n");
//		
//		System.out.println("XVector:");
//		for(int i = 0; i < xVector.length; i++)
//		{
//			System.out.println(xVector[i] + "   ");
//		}
//		System.out.println("\n\n");
//		
		System.out.println("\n\nX-SOLUTION:");
		for(int i = 0; i < xSolution.length; i++)
		{
			System.out.printf(i + ": %.3f\n",xSolution[i]);
		}
		System.out.println("\n\n");
//		
//		System.out.println("YMatrix:");
//		for(int i = 0; i < yVector.length; i++)
//		{
//			for(int j = 0; j < yVector.length; j++)
//			{
//				System.out.print(yMatrix.getElement(i, j) + "       ");
//			}
//			System.out.println();
//		}
//		System.out.println("\n\n");
//		
//		System.out.println("YVector:");
//		for(int i = 0; i < yVector.length; i++)
//		{
//			System.out.println(yVector[i] + "   ");
//		}
//		System.out.println("\n\n");
//		
		System.out.println("\nY-SOLUTION:");
		for(int i = 0; i < ySolution.length; i++)
		{
			System.out.printf(i + ": %.3f\n", ySolution[i]);
		}
		System.out.println();
	}
	
	private void clusterCutSpreadRecursive()
	{
		//int nbTodo = linearX.length;
		//boolean[] todo = new boolean[linearX.length];
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
			
			System.out.println("\n\nINDICES:");
			for(int index:indices)
			{
				System.out.print(index + " ");
			}
			System.out.println("\nX_LOCATIONS:");
			for(double x:positionsX)
			{
				System.out.printf("%.3f ", x);
			}
			System.out.println("\nYLOCATIONS:");
			for(double y:positionsY)
			{
				System.out.printf("%.3f ", y);
			}
			System.out.println("\nXDown: " + areaXDownBound + ", XUp: " + areaXUpBound + ", YDown: " + areaYDownBound + ", YUp: " + areaYUpBound);
			
			
			
			
			if(indices.size() == 1)
			{
				System.out.println();
				continue;
			}
			
			
			
			
			//Grow area until not overutilized
			double curUtilization = (double)positionsX.size() / (double)((areaXUpBound - areaXDownBound) * (areaYUpBound - areaYDownBound));
			System.out.printf("Utilization: %.3f\n", curUtilization);
			int curDirection = 0; //0 = right, 1 = top, 2 = left, 3 = bottom
			while(curUtilization >= utilizationFactor)
			{
				switch(curDirection)
				{
					case 0: //Grow to the right if possible
						if(areaXUpBound <= maximalX)
						{
							areaXUpBound += 1;
						}
						break;
					case 1: //Grow to the top if possible
						if(areaYDownBound >= minimalY)
						{
							areaYDownBound -= 1;
						}
						break;
					case 2: //Grow to the left if possible
						if(areaXDownBound >= minimalX)
						{
							areaXDownBound -= 1;
						}
						break;
					default: //Grow to the bottom if possible
						if(areaYUpBound <= maximalY)
						{
							areaYUpBound += 1;
						}
						break;
				}
				curUtilization = (double)positionsX.size() / (double)((areaXUpBound - areaXDownBound) * (areaYUpBound - areaYDownBound));
				curDirection++;
				curDirection %= 4;
			}
			System.out.println("New limits: XDown: " + areaXDownBound + ", XUp: " + areaXUpBound + ", YDown: " + areaYDownBound + ", YUp: " + areaYUpBound);
			System.out.printf("New utilization: %.3f\n", curUtilization);
			
			
			
			
			//Cut and spread
			boolean cutDir = true; //Initial cut is horizontally
			if(areaYUpBound - areaYDownBound <= 1) //Check if it is possible to cut horizontally
			{
				cutDir = false; //Cut vertically if not possible to cut horizontally
			}
			cutAndSpread(cutDir, indices, positionsX, positionsY, areaXDownBound, areaXUpBound, areaYDownBound, areaYUpBound); 

			
			
			
			
			System.out.println();
		}
	}
	
	/*
	 * Cut and spread recursively
	 * horCutDirection = true ==> cut horizontally, horCutDirection = false ==> cut vertically
	 */
	private void cutAndSpread(boolean horCutDirection, List<Integer> indices, List<Double> positionsX, List<Double> positionsY, 
								int areaXDownBound, int areaXUpBound, int areaYDownBound, int areaYUpBound)
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
				System.out.println("Horizontal cut y-position: " + cutPosition);
				System.out.printf("Top utilization: %.3f\n", utilizationTop);
				System.out.printf("Bottom utilization: %.3f\n", utilizationBottom);
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
				System.out.println("AN ERROR OCCURRED WHILE CUTTING!");
			}
			else
			{
				System.out.println("Vertical cut x-position: " + cutPosition);
				System.out.printf("Left utilization: %.3f\n", utilizationLeft);
				System.out.printf("Right utilization: %.3f\n", utilizationRight);
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
				cutAndSpread(nextCut, indices1, positionsX1, positionsY1, areaXDownBound, areaXUpBound, areaYDownBound, cutPosition);
			}
			else
			{
				boolean nextCut = !horCutDirection; //Next cut will be horizontal
				if(areaYUpBound - areaYDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices1, positionsX1, positionsY1, areaXDownBound, cutPosition, areaYDownBound, areaYUpBound);
			}
		}
		else //Snap to grid
		{
			System.out.printf("Index: %d, X: %d, Y: %d\n", indices1.get(0), areaXDownBound, areaYDownBound);
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
				cutAndSpread(nextCut, indices2, positionsX2, positionsY2, areaXDownBound, areaXUpBound, cutPosition, areaYUpBound);
			}
			else
			{
				boolean nextCut = !horCutDirection; //Next cut will be horizontal
				if(areaYUpBound - areaYDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices2, positionsX2, positionsY2, cutPosition, areaXUpBound, areaYDownBound, areaYUpBound);
			}
		}
		else //Snap to grid
		{
			if(horCutDirection)
			{
				System.out.printf("Index: %d, X: %d, Y: %d\n", indices2.get(0), areaXDownBound, cutPosition);
			}
			else
			{
				System.out.printf("Index: %d, X: %d, Y: %d\n", indices2.get(0), cutPosition, areaYDownBound);
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
	
	private void randomInitialPlace()
	{
		// Generate random placing (CLBs only)
		Rplace.place(circuit, architecture);
		// Place IO's (not random because IOs are not placed by the analytical placer for now)
		int index = 0;
		for(Input input:circuit.inputs.values())
		{
			Site site = architecture.Isites.get(index);
			site.block = input;
			input.setSite(site);
			index++;
		}
		index = 0;
		for(Output output:circuit.outputs.values())
		{
			Site site = architecture.Osites.get(index);
			site.block = output;
			output.setSite(site);
			index++;
		}
	}
	
	private void initializeDataStructures()
	{
		int dimensions = circuit.clbs.values().size();
		indexMap = new HashMap<>();;
		linearX = new double[dimensions];
		linearY = new double[dimensions];
		int index = 0;
		double xPos = 0.15;
		double yPos = 0.09;
		for(Clb clb:circuit.clbs.values())
		{
			indexMap.put(clb, index);
			//linearX[index] = clb.getSite().x;
			//linearY[index] = clb.getSite().y;
			linearX[index] = xPos;
			linearY[index] = yPos;
			xPos += 0.15;
			yPos += 0.09;
			index++;
		}
	}
	
}
