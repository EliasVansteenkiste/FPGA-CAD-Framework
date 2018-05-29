package place.circuit.timing;

import java.util.List;

public class RequiredTimeWorker implements Runnable{
	private final String name;
	private Thread thread;
	
	public volatile boolean isRunning;
	
	private final List<TimingNode> leafNodes;
	private final TimingNode[] timingNodes;
	    
	RequiredTimeWorker(
		List<TimingNode> leafNodes,
		TimingNode[] timingNodes) {

	    this.name = "RequiredTimeWorker";
	    
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
    	for(TimingNode leafNode: this.leafNodes) {
    		leafNode.setRequiredTime(0.0);
    	}
		for(TimingNode timingNode: this.timingNodes){
			timingNode.calculateRequiredTime();
		}
		this.isRunning = false;
	}
}
