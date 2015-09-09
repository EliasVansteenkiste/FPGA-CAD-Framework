package flexible_architecture.block;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.site.AbstractSite;
import flexible_architecture.site.Site;

public class GlobalBlock extends AbstractBlock {
	
	private AbstractSite site;
	
	public GlobalBlock(String name, BlockType type) {
		super(name, type);
	}
	
	public AbstractSite getSite() {
		return this.site;
	}
	public void setSite(AbstractSite site) {
		this.site = site;
	}
}
