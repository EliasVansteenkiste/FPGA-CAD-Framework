package route.route;

import route.circuit.resource.RouteNode;

public class QueueElement {
	final RouteNode node;
	final float cost;
	
	public QueueElement(RouteNode node, float cost) {
		this.node = node;
		this.cost = cost;
	}
}
