package architecture;

import java.util.ArrayList;

import circuit.PackedCircuit;

public class HeterogeneousArchitecture extends Architecture
{
	
	private static final double FILL_GRADE = 1.20;
	
	private String[] hardBlockTypeNames;
	private ArrayList<IoSite> IOSites;
	private int IOTileCapacity;
	
	/*
	 * A heterogeneousArchitecture is always fitted to a circuit
	 */
	public HeterogeneousArchitecture(PackedCircuit circuit, int IOTileCapacity)
	{
		this.IOTileCapacity = IOTileCapacity;
		int nbClbs = circuit.clbs.values().size();
		int nbInputs = circuit.getInputs().values().size();
		int nbOutputs = circuit.getOutputs().values().size();
		IOSites = new ArrayList<>();
		int[] nbHardBlocksPerType = new int[circuit.getHardBlocks().size()];
		hardBlockTypeNames = new String[nbHardBlocksPerType.length];
		for(int i = 0; i < nbHardBlocksPerType.length; i++)
		{
			nbHardBlocksPerType[i] = circuit.getHardBlocks().get(i).size();
			hardBlockTypeNames[i] = circuit.getHardBlocks().get(i).get(0).getTypeName();
		}
		
		//Create an x by x architecture which ensures that we can house all IO's and have enough CLB sites

		int nbIOTiles = (int)Math.ceil((double)(nbInputs + nbOutputs) / IOTileCapacity);
		int size1 = (int)Math.ceil((double)nbIOTiles / 4);
		int size2 = (int)Math.ceil(Math.sqrt(nbClbs * FILL_GRADE));
		int size;
		if(size1 > size2)
		{
			size = size1;
		}
		else
		{
			size = size2;
		}
		
		//Enlarge the square architecture to also be able to add sufficient hardblock columns
		int[] nbColumnsPerType = new int[nbHardBlocksPerType.length];
		int totalNbHardBlockColumns = 0;
		for(int i = 0; i < nbHardBlocksPerType.length; i++)
		{
			int nbBlocksLeft = nbHardBlocksPerType[i];
			while(nbBlocksLeft > 0)
			{
				size++;
				nbBlocksLeft -= size;
				nbColumnsPerType[i] = nbColumnsPerType[i] + 1;
				totalNbHardBlockColumns++;
			}
		}
		
		//Insert hard blocks in array
		width = size;
		height = size;
		tileArray = new GridTile[width+2][height+2];
		int deltaHardBlockColumns = size / (totalNbHardBlockColumns + 1);
		int leftToPlace = totalNbHardBlockColumns;
		int nextLeft = deltaHardBlockColumns;
		int nextRight = deltaHardBlockColumns * totalNbHardBlockColumns;
		while(leftToPlace > 0)
		{
			
			int leftMaxIndex = getMaxIndex(nbColumnsPerType);
			insertHardBlockColumn(nextLeft, hardBlockTypeNames[leftMaxIndex]);
			nbColumnsPerType[leftMaxIndex]--;
			leftToPlace--;
			if(nextLeft != nextRight)
			{
				int rightMaxIndex = getMaxIndex(nbColumnsPerType);
				insertHardBlockColumn(nextRight, hardBlockTypeNames[rightMaxIndex]);
				nbColumnsPerType[rightMaxIndex]--;
				leftToPlace--;
			}
			nextLeft += deltaHardBlockColumns;
			nextRight -= deltaHardBlockColumns;
		}
		
		//insert IO blocks and Clb blocks
		for(int x = 0; x < width+2; x++)
		{
			if(x == 0 || x == width+1)//This is an IO column
			{
				insertIOColumn(x);
			}
			else
			{
				if(!hardBlocksInThisColumn(x,deltaHardBlockColumns,totalNbHardBlockColumns))
				{
					insertClbColumn(x);
				}
			}
		}
	}
	
	
	
	public String[] getHardBlockTypeNames()
	{
		return hardBlockTypeNames;
	}
	
	public ArrayList<IoSite> getIOSites()
	{
		return IOSites;
	}
	
