package architecture;


import java.util.Vector;

import circuit.PackedCircuit;
import circuit.Clb;
import circuit.Input;
import circuit.Output;

public class FourLutSanitized extends Architecture {

	public int width;
	public int height;
	public int channelWidth;
	public Site[][][] siteArray;
	public Vector<Site> Isites; 
	public Vector<Site> Osites;


	@SuppressWarnings("unchecked")
	public FourLutSanitized(int width, int height, int channelWidth) {
		super();
		this.width=width;
		this.height=height;
		this.channelWidth=channelWidth;
		
		int x, y, n;
		
		siteArray=new Site[width+2][height+2][2];
		
		Vector<RouteNode>[][] horizontalChannels;
		Vector<RouteNode>[][] verticalChannels;
		
		
		
		//Generate routing wires 
		horizontalChannels =new Vector[width+2][height+2];
		for (x= 1; x<width+1; x++) {
			for (y= 0; y<height+1; y++) {
				horizontalChannels[x][y]=new Vector<RouteNode>();
				for (int i=0; i<channelWidth; i++) {
					RouteNode wire = new RouteNode("ChanX_"+x+"_"+y+"_"+i, 1, x, y, i, RouteNodeType.HCHAN,null);
					horizontalChannels[x][y].add(wire);
					putRouteNode(wire);
				}
			}
		}
		verticalChannels =new Vector[width+2][height+2];
		for (x= 0; x<width+1; x++) {
			for (y= 1; y<height+1; y++) {
				verticalChannels[x][y]=new Vector<RouteNode>();
				for (int i=0; i<channelWidth; i++) {
					RouteNode wire = new RouteNode("ChanY_"+x+"_"+y+"_"+i, 1, x, y, i, RouteNodeType.VCHAN,null);
					verticalChannels[x][y].add(wire);
					putRouteNode(wire);
				}
			}
		}
		
		for (x= 1; x<width+1; x++) {
			for (y= 0; y<height+1; y++) {
				if (x!=1) RouteNode.connect(horizontalChannels[x][y],horizontalChannels[x-1][y]);
				if (x!=width)RouteNode.connect(horizontalChannels[x][y],horizontalChannels[x+1][y]);
				if (y!=0){
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x][y]);
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x-1][y]);
				}
				if (y!=height) {
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x][y+1]);
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x-1][y+1]);
				}
			}
		}
		
		for (x= 0; x<width+1; x++) {
			for (y= 1; y<height+1; y++) {
				if (y!=1) RouteNode.connect(verticalChannels[x][y],verticalChannels[x][y-1]);
				if (y!=height)RouteNode.connect(verticalChannels[x][y],verticalChannels[x][y+1]);
				if (x!=0){
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x][y]);
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x][y-1]);
				}
				if (x!=width) {
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x+1][y]);
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x+1][y-1]);
				}
			}
		}
		
		
		//Generating the IO blocks
		for (y=1; y<height+1; y++) {
			for (n=0; n<2; n++) {
				putIoSite(0, y, n, verticalChannels[0][y]);
				putIoSite(width+1, y, n, verticalChannels[width][y]);
			}
		}
		for (x=1; x<width+1; x++) {
			for (n=0; n<2; n++) {
				putIoSite(x, 0, n, horizontalChannels[x][0]);
				putIoSite(x, height+1, n, horizontalChannels[x][height]);
			}
		}
		
		//Generate CLBs
		for (x=1; x<=width; x++) {
			for (y=1; y<=height; y++) {
				putClbSite(x,y,0,horizontalChannels[x][y-1],horizontalChannels[x][y],verticalChannels[x][y],horizontalChannels[x][y-1],verticalChannels[x-1][y]);
			}
		}
		
		//Generate Isites set
		Isites = new Vector<Site>();
		for (y=1; y<height+1; y++) {
			Isites.add(siteArray[0][y][0]);
			Isites.add(siteArray[width+1][y][0]);
		}
		for (x=1; x<width+1; x++) {
			Isites.add(siteArray[x][0][0]);
			Isites.add(siteArray[x][height+1][0]);
		}
		
		//Generate Osites set
		Osites = new Vector<Site>();
		for (y=1; y<height+1; y++) {
			Osites.add(siteArray[0][y][1]);
			Osites.add(siteArray[width+1][y][1]);
		}
		for (x=1; x<width+1; x++) {
			Osites.add(siteArray[x][0][1]);
			Osites.add(siteArray[x][height+1][1]);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public FourLutSanitized(FourLutSanitized a, PackedCircuit c, int channelWidth) {
		super();
		this.width=a.width;
		this.height=a.height;
		this.channelWidth=channelWidth;
		
		int x, y, n;
		
		siteArray=new Site[width+2][height+2][2];
		
		Vector<RouteNode>[][] horizontalChannels;
		Vector<RouteNode>[][] verticalChannels;
		
		//Generate routing wires 
		horizontalChannels =new Vector[width+2][height+2];
		for (x= 1; x<width+1; x++) {
			for (y= 0; y<height+1; y++) {
				horizontalChannels[x][y]=new Vector<RouteNode>();
				for (int i=0; i<channelWidth; i++) {
					RouteNode wire = new RouteNode("ChanX_"+x+"_"+y+"_"+i, 1, x, y, i, RouteNodeType.HCHAN,null);
					horizontalChannels[x][y].add(wire);
					putRouteNode(wire);
				}
			}
		}
		verticalChannels =new Vector[width+2][height+2];
		for (x= 0; x<width+1; x++) {
			for (y= 1; y<height+1; y++) {
				verticalChannels[x][y]=new Vector<RouteNode>();
				for (int i=0; i<channelWidth; i++) {
					RouteNode wire = new RouteNode("ChanY_"+x+"_"+y+"_"+i, 1, x, y, i, RouteNodeType.VCHAN,null);
					verticalChannels[x][y].add(wire);
					putRouteNode(wire);
				}
			}
		}
		
		for (x= 1; x<width+1; x++) {
			for (y= 0; y<height+1; y++) {
				if (x!=1) RouteNode.connect(horizontalChannels[x][y],horizontalChannels[x-1][y]);
				if (x!=width)RouteNode.connect(horizontalChannels[x][y],horizontalChannels[x+1][y]);
				if (y!=0){
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x][y]);
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x-1][y]);
				}
				if (y!=height) {
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x][y+1]);
					RouteNode.connect(horizontalChannels[x][y],verticalChannels[x-1][y+1]);
				}
			}
		}
		
		for (x= 0; x<width+1; x++) {
			for (y= 1; y<height+1; y++) {
				if (y!=1) RouteNode.connect(verticalChannels[x][y],verticalChannels[x][y-1]);
				if (y!=height)RouteNode.connect(verticalChannels[x][y],verticalChannels[x][y+1]);
				if (x!=0){
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x][y]);
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x][y-1]);
				}
				if (x!=width) {
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x+1][y]);
					RouteNode.connect(verticalChannels[x][y],horizontalChannels[x+1][y-1]);
				}
			}
		}
		
		
		//Generating the IO blocks
		for (y=1; y<height+1; y++) {
			for (n=0; n<2; n++) {
				putIoSite(0, y, n, verticalChannels[0][y]);
				putIoSite(width+1, y, n, verticalChannels[width][y]);
			}
		}
		for (x=1; x<width+1; x++) {
			for (n=0; n<2; n++) {
				putIoSite(x, 0, n, horizontalChannels[x][0]);
				putIoSite(x, height+1, n, horizontalChannels[x][height]);
			}
		}
		
		//Generate CLBs
		for (x=1; x<=width; x++) {
			for (y=1; y<=height; y++) {
				putClbSite(x,y,0,horizontalChannels[x][y-1],horizontalChannels[x][y],verticalChannels[x][y],horizontalChannels[x][y-1],verticalChannels[x-1][y]);
			}
		}
		
		//
		for(Clb clb:c.clbs.values()){
			int xco = clb.getSite().x;
			int yco = clb.getSite().y;
			Site site = siteArray[xco][yco][0];
			clb.setSite(site);
			site.block = clb;
		}
		for(Input input:c.inputs.values()){
			int xco = input.getSite().x;
			int yco = input.getSite().y;
			Site site = siteArray[xco][yco][0];
			input.setSite(site);
			site.block = input;
		}
		for(Output output:c.outputs.values()){
			int xco = output.getSite().x;
			int yco = output.getSite().y;
			Site site = siteArray[xco][yco][1];
			output.setSite(site);
			site.block = output;
		}
		
		
	}



	private void putIoSite(int x,int y, int n, Vector<RouteNode> channel) {
		IoSite site = new IoSite("Site_"+x+"_"+y+"_"+n, x, y,n);
		addSite(site, x, y, n);
		addRouteNodes(site);
		RouteNode.connect(site.opin, channel);
		RouteNode.connect(channel, site.ipin);
	}
	
	private void putClbSite(int x,int y, int n, Vector<RouteNode> opinChan, Vector<RouteNode> ipin0Chan, Vector<RouteNode> ipin1Chan, Vector<RouteNode> ipin2Chan, Vector<RouteNode> ipin3Chan) {
		ClbSite site = new ClbSite("Site_"+x+"_"+y+"_"+n, x,y, n);
		addSite(site, x, y, n);
		addRouteNodes(site);
		RouteNode.connect(site.opin, opinChan);
		RouteNode.connect(ipin0Chan, site.ipin.get(0));
		RouteNode.connect(ipin1Chan, site.ipin.get(1));
		RouteNode.connect(ipin2Chan, site.ipin.get(2));
		RouteNode.connect(ipin3Chan, site.ipin.get(3));
	}

	public Site randomSite(int Rlim, Site pl1) {
		Site pl2;
		do {
		//-1 is nodig om de coordinaten in clbPlaatsArray te verkrijgen.	
		int x_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.x;	
		if (x_to<1) x_to+=width;
		if (x_to>= width+1) x_to-=width;
	
		int y_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.y;					
		if (y_to<1) y_to+=height;
		if (y_to>= height+1) y_to-=height;
		
		pl2=siteArray[x_to][y_to][0];
			
		} while (pl1==pl2);
		return pl2;
	}
	
