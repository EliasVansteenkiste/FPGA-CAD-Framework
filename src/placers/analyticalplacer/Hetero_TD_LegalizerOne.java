package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import timinganalysis.TimingGraph;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.HardBlock;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;
import architecture.ClbSite;
import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import architecture.Site;
import architecture.SiteType;

public class Hetero_TD_LegalizerOne
{
	
	private final double UTILIZATION_FACTOR = 0.9;
	private static final double TRADE_OFF_FACTOR = 0.5;
	
	private int[] bestLegalX;
	private int[] bestLegalY;
	private HeterogeneousArchitecture architecture;
	private int[] typeStartIndices;
	private String[] typeNames;
	private TimingGraph timingGraph;
	private PackedCircuit circuit;
	private boolean firstDone;
	private double previousMaxDelay;
	private double previousBBCost;
	
	public Hetero_TD_LegalizerOne(HeterogeneousArchitecture architecture, int[] typeStartIndices, String[] typeNames, 
														int nbMovableBlocks, TimingGraph timingGraph, PackedCircuit circuit)
	{
		this.bestLegalX = new int[nbMovableBlocks];
		this.bestLegalY = new int[nbMovableBlocks];
		this.architecture = architecture;
		this.typeStartIndices = typeStartIndices;
		this.typeNames = typeNames;
		this.timingGraph = timingGraph;
		this.circuit = circuit;
		this.firstDone = false;
		this.previousMaxDelay = Double.MAX_VALUE;
		this.previousBBCost = Double.MAX_VALUE;
	}
	
	/*
	 * Legalize the linear solution and store it if it is the best one found so far
	 * solveMode: 0 = solve all, 1 = solve CLBs only, 2 = solve hb1 type only, 3 = solve hb2 type only,...
	 */
	public void legalize(double[] linearX, double[] linearY, Map<Block,Integer> indexMap, int solveMode)
	{
		int[] semiLegalX = new int[bestLegalX.length];
		int[] semiLegalY = new int[bestLegalY.length];
		if(solveMode == 0)
		{
			for(int i = 0; i < typeNames.length; i++)
			{
				if(i != typeNames.length - 1)
				{
					subTypeLegalize(typeStartIndices[i], typeStartIndices[i+1], linearX, linearY, typeNames[i], semiLegalX, semiLegalY);
				}
				else
				{
					subTypeLegalize(typeStartIndices[i], bestLegalX.length, linearX, linearY, typeNames[i], semiLegalX, semiLegalY);
				}
			}
		}
		else
		{
			if(solveMode != typeNames.length)
			{
				subTypeLegalize(typeStartIndices[solveMode - 1], typeStartIndices[solveMode], linearX, linearY, 
																typeNames[solveMode - 1], semiLegalX, semiLegalY);
				for(int i = 0; i < typeStartIndices[solveMode - 1]; i++)
				{
					semiLegalX[i] = bestLegalX[i];
					semiLegalY[i] = bestLegalY[i];
				}
				for(int i = typeStartIndices[solveMode]; i < semiLegalX.length; i++)
				{
					semiLegalX[i] = bestLegalX[i];
					semiLegalY[i] = bestLegalY[i];
				}
			}
			else
			{
				//System.out.println("Calling subTypeLegalize with startIndex = " + typeStartIndices[solveMode - 1] + 
				//		" and endIndex = " + bestLegalX.length);
				subTypeLegalize(typeStartIndices[solveMode - 1], bestLegalX.length, linearX, linearY, 
																typeNames[solveMode - 1], semiLegalX, semiLegalY);
				for(int i = 0; i < typeStartIndices[solveMode - 1]; i++)
				{
					semiLegalX[i] = bestLegalX[i];
					semiLegalY[i] = bestLegalY[i];
				}
			}
		}
		
		int[] legalX = new int[semiLegalX.length];
		int[] legalY = new int[semiLegalY.length];
		finalLegalization(semiLegalX, semiLegalY, legalX, legalY);
		
		updateBestLegal(legalX, legalY, indexMap);
	}
	
