package flexible_architecture.net;

import java.util.ArrayList;
import java.util.List;

import flexible_architecture.block.AbstractBlock;

public class Net extends AbstractNet {

	private List<AbstractBlock> sinks;
	
	public Net(AbstractBlock source) {
		super(source);
		this.sinks = new ArrayList<AbstractBlock>();
	}
	
	public Net(DirectNet net) {
		super(net.getSource());
		this.addSink(net.getSink());
	}
	
	public List<AbstractBlock> getSinks() {
		return this.sinks;
	}
	public void addSink(AbstractBlock sink) {
		this.sinks.add(sink);
	}
}
