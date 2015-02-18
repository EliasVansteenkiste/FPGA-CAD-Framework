package router;

import java.util.HashMap;
import java.util.PriorityQueue;

import architecture.Architecture;
import architecture.RouteNode;
import circuit.PackedCircuit;
import circuit.Net;
import circuit.Pin;




public class PathfinderRouter {
	public HashMap<RouteNode,RouteNodeData> routeNodeData;
	private PriorityQueue<QueueElement> queue;
	private double pres_fac;
	private int iteration;
	private boolean queueEmpty;
	
	Architecture a;
	PackedCircuit c;
	
	public PathfinderRouter(PackedCircuit c) {
		super();
		//initialize data
		this.c=c;
	}

    private void add(Net net) {
		for (RouteNode node:net.routeNodes) {
			RouteNodeData data=routeNodeData.get(node);
			data.occupation++;
			int occ = data.occupation;
			//Calculation of present cost
			int cap = node.capacity;
			if(occ < cap){
				data.pres_cost = 1.;
			}else{
				data.pres_cost = 1. + (occ + 1 - cap) * pres_fac;
			}			
		}
	}

	private void addNodeToQueue(RouteNode node, QueueElement prev, double cost) {
		
		if (cost < routeNodeData.get(node).path_cost) {
			routeNodeData.get(node).path_cost = cost;
			queue.add(new QueueElement(node,prev,cost));
		}
	}

	private void allocateRouteNodeData(Architecture a) {
		routeNodeData = new HashMap<RouteNode, RouteNodeData>();
		for (RouteNode node:a.routeNodeMap.values()) {
			routeNodeData.put(node, new RouteNodeData());			
		}
	}

	private void expandFirstNode(Net net) {
		
		QueueElement qe=queue.poll();
		RouteNode node=qe.node;
		
		for (RouteNode child:node.children) {
			
			double childCost = qe.cost + getCost(child, net);
			
			addNodeToQueue(child, qe, childCost);
			
		}
	}
	
	private double getCost(RouteNode node, Net net) {//
			if(net.routeNodes.contains(node)){	
				return 0;
			}else{
				RouteNodeData data = routeNodeData.get(node);
				return node.baseCost * data.acc_cost * data.pres_cost;
			}
		
	}
	
	private void resetPathCost() {
		for (RouteNodeData data:routeNodeData.values()) {
			data.path_cost=Double.MAX_VALUE;
		}
		
	}
	
	private void ripup(Net net) {
		for (RouteNode node:net.routeNodes) {
			RouteNodeData data=routeNodeData.get(node);
			data.occupation --;
			int occ = data.occupation;
			//Calculation of present cost
			int cap = node.capacity;
			if(occ < cap){
				data.pres_cost = 1.;
			}else{
				data.pres_cost = 1. + (occ + 1 - cap) * pres_fac;
			}   
		}
	}
		
	public int route(Architecture a, int maxNOiterations) {
		this.a=a;
		
		queueEmpty = false;
		allocateRouteNodeData(a);
		queue = new PriorityQueue<QueueElement>();
	    double initial_pres_fac = 0.5;
		double pres_fac_mult = 2;
		double acc_fac = 1;
		pres_fac = 0.5;	
		iteration=1;
		
		while (iteration <= maxNOiterations) {
			System.out.print(iteration+"..");
	        for(Net net:c.nets.values()){
	        	ripup(net);
				if(!route(net))return -2;
				add(net);
			}

	        //Check if the routing is realizable, if realizable return, the routing succeeded 
			if (routingIsFeasable()){
				c.netRouted = true;
				System.out.println("\nRouting succeeded in "+iteration+" trials.");
				System.out.println("Total wires used: "+c.totalWires());
				return iteration;
			}
			
			//Calculate and print out overuse
			int noUsed = 0;
			int noOverused = 0;
			int totalOveruse = 0;
			for (RouteNode node: a.routeNodeMap.values()) {
				RouteNodeData data=routeNodeData.get(node);
				if(data.occupation>0){
					noUsed++;
					if (node.capacity < data.occupation) {
						noOverused++;
						totalOveruse += (data.occupation-node.capacity);
						
					}
				}
			}
			System.out.println("Congestion:"+noOverused+" RNs of the "+noUsed+" routed RNs overused, total overuse = "+totalOveruse);
			
			//Updating the cost factors
			if (iteration == 1)
				pres_fac = initial_pres_fac;
			else
				pres_fac *= pres_fac_mult;
			
			updateCost(pres_fac, acc_fac);
			
			iteration++;
		}
		if (iteration==maxNOiterations+1){
			System.out.println("Routing failled after "+iteration+" trials!");
			for (RouteNode node: a.routeNodeMap.values()) {
				RouteNodeData data=routeNodeData.get(node);
				if (node.capacity < data.occupation) {
					System.out.println(node);
					System.out.println("basecost = "+node.baseCost+", capacity = "+node.capacity+", occupation = "+data.occupation+", type = "+node.type);
				}
			}
		}
		return -1;
	}

	private boolean route(Net net) {
		//Clear routing
		net.routeNodes.clear();
		for(Pin sinkPin:net.sinks){
			//Clear Queue
			queue.clear();
			//Add source to queue
			RouteNode source = net.source.owner.getSite().source;
			addNodeToQueue(source, null, getCost(source,net));
			//Set target flag sink
			RouteNode sink = sinkPin.owner.getSite().sink;
			sink.target=true;
			//Start Dijkstra
			while (!targetReached()) {
				expandFirstNode(net);
			}
			if(queueEmpty){
				System.out.println("\twhile routing net "+net);
			}
			//Reset target flag sink
			sink.target=false;
			//Save routing in connection class
			saveRouting(net);//
			//Reset path cost from Dijkstra Algorithm
			resetPathCost();
		}
		return true;
	}
		
	private boolean routingIsFeasable() {
		for (RouteNode node: a.routeNodeMap.values()) {
			RouteNodeData data=routeNodeData.get(node);
			if (node.capacity < data.occupation) {
				return false;
			}
		}
		return true;
	}

	private void saveRouting(Net net) {
		QueueElement qe=queue.peek();
		while (qe!=null) {
			RouteNode node = qe.node;
			net.routeNodes.add(node);
			qe=qe.prev;
		}
		
	}	
		
	private boolean targetReached() {
		if(queue.peek()==null){
			System.out.println("queue is leeg");
			queueEmpty = true;
			return false;
		}
		else return queue.peek().node.target;
	}
	
	private void updateCost(double pres_fac, double acc_fac){
		 for (RouteNode node : a.routeNodeMap.values()) {
			RouteNodeData data = routeNodeData.get(node);
			int occ = data.occupation;
			int cap = node.capacity;
			
			if (occ > cap) {
				data.acc_cost += (occ - cap) * acc_fac;
				data.pres_cost = 1.0 + (occ + 1 - cap) * pres_fac;
			} else if (occ == cap) {
				data.pres_cost = 1.0 + pres_fac;
			}
		}
	}

}


