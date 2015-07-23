package architecture;

import java.util.ArrayList;
import java.util.Vector;

import circuit.HardBlock;
import circuit.PackedCircuit;

public class HeterogeneousArchitecture extends Architecture
{
	
	private static final double FILL_GRADE = 1.20;
	
	private int width;
	private int height;
	private Site[][][] siteArray;
	private String[] hardBlockTypeNames;
	private ArrayList<Site> ISites;
	private ArrayList<Site> OSites;
	
	/*
	 * A heterogeneousArchitecture is always fitted to a circuit
	 */
	public HeterogeneousArchitecture(PackedCircuit circuit)
	{
		int nbClbs = circuit.clbs.values().size();
		int nbInputs = circuit.getInputs().values().size();
		int nbOutputs = circuit.getOutputs().values().size();
		ISites = new ArrayList<>();
		OSites = new ArrayList<>();
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
	
//	/*
//	 * A heterogeneousArchitecture is always fitted to a circuit
//	 */
//	public HeterogeneousArchitecture(PackedCircuit circuit)
//	{
//		int nbClbs = circuit.clbs.values().size();
//		int nbInputs = circuit.getInputs().values().size();
//		int nbOutputs = circuit.getOutputs().values().size();
//		int nbMemories = 0;
//		int nbMult36 = 0;
//		for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
//		{
//			if(hbVector.get(0).getTypeName().equals("memory"))
//			{
//				nbMemories = hbVector.size();
//			}
//			else
//			{
//				if(hbVector.get(0).getTypeName().equals("mult_36"))
//				{
//					nbMult36 = hbVector.size();
//				}
//				else
//				{
//					System.err.println("Wrong type of hardblock in the circuit!");
//				}
//			}
//		}
//		ISites = new ArrayList<>();
//		OSites = new ArrayList<>();
//		hardBlockTypeNames = new String[2];
//		hardBlockTypeNames[0] = "memory";
//		hardBlockTypeNames[1] = "mult_36";
//		
//		//Create an x by x architecture which ensures that we can house all IOs, CLBs, memories and mult*36s
//		int x = 1;
//		int roomForInputs = x * 4;
//		int roomForOutputs = x * 4;
//		int roomForMemorieColumns = (x + 6) / 8;
//		int roomForMemories = roomForMemorieColumns * x;
//		int roomForMult36Columns = (x + 2) / 8;
//		int roomForMult36s = roomForMult36Columns * x;
//		int roomForClbs = (x - roomForMemorieColumns - roomForMult36Columns) * x;
//		while(roomForInputs < nbInputs || roomForOutputs < nbOutputs || roomForMemories < nbMemories || 
//																roomForMult36s < nbMult36 || roomForClbs < nbClbs)
//		{
//			x++;
//			roomForInputs = x * 4;
//			roomForOutputs = x * 4;
//			roomForMemorieColumns = (x + 6) / 8;
//			roomForMemories = roomForMemorieColumns * x;
//			roomForMult36Columns = (x + 2) / 8;
//			roomForMult36s = roomForMult36Columns * x;
//			roomForClbs = (x - roomForMemorieColumns - roomForMult36Columns) * x;
//		}
//		
//		width = x;
//		height = x;
//		siteArray = new Site[width+2][height+2][2];
//		for(int i = 0; i < width+2; i++)
//		{
//			if(i == 0 || i == width+1)
//			{
//				insertIOColumn(i);
//			}
//			else
//			{
//				if((i+6)%8 == 0)
//				{
//					insertHardBlockColumn(i, "memory");
//				}
//				else
//				{
//					if((i+2)%8 == 0)
//					{
//						insertHardBlockColumn(i, "mult_36");
//					}
//					else
//					{
//						insertClbColumn(i);
//					}
//				}
//			}
//		}
//	}
	
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
		return ISites;
	}
	
	public ArrayList<Site> getOSites()
	{
		return OSites;
	}
	
	public Site randomClbSite(int Rlim, Site pl1)
	{
		Site pl2;
		int minX = pl1.x - Rlim;
		if(minX < 1)
		{
			minX = 1;
		}
		int maxX = pl1.x + Rlim;
		if(maxX > width)
		{
			maxX = width;
		}
		int minY = pl1.y - Rlim;
		if(minY < 1)
		{
			minY = 1;
		}
		int maxY = pl1.y + Rlim;
		if(maxY > height)
		{
			maxY = height;
		}
		do
		{
			int x_to = rand.nextInt(maxX - minX + 1) + minX;
			int y_to = rand.nextInt(maxY - minY + 1) + minY;
			pl2 = siteArray[x_to][y_to][0];
			if(pl2.type == SiteType.HARDBLOCK)
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
		int minX = pl1.x - Rlim;
		if(minX < 1)
		{
			minX = 1;
		}
		int maxX = pl1.x + Rlim;
		if(maxX > width)
		{
			maxX = width;
		}
		int minY = pl1.y - Rlim;
		if(minY < 1)
		{
			minY = 1;
		}
		int maxY = pl1.y + Rlim;
		if(maxY > height)
		{
			maxY = height;
		}
		while((siteArray[minX][1][0]).type != SiteType.HARDBLOCK)
		{
			minX++;
		}
		while((siteArray[maxX][1][0]).type != SiteType.HARDBLOCK)
		{
			maxX--;
		}
		do
		{
			int x_to = rand.nextInt(maxX - minX + 1) + minX;
			int y_to = rand.nextInt(maxY - minY + 1) + minY;
			pl2 = siteArray[x_to][y_to][0];
			if(pl2.type == SiteType.HARDBLOCK)
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
	
	public Site randomISite(int Rlim, Site pl1)
	{
		Site pl2 = null;
		int manhattanDistance = -1;
		do
		{
			pl2 = ISites.get(rand.nextInt(ISites.size()));
			if(pl2 == null)
			{
				System.out.println("woops");
			}
			manhattanDistance = Math.abs(pl1.x - pl2.x) + Math.abs(pl1.y - pl2.y);
		}while(pl1 == pl2 || manhattanDistance > Rlim);
		return pl2;
	}
	
	public Site randomOSite(int Rlim, Site pl1)
	{
		Site pl2 = null;
		int manhattanDistance = -1;
		do
		{
			pl2 = OSites.get(rand.nextInt(OSites.size()));
			if(pl2 == null)
			{
				System.out.println("woops");
			}
			manhattanDistance = Math.abs(pl1.x - pl2.x) + Math.abs(pl1.y - pl2.y);
		}while(pl1 == pl2 || manhattanDistance > Rlim);
		return pl2;
	}
	
	private void insertClbColumn(int x)
	{
		putIoSite(x,0,0);
		putIoSite(x,0,1);
		for(int y = 1; y < height + 1; y++)
		{
			addSite(new ClbSite("Site_"+x+"_"+y+"_"+0, x,y, 0), x, y, 0);
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
	
	private void putIoSite(int x, int y, int n)
	{
		IoSite site = new IoSite("Site_"+x+"_"+y+"_"+n, x, y,n);
		addSite(site, x, y, n);
		if(n == 0)
		{
			ISites.add(site);
		}
		else
		{
			OSites.add(site);
		}
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
