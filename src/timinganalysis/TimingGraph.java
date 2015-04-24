package timinganalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import circuit.Block;
import circuit.BlockType;
import circuit.Flipflop;
import circuit.Input;
import circuit.Lut;
import circuit.Net;
import circuit.Pin;
import circuit.PrePackedCircuit;

public class TimingGraph
{

	private static final double MHD_DELAY = 0.5;
	private static final double LUT_DELAY = 1.0;
	
	private PrePackedCircuit circuit;
	private List<TimingNode> startNodes;
	private List<TimingNode> endNodes;
	private Map<Block,ArrayList<TimingNode>> blockMap; //Maps a block to all its associated timingnodes
	private ArrayList<TimingEdge> edges; //Contains all edges present in the timing graph
	private double maxDelay;
	
	public TimingGraph(PrePackedCircuit circuit)
	{
		this.circuit = circuit;
		startNodes = new ArrayList<>();
		endNodes = new ArrayList<>();
		blockMap = new HashMap<>();
		edges = new ArrayList<>();
		maxDelay = 0.0;
	}
	
	public void buildTimingGraph()
	{
		//Build all trees starting from circuit inputs
		for(Input input:circuit.getInputs().values())
		{
			processStartPin(input.output);
		}
		
		//Build all trees starting from flipflop outputs
		for(Flipflop flipflop:circuit.getFlipflops().values())
		{
			processStartPin(flipflop.getOutput());
		}
		
		//Calculate arrival and required times (definition: see VPR book)
		calculateArrivalTimesFromScratch();
		maxDelay = calculateMaximalDelay();
		calculateRequiredTimesFromScratch();
		recalculateAllSlacks();
	}
	
	public double calculateMaximalDelay()
	{
		double maxDelay = 0.0;
		for(TimingNode endNode:endNodes)
		{
			if(endNode.getTArrival() > maxDelay)
			{
				maxDelay = endNode.getTArrival();
			}
		}
		return maxDelay;
	}
	
	private void calculateArrivalTimesFromScratch()
	{
		//Do a breadth first search of the timing graph (every startNode separately)
		for(TimingNode startNode: startNodes)
		{
			startNode.setTArrival(0.0);
			Queue<TimingNode> nodeQueue = new LinkedList<>();
			nodeQueue.add(startNode);
			
			while(!nodeQueue.isEmpty())
			{
				TimingNode currentNode = nodeQueue.remove();
				for(TimingEdge edge: currentNode.getOutputs())
				{
					TimingNode connectedNode = edge.getOutput();
					double possibleNewTArrival = currentNode.getTArrival() + edge.getDelay();
					if(possibleNewTArrival > connectedNode.getTArrival())
					{
						connectedNode.setTArrival(possibleNewTArrival);
						nodeQueue.add(connectedNode);
					}
				}
			}
		}
	}
	
	private void calculateRequiredTimesFromScratch()
	{
		//Do a breadth first search of the timing graph (every endNode separately)
		for(TimingNode endNode: endNodes)
		{
			endNode.setTRequired(maxDelay);
			Queue<TimingNode> nodeQueue = new LinkedList<>();
			nodeQueue.add(endNode);
			
			while(!nodeQueue.isEmpty())
			{
				TimingNode currentNode = nodeQueue.remove();
				for(TimingEdge edge: currentNode.getInputs())
				{
					TimingNode connectedNode = edge.getInput();
					double possibleNewTRequired = currentNode.getTRequired() - edge.getDelay();
					if(possibleNewTRequired < connectedNode.getTRequired())
					{
						connectedNode.setTRequired(possibleNewTRequired);
						nodeQueue.add(connectedNode);
					}
				}
			}
		}
	}
	
	private void recalculateAllSlacks()
	{
		for(TimingEdge edge: edges)
		{
			edge.recalculateSlack();
		}
	}
	
