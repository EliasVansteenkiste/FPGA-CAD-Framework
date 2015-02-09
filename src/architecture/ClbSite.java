package architecture;

import java.util.Vector;

public class ClbSite extends Site {

//	public Clb clb;
	
	public RouteNode opin;
	public Vector<RouteNode> ipin; 

	public ClbSite(String naam, int x, int y, int n) {
		super(x, y, n, SiteType.CLB, naam);
		source = new RouteNode(naam+"_source", 1, x, y, n, RouteNodeType.SOURCE,this);
		opin = new RouteNode(naam+"_opin", 1, x, y, n, RouteNodeType.OPIN,this);
		sink = new RouteNode(naam+"_sink", 4, x, y, n, RouteNodeType.SINK,this);
		ipin = new Vector<RouteNode>();
		for(int i=0;i<4;i++) {
			ipin.add(new RouteNode(naam+"_ipin_"+i, 1, x, y, n, RouteNodeType.IPIN,this));
		}
		
		RouteNode.connect(source, opin);
		RouteNode.connect(ipin, sink);
	}


}
