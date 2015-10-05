package flexible_architecture.timing_graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import placers.SAPlacer.Swap;

import flexible_architecture.Circuit;
import flexible_architecture.architecture.BlockType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.block.LocalBlock;
import flexible_architecture.pin.AbstractPin;
import flexible_architecture.site.AbstractSite;

public class TimingGraph {
	
	private Circuit circuit;
	private Map<LocalBlock, TimingNode> nodes = new HashMap<LocalBlock, TimingNode>();
	private Map<GlobalBlock, List<TimingNode>> nodesInGlobalBlocks = new HashMap<GlobalBlock, List<TimingNode>>();
	private List<TimingNode> clockedNodes = new ArrayList<TimingNode>();
	private List<TimingNode> affectedNodes = new ArrayList<TimingNode>();
	
	private double criticalityExponent = 8;
	private double maxArrivalTime;
	
	public TimingGraph(Circuit circuit) {
		this.circuit = circuit;
		TimingNode.setWireDelay(circuit.getArchitecture().getWireDelay());
	}
	
	public void build() {
		
		// Get all leaf nodes
		for(BlockType blockType : this.circuit.getBlockTypes()) {
			if(!blockType.isLeaf()) {
				continue;
			}
			
			for(AbstractBlock block : this.circuit.getBlocks(blockType)) {
				AbstractBlock parent = block;
				while(!parent.isGlobal()) {
					parent = parent.getParent();
				}
				TimingNode node = new TimingNode((GlobalBlock) parent);
				
				
				this.nodes.put((LocalBlock) block, node);
				
				if(this.nodesInGlobalBlocks.get(parent) == null) {
					this.nodesInGlobalBlocks.put((GlobalBlock) parent, new ArrayList<TimingNode>());
				}
				this.nodesInGlobalBlocks.get(parent).add(node);
				
				if(blockType.isClocked()) {
					this.clockedNodes.add(node);
				}
			}
		}
		
		for(Map.Entry<LocalBlock, TimingNode> nodeEntry : this.nodes.entrySet()) {
			LocalBlock block = nodeEntry.getKey();
			TimingNode node = nodeEntry.getValue();
			
			this.traverseFromSource(block, node);
		}
	}
	
	private void traverseFromSource(LocalBlock block, TimingNode pathSource) {
		Stack<AbstractPin> pinStack = new Stack<AbstractPin>();
		Stack<Double> delayStack = new Stack<Double>();
		
		for(AbstractPin outputPin : block.getOutputPins()) {
			double setupDelay = 0;
			if(block.isClocked()) {
				setupDelay = outputPin.getPortType().getSetupTime();
			}
			
			pinStack.push(outputPin);
			delayStack.push(setupDelay);
		}
		
		
		while(pinStack.size() > 0) {
			AbstractPin currentPin = pinStack.pop();
			double currentDelay = delayStack.pop();
			
			AbstractBlock owner = currentPin.getOwner();
			
			if(currentPin.isInput() && owner.isLeaf()) {
				TimingNode pathSink = this.nodes.get(owner);
				
				double endDelay; 
				if(owner.isClocked()) {
					endDelay = currentPin.getPortType().getSetupTime();
				
				} else {
					List<AbstractPin> outputPins = owner.getOutputPins();
					endDelay = currentPin.getPortType().getDelay(outputPins.get(0).getPortType());
				}
				
				
				pathSource.addSink(pathSink, currentDelay + endDelay);
				
			
			// The block has children: proceed with the sinks of the current pin
			} else {
				for(AbstractPin sinkPin : currentPin.getSinks()) {
					if(sinkPin != null) {
						double sourceSinkDelay = currentPin.getPortType().getDelay(sinkPin.getPortType());
						
						pinStack.push(sinkPin);
						delayStack.push(currentDelay + sourceSinkDelay);
					}
				}
			}
		}
	}
	
	
	
	public void setCriticalityExponent(double criticalityExponent) {
		this.criticalityExponent = criticalityExponent;
	}
	
	
	public void recalculateAllSlackCriticalities() {
		this.maxArrivalTime = this.calculateArrivalTimes();
		this.calculateRequiredTimes();
		
		for(TimingNode node : this.nodes.values()) {
			node.calculateCriticalities(this.maxArrivalTime, this.criticalityExponent);
		}
	}
	
	
	
	private double calculateArrivalTimes() {
		for(TimingNode node : this.nodes.values()) {
			node.reset();
			node.calculateSinkDelays();
		}
		
		
		
		Stack<TimingNode> todo = new Stack<TimingNode>();
		todo.addAll(this.clockedNodes);
		
		double maxArrivalTime = 0;
		while(todo.size() > 0) {
			TimingNode currentNode = todo.pop();
			
			double arrivalTime = currentNode.calculateArrivalTime();
			
			if(arrivalTime > maxArrivalTime) {
				maxArrivalTime = arrivalTime;
			}
			
			for(TimingNode sink : currentNode.getSinks()) {
				sink.incrementProcessedSources();
				if(sink.allSourcesProcessed()) {
					todo.add(sink);
				}
			}
		}
		
		return maxArrivalTime;
	}
	
	private void calculateRequiredTimes() {
		Stack<TimingNode> todo = new Stack<TimingNode>();
		Stack<TimingNode> done = new Stack<TimingNode>();
		
		for(TimingNode endNode : this.clockedNodes) {
			endNode.setRequiredTime(this.maxArrivalTime);
			this.requiredTimesAddChildren(endNode, todo);
			done.add(endNode);
		}
		
		while(todo.size() > 0) {
			TimingNode currentNode = todo.pop();
			currentNode.calculateRequiredTime();
			this.requiredTimesAddChildren(currentNode, todo);
			done.add(currentNode);
		}
	}
	
	private void requiredTimesAddChildren(TimingNode node, Collection<TimingNode> todo) {
		for(TimingNode source : node.getSources()) {
			source.incrementProcessedSinks();
			if(source.allSinksProcessed()) {
				todo.add(source);
			}
		}
	}
	
	
	
	public double getMaxArrivalTime() {
		return this.maxArrivalTime;
	}
	
	
	
	public double calculateTotalCost() {
		double totalCost = 0;
		
		for(TimingNode node : this.nodes.values()) {
			totalCost += node.calculateCost();
		}
		
		return totalCost;
	}
	
	
	public double calculateDeltaCost(Swap swap) {
		double cost = 0;
		
		this.affectedNodes.clear();
		
		List<TimingNode> nodes = this.nodesInGlobalBlocks.get(swap.getBlock1());
		this.affectedNodes.addAll(nodes);
		cost += this.calculateDeltaCost(nodes, swap.getSite2());
		
		if(swap.getBlock2() != null) {
			nodes = this.nodesInGlobalBlocks.get(swap.getBlock2());
			this.affectedNodes.addAll(nodes);
			cost += this.calculateDeltaCost(nodes, swap.getSite1());
		}
		
		return cost;
	}
	
	private double calculateDeltaCost(List<TimingNode> nodes, AbstractSite site) {
		double cost = 0;
		
		for(TimingNode node : nodes) {
			cost += node.calculateDeltaCost(site.getX(), site.getY());
		}
		
		return cost;
	}
	
	
	public void pushThrough() {
		for(TimingNode node : this.affectedNodes) {
			node.pushThrough();
		}
	}
	
	public void revert() {
		// Do nothing
	}
}
