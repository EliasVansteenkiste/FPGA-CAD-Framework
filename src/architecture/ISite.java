package architecture;


public class ISite extends Site {

	public RouteNode ipin;

	public ISite(String naam, int x, int y, int n) {
		super(x, y, n, SiteType.I, naam);
		
		sink = new RouteNode(naam+"_sink", 1, x, y, n, RouteNodeType.SINK,this);
		ipin = new RouteNode(naam+"_ipin", 1, x, y, n, RouteNodeType.IPIN,this);
		
		RouteNode.connect(ipin, sink);
		
	}

}
