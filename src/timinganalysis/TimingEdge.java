package timinganalysis;

public class TimingEdge
{

	private TimingNode input;
	private TimingNode output;
	private double delay;
	private double slack;
	
	public TimingEdge(TimingNode input, TimingNode output, double delay)
	{
		this.input = input;
		this.output = output;
		this.delay = delay;
		recalculateSlack();
	}
	
	public double getDelay()
	{
		return delay;
	}
	
	public void setDelay(double delay)
	{
		this.delay = delay;
		recalculateSlack();
	}
	
	public TimingNode getInput()
	{
		return input;
	}
	
	public TimingNode getOutput()
	{
		return output;
	}
	
	public void recalculateSlack()
	{
		this.slack = output.getTRequired() - input.getTArrival() - this.delay;
	}
	
	public double getSlack()
	{
		return this.slack;
	}
	
}