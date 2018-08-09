package route.circuit.resource;

import route.circuit.pin.Pin;
import route.route.RouteNodeData;
import route.util.CountingSet;

public abstract class RouteNode implements Comparable<RouteNode> {
	public final int index;//Unique index number
	
	public final short xlow, xhigh;
	public final short ylow, yhigh;
	public final short n;
	
	public final RouteNodeType type;
	public final short capacity;
	public final double baseCost;
	
	public RouteNode[] children;
	public short[] switchType;

	public CountingSet<Pin> sourcesSet;
	public int occupation;
	
	//Route Node data for each leaf node
	public RouteNodeData[] routeNodeData;
	
	public boolean[] target;
	
	public RouteNode(int index, int xlow, int xhigh, int ylow, int yhigh, int n, int capacity, RouteNodeType t, double baseCost) {
		this.index = index;
		
		this.xlow = (short) xlow;
		this.xhigh = (short) xhigh;
		this.ylow = (short) ylow;
		this.yhigh = (short) yhigh;
		
		this.n = (short) n;
		
		this.type = t;
		this.capacity = (short) capacity;
		this.baseCost = this.calculateBaseCost();
		//this.baseCost = baseCost;
		
		this.sourcesSet = null;
		this.occupation = 0;
		
		this.children = null;
		this.switchType = null;
	}
	
	public void addSource(Pin source) {
		if(this.sourcesSet == null) {
			this.sourcesSet = new CountingSet<Pin>();
		}
		this.sourcesSet.add(source);
		
		for(RouteNodeData data: routeNodeData) {
			data.addSource(source);
		}
		
		this.occupation = this.sourcesSet.uniqueSize();
	}
	
	public void setNumParallelThreads(int numParallelThreads) {
		this.routeNodeData = new RouteNodeData[numParallelThreads];
		this.target = new boolean[numParallelThreads];
		
		for(int i = 0; i < numParallelThreads; i++) {
			this.routeNodeData[i] = new RouteNodeData();//TODO Not all leaf nodes will use this RouteNodeData
			this.target[i] = false;
		}
	}
	
	public void resetDataInNode(int thread) {
		this.target[thread] = false;
	}
	public void reset(int thread) {
		this.routeNodeData[thread].reset();
	}

	private double calculateBaseCost() {
		switch (this.type) {
			case SOURCE:
			case OPIN:
			case HCHAN:
			case VCHAN:
				return 1;
			case SINK:
				return 0;
			case IPIN:
				return 0.95;
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
	
	public void setSwitchType(short[] switchType) {
		this.switchType = switchType;
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
	public int compareTo(RouteNode o) {//TODO
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
		s.append(String.format("occupation = %2d ", this.occupation));
		s.append(", ");
		s.append(String.format("type = %s", this.type));
		
		return s.toString();
	}
	
	public boolean overUsed() {
		return this.capacity < this.occupation;
	}
	public boolean overUsed(int thread) {
		return this.capacity < this.routeNodeData[thread].occupation;
	}
	public boolean used() {
		return this.occupation > 0;
	}
	public boolean used(int thread) {
		return this.routeNodeData[thread].occupation > 0;
	}
	
	public void updatePresentCongestionPenalty(double pres_fac, int thread) {
		RouteNodeData data = this.routeNodeData[thread];
		
		int occ = data.numUniqueSources();
		int cap = this.capacity;
		
		if (occ < cap) {
			data.pres_cost = 1.0;
		} else {
			data.pres_cost = 1.0 + (occ + 1 - cap) * pres_fac;
		}
		
		data.occupation = occ;
	}
}
