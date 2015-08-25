package architecture;

import java.util.Vector;

import circuit.PackedCircuit;

public class FourLutSanitized extends Architecture
{

	private Vector<Site> IOSites;
	private int IOSiteCapacity;
	
	private static final double FILL_GRADE = 1.20;
	
	public FourLutSanitized(PackedCircuit circuit, int IOSiteCapacity)
	{
		this(FourLutSanitized.calculateSquareArchDimensions(circuit, IOSiteCapacity), IOSiteCapacity);
	}
	
	public FourLutSanitized(int dimension, int IOSiteCapacity) {
		this(dimension, dimension, IOSiteCapacity);
	}
	
	private FourLutSanitized(int width, int height, int IOSiteCapacity)
	{
		super();
		this.width = width;
		this.height = height;
		this.IOSiteCapacity = IOSiteCapacity;
		this.IOSites = new Vector<>();
		
		siteArray = new Site[width+2][height+2];
				
		//Generating the IO blocks
		for(int y = 1; y < height+1; y++)
		{
			putIoSite(0, y);
			putIoSite(width+1, y);
		}
		
		for(int x = 1; x < width+1; x++)
		{
			putIoSite(x, 0);
			putIoSite(x, height+1);
		}
		
		//Generate CLBs
		for(int x = 1; x <= width; x++)
		{
			for(int y = 1; y <= height; y++)
			{
				putClbSite(x,y);
			}
		}
	}
	
	public static int calculateSquareArchDimensions(PackedCircuit circuit, int IOSiteCapacity)
	{
		int nbInputs = circuit.getInputs().values().size();
		int nbOutputs = circuit.getOutputs().values().size();
		int nbClbs = circuit.clbs.values().size();
		
		int nbIOSites = (int)Math.ceil((double)(nbInputs + nbOutputs) / IOSiteCapacity);
		int size1 = (int)Math.ceil((double)nbIOSites / 4);
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
		return size;
	}
	
	public Site randomClbSite(int Rlim, Site pl1)
	{
		Site pl2;
		do
		{
			//-1 is nodig om de coordinaten in clbPlaatsArray te verkrijgen.	
			int x_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.getX();	
			if(x_to<1)
				x_to+=width;
			if(x_to>= width+1)
				x_to-=width;
			int y_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.getY();					
			if(y_to<1)
				y_to+=height;
			if(y_to>= height+1)
				y_to-=height;
			pl2=siteArray[x_to][y_to];
		}while(pl1==pl2);
		return pl2;
	}
	
	public Site randomHardBlockSite(int Rlim, HardBlockSite pl1) {
		return null;
	}
	
	public Site randomIOSite(int Rlim, Site pl1)
	{
		Site pl2 = null;
		int manhattanDistance = -1;
		do{
			pl2 = IOSites.elementAt(rand.nextInt(IOSites.size()));
			if(pl2==null)
				System.out.println("woops");
			manhattanDistance = Math.abs(pl1.getX()-pl2.getX())+Math.abs(pl1.getY()-pl2.getY());
		}while (pl1==pl2||manhattanDistance>Rlim);
		return pl2;
	}
	
	public Site getIOSite(int index)
	{
		return IOSites.get(index);
	}
	
	private void putIoSite(int x, int y)
	{
		IoSite site = new IoSite(x, y, IOSiteCapacity);
		addSite(site, x, y);
		IOSites.add(site);
	}
	
	private void putClbSite(int x,int y)
	{
		ClbSite site = new ClbSite(x, y);
		addSite(site, x, y);
	}

}
