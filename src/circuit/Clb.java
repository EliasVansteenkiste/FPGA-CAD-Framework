package circuit;

import architecture.Site;

/*
 * For the moment a CLB can only contain one BLE
 * Needs to be expanded in the future
 */
public class Clb extends Block {
	public Pin[] output;
	int nro;
	public Pin[] input;
	int nri;
	public Pin clock;
	private Ble ble;
	
	public Clb(String name, int nro, int nri) {
		super(name, BlockType.CLB);
		this.nro = nro;
		this.nri = nri;
		this.ble = null;
		
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
	
	public Clb(String name, int nro, int nri, Ble ble)
	{
		this(name, nro, nri);
		this.ble = ble;
	}

	public Ble getBle()
	{
		return this.ble;
	}
	
	@Override
	public void setSite(Site site) //Pushes site through to embedded BLE
	{
		super.setSite(site);
		if(this.ble != null)
		{
			this.ble.setSite(site);
		}
	}
		
}
