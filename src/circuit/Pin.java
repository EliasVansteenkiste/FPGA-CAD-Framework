package circuit;
import architecture.RouteNode;


public class Pin {
	public String name;
	public PinType type;
	public Block owner;
	public Block routingBlock;
	public Connection con;

	public RouteNode routeNode;
	
	public Pin(String name, PinType type) {
		super();
		this.name = name;
		this.type = type;
	}
	
	public Pin(String name, PinType type, Block owner) {
		super();
		this.name = name;
		this.type = type;
		this.owner = owner;
	}
	
	public RouteNode getRouteNode() {
		return routeNode;
	}

	@Override
	public String toString() {
		return name;
	}
	
	public int compareTo(Pin otherPin){
		return this.name.compareTo(otherPin.name);
	}

	@Override
	public boolean equals(Object o){
		if (o == null) return false;
	    if (!(o instanceof Pin)) return false;
	    Pin p = (Pin) o;
		if(p.name.compareTo(this.name)==0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}
}
