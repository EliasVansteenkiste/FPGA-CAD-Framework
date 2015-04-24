package circuit;

import java.util.HashMap;
import java.util.Map;

public class Circuit 
{

	public Map<String,Output> outputs;
	public Map<String,Input> inputs;
	public Map<String,Net>	nets;
	
	private String name;
	
	public Circuit()
	{
		this.outputs = new HashMap<String,Output>();
		this.inputs = new HashMap<String,Input>();
		this.nets = new HashMap<String,Net>();
	}
	
	public Circuit(String name)
	{
		this.outputs = new HashMap<String,Output>();
		this.inputs = new HashMap<String,Input>();
		this.nets = new HashMap<String,Net>();
		this.name = name;
	}
	
	public Circuit(Map<String,Output> outputs, Map<String,Input> inputs)
	{
		this.outputs = outputs;
		this.inputs = inputs;
		this.nets = new HashMap<String,Net>();
	}
	
	public void addOutput(Output output)
	{
		this.outputs.put(output.name, output);
	}
	
	public void addInput(Input input)
	{
		this.inputs.put(input.name, input);
	}
	
	public void addNet(Net net)
	{
		this.nets.put(net.name, net);
	}
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public Map<String, Output> getOutputs() 
	{
		return outputs;
	}

	public Map<String, Input> getInputs() 
	{
		return inputs;
	}
	
	public Map<String, Net> getNets() 
	{
		return nets;
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}
	
}