//	//Er zit een fout in
//	public Site randomIOSiteOld(int Rlim, Site pl1) {
//		Site pl2 = null;
//		do {
//			//-1 is nodig om de coordinaten in clbPlaatsArray te verkrijgen.
//			if(pl1.y==0){
//				int x_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.x;	
//				if (x_to<1) x_to+=width;
//				if (x_to>= width+1) x_to-=width;
//				pl2=siteArray[x_to][0][0];
//			}else if(pl1.y==height+1){
//				int x_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.x;	
//				if (x_to<1) x_to+=width;
//				if (x_to>= width+1) x_to-=width;
//				pl2=siteArray[x_to][height+1][0];
//			}else if(pl1.x==0){
//				int y_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.y;					
//				if (y_to<1) y_to+=height;
//				if (y_to>= height+1) y_to-=height;
//				pl2=siteArray[0][y_to][0];
//			}else if(pl1.x==width+1){
//				int y_to=rand.nextInt(2*Rlim+1)-Rlim+pl1.y;					
//				if (y_to<1) y_to+=height;
//				if (y_to>= height+1) y_to-=height;
//				pl2=siteArray[width+1][y_to][0];
//			}else{
//				System.out.println("Warning searching for random IO site and pl1 is not an IO site!");
//			}	
//		} while (pl1==pl2);
//		return pl2;
//	}
	
	public Site randomISite(int Rlim, Site pl1) {
		Site pl2 = null;
		int manhattanDistance = -1;
		do {
			pl2 = Isites.elementAt(rand.nextInt(Isites.size()));
			if(pl2==null)System.out.println("woops");
			manhattanDistance = Math.abs(pl1.x-pl2.x)+Math.abs(pl1.y-pl2.y);
		} while (pl1==pl2||manhattanDistance>Rlim);
		return pl2;
	}
	
	public Site randomOSite(int Rlim, Site pl1) {
		Site pl2 = null;
		int manhattanDistance = -1;
		do {
			pl2 = Osites.elementAt(rand.nextInt(Isites.size()));
			if(pl2==null)System.out.println("woops");
			manhattanDistance = Math.abs(pl1.x-pl2.x)+Math.abs(pl1.y-pl2.y);
		} while (pl1==pl2||manhattanDistance>Rlim);
		return pl2;
	}

	private void addRouteNodes(ClbSite site) {
		routeNodeMap.put(site.source.name, site.source);
		routeNodeMap.put(site.opin.name, site.opin);
		routeNodeMap.put(site.sink.name, site.sink);
		for (RouteNode ipin:site.ipin) {
			routeNodeMap.put(ipin.name, ipin);			
		}

		
	}



	private void addRouteNodes(IoSite site) {
		routeNodeMap.put(site.source.name, site.source);
		routeNodeMap.put(site.opin.name, site.opin);
		routeNodeMap.put(site.sink.name, site.sink);
		routeNodeMap.put(site.ipin.name, site.ipin);
	}



	public Site getSite(int x, int y, int n) {
		return siteArray[x][y][n];
	}

	public void addSite(Site site, int x, int y, int n) {
		siteArray[x][y][n]= site;
		siteMap.put(site.naam,site);
	}

}
