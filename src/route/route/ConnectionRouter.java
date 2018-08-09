package route.route;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		
		this.threadPool = new LinkedList<RouteThread>();
		this.numThreads = 8;
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
			
			System.out.print("Set num parallel threads");
			for(RouteNode node: this.rrg.getRouteNodes()) {
				node.setNumParallelThreads(this.numThreads);
			}
			System.out.println(" => finished");
			
			
//			//Start Theads
//			for(LeafNode leafNode : leafNodes) {
//				
//				System.out.println("Leaf node " + (leafNode.getIndex()) + " (" + leafNode.numConnections() + ") of " + leafNodes.size() + " Cost: " + leafNode.cost());
//				
//				RouteThread routeThread = new RouteThread(leafNode, 10, false, this.circuit, this.rrg);
//				routeThread.start();
//				this.threadPool.add(routeThread);
//			}
//			
//			//Finish Threads
//			int maxTimeMilliseconds = 0;
//			for(RouteThread routeThread : this.threadPool) {
//				if(!routeThread.isRunning()) {
//					this.threadPool.remove(routeThread);
//					
//					//int timeMilliseconds = routeThread.getTimeMilliSeconds();
//					//if(timeMilliseconds > maxTimeMilliseconds) {
//					//	maxTimeMilliseconds = timeMilliseconds;
//					//}
//					//
//					//runtimeMap.put(routeThread.getLeafNode(), timeMilliseconds);
//				}
//			}
//
//			System.out.println("Longest cluster took " + maxTimeMilliseconds + "ms");
//			System.out.println();
//			
//			System.out.println("Route remaining congested connections");
//			
//			RouteThread routeThread = new RouteThread("remainingCongestion", this.circuit.getConnections(), 100, false, this.circuit, this.rrg);
//			routeThread.start();
//			while(routeThread.isRunning()) {/*WAIT*/}
//			
//			System.out.println();
//			
//			boolean printRuntimeMap = true;
//			if(printRuntimeMap) {
//				System.out.println("RuntimeMap:");
//				System.out.println("Connections\tCost\tRuntime[ms]");
//				for(LeafNode leafNode : runtimeMap.keySet()) {
//					System.out.println(leafNode.numConnections() + "\t" + leafNode.cost() + "\t" + runtimeMap.get(leafNode));
//				}
//				System.out.println();
//			}
		} else {
			RouteThread routeThread = new RouteThread("Route All", this.circuit.getConnections(), 100, false, this.circuit, this.rrg);
			routeThread.start();
			while(routeThread.isRunning()) {/*WAIT*/}
		}
		
		return -1;
	}
}
