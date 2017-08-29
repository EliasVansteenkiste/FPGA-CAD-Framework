package place.placers.analytical;

class LinearSolverGradient {

    private double[] coordinatesX, coordinatesY;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;

    private DimensionSolverGradient solverX, solverY;

    LinearSolverGradient(
            double[] coordinatesX,
            double[] coordinatesY,
            int[] netBlockIndexes,
            float[] netBlockOffsets,
            double maxConnectionLength,
            boolean[] fixed,
            int[] leafNode,
            double beta1,
            double beta2,
            double eps) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.netBlockIndexes = netBlockIndexes;
        this.netBlockOffsets = netBlockOffsets;

        this.solverX = new DimensionSolverGradient(coordinatesX, maxConnectionLength, fixed, leafNode, beta1, beta2, eps);
        this.solverY = new DimensionSolverGradient(coordinatesY, maxConnectionLength, fixed, leafNode, beta1, beta2, eps);
    }

    public void initializeIteration(double pseudoWeight, double learningRate) {
        this.solverX.initializeIteration(pseudoWeight, learningRate);
        this.solverY.initializeIteration(pseudoWeight, learningRate);
    }


    void addPseudoConnections(double[] legalX, double[] legalY) {
        this.solverX.setLegal(legalX);
        this.solverY.setLegal(legalY);
    }

    void processNet(int netStart, int netEnd) {
        int numNetBlocks = netEnd - netStart;
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = this.netBlockIndexes[netStart],
                blockIndex2 = this.netBlockIndexes[netStart + 1];

            double coordinate1, coordinate2;
            
            coordinate1 = this.coordinatesY[blockIndex1] + this.netBlockOffsets[netStart];
            coordinate2 = this.coordinatesY[blockIndex2] + this.netBlockOffsets[netStart + 1];
            this.solverY.processConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight, false);

            coordinate1 = this.coordinatesX[blockIndex1];
            coordinate2 = this.coordinatesX[blockIndex2];
            this.solverX.processConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight, false);

            return;
        }


        // For bigger nets, we have to find the min and max block
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
        this.solverX.processConnection(minXIndex, maxXIndex, maxX - minX, weight, false);
        this.solverY.processConnection(minYIndex, maxYIndex, maxY - minY, weight, false);
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