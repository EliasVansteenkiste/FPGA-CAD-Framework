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
			System.out.println(xSolution[i] + "   ");
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
		System.out.println("\n\nY-SOLUTION:");
		for(int i = 0; i < ySolution.length; i++)
		{
			System.out.println(ySolution[i] + "   ");
		}
		System.out.println("\n\n");
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
				for(int x = areaXDownBound; x < areaXDownBound; x++)
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
			//Grow area until not overutilized
			double curUtilization = (double)positionsX.size() / (double)((areaXUpBound - areaXDownBound) * (areaYUpBound - areaYDownBound));
		}
		
		//Cut and spread
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
