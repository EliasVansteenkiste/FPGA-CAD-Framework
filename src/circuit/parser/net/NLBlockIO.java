package circuit.parser.net;

public class NLBlockIO
{
	
	private InnerNLBlock owner;
	
	public NLBlockIO(InnerNLBlock owner)
	{
		this.owner = owner;
	}
	
	public InnerNLBlock getOwner()
	{
		return owner;
	}
	
}
