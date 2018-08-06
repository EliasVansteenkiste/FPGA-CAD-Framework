package route.circuit.resource;

public class Opin extends RouteNode {
	private final String portName;
	private final int portIndex;
	
	public Opin(int index, int xlow, int xhigh, int ylow, int yhigh, int n, String portName, int portIndex, double baseCost) {
		super(index, xlow, xhigh, ylow, yhigh, n, 1, RouteNodeType.OPIN, baseCost);
		
		this.portName = portName;
		this.portIndex = portIndex;
	}
	
	public String getPortName() {
		return this.portName;
	}
	public int getPortIndex() {
		return this.portIndex;
	}
}
