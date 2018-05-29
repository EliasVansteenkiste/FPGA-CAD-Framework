package place.circuit.timing;

import java.util.List;

public class ArrivalTimeWorker implements Runnable{
	private final String name;
	private Thread thread;
	
	public volatile boolean isRunning;
	
	private final List<TimingNode> rootNodes;
	private final List<TimingNode> leafNodes;
	private final TimingNode[] timingNodes;
	
	public volatile double maxDelay;
	    
	ArrivalTimeWorker(
		List<TimingNode> rootNodes,
		List<TimingNode> leafNodes,
		TimingNode[] timingNodes) {

	    this.name = "ArrivalTimeWorker";
	    
	    this.rootNodes = rootNodes;
	    this.leafNodes = leafNodes;
	    this.timingNodes = timingNodes;
	    
	    this.isRunning = true;
	    this.start();
	}
	    
	public void start () {
		if (this.thread == null) {
    		this.thread = new Thread(this, this.name);
    		this.thread.start();
    	}
    }  

	@Override
	public void run() {
        for(TimingNode rootNode : this.rootNodes) {
        	rootNode.setArrivalTime(0.0);
        }
		for(TimingNode timingNode: this.timingNodes){
			timingNode.calculateArrivalTime();
		}
		this.maxDelay = 0.0;
		for(TimingNode leafNode: this.leafNodes){
        	if(leafNode.getArrivalTime() > maxDelay){
        		this.maxDelay = leafNode.getArrivalTime();
        	}
        }
		this.isRunning = false;
	}
	
	public double getMaxDelay(){
		return this.maxDelay;
	}
}
