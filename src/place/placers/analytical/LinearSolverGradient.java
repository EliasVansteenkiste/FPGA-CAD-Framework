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
            double stepSize,
            double maxConnectionLength,
            double speedAveraging,
            boolean[] fixed) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.netBlockIndexes = netBlockIndexes;
        this.netBlockOffsets = netBlockOffsets;

        this.solverX = new DimensionSolverGradient(coordinatesX, stepSize, maxConnectionLength, speedAveraging, fixed);
        this.solverY = new DimensionSolverGradient(coordinatesY, stepSize, maxConnectionLength, speedAveraging, fixed);
    }

    public void initializeIteration(double pseudoWeight) {
        this.solverX.initializeIteration(pseudoWeight);
        this.solverY.initializeIteration(pseudoWeight);
    }


    void addPseudoConnections(int[] legalX, int[] legalY) {
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

            double coordinate1 = this.coordinatesY[blockIndex1] + this.netBlockOffsets[netStart],
                   coordinate2 = this.coordinatesY[blockIndex2] + this.netBlockOffsets[netStart + 1];
            if(coordinate1 < coordinate2) {
                this.solverY.addConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight);
            } else {
                this.solverY.addConnection(blockIndex2, blockIndex1, coordinate1 - coordinate2, weight);
            }

            coordinate1 = this.coordinatesX[blockIndex1];
            coordinate2 = this.coordinatesX[blockIndex2];
            if(coordinate1 < coordinate2) {
                this.solverX.addConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight);
            } else {
                this.solverX.addConnection(blockIndex2, blockIndex1, coordinate1 - coordinate2, weight);
            }

            return;
        }


        // For bigger nets, we have to find the min and max block
        int minXIndex = this.netBlockIndexes[netStart],
            maxXIndex = this.netBlockIndexes[netStart],
            minYIndex = this.netBlockIndexes[netStart],
            maxYIndex = this.netBlockIndexes[netStart];

        float minYOffset = this.netBlockOffsets[netStart],
              maxYOffset = this.netBlockOffsets[netStart];

        double minX = this.coordinatesX[minXIndex],
               maxX = this.coordinatesX[maxXIndex],
               minY = this.coordinatesY[minYIndex] + minYOffset,
               maxY = this.coordinatesY[maxYIndex] + maxYOffset;

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
        this.solverX.addConnection(minXIndex, maxXIndex, maxX - minX, weight);
        this.solverY.addConnection(minYIndex, maxYIndex, maxY + maxYOffset - minY - minYOffset, weight);
    }

    void processConnection(int blockIndex1, int blockIndex2, float offset, float weight) {
        double x1 = this.coordinatesX[blockIndex1],
               x2 = this.coordinatesX[blockIndex2],
               y1 = this.coordinatesY[blockIndex1],
               y2 = this.coordinatesY[blockIndex2];

        if(x2 > x1) {
            this.solverX.addConnection(blockIndex1, blockIndex2, x2 - x1, weight);
        } else {
            this.solverX.addConnection(blockIndex2, blockIndex1, x1 - x2, weight);
        }

        if(y2 > y1) {
            this.solverY.addConnection(blockIndex1, blockIndex2, y2 - y1 + offset, weight);
        } else {
            this.solverY.addConnection(blockIndex2, blockIndex1, y1 - y2 - offset, weight);
        }
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