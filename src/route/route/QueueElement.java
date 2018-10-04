package route.route;

import route.circuit.resource.RouteNode;

public class QueueElement implements Comparable<QueueElement> {
	public final RouteNode node;
	public final QueueElement prev;
	public final double lowerBoundTotalPathCost;
	public final double partialPathCost;

	public QueueElement(RouteNode node, QueueElement qe) {
		this.node = node;
		this.prev = qe;
		this.lowerBoundTotalPathCost = node.routeNodeData.getLowerBoundTotalPathCost();
		this.partialPathCost = node.routeNodeData.getPartialPathCost();
	}

	@Override
	public String toString() {
		return this.node.index + "@" + this.partialPathCost;
	}
	
	@Override
	public int hashCode() {
		return (int)this.partialPathCost ^ this.node.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		QueueElement rn = (QueueElement) o;
		return this.partialPathCost == rn.partialPathCost && this.node == rn.node;
	}
	
	@Override
	public int compareTo(QueueElement obj) {
		if(this.lowerBoundTotalPathCost < obj.lowerBoundTotalPathCost) {
			return -1;
		} else {
			return 1;
		}
	}
}
