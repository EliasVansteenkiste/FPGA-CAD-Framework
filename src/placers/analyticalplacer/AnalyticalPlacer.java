package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashMap;
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
	
	public AnalyticalPlacer(FourLutSanitized architecture, PackedCircuit circuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	public void place()
	{
		randomInitialPlace();
		initializeDataStructures();
		solveLinear(true);
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
			
			for(Integer index:netClbIndices)
			{
				double deltaMaxX = Math.abs(linearX[index] - maxX);
				if(deltaMaxX == 0)
				{
					deltaMaxX = 0.001;
				}
				double weightMaxX = (2/(nbPins-1)) * (1/deltaMaxX);
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
						xMatrix.setElement(index, maxXIndex, xMatrix.getElement(index, maxXIndex) - weightMaxX);
						xMatrix.setElement(maxXIndex, index, xMatrix.getElement(maxXIndex, index) - weightMaxX);
					}
				}
				
				double deltaMinX = Math.abs(linearX[index] - minX);
				if(deltaMinX == 0)
				{
					deltaMinX = 0.001;
				}
				double weightMinX = (2/(nbPins-1)) * (1/deltaMinX);
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
				
				double deltaMaxY = Math.abs(linearY[index] - maxY);
				if(deltaMaxY == 0)
				{
					deltaMaxY = 0.001;
				}
				double weightMaxY = (2/(nbPins-1)) * (1/deltaMaxY);
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
				
				double deltaMinY = Math.abs(linearY[index] - minY);
				if(deltaMinY == 0)
				{
					deltaMinY = 0.001;
				}
				double weightMinY = (2/(nbPins-1)) * (1/deltaMinY);
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
			
			for(double fixedXPosition:fixedXPositions)
			{
				if(!(maxXIndex == -1))
				{
					double deltaMaxX = Math.abs(fixedXPosition - maxX);
					if(deltaMaxX == 0)
					{
						deltaMaxX = 0.001;
					}
					double weightMaxX = (2/(nbPins-1)) * (1/deltaMaxX);
					//Connection between fixed and non fixed block
					xMatrix.setElement(maxXIndex, maxXIndex, xMatrix.getElement(maxXIndex, maxXIndex) + weightMaxX);
					xVector[maxXIndex] = xVector[maxXIndex] + weightMaxX*fixedXPosition;
				}
				
				if(!(minXIndex == -1))
				{
					double deltaMinX = Math.abs(fixedXPosition - minX);
					if(deltaMinX == 0)
					{
						deltaMinX = 0.001;
					}
					double weightMinX = (2/(nbPins-1)) * (1/deltaMinX);
					//Connection between fixed and non fixed block
					xMatrix.setElement(minXIndex, minXIndex, xMatrix.getElement(minXIndex, minXIndex) + weightMinX);
					xVector[minXIndex] = xVector[minXIndex] + weightMinX*fixedXPosition;
				}
			}
			
			for(double fixedYPosition:fixedYPositions)
			{
				if(!(maxYIndex == -1))
				{
					double deltaMaxY = Math.abs(fixedYPosition - maxY);
					if(deltaMaxY == 0)
					{
						deltaMaxY = 0.001;
					}
					double weightMaxY = (2/(nbPins-1)) * (1/deltaMaxY);
					//Connection between fixed and non fixed block
					yMatrix.setElement(maxYIndex, maxYIndex, yMatrix.getElement(maxYIndex, maxYIndex) + weightMaxY);
					yVector[maxYIndex] = yVector[maxYIndex] + weightMaxY*fixedYPosition;
				}
				
				if(!(minYIndex == -1))
				{
					double deltaMinY = Math.abs(fixedYPosition - minY);
					if(deltaMinY == 0)
					{
						deltaMinY = 0.001;
					}
					double weightMinY = (2/(nbPins-1)) * (1/deltaMinY);
					//Connection between fixed and non fixed block
					yMatrix.setElement(minYIndex, minYIndex, yMatrix.getElement(minYIndex, minYIndex) + weightMinY);
					yVector[minYIndex] = yVector[minYIndex] + weightMinY*fixedYPosition;
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
		
		
		
		
		
		
		System.out.println("Indices:");
		for(Clb clb:circuit.clbs.values())
		{
			System.out.println(clb.name + ": " + indexMap.get(clb));
		}
		System.out.println("\n\n");
		
		System.out.println("Nets:");
		for(Net net:circuit.getNets().values())
		{
			System.out.print("Source: " + net.source.owner.name + "(" + net.source.owner.getSite().x + "," + net.source.owner.getSite().y + ") Sinks: ");
			for(Pin sink:net.sinks)
			{
				System.out.print(sink.owner.name + "(" + sink.owner.getSite().x + "," + sink.owner.getSite().y + ") ");
			}
			System.out.println();
		}
		System.out.println("\n\n");
		
		System.out.println("XMatrix:");
		for(int i = 0; i < xVector.length; i++)
		{
			for(int j = 0; j < xVector.length; j++)
			{
				System.out.print(xMatrix.getElement(i, j) + "       ");
			}
			System.out.println();
		}
		System.out.println("\n\n");
		
		System.out.println("XVector:");
		for(int i = 0; i < xVector.length; i++)
		{
			System.out.println(xVector[i] + "   ");
		}
		System.out.println("\n\n");
		
		System.out.println("\n\nX-SOLUTION:");
		for(int i = 0; i < xSolution.length; i++)
		{
			System.out.println(xSolution[i] + "   ");
		}
		System.out.println("\n\n");
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
		for(Clb clb:circuit.clbs.values())
		{
			indexMap.put(clb, index);
			linearX[index] = clb.getSite().x;
			linearY[index] = clb.getSite().y;
			index++;
		}
	}
	
}
