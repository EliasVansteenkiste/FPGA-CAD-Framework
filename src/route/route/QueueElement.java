package route.route;

import route.circuit.resource.RouteNode;

public class QueueElement implements Comparable<QueueElement> {
	public final RouteNode node;
	public final RouteNodeData data;
	public final QueueElement prev;

	public QueueElement(RouteNode node, RouteNodeData data, QueueElement qe) {
		this.node = node;
		this.data = data;
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
		} else if (getLowerBoundTotalPathCost() == obj.getLowerBoundTotalPathCost()) {
			return node.compareTo(obj.node);
		} else {
			return 1;
		}
	}

	double getPartialPathCost() {
		return data.getPartialPathCost();
	}
	
	double getLowerBoundTotalPathCost() {
		return data.getLowerBoundTotalPathCost();
	}
}
