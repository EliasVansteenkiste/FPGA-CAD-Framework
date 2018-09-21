package route.route;

import route.circuit.resource.RouteNode;

public class QueueElement implements Comparable<QueueElement> {
	public final RouteNode node;
	public final QueueElement prev;

	public QueueElement(RouteNode node, QueueElement qe) {
		this.node = node;
		this.prev = qe;
	}

	@Override
	public String toString() {
		return this.node.index + "@" + getPartialPathCost();
	}
	
	@Override
	public int hashCode() {
		return (int)this.getPartialPathCost() ^ this.node.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		QueueElement rn = (QueueElement) o;
		return this.getPartialPathCost() == rn.getPartialPathCost() && this.node == rn.node;
	}
	
	@Override
	public int compareTo(QueueElement obj) {
		if (getLowerBoundTotalPathCost() < obj.getLowerBoundTotalPathCost()) {
			return -1;
		} else {
			return 1;
		}
	}

	double getPartialPathCost() {
		return node.routeNodeData.getPartialPathCost();
	}
	
	double getLowerBoundTotalPathCost() {
		return node.routeNodeData.getLowerBoundTotalPathCost();
	}
}
