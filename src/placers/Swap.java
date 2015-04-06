package placers;

import circuit.Block;
import architecture.Site;

public class Swap {
	public Site pl1;
	public Site pl2;
	
	public void apply()
	{
		if (pl1.block!=null) pl1.block.setSite(pl2);
		if (pl2.block!=null) pl2.block.setSite(pl1);
		Block temp1=pl1.block;
		pl1.block=pl2.block;
		pl2.block=temp1;
	}
	
	public String toString()
	{
		return "Site 1: "+pl1+"- clb: "+pl1.block+", Site 2: "+pl2+"- clb: "+pl2.block;
	}
}
