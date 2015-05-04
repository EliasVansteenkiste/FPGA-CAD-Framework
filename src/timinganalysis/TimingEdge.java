package timinganalysis;

public class TimingEdge
{

	private TimingNode input;
	private TimingNode output;
	private double delay;
	private double slack;
	private double criticality;
	private double criticalityWithExponent;
	private double possibleNewDelay;
	
	public TimingEdge(TimingNode input, TimingNode output, double delay)
	{
		this.input = input;
		this.output = output;
		this.delay = delay;
		this.slack = -1.0;
		this.criticality = -1.0;
		this.criticalityWithExponent = -1.0;
	}
	
	public double getDelay()
	{
		return delay;
	}
	
	public void setDelay(double delay)
	{
		this.delay = delay;
	}
	
	public TimingNode getInput()
	{
		return input;
	}
	
	public TimingNode getOutput()
	{
		return output;
	}
	
	public void recalculateSlackCriticality(double maxDelay, double criticalityExponent)
	{
		this.slack = output.getTRequired() - input.getTArrival() - this.delay;
		this.criticality = 1 - slack/maxDelay;
		this.criticalityWithExponent = Math.pow(criticality, criticalityExponent);
	}
	
	public double calculateDeltaCost(double newDelay)
	{
		possibleNewDelay = newDelay;
		return (newDelay - delay)*criticalityWithExponent;
	}
	
	public void revert()
	{
		//Do nothing
	}
	
	public void pushThrough()
	{
		this.delay = possibleNewDelay;
	}
	
	public double getCost()
	{
		return criticalityWithExponent*delay;
	}
	
	public double getSlack()
	{
		return this.slack;
	}
	
	public double getCriticality()
	{
		return this.criticality;
	}
	
	public double getCriticalityWithExponent()
	{
		return criticalityWithExponent;
	}
	
}