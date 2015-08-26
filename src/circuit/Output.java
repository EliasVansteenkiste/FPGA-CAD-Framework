package circuit;

public class Output extends Block {
	public Pin input;

	public Output(String name) {
		super(name, BlockType.OUTPUT);
		input = new Pin(name+"_OUTPUTin", PinType.SINK,this);
	}
	
	@Override
	public int maxNets() {
		return 1;
	}
}
