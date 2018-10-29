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
	private float alpha;
	
	private final PriorityQueue<RouteNode> queue;
	
	private final Collection<RouteNodeData> nodesTouched;
	
	private final List<String> printRouteInformation;
	
	private final static float BASE_COST_PER_DISTANCE = 3.34756e-11f;
	private final static float IPIN_BASE_COST = 3.18018e-11f;
	private static final float MAX_CRITICALITY = 0.99f;
	
	private float averageDelay;
	
	public static final boolean DEBUG = true;
	
	public ConnectionRouter(ResourceGraph rrg, Circuit circuit) {
		this.rrg = rrg;
		this.circuit = circuit;

		this.nodesTouched = new ArrayList<>();
		
		this.queue = new PriorityQueue<>(Comparators.PRIORITY_COMPARATOR);
		
		this.printRouteInformation = new ArrayList<>();
		
		this.averageDelay = 0;
		float divide = 0;
		for(RouteNode node : this.rrg.getRouteNodes()) {
			if(node.type == RouteNodeType.CHANX || node.type == RouteNodeType.CHANY) {
				this.averageDelay += node.indexedData.t_linear;
				divide += 1.0 / node.indexedData.inv_length;
			}
		}
		this.averageDelay /= divide;
		
		System.out.println(averageDelay);
	}
    
    public int route() {
    	System.out.println("---------------------------");
    	System.out.println("|         HROUTE          |");
    	System.out.println("---------------------------");
    	System.out.println("Num nets: " + this.circuit.getNets().size());
		System.out.println("Num cons: " + this.circuit.getConnections().size());
		System.out.println("---------------------------");
		System.out.println();
	
		this.doRouting("Route all", 100, true, 4, 1.5f);
		
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
		
		return -1;
	}
    private void doRouting(String name, int nrOfTrials, boolean routeAll, int fixOpins, float alpha) {
		System.out.printf("----------------------------------------------------------\n");
		System.out.println(name);
		int timeMilliseconds = this.doRouting(nrOfTrials, routeAll, fixOpins, alpha);
		System.out.printf("----------------------------------------------------------\n");
		System.out.println("Runtime " + timeMilliseconds + " ms");
		System.out.printf("----------------------------------------------------------\n\n\n");
		
		this.printRouteInformation.add(String.format("%s %d", name, timeMilliseconds));
    }
    private int doRouting(int nrOfTrials, boolean routeAll, int fixOpins, float alpha) {
    	long start = System.nanoTime();
    	
    	this.alpha = alpha;
    	
    	this.nodesTouched.clear();
    	this.queue.clear();
		
    	float initial_pres_fac = 0.6f;
		float pres_fac_mult = 2;//1.3f or 2
		float acc_fac = 1;
		this.pres_fac = initial_pres_fac;
		
		List<Connection> sortedListOfConnections = new ArrayList<>();
		sortedListOfConnections.addAll(this.circuit.getConnections());

		int itry = 1;
		boolean bbnotfanout = false;
		if(bbnotfanout) {
			Collections.sort(sortedListOfConnections, Comparators.BBComparator);
		} else {
			Collections.sort(sortedListOfConnections, Comparators.FanoutComparator);
		}
        
        List<Net> sortedListOfNets = new ArrayList<>();
        sortedListOfNets.addAll(this.circuit.getNets());
        Collections.sort(sortedListOfNets, Comparators.FANOUT);
        
        //Placement estimated timing
        this.circuit.getTimingGraph().calculatePlacementEstimatedWireDelay();
        this.circuit.getTimingGraph().calculateArrivalAndRequiredTimes();
        this.circuit.getTimingGraph().calculateConnectionCriticality(0);//MAX_CRITICALITY);
        
        System.out.printf("---------  -----  ---------  -----------------  -----------\n");
        System.out.printf("%9s  %5s  %9s  %17s  %11s\n", "Iteration", "Alpha", "Time (ms)", "Overused RR Nodes", "Wire-Length");
        System.out.printf("---------  -----  ---------  -----------------  -----------\n");
        
        Set<RouteNode> overUsed = new HashSet<>();
        
        boolean validRouting = false;
        
        while (itry <= nrOfTrials) {
        	validRouting = true;
        	long iterationStart = System.nanoTime();

        	//Fix opins in order of high fanout nets
        	if(itry >= fixOpins) {
            	for(Net net : sortedListOfNets) {
            		if(!net.hasOpin()) {
            			Opin opin = net.getUniqueOpin();
            			if(opin != null) {
            				if(!opin.overUsed()) {
            					net.setOpin(opin);
            				}
            			}
            			validRouting = false;
            		}
            	}
            	for(Net net : sortedListOfNets) {
            		if(!net.hasOpin()) {
            			Opin opin = net.getMostUsedOpin();
                		if(!opin.used()) {
                			net.setOpin(opin);
                		}
                		validRouting = false;
            		}
            	}
        	} else if(fixOpins < Integer.MAX_VALUE){
        		validRouting = false;
        	}
        			
        	for(Connection con : sortedListOfConnections) {
				if((itry == 1 && routeAll) || con.congested()) {
					this.ripup(con);
					this.route(con);
					this.add(con);
					
					validRouting = false;
				} else if(con.net.hasOpin()) {
					if(con.getOpin().index != con.net.getOpin().index) {
						this.ripup(con);
						this.route(con);
						this.add(con);
						
						validRouting = false;
					}
				}
			}
        	
			//Runtime
			long iterationEnd = System.nanoTime();
			int rt = (int) Math.round((iterationEnd-iterationStart) * Math.pow(10, -6));
			
			String numOverUsed = String.format("%8s", "---");
			String overUsePercentage = String.format("%7s", "---");
			String wireLength = String.format("%11s", "---");
			
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
			}
			
			
			//Check if the routing is realizable, if realizable return, the routing succeeded 
			if (validRouting){
				this.circuit.setConRouted(true);
				
				long end = System.nanoTime();
				int timeMilliSeconds = (int)Math.round((end-start) * Math.pow(10, -6));
				return timeMilliSeconds;
			} else {
				System.out.printf("%9d  %.3f %9d  %s  %s  %s\n", itry, this.alpha, rt, numOverUsed, overUsePercentage, wireLength);
			}
			
			//Updating the cost factors
			if (itry == 1) {
				this.pres_fac = initial_pres_fac;
			} else {
				this.pres_fac *= pres_fac_mult;
			}
			this.updateCost(this.pres_fac, acc_fac);
			
			//Update timing and criticality
			this.circuit.getTimingGraph().calculateActualWireDelay();
			this.circuit.getTimingGraph().calculateArrivalAndRequiredTimes();
			this.circuit.getTimingGraph().calculateConnectionCriticality(MAX_CRITICALITY);            
			
			itry++;
		}
        
		if (itry == nrOfTrials + 1) {
			System.out.println("Routing failled after " + itry + " trials!");
			
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
		RouteNode rn = this.queue.peek();
		while (rn != null) {
			con.addRouteNode(rn);
			rn = rn.routeNodeData.prev;
		}
	}

	private boolean targetReached() {
		if(this.queue.peek()==null){
			System.out.println("queue is empty");			
			return false;
		} else {
			return queue.peek().target;
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
			System.out.println(con.netName + " " + con.source.getPortName() + " " + con.sink.getPortName());
			throw new RuntimeException("Queue is empty: target unreachable?");
		}

		RouteNode node = this.queue.poll();
		
		for (RouteNode child : node.children) {
			RouteNodeType childType = child.type;
			
			//OPIN
			if(childType == RouteNodeType.OPIN) {
				if(con.net.hasOpin()) {
					if(child.index == con.net.getOpin().index) {
						this.addNodeToQueue(node, child, con);
					}
				} else if(!child.used()) {
					this.addNodeToQueue(node, child, con);
				}
			
			//IPIN
			} else if(childType ==  RouteNodeType.IPIN) {
				if(child.children[0].target) {
					this.addNodeToQueue(node, child, con);
				}
				
			//CHANX OR CHANY
			} else if(con.isInBoundingBoxLimit(child)) {
				this.addNodeToQueue(node, child, con);
			}
		}
	}
	
	private void addNodeToQueue(RouteNode node, RouteNode child, Connection con) {
		RouteNodeData data = child.routeNodeData;
		int countSourceUses = data.countSourceUses(con.source);
		
		float new_partial_path_cost =  node.routeNodeData.getPartialPathCost() + (1 - con.getCriticality()) * this.getRouteNodeCost(child, con, countSourceUses) + con.getCriticality() * child.getDelay();
		float new_lower_bound_total_path_cost = getLowerBoundTotalPathCost(child, con, new_partial_path_cost, countSourceUses);
		
		this.addNodeToQueue(child, node, new_partial_path_cost, new_lower_bound_total_path_cost);
	}
	private void addNodeToQueue(RouteNode node, RouteNode prev, float new_partial_path_cost, float new_lower_bound_total_path_cost) {
		RouteNodeData nodeData = node.routeNodeData;
		if(!nodeData.pathCostsSet()) this.nodesTouched.add(nodeData);

		nodeData.updatePartialPathCost(new_partial_path_cost);
		if (nodeData.updateLowerBoundTotalPathCost(new_lower_bound_total_path_cost)) { //queue is sorted by lower bound total cost
			node.routeNodeData.prev = prev;
			this.queue.add(node);
		}
	}

	/**
	 * This is just an estimate and not an absolute lower bound.
	 * The routing algorithm is therefore not A* and optimal.
	 * It's directed search and heuristic.
	 */
	private float getLowerBoundTotalPathCost(RouteNode node, Connection con, float partial_path_cost, int countSourceUses) {
		if(node.type == RouteNodeType.CHANX || node.type == RouteNodeType.CHANY) {
			//Expected remaining cost
			RouteNode target = con.sinkRouteNode;
			int distance =  this.rrg.get_expected_distance_to_target(node, target);
			float expected_wire_cost = distance * BASE_COST_PER_DISTANCE / (1 + countSourceUses) + IPIN_BASE_COST;
			float expected_timing_cost = distance * this.averageDelay;

			return partial_path_cost + this.alpha * ((1 - con.getCriticality()) * expected_wire_cost + con.getCriticality() * expected_timing_cost);
		} else {
			return partial_path_cost;
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
		Net net = con.net;
		float bias_cost = node.base_cost / (2 * net.fanout) * (Math.abs(node.centerx - net.x_geo) + Math.abs(node.centery - net.y_geo)) / net.hpwl;
		
		return node.base_cost * data.acc_cost * pres_cost / (1 + (10 * countSourceUses)) + bias_cost;
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