	public Site randomClbSite(int Rlim, Site pl1)
	{
		Site pl2;
		int minX = pl1.getX() - Rlim;
		if(minX < 1)
		{
			minX = 1;
		}
		int maxX = pl1.getX() + Rlim;
		if(maxX > width)
		{
			maxX = width;
		}
		int minY = pl1.getY() - Rlim;
		if(minY < 1)
		{
			minY = 1;
		}
		int maxY = pl1.getY() + Rlim;
		if(maxY > height)
		{
			maxY = height;
		}
		do
		{
			int x_to = rand.nextInt(maxX - minX + 1) + minX;
			int y_to = rand.nextInt(maxY - minY + 1) + minY;
			pl2 = tileArray[x_to][y_to].getSite(0);
			if(pl2.getType() == SiteType.HARDBLOCK)
			{
				pl2 = null;
			}
		}while(pl2 == null || pl1 == pl2);
		return pl2;
	}
	
	public Site randomHardBlockSite(int Rlim, HardBlockSite pl1)
	{
		String typeName = pl1.getTypeName();
		Site pl2 = null;
		int minX = pl1.getX() - Rlim;
		if(minX < 1)
		{
			minX = 1;
		}
		int maxX = pl1.getX() + Rlim;
		if(maxX > width)
		{
			maxX = width;
		}
		int minY = pl1.getY() - Rlim;
		if(minY < 1)
		{
			minY = 1;
		}
		int maxY = pl1.getY() + Rlim;
		if(maxY > height)
		{
			maxY = height;
		}
		while((tileArray[minX][1]).getType() != SiteType.HARDBLOCK)
		{
			minX++;
		}
		while((tileArray[maxX][1]).getType() != SiteType.HARDBLOCK)
		{
			maxX--;
		}
		do
		{
			int x_to = rand.nextInt(maxX - minX + 1) + minX;
			int y_to = rand.nextInt(maxY - minY + 1) + minY;
			pl2 = tileArray[x_to][y_to].getSite(0);
			if(pl2.getType() == SiteType.HARDBLOCK)
			{
				if(!((HardBlockSite)pl2).getTypeName().contains(typeName))
				{
					pl2 = null;
				}
			}
			else
			{
				pl2 = null;
			}
		}while(pl2 == null || pl1 == pl2);
		return pl2;
	}
	
	public Site randomIOSite(int Rlim, Site pl1)
	{
		Site pl2 = null;
		int manhattanDistance = -1;
		do
		{
			pl2 = IOSites.get(rand.nextInt(IOSites.size()));
			manhattanDistance = Math.abs(pl1.getX() - pl2.getX()) + Math.abs(pl1.getY() - pl2.getY());
		}while(pl1 == pl2 || manhattanDistance > Rlim || manhattanDistance == 0);
		return pl2;
	}
	
	private void insertClbColumn(int x)
	{
		putIoTile(x,0);
		for(int y = 1; y < height + 1; y++)
		{
			GridTile clbTile = GridTile.constructClbGridTile(x, y);
			addTile(clbTile);
		}
		putIoTile(x,height+1);
	}
	
	private void insertIOColumn(int x)
	{
		for(int y = 1; y < height + 1; y++)
		{
			putIoTile(x,y);
		}
	}
	
	private void insertHardBlockColumn(int x, String typeName)
	{
		putIoTile(x,0);
		for(int y = 1; y < height + 1; y++)
		{
			GridTile hbTile = GridTile.constructHardBlockGridTile(x, y, typeName);
			addTile(hbTile);
		}
		putIoTile(x,height+1);
	}
	
	private void putIoTile(int x, int y)
	{
		GridTile ioTile = GridTile.constructIOGridTile(x, y, IOTileCapacity);
		addTile(ioTile);
		for(int i = 0; i < IOTileCapacity; i++)
		{
			IOSites.add((IoSite)ioTile.getSite(i));
		}
	}
	
	private boolean hardBlocksInThisColumn(int x, int delta, int total)
	{
		int under = (x / delta)*delta;
		if(x != under || x / delta > total)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	private int getMaxIndex(int[] array)
	{
		int curMaxIndex = 0;
		for(int i = 1; i < array.length; i++)
		{
			if(array[i] > array[curMaxIndex])
			{
				curMaxIndex = i;
			}
		}
		return curMaxIndex;
	}
	
}
