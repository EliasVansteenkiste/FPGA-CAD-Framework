package placers.SAPlacer;

import flexible_architecture.block.GlobalBlock;
import flexible_architecture.site.AbstractSite;

public class Swap {
	private GlobalBlock block1;
	private GlobalBlock block2;
	
	public Swap(GlobalBlock block1, GlobalBlock block2) {
		this.block1 = block1;
		this.block2 = block2;
	}
	
	public GlobalBlock getBlock1() {
		return this.block1;
	}
	public GlobalBlock getBlock2() {
		return this.block2;
	}
	
	public void apply() {
		AbstractSite site1 = this.block1.getSite();
		site1.removeBlock(this.block1);
		
		AbstractSite site2 = this.block2.getSite();
		site2.removeBlock(this.block2);
		
		site1.addBlock(this.block2);
		this.block2.setSite(site1);
		
		site2.addBlock(this.block1);
		this.block1.setSite(site2);
	}
	
	
	public String toString() {
		return "block " + this.block1 + " on site " + this.block1.getSite()
				+ ", block" + this.block2 + " on site " + this.block2.getSite();
	}
}
