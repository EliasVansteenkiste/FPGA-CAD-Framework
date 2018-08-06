package route.circuit.resource;

public class HChan extends RouteNode {
	public final double r;
	public final double c;
	
	public HChan(int index, int xlow, int xhigh, int ylow, int yhigh, int n, double r, double c, double baseCost) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.HCHAN, baseCost);
		this.r = r;
		this.c = c;
	}
}
