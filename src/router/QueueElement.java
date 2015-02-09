package router;

import architecture.RouteNode;

public class QueueElement implements Comparable {
	RouteNode node;
	QueueElement prev;
	double cost;

	public QueueElement(RouteNode node, QueueElement qe, double cost) {
		this.node=node;
		this.prev=qe;
		this.cost=cost;
	}

	@Override
	public String toString() {
		return node.name+"@"+cost;
	}
	
	public int compareTo(Object obj) {
		QueueElement qe = (QueueElement) obj;
		if (cost < qe.cost) return -1;
		else if (cost == qe.cost) {
			return node.name.compareTo(qe.node.name);
		}
		else return 1;
	}
	
}
