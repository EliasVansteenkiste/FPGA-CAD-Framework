package route.circuit.resource;

public class VChan extends RouteNode {
	public final double r;
	public final double c;
	
	public VChan(int index, int xlow, int xhigh, int ylow, int yhigh, int n, double r, double c, double baseCost) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.VCHAN, baseCost);
		this.r = r;
		this.c = c;
	}
}
