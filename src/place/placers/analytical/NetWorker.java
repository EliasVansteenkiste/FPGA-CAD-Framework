package place.placers.analytical;

import java.util.List;

import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;

class NetWorker implements Runnable{
	private final String name;
	private Thread thread;
	private volatile boolean finished;
	
    private final double[] coordinatesX, coordinatesY;
    private final DimensionForceGradient horizontalForce, verticalForce;
    
    private Net[] nets;
    private CritConn[] crits;
    
    public int netCounter;
    public int critCounter;
    
    NetWorker(
    		String name,
            double[] coordinatesX,
            double[] coordinatesY,
            double maxConnectionLength) {

    	this.name = name;
    	
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.horizontalForce = new DimensionForceGradient(this.coordinatesX.length, maxConnectionLength);
        this.verticalForce = new DimensionForceGradient(this.coordinatesY.length, maxConnectionLength);
        
        this.start();
        
        this.finished = true;
        
        this.netCounter = 0;
        this.critCounter = 0;
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
    synchronized void resume() {
    	this.finished = false;
    	notify();
    }
    public boolean isFinished() {
    	return this.finished;
    }
    
    public synchronized void setNets(List<Net> tempNets){
        this.nets = new Net[tempNets.size()];
        tempNets.toArray(this.nets);
    }
    public synchronized void setNets(Net[] tempNets){
        this.nets = tempNets;
    }
    public synchronized void setCriticalConnections(List<CritConn> tempCritConn) {
        this.crits = new CritConn[tempCritConn.size()];
        tempCritConn.toArray(this.crits);
    }

	@Override
	public void run() {
		try {
			while(true){
				this.initializeIteration();
		    	
				if(this.nets != null) {
		        	for(Net net: this.nets) {
		        		this.processNet(net);
		        		this.netCounter++;
		        	}
		    	}
		    	if(this.crits != null) {
		        	for(CritConn critConn:this.crits) {
		            	this.processConnection(critConn.sourceIndex, critConn.sinkIndex, critConn.sinkOffset - critConn.sourceOffset, critConn.weight, true);
		            	this.critCounter++;
		            }
		    	}
		    	
		    	this.finished = true;
		    	
		    	synchronized(this){
		    		wait();
		    	}
			}
		} catch (InterruptedException e) {
			System.out.println("Thread interrupted");
		}
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