package route.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import route.circuit.Circuit;
import route.circuit.resource.Opin;
import route.circuit.resource.ResourceGraph;
import route.circuit.resource.RouteNode;
import route.circuit.resource.RouteNodeType;

public class ConnectionRouter {
	final ResourceGraph rrg;
	final Circuit circuit;
	
	private float pres_fac;
	private float alphaWLD;
	
	private final static float CRITICALITY_THRESHOLD = 0.85f;
	private float currentCriticalityThreshold;
	private final List<Connection> criticalConnections;
	private final static int MAX_PERCENTAGE_CRITICAL_CONNECTIONS = 3;
	private float alphaTD = 0.75f;
	
	private final PriorityQueue<QueueElement> queue;
	
	private final Collection<RouteNodeData> nodesTouched;
	
	private final float BASE_COST_PER_DISTANCE;
	private final float IPIN_BASE_COST;
	private final static float MAX_CRITICALITY = 0.99f;
	private final static float CRITICALITY_EXPONENT = 3;
	
	private int itry;
	private boolean td;
	
	public static final boolean DEBUG = true;
	
	public ConnectionRouter(ResourceGraph rrg, Circuit circuit, boolean td) {
		this.rrg = rrg;
		this.circuit = circuit;

		this.nodesTouched = new ArrayList<>();
		
		this.queue = new PriorityQueue<>(Comparators.PRIORITY_COMPARATOR);
		
		this.criticalConnections = new ArrayList<>();
		
		double averageDelay = 0;
		int divide = 0;
		for(RouteNode node : this.rrg.getRouteNodes()) {
			if(node.isWire) {
				averageDelay += node.indexedData.t_linear;
				divide += node.indexedData.length;
			}
		}
		averageDelay /= divide;

		BASE_COST_PER_DISTANCE = (float) averageDelay;
		IPIN_BASE_COST = this.rrg.get_ipin_indexed_data().getBaseCost();
		
		for(RouteNode node : this.rrg.getRouteNodes()) {
			if(node.isWire) {
				node.updateBaseCost(BASE_COST_PER_DISTANCE);
			}
		}
		
		this.td = td;
	}
    
