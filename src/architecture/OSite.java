package architecture;

import architecture.old.RouteNode;
import architecture.old.RouteNodeType;

public class OSite extends Site {

	public RouteNode opin;

	public OSite(String naam, int x, int y, int n) {
		super(x, y, n, SiteType.O, naam);
		
		source = new RouteNode(naam+"_source", 1, x, y, n, RouteNodeType.SOURCE,this);
		opin = new RouteNode(naam+"_opin", 1, x, y, n, RouteNodeType.OPIN,this);
		
		RouteNode.connect(source, opin);
		
	}

}