package circuit;

import architecture.Site;

/*
 * For the moment a CLB can only contain one BLE
 * Needs to be expanded in the future
 */
public class Clb extends Block {
	public Pin[] output;
	public Pin[] input;
	public Pin clock;
	private Ble[] bles;
	
	public Clb(String name, int nro, int nri, int nrBle) {
		super(name, BlockType.CLB);
		this.bles = new Ble[nrBle];
		
		output = new Pin[nro];
		for (int i=0; i<nro; i++) {
			output[i]=new Pin(name+"_CLBout_"+i, PinType.SOURCE, this);
		}
		
		input  = new Pin[nri];
		for (int i=0; i<nri; i++) {
			input[i]=new Pin(name+"_CLBin_"+i, PinType.SINK, this);
		}
		
		clock=new Pin(name+"_clock", PinType.SINK, this);
	}
	
	@Override
	public int maxNets() {
		return this.input.length + this.output.length;
	}
	
	public Ble getBle(int index)
	{
		return this.bles[index];
	}
	
	public Ble getBle()
	{
		return this.bles[0];
	}
	
	public boolean addBle(Ble ble)
	{
		boolean success = false;
		for(int i = 0; i < bles.length; i++)
		{
			if(bles[i] == null)
			{
				bles[i] = ble;
				success = true;
				break;
			}
		}
		return success;
	}
	
	@Override
	public void setSite(Site site) //Pushes site through to embedded BLE
	{
		super.setSite(site);
		for(int i = 0; i < bles.length; i++)
		{
			if(bles[i] != null)
			{
				bles[i].setSite(site);
			}
		}
		
	}
		
}
