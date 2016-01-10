package placers.analyticalplacer;

import placers.analyticalplacer.AnalyticalAndGradientPlacer.Net;
import placers.analyticalplacer.AnalyticalAndGradientPlacer.NetBlock;

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

    void processNet(Net net, double criticality) {
        int numNetBlocks = net.blocks.length;

        double weight = criticality * AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            NetBlock block1 = net.blocks[0],
                     block2 = net.blocks[1];
            int blockIndex1 = block1.blockIndex,
                blockIndex2 = block2.blockIndex;
            double offset1 = block1.offset,
                   offset2 = block2.offset;

            double coordinate1 = this.coordinatesX[blockIndex1],
                   coordinate2 = this.coordinatesX[blockIndex2];
            if(coordinate1 < coordinate2) {
                this.solverX.addConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight);
            } else {
                this.solverX.addConnection(blockIndex2, blockIndex1, coordinate1 - coordinate2, weight);
            }

            coordinate1 = this.coordinatesY[blockIndex1] + offset1;
            coordinate2 = this.coordinatesY[blockIndex2] + offset2;
            if(coordinate1 < coordinate2) {
                this.solverY.addConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight);
            } else {
                this.solverY.addConnection(blockIndex2, blockIndex1, coordinate1 - coordinate2, weight);
            }

            return;
        }


        // For bigger nets, we have to find the min and max block
        NetBlock initialNetBlock = net.blocks[0];

        int initialBlockIndex = initialNetBlock.blockIndex;
        int minXIndex = initialBlockIndex,
            maxXIndex = initialBlockIndex,
            minYIndex = initialBlockIndex,
            maxYIndex = initialBlockIndex;

        double initialOffset = initialNetBlock.offset;
        double minYOffset = initialOffset,
               maxYOffset = initialOffset;

        double minX = this.coordinatesX[minXIndex],
               maxX = this.coordinatesX[maxXIndex],
               minY = this.coordinatesY[minYIndex] + minYOffset,
               maxY = this.coordinatesY[maxYIndex] + maxYOffset;

        for(int i = 1; i < numNetBlocks; i++) {
            NetBlock block = net.blocks[i];
            int blockIndex = block.blockIndex;
            double x = this.coordinatesX[blockIndex],
                   y = this.coordinatesY[blockIndex] + block.offset;

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


    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}