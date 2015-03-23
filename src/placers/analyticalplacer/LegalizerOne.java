package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.Net;
import circuit.Pin;

/*
 * Uses repetitive partitioning
 * Takes blocks in cluster when spreading
 * Legalizes completely: no overlap in anchor points
 */
public class LegalizerOne implements Legalizer
{
	
	private final double UTILIZATION_FACTOR = 0.9;
	
	private int minimalX;
	private int maximalX;
	private int minimalY;
	private int maximalY;
	private int[] bestLegalX;
	private int[] bestLegalY;
	private double currentCost;
	
	public LegalizerOne(int minimalX, int maximalX, int minimalY, int maximalY, int nbMovableBlocks)
	{
		this.bestLegalX = new int[nbMovableBlocks];
		this.bestLegalY = new int[nbMovableBlocks];
		this.minimalX = minimalX;
		this.maximalX = maximalX;
		this.minimalY = minimalY;
		this.maximalY = maximalY;
		currentCost = Double.MAX_VALUE;
	}
	
	public void legalize(double[] linearX, double[] linearY, Collection<Net> nets, Map<Clb,Integer> indexMap)
	{
		int[] semiLegalX = new int[bestLegalX.length];
		int[] semiLegalY = new int[bestLegalY.length];
		clusterCutSpreadRecursive(linearX, linearY, semiLegalX, semiLegalY);
		
		int[] legalX = new int[semiLegalX.length];
		int[] legalY = new int[semiLegalY.length];
		finalLegalization(semiLegalX, semiLegalY, legalX, legalY);
		
		updateBestLegal(legalX, legalY, nets, indexMap);
	}
	
