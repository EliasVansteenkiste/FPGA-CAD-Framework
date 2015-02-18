package circuit;


public class Lut extends Block
{
	
	private Pin[] outputs;
	private Pin[] inputs;
	private int nbInputs;
	
	public Lut(String name, int nbOutputs, int nbInputs)
	{
		super(name, BlockType.LUT);
		this.nbInputs = nbInputs;
		
		outputs = new Pin[nbOutputs];
		for(int i = 0; i < nbOutputs; i++)
		{
			outputs[i] = new Pin(name + "_LUTout_" + i, PinType.SOURCE, this);
		}
		
		inputs = new Pin[nbInputs];
		for(int i = 0; i < nbInputs; i++)
		{
			inputs[i] = new Pin(name + "_LUTin_" + i, PinType.SINK, this);
		}
	}

	public Pin[] getOutputs() {
		return outputs;
	}

	public Pin[] getInputs() {
		return inputs;
	}
	
	public int getNbInputs()
	{
		return this.nbInputs;
	}
	
}
