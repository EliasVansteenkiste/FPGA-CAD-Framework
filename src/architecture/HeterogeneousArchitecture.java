package architecture;

import java.util.ArrayList;

import circuit.PackedCircuit;

public class HeterogeneousArchitecture extends Architecture
{
	
	private static final double FILL_GRADE = 1.20;
	
	private int width;
	private int height;
	private Site[][][] siteArray;
	private String[] hardBlockTypeNames;
	
	/*
	 * A heterogeneousArchitecture is always fitted to a circuit
	 */
	public HeterogeneousArchitecture(PackedCircuit circuit)
	{
		int nbClbs = circuit.clbs.values().size();
		int nbInputs = circuit.getInputs().values().size();
		int nbOutputs = circuit.getOutputs().values().size();
		int[] nbHardBlocksPerType = new int[circuit.getHardBlocks().size()];
		hardBlockTypeNames = new String[nbHardBlocksPerType.length];
		for(int i = 0; i < nbHardBlocksPerType.length; i++)
		{
			nbHardBlocksPerType[i] = circuit.getHardBlocks().get(i).size();
			hardBlockTypeNames[i] = circuit.getHardBlocks().get(i).get(0).getTypeName();
		}
		
		//Create an x by x architecture which ensures that we can house all IO's and have enough CLB sites
		int maxIO;
		if(nbInputs > nbOutputs)
		{
			maxIO = nbInputs;
		}
		else
		{
			maxIO = nbOutputs;
		}
		int size1 = (maxIO + 3) / 4;
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
		siteArray = new Site[width+2][height+2][2];
		int deltaHardBlockColumns = size / (totalNbHardBlockColumns + 1);
		int leftToPlace = totalNbHardBlockColumns;
		int nextLeft = deltaHardBlockColumns;
		int nextRight = deltaHardBlockColumns * totalNbHardBlockColumns;
		while(leftToPlace > 0)
		{
			
			int leftMaxIndex = getMaxIndex(nbHardBlocksPerType);
			insertHardBlockColumn(nextLeft, hardBlockTypeNames[leftMaxIndex]);
			nbHardBlocksPerType[leftMaxIndex]--;
			leftToPlace--;
			if(nextLeft != nextRight)
			{
				int rightMaxIndex = getMaxIndex(nbHardBlocksPerType);
				insertHardBlockColumn(nextRight, hardBlockTypeNames[rightMaxIndex]);
				nbHardBlocksPerType[rightMaxIndex]--;
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
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public Site getSite(int x, int y, int n)
	{
		return siteArray[x][y][n];
	}
	
	public String[] getHardBlockTypeNames()
	{
		return hardBlockTypeNames;
	}
	
	public ArrayList<Site> getISites()
	{
		ArrayList<Site> toReturn = new ArrayList<>();
		for(int y = 1; y < height + 1; y++)
		{
			toReturn.add(siteArray[0][y][0]);
			toReturn.add(siteArray[width+1][y][0]);
		}
		for(int x = 1; x < width + 1; x++)
		{
			toReturn.add(siteArray[x][0][0]);
			toReturn.add(siteArray[x][height+1][0]);
		}
		return toReturn;
	}
	
	public ArrayList<Site> getOSites()
	{
		ArrayList<Site> toReturn = new ArrayList<>();
		for(int y = 1; y < height + 1; y++)
		{
			toReturn.add(siteArray[0][y][1]);
			toReturn.add(siteArray[width+1][y][1]);
		}
		for(int x = 1; x < width + 1; x++)
		{
			toReturn.add(siteArray[x][0][1]);
			toReturn.add(siteArray[x][height+1][1]);
		}
		return toReturn;
	}
	
	private void insertClbColumn(int x)
	{
		putIoSite(x,0,0);
		putIoSite(x,0,1);
		for(int y = 1; y < height + 1; y++)
		{
			putClbSite(x,y,0);
		}
		putIoSite(x,height+1,0);
		putIoSite(x,height+1,1);
	}
	
	private void insertIOColumn(int x)
	{
		for(int y = 1; y < height + 1; y++)
		{
			putIoSite(x,y,0);
			putIoSite(x,y,1);
		}
	}
	
	private void insertHardBlockColumn(int x, String typeName)
	{
		putIoSite(x,0,0);
		putIoSite(x,0,1);
		for(int y = 1; y < height + 1; y++)
		{
			addSite(new HardBlockSite("Site_"+x+"_"+y+"_"+0, x, y, 0, typeName), x, y, 0);
		}
		putIoSite(x,height+1,0);
		putIoSite(x,height+1,1);
	}
	
	private void putClbSite(int x, int y, int n)
	{
		addSite(new ClbSite("Site_"+x+"_"+y+"_"+n, x,y, n), x, y, n);
	}
	
	private void putIoSite(int x, int y, int n)
	{
		addSite(new IoSite("Site_"+x+"_"+y+"_"+n, x, y,n), x, y, n);
	}
	
	private void addSite(Site site, int x, int y, int n)
	{
		siteArray[x][y][n] = site;
		siteVector.add(site);
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