	private void clusterCutSpreadRecursive(double[] linearX, double[] linearY, int[] semiLegalX, int[] semiLegalY)
	{
		List<Integer> todo = new ArrayList<>();
		for(int i = 0; i < linearX.length; i++)
		{
			todo.add(i);
		}
		while(todo.size() != 0)
		{
			//Cluster
			double areaXUpBoundHalf = 0.0;
			double areaXDownBoundHalf = 0.0;
			double areaYUpBoundHalf = 0.0;
			double areaYDownBoundHalf = 0.0;
			List<Integer> indices = new ArrayList<>();
			List<Double> positionsX = new ArrayList<>();
			List<Double> positionsY = new ArrayList<>();
			
			//Find a starting point for the cluster
			int startIndex = todo.get(0);
			areaXUpBoundHalf = (double)Math.round(linearX[startIndex]) + 0.5;
			areaXDownBoundHalf = (double)Math.round(linearX[startIndex]) - 0.5;
			areaYUpBoundHalf = (double)Math.round(linearY[startIndex]) + 0.5;
			areaYDownBoundHalf = (double)Math.round(linearY[startIndex]) - 0.5;
			indices.add(startIndex);
			positionsX.add(linearX[startIndex]);
			positionsY.add(linearY[startIndex]);
			todo.remove(0);
			for(int i = 0; i < todo.size(); i++)
			{
				int currentIndex = todo.get(i);
				if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
						linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
				for(double y = areaYDownBoundHalf; y < areaYUpBoundHalf; y += 1.0)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXUpBoundHalf && linearX[currentIndex] < areaXUpBoundHalf+1.0 && 
								linearY[currentIndex] >= y && linearY[currentIndex] < y+1.0)
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
					areaXUpBoundHalf += 1.0;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
								linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
				for(double x = areaXDownBoundHalf; x < areaXUpBoundHalf; x += 1.0)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= x && linearX[currentIndex] < x+1.0 && 
								linearY[currentIndex] >= areaYDownBoundHalf-1.0 && linearY[currentIndex] < areaYDownBoundHalf)
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
					areaYDownBoundHalf -= 1.0;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
								linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
				for(double y = areaYDownBoundHalf; y < areaYUpBoundHalf; y += 1.0)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBoundHalf-1.0 && linearX[currentIndex] < areaXDownBoundHalf && 
								linearY[currentIndex] >= y && linearY[currentIndex] < y+1.0)
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
					areaXDownBoundHalf -= 1.0;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
								linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
				for(double x = areaXDownBoundHalf; x < areaXUpBoundHalf; x += 1.0)
				{
					int nbCells = 0;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= x && linearX[currentIndex] < x+1.0 && 
								linearY[currentIndex] >= areaYUpBoundHalf && linearY[currentIndex] < areaYUpBoundHalf+1.0)
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
					areaYUpBoundHalf += 1.0;
					expanded = true;
					for(int i = 0; i < todo.size(); i++)
					{
						int currentIndex = todo.get(i);
						if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
								linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
				semiLegalX[index] = x;
				semiLegalY[index] = y;
				//System.out.printf("Index: %d, X: %d, Y: %d\n", indices.get(0), x, y);
				//System.out.println();
				continue;
			}
			
			
			
			
			//Grow area until not overutilized
			double curUtilization = (double)positionsX.size() / ((areaXUpBoundHalf - areaXDownBoundHalf) * (areaYUpBoundHalf - areaYDownBoundHalf));
			//System.out.printf("Utilization: %.3f\n", curUtilization);
			int curDirection = 0; //0 = right, 1 = top, 2 = left, 3 = bottom
			while(curUtilization >= UTILIZATION_FACTOR)
			{
				switch(curDirection)
				{
					case 0: //Grow to the right if possible
						if(areaXUpBoundHalf <= maximalX+1.0)
						{
							areaXUpBoundHalf += 1.0;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
										linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
						if(areaYDownBoundHalf >= minimalY-1.0)
						{
							areaYDownBoundHalf -= 1.0;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
										linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
						if(areaXDownBoundHalf >= minimalX-1.0)
						{
							areaXDownBoundHalf -= 1.0;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
										linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
						if(areaYUpBoundHalf <= maximalY+1.0)
						{
							areaYUpBoundHalf += 1.0;
							
							//Add blocks which are not in the cluster yet
							for(int i = 0; i < todo.size(); i++)
							{
								int currentIndex = todo.get(i);
								if(linearX[currentIndex] >= areaXDownBoundHalf && linearX[currentIndex] < areaXUpBoundHalf && 
										linearY[currentIndex] >= areaYDownBoundHalf && linearY[currentIndex] < areaYUpBoundHalf)
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
				curUtilization = (double)positionsX.size() / ((areaXUpBoundHalf - areaXDownBoundHalf) * (areaYUpBoundHalf - areaYDownBoundHalf));
				curDirection++;
				curDirection %= 4;
			}
			//System.out.println("New limits: XDown: " + areaXDownBound + ", XUp: " + areaXUpBound + ", YDown: " + areaYDownBound + ", YUp: " + areaYUpBound);
			//System.out.printf("New utilization: %.3f\n", curUtilization);
			
			
			
			
			//Cut and spread
			boolean cutDir = true; //Initial cut is horizontally
			if(areaYUpBoundHalf - areaYDownBoundHalf <= 1.01) //Check if it is possible to cut horizontally, watch out for rounding errors
			{
				cutDir = false; //Cut vertically if not possible to cut horizontally
			}
			//Change double boundaries on half coordinates in integer boundaries on integer coordinates
			int areaXDownBound = (int)Math.ceil(areaXDownBoundHalf);
			int areaXUpBound = (int)Math.ceil(areaXUpBoundHalf);
			int areaYDownBound = (int)Math.ceil(areaYDownBoundHalf);
			int areaYUpBound = (int)Math.ceil(areaYUpBoundHalf);
			cutAndSpread(cutDir, indices, positionsX, positionsY, areaXDownBound, areaXUpBound, areaYDownBound, areaYUpBound, 
							semiLegalX, semiLegalY); 

			//System.out.println();
		}
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
		
		//Target cut
		int cutPosition;
		int area1; //Top or left
		int area2; //Bottom or right
		//area1 will always be smaller or equal to area2
		if(horCutDirection) //Cut horizontally
		{
			cutPosition = (areaYDownBound + areaYUpBound) / 2;
			area1 = (areaXUpBound - areaXDownBound) * (cutPosition - areaYDownBound);
			area2 = (areaXUpBound - areaXDownBound) * (areaYUpBound - cutPosition);
		}
		else //Cut vertically
		{
			cutPosition = (areaXDownBound + areaXUpBound) / 2;
			area1 = (cutPosition - areaXDownBound) * (areaYUpBound - areaYDownBound);
			area2 = (areaXUpBound - cutPosition) * (areaYUpBound - areaYDownBound);
		}
		
		//Source cut
		int endIndex = indices.size();
		double totalUtilization = ((double)positionsX.size())/((double)(area1 + area2));
		int cutIndex = (int)Math.round(area1*totalUtilization);
		List<Integer> indices1 = new ArrayList<>();
		List<Double> positionsX1 = new ArrayList<>();
		List<Double> positionsY1 = new ArrayList<>();
		for(int i = 0; i < cutIndex; i++)
		{
			indices1.add(indices.get(i));
			positionsX1.add(positionsX.get(i));
			positionsY1.add(positionsY.get(i));
		}
		List<Integer> indices2 = new ArrayList<>();
		List<Double> positionsX2 = new ArrayList<>();
		List<Double> positionsY2 = new ArrayList<>();
		for(int i = cutIndex; i < endIndex; i++)
		{
			indices2.add(indices.get(i));
			positionsX2.add(positionsX.get(i));
			positionsY2.add(positionsY.get(i));
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
	private void finalLegalization(int[] semiLegalXPositions, int[] semiLegalYPositions, int[] legalX, int[] legalY)
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
	
	private void updateBestLegal(int[] legalX, int[] legalY, Collection<Net> nets, Map<Clb,Integer> indexMap)
	{
		double newCost = calculateTotalCost(legalX, legalY, nets, indexMap);
		if(newCost < currentCost)
		{
			for(int i = 0; i < legalX.length; i++)
			{
				bestLegalX[i] = legalX[i];
				bestLegalY[i] = legalY[i];
			}
			currentCost = newCost;
		}
	}
	
	public double calculateBestLegalCost(Collection<Net> nets, Map<Clb,Integer> indexMap)
	{
		return calculateTotalCost(bestLegalX, bestLegalY, nets, indexMap);
	}
	
	private double calculateTotalCost(int[] xArray, int[] yArray, Collection<Net> nets, Map<Clb,Integer> indexMap)
	{
		double cost = 0.0;
		for(Net net:nets)
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
	
	public void getAnchorPoints(int[] anchorX, int[] anchorY)
	{
		for(int i = 0; i < bestLegalX.length; i++)
		{
			anchorX[i] = bestLegalX[i];
			anchorY[i] = bestLegalY[i];
		}
	}
	
	public void getBestLegal(int[] bestX, int[] bestY)
	{
		if(bestX == null || bestY == null)
		{
			System.err.println("Legalizer: X and Y arrays must be initialized before passing them to getBestLegal!");
		}
		else
		{
			for(int i = 0; i < bestLegalX.length; i++)
			{
				bestX[i] = bestLegalX[i];
				bestY[i] = bestLegalY[i];
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
	
}
