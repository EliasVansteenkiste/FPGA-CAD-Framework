package route.circuit.resource;

public class Opin extends RouteNode {
	private final String portName;
	private final int portIndex;
	
	private boolean used;
	
	public Opin(int index, int xlow, int xhigh, int ylow, int yhigh, int n, String portName, int portIndex, IndexedData indexedData, int numChildren) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.OPIN, 0, 0, indexedData, numChildren);
		
		this.portName = portName;
		this.portIndex = portIndex;
		
		this.used = false;
	}
	
	public String getPortName() {
		return this.portName;
	}
	public int getPortIndex() {
		return this.portIndex;
	}
	
	public void use() {
		this.used = true;
	}
	public boolean used() {
		return this.used;
	}
}
