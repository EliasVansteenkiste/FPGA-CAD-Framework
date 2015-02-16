package timinganalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import placers.BoundingBoxData;

import circuit.Block;
import circuit.BlockType;
import circuit.Flipflop;
import circuit.Net;
import circuit.Pin;
import circuit.PrePackedCircuit;
import circuit.Input;
import circuit.Lut;

public class TimingGraph 
{

	private static final double MHD_DELAY = 0.5;
	private static final double LUT_DELAY = 1.0;
	
	private List<Pin> startNodes;
	private List<Pin> endNodes;
	private Map<Pin,Vector<Pin>> edges;
	private Map<Pin,Vector<Double>> edgeWeights; //only valid boundingbox data for circuit inputs, lut outputs and ff outputs
	
	public TimingGraph()
	{
		startNodes = new ArrayList<>();
		endNodes = new ArrayList<>();
		edges = new HashMap<>();
		edgeWeights = new HashMap<>();
	}
	
	public void buildTimingGraph(PrePackedCircuit circuit)
	{
		Map<String,Net> nets = circuit.getNets();
		
		//Build all trees starting from circuit inputs
		for(Input input:circuit.getInputs().values())
		{
			startNodes.add(input.output);
			Net startNet = nets.get(input.name);
			edges.put(input.output, startNet.sinks);
			Vector<Double> delayVector = new Vector<>();
			for(Pin sinkPin:startNet.sinks)
			{
				int bb = Math.abs(input.site.x - sinkPin.owner.site.x) + Math.abs(input.site.y - sinkPin.owner.site.y) + 2;
				delayVector.add(bb * MHD_DELAY);
			}
			edgeWeights.put(input.output, delayVector);
			processStartPin(nets, startNet);
		}
		
		//Build all trees starting from flipflop outputs
		for(Flipflop flipflop:circuit.getFlipflops().values())
		{
			startNodes.add(flipflop.getOutput());
			Net startNet = nets.get(flipflop.name);
			edges.put(flipflop.getOutput(), startNet.sinks);
			Vector<Double> delayVector = new Vector<>();
			for(Pin sinkPin:startNet.sinks)
			{
				int bb = Math.abs(flipflop.site.x - sinkPin.owner.site.x) + Math.abs(flipflop.site.y - sinkPin.owner.site.y) + 2;
				delayVector.add(bb * MHD_DELAY);
			}
			edgeWeights.put(flipflop.getOutput(), delayVector);
			processStartPin(nets, startNet);
		}
	}
	
	/*
	 * Calculates the maximal Manhattan distance in the placed circuit
	 */
	public double calculateMaximalDelay()
	{
		double curMax = 0.0;
		for(Pin startPin:startNodes)
		{
			
		}
	}
	
//	public void test(PrePackedCircuit circuit)
//	{
//		Lut lut = circuit.getLuts().get("[96]");
//		Pin output = lut.getOutputs()[0];
//		Vector<Pin> pinVector = edges.get(output);
//		for(Pin pin:pinVector)
//		{
//			System.out.print(" " + pin.name);
//		}
//	}
//	
//	public void printGraph()
//	{
//		for(Pin startPin:startNodes)
//		{
//			System.out.print(startPin.name);
//			Vector<Pin> currentPins = edges.get(startPin);
//			if(currentPins.size() == 0)
//			{
//				System.out.println();
//				continue;
//			}
//			while(currentPins != null)
//			{
//				System.out.print(" --> " + currentPins.get(0).name);
//				currentPins = edges.get(currentPins.get(0));
//			}
//			System.out.println();
//		}
//		System.out.println();
//	}
	
	private void processStartPin(Map<String,Net> nets, Net startNet)
	{
		Stack<Net> netsStack = new Stack<>();
		Stack<Integer> sinkIndexStack = new Stack<>();
		Net currentNet = startNet;
		int currentIndex = 0;
		if(currentNet.sinks.size() == 0) //Can happen with clock nets which are declared as an input in the blif file
		{
			return;
		}
		boolean keepGoing = true;
		while(keepGoing)
		{
			Pin currentSink = currentNet.sinks.get(currentIndex);
			if(currentSink.owner.type == BlockType.FLIPFLOP || currentSink.owner.type == BlockType.OUTPUT)
			{
				endNodes.add(currentSink);
				edges.put(currentSink, null);
				edgeWeights.put(currentSink, null);
			}
			else //Must be a LUT ==> keep on going
			{
				Vector <Pin> pinVector= new Vector<Pin>();
				pinVector.add(((Lut)(currentSink.owner)).getOutputs()[0]);
				edges.put(currentSink, pinVector);
				Vector<Double> delayVector1 = new Vector<>();
				delayVector1.add(LUT_DELAY);
				edgeWeights.put(currentSink,delayVector1);
				netsStack.push(currentNet);
				sinkIndexStack.push(currentIndex);
				currentNet = nets.get(currentSink.owner.name);
				currentIndex = -1; //will immediately be increased (see below)
				edges.put(currentNet.source, currentNet.sinks);
				Vector<Double> delayVector2 = new Vector<>();
				for(Pin sinkPin:currentNet.sinks)
				{
					int bb = Math.abs(currentNet.source.owner.site.x - sinkPin.owner.site.x) + 
							Math.abs(currentNet.source.owner.site.y - sinkPin.owner.site.y) + 2;
					delayVector2.add(bb * MHD_DELAY);
				}
				edgeWeights.put(currentNet.source, delayVector2);
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
						++currentIndex;
					}
				}
			}
		}
	}
	
}
