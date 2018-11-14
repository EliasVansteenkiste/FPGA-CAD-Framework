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
	
	public RouteNode prev;
	
	public int occupation;
	
	private CountingSet<GlobalPin> sourcesSet;
	
    public RouteNodeData(int index) {
    	this.index = index;
    	this.pres_cost = 1;
    	this.acc_cost = 1;
    	this.occupation = 0;
    	this.resetPathCosts();

		this.sourcesSet = null;
		
		this.prev = null;
	}
    
	public void resetPathCosts() {
		this.partial_path_cost = Float.MAX_VALUE;
		this.lower_bound_total_path_cost = Float.MAX_VALUE;
		
		this.prev = null;
	}
	
	public boolean pathCostsSet() {
		return partial_path_cost != Float.MAX_VALUE || this.lower_bound_total_path_cost != Float.MAX_VALUE;
	}

	public boolean updateLowerBoundTotalPathCost(float new_lower_bound_total_path_cost) {
		if (new_lower_bound_total_path_cost < this.lower_bound_total_path_cost) {
			this.lower_bound_total_path_cost = new_lower_bound_total_path_cost;
			return true;
		}
		return false;
	}
	public boolean updatePartialPathCost(float new_partial_path_cost) {
		if (new_partial_path_cost < this.partial_path_cost) {
			this.partial_path_cost = new_partial_path_cost;
			return true;
		}
		return false;
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
	
	@Override
	public int hashCode() {
		return this.index;
	}
}
