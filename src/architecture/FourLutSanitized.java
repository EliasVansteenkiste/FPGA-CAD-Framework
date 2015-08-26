package architecture;

import java.util.Vector;


import circuit.PackedCircuit;

public class FourLutSanitized extends Architecture
{

	private static final double FILL_GRADE = 1.20;
	private int channelWidth;
	
	
	
	public FourLutSanitized(PackedCircuit circuit) {
		// TODO: what is a good channelWidth?
		this(FourLutSanitized.calculateSquareArchDimensions(circuit), 10);
	}
	
	public FourLutSanitized(int dimension, int channelWidth) {
		this(dimension, dimension, channelWidth);
	}
	
	public FourLutSanitized(int width, int height, int channelWidth)
	{
		super();
		this.width = width;
		this.height = height;
		this.n = 2;
		this.channelWidth = channelWidth;
		
		int x, y, n;
		
		siteArray = new Site[width+2][height+2][this.n];
		
		//Generate routing wires
		@SuppressWarnings("unchecked")
		Vector<RouteNode>[][] horizontalChannels = new Vector[width+2][height+2];
		for(x= 1; x<width+1; x++)
		{
			for(y= 0; y<height+1; y++)
			{
				horizontalChannels[x][y]=new Vector<RouteNode>();
				for(int i=0; i<channelWidth; i++)
				{
					RouteNode wire = new RouteNode("ChanX_"+x+"_"+y+"_"+i, 1, x, y, i, RouteNodeType.HCHAN,null);
					horizontalChannels[x][y].add(wire);
					routeNodeVector.add(wire);
				}
			}
		}
		@SuppressWarnings("unchecked")
		Vector<RouteNode>[][] verticalChannels = new Vector[width+2][height+2];
		for(x= 0; x<width+1; x++)
		{
			for(y= 1; y<height+1; y++)
			{
				verticalChannels[x][y]=new Vector<RouteNode>();
				for(int i=0; i<channelWidth; i++)
				{
					RouteNode wire = new RouteNode("ChanY_"+x+"_"+y+"_"+i, 1, x, y, i, RouteNodeType.VCHAN,null);
					verticalChannels[x][y].add(wire);
					routeNodeVector.add(wire);
				}
			}
		}
		
		for(x= 1; x<width+1; x++)
		{
			for(y= 0; y<height+1; y++)
			{
				if(x!=1)
					RouteNode.connect(horizontalChannels[x][y],horizontalChannels[x-1][y]);
				if(x!=width)
					RouteNode.connect(horizontalChannels[x][y],horizontalChannels[x+1][y]);
				if(y!=0)
				{
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x][y]);
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x-1][y]);
				}
				if(y!=height)
				{
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x][y+1]);
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x-1][y+1]);
				}
			}
		}
		for(x= 0; x<width+1; x++)
		{
			for(y= 1; y<height+1; y++)
			{
				if(y!=1)
					RouteNode.connect(verticalChannels[x][y],verticalChannels[x][y-1]);
				if(y!=height)
					RouteNode.connect(verticalChannels[x][y],verticalChannels[x][y+1]);
				if(x!=0)
				{
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x][y]);
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x][y-1]);
				}
				if(x!=width)
				{
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x+1][y]);
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x+1][y-1]);
				}
			}
		}
		
		//Generating the IO blocks
		for(y=1; y<height+1; y++)
		{
			putISite(0, y, 0, verticalChannels[0][y]);
			putISite(width+1, y, 0, verticalChannels[width][y]);
			
			putOSite(0, y, 1, verticalChannels[0][y]);
			putOSite(width+1, y, 1, verticalChannels[width][y]);
		}
		for(x=1; x<width+1; x++)
		{
			putISite(x, 0, 0, horizontalChannels[x][0]);
			putISite(x, height+1, 0, horizontalChannels[x][height]);
			
			putOSite(x, 0, 1, horizontalChannels[x][0]);
			putOSite(x, height+1, 1, horizontalChannels[x][height]);
		}
		
		//Generate CLBs
		for(x=1; x<=width; x++)
		{
			for(y=1; y<=height; y++)
			{
				putClbSite(x,y,0,horizontalChannels[x][y-1],horizontalChannels[x][y],verticalChannels[x][y],horizontalChannels[x][y-1],verticalChannels[x-1][y]);
			}
		}
	}
	
	public static int calculateSquareArchDimensions(PackedCircuit circuit)
	{
		int nbInputs = circuit.getInputs().values().size();
		int nbOutputs = circuit.getOutputs().values().size();
		int nbClbs = circuit.clbs.values().size();
		int maxIO;
		if(nbInputs > nbOutputs)
		{
			maxIO = nbInputs;
		}
		else
		{
			maxIO = nbOutputs;
		}
		int x1 = (maxIO + 3) / 4;
		int x2 = (int)Math.ceil(Math.sqrt(nbClbs * 1.20));
		int x;
		if(x1 > x2)
		{
			x = x1;
		}
		else
		{
			x = x2;
		}
		return x;
	}

	
	
	public int getChannelWidth()
	{
		return channelWidth;
	}
	
	
	
	public Site randomClbSite(int Rlim, Site pl1)
	{
		Site pl2;
		do
		{
			//-1 is nodig om de coordinaten in clbPlaatsArray te verkrijgen.	
			int x_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.x;	
			if(x_to<1)
				x_to+=width;
			if(x_to>= width+1)
				x_to-=width;
			int y_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.y;					
			if(y_to<1)
				y_to+=height;
			if(y_to>= height+1)
				y_to-=height;
			pl2=siteArray[x_to][y_to][0];
		}while(pl1==pl2);
		return pl2;
	}
	
	public Site randomHardBlockSite(int Rlim, HardBlockSite pl1) {
		return null;
	}
	
	public Site randomISite(int Rlim, Site pl1)
	{
		Site pl2 = null;
		int manhattanDistance = -1;
		do{
			Vector<Site> ISites = this.getSites(SiteType.I);
			pl2 = ISites.elementAt(rand.nextInt(ISites.size()));
			if(pl2==null)
				System.out.println("woops");
			manhattanDistance = Math.abs(pl1.x-pl2.x)+Math.abs(pl1.y-pl2.y);
		}while (pl1==pl2||manhattanDistance>Rlim);
		return pl2;
	}
	
	public Site randomOSite(int Rlim, Site pl1)
	{
		Site pl2 = null;
		int manhattanDistance = -1;
		do
		{
			Vector<Site> OSites = this.getSites(SiteType.O);
			pl2 = OSites.elementAt(rand.nextInt(OSites.size()));
			if(pl2==null)
				System.out.println("woops");
			manhattanDistance = Math.abs(pl1.x-pl2.x)+Math.abs(pl1.y-pl2.y);
		}while (pl1==pl2||manhattanDistance>Rlim);
		return pl2;
	}
	
	
	
	private void putISite(int x,int y, int n, Vector<RouteNode> channel)
	{
		ISite site = new ISite("Site_"+x+"_"+y+"_"+n, x, y,n);
		addSite(site, x, y, n);
		addRouteNodes(site);
		RouteNode.connect(channel, site.ipin);
	}
	
	private void putOSite(int x,int y, int n, Vector<RouteNode> channel)
	{
		OSite site = new OSite("Site_"+x+"_"+y+"_"+n, x, y, n);
		addSite(site, x, y, n);
		addRouteNodes(site);
		RouteNode.connect(site.opin, channel);
	}
	
	private void putClbSite(int x,int y, int n, Vector<RouteNode> opinChan, Vector<RouteNode> ipin0Chan, Vector<RouteNode> ipin1Chan, Vector<RouteNode> ipin2Chan, Vector<RouteNode> ipin3Chan)
	{
		ClbSite site = new ClbSite("Site_"+x+"_"+y+"_"+n, x,y, n);
		addSite(site, x, y, n);
		addRouteNodes(site);
		RouteNode.connect(site.opin, opinChan);
		RouteNode.connect(ipin0Chan, site.ipin.get(0));
		RouteNode.connect(ipin1Chan, site.ipin.get(1));
		RouteNode.connect(ipin2Chan, site.ipin.get(2));
		RouteNode.connect(ipin3Chan, site.ipin.get(3));
	}
	
	private void addRouteNodes(ClbSite site)
	{
		routeNodeVector.add(site.source);
		routeNodeVector.add(site.opin);
		routeNodeVector.add(site.sink);
		for(RouteNode ipin:site.ipin)
		{
			routeNodeVector.add(ipin);			
		}
	}

	private void addRouteNodes(ISite site)
	{
		routeNodeVector.add(site.sink);
		routeNodeVector.add(site.ipin);
	}
	
	private void addRouteNodes(OSite site)
	{
		routeNodeVector.add(site.source);
		routeNodeVector.add(site.opin);
	}

}
