package place.placers.analytical;

import java.util.List;

import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;

class NetAndCritWorker implements Runnable{
	private final String name;
	private Thread thread;
	
    private volatile double[] coordinatesX, coordinatesY;
    
    private int[] netStarts;
    private int[] netEnds;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;
    
    private volatile DimensionForceGradient horizontalForce, verticalForce;
    
    private int[] nets;
    private CritConn[] crits;

    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
    
    NetAndCritWorker(
    		String name,
            double[] coordinatesX,
            double[] coordinatesY,
            int[] netStarts,
            int[] netEnds,
            int[] netBlockIndexes,
            float[] netBlockOffsets,
            double maxConnectionLength) {

    	this.name = name;
    	
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;
        
        this.netStarts = netStarts;
        this.netEnds = netEnds;
        this.netBlockIndexes = netBlockIndexes;
        this.netBlockOffsets = netBlockOffsets;

        this.horizontalForce = new DimensionForceGradient(this.coordinatesX.length, maxConnectionLength);
        this.verticalForce = new DimensionForceGradient(this.coordinatesY.length, maxConnectionLength);
        
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
    
    public void setNets(List<Integer> tempNets){
        this.nets = new int[tempNets.size()];
        for(int i = 0; i < tempNets.size(); i++){
        	this.nets[i] = tempNets.get(i);
        }
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
		this.initializeIteration();
    	
		if(this.nets != null) {
        	for(int net: this.nets) {
        		this.processNet(this.netStarts[net], this.netEnds[net]);
        	}
    	}
    	if(this.crits != null) {
        	for(CritConn critConn:this.crits) {
            	this.processConnection(critConn.sourceIndex, critConn.sinkIndex, critConn.sinkOffset - critConn.sourceOffset, critConn.weight, true);
            }
    	}
    	
    	this.pause();
	}
	
    private void initializeIteration() {
        this.horizontalForce.initializeIteration();
        this.verticalForce.initializeIteration();
    }

    private void processNet(int netStart, int netEnd) {
    	int numBlocks = netEnd - netStart;
        if(numBlocks == 2) {
            this.processSmallNet(netStart, netEnd);
        } else {
        	this.processBigNet(netStart, netEnd);
        }
    }
    private void processSmallNet(int netStart, int netEnd) {
    	int blockIndex1 = this.netBlockIndexes[netStart];
    	int blockIndex2 = this.netBlockIndexes[netStart + 1];

    	double coordinate1, coordinate2;
            
    	coordinate1 = this.coordinatesY[blockIndex1] + this.netBlockOffsets[netStart];
    	coordinate2 = this.coordinatesY[blockIndex2] + this.netBlockOffsets[netStart + 1];
    	this.verticalForce.processConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, 1, false);

    	coordinate1 = this.coordinatesX[blockIndex1];
    	coordinate2 = this.coordinatesX[blockIndex2];
    	this.horizontalForce.processConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, 1, false);
    }
    private void processBigNet(int netStart, int netEnd) {
        // For bigger nets, we have to find the min and max block
        
    	int numNetBlocks = netEnd - netStart;
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks);
        
    	int minXIndex = this.netBlockIndexes[netStart],
            maxXIndex = this.netBlockIndexes[netStart],
            minYIndex = this.netBlockIndexes[netStart],
            maxYIndex = this.netBlockIndexes[netStart];

        double minX = this.coordinatesX[minXIndex],
               maxX = this.coordinatesX[maxXIndex],
               minY = this.coordinatesY[minYIndex] + this.netBlockOffsets[netStart],
               maxY = this.coordinatesY[maxYIndex] + this.netBlockOffsets[netStart];

        for(int i = netStart + 1; i < netEnd; i++) {
            int blockIndex = this.netBlockIndexes[i];
            double x = this.coordinatesX[blockIndex],
                   y = this.coordinatesY[blockIndex] + this.netBlockOffsets[i];

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
        this.horizontalForce.processConnection(minXIndex, maxXIndex, maxX - minX, weight, false);
        this.verticalForce.processConnection(minYIndex, maxYIndex, maxY - minY, weight, false);
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