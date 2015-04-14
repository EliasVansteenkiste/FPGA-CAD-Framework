package timinganalysis;

import java.util.ArrayList;
import java.util.List;

import circuit.Pin;

public class TimingNode
{
	
	private Pin pin;
	private TimingNodeType type;
	private double Tarrival;
	private double Trequired;
	private List<TimingNode> inputs; //
	private List<TimingNode> outputs;
	//Contains the delay from output of this node to the input of the corresponding connected TimingBlock (contained in outputs)
	private List<Double> outputDelays;
	
	public TimingNode(TimingNodeType type, Pin pin)
	{
		this.type = type;
		this.pin = pin;
		Tarrival = 0.0;
		Trequired = 0.0;
		if(type != TimingNodeType.EndNode)
		{
			outputs = new ArrayList<>();
			outputDelays = new ArrayList<>();
		}
		else
		{
			outputs = null;
			outputDelays = null;
		}
		if(type != TimingNodeType.StartNode)
		{
			inputs = new ArrayList<>();
			
		}
		else
		{
			inputs = null;
		}
	}
	
	public double getTarrival()
	{
		return Tarrival;
	}
	
	public void setTarrival(double Tarrival)
	{
		this.Tarrival = Tarrival;
	}
	
	public double getTrequired()
	{
		return Trequired;
	}
	
	public void setTrequired(double Trequired)
	{
		this.Trequired = Trequired;
	}
	
	public void addOutput(TimingNode outputNode, double outputDelay)
	{
		if(type != TimingNodeType.EndNode)
		{
			outputs.add(outputNode);
			outputDelays.add(outputDelay);
		}
	}
	
	public void addInput(TimingNode inputNode)
	{
		if(type != TimingNodeType.StartNode)
		{
			inputs.add(inputNode);
		}
	}
	
	public Pin getPin()
	{
		return pin;
	}
	
	public List<TimingNode> getOutputs()
	{
		return outputs;
	}
	
}
