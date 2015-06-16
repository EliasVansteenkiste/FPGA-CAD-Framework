package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import circuit.Clb;
import circuit.Net;

import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import architecture.Site;
import architecture.SiteType;

public class HeteroLegalizerOne
{

	private final double UTILIZATION_FACTOR = 0.9;
	
	private int[] bestLegalX;
	private int[] bestLegalY;
	private double currentCost;
	private HeterogeneousArchitecture architecture;
	private int[] typeStartIndices;
	private String[] typeNames;
	
	public HeteroLegalizerOne(HeterogeneousArchitecture architecture, int[] typeStartIndices, String[] typeNames, int nbMovableBlocks)
	{
		this.bestLegalX = new int[nbMovableBlocks];
		this.bestLegalY = new int[nbMovableBlocks];
		this.currentCost = Double.MAX_VALUE;
		this.architecture = architecture;
		this.typeStartIndices = typeStartIndices;
		this.typeNames = typeNames;
	}
	
	public void legalize(double[] linearX, double[] linearY, Collection<Net> nets, Map<Clb,Integer> indexMap, int solveMode)
	{
		
	}
	
	/*
	 * EndIndex is the last index which is not of the considered block type anymore
	 */
	private void subTypeLegalize(int startIndex, int endIndex, double[] linearX, double[] linearY, String typeName)
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
						if (linearX[currentIndex] >= areaXUpBoundHalf && linearX[currentIndex] < areaXUpBoundHalf + 1.0 && linearY[currentIndex] >= y
								&& linearY[currentIndex] < y + 1.0)
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
						if (linearX[currentIndex] >= x && linearX[currentIndex] < x + 1.0 && linearY[currentIndex] >= areaYDownBoundHalf - 1.0
								&& linearY[currentIndex] < areaYDownBoundHalf)
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
			
			if(indices.size() == 1)
			{
				int x = (int)Math.round(positionsX.get(0));
				int y = (int)Math.round(positionsY.get(0));
				Site site = architecture.getSite(x, y, 0);
				if(typeName.equals("CLB"))
				{
					if(site.type == SiteType.CLB)
					{
						int index = indices.get(0);
						semiLegalX[index] = x;
						semiLegalY[index] = y;
						continue;
					}
				}
				else
				{
					if(site.type == SiteType.HARDBLOCK)
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
			while (curUtilization >= UTILIZATION_FACTOR || (areaXDownBoundHalf < minimalX && areaXUpBoundHalf > maximalX && 
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
			int areaXUpBound = (int) Math.ceil(areaXUpBoundHalf);
			int areaYDownBound = (int) Math.ceil(areaYDownBoundHalf);
			int areaYUpBound = (int) Math.ceil(areaYUpBoundHalf);
			
			while (areaXDownBound < minimalX)
			{
				areaXDownBound++;
				areaXUpBound++;
			}
			while (areaXUpBound > maximalX + 1)
			{
				areaXDownBound--;
				areaXUpBound--;
			}
			while (areaYDownBound < minimalY)
			{
				areaYDownBound++;
				areaYUpBound++;
			}
			while (areaYUpBound > maximalY + 1)
			{
				areaYDownBound--;
				areaYUpBound--;
			}
			
			//Identify rectangles in which we can put the considered blocks
			ArrayList<Integer> rectangleStartX = new ArrayList<>();
			ArrayList<Integer> rectangleStopX = new ArrayList<>();
			boolean alreadyRunning = false;
			for(int x = areaXDownBound; x < areaXUpBound; x++)
			{
				Site site = architecture.getSite(x, 1, 0);
				if(typeName.equals("CLB"))
				{
					if(site.type == SiteType.CLB)
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
					if(site.type == SiteType.HARDBLOCK)
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
			for(int i = 0; i < rectangleStopX.size(); i++)
			{
				ArrayList<Integer> rectangleIndices = new ArrayList<>();
				ArrayList<Double> rectanglePositionsX = new ArrayList<>();
				ArrayList<Double> rectanglePositionsY = new ArrayList<>();
				int totalBlocksRemaining = positionsX.size();
				for(int j = 0; j < totalBlocksRemaining; j++)
				{
					rectangleIndices.add(indices.remove(j));
					rectanglePositionsX.add(positionsX.remove(j));
					rectanglePositionsY.add(positionsY.remove(j));
				}
				cutAndSpread(cutDir, rectangleIndices, rectanglePositionsX, rectanglePositionsY, rectangleStartX.get(i), 
									rectangleStopX.get(i), areaYDownBound, areaYUpBound, semiLegalX, semiLegalY);
			}
			
		}
	}
	
	private double getUtilization(int nbBlocks, double areaXDownBoundHalf, double areaXUpBoundHalf, double areaYDownBoundHalf, 
															double areaYUpBoundHalf, String typeName)
	{
		int curNumberOfLegalSites = 0;
		int curNbBlocksPerColumn = (int)Math.round(Math.floor(areaYUpBoundHalf) - Math.floor(areaYDownBoundHalf));
		for(int x = (int)Math.round(Math.ceil(areaXDownBoundHalf)); x <= (int)Math.round(Math.floor(areaXUpBoundHalf)); x++)
		{
			if(x >= 1 && x <= architecture.getWidth())
			{
				if(typeName.equals("CLB"))
				{
					if(architecture.getSite(x, 1, 0).type == SiteType.CLB)
					{
						curNumberOfLegalSites += curNbBlocksPerColumn;
					}
				}
				else //We are working with hardBlocks
				{
					if(architecture.getSite(x, 1, 0).type == SiteType.HARDBLOCK)
					{
						HardBlockSite hbSite = (HardBlockSite)architecture.getSite(x, 1, 0);
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
	
}
