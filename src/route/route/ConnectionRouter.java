package route.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import route.circuit.Circuit;
import route.circuit.resource.ResourceGraph;
import route.circuit.resource.RouteNode;
import route.hierarchy.HierarchyNode;
import route.hierarchy.LeafNode;

public class ConnectionRouter {
	final ResourceGraph rrg;
	final Circuit circuit;
	
	private double pres_fac;
	private double alpha;
	private final PriorityQueue<QueueElement> queue;
	private final Collection<RouteNodeData> nodesTouched;
	
	private final int numThreads = 30;
	
	public ConnectionRouter(ResourceGraph rrg, Circuit circuit) {
		this.rrg = rrg;
		this.circuit = circuit;
		
		this.alpha = 1;
		this.nodesTouched = new ArrayList<RouteNodeData>();
		this.queue = new PriorityQueue<QueueElement>();
	}
    
    public int route(HierarchyNode rootNode) {
    	System.out.println("Num nets: " + this.circuit.getNets().size());
		System.out.println("Num cons: " + this.circuit.getConnections().size());
		System.out.println();
		
		boolean parallelRouting = true;
		
		if(parallelRouting) {
			Map<LeafNode, Integer> runtimeMap = new HashMap<>();
			
			Set<LeafNode> leafNodes = new HashSet<>();
			List<HierarchyNode> work = new LinkedList<>();
			work.add(rootNode);
			while(!work.isEmpty()) {
				HierarchyNode node = work.remove(0);
				if(node.isLeafNode()) {
					leafNodes.add((LeafNode)node);
				} else {
					for(HierarchyNode child : node.getChildren()) {
						work.add(child);
					}
				}
			}
			
			int maxTimeMilliseconds = 0;
		
			for(LeafNode leafNode : leafNodes) {
				System.out.printf("---------------------------------------\n");
				System.out.println("Leaf node " + (leafNode.getIndex()) + " (" + leafNode.connections.size() + ") of " + leafNodes.size() + " Cost: " + leafNode.cost());
				int timeMilliseconds = this.doRouting(leafNode.connections, 10, false);
				System.out.printf("---------------------------------------\n");
				if(timeMilliseconds > maxTimeMilliseconds) {
					maxTimeMilliseconds = timeMilliseconds;
				}
				System.out.println("RunTime: " + timeMilliseconds + " ms");
				System.out.printf("---------------------------------------\n\n\n");
				
				runtimeMap.put(leafNode, timeMilliseconds);
			}
	
			System.out.println("Longest cluster took " + maxTimeMilliseconds + "ms");
			System.out.println();
			
			System.out.println("Route remaining congested connections");
			this.doRouting(this.circuit.getConnections(), 100, true);
			System.out.println();
			
			boolean printRuntimeMap = true;
			if(printRuntimeMap) {
				System.out.println("RuntimeMap:");
				System.out.println("Connections\tCost\tRuntime[ms]");
				for(LeafNode leafNode : runtimeMap.keySet()) {
					System.out.println(leafNode.connections.size() + "\t" + leafNode.cost() + "\t" + runtimeMap.get(leafNode));
				}
				System.out.println();
			}
		} else {
			this.doRouting(this.circuit.getConnections(), 100, false);
		}
		
		return -1;
	}
    private int doRouting(Set<Connection> connections, int nrOfTrials, boolean onlyCongested) {
    	long start = System.nanoTime();
    	
    	this.nodesTouched.clear();
    	this.queue.clear();
		
	    double initial_pres_fac = 0.5;
		double pres_fac_mult = 2;
		double acc_fac = 1;
		this.pres_fac = initial_pres_fac;
		int itry = 1;
		
		Map<Connection, Integer> mapOfConnections = new HashMap<>();
		for(Connection con : connections) {
			mapOfConnections.put(con, con.boundingBox);
		}
		
		BBComparator bvc =  new BBComparator(mapOfConnections);
        Map<Connection, Integer> sortedMapOfConnections = new TreeMap<>(bvc);
        sortedMapOfConnections.putAll(mapOfConnections);
        
        int numRouteNodes = this.rrg.getRouteNodes().size();
        
        /******************* Initialization *****************/
		for(RouteNode routeNode : this.rrg.getRouteNodes()) {
			routeNode.reset();
		}
		/****************************************************/
		
        System.out.printf("---------  ---------  -----------------\n");
        System.out.printf("%9s  %9s  %17s\n", "Iteration", "Time (ms)", "Overused RR Nodes");
        System.out.printf("---------  ---------  -----------------\n");
        
        while (itry <= nrOfTrials) {
			long iterationStart = System.nanoTime();
        	
			for(Connection con : sortedMapOfConnections.keySet()){
				if((itry == 1 && !onlyCongested) || con.congested()) {
					this.ripup(con);
					this.route(con);
					this.add(con);			
				}
			}
			
			//Runtime
			long iterationEnd = System.nanoTime();
			int rt = (int) Math.round((iterationEnd-iterationStart)*Math.pow(10, -6));
			
			//Overuse
			Set<RouteNode> overused = new HashSet<>();
			for (Connection conn: connections) {
				for (RouteNode node: conn.routeNodes) {
					if (node.overUsed()) {
						overused.add(node);
					}
				}
			}

			double overUsePercentage = 100.0 * (double)overused.size() / numRouteNodes;

			System.out.printf("%9d  %9d  %8d   %5.2f%%\n", itry, rt, overused.size(), overUsePercentage);
			
			//Check if the routing is realizable, if realizable return, the routing succeeded 
			if (routingIsFeasible(connections)){
				this.circuit.setConRouted(true);
		        
				long end = System.nanoTime();
				int timeMilliSeconds = (int)Math.round((end-start) * Math.pow(10, -6));
				return timeMilliSeconds;
			}
			
			//Updating the cost factors
			if (itry == 1) {
				this.pres_fac = initial_pres_fac;
			} else {
				this.pres_fac *= pres_fac_mult;
			}
			this.updateCost(this.pres_fac, acc_fac);
			
			itry++;
		}
        
		if (itry == nrOfTrials + 1) {
			System.out.println("Routing failled after "+itry+" trials!");
			
			int maxNameLength = 0;
			
			Set<RouteNode> overused = new HashSet<>();
			for (Connection conn: connections) {
				for (RouteNode node: conn.routeNodes) {
					if (node.overUsed()) {
						overused.add(node);
					}
				}
			}
			for (RouteNode node: overused) {
				if (node.overUsed()) {
					if(node.toString().length() > maxNameLength) {
						maxNameLength = node.toString().length();
					}
				}
			}
			
			for (RouteNode node: overused) {
				if (node.overUsed()) {
					System.out.println(node.toString());
				}
			}
			System.out.println();
		}
		
		long end = System.nanoTime();
		int timeMilliSeconds = (int)Math.round((end-start) * Math.pow(10, -6));
		return timeMilliSeconds;
    }

