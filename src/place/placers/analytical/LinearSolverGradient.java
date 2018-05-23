package place.placers.analytical;

import place.placers.analytical.AnalyticalAndGradientPlacer.Net;

class LinearSolverGradient {

    private double[] coordinatesX, coordinatesY;

    private DimensionSolverGradient solverX, solverY;

    LinearSolverGradient(
            double[] coordinatesX,
            double[] coordinatesY,
            double maxConnectionLength,
            boolean[] fixed,
            double beta1,
            double beta2,
            double eps) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.solverX = new DimensionSolverGradient(coordinatesX, maxConnectionLength, fixed, beta1, beta2, eps);
        this.solverY = new DimensionSolverGradient(coordinatesY, maxConnectionLength, fixed, beta1, beta2, eps);
    }

    public void initializeIteration(double pseudoWeight, double learningRate) {
        this.solverX.initializeIteration(pseudoWeight, learningRate);
        this.solverY.initializeIteration(pseudoWeight, learningRate);
    }


    void addPseudoConnections(double[] legalX, double[] legalY) {
        this.solverX.setLegal(legalX);
        this.solverY.setLegal(legalY);
    }

    void processNet(Net net) {
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
    	this.solverY.processConnection(blockIndex0, blockIndex1, coordinate2 - coordinate1, net.weight, false);

    	coordinate1 = this.coordinatesX[blockIndex0];
    	coordinate2 = this.coordinatesX[blockIndex1];
    	this.solverX.processConnection(blockIndex0, blockIndex1, coordinate2 - coordinate1, net.weight, false);
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
        this.solverX.processConnection(minXIndex, maxXIndex, maxX - minX, net.weight, false);
        this.solverY.processConnection(minYIndex, maxYIndex, maxY - minY, net.weight, false);
    }

    void processConnection(int blockIndex1, int blockIndex2, float offset, float weight, boolean critical) {
        double x1 = this.coordinatesX[blockIndex1],
               x2 = this.coordinatesX[blockIndex2],
               y1 = this.coordinatesY[blockIndex1],
               y2 = this.coordinatesY[blockIndex2];

        this.solverX.processConnection(blockIndex1, blockIndex2, x2 - x1, weight, critical);
        this.solverY.processConnection(blockIndex1, blockIndex2, y2 - y1 + offset, weight, critical);
    }

    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
    
    double[] getCoordinatesX(){
    	return this.coordinatesX;
    }
    double[] getCoordinatesY(){
    	return this.coordinatesY;
    }
}