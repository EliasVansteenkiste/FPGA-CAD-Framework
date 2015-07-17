package circuit.parser.net;

public class NLBlockInput extends NLBlockIO
{

	private NLBlockInput connectedTo;
	
	public NLBlockInput(InnerNLBlock owner, NLBlockInput connectedTo)
	{
		super(owner);
		this.connectedTo = connectedTo;
	}
	
}
