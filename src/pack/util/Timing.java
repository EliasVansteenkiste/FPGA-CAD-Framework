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
	public void stop(){
		this.endTime = System.nanoTime();
		this.totalTime += (this.endTime - this.startTime);
		
		this.startTime = 0;
		this.endTime = 0;
	}
	public static double currentTime(long startTime){
		return Util.round(1.0*(System.nanoTime()-startTime)*Math.pow(10, -9),3);
	}
	public String toString(){
		double time = this.totalTime * Math.pow(10, -9);
		if(time > 1){
			return String.format("%.0f s\n", time);
		}

		time *= 1000;//ms

		if(time > 1){
			return String.format("%.0f ms\n", time);
		}
    	
		time *= 1000;//ns

		return String.format("%.0f ns\n", time);
	}
	public double time(){
		return Util.round(1.0*this.totalTime*Math.pow(10, -9),3);
	}
}
