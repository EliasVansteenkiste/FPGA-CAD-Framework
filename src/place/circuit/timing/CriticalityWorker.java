package place.circuit.timing;

import java.util.List;

public class CriticalityWorker implements Runnable{
	private final String name;
	private Thread thread;
	
    private volatile boolean running;
    private volatile boolean paused;
    private final Object pauseLock;
	
	public volatile boolean isRunning;
	
	private double[] criticalityLookupTable;
	private List<TimingEdge> timingEdges;
	private double maxDelay;
	    
	CriticalityWorker(
			double[] criticalityLookupTable,
			List<TimingEdge> timingEdges) {

	    this.name = "CriticalityWorker";
	    
	    this.criticalityLookupTable = criticalityLookupTable;
	    this.timingEdges = timingEdges;
	    
        this.running = true;
        this.paused = true;
        this.pauseLock = new Object();
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
        while (running){
            synchronized (pauseLock) {
                if (!running) { // may have changed while waiting to
                                // synchronize on pauseLock
                    break;
                }
                if (paused) {
                    try {
                        pauseLock.wait(); // will cause this Thread to block until 
                                          // another thread calls pauseLock.notifyAll()
                                          // Note that calling wait() will 
                                          // relinquish the synchronized lock that this 
                                          // thread holds on pauseLock so another thread
                                          // can acquire the lock to call notifyAll()
                                          // (link with explanation below this code)
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!running) { // running might have changed since we paused
                        break;
                    }
                }
            }
            this.doWork();
        }
	}
    public void stop() {
        running = false;
        // you might also want to interrupt() the Thread that is 
        // running this Runnable, too, or perhaps call:
        resume();
        // to unblock
    }

    public void pause() {
        // you may want to throw an IllegalStateException if !running
        this.paused = true;
    }
    public boolean paused() {
    	return this.paused;
    }

    public void resume() {
        synchronized (pauseLock) {
            this.paused = false;
            this.pauseLock.notifyAll(); // Unblocks thread
        }
    }
	private void doWork() {
        for(TimingEdge edge:this.timingEdges){
        	double slack = edge.getSink().getRequiredTime() - edge.getSource().getArrivalTime() - edge.getTotalDelay();
        	edge.setSlack(slack);

            double val = (1 - (this.maxDelay + edge.getSlack()) / this.maxDelay) * 20;
            int i = Math.min(19, (int) val);
            double linearInterpolation = val - i;

            edge.setCriticality(
                    (1 - linearInterpolation) * this.criticalityLookupTable[i]
                    + linearInterpolation * this.criticalityLookupTable[i+1]);
        }
		this.pause();
	}
	
	
	public void setDelay(double delay) {
		this.maxDelay = delay;
	}
}
