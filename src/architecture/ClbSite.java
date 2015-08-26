package architecture;

import circuit.Block;
import circuit.Clb;

public class ClbSite extends Site
{

	private Clb clb;
	
	public ClbSite(int z, GridTile tyle)
	{
		super(z, tyle);
		clb = null;
	}
	
	@Override
	public void setBlock(Block clb)
	{
		this.clb = (Clb)clb;
	}
	
	@Override
	public Block getBlock()
	{
		return clb;
	}

}
