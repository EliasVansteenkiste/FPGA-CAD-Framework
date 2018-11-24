package route.circuit.timing;

public class ClockDomain {
	public final int sourceClockDomain;
	public final int sinkClockDomain;
	private double maxDelay;
	
	public ClockDomain(int sourceClockDomain, int sinkClockDomain) {
		this.sourceClockDomain = sourceClockDomain;
		this.sinkClockDomain = sinkClockDomain;
		this.maxDelay = 0;
	}
	
	public void setMaxDelay(double delay) {
		this.maxDelay = delay;
	}
	public double getMaxDelay() {
		return this.maxDelay;
	}

	public boolean includeDomain() {
		return true;
	        //return
	        //        sourceClockDomain == this.virtualIoClockDomain
	        //        || sinkClockDomain == this.virtualIoClockDomain
	        //        || sourceClockDomain == sinkClockDomain
	}
	
	@Override
	public String toString() {
		return this.sourceClockDomain + " => " + this.sinkClockDomain + ": " + this.maxDelay;
	}
}
