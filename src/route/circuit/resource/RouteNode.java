package route.circuit.resource;

import route.route.RouteNodeData;

public abstract class RouteNode implements Comparable<RouteNode> {
	public final int index;//Unique index number
	
	public final short xlow, xhigh;
	public final short ylow, yhigh;
	public final short n;
	
	public final float r;
	public final float c;
	
	public final RouteNodeType type;
	public final short capacity;
	public final float baseCost;
	
	public RouteNode[] children;
	public SwitchType[] switches;

	public final RouteNodeData routeNodeData;
	public final IndexedData indexedData;
	
	public boolean target;
	
	public RouteNode(int index, int xlow, int xhigh, int ylow, int yhigh, int n, int capacity, RouteNodeType t, float r, float c, IndexedData indexedData) {
		this.index = index;
		
		this.xlow = (short) xlow;
		this.xhigh = (short) xhigh;
		this.ylow = (short) ylow;
		this.yhigh = (short) yhigh;
		
		this.n = (short) n;
		
		this.type = t;
		this.capacity = (short) capacity;
		this.baseCost = this.calculateBaseCost();
		
		this.r = r;
		this.c = c;
		
		this.children = new RouteNode[0];
		this.switches = new SwitchType[0];
		
		this.indexedData = indexedData;
		this.routeNodeData = new RouteNodeData();

		this.target = false;
	}

	private float calculateBaseCost() {
		switch (this.type) {
			case SOURCE:
				return 1;
			case OPIN:
				return 4;
			case HCHAN:
			case VCHAN:
				return this.wireLength();
			case SINK:
				return 0;
			case IPIN:
				return 0.95f;
			default:
				throw new RuntimeException();
		}
	}
	
	public void setChildren(RouteNode[] children) {
		this.children = children;
	}
	public int numChildren() {
		return this.children.length;
	}
	
	public void setSwitchType(SwitchType[] switches) {
		this.switches = switches;
	}

	public boolean isWire() {
		return this.type == RouteNodeType.HCHAN || this.type == RouteNodeType.VCHAN;
	}
	public int wireLength() {
		int length = this.xhigh - this.xlow + this.yhigh - this.ylow + 1;
		
		if(length <= 0) System.err.println("The length of wire with type " + this.type + " is equal to " + length);
		
		return length;
	}
	
	@Override
	public int compareTo(RouteNode o) {
		int r = this.type.compareTo(o.type);
		if (this == o)
			return 0;
		else if (r < 0)
			return -1;
		else if (r > 0)
			return 1;
		else if(this.xlow < o.xlow)
			return -1;
		else if (this.xhigh > o.xhigh)
			return 1;
		else if (this.ylow < o.ylow)
			return -1;
		else if (this.yhigh > o.yhigh)
			return 1;
		else if (this.index < o.index)
			return -1;
		else if (this.index > o.index)
			return 1;
		else 
			return Long.valueOf(this.hashCode()).compareTo(Long.valueOf(o.hashCode()));
	}
	
	@Override
	public String toString() {
		
		String index = "" + this.index;
		while(index.length() < 10) index = "0" + index;
		
		String coordinate = "";
		if(this.xlow == this.xhigh && this.ylow == this.yhigh) {
			coordinate = "(" + this.xlow + "," + this.ylow + ")";
		} else {
			coordinate = "(" + this.xlow + "," + this.ylow + ") to (" + this.xhigh + "," + this.yhigh + ")";
		}
		
		StringBuilder s = new StringBuilder();
		s.append("RouteNode " + index + " ");
		s.append(String.format("%-11s", coordinate));
		s.append(String.format("ptc_num = %3d", this.n));
		s.append(", ");
		s.append(String.format("basecost = %3.1f", this.baseCost));
		s.append(", ");
		s.append(String.format("capacity = %2d", this.capacity));
		s.append(", ");
		s.append(String.format("occupation = %2d ", this.routeNodeData.occupation));
		s.append(", ");
		s.append(String.format("type = %s", this.type));
		
		return s.toString();
	}
	
	public boolean overUsed() {
		return this.capacity < this.routeNodeData.occupation;
	}
	public boolean used() {
		return this.routeNodeData.occupation > 0;
	}
	
	public void updatePresentCongestionPenalty(float pres_fac) {
		RouteNodeData data = this.routeNodeData;
		
		int occ = data.numUniqueSources();
		int cap = this.capacity;
		
		if (occ < cap) {
			data.pres_cost = 1;
		} else {
			data.pres_cost = 1 + (occ - cap + 1) * pres_fac;
		}
		
		data.occupation = occ;
	}
	
	@Override
	public int hashCode() {
		return this.index;
	}
}
