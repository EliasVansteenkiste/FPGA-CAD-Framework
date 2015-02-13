package prepackedcircuit;

import circuit.Block;
import circuit.BlockType;
import circuit.Pin;
import circuit.PinType;

public class Lut extends Block
{
	
	private Pin[] outputs;
	private Pin[] inputs;
	
	public Lut(String name, int nbOutputs, int nbInputs)
	{
		super(name, BlockType.LUT);
		
		outputs = new Pin[nbOutputs];
		for(int i = 0; i < nbOutputs; i++)
		{
			outputs[i] = new Pin(name + "_out_" + i, PinType.SOURCE, this);
		}
		
		inputs = new Pin[nbInputs];
		for(int i = 0; i < nbInputs; i++)
		{
			inputs[i] = new Pin(name + "_in_" + i, PinType.SINK, this);
		}
	}

	public Pin[] getOutputs() {
		return outputs;
	}

	public Pin[] getInputs() {
		return inputs;
	}
	
}
