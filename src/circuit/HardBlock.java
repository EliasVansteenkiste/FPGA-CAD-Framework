package circuit;

public class HardBlock extends Block
{
	
	private Pin[] outputs;
	private Pin[] inputs;
	private boolean isClockEdge;
	private String typeName;
	
	public HardBlock(String name, int nbOutputs, int nbInputs, String typeName, boolean isClockEdge)
	{
		super(name, BlockType.HARDBLOCK);
		this.typeName = typeName;
		this.isClockEdge = isClockEdge;
		
		outputs = new Pin[nbOutputs];
		for(int i = 0; i < nbOutputs; i++)
		{
			outputs[i] = new Pin(name + "_HBout_" + i, PinType.SOURCE, this);
		}
		
		inputs = new Pin[nbInputs];
		for(int i = 0; i < nbInputs; i++)
		{
			inputs[i] = new Pin(name + "_HBin_" + i, PinType.SINK, this);
		}
	}
	
	public Pin[] getInputs()
	{
		return inputs;
	}
	
	public Pin[] getOutputs()
	{
		return outputs;
	}
	
	public String getTypeName()
	{
		return typeName;
	}
	
	public boolean getIsClockEdge()
	{
		return isClockEdge;
	}
	
}
