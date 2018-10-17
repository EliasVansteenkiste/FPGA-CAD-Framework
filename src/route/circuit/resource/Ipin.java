package route.circuit.resource;

public class Ipin extends RouteNode {
	private final String portName;
	private final int portIndex;
	
	public Ipin(int index, int xlow, int xhigh, int ylow, int yhigh, int n, String portName, int portIndex, IndexedData indexedData) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.IPIN, 0, 0, indexedData);
		this.portName = portName;
		this.portIndex = portIndex;
	}
	
	public String getPortName() {
		return this.portName;
	}
	public int getPortIndex() {
		return this.portIndex;
	}
	
	public void setSinkName() {
		int numSinks = this.children.length;
		assert numSinks == 1;
		
		RouteNode child = this.children[0];
		assert child.type.equals(RouteNodeType.SINK);
		
		((Sink)child).setName(this.portName, this.portIndex);
	}
}
