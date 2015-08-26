package circuit;

public class Input extends Block {
	public Pin output;

	public Input(String name) {
		super(name, BlockType.INPUT);
		output = new Pin(name+"_INPUTout", PinType.SOURCE,this);
	}
	
	@Override
	public int maxNets() {
		return 1;
	}
}
