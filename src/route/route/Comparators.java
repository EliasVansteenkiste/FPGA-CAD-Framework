package route.route;

import java.util.Comparator;

import route.circuit.resource.RouteNode;

public class Comparators {
	public static Comparator<Net> FANOUT = new Comparator<Net>() {
        @Override
        public int compare(Net n1, Net n2) {
            return n2.fanout - n1.fanout;
        }
    };
    public static Comparator<RouteNode> PRIORITY_COMPARATOR = new Comparator<RouteNode>() {
        @Override
        public int compare(RouteNode node1, RouteNode node2) {
            if(node1.routeNodeData.lower_bound_total_path_cost < node2.routeNodeData.lower_bound_total_path_cost) {
            	return -1;
            } else {
            	return 1;
            }
        }
    };
}