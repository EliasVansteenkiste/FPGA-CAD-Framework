package circuit;

public class Clb extends Block {
	public Pin[] output;
	int nro;
	public Pin[] input;
	int nri;
	public Pin clock;
	
	public Clb(String name, int nro, int nri) {
		super(name, BlockType.CLB);
		// TODO Auto-generated constructor stub
		this.nro = nro;
		this.nri = nri;
		
		output = new Pin[nro];
		for (int i=0; i<nro; i++) {
			output[i]=new Pin(name+"_out_"+i, PinType.SOURCE, this);
		}
		input  = new Pin[nri];
		for (int i=0; i<nri; i++) {
			input[i]=new Pin(name+"_in_"+i, PinType.SINK, this);
		}
		clock=new Pin(name+"_clock", PinType.SINK, this);

	}

		
}
