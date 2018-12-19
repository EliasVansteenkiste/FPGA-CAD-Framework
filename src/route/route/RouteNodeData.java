package route.route;

import route.circuit.pin.GlobalPin;
import route.circuit.resource.RouteNode;
import route.util.CountingSet;

public class RouteNodeData {
	public final int index;
	
	public float pres_cost;
	public float acc_cost;
	
	public float partial_path_cost;
	public float lower_bound_total_path_cost;
	public boolean touched;
	
	public RouteNode prev;
	
	public int occupation;
	
	private CountingSet<GlobalPin> sourcesSet;
	private CountingSet<RouteNode> parentsSet;
	
    public RouteNodeData(int index) {
    	this.index = index;
    	this.pres_cost = 1;
    	this.acc_cost = 1;
    	this.occupation = 0;
    	this.touched = false;

		this.sourcesSet = null;
		this.parentsSet = null;
		
		this.prev = null;
	}

	public boolean updateLowerBoundTotalPathCost(float new_lower_bound_total_path_cost) {
		if (new_lower_bound_total_path_cost < this.lower_bound_total_path_cost) {
			this.lower_bound_total_path_cost = new_lower_bound_total_path_cost;
			return true;
		}
		return false;
	}
	
	public void setLowerBoundTotalPathCost(float new_lower_bound_total_path_cost) {
		this.lower_bound_total_path_cost = new_lower_bound_total_path_cost;
		this.touched = true;
	}
	public void setPartialPathCost(float new_partial_path_cost) {
		this.partial_path_cost = new_partial_path_cost;
	}
	
	public float getLowerBoundTotalPathCost() {
		return this.lower_bound_total_path_cost;
	}
	public float getPartialPathCost() {
		return this.partial_path_cost;
	}

	public void addSource(GlobalPin source) {
		if(this.sourcesSet == null) {
			this.sourcesSet = new CountingSet<GlobalPin>();
		}
		this.sourcesSet.add(source);
	}
	
	public int numUniqueSources() {
		if(this.sourcesSet == null) {
			return 0;
		}
		return this.sourcesSet.uniqueSize();
	}
	
	public void removeSource(GlobalPin source) {
		this.sourcesSet.remove(source);
		if(this.sourcesSet.isEmpty()) {
			this.sourcesSet = null;
		}
	}

	public int countSourceUses(GlobalPin source) {
		if(this.sourcesSet == null) {
			return 0;
		}
		return this.sourcesSet.count(source);
	}
	
	public int numUniqueParents() {
		if(this.parentsSet == null) {
			return 0;
		}
		return this.parentsSet.uniqueSize();
	}
	
	public void addParent(RouteNode parent) {
		if(this.parentsSet == null) {
			this.parentsSet = new CountingSet<RouteNode>();
		}
		this.parentsSet.add(parent);
	}
	
	public void removeParent(RouteNode parent) {
		this.parentsSet.remove(parent);
		if(this.parentsSet.isEmpty()) {
			this.parentsSet = null;
		}
	}
	
	@Override
	public int hashCode() {
		return this.index;
	}
}
