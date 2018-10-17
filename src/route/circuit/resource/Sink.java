package route.circuit.resource;

public class Sink extends RouteNode {
	private String name;
	
	public Sink(int index, int xlow, int xhigh, int ylow, int yhigh, int n, int capacity, IndexedData indexedData) {
		super(index, xlow, xhigh, ylow, yhigh, n, capacity, RouteNodeType.SINK, 0, 0, indexedData);
		
		this.name = null;
	}
	
	public void setName(String portName, int portIndex){
		if(this.name == null) {
			this.name = portName + "[" + portIndex + "]";
		}else{
			this.name = portName;
		}
	}
	public String getName() {
		return this.name;
	}
}