	private void processStartPin(Pin startPin)
	{
		Map<String,Net> nets = circuit.getNets();
		Stack<Net> netsStack = new Stack<>();
		Stack<Integer> sinkIndexStack = new Stack<>();
		Stack<TimingNode> currentTimingNodeStack = new Stack<>();
		TimingNode startNode = new TimingNode(TimingNodeType.StartNode, startPin);
		Block startBlock = startPin.owner;
		startNodes.add(startNode);
		if(blockMap.get(startBlock) == null)
		{
			blockMap.put(startBlock, new ArrayList<TimingNode>());
		}
		blockMap.get(startBlock).add(startNode);
		Net currentNet = nets.get(startBlock.name);
		int currentIndex = 0;
		TimingNode currentNode = startNode;
		if(currentNet.sinks.size() == 0) //Can happen with clock nets which are declared as an input in the blif file
		{
			return;
		}
		boolean keepGoing = true;
		while(keepGoing)
		{
			Pin currentSink = currentNet.sinks.get(currentIndex);
			int mhd = Math.abs(currentNode.getPin().owner.getSite().x - currentSink.owner.getSite().x) 
					+ Math.abs(currentNode.getPin().owner.getSite().y - currentSink.owner.getSite().y);
			if(currentSink.owner.type == BlockType.FLIPFLOP || currentSink.owner.type == BlockType.OUTPUT)
			{
				TimingNode endNode = new TimingNode(TimingNodeType.EndNode, currentSink);
				TimingEdge connection = new TimingEdge(currentNode, endNode, mhd * MHD_DELAY);
				edges.add(connection);
				currentNode.addOutput(connection);
				endNode.addInput(connection);
				endNodes.add(endNode);
				if(blockMap.get(currentSink.owner) == null)
				{
					blockMap.put(currentSink.owner, new ArrayList<TimingNode>());
				}
				blockMap.get(currentSink.owner).add(endNode);
			}
			else //Must be a LUT ==> keep on going
			{
				//Process TimingNode for Lut input
				TimingNode inputNode = new TimingNode(TimingNodeType.InternalNode, currentSink);
				TimingEdge connectionOne = new TimingEdge(currentNode, inputNode, mhd * MHD_DELAY);
				edges.add(connectionOne);
				currentNode.addOutput(connectionOne);
				inputNode.addInput(connectionOne);
				if(blockMap.get(currentSink.owner) == null)
				{
					blockMap.put(currentSink.owner, new ArrayList<TimingNode>());
				}
				List<TimingNode> lutNodeList = blockMap.get(currentSink.owner);
				lutNodeList.add(inputNode);
				TimingNode outputNode = null;
				Pin lutOutput = ((Lut)currentSink.owner).getOutputs()[0];
				for(TimingNode localNode: lutNodeList)
				{
					if(localNode.getPin() == lutOutput)
					{
						outputNode = localNode;
						break;
					}
				}
				if(outputNode == null)
				{
					outputNode = new TimingNode(TimingNodeType.InternalNode, lutOutput);
					lutNodeList.add(outputNode);
					TimingEdge connectionTwo = new TimingEdge(inputNode, outputNode, LUT_DELAY);
					edges.add(connectionTwo);
					inputNode.addOutput(connectionTwo);
					outputNode.addInput(connectionTwo);
					netsStack.push(currentNet);
					sinkIndexStack.push(currentIndex);
					currentTimingNodeStack.push(currentNode);
					currentNet = nets.get(currentSink.owner.name);
					currentIndex = -1; //will immediately be increased (see below)
					currentNode = outputNode;
				}
				else
				{
					TimingEdge connectionTwo = new TimingEdge(inputNode, outputNode, LUT_DELAY);
					edges.add(connectionTwo);
					inputNode.addOutput(connectionTwo);
					outputNode.addInput(connectionTwo);
				}
			}
			++currentIndex;
			if(!(currentIndex < currentNet.sinks.size()))
			{
				while(!(currentIndex < currentNet.sinks.size()) && keepGoing)
				{
					if(netsStack.isEmpty())
					{
						keepGoing = false;
					}
					else
					{
						currentNet = netsStack.pop();
						currentIndex = sinkIndexStack.pop();
						currentNode = currentTimingNodeStack.pop();
						++currentIndex;
					}
				}
			}
		}
	}
	
	public String getMapString()
	{
		String toReturn = "";
		int nbBlocks = 0;
		for(Block block:blockMap.keySet())
		{
			nbBlocks++;
			ArrayList<TimingNode> nodeList = blockMap.get(block);
			for(TimingNode node: nodeList)
			{
				toReturn += node.getPin().name + "(" + node.getTRequired() + ") ";
			}
			toReturn += "\n";
		}
		toReturn += "nbBlocks: " + nbBlocks + "\n";
		return toReturn;
	}
	
	public String getStartSlacks()
	{
		String toReturn = "";
		for(TimingNode node: startNodes)
		{
			toReturn += node.getPin().name + ": ";
			for(TimingEdge connectedEdge: node.getOutputs())
			{
				toReturn += connectedEdge.getSlack() + ", ";
			}
			toReturn += "\n";
		}
		return toReturn;
	}
	
	@Override
	public String toString()
	{
		String toReturn = "";
		for(TimingNode startNode: startNodes)
		{
			toReturn += (startNode.getPin().name);
			List<TimingEdge> connectedEdges = startNode.getOutputs();
			while(!connectedEdges.isEmpty())
			{
				TimingEdge firstEdge = connectedEdges.get(0);
				toReturn += "--" + firstEdge.getDelay() + "-->" + firstEdge.getOutput().getPin().name;
				connectedEdges = firstEdge.getOutput().getOutputs();
			}
			toReturn += "\n";
		}
		return toReturn;
	}
	
}