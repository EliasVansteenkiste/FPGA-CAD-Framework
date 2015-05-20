package circuit;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Circuit 
{

	public Map<String,Output> outputs;
	public Map<String,Input> inputs;
	public Map<String,Net>	nets;
	protected Vector<Vector<HardBlock>> hardBlocks;
	
	private String name;
	
	public Circuit()
	{
		this.outputs = new HashMap<String,Output>();
		this.inputs = new HashMap<String,Input>();
		this.nets = new HashMap<String,Net>();
		this.hardBlocks = new Vector<>();
	}
	
	public Circuit(String name)
	{
		this.outputs = new HashMap<String,Output>();
		this.inputs = new HashMap<String,Input>();
		this.nets = new HashMap<String,Net>();
		this.hardBlocks = new Vector<>();
		this.name = name;
	}
	
	public Circuit(Map<String,Output> outputs, Map<String,Input> inputs, Vector<Vector<HardBlock>> hardBlocks)
	{
		this.outputs = outputs;
		this.inputs = inputs;
		this.hardBlocks = hardBlocks;
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
	
	public Vector<Vector<HardBlock>> getHardBlocks()
	{
		return hardBlocks;
	}
	
	public void addHardBlock(HardBlock blockToAdd)
	{
		int typeIndex = -1;
		int counter = 0;
		for(Vector<HardBlock> vector: hardBlocks)
		{
			if(blockToAdd.getTypeName().contains(vector.get(0).getTypeName()))
			{
				typeIndex = counter;
				break;
			}
			counter++;
		}
		if(typeIndex != -1)
		{
			hardBlocks.get(typeIndex).add(blockToAdd);
		}
		else
		{
			Vector <HardBlock> newVector = new Vector<HardBlock>();
			newVector.add(blockToAdd);
			hardBlocks.add(newVector);
		}
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}
	
}
