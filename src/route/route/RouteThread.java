package route.route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import route.circuit.Circuit;
import route.circuit.resource.ResourceGraph;
import route.circuit.resource.RouteNode;
import route.hierarchy.LeafNode;

public class RouteThread implements Runnable{
	private Thread t;
	private String threadName;
	public final int threadNum;
	
	private volatile boolean running;
	private volatile boolean paused;
	private final Object pauseLock;
	
	private final Circuit circuit;
	private final ResourceGraph rrg;
	private final LeafNode leafNode;
	
	public final Set<Connection> connections;
	private final int nrOfTrials;
	private final boolean onlyCongested;
	
	private double pres_fac;
	private double alpha;
	private final PriorityQueue<QueueElement> queue;
	private final Collection<RouteNodeData> nodesTouched;
	
	RouteThread(String name, Set<Connection> connections, int nrOfTrials, boolean onlyCongested, Circuit circuit, ResourceGraph rrg) {
		this.circuit = circuit;
		this.rrg = rrg;
		this.leafNode = null;
		this.threadNum = 0;
		
		this.threadName = name;
		System.out.println("Creating " +  this.threadName);
		
		this.connections = connections;
		this.nrOfTrials = nrOfTrials;
		this.onlyCongested = onlyCongested;
		
		this.running = true;
		this.paused = false;
		this.pauseLock = new Object();
		
		this.alpha = 1;
		this.nodesTouched = new ArrayList<RouteNodeData>();
		this.queue = new PriorityQueue<QueueElement>();
	}
	RouteThread(int threadNum, LeafNode leafNode, int nrOfTrials, boolean onlyCongested, Circuit circuit, ResourceGraph rrg) {
		this.circuit = circuit;
		this.rrg = rrg;
		this.leafNode = leafNode;

		this.threadName = "LeafNode" + leafNode.getIndex();
		this.threadNum = threadNum;
		System.out.println("Creating " +  this.threadName);
		
		this.connections = leafNode.getConnections();
		this.nrOfTrials = nrOfTrials;
		this.onlyCongested = onlyCongested;
		
		this.running = true;
		this.paused = false;
		this.pauseLock = new Object();
		
		this.alpha = 1;
		this.nodesTouched = new ArrayList<RouteNodeData>();
		this.queue = new PriorityQueue<QueueElement>();
	}
	
	//Getters and setters
	public LeafNode getLeafNode() {
		return this.leafNode;
	}
	
	//Thread Functionality
	public void start () {
		if (this.t == null) {
			this.t = new Thread (this, threadName);
			this.t.start ();
		}
	}
	
	@Override
	public void run() {
		while (this.running){
			synchronized (this.pauseLock) {
				if (!this.running) {// may have changed while waiting to
									// synchronize on pauseLock
					break;
				}
				if (this.paused) {
					try {
						this.pauseLock.wait();	// will cause this Thread to block until 
												// another thread calls pauseLock.notifyAll()
												// Note that calling wait() will
												// relinquish the synchronized lock that this
												// thread holds on pauseLock so another thread
												// can acquire the lock to call notifyAll()
												// (link with explanation below this code)
					} catch (InterruptedException ex) {
						break;
					}
					if (!this.running) { // running might have changed since we paused
						break;
					}
				}
			}
			this.doWork();
		}
	}
	
	public void pause() {
		// you may want to throw an IllegalStateException if !running
		this.paused = true;
	}
	public void resume() {
		synchronized (pauseLock) {
			this.paused = false;
			this.pauseLock.notifyAll(); // Unblocks thread
		}
	}
	
	public void stop() {
		this.running = false;
		// you might also want to interrupt() the Thread that is 
		// running this Runnable, too, or perhaps call:
		resume();
		// to unblock
	}
	
	public boolean isPaused() {
		return this.paused;
	}
	public boolean isRunning() {
		return this.running;
	}
	
	//Actual Routing Functionality
	private void doWork() {
    	this.doRouting();
		
    	this.stop();
	}
	
