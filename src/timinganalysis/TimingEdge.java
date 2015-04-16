package timinganalysis;

public class TimingEdge
{

	private TimingNode input;
	private TimingNode output;
	private double delay;
	
	public TimingEdge(TimingNode input, TimingNode output, double delay)
	{
		this.input = input;
		this.output = output;
		this.delay = delay;
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
	
}