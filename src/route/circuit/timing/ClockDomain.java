package route.circuit.timing;

import java.util.List;

public class ClockDomain {
	public final int sourceClockDomain;
	public final int sinkClockDomain;
	
	public final List<TimingNode> clockDomainRootNodes;
	public final List<TimingNode> clockDomainLeafNodes;
	
	public boolean hasPaths;
	private double maxDelay = -1;
	
	public ClockDomain(int sourceClockDomain, int sinkClockDomain, List<TimingNode> clockDomainRootNodes, List<TimingNode> clockDomainLeafNodes) {
		this.sourceClockDomain = sourceClockDomain;
		this.sinkClockDomain = sinkClockDomain;
		this.maxDelay = 0;
		
		this.clockDomainRootNodes = clockDomainRootNodes;
		this.clockDomainLeafNodes = clockDomainLeafNodes;
		this.hasPaths();
	}
	
	public void setMaxDelay(double delay) {
		this.maxDelay = delay;
	}
	public double getMaxDelay() {
		return this.maxDelay;
	}

	public boolean includeDomain() {
		//return true;
		return this.hasPaths && (this.sourceClockDomain == 0 || this.sinkClockDomain == 0 || this.sourceClockDomain == this.sinkClockDomain);
	}
	
	@Override
	public String toString() {
		String result = "";
		if(this.sourceClockDomain != this.sinkClockDomain) result += "   ";
		result += this.sourceClockDomain + " => " + this.sinkClockDomain + ": " + (this.maxDelay == -1 ? "---" : String.format("%.3e", this.maxDelay));
		return result;
	}
	
	private void hasPaths() {
		boolean hasPathsToSource = false;
		boolean hasPathsToSink = false;
		
		for (TimingNode leafNode : this.clockDomainLeafNodes) {
			if (leafNode.hasClockDomainAsSource(this.sourceClockDomain)) {
				hasPathsToSource = true;
			}
		}
		
		for (TimingNode rootNode : this.clockDomainRootNodes) {
			if (rootNode.hasClockDomainAsSink(this.sinkClockDomain)) {
				hasPathsToSink = true;
			}
		}
		
		if(hasPathsToSource != hasPathsToSink) System.err.println("Num paths from leaf to source is not equal to num paths form sink to source");
		this.hasPaths = hasPathsToSource;
	}
}