    private int doRouting() {
    	long start = System.nanoTime();
    	
    	this.nodesTouched.clear();
    	this.queue.clear();
    	
    	double initial_pres_fac = 0.5;
    	double pres_fac_mult = 2;
    	double acc_fac = 1;
    	this.pres_fac = initial_pres_fac;
    	int itry = 1;
    	
    	Map<Connection, Integer> mapOfConnections = new HashMap<>();
    	for(Connection con : this.connections) {
    		mapOfConnections.put(con, con.boundingBox);
    	}
    	
    	BBComparator bvc =  new BBComparator(mapOfConnections);
    	Map<Connection, Integer> sortedMapOfConnections = new TreeMap<>(bvc);
    	sortedMapOfConnections.putAll(mapOfConnections);
    	
    	int numRouteNodes = this.rrg.getRouteNodes().size();
    	
    	System.out.printf("---------  ---------  -----------------\n");
    	System.out.printf("%9s  %9s  %17s\n", "Iteration", "Time (ms)", "Overused RR Nodes");
    	System.out.printf("---------  ---------  -----------------\n");
    	
    	while (itry <= this.nrOfTrials) {
    		long iterationStart = System.nanoTime();
    		
    		for(Connection con : sortedMapOfConnections.keySet()){
    			if((itry == 1 && !this.onlyCongested) || con.congested(this.threadNum)) {
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
    		for (Connection conn: this.connections) {
    			for (RouteNode node: conn.routeNodes) {
    				if (node.overUsed(this.threadNum)) {
    					overused.add(node);
    				}
    			}
    		}
    		
    		double overUsePercentage = 100.0 * (double)overused.size() / numRouteNodes;
    		
    		System.out.printf("%9d  %9d  %8d   %5.2f%%\n", itry, rt, overused.size(), overUsePercentage);
    		
    		//Check if the routing is realizable, if realizable return, the routing succeeded 
    		if (routingIsFeasible(this.connections, this.threadNum)){
    			this.circuit.setConRouted(true);
    			
    			//TODO Update the RRG of all other leaf nodes so that the route nodes are fixed for this leaf node
    			
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

    	if (itry == this.nrOfTrials + 1) {
    		System.out.println("Routing failled after "+itry+" trials!");
    		
    		int maxNameLength = 0;
    		
    		Set<RouteNode> overused = new HashSet<>();
    		for (Connection conn: connections) {
    			for (RouteNode node: conn.routeNodes) {
    				if (node.overUsed(this.threadNum)) {
    					overused.add(node);
    				}
    			}
    		}
    		for (RouteNode node: overused) {
    			if(node.toString().length() > maxNameLength) {
    				maxNameLength = node.toString().length();
    			}
    		}
    		
    		for (RouteNode node: overused) {
    			System.out.println(node.toString());
    		}
    		System.out.println();
    	}
    	
    	long end = System.nanoTime();
    	int timeMilliSeconds = (int)Math.round((end-start) * Math.pow(10, -6));
    	return timeMilliSeconds;
    }
    
    private boolean routingIsFeasible(Set<Connection> connections, int thread) {
    	for (Connection con : connections) {
    		if(con.congested(thread)) {
    			return false;
    		}
    	}
    	return true;
    }

    
    
	private void ripup(Connection con) {
		for (RouteNode node : con.routeNodes) {
			RouteNodeData data = node.getRouteNodeData(this.threadNum);
		
			data.removeSource(con.source);
		
			// Calculation of present congestion penalty
			data.updatePresentCongestionPenalty(this.pres_fac, node.capacity);
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
		sink.target[this.threadNum] = true;
	
		// Add source to queue
		RouteNode source = con.sourceRouteNode;
		double source_cost = getRouteNodeCost(source, con);
		addNodeToQueue(source, null, source_cost, getLowerBoundTotalPathCost(source, con, source_cost));
	
		// Start Dijkstra / directed search
		while (!targetReached()) {//TODO CHECK
			expandFirstNode(con);
		}
		
		// Reset target flag sink
		sink.target[this.threadNum] = false;
		
		// Save routing in connection class
		saveRouting(con);
		
		// Reset path cost from Dijkstra Algorithm
		resetPathCost();
		
		return true;
	}
	
	private void add(Connection con) {
		for (RouteNode node : con.routeNodes) {
			RouteNodeData data = node.getRouteNodeData(this.threadNum);
			
			data.addSource(con.source);

			// Calculation of present congestion penalty
			data.updatePresentCongestionPenalty(this.pres_fac, node.capacity);
		}
	}
	
	private double getRouteNodeCost(RouteNode node, Connection con) {
		RouteNodeData data = node.getRouteNodeData(this.threadNum);
		
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
	
	
	/**
	 * This is just an estimate and not an absolute lower bound.
	 * The routing algorithm is therefore not A* and optimal.
	 * It's directed search and heuristic.
	 */
	private double getLowerBoundTotalPathCost(RouteNode node, Connection con, double partial_path_cost) {
		if(this.alpha == 0) return partial_path_cost;
		
		RouteNode target = con.sinkRouteNode;
		RouteNodeData data = node.getRouteNodeData(this.threadNum);
		
		int usage = 1 + data.countSourceUses(con.source);
		
		double expected_cost = this.alpha * this.rrg.lowerEstimateConnectionCost(node, target) / usage;
		
		double bias_cost = node.baseCost 
							/ con.net.fanout
							* (Math.abs((0.5 * (node.xlow + node.xhigh)) - con.net.x_geo) + Math.abs((0.5 * (node.ylow + node.yhigh)) - con.net.y_geo))
							/ ((double) con.net.hpwl);
		return partial_path_cost + expected_cost + bias_cost;
	}
    
	private void addNodeToQueue(RouteNode node, QueueElement prev, double new_partial_path_cost, double new_lower_bound_total_path_cost) {
		RouteNodeData nodeData = node.getRouteNodeData(this.threadNum);
		if(!nodeData.pathCostsSet()) this.nodesTouched.add(nodeData);
		nodeData.updatePartialPathCost(new_partial_path_cost);
		if (nodeData.updateLowerBoundTotalPathCost(new_lower_bound_total_path_cost)) {	//queue is sorted by lower bound total cost
			this.queue.add(new QueueElement(node, nodeData, prev));
		}
	}
	
	private void expandFirstNode(Connection con) {
		if (this.queue.isEmpty()) {
			throw new RuntimeException("Queue is empty: target unreachable?");
		}
		
		QueueElement qe = this.queue.poll();
		RouteNode node = qe.node;
		for (RouteNode child : node.children) {			
			if(con.isInBoundingBoxLimit(child)) {
				
				double childCost = node.getRouteNodeData(this.threadNum).getPartialPathCost() + getRouteNodeCost(child, con);
				double childCostEstimate = getLowerBoundTotalPathCost(child, con, childCost);
				this.addNodeToQueue(child, qe, childCost, childCostEstimate);
			}
		}
	}

	private void resetPathCost() {
		for (RouteNodeData node : this.nodesTouched) {
			node.resetPathCosts();
		}
		this.nodesTouched.clear();
	}
	
	private boolean targetReached() {
		if(this.queue.peek()==null){
			System.out.println("queue is leeg");			
			return false;
		} else {
			return this.queue.peek().node.target[this.threadNum];
		}
	}
	
	private void updateCost(double pres_fac, double acc_fac){
		for (RouteNode node : this.rrg.getRouteNodes()) {
			RouteNodeData data = node.getRouteNodeData(this.threadNum);
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
	
	
	private void saveRouting(Connection con) { //TODO SYNCHRONISATION?
		QueueElement qe = this.queue.peek();
		
		while (qe != null) {
			con.addRouteNode(qe.node);
			qe = qe.prev;
		}
	}
}
