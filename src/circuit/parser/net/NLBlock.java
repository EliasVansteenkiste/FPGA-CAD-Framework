package circuit.parser.net;

import java.util.ArrayList;

public class NLBlock
{

	private String name;
	private String instanceType;
	private ArrayList<InnerNLBlock> innerBlocks;
	
	public NLBlock(String name, String instanceType)
	{
		this.name = name;
		this.instanceType = instanceType;
		this.innerBlocks = new ArrayList<>();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getInstance()
	{
		return instanceType;
	}
	
	public void addInnerBlock(String name, String instance)
	{
		innerBlocks.add(new InnerNLBlock(name, instance));
	}
	
}
