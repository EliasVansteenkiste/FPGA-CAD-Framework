package timinganalysis;

import java.util.ArrayList;
import java.util.List;

import circuit.Pin;

public class TimingNode
{
	
	private Pin pin;
	private TimingNodeType type;
	private double tArrival;
	private double tRequired;
	private List<TimingEdge> inputs;
	private List<TimingEdge> outputs;
	
	public TimingNode(TimingNodeType type, Pin pin)
	{
		this.type = type;
		this.pin = pin;
		this.tArrival = 0.0;
		this.tRequired = 0.0;
		if(type != TimingNodeType.EndNode)
		{
			outputs = new ArrayList<>();
		}
		else
		{
			outputs = null;
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
	
	public double getTArrival()
	{
		return tArrival;
	}
	
	public void setTArrival(double tArrival)
	{
		this.tArrival = tArrival;
	}
	
	public double getTRequired()
	{
		return tRequired;
	}
	
	public void setTRequired(double tRequired)
	{
		this.tRequired = tRequired;
	}
	
	public void setTrequired(double tRequired)
	{
		this.tRequired = tRequired;
	}
	
	public void addOutput(TimingEdge outputEdge)
	{
		if(type != TimingNodeType.EndNode)
		{
			outputs.add(outputEdge);
		}
	}
	
	public void addInput(TimingEdge inputEdge)
	{
		if(type != TimingNodeType.StartNode)
		{
			inputs.add(inputEdge);
		}
	}
	
	public Pin getPin()
	{
		return pin;
	}
	
	public List<TimingEdge> getOutputs()
	{
		if(outputs == null)
		{
			return new ArrayList<>();
		}
		return outputs;
	}
	
	public List<TimingEdge> getInputs()
	{
		if(inputs == null)
		{
			return new ArrayList<>();
		}
		return inputs;
	}
	
}
