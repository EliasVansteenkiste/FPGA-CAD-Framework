package circuit.parser.net;

public class NLBlockOutput extends NLBlockIO
{
	
	private NLBlockOutput connectedTo;
	
	public NLBlockOutput(InnerNLBlock owner, NLBlockOutput connectedTo)
	{
		super(owner);
		this.connectedTo = connectedTo;
	}
	
}