    public int route(float alphaTD) {
    	if(alphaTD > 0) this.alphaTD = alphaTD;
    	
    	System.out.println("---------------------------------------------------------------------------------------------------------");
    	System.out.println("|                                           CONNECTION ROUTER                                           |");
    	System.out.println("---------------------------------------------------------------------------------------------------------");
    	System.out.println("Num nets: " + this.circuit.getNets().size());
		System.out.println("Num cons: " + this.circuit.getConnections().size());
	
		int timeMilliseconds = this.doRuntimeRouting(100, 4, 1.5f);
		
		//TODO
		//System.out.println(this.circuit.getTimingGraph().criticalPathToString());
		
		/***************************
		 * OPIN tester: test if each
		 * net uses only one OPIN
		 ***************************/
		for(Net net : this.circuit.getNets()) {
			Set<Opin> opins = new HashSet<>();
			String name = null;
			for(Connection con : net.getConnections()) {
				Opin opin = con.getOpin();
				if(opin == null) {
					System.out.println("Connection has no opin!");
				} else {
					opins.add(opin);
				}
			}
			if(opins.size() != 1) {
				System.out.println("Net " + name + " has " + opins.size() + " opins");
			} 
		}
		
		return timeMilliseconds;
	}
    private int doRuntimeRouting(int nrOfTrials, int fixOpins, float alpha) {
    	System.out.printf("---------------------------------------------------------------------------------------------------------\n");
    	int timeMilliseconds = this.doRouting(nrOfTrials, fixOpins, alpha);
    	System.out.printf("---------------------------------------------------------------------------------------------------------\n");
    	System.out.println("Runtime " + timeMilliseconds + " ms");
    	System.out.printf("---------------------------------------------------------------------------------------------------------\n\n");
    	return timeMilliseconds;
    }
    private int doRouting(int nrOfTrials, int fixOpins, float alpha) {
    	long start = System.nanoTime();
    	
    	this.alphaWLD = alpha;
    	
    	this.nodesTouched.clear();
    	this.queue.clear();
		
    	float initial_pres_fac = 0.6f;
		float pres_fac_mult = 2;//1.3f or 2 //TODO WEEP
		float acc_fac = 1;
		this.pres_fac = initial_pres_fac;
		
		List<Connection> sortedListOfConnections = new ArrayList<>();
		sortedListOfConnections.addAll(this.circuit.getConnections());

		int intOldDelay = 0, intMaxDelay = 0;
		
		//TODO
		this.circuit.getTimingGraph().calculatePlacementEstimatedWireDelay();
		this.circuit.getTimingGraph().calculateConnectionCriticalities(MAX_CRITICALITY, CRITICALITY_EXPONENT);
		
		this.setRerouteCriticality(sortedListOfConnections);
		
		this.itry = 1;
		boolean bbnotfanout = false;
		if(bbnotfanout) {
			Collections.sort(sortedListOfConnections, Comparators.BBComparator);
		} else {
			Collections.sort(sortedListOfConnections, Comparators.FanoutCriticalityComparator);
		}
		
		List<Net> sortedListOfNets = new ArrayList<>();
		sortedListOfNets.addAll(this.circuit.getNets());
		Collections.sort(sortedListOfNets, Comparators.FANOUT);
        
		System.out.printf("%-22s | %s\n", "Timing Driven", this.td);
		System.out.printf("%-22s | %.1f\n", "Criticality Exponent", CRITICALITY_EXPONENT);
		System.out.printf("%-22s | %.2f\n", "Max Criticality", MAX_CRITICALITY);
		System.out.printf("%-22s | %.3e\n", "Base cost per distance", BASE_COST_PER_DISTANCE);
		System.out.printf("%-22s | %.3e\n", "IPIN Base cost", IPIN_BASE_COST);
		System.out.printf("%-22s | %.2f\n", "WLD Alpha", this.alphaWLD);
		System.out.printf("%-22s | %.2f\n", "TD Alpha", this.alphaTD);
		System.out.printf("%-22s | %.2f\n", "Crit Thres", CRITICALITY_THRESHOLD);
		System.out.printf("%-22s | %d\n", "Max per crit con", MAX_PERCENTAGE_CRITICAL_CONNECTIONS);
		
        System.out.printf("---------------------------------------------------------------------------------------------------------\n");
        System.out.printf("%9s  %8s  %7s  %9s  %10s  %9s  %17s  %11s  %9s\n", "Iteration", "AlphaWLD", "AlphaTD", "Crit Conn", "Crit Thres", "Time (ms)", "Overused RR Nodes", "Wire-Length", "Max Delay");
        System.out.printf("---------  --------  -------  ---------  ----------  ---------  -----------------  -----------  ---------\n");
        
        Set<RouteNode> overUsed = new HashSet<>();
        
        while (this.itry <= nrOfTrials) {
        	long iterationStart = System.nanoTime();
        	
        	//Route Connections
        	for(Connection con : sortedListOfConnections) {
        		
				if (this.itry == 1) {
					this.ripup(con);
					this.route(con);
					this.add(con);
			
					if(!con.net.hasOpin()) {
						con.net.setOpin(con.getOpin());
						con.getOpin().isOpin = true;
					}

				} else if(con.congested()) {
					this.ripup(con);
					this.route(con);
					this.add(con);
					
				} else if (con.getCriticality() > this.currentCriticalityThreshold) {
					this.ripup(con);
					this.route(con);
					this.add(con);
				}
			}
        	
        	Collections.sort(sortedListOfConnections, Comparators.FanoutComparator);
			
			String numOverUsed = String.format("%8s", "---");
			String overUsePercentage = String.format("%7s", "---");
			String wireLength = String.format("%11s", "---");
			String maxDelayString = String.format("%9s", "---");
			String criticalityThreshold = String.format("%9s", "---");
			String percentageCriticalConnections = String.format("%9s", "---");
			
			//Update timing and criticality
			if(this.td) {
				intOldDelay = (int) Math.round(100 * this.circuit.getTimingGraph().getMaxDelay());
				
				//TODO
				//this.circuit.getTimingGraph().calculateActualWireDelay();
				//this.circuit.getTimingGraph().calculateArrivalAndRequiredTimes();
				//this.circuit.getTimingGraph().calculateConnectionCriticality(MAX_CRITICALITY, CRITICALITY_EXPONENT);
				
				double maxDelay = this.circuit.getTimingGraph().getMaxDelay();
				intMaxDelay = (int) Math.round(100 * maxDelay);
				maxDelayString = String.format("%9.2f", maxDelay);
				
				this.setRerouteCriticality(sortedListOfConnections);
				
				criticalityThreshold = String.format("%10.3f", this.currentCriticalityThreshold);
				percentageCriticalConnections = String.format("%7.3f%%", (float)this.criticalConnections.size() / this.circuit.getConnections().size() * 100);
			} 
			
			if(DEBUG) {
				overUsed.clear();
				for (Connection conn : sortedListOfConnections) {
					for (RouteNode node : conn.routeNodes) {
						if (node.overUsed()) {
							overUsed.add(node);
						}
					}
				}
				int numRouteNodes = this.rrg.getRouteNodes().size();
				numOverUsed = String.format("%8d", overUsed.size());
				overUsePercentage = String.format("%6.2f%%", 100.0 * (float)overUsed.size() / numRouteNodes);
				
				wireLength = String.format("%11d", this.rrg.congestedTotalWireLengt());
				
				if(!this.td) {
					//TODO
					//this.circuit.getTimingGraph().calculateActualWireDelay();
					//this.circuit.getTimingGraph().calculateArrivalAndRequiredTimes();
					//maxDelayString = String.format("%9.3f", this.circuit.getTimingGraph().getMaxDelay());
				}
			}
			
			//Runtime
			long iterationEnd = System.nanoTime();
			int rt = (int) Math.round((iterationEnd-iterationStart) * Math.pow(10, -6));
			
			System.out.printf("%9d  %8.2f  %7.2f  %9s  %10s  %9d  %s  %s  %s  %s\n", this.itry, this.alphaWLD, this.alphaTD, percentageCriticalConnections, criticalityThreshold, rt, numOverUsed, overUsePercentage, wireLength, maxDelayString);
			
			//Check if the routing is realizable, if realizable return, the routing succeeded 
			if (this.validRouting(sortedListOfConnections, intOldDelay, intMaxDelay)){
				this.circuit.setConRouted(true);
				
				long end = System.nanoTime();
				int timeMilliSeconds = (int)Math.round((end-start) * Math.pow(10, -6));
				return timeMilliSeconds;
			}
			
			//Updating the cost factors
			if (this.itry == 1) {
				this.pres_fac = initial_pres_fac;
			} else {
				this.pres_fac *= pres_fac_mult;
			}
			this.updateCost(this.pres_fac, acc_fac);
			
			this.itry++;
		}
        
		if (this.itry == nrOfTrials + 1) {
			System.out.println("Routing failled after " + this.itry + " trials!");
			
			int maxNameLength = 0;
			
			Set<RouteNode> overused = new HashSet<>();
			for (Connection conn: sortedListOfConnections) {
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
    
    private boolean validRouting(List<Connection> connections, int oldDelay, int delay) {
    	if(delay < oldDelay) {
    		return false;
    	}
    	for(Connection connection : connections) {
    		if(connection.congested()) {
    			return false;
    		}
    	}
    	return true;
    }

    private void setRerouteCriticality(List<Connection> connections) {
    	//Limit number of critical connections
    	this.criticalConnections.clear();
    	
    	int maxNumberOfCriticalConnections = (int) (connections.size() * 0.01 * MAX_PERCENTAGE_CRITICAL_CONNECTIONS);
    	
    	for(Connection con : connections) {
    		if(con.getCriticality() > CRITICALITY_THRESHOLD) {
    			this.criticalConnections.add(con);
    		}
    	}
    	
    	if(this.criticalConnections.size() > maxNumberOfCriticalConnections) {
    		Collections.sort(this.criticalConnections, Comparators.ConnectionCriticality);
    		
    		this.currentCriticalityThreshold = this.criticalConnections.get(maxNumberOfCriticalConnections).getCriticality();
    		
        	this.criticalConnections.clear();
        	for(Connection con : connections) {
        		if(con.getCriticality() > this.currentCriticalityThreshold) {
        			this.criticalConnections.add(con);
        		}
        	}
    	} else {
    		this.currentCriticalityThreshold = CRITICALITY_THRESHOLD;
    	}
    	
    	for(Connection con: connections) {
    		con.setCriticalityThreshold(this.currentCriticalityThreshold);
    	}
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
		// Clear Routing
		con.resetConnection();

		// Clear Queue
		this.queue.clear();
		
		// Set target flag sink
		RouteNode sink = con.sinkRouteNode;
		sink.target = true;
		
		// Add source to queue
		RouteNode source = con.sourceRouteNode;
		this.addNodeToQueue(source, null, 0, 0);
		
		// Start Dijkstra / directed search
		while (!targetReached()) {
			this.expandFirstNode(con);
		}
		
		// Reset target flag sink
		sink.target = false;
		
		// Save routing in connection class
		this.saveRouting(con);
		
		// Reset path cost from Dijkstra Algorithm
		this.resetPathCost();

		return true;
	}

		
	private void saveRouting(Connection con) {
		RouteNode rn = con.sinkRouteNode;
		while (rn != null) {
			con.addRouteNode(rn);
			rn = rn.routeNodeData.prev;
		}
	}

	private boolean targetReached() {
		RouteNode queueHead = this.queue.peek().node;
		if(queueHead == null){
			System.out.println("queue is empty");			
			return false;
		} else {
			return queueHead.target;
		}
	}
	
	private void resetPathCost() {
		for (RouteNodeData node : this.nodesTouched) {
			node.touched = false;
		}
		this.nodesTouched.clear();
	}

	private void expandFirstNode(Connection con) {
		if (this.queue.isEmpty()) {
			System.out.println(con.netName + " " + con.source.getPortName() + " " + con.sink.getPortName());
			throw new RuntimeException("Queue is empty: target unreachable?");
		}

		RouteNode node = this.queue.poll().node;
		
		for (RouteNode child : node.children) {
			
			//CHANX OR CHANY
			if (child.isWire) {
				if (con.isInBoundingBoxLimit(child)) {
					this.addNodeToQueue(node, child, con);
				}
			
			//OPIN
			} else if (child.type == RouteNodeType.OPIN) {
				if(con.net.hasOpin()) {
					if (child.index == con.net.getOpin().index) {
						this.addNodeToQueue(node, child, con);
					}
				} else if (!child.isOpin) {
					this.addNodeToQueue(node, child, con);
				}
			
			//IPIN
			} else if (child.type ==  RouteNodeType.IPIN) {
				if(child.children[0].target) {
					this.addNodeToQueue(node, child, con);
				}
				
			//SINK
			} else if (child.type == RouteNodeType.SINK) {
				this.addNodeToQueue(node, child, con);
			}
		}
	}
	
	private void addNodeToQueue(RouteNode node, RouteNode child, Connection con) {
		RouteNodeData data = child.routeNodeData;
		int countSourceUses = data.countSourceUses(con.source);
		
		float partial_path_cost = node.routeNodeData.getPartialPathCost();
		float new_partial_path_cost, new_lower_bound_total_path_cost;
		
		// PARTIAL PATH COST
		
		if(con.isCritical) {
			new_partial_path_cost = partial_path_cost + (1 - con.getCriticality()) * this.getRouteNodeCost(child, con, countSourceUses) + con.getCriticality() * child.getDelay();
		} else {
			new_partial_path_cost = partial_path_cost + this.getRouteNodeCost(child, con, countSourceUses);
		}
		
		// LOWER BOUND TOTAL PATH COST
		// This is just an estimate and not an absolute lower bound.
		// The routing algorithm is therefore not A* and optimal.
		// It's directed search and heuristic.
		
		if(child.isWire) {
			//Expected remaining cost
			RouteNode target = con.sinkRouteNode;
			float distance_cost =  this.rrg.get_expected_distance_to_target(child, target) * BASE_COST_PER_DISTANCE;
			float expected_wire_cost = distance_cost / (1 + countSourceUses) + IPIN_BASE_COST;
			float expected_timing_cost = distance_cost;
			
			if(con.isCritical) {
				new_lower_bound_total_path_cost = new_partial_path_cost + this.alphaWLD * (1 - con.getCriticality()) * expected_wire_cost + this.alphaTD * con.getCriticality() * expected_timing_cost;
			} else {
				new_lower_bound_total_path_cost = new_partial_path_cost + this.alphaWLD * expected_wire_cost;
			}

		} else {
			new_lower_bound_total_path_cost = new_partial_path_cost;
		}
		
		this.addNodeToQueue(child, node, new_partial_path_cost, new_lower_bound_total_path_cost);
	}
	private void addNodeToQueue(RouteNode node, RouteNode prev, float new_partial_path_cost, float new_lower_bound_total_path_cost) {
		RouteNodeData data = node.routeNodeData;
		
		if(!data.touched) {
			this.nodesTouched.add(data);
			data.setLowerBoundTotalPathCost(new_lower_bound_total_path_cost);
			data.setPartialPathCost(new_partial_path_cost);
			data.prev = prev;
			this.queue.add(new QueueElement(node, new_lower_bound_total_path_cost));
			
		} else if (data.updateLowerBoundTotalPathCost(new_lower_bound_total_path_cost)) { //queue is sorted by lower bound total cost
			data.setPartialPathCost(new_partial_path_cost);
			data.prev = prev;
			this.queue.add(new QueueElement(node, new_lower_bound_total_path_cost));
		}
	}

	private float getRouteNodeCost(RouteNode node, Connection con, int countSourceUses) {
		RouteNodeData data = node.routeNodeData;
		
		boolean containsSource = countSourceUses != 0;
		
		float pres_cost;
		if (containsSource) {
			int overoccupation = data.numUniqueSources() - node.capacity;
			if (overoccupation < 0) {
				pres_cost = 1;
			} else {
				pres_cost = 1 + overoccupation * this.pres_fac;
			}
		} else {
			pres_cost = data.pres_cost;
		}
		
		//Bias cost
		float bias_cost = 0;
		if(node.isWire) {
			final float beta = 0.5f; //TODO SWEEP
			Net net = con.net;
			bias_cost = beta * node.base_cost / net.fanout * (Math.abs(node.centerx - net.x_geo) + Math.abs(node.centery - net.y_geo)) / net.hpwl;
		}

		final int usage_multiplier = 10; //TODO SWEEP
		return node.base_cost * data.acc_cost * pres_cost / (1 + (usage_multiplier * countSourceUses)) + bias_cost;
	}
	
	private void updateCost(float pres_fac, float acc_fac){
		for (RouteNode node : this.rrg.getRouteNodes()) {
			RouteNodeData data = node.routeNodeData;

			int overuse = data.occupation - node.capacity;
			
			//Present congestion penalty
			if(overuse == 0) {
				data.pres_cost = 1 + pres_fac;
			} else if (overuse > 0) {
				data.pres_cost = 1 + (overuse + 1) * pres_fac;
				data.acc_cost = data.acc_cost + overuse * acc_fac;
			}
		}
	}
}
