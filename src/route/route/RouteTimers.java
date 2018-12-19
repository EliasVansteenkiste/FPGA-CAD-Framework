package route.route;

public class RouteTimers {
	public Timer firstIteration;
	public Timer updateTiming;
	public Timer rerouteCritical;
	public Timer rerouteCongestion;
	public Timer rerouteIllegal;
	public Timer rerouteOpin;
	public Timer fixOpin;
	public Timer setRerouteCriticality;
	public Timer calculateStatistics;
	public Timer updateCost;
	
	public RouteTimers() {
		this.firstIteration = new Timer("first iteration");
		this.updateTiming = new Timer("update timing");
		this.rerouteCritical = new Timer("reroute critical");
		this.rerouteCongestion = new Timer("reroute congestion");
		this.rerouteIllegal = new Timer("reroute illegal");
		this.rerouteOpin = new Timer("reroute opin");
		this.fixOpin = new Timer("fix opin");
		this.setRerouteCriticality = new Timer("set reroute crit");
		this.calculateStatistics = new Timer("calc stat");
		this.updateCost = new Timer("update cost");
	}
	
	@Override
	public String toString() {
		String result = "";
		
		result += this.firstIteration;
		result += this.rerouteCongestion;
		result += this.rerouteIllegal;
		result += this.rerouteOpin;
		result += this.rerouteCritical;
		result += this.fixOpin;
		result += this.setRerouteCriticality;
		result += this.calculateStatistics;
		result += this.updateTiming;
		result += this.updateCost;
		
		return result;
	}
}

class Timer {
	private String name;
	private long time;
	private long start;
	
	public Timer(String name) {
		this.name = name;
		this.time = 0;
	}
	
	public void start() {
		this.start = System.nanoTime();
	}
	public void finish() {
		this.time += System.nanoTime() - this.start;
	}
	
	public String toString() {
		return String.format("%-20s %7.2f s\n", this.name, this.time * 1e-9);
	}
}