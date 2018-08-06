package route.circuit.resource;

public class Source extends RouteNode {
	private String name;
	
	public Source(int index, int xlow, int xhigh, int ylow, int yhigh, int n, int capacity, double baseCost) {
		super(index, xlow, xhigh, ylow, yhigh, n, capacity, RouteNodeType.SOURCE, baseCost);
		
		this.name = null;
	}
	
	public void setName() {
		int numChildren = this.children.length;
		if(numChildren == 0){
			System.err.println("Problem in source children\n\t=> " + this.index);
		} else if(numChildren == 1) {
			Opin outputPin = (Opin) this.children[0];
			this.name = outputPin.getPortName() + "[" + outputPin.getPortIndex() + "]";
		} else {
			Opin outputPin = (Opin) this.children[0];
			this.name = outputPin.getPortName();
		}
	}
	public String getName() {
		return this.name;
	}
}
