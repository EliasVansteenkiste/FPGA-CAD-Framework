package placers;

import architecture.Site;

public class Swap {
	public Site pl1;
	public Site pl2;
	
	public String toString(){
		return "Site 1: "+pl1+"- clb: "+pl1.block+", Site 2: "+pl2+"- clb: "+pl2.block;
	}
}
