package route.circuit.resource;

public class HChan extends RouteNode {
	
	public HChan(int index, int xlow, int xhigh, int ylow, int yhigh, int n, float r, float c, IndexedData indexedData) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.HCHAN, r, c, indexedData);
	}
}
