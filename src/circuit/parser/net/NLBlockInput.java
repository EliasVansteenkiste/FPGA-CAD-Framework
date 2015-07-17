package circuit.parser.net;

public class NLBlockInput extends NLBlockIO
{

	private NLBlockOutput connectedTo;
	
	public NLBlockInput(InnerNLBlock owner, NLBlockOutput connectedTo)
	{
		super(owner);
		this.connectedTo = connectedTo;
	}
	
}
