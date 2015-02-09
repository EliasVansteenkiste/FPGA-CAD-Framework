package circuit;

public class Output extends Block {
	public Pin input;

	public Output(String name) {
		super(name, BlockType.OUTPUT);
		input = new Pin(name+"_in", PinType.SINK,this);
	}
}
