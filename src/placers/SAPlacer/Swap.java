package placers.SAPlacer;

import circuit.Block;
import architecture.Site;

public class Swap {
	public Site pl1;
	public Site pl2;
	
	public void apply()
	{
		if (pl1.getBlock() != null) pl1.getBlock().setSite(pl2);
		if (pl2.getBlock() != null) pl2.getBlock().setSite(pl1);
		Block temp1 = pl1.getBlock();
		pl1.setBlock(pl2.getBlock());
		pl2.setBlock(temp1);
	}
	
	public String toString()
	{
		return "Site 1: "+pl1+"- clb: "+pl1.getBlock() + ", Site 2: "+pl2+"- clb: "+pl2.getBlock();
	}
}