	private boolean routingIsFeasible(Set<Connection> connections) {
		for (Connection con : connections) {
			if(con.congested()) {
				return false;
			}
		}
		return true;
	}

	private void ripup(Connection con) {
		for (RouteNode node : con.routeNodes) {
			RouteNodeData data = node.routeNodeData;
			
			data.removeSource(con.source);
			
			// Calculation of present congestion penalty
			node.updatePresentCongestionPenalty(this.pres_fac);
		}
	}
	private void add(Connection con) {
		for (RouteNode node : con.routeNodes) {
			RouteNodeData data = node.routeNodeData;

			data.addSource(con.source);

			// Calculation of present congestion penalty
			node.updatePresentCongestionPenalty(this.pres_fac);
		}
	}
	private boolean route(Connection con) {
		//System.out.println("Route connection");
		// Clear Routing
		con.resetConnection();
		
		// Clear Queue
		this.queue.clear();
		
		// Set target flag sink
		RouteNode sink = con.sinkRouteNode;
		sink.target = true;
		
		// Add source to queue
		RouteNode source = con.sourceRouteNode;
		double source_cost = getRouteNodeCost(source, con);
		addNodeToQueue(source, null, source_cost, getLowerBoundTotalPathCost(source, con, source_cost));
		
		// Start Dijkstra / directed search
		while (!targetReached()) {//TODO CHECK
			expandFirstNode(con);
		}
		
		// Reset target flag sink
		sink.target = false;
		
		// Save routing in connection class
		saveRouting(con);
		
		// Reset path cost from Dijkstra Algorithm
		resetPathCost();

		return true;
	}

		
	private void saveRouting(Connection con) {
		QueueElement qe = this.queue.peek();
		while (qe != null) {
			con.addRouteNode(qe.node);
			qe = qe.prev;
		}
	}

