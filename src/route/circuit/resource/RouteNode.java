package route.circuit.resource;

import route.route.RouteNodeData;

public abstract class RouteNode implements Comparable<RouteNode> {
	public final int index;//Unique index number
	
	public final short xlow, xhigh;
	public final short ylow, yhigh;
	public final float centerx, centery;
	public final short n;
	
	public float delay;
	public final float r;
	public final float c;
	public RouteSwitch drivingRouteSwitch;
	
	public final float base_cost;
	
	public final RouteNodeType type;
	public final short capacity;
	
	public final int numChildren;
	public final RouteNode[] children;
	public final RouteSwitch[] switches;

	public final IndexedData indexedData;
	public final RouteNodeData routeNodeData;
	
	public boolean target;
	
	public RouteNode(int index, int xlow, int xhigh, int ylow, int yhigh, int n, int capacity, RouteNodeType t, float r, float c, IndexedData indexedData, int numChildren) {
		this.index = index;
		
		this.xlow = (short) xlow;
		this.xhigh = (short) xhigh;
		this.ylow = (short) ylow;
		this.yhigh = (short) yhigh;
		
		this.centerx = 0.5f * (this.xlow + this.xhigh);
		this.centery = 0.5f * (this.ylow + this.yhigh);
		
		this.indexedData = indexedData;
		this.routeNodeData = new RouteNodeData();
		
		this.n = (short) n;
		
		this.type = t;
		this.capacity = (short) capacity;
		
		this.r = r;
		this.c = c;
		this.delay = -1;
		this.drivingRouteSwitch = null;
		
		this.base_cost = this.indexedData.base_cost;
		
		this.numChildren = numChildren;
		this.children = new RouteNode[this.numChildren];
		this.switches = new RouteSwitch[this.numChildren];

		this.target = false;
	}
	
	public void setChild(int index, RouteNode child) {
		this.children[index] = child;
	}
	public void setSwitchType(int index, RouteSwitch routeSwitch) {
		this.switches[index] = routeSwitch;
	}

	public boolean isWire() {
		return this.type == RouteNodeType.CHANX || this.type == RouteNodeType.CHANY;
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
		s.append(String.format("basecost = %3.1f", this.indexedData.base_cost));
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
	
	public float getDelay() {
		return this.delay;
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
	
	public void setDrivingRouteSwitch(RouteSwitch drivingRouteSwitch) {
		this.drivingRouteSwitch = drivingRouteSwitch;
		if(this.type == RouteNodeType.SOURCE || this.type == RouteNodeType.SINK) {
			this.delay = 0;
		} else {
			this.delay =  this.c * (this.drivingRouteSwitch.r + 0.5f * this.r) + this.drivingRouteSwitch.tdel;
		}
	}
	
	@Override
	public int hashCode() {
		return this.index;
	}
}
