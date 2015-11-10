package placers.analyticalplacer.linear_solver;

import placers.analyticalplacer.AnalyticalPlacer;


public class LinearSolverGradient extends LinearSolver {

    public LinearSolverGradient(double[] coordinatesX, double[] coordinatesY, int numIOBlocks, double pseudoWeightFactor, double stepSize) {
        super(coordinatesX, coordinatesY, numIOBlocks);

        this.solverX = new DimensionSolverGradient(coordinatesX, stepSize, pseudoWeightFactor);
        this.solverY = new DimensionSolverGradient(coordinatesY, stepSize, pseudoWeightFactor);
    }

    @Override
    public int getPseudoBlockIndexStart() {
        return 0;
    }

    @Override
    public void processNet(int[] blockIndexes) {
        int numNetBlocks = blockIndexes.length;

        double weightMultiplier = AnalyticalPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = blockIndexes[0], blockIndex2 = blockIndexes[1];
            boolean fixed1 = isFixed(blockIndex1), fixed2 = isFixed(blockIndex2);

            this.solverX.addConnectionMinMaxUnknown(
                    fixed1, blockIndex1, this.coordinatesX[blockIndex1],
                    fixed2, blockIndex2, this.coordinatesX[blockIndex2],
                    weightMultiplier, false);

            this.solverY.addConnectionMinMaxUnknown(
                    fixed1, blockIndex1, this.coordinatesY[blockIndex1],
                    fixed2, blockIndex2, this.coordinatesY[blockIndex2],
                    weightMultiplier, false);

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


        boolean minXFixed = isFixed(minXIndex), maxXFixed = isFixed(maxXIndex),
                minYFixed = isFixed(minYIndex), maxYFixed = isFixed(maxYIndex);

        // Add connections between the min and max block
        this.solverX.addConnection(
                minXFixed, minXIndex, minX,
                maxXFixed, maxXIndex, maxX,
                weightMultiplier, false);

        this.solverY.addConnection(
                minYFixed, minYIndex, minY,
                maxYFixed, maxYIndex, maxY,
                weightMultiplier, false);
    }


    @Override
    public void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
