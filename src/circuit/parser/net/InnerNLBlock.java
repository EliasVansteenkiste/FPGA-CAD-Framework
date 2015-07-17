package circuit.parser.net;

import java.util.ArrayList;

public class InnerNLBlock extends NLBlock
{

	private ArrayList<NLBlockInput> inputs;
	private ArrayList<NLBlockOutput> outputs;
	
	public InnerNLBlock(String name, String instance)
	{
		super(name, instance);
		this.inputs = new ArrayList<>();
		this.outputs = new ArrayList<>();
	}
	
}
