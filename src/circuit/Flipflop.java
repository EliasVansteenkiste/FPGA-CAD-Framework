package circuit;


public class Flipflop extends Block
{

	private Pin output;
	private Pin input;
	private Pin clock;
	private Type flipflopType;
	private InitVal initVal;
	
	public enum Type {FALLING_EDGE, RISING_EDGE, ACTIVE_HIGH, ACTIVE_LOW, ASYNCHRONOUS, UNSPECIFIED}
	public enum InitVal {ZERO, ONE, DONT_CARE, UNKNOWN}
	
	public Flipflop(String name, Type flipflopType, InitVal initVal)
	{
		super(name, BlockType.FLIPFLOP);
		this.flipflopType = flipflopType;
		this.initVal = initVal;
		output = new Pin(name + "_FFout", PinType.SOURCE, this);
		input = new Pin(name + "_FFin", PinType.SINK, this);
		clock = new Pin(name + "_clock", PinType.SINK, this);
	}
	
	@Override
	public int maxNets() {
		return 2;
	}

	public Pin getOutput() {
		return output;
	}

	public Pin getInput() {
		return input;
	}

	public Pin getClock() {
		return clock;
	}

	public Type getFlipflopType() {
		return flipflopType;
	}

	public InitVal getInitVal() {
		return initVal;
	}
	
}
