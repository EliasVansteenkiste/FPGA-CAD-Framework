package circuit.parser.net;

import java.util.ArrayList;

public class OuterNLBlock extends NLBlock
{
	
	private ArrayList<String> inputs;
	private ArrayList<String> outputs;
	
	public OuterNLBlock(String name, String instanceType)
	{
		super(name, instanceType);
		this.inputs = new ArrayList<>();
		this.outputs = new ArrayList<>();
	}
	
	public void addInput(String name)
	{
		inputs.add(name);
	}
	
	public void addOutput(String name)
	{
		outputs.add(name);
	}
	
}
