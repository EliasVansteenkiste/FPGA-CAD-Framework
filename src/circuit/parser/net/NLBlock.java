package circuit.parser.net;

import java.util.ArrayList;

public class NLBlock
{

	private String name;
	private String instance;
	private ArrayList<InnerNLBlock> innerBlocks;
	
	public NLBlock(String name, String instance)
	{
		this.name = name;
		this.instance = instance;
		this.innerBlocks = new ArrayList<>();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getInstance()
	{
		return instance;
	}
	
	public void addInnerBlock(String name, String instance)
	{
		innerBlocks.add(new InnerNLBlock(name, instance));
	}
	
}
