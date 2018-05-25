package place.placers.analytical;

import java.util.List;

import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;

class WorkerThread implements Runnable{
	private final String name;
	private Thread thread;
	
    private final double[] coordinatesX, coordinatesY;
    private final DimensionForceGradient horizontalForce, verticalForce;
    
    private Net[] nets;
    private CritConn[] crits;

    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
    
    public long totalWorkTime;
    
    WorkerThread(
    		String name,
            double[] coordinatesX,
            double[] coordinatesY,
            double maxConnectionLength) {

    	this.name = name;
    	
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.horizontalForce = new DimensionForceGradient(this.coordinatesX.length, maxConnectionLength);
        this.verticalForce = new DimensionForceGradient(this.coordinatesY.length, maxConnectionLength);
        
        this.totalWorkTime = 0;
        
        this.start();
    }
    
    public void reset() {
    	this.nets = null;
    	this.crits = null;
    }
    
    public void start () {
    	if (this.thread == null) {
    		this.thread = new Thread(this, this.name);
    		this.thread.start();
    	}
    }
    
    public void setNets(List<Net> tempNets){
        this.nets = new Net[tempNets.size()];
        tempNets.toArray(this.nets);
    }
    public void setNets(Net[] tempNets){
        this.nets = tempNets;
    }
    public void setCriticalConnections(List<CritConn> tempCritConn) {
        this.crits = new CritConn[tempCritConn.size()];
        tempCritConn.toArray(this.crits);
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
        paused = true;
    }
    public boolean paused() {
    	return this.paused;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }
    
	private void doWork() {
		long start = System.nanoTime();
		this.initializeIteration();
    	
		if(this.nets != null) {
        	for(Net net: this.nets) {
        		this.processNet(net);
        	}
    	}
    	if(this.crits != null) {
        	for(CritConn critConn:this.crits) {
            	this.processConnection(critConn.sourceIndex, critConn.sinkIndex, critConn.sinkOffset - critConn.sourceOffset, critConn.weight, true);
            }
    	}
    	
    	long stop = System.nanoTime();
    	this.totalWorkTime += stop - start;
    	
    	this.pause();
	}
	
    private void initializeIteration() {
        this.horizontalForce.initializeIteration();
        this.verticalForce.initializeIteration();
    }

    private void processNet(Net net) {
        if(net.numBlocks == 2) {
            this.processSmallNet(net);
        } else {
        	this.processBigNet(net);
        }
    }
    private void processSmallNet(Net net) {
    	// Nets with 2 blocks are common and can be processed very quick
    	int blockIndex0 = net.index0;
    	int blockIndex1 = net.index1;

    	double coordinate1, coordinate2;
            
    	coordinate1 = this.coordinatesY[blockIndex0] + net.offset0;
    	coordinate2 = this.coordinatesY[blockIndex1] + net.offset1;
    	this.verticalForce.processConnection(blockIndex0, blockIndex1, coordinate2 - coordinate1, net.weight, false);

    	coordinate1 = this.coordinatesX[blockIndex0];
    	coordinate2 = this.coordinatesX[blockIndex1];
    	this.horizontalForce.processConnection(blockIndex0, blockIndex1, coordinate2 - coordinate1, net.weight, false);
    }
    private void processBigNet(Net net) {
    	// For bigger nets, we have to find the min and max block
        int minXIndex = net.netBlockIndexes[0];
        int maxXIndex = net.netBlockIndexes[0];
        int minYIndex = net.netBlockIndexes[0];
        int maxYIndex = net.netBlockIndexes[0];

        double minX = this.coordinatesX[minXIndex];
        double maxX = this.coordinatesX[maxXIndex];
        double minY = this.coordinatesY[minYIndex] + net.netBlockOffsets[0];
        double maxY = this.coordinatesY[maxYIndex] + net.netBlockOffsets[0];

        for(int i = 0; i < net.numBlocks; i++) {
            int blockIndex = net.netBlockIndexes[i];
            double x = this.coordinatesX[blockIndex];
            double y = this.coordinatesY[blockIndex] + net.netBlockOffsets[i];

            if(x < minX) {
                minX = x;
                minXIndex = blockIndex;
            } else if(x > maxX) {
                maxX = x;
                maxXIndex = blockIndex;
            }

            if(y < minY) {
                minY = y;
                minYIndex = blockIndex;
            } else if(y > maxY) {
                maxY = y;
                maxYIndex = blockIndex;
            }
        }

        // Add connections between the min and max block
        this.horizontalForce.processConnection(minXIndex, maxXIndex, maxX - minX, net.weight, false);
        this.verticalForce.processConnection(minYIndex, maxYIndex, maxY - minY, net.weight, false);
    }

    void processConnection(int blockIndex1, int blockIndex2, float offset, float weight, boolean critical) {
        double x1 = this.coordinatesX[blockIndex1];
        double x2 = this.coordinatesX[blockIndex2];
        double y1 = this.coordinatesY[blockIndex1];
        double y2 = this.coordinatesY[blockIndex2];

        this.horizontalForce.processConnection(blockIndex1, blockIndex2, x2 - x1, weight, critical);
        this.verticalForce.processConnection(blockIndex1, blockIndex2, y2 - y1 + offset, weight, critical);
    }
    
    public DimensionForceGradient getHorizontalForces() {
    	return this.horizontalForce;
    }
    public DimensionForceGradient getVerticalForces() {
    	return this.verticalForce;
    }
    
    public String getName() {
    	return this.name;
    }
}