package pack.util;

public class Timing {
	private long startTime;
	private long endTime;
	private long totalTime;
	
	public Timing(){
		this.startTime = 0;
		this.endTime = 0;
		this.totalTime = 0;
	}
	public void start(){
		this.startTime = System.nanoTime();
	}
	public void end(){
		this.endTime = System.nanoTime();
		this.totalTime += (endTime - startTime);
		
		this.startTime = 0;
		this.endTime = 0;
	}
	public static double currentTime(long startTime){
		return Util.round(1.0*(System.nanoTime()-startTime)*Math.pow(10, -9),3);
	}
	public String toString(){
		return "" + Util.round(1.0*this.totalTime*Math.pow(10, -9),3) + " s";
	}
	public double time(){
		return Util.round(1.0*this.totalTime*Math.pow(10, -9),3);
	}
}
