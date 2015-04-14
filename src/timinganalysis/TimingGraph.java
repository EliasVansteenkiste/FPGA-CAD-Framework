package timinganalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	public TimingGraph(PrePackedCircuit circuit)
	{
		this.circuit = circuit;
		startNodes = new ArrayList<>();
		endNodes = new ArrayList<>();
		blockMap = new HashMap<>();
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
	}
	
	public double calculateMaximumDelay()
	{
		calculateArrivalTimesFromScratch();
		double maxDelay = 0.0;
		for(TimingNode endNode:endNodes)
		{
			if(endNode.getTarrival() > maxDelay)
			{
				maxDelay = endNode.getTarrival();
			}
		}
		return maxDelay;
	}
	
	private void calculateArrivalTimesFromScratch()
	{
		//Do a breadth first search of the timing graph
		List<TimingNode> nextLevelNodes = new ArrayList<>();
		for(TimingNode startNode: startNodes)
		{
			startNode.setTarrival(0.0);
			nextLevelNodes.addAll(startNode.getOutputs());
		}
		while(!nextLevelNodes.isEmpty())
		{
			List<TimingNode> curLevelNodes = nextLevelNodes;
			nextLevelNodes = new ArrayList<>();
			for(TimingNode node: curLevelNodes)
			{
				node.setTarrival(0.0);
				nextLevelNodes.addAll(node.getOutputs());
			}
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
				currentNode.addOutput(endNode, mhd * MHD_DELAY);
				endNode.addInput(currentNode);
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
				currentNode.addOutput(inputNode, mhd * MHD_DELAY);
				inputNode.addInput(currentNode);
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
				}
				inputNode.addOutput(outputNode, LUT_DELAY);
				outputNode.addInput(inputNode);
				netsStack.push(currentNet);
				sinkIndexStack.push(currentIndex);
				currentTimingNodeStack.push(currentNode);
				currentNet = nets.get(currentSink.owner.name);
				currentIndex = -1; //will immediately be increased (see below)
				currentNode = outputNode;
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
	
}