	private boolean targetReached() {
		if(this.queue.peek()==null){
			System.out.println("queue is leeg");			
			return false;
		} else {
			return queue.peek().node.target;
		}
	}
	
	private void resetPathCost() {
		for (RouteNodeData node : this.nodesTouched) {
			node.resetPathCosts();
		}
		this.nodesTouched.clear();
	}

	private void expandFirstNode(Connection con) {
		if (this.queue.isEmpty()) {
			throw new RuntimeException("Queue is empty: target unreachable?");
		}
		QueueElement qe = this.queue.poll();
		RouteNode node = qe.node;
		//System.out.println(node.type);
		for (RouteNode child : node.children) {			
			if(con.isInBoundingBoxLimit(child)) {
				double childCost = node.routeNodeData.getPartialPathCost() + getRouteNodeCost(child, con);
				double childCostEstimate = getLowerBoundTotalPathCost(child, con, childCost);
				this.addNodeToQueue(child, qe, childCost, childCostEstimate);
			}
		}
	}
	private void addNodeToQueue(RouteNode node, QueueElement prev, double new_partial_path_cost, double new_lower_bound_total_path_cost) {
		RouteNodeData nodeData = node.routeNodeData;
		if(!nodeData.pathCostsSet()) this.nodesTouched.add(nodeData);
		nodeData.updatePartialPathCost(new_partial_path_cost);
		if (nodeData.updateLowerBoundTotalPathCost(new_lower_bound_total_path_cost)) {	//queue is sorted by lower bound total cost
			this.queue.add(new QueueElement(node, prev));
		}
	}

	/**
	 * This is just an estimate and not an absolute lower bound.
	 * The routing algorithm is therefore not A* and optimal.
	 * It's directed search and heuristic.
	 */
	private double getLowerBoundTotalPathCost(RouteNode node, Connection con, double partial_path_cost) {
		if(this.alpha == 0) return partial_path_cost;
		RouteNode target = con.sinkRouteNode;
		RouteNodeData data = node.routeNodeData;

		int usage = 1 + data.countSourceUses(con.source);
		
		double expected_cost = this.alpha * this.rrg.lowerEstimateConnectionCost(node, target) / usage;
		
		double bias_cost = node.baseCost 
							/ con.net.fanout
							* (Math.abs((0.5 * (node.xlow + node.xhigh)) - con.net.x_geo) + Math.abs((0.5 * (node.ylow + node.yhigh)) - con.net.y_geo))
							/ ((double) con.net.hpwl);
		return partial_path_cost + expected_cost + bias_cost;
	}

	private double getRouteNodeCost(RouteNode node, Connection con) {
		RouteNodeData data = node.routeNodeData;
		
		int usageSource = 1 + data.countSourceUses(con.source);
		boolean containsSource = usageSource != 1;
		
		double pres_cost;
		if (containsSource) {
			int overoccupation = data.numUniqueSources() - node.capacity;
			if (overoccupation < 0) {
				pres_cost = 1.0;
			} else {
				pres_cost = 1.0 + overoccupation * this.pres_fac;
			}
		} else {
			pres_cost = data.pres_cost;
		}
		
		return node.baseCost * data.acc_cost * pres_cost / (1 + data.countSourceUses(con.source));
	}
	
	private void updateCost(double pres_fac, double acc_fac){
		for (RouteNode node : this.rrg.getRouteNodes()) {
			RouteNodeData data = node.routeNodeData;
			int occ = data.occupation;
			int cap = node.capacity;

			//Present congestion penalty
			if (occ >= cap) {
				data.pres_cost = 1.0 + (occ - cap + 1) * pres_fac;
			} else {
				data.pres_cost = 1.0;
			}
			
			//Historical congestion penalty
			if(occ > cap) {
				data.acc_cost = data.acc_cost + (occ - cap) * acc_fac;
			} else {
				data.acc_cost = data.acc_cost;
			}
		}
	}
}
