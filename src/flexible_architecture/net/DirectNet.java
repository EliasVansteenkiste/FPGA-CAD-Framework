package flexible_architecture.net;

import flexible_architecture.block.AbstractBlock;

public class DirectNet extends AbstractNet {

	private AbstractBlock sink;
	
	public DirectNet(AbstractBlock source) {
		super(source);
	}
	public DirectNet(AbstractBlock source, AbstractBlock sink) {
		super(source);
		this.sink = sink;
	}
	
	public AbstractBlock getSink() {
		return this.sink;
	}
	public void setSink(AbstractBlock sink) {
		this.sink = sink;
	}
}
