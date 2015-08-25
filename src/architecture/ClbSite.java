package architecture;

import circuit.Clb;

public class ClbSite extends Site
{

	private Clb clb;
	
	public ClbSite(int x, int y)
	{
		super(x, y, SiteType.CLB);
		clb = null;
	}
	
	public void setClb(Clb clb)
	{
		this.clb = clb;
	}
	
	public Clb getClb()
	{
		return clb;
	}

}
