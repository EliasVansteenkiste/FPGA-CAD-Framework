package route.route;

import route.circuit.pin.Pin;
import route.util.CountingSet;

public class RouteNodeData {
	//Global infomation
	public double pres_cost;
	public double acc_cost;
	private double partial_path_cost;
	private double lower_bound_total_path_cost;
	public int occupation;
	
	private CountingSet<Pin> sourcesSet;
	
    public RouteNodeData(int occupation, CountingSet sourcesSet) {
    	this.pres_cost = 1;
    	this.acc_cost = 1;
    	this.occupation = occupation;
    	this.resetPathCosts();

		this.sourcesSet = new CountingSet<Pin>();//TODO COPY THE SOURCES
	}
    
	public void resetPathCosts() {
		this.partial_path_cost = Double.MAX_VALUE;
		this.lower_bound_total_path_cost = Double.MAX_VALUE;
	}
	
	public boolean pathCostsSet() {
		return partial_path_cost != Double.MAX_VALUE || this.lower_bound_total_path_cost != Double.MAX_VALUE;
	}

	public boolean updateLowerBoundTotalPathCost(double new_lower_bound_total_path_cost) {
		if (new_lower_bound_total_path_cost < this.lower_bound_total_path_cost) {
			this.lower_bound_total_path_cost = new_lower_bound_total_path_cost;
			return true;
		}
		return false;
	}
	public boolean updatePartialPathCost(double new_partial_path_cost) {
		if (new_partial_path_cost < this.partial_path_cost) {
			this.partial_path_cost = new_partial_path_cost;
			return true;
		}
		return false;
	}
	
	public double getLowerBoundTotalPathCost() {
		return this.lower_bound_total_path_cost;
	}
	public double getPartialPathCost() {
		return this.partial_path_cost;
	}

	public synchronized void addSource(Pin source) {//TODO Is synchronized required?
		if(this.sourcesSet == null) {
			this.sourcesSet = new CountingSet<Pin>();
		}
		this.sourcesSet.add(source);
	}
	
	public int numUniqueSources() {
		if(this.sourcesSet == null) {
			return 0;
		}
		return this.sourcesSet.uniqueSize();
	}
	
	public void removeSource(Pin source) {
		this.sourcesSet.remove(source);
		if(this.sourcesSet.isEmpty()) {
			this.sourcesSet = null;
		}
	}

	public int countSourceUses(Pin source) {
		if(this.sourcesSet == null) {
			return 0;
		}
		return this.sourcesSet.count(source);
	}
	
	public void updatePresentCongestionPenalty(double pres_fac, int cap) {
		int occ = this.numUniqueSources();
		
		if (occ < cap) {
			this.pres_cost = 1.0;
		} else {
			this.pres_cost = 1.0 + (occ + 1 - cap) * pres_fac;
		}
		
		this.occupation = occ;
	}
}
