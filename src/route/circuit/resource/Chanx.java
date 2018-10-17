package route.circuit.resource;

public class Chanx extends RouteNode {
	
	public Chanx(int index, int xlow, int xhigh, int ylow, int yhigh, int n, float r, float c, IndexedData indexedData) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.CHANX, r, c, indexedData);
	}
}
