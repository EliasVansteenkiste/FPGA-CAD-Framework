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
	private final ResourceGraph rrg;
	private final Circuit circuit;
	
	private float pres_fac;
	private float alpha;
	
	private final PriorityQueue<RouteNode> queue;
	private final Collection<RouteNodeData> nodesTouched;
	
	private static final float MAX_CRITICALITY = 0.99f;
	
	private static final boolean DEBUG = false;
	
	public ConnectionRouter(ResourceGraph rrg, Circuit circuit) {
		this.rrg = rrg;
		this.circuit = circuit;

		this.queue = new PriorityQueue<>(Comparators.PRIORITY_COMPARATOR);
		this.nodesTouched = new ArrayList<RouteNodeData>();
	}
    
    public int route() {
    	System.out.println("---------------------------");
    	System.out.println("|         HROUTE          |");
    	System.out.println("---------------------------");
    	System.out.println("Num nets: " + this.circuit.getNets().size());
		System.out.println("Num cons: " + this.circuit.getConnections().size());
		System.out.println("---------------------------");
		System.out.println();
		
		this.doRouting("Route all", this.circuit.getConnections(), 100, true, 4, 1.2f);
		
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
    
    private void doRouting(String name, List<Connection> connections, int nrOfTrials, boolean routeAll, int fixOpins, float alpha) {
		System.out.printf("----------------------------------------------------------\n");
		System.out.println(name);
		int timeMilliseconds = this.doRouting(connections, nrOfTrials, routeAll, fixOpins, alpha);
		System.out.printf("----------------------------------------------------------\n");
		System.out.println("Runtime " + timeMilliseconds + " ms");
		System.out.printf("----------------------------------------------------------\n\n\n");
    }
    private int doRouting(List<Connection> connections, int nrOfTrials, boolean routeAll, int fixOpins, float alpha) {
    	long start = System.nanoTime();
    	
    	this.alpha = alpha;
    	
    	this.nodesTouched.clear();
    	this.queue.clear();
		
    	float initial_pres_fac = 0.6f;
		float pres_fac_mult = 2;//1.3 or 2.0
		float acc_fac = 1.0f;
		this.pres_fac = initial_pres_fac;
		
		int itry = 1;
		
		this.circuit.getTimingGraph().calculatePlacementEstimatedWireDelay();
		this.circuit.getTimingGraph().calculateArrivalAndRequiredTimes();
		this.circuit.getTimingGraph().calculateConnectionCriticality(0);//MAX_CRITICALITY);
		
		boolean bbnotfanout = false;
		if(bbnotfanout) {
			Collections.sort(connections, Comparators.BBComparator);
		} else {
			Collections.sort(connections, Comparators.FanoutComparator);
		}
		//Collections.sort(connections, Comparators.CriticalityComparator);
		
		boolean printConnections = false;
		if(printConnections) {
			for(Connection con : connections) {
				System.out.printf("%6d | Crit %.3f | Fanout %5d | BBCost %6d\n" , con.id, con.getCriticality(), con.net.fanout, con.boundingBox);
			}
		}
        
        Set<Net> nets = new HashSet<>();
        for(Connection con : connections) {
        	nets.add(con.net);
        }
        List<Net> sortedNets = new ArrayList<>(nets);
        Collections.sort(sortedNets, Comparators.FANOUT);
        
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
            	for(Net net : sortedNets) {
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
            	for(Net net : sortedNets) {
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
        			
        	for(Connection con : connections) {
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
				for (Connection conn : connections) {
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
			
			this.circuit.getTimingGraph().calculateActualWireDelay();
			this.circuit.getTimingGraph().calculateArrivalAndRequiredTimes();
			this.circuit.getTimingGraph().calculateConnectionCriticality(MAX_CRITICALITY);
			
			itry++;
		}
        
		if (itry == nrOfTrials + 1) {
			System.out.println("Routing failled after " + itry + " trials!");
			
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
		float source_cost = getRouteNodeCongestionCost(source, con);
		this.addNodeToQueue(source, null, source_cost, source_cost + expectedPathCost(source, con));
		
		// Start Dijkstra / directed search
		while (this.expandFirstNode(con));
		
		// Reset target flag sink
		sink.target = false;
		
		// Save routing in connection class
		this.saveRouting(con, sink);
		
		// Reset path cost from Dijkstra Algorithm
		this.resetPathCost();

		return true;
	}

	private void saveRouting(Connection con, RouteNode sink) {
		RouteNode rn = sink;
		while (rn != null) {
			con.addRouteNode(rn);
			rn = rn.routeNodeData.prev;
		}
	}
	
	private void resetPathCost() {
		for (RouteNodeData node : this.nodesTouched) {
			node.resetPathCosts();
		}
		this.nodesTouched.clear();
	}

	private boolean expandFirstNode(Connection con) {
		if (this.queue.isEmpty()) {
			System.out.println(con.netName + " " + con.source.getPortName() + " " + con.sink.getPortName());
			throw new RuntimeException("Queue is empty: target unreachable?");
		}

		RouteNode node = this.queue.poll();
		
		if(node.type.equals(RouteNodeType.IPIN)) { //Ipin with minimum cost found
			node.children[0].routeNodeData.prev = node;
			return false;
			
		} else { //Expand node
			for (RouteNode child : node.children) {
				if(child.type.equals(RouteNodeType.OPIN)) { //OPIN
					if(con.net.hasOpin()) {
						if(child.index == con.net.getOpin().index) {
							this.addNodeToQueue(node, child, con);
						}
					} else if(!child.used()) {
						this.addNodeToQueue(node, child, con);
					}
				} else if(child.type.equals(RouteNodeType.IPIN)) { //IPIN
					if(child.children[0].target) {
						this.addNodeToQueue(node, child, con);
					}
				} else if(con.isInBoundingBoxLimit(child)) { //ELSE
					this.addNodeToQueue(node, child, con);
				}
			}
			return true;
		}
	}
	
	private void addNodeToQueue(RouteNode node, RouteNode child, Connection con) {
		float partial_path_cost = this.partialPathCost(node, child, con);
		float lower_bound_total_path_cost = partial_path_cost + this.alpha * this.expectedPathCost(child, con) + this.biasCost(child, con);
		
		this.addNodeToQueue(child, node, partial_path_cost, lower_bound_total_path_cost);
	}
	private float partialPathCost(RouteNode node, RouteNode child, Connection con) {
		RouteNodeData data = node.routeNodeData;
		int usage = 1 + 10 * data.countSourceUses(con.source);
		
		return node.routeNodeData.getPartialPathCost() + getRouteNodeCongestionCost(child, con) / usage;
				//+ (1 - con.getCriticality()) * getRouteNodeCongestionCost(child, con) / usage
				//+ con.getCriticality() * child.getDelay() / usage; 
	}
	private float expectedPathCost(RouteNode node, Connection con) {
		if (node.type == RouteNodeType.CHANX || node.type == RouteNodeType.CHANY) {
			RouteNode target = con.sinkRouteNode;
			RouteNodeData data = node.routeNodeData;
			
			int usage = 1 + data.countSourceUses(con.source);
			
			int[] num_segs_to_target = this.rrg.get_expected_segs_to_target(node, target);
			int num_segs_same_dir = num_segs_to_target[0];
			int num_segs_ortho_dir = num_segs_to_target[1];

			float cong_cost = (num_segs_same_dir * node.indexedData.base_cost + num_segs_ortho_dir * node.indexedData.base_cost) / usage + this.rrg.get_ipin_indexed_data().base_cost;
			
			//float timing_cost = 0;
			//timing_cost += num_segs_same_dir * node.indexedData.t_linear;
			//timing_cost += num_segs_ortho_dir * node.indexedData.getOrthoData().t_linear;
			//timing_cost += this.ipinData.t_linear;
			//cload en t_quadratic zijn altijd 0 want buffered

			//return con.getCriticality() * timing_cost / usage + (1 - con.getCriticality()) * 
			return cong_cost;
		} else {
			
			return 0;
		}
	}
	private float biasCost( RouteNode node, Connection con) {
		return node.indexedData.base_cost / (2 * con.net.fanout)
				* (float)(Math.abs((0.5 * (node.xlow + node.xhigh)) - con.net.x_geo) + Math.abs((0.5 * (node.ylow + node.yhigh)) - con.net.y_geo))
				/ con.net.hpwl;
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
	
	private float getRouteNodeCongestionCost(RouteNode node, Connection con) {
		RouteNodeData data = node.routeNodeData;
		
		boolean containsSource = data.countSourceUses(con.source) != 0;
		
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
		
		return node.indexedData.base_cost * data.acc_cost * pres_cost;
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
