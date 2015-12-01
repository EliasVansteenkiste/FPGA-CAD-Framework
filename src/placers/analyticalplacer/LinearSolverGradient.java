package placers.analyticalplacer;

class LinearSolverGradient {

    private double[] coordinatesX, coordinatesY;

    private DimensionSolverGradient solverX, solverY;

    LinearSolverGradient(double[] coordinatesX, double[] coordinatesY, double stepSize) {
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.solverX = new DimensionSolverGradient(coordinatesX, stepSize);
        this.solverY = new DimensionSolverGradient(coordinatesY, stepSize);
    }

    public void initializeIteration(double pseudoWeight) {
        this.solverX.initializeIteration(pseudoWeight);
        this.solverY.initializeIteration(pseudoWeight);
    }


    void addPseudoConnections(int[] legalX, int[] legalY) {
        this.solverX.setLegal(legalX);
        this.solverY.setLegal(legalY);
    }

    void processNet(int[] blockIndexes, double criticality) {
        int numNetBlocks = blockIndexes.length;

        double weight = criticality * AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = blockIndexes[0], blockIndex2 = blockIndexes[1];

            double coordinate1 = this.coordinatesX[blockIndex1];
            double coordinate2 = this.coordinatesX[blockIndex2];
            this.solverX.addConnectionMinMaxUnknown(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight);

            coordinate1 = this.coordinatesY[blockIndex1];
            coordinate2 = this.coordinatesY[blockIndex2];
            this.solverY.addConnectionMinMaxUnknown(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight);

            return;
        }


        // For bigger nets, we have to find the min and max block
        int initialBlockIndex = blockIndexes[0];
        double minX = this.coordinatesX[initialBlockIndex], maxX = this.coordinatesX[initialBlockIndex],
               minY = this.coordinatesY[initialBlockIndex], maxY = this.coordinatesY[initialBlockIndex];
        int minXIndex = initialBlockIndex, maxXIndex = initialBlockIndex,
            minYIndex = initialBlockIndex, maxYIndex = initialBlockIndex;

        for(int i = 1; i < numNetBlocks; i++) {
            int blockIndex = blockIndexes[i];
            double x = this.coordinatesX[blockIndex], y = this.coordinatesY[blockIndex];

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
        this.solverY.addConnection(minYIndex, maxYIndex, maxY - minY, weight);
    }


    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
