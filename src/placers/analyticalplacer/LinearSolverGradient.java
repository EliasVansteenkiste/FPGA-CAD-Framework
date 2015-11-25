package placers.analyticalplacer;

import java.util.List;

import util.Pair;
import circuit.block.TimingEdge;

class LinearSolverGradient extends LinearSolver {

    private DimensionSolverGradient solverX, solverY;

    LinearSolverGradient(double[] coordinatesX, double[] coordinatesY, int numIOBlocks, double pseudoWeight, double stepSize) {
        super(coordinatesX, coordinatesY, numIOBlocks);

        this.solverX = new DimensionSolverGradient(coordinatesX, pseudoWeight, stepSize);
        this.solverY = new DimensionSolverGradient(coordinatesY, pseudoWeight, stepSize);
    }

    @Override
    void addPseudoConnections(int[] legalX, int[] legalY) {
        this.solverX.setLegal(legalX);
        this.solverY.setLegal(legalY);
    }

    @Override
    void processNetWLD(int[] blockIndexes) {
        int numNetBlocks = blockIndexes.length;

        double weightMultiplier = AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = blockIndexes[0], blockIndex2 = blockIndexes[1];
            boolean fixed1 = this.isFixed(blockIndex1), fixed2 = this.isFixed(blockIndex2);

            double coordinate1 = this.coordinatesX[blockIndex1];
            double coordinate2 = this.coordinatesX[blockIndex2];
            if(coordinate1 < coordinate2) {
                this.solverX.addConnection(
                        fixed1, blockIndex1, coordinate1,
                        fixed2, blockIndex2, coordinate2,
                        weightMultiplier);
            } else {
                this.solverX.addConnection(
                        fixed2, blockIndex2, coordinate2,
                        fixed1, blockIndex1, coordinate1,
                        weightMultiplier);
            }

            coordinate1 = this.coordinatesY[blockIndex1];
            coordinate2 = this.coordinatesY[blockIndex2];
            if(coordinate1 < coordinate2) {
                this.solverY.addConnection(
                        fixed1, blockIndex1, coordinate1,
                        fixed2, blockIndex2, coordinate2,
                        weightMultiplier);
            } else {
                this.solverY.addConnection(
                        fixed2, blockIndex2, coordinate2,
                        fixed1, blockIndex1, coordinate1,
                        weightMultiplier);
            }

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
                weightMultiplier);

        this.solverY.addConnection(
                minYFixed, minYIndex, minY,
                maxYFixed, maxYIndex, maxY,
                weightMultiplier);
    }

    @Override
    void processNetTD(List<Pair<Integer, TimingEdge>> net) {
        // TODO: implement

    }


    @Override
    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
