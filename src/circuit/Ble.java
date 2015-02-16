package circuit;


/*
 * A Ble contains one Lut and one FF
 */
public class Ble extends Block
{
	
	private Pin output;
	private Pin inputs[];
	private int nbInputs;
	private Lut lut;
	private Flipflop flipflop;
	private boolean isFFUsed; //false if output is LUT output, true if output is FF output
	
	public Ble(String name, int nbInputs, Flipflop flipflop, Lut lut, boolean isFFUsed)
	{
		super(name, BlockType.BLE);
		this.isFFUsed = isFFUsed;
		output = new Pin(name + "_out", PinType.SOURCE, this);
		inputs = new Pin[nbInputs];
		for(int i = 0; i < nbInputs; i++)
		{
			inputs[i] = new Pin(name + "_in_" + i, PinType.SINK, this);
		}
		this.nbInputs = nbInputs;
		this.flipflop = flipflop;
		this.lut = lut;
	}

	public Pin getOutput() {
		return output;
	}

	public Pin[] getInputs() {
		return inputs;
	}

	public boolean isFFUsed() {
		return isFFUsed;
	}

	public Lut getLut() {
		return lut;
	}

	public Flipflop getFlipflop() {
		return flipflop;
	}
	
	public int getNbInputs()
	{
		return this.nbInputs;
	}
	
}
