package timing_graph;

import architecture.circuit.block.GlobalBlock;

public class TimingGraphEntry {
	private GlobalBlock source;
	private GlobalBlock sink;
	private double criticality;
	private int netSize;
	
	TimingGraphEntry(GlobalBlock source, GlobalBlock sink, double criticality, int netSize) {
		this.source = source;
		this.sink = sink;
		this.criticality = criticality;
		this.netSize = netSize;
	}
	
	public GlobalBlock getSource() {
		return this.source;
	}
	public GlobalBlock getSink() {
		return this.sink;
	}
	public double getCriticality() {
		return this.criticality;
	}
	public int getNetSize() {
		return this.netSize;
	}
}
