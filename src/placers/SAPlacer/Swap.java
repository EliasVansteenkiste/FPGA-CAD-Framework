package placers.SAPlacer;

import java.util.Random;

import flexible_architecture.block.GlobalBlock;
import flexible_architecture.site.AbstractSite;

public class Swap {
	private GlobalBlock block1;
	private GlobalBlock block2;
	private AbstractSite site1;
	private AbstractSite site2;
	
	public Swap(GlobalBlock block, AbstractSite site, Random random) {
		this.block1 = block;
		this.site1 = block.getSite();
		
		this.block2 = site.getRandomBlock(random);
		this.site2 = site;
	}
	
	
	public GlobalBlock getBlock1() {
		return this.block1;
	}
	public GlobalBlock getBlock2() {
		return this.block2;
	}
	
	
	public void apply() {
		this.site1.removeBlock(this.block1);
		
		if(this.block2 != null) {
			this.site2.removeBlock(this.block2);
			
			this.site1.addBlock(this.block2);
			this.block2.setSite(this.site1);
		}
		
		this.site2.addBlock(this.block1);
		this.block1.setSite(this.site2);
	}
	
	
	public String toString() {
		return "block " + this.block1 + " on site " + this.site1
				+ ", block" + this.block2 + " on site " + this.site2;
	}
}