	/*
	 * EndIndex is the first index which is not of the considered block type anymore
	 */
	private void subTypeLegalize(int startIndex, int endIndex, double[] linearX, double[] linearY, String typeName, 
														int[] semiLegalX, int[] semiLegalY)
	{
		List<Integer> todo = new ArrayList<>();
		for(int i = startIndex; i < endIndex; i++)
		{
			todo.add(i);
		}
		
		//Legalize untill all blocks are done
		while(todo.size() != 0)
		{
			//Cluster
			double areaXUpBoundHalf;
			double areaXDownBoundHalf;
			double areaYUpBoundHalf;
			double areaYDownBoundHalf;
			List<Integer> indices = new ArrayList<>();
			List<Double> positionsX = new ArrayList<>();
			List<Double> positionsY = new ArrayList<>();
			
			//Find a starting point for the cluster
			int firstIndex = todo.get(0);
			areaXUpBoundHalf = (double) Math.round(linearX[firstIndex]) + 0.5;
			areaXDownBoundHalf = (double) Math.round(linearX[firstIndex]) - 0.5;
			areaYUpBoundHalf = (double) Math.round(linearY[firstIndex]) + 0.5;
			areaYDownBoundHalf = (double) Math.round(linearY[firstIndex]) - 0.5;
			indices.add(firstIndex);
			positionsX.add(linearX[firstIndex]);
			positionsY.add(linearY[firstIndex]);
			todo.remove(0);
			for (int i = 0; i < todo.size(); i++)
			{
				int currentIndex = todo.get(i);
				if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
						&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
				{
					indices.add(currentIndex);
					positionsX.add(linearX[currentIndex]);
					positionsY.add(linearY[currentIndex]);
					todo.remove(i);
					i--;
				}
			}
			
			// Grow cluster until it is surrounded by non overutilized areas
			boolean expanded = false;
			if (indices.size() > 1)
			{
				expanded = true;
			}
			while (expanded)
			{
				expanded = false;
				// Check if need to grow to the right
				boolean addRight = false;
				for (double y = areaYDownBoundHalf; y < areaYUpBoundHalf; y += 1.0)
				{
					int nbCells = 0;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= areaXUpBoundHalf && linearX[currentIndex] < areaXUpBoundHalf + 1.0 && 
								linearY[currentIndex] >= y && linearY[currentIndex] < y + 1.0)
						{
							nbCells++;
							if (nbCells >= 2)
							{
								addRight = true;
								break;
							}
						}
					}
					if (addRight)
					{
						break;
					}
				}
				if (addRight)
				{
					areaXUpBoundHalf += 1.0;
					expanded = true;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
								&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}

				// Check if need to grow to the top
				boolean addTop = false;
				for (double x = areaXDownBoundHalf; x < areaXUpBoundHalf; x += 1.0)
				{
					int nbCells = 0;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= x && linearX[currentIndex] < x + 1.0 && 
								linearY[currentIndex] >= areaYDownBoundHalf - 1.0 && linearY[currentIndex] < areaYDownBoundHalf)
						{
							nbCells++;
							if (nbCells >= 2)
							{
								addTop = true;
								break;
							}
						}
					}
					if (addTop)
					{
						break;
					}
				}
				if (addTop)
				{
					areaYDownBoundHalf -= 1.0;
					expanded = true;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
								&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}

				// Check if need to grow to the left
				boolean addLeft = false;
				for (double y = areaYDownBoundHalf; y < areaYUpBoundHalf; y += 1.0)
				{
					int nbCells = 0;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= areaXDownBoundHalf - 1.0 && linearX[currentIndex] < areaXDownBoundHalf
								&& linearY[currentIndex] >= y && linearY[currentIndex] < y + 1.0)
						{
							nbCells++;
							if (nbCells >= 2)
							{
								addLeft = true;
								break;
							}
						}
					}
					if (addLeft)
					{
						break;
					}
				}
				if (addLeft)
				{
					areaXDownBoundHalf -= 1.0;
					expanded = true;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
								&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
						{
							indices.add(currentIndex);
							positionsX.add(linearX[currentIndex]);
							positionsY.add(linearY[currentIndex]);
							todo.remove(i);
							i--;
						}
					}
				}

				// Check if need to grow to the bottom
				boolean addBottom = false;
				for (double x = areaXDownBoundHalf; x < areaXUpBoundHalf; x += 1.0)
				{
					int nbCells = 0;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= x && linearX[currentIndex] < x + 1.0 && linearY[currentIndex] >= areaYUpBoundHalf
								&& linearY[currentIndex] < areaYUpBoundHalf + 1.0)
						{
							nbCells++;
							if (nbCells >= 2)
							{
								addBottom = true;
								break;
							}
						}
					}
					if (addBottom)
					{
						break;
					}
				}
				if (addBottom)
				{
					areaYUpBoundHalf += 1.0;
					expanded = true;
					for (int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
								&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
			
//			System.out.println(indices.size());
			if(indices.size() == 1)
			{
				int x = (int)Math.round(positionsX.get(0));
				int y = (int)Math.round(positionsY.get(0));
				while(x < 1)
				{
					x++;
				}
				while(x > architecture.getWidth())
				{
					x--;
				}
				while(y < 1)
				{
					y++;
				}
				while(y > architecture.getHeight())
				{
					y--;
				}
				Site site = architecture.getSite(x, y);
				if(typeName.equals("CLB"))
				{
					if(site.getType() == SiteType.CLB)
					{
						int index = indices.get(0);
						semiLegalX[index] = x;
						semiLegalY[index] = y;
						continue;
					}
				}
				else
				{
					if(site.getType() == SiteType.HARDBLOCK)
					{
						if(((HardBlockSite)site).getTypeName().equals(typeName))
						{
							int index = indices.get(0);
							semiLegalX[index] = x;
							semiLegalY[index] = y;
							continue;
						}
					}
				}
			}
			
			// Grow area until not overutilized
			double curUtilization = getUtilization(positionsX.size(), areaXDownBoundHalf, areaXUpBoundHalf, areaYDownBoundHalf,
										areaYUpBoundHalf, typeName);
			int curDirection = 0; // 0 = right, 1 = top, 2 = left, 3 = bottom
			int minimalX = 1;
			int maximalX = architecture.getWidth();
			int minimalY = 1;
			int maximalY = architecture.getHeight();
			while(curUtilization >= UTILIZATION_FACTOR && !(areaXDownBoundHalf < minimalX && areaXUpBoundHalf > maximalX && 
																areaYDownBoundHalf < minimalY && areaYUpBoundHalf > maximalY))
			{
				switch (curDirection)
				{
				case 0: // Grow to the right if possible
					if (areaXUpBoundHalf <= maximalX)
					{
						areaXUpBoundHalf += 1.0;

						// Add blocks which are not in the cluster yet
						for (int i = 0; i < todo.size(); i++)
						{
							int currentIndex = todo.get(i);
							if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
									&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
							{
								indices.add(currentIndex);
								positionsX.add(linearX[currentIndex]);
								positionsY.add(linearY[currentIndex]);
								todo.remove(i);
								i--;
							}
						}
					} 
					else // We would like to grow to the right but can't: try to grow to the left
					{
						if (areaXDownBoundHalf >= minimalX)
						{
							areaXDownBoundHalf -= 1.0;

							// Add blocks which are not in the cluster yet
							for (int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
										&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
					break;
				case 1: // Grow to the top if possible
					if (areaYDownBoundHalf >= minimalY)
					{
						areaYDownBoundHalf -= 1.0;

						// Add blocks which are not in the cluster yet
						for (int i = 0; i < todo.size(); i++)
						{
							int currentIndex = todo.get(i);
							if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
									&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
							{
								indices.add(currentIndex);
								positionsX.add(linearX[currentIndex]);
								positionsY.add(linearY[currentIndex]);
								todo.remove(i);
								i--;
							}
						}
					}
					else // We would like to grow to the top but can't: try to grow to the bottom
					{
						if (areaYUpBoundHalf <= maximalY)
						{
							areaYUpBoundHalf += 1.0;

							// Add blocks which are not in the cluster yet
							for (int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
										&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
					break;
				case 2: // Grow to the left if possible
					if (areaXDownBoundHalf >= minimalX)
					{
						areaXDownBoundHalf -= 1.0;

						// Add blocks which are not in the cluster yet
						for (int i = 0; i < todo.size(); i++)
						{
							int currentIndex = todo.get(i);
							if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
									&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
							{
								indices.add(currentIndex);
								positionsX.add(linearX[currentIndex]);
								positionsY.add(linearY[currentIndex]);
								todo.remove(i);
								i--;
							}
						}
					}
					else // We would like to grow to the left but can't: try to grow to the right
					{
						if (areaXUpBoundHalf <= maximalX)
						{
							areaXUpBoundHalf += 1.0;

							// Add blocks which are not in the cluster yet
							for (int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
										&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
					break;
				default: // Grow to the bottom if possible
					if (areaYUpBoundHalf <= maximalY)
					{
						areaYUpBoundHalf += 1.0;

						// Add blocks which are not in the cluster yet
						for (int i = 0; i < todo.size(); i++)
						{
							int currentIndex = todo.get(i);
							if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
									&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
							{
								indices.add(currentIndex);
								positionsX.add(linearX[currentIndex]);
								positionsY.add(linearY[currentIndex]);
								todo.remove(i);
								i--;
							}
						}
					}
					else // We would like to grow to the bottom but can't: try to grow to the top
					{
						if (areaYDownBoundHalf >= minimalY)
						{
							areaYDownBoundHalf -= 1.0;

							// Add blocks which are not in the cluster yet
							for (int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if (linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf
										&& linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
					break;
				}
				curUtilization = getUtilization(positionsX.size(), areaXDownBoundHalf, areaXUpBoundHalf, areaYDownBoundHalf, areaYUpBoundHalf, typeName);
				curDirection++;
				curDirection %= 4;
			}
			
			// Change double boundaries on half coordinates in integer boundaries on integer coordinates
			int areaXDownBound = (int) Math.ceil(areaXDownBoundHalf);
			int areaXUpBound = (int) Math.ceil(areaXUpBoundHalf); //First one that is not valid anymore
			int areaYDownBound = (int) Math.ceil(areaYDownBoundHalf);
			int areaYUpBound = (int) Math.ceil(areaYUpBoundHalf); //First one that is not valid anymore
			
			while (areaXDownBound < minimalX)
			{
				areaXDownBound++;
				if(areaXUpBound == areaXDownBound)
				{
					areaXUpBound++;
				}
			}
			while (areaXUpBound > maximalX + 1)
			{
				areaXUpBound--;
				if(areaXDownBound == areaXUpBound)
				{
					areaXDownBound--;
				}
			}
			while (areaYDownBound < minimalY)
			{
				areaYDownBound++;
				if(areaYUpBound == areaYDownBound)
				{
					areaYUpBound++;
				}
			}
			while (areaYUpBound > maximalY + 1)
			{
				areaYUpBound--;
				if(areaYDownBound == areaYUpBound)
				{
					areaYDownBound--;
				}
			}
			
			//Identify rectangles in which we can put the considered blocks
			ArrayList<Integer> rectangleStartX = new ArrayList<>();
			ArrayList<Integer> rectangleStopX = new ArrayList<>();
			boolean alreadyRunning = false;
			for(int x = areaXDownBound; x < areaXUpBound; x++)
			{
				Site site = architecture.getSite(x, 1);
				if(typeName.equals("CLB"))
				{
					if(site.getType() == SiteType.CLB)
					{
						if(!alreadyRunning)
						{
							alreadyRunning = true;
							rectangleStartX.add(x);
						}
					}
					else
					{
						if(alreadyRunning)
						{
							alreadyRunning = false;
							rectangleStopX.add(x);
						}
					}
				}
				else //We are working with hardBlocks
				{
					if(site.getType() == SiteType.HARDBLOCK)
					{
						if(((HardBlockSite)site).getTypeName().equals(typeName))
						{
							if(!alreadyRunning)
							{
								alreadyRunning = true;
								rectangleStartX.add(x);
							}
						}
						else
						{
							if(alreadyRunning)
							{
								alreadyRunning = false;
								rectangleStopX.add(x);
							}
						}
					}
					else
					{
						if(alreadyRunning)
						{
							alreadyRunning = false;
							rectangleStopX.add(x);
						}
					}
				}
			}
			if(alreadyRunning)
			{
				rectangleStopX.add(areaXUpBound);
			}
			
			//Divide blocks over rectangles and cut and spread 
			boolean cutDir;
			if(areaYUpBound - areaYDownBound > 1)
			{
				cutDir = true; //Initial cut is horizontally if possible
			}
			else
			{
				cutDir = false; //Initial cut is vertically if not possible horizontally
			}
			sort(true, indices, positionsX, positionsY);
			int totalArea = 0;
			for(int i = 0; i < rectangleStopX.size(); i++)
			{
				totalArea += (rectangleStopX.get(i) - rectangleStartX.get(i)) * ((areaYUpBound - areaYDownBound));
			}
			
			int totalNbBlocks = positionsX.size();
			for(int i = rectangleStopX.size() - 1; i >= 0; i--)
			{
				int rectangleArea = (rectangleStopX.get(i) - rectangleStartX.get(i)) * ((areaYUpBound - areaYDownBound));
				int nbBlocksRectangle;
				if(i == 0)
				{
					nbBlocksRectangle = positionsX.size();
				}
				else
				{
					nbBlocksRectangle = (int)(((double)rectangleArea)/((double)totalArea) * totalNbBlocks);
				}
				ArrayList<Integer> rectangleIndices = new ArrayList<>();
				ArrayList<Double> rectanglePositionsX = new ArrayList<>();
				ArrayList<Double> rectanglePositionsY = new ArrayList<>();
				for(int j = 0; j< nbBlocksRectangle; j++)
				{
					rectangleIndices.add(indices.remove(indices.size() - 1));
					rectanglePositionsX.add(positionsX.remove(positionsX.size() - 1));
					rectanglePositionsY.add(positionsY.remove(positionsY.size() - 1));
				}
//				System.out.println("Calling cutAndSpread: " + rectangleIndices.size() + ", " + rectangleStartX.get(i) + ", " + 
//								rectangleStopX.get(i) + ", " + areaYDownBound + ", " + areaYUpBound);
				cutAndSpread(cutDir, rectangleIndices, rectanglePositionsX, rectanglePositionsY, rectangleStartX.get(i), 
									rectangleStopX.get(i), areaYDownBound, areaYUpBound, semiLegalX, semiLegalY);
			}
			
		}
	}
	
	/*
	 * Cut and spread recursively horCutDirection = true ==> cut horizontally,
	 * horCutDirection = false ==> cut vertically
	 */
	private void cutAndSpread(boolean horCutDirection, List<Integer> indices, List<Double> positionsX, List<Double> positionsY, int areaXDownBound,
			int areaXUpBound, int areaYDownBound, int areaYUpBound, int[] semiLegalXPositions, int[] semiLegalYPositions)
	{
		// Bug checks
		if (areaXDownBound >= areaXUpBound || areaYDownBound >= areaYUpBound)
		{
			System.err.println("ERROR: A DOWNBOUND IS BIGGER THAN OR EQUAL TO AN UPBOUND: downboundX = " + areaXDownBound + 
					", upboundX = " + areaXUpBound + ", downboundY = " + areaYDownBound + ", upboundY = " + areaYUpBound + 
					", nbOfBlocks = " + indices.size());
		}

		// Sort
		if (horCutDirection) // Cut horizontally => sort in i
		{
			sort(false, indices, positionsX, positionsY);
		} 
		else //Cut vertically
		{
			sort(true, indices, positionsX, positionsY);
		}

		// Target cut
		int cutPosition;
		int area1; // Top or left
		int area2; // Bottom or right
		// area1 will always be smaller or equal to area2
		if (horCutDirection) // Cut horizontally
		{
			cutPosition = (areaYDownBound + areaYUpBound) / 2;
			area1 = (areaXUpBound - areaXDownBound) * (cutPosition - areaYDownBound);
			area2 = (areaXUpBound - areaXDownBound) * (areaYUpBound - cutPosition);
		} else
		// Cut vertically
		{
			cutPosition = (areaXDownBound + areaXUpBound) / 2;
			area1 = (cutPosition - areaXDownBound) * (areaYUpBound - areaYDownBound);
			area2 = (areaXUpBound - cutPosition) * (areaYUpBound - areaYDownBound);
		}

		// Source cut
		int endIndex = indices.size();
		double totalUtilization = ((double) positionsX.size()) / ((double) (area1 + area2));
		int cutIndex = (int) Math.round(area1 * totalUtilization);
		if(cutIndex == 0 && endIndex >= 2)
		{
			cutIndex = 1;
			//System.out.println("Had to intervene");
		}
		if(cutIndex == endIndex && endIndex >=2)
		{
			cutIndex = endIndex - 1;
			//System.out.println("Had to intervene");
		}
		List<Integer> indices1 = new ArrayList<>();
		List<Double> positionsX1 = new ArrayList<>();
		List<Double> positionsY1 = new ArrayList<>();
		for (int i = 0; i < cutIndex; i++)
		{
			indices1.add(indices.get(i));
			positionsX1.add(positionsX.get(i));
			positionsY1.add(positionsY.get(i));
		}
		List<Integer> indices2 = new ArrayList<>();
		List<Double> positionsX2 = new ArrayList<>();
		List<Double> positionsY2 = new ArrayList<>();
		for (int i = cutIndex; i < endIndex; i++)
		{
			indices2.add(indices.get(i));
			positionsX2.add(positionsX.get(i));
			positionsY2.add(positionsY.get(i));
		}
		
		// Recursive calls if necessary (check for base cases)
		if (indices1.size() > 1) // Do recursive call
		{
			if (horCutDirection)
			{
				boolean nextCut = !horCutDirection; // Next cut will be vertical
				if (areaXUpBound - areaXDownBound <= 1) // Next cut will be
														// vertical ==> check if
														// it will be possible
														// to cut vertically
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices1, positionsX1, positionsY1, areaXDownBound, areaXUpBound, areaYDownBound, cutPosition,
						semiLegalXPositions, semiLegalYPositions);
			} else
			{
				boolean nextCut = !horCutDirection; // Next cut will be
													// horizontal
				if (areaYUpBound - areaYDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices1, positionsX1, positionsY1, areaXDownBound, cutPosition, areaYDownBound, areaYUpBound,
						semiLegalXPositions, semiLegalYPositions);
			}
		} else
		// Snap to grid
		{
			if(indices1.size() > 0)
			{
				int x;
				int y;
				if (positionsX1.get(0) <= areaXDownBound)
				{
					x = areaXDownBound;
				}
				else
				{
					if (horCutDirection)
					{
						if (positionsX1.get(0) >= areaXUpBound - 1)
						{
							x = areaXUpBound - 1;
						} else
						{
							x = (int) Math.round(positionsX1.get(0));
						}
					} else
					{
						if (positionsX1.get(0) >= cutPosition - 1)
						{
							x = cutPosition - 1;
						} else
						{
							x = (int) Math.round(cutPosition - 1);
						}
					}
				}
				if (positionsY1.get(0) <= areaYDownBound)
				{
					y = areaYDownBound;
				} else
				{
					if (horCutDirection)
					{
						if (positionsY1.get(0) >= cutPosition - 1)
						{
							y = cutPosition - 1;
						} else
						{
							y = (int) Math.round(positionsY1.get(0));
						}
					} else
					{
						if (positionsY1.get(0) >= areaYUpBound - 1)
						{
							y = areaYUpBound - 1;
						} else
						{
							y = (int) Math.round(positionsY1.get(0));
						}
					}
				}
				int index = indices1.get(0);
				// System.out.printf("Index: %d, X: %d, Y: %d\n", index, x, y);
				semiLegalXPositions[index] = x;
				semiLegalYPositions[index] = y;
			}
		}
		if (indices2.size() > 1) // Do recursive call
		{
			if (horCutDirection)
			{
				boolean nextCut = !horCutDirection; // Next cut will be vertical
				if (areaXUpBound - areaXDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices2, positionsX2, positionsY2, areaXDownBound, areaXUpBound, cutPosition, areaYUpBound,
						semiLegalXPositions, semiLegalYPositions);
			} else
			{
				boolean nextCut = !horCutDirection; // Next cut will be
													// horizontal
				if (areaYUpBound - areaYDownBound <= 1)
				{
					nextCut = horCutDirection;
				}
				cutAndSpread(nextCut, indices2, positionsX2, positionsY2, cutPosition, areaXUpBound, areaYDownBound, areaYUpBound,
						semiLegalXPositions, semiLegalYPositions);
			}
		} else
		// Snap to grid
		{
			if(indices2.size() > 0)
			{
				int x;
				int y;
				if (positionsX2.get(0) >= areaXUpBound - 1)
				{
					x = areaXUpBound - 1;
				} else
				{
					if (horCutDirection)
					{
						if (positionsX2.get(0) <= areaXDownBound)
						{
							x = areaXDownBound;
						} else
						{
							x = (int) Math.round(positionsX2.get(0));
						}
					} else
					{
						if (positionsX2.get(0) <= cutPosition)
						{
							x = cutPosition;
						} else
						{
							x = (int) Math.round(positionsX2.get(0));
						}
					}
				}
				if (positionsY2.get(0) >= areaYUpBound - 1)
				{
					y = areaYUpBound - 1;
				} else
				{
					if (horCutDirection)
					{
						if (positionsY2.get(0) <= cutPosition)
						{
							y = cutPosition;
						} else
						{
							y = (int) Math.round(positionsY2.get(0));
						}
					} else
					{
						if (positionsY2.get(0) <= areaYDownBound)
						{
							y = areaYDownBound;
						} else
						{
							y = (int) Math.round(positionsY2.get(0));
						}
					}
				}
				int index = indices2.get(0);
				// System.out.printf("Index: %d, X: %d, Y: %d\n", index, x, y);
				semiLegalXPositions[index] = x;
				semiLegalYPositions[index] = y;
			}
		}
	}
	
	/*
	 * Eliminates the final overlaps (between different clusters) Works from
	 * left to right
	 */
	private void finalLegalization(int[] semiLegalXPositions, int[] semiLegalYPositions, int[] legalX, int[] legalY)
	{
		int[] semiLegalIndices = new int[semiLegalXPositions.length];
		for (int i = 0; i < semiLegalIndices.length; i++)
		{
			semiLegalIndices[i] = i;
		}
		
		sort(true, semiLegalIndices, semiLegalXPositions, semiLegalYPositions); // Sort in x direction
		
		int minimalX = 1;
		int maximalX = architecture.getWidth();
		int minimalY = 1;
		int maximalY = architecture.getHeight();
		int ySize = maximalY - minimalY + 1;
		int xSize = maximalX - minimalX + 1;
		boolean[][] occupied = new boolean[ySize][xSize]; // True if CLB site is occupied, false if not
		for (int i = 0; i < ySize; i++)
		{
			for (int j = 0; j < xSize; j++)
			{
				occupied[i][j] = false;
			}
		}
		
		for (int i = 0; i < semiLegalIndices.length; i++)
		{
			int index = semiLegalIndices[i];
			int x = semiLegalXPositions[i];
			int y = semiLegalYPositions[i];

			// Shift to legal zone
			while (x < minimalX)
			{
				x++;
			}
			while (x > maximalX)
			{
				x--;
			}
			while (y < minimalY)
			{
				y++;
			}
			while (y > maximalY)
			{
				y--;
			}

			if (!occupied[y - minimalY][x - minimalX]) // Check if there's overlap, you know for sure that the siteType is always correct
			{
				occupied[y - minimalY][x - minimalX] = true;
				legalX[index] = x;
				legalY[index] = y;
				//System.out.println("(" + x + "," + y + ")");
			} 
			else // Eliminate overlap
			{
				String typeName = null;
				for(int j = 0; j < typeNames.length - 1; j++)
				{
					if(index >= typeStartIndices[j] && index < typeStartIndices[j+1])
					{
						typeName = typeNames[j];
					}
				}
				if(typeName == null)
				{
					typeName = typeNames[typeNames.length - 1];
				}
				//System.out.println(typeName);
				
				// Look around for free spot ==> counterclockwise with increasing box size until we find available position that is of the correct type
				int currentX = x;
				int currentY = y - 1;
				int curBoxSize = 1;
				boolean xDir = true; //true = x-direction, false = y-direction
				int moveSpeed = -1; //Always +1 or -1
				while(!isValidPosition(currentX, currentY, minimalX, maximalX, minimalY, maximalY, occupied, typeName))
				{
					if(xDir && currentX == x - curBoxSize) //Check if we reached top left corner
					{
						xDir = false;
						moveSpeed = 1;
						currentY = y - curBoxSize + 1;
					}
					else
					{
						if (!xDir && currentY == y + curBoxSize) //Check if we reached bottom left corner
						{
							xDir = true;
							moveSpeed = 1;
							currentX = x - curBoxSize + 1;
						}
						else
						{
							if(xDir && currentX == x + curBoxSize) //Check if we reached bottom right corner
							{
								xDir = false;
								moveSpeed = -1;
								currentY = y + curBoxSize - 1;
							}
							else
							{
								if(!xDir && currentY == y - curBoxSize) //Check if we reached top right corner
								{
									xDir = true;
									moveSpeed = -1;
									currentX = x + curBoxSize - 1;
									if (currentX == x && currentY == y - curBoxSize) //We've went completely around the box and didn't find an available position ==> increase box size
									{
										curBoxSize++;
										currentX = x;
										currentY = y - curBoxSize;
										xDir = true;
										moveSpeed = -1;
									}
								}
								else //We didn't reach a corner and just have to keep moving
								{
									if (xDir) //Move in x-direction
									{
										currentX += moveSpeed;
									}
									else//Move in y-direction
									{
										currentY += moveSpeed;
									}
									if(currentX == x && currentY == y - curBoxSize) // We've went completely around the box and didn't find an available position ==> increase box size
									{
										curBoxSize++;
										currentX = x;
										currentY = y - curBoxSize;
										xDir = true;
										moveSpeed = -1;
									}
								}
							}
						}
					}
				}
				if(currentY == 0)
				{
					System.out.println("Trouble");
				}
				occupied[currentY - minimalY][currentX - minimalX] = true;
				legalX[index] = currentX;
				legalY[index] = currentY;
			}
		}
	}
	
	private void updateBestLegal(int[] legalX, int[] legalY, Map<Block, Integer> indexMap)
	{
		double newBBCost = calculateTotalBBCost(legalX, legalY, indexMap);
		updateCircuit(legalX, legalY, indexMap);
		timingGraph.updateDelays();
		double newMaxDelay = timingGraph.calculateMaximalDelay();
		if(firstDone)
		{
			double deltaCost = TRADE_OFF_FACTOR * ((newMaxDelay - previousMaxDelay) / previousMaxDelay)
								+ (1 - TRADE_OFF_FACTOR) * ((newBBCost - previousBBCost) / previousBBCost);
			if (deltaCost < 0)
			{
				//System.out.println("Improved: bb cost = " + newBBCost + ", critical path = " + timingGraph.calculateMaximalDelay());
				for (int i = 0; i < legalX.length; i++)
				{
					bestLegalX[i] = legalX[i];
					bestLegalY[i] = legalY[i];
				}
				previousBBCost = newBBCost;
				previousMaxDelay = newMaxDelay;
			}
			else //revert timingGraph
			{
				updateCircuit(bestLegalX, bestLegalY, indexMap);
				timingGraph.updateDelays();
			}
		}
		else
		{
			for (int i = 0; i < legalX.length; i++)
			{
				bestLegalX[i] = legalX[i];
				bestLegalY[i] = legalY[i];
			}
			previousBBCost = calculateTotalBBCost(legalX, legalY, indexMap);
			updateCircuit(bestLegalX, bestLegalY, indexMap);
			timingGraph.updateDelays();
			previousMaxDelay = timingGraph.calculateMaximalDelay();
			firstDone = true;
		}
	}
	
	public int[] getAnchorPointsX()
	{
		return bestLegalX;
	}
	
	public int[] getAnchorPointsY()
	{
		return bestLegalY;
	}
	
	public int[] getBestLegalX()
	{
		return bestLegalX;
	}
	
	public int[] getBestLegalY()
	{
		return bestLegalY;
	}
	
	private void updateCircuit(int[] xPositions, int[] yPositions, Map<Block,Integer> indexMap)
	{
		//Clear all previous locations
		int maximalX = architecture.getWidth();
		int maximalY = architecture.getHeight();
		for(int i = 1; i <= maximalX; i++)
		{
			for(int j = 1; j <= maximalY; j++)
			{
				Site site = architecture.getSite(i, j);
				if(site.getType() == SiteType.CLB)
				{
					ClbSite clbSite = (ClbSite)site;
					if(clbSite.getClb() != null)
					{
						clbSite.getClb().setSite(null);
					}
					clbSite.setClb(null);
				}
				else //Must be a hardBlockSite
				{
					HardBlockSite hbSite = (HardBlockSite)site;
					if(hbSite.getHardBlock() != null)
					{
						hbSite.getHardBlock().setSite(null);
					}
					hbSite.setHardBlock(null);
				}
			}
		}
		
		//Update locations
		for(Clb clb:circuit.clbs.values())
		{
			int index = indexMap.get(clb);
			Site site = architecture.getSite(xPositions[index], yPositions[index]);
			((ClbSite)site).setClb(clb);
			clb.setSite(site);
		}
		for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
		{
			for(HardBlock hb: hbVector)
			{
				int index = indexMap.get(hb);
				Site site = architecture.getSite(xPositions[index], yPositions[index]);
				((HardBlockSite)site).setHardBlock(hb);
				hb.setSite(site);
			}
		}
	}
	
	private boolean isValidPosition(int currentX, int currentY, int minimalX, int maximalX, 
												int minimalY, int maximalY, boolean[][] occupied, String typeName)
	{
		boolean toReturn;
		if(currentX >= minimalX && currentX <= maximalX && currentY >= minimalY && currentY <= maximalY)
		{
			Site site = architecture.getSite(currentX, currentY);
			if(site.getType() == SiteType.CLB)
			{
				if(typeName.equals("CLB") && !occupied[currentY - minimalY][currentX - minimalX])
				{
					toReturn = true;
				}
				else
				{
					toReturn = false;
				}
			}
			else
			{
				String siteTypeName = ((HardBlockSite)site).getTypeName();
				if(siteTypeName.equals(typeName) && !occupied[currentY - minimalY][currentX - minimalX])
				{
					toReturn = true;
				}
				else
				{
					toReturn = false;
				}
			}
		}
		else
		{
			toReturn = false;
		}
		return toReturn;
	}
	
	public double calculateTotalBBCost(int[] xArray, int[] yArray, Map<Block, Integer> indexMap)
	{
		double cost = 0.0;
		Collection<Net> nets = circuit.getNets().values();
		for (Net net : nets)
		{
			int minX;
			int maxX;
			int minY;
			int maxY;
			Block sourceBlock = net.source.owner;
			if (sourceBlock.type == BlockType.INPUT || sourceBlock.type == BlockType.OUTPUT)
			{
				minX = sourceBlock.getSite().getX();
				maxX = sourceBlock.getSite().getX();
				minY = sourceBlock.getSite().getY();
				maxY = sourceBlock.getSite().getY();
			}
			else
			{
				int index = indexMap.get(sourceBlock);
				minX = xArray[index];
				maxX = xArray[index];
				minY = yArray[index];
				maxY = yArray[index];
			}

			for (Pin pin : net.sinks)
			{
				Block sinkOwner = pin.owner;
				if (sinkOwner.type == BlockType.INPUT || sinkOwner.type == BlockType.OUTPUT)
				{
					Site sinkOwnerSite = sinkOwner.getSite();
					if (sinkOwnerSite.getX() < minX)
					{
						minX = sinkOwnerSite.getX();
					}
					if (sinkOwnerSite.getX() > maxX)
					{
						maxX = sinkOwnerSite.getX();
					}
					if (sinkOwnerSite.getY() < minY)
					{
						minY = sinkOwnerSite.getY();
					}
					if (sinkOwnerSite.getY() > maxY)
					{
						maxY = sinkOwnerSite.getY();
					}
				}
				else
				{
					int index = indexMap.get(sinkOwner);
					if (xArray[index] < minX)
					{
						minX = xArray[index];
					}
					if (xArray[index] > maxX)
					{
						maxX = xArray[index];
					}
					if (yArray[index] < minY)
					{
						minY = yArray[index];
					}
					if (yArray[index] > maxY)
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
		switch (size)
		{
		case 1:
			weight = 1;
			break;
		case 2:
			weight = 1;
			break;
		case 3:
			weight = 1;
			break;
		case 4:
			weight = 1.0828;
			break;
		case 5:
			weight = 1.1536;
			break;
		case 6:
			weight = 1.2206;
			break;
		case 7:
			weight = 1.2823;
			break;
		case 8:
			weight = 1.3385;
			break;
		case 9:
			weight = 1.3991;
			break;
		case 10:
			weight = 1.4493;
			break;
		case 11:
		case 12:
		case 13:
		case 14:
		case 15:
			weight = (size - 10) * (1.6899 - 1.4493) / 5 + 1.4493;
			break;
		case 16:
		case 17:
		case 18:
		case 19:
		case 20:
			weight = (size - 15) * (1.8924 - 1.6899) / 5 + 1.6899;
			break;
		case 21:
		case 22:
		case 23:
		case 24:
		case 25:
			weight = (size - 20) * (2.0743 - 1.8924) / 5 + 1.8924;
			break;
		case 26:
		case 27:
		case 28:
		case 29:
		case 30:
			weight = (size - 25) * (2.2334 - 2.0743) / 5 + 2.0743;
			break;
		case 31:
		case 32:
		case 33:
		case 34:
		case 35:
			weight = (size - 30) * (2.3895 - 2.2334) / 5 + 2.2334;
			break;
		case 36:
		case 37:
		case 38:
		case 39:
		case 40:
			weight = (size - 35) * (2.5356 - 2.3895) / 5 + 2.3895;
			break;
		case 41:
		case 42:
		case 43:
		case 44:
		case 45:
			weight = (size - 40) * (2.6625 - 2.5356) / 5 + 2.5356;
			break;
		case 46:
		case 47:
		case 48:
		case 49:
		case 50:
			weight = (size - 45) * (2.7933 - 2.6625) / 5 + 2.6625;
			break;
		default:
			weight = (size - 50) * 0.02616 + 2.7933;
			break;
		}
		return weight;
	}
	
	private double getUtilization(int nbBlocks, double areaXDownBoundHalf, double areaXUpBoundHalf, 
										double areaYDownBoundHalf, double areaYUpBoundHalf, String typeName)
	{
		int curNumberOfLegalSites = 0;
		double yTop = areaYUpBoundHalf;
		while(yTop > (architecture.getHeight() + 1.0))
		{
			yTop -= 1.0;
		}
		double yBottom = areaYDownBoundHalf;
		while(yBottom < 0)
		{
			yBottom += 1.0;
		}
		int curNbBlocksPerColumn = (int)Math.round(yTop - yBottom);
		for(int x = (int)Math.round(Math.ceil(areaXDownBoundHalf)); x <= (int)Math.round(Math.floor(areaXUpBoundHalf)); x++)
		{
			if(x >= 1 && x <= architecture.getWidth())
			{
				if(typeName.equals("CLB"))
				{
					if(architecture.getSite(x, 1).getType() == SiteType.CLB)
					{
						curNumberOfLegalSites += curNbBlocksPerColumn;
					}
				}
				else //We are working with hardBlocks
				{
					if(architecture.getSite(x, 1).getType() == SiteType.HARDBLOCK)
					{
						HardBlockSite hbSite = (HardBlockSite)architecture.getSite(x, 1);
						if(hbSite.getTypeName().equals(typeName))
						{
							curNumberOfLegalSites += curNbBlocksPerColumn;
						}
					}
				}
			}
		}
		return ((double)nbBlocks)/((double)curNumberOfLegalSites);
	}
	
	/*
	 * Sort in increasing order of X or Y position xDir = true ==> sort in x
	 * direction, xDir = false ==> sort in Y direction
	 */
	private void sort(boolean xDir, List<Integer> indices, List<Double> positionsX, List<Double> positionsY)
	{
		for (int i = 0; i < indices.size(); i++)
		{
			int minIndex = i;
			double minValue;
			if (xDir)
			{
				minValue = positionsX.get(i);
			} else
			{
				minValue = positionsY.get(i);
			}
			for (int j = i + 1; j < indices.size(); j++)
			{
				if (xDir && positionsX.get(j) < minValue)
				{
					minIndex = j;
					minValue = positionsX.get(j);
				}
				if (!xDir && positionsY.get(j) < minValue)
				{
					minIndex = j;
					minValue = positionsY.get(j);
				}
			}
			if (minIndex != i) // Switch index, X-position and Y-position
								// between i and minIndex
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
	
	/*
	 * Sort in increasing order of X or Y position xDir = true ==> sort in x
	 * direction, xDir = false ==> sort in Y direction
	 */
	private void sort(boolean xDir, int[] indices, int[] positionsX, int[] positionsY)
	{
		for (int i = 0; i < indices.length; i++)
		{
			int minIndex = i;
			int minValue;
			if (xDir)
			{
				minValue = positionsX[i];
			} else
			{
				minValue = positionsY[i];
			}
			for (int j = i + 1; j < indices.length; j++)
			{
				if (xDir && positionsX[j] < minValue)
				{
					minIndex = j;
					minValue = positionsX[j];
				}
				if (!xDir && positionsY[j] < minValue)
				{
					minIndex = j;
					minValue = positionsY[j];
				}
			}
			if (minIndex != i)
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
	
}
