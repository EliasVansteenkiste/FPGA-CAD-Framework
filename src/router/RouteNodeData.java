package router;

public class RouteNodeData {
	double base_cost;
	double adapt_base_cost;
	double pres_cost;
	double acc_cost;
	double path_cost;
	double adapt_path_cost;
	int occupation;
	double reuseFactor;
	
    public RouteNodeData() {

    	pres_cost = 1;
    	acc_cost = 1;
    	reuseFactor = 1;
    	path_cost = Double.MAX_VALUE;
    	adapt_path_cost = Double.MAX_VALUE;
    	occupation = 0;    	
	}
}
