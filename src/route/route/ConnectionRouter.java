package route.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import route.circuit.Circuit;
import route.circuit.resource.ResourceGraph;
import route.circuit.resource.RouteNode;
import route.hierarchy.HierarchyNode;
import route.hierarchy.LeafNode;

public class ConnectionRouter {
	private final ResourceGraph rrg;
	private final Circuit circuit;
	
	private final List<RouteThread> threadPool;
	private final int numThreads;
	
	public ConnectionRouter(ResourceGraph rrg, Circuit circuit) {
		this.rrg = rrg;
		this.circuit = circuit;
		
		this.numThreads = 1;
		this.threadPool = new LinkedList<RouteThread>();
		
		System.out.print("Set num parallel threads");
		for(RouteNode node: this.rrg.getRouteNodes()) {
			node.setNumParallelThreads(this.numThreads);
		}
		System.out.println(" => finished");
	}
    
    public int route(HierarchyNode rootNode) {
    	System.out.println("Num nets: " + this.circuit.getNets().size());
		System.out.println("Num cons: " + this.circuit.getConnections().size());
		System.out.println();
		
		boolean parallelRouting = true;
		
		if(parallelRouting) {
			Map<LeafNode, Integer> runtimeMap = new HashMap<>();
			
			//Make a sorted list of leaf nodes //TODO Sort criterium?
			List<LeafNode> leafNodes = new ArrayList<>();
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
			Collections.sort(leafNodes, new Comparator<LeafNode>() {
			     @Override
			     public int compare(LeafNode l1, LeafNode l2) {
			         return Integer.compare(l1.getIndex(), l2.getIndex());
			     }
			 });
			
			
			int leafNodeNumber = 0;
			int threadNumber = 0;
			
			while(leafNodeNumber < leafNodes.size()) {//TODO THIS IS NOT CORRECT
				while(this.threadPool.size() < this.numThreads  && leafNodeNumber < leafNodes.size()) {
					LeafNode leafNode = leafNodes.get(leafNodeNumber);
					
					System.out.println("Leaf node " + (leafNode.getIndex()) + " of " + leafNodes.size() + " Num connections: " + leafNode.numConnections() + " Cost: " + leafNode.cost());
					
					RouteThread routeThread = new RouteThread(threadNumber, leafNode, 10, false, this.circuit, this.rrg);
					routeThread.start();
					this.threadPool.add(routeThread);
					
					leafNodeNumber++;
					threadNumber++;
				}
				
				//Finish Threads
				while(!this.threadPool.isEmpty()) {
					for(int i = 0; i < this.threadPool.size(); i++) {
						RouteThread routeThread = this.threadPool.get(i);
						
						if(!routeThread.isRunning()) {
							this.threadPool.remove(routeThread);
							
							i = this.threadPool.size() + 1;
							
							//int timeMilliseconds = routeThread.getTimeMilliSeconds();
							//if(timeMilliseconds > maxTimeMilliseconds) {
							//	maxTimeMilliseconds = timeMilliseconds;
							//}
							//
							//runtimeMap.put(routeThread.getLeafNode(), timeMilliseconds);
							
							//TODO clean up the data
							for(Connection con : routeThread.connections) {
								for(RouteNode node : con.routeNodes) {
									node.resetRouteNodeData(routeThread.threadNum);
								}
							}
							
							//TODO Update all the route nodes on the path
							for(Connection con : routeThread.connections) {
								for(RouteNode node : con.routeNodes) {
									node.addSource(con.source);
								}
							}
						}
					}
				}
				
				threadNumber = 0;
			}
			

			//System.out.println("Longest cluster took " + maxTimeMilliseconds + "ms");
			System.out.println();
			
			System.out.println("Route remaining congested connections");
			
			RouteThread routeThread = new RouteThread("remainingCongestion", this.circuit.getConnections(), 100, true, this.circuit, this.rrg);
			routeThread.start();
			while(routeThread.isRunning()) {/*WAIT*/}
			
			System.out.println();
			
			boolean printRuntimeMap = true;
			if(printRuntimeMap) {
				System.out.println("RuntimeMap:");
				System.out.println("Connections\tCost\tRuntime[ms]");
				for(LeafNode leafNode : runtimeMap.keySet()) {
					System.out.println(leafNode.numConnections() + "\t" + leafNode.cost() + "\t" + runtimeMap.get(leafNode));
				}
				System.out.println();
			}
		} else {
			RouteThread routeThread = new RouteThread("Route All", this.circuit.getConnections(), 100, false, this.circuit, this.rrg);
			routeThread.start();
			while(routeThread.isRunning()) {/*WAIT*/}
		}
		
		return -1;
	}
}
