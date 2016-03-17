package placers.analytical;

import placers.analytical.AnalyticalAndGradientPlacer.Net;
import placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import placers.analytical.AnalyticalAndGradientPlacer.TimingNetBlock;

class LinearSolverAnalytical {

    private double[] coordinatesX, coordinatesY;
    private int numIOBlocks;

    private DimensionSolverAnalytical solverX, solverY;
    private double criticalityThreshold, tradeOff;

    LinearSolverAnalytical(
            double[] coordinatesX,
            double[] coordinatesY,
            int numIOBlocks,
            double pseudoWeight,
            double criticalityThreshold,
            double tradeOff,
            double epsilon) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.numIOBlocks = numIOBlocks;

        this.criticalityThreshold = criticalityThreshold;
        this.tradeOff = tradeOff;

        this.solverX = new DimensionSolverAnalytical(coordinatesX, numIOBlocks, pseudoWeight, epsilon);
        this.solverY = new DimensionSolverAnalytical(coordinatesY, numIOBlocks, pseudoWeight, epsilon);
    }


    void addPseudoConnections(int[] legalX, int[] legalY) {
        int numBlocks = this.coordinatesX.length;
        for(int blockIndex = this.numIOBlocks; blockIndex < numBlocks; blockIndex++) {
            this.solverX.addPseudoConnection(blockIndex, legalX[blockIndex]);
            this.solverY.addPseudoConnection(blockIndex, legalY[blockIndex]);
        }
    }


    void processNetWLD(Net net) {

        int numNetBlocks = net.blocks.length;
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks) / (2*numNetBlocks - 3);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            NetBlock block1 = net.blocks[0],
                     block2 = net.blocks[1];
            int blockIndex1 = block1.blockIndex,
                blockIndex2 = block2.blockIndex;
            double offset1 = block1.offset,
                   offset2 = block2.offset;
            boolean fixed1 = this.isFixed(blockIndex1),
                    fixed2 = this.isFixed(blockIndex2);


            this.solverX.addConnection(
                    fixed1, blockIndex1, this.coordinatesX[blockIndex1], 0,
                    fixed2, blockIndex2, this.coordinatesX[blockIndex2], 0,
                    weight);

            this.solverY.addConnection(
                    fixed1, blockIndex1, this.coordinatesY[blockIndex1] + offset1, offset1,
                    fixed2, blockIndex2, this.coordinatesY[blockIndex2] + offset2, offset2,
                    weight);

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
               minY = this.coordinatesY[minYIndex] + initialOffset,
               maxY = this.coordinatesY[maxYIndex] + initialOffset;

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

        boolean minXFixed = this.isFixed(minXIndex), maxXFixed = isFixed(maxXIndex),
                minYFixed = this.isFixed(minYIndex), maxYFixed = isFixed(maxYIndex);

        // Add connections from the min and max block to every block inside the net
        for(int i = 0; i < numNetBlocks; i++) {
            NetBlock block = net.blocks[i];
            int blockIndex = block.blockIndex;
            double offset = block.offset;

            boolean isFixed = this.isFixed(blockIndex);
            double x = this.coordinatesX[blockIndex],
                   y = this.coordinatesY[blockIndex] + offset;

            if(blockIndex != minXIndex) {
                this.solverX.addConnection(
                        minXFixed, minXIndex, minX, 0,
                        isFixed, blockIndex, x, 0,
                        weight);

                if(blockIndex != maxXIndex) {
                    this.solverX.addConnection(
                            maxXFixed, maxXIndex, maxX, 0,
                            isFixed, blockIndex, x, 0,
                            weight);
                }
            }

            if(blockIndex != minYIndex) {
                this.solverY.addConnection(
                        minYFixed, minYIndex, minY, minYOffset,
                        isFixed, blockIndex, y, offset,
                        weight);

                if(blockIndex != maxYIndex) {
                    this.solverY.addConnection(
                            maxYFixed, maxYIndex, maxY, maxYOffset,
                            isFixed, blockIndex, y, offset,
                            weight);
                }
            }
        }
    }


    void processNetTD(TimingNet net) {
        int numSinks = net.sinks.length;
        int sourceIndex = net.source.blockIndex;
        double sourceOffset = net.source.offset;

        for(TimingNetBlock sink : net.sinks) {
            double criticality = sink.timingEdge.getCriticality();

            if(criticality > this.criticalityThreshold) {
                double weight = this.tradeOff / numSinks * criticality;

                int sinkIndex = sink.blockIndex;
                double sinkOffset = sink.offset;

                boolean sourceFixed = this.isFixed(sourceIndex);
                boolean sinkFixed = this.isFixed(sinkIndex);

                this.solverX.addConnection(
                        sourceFixed, sourceIndex, this.coordinatesX[sourceIndex], 0,
                        sinkFixed, sinkIndex, this.coordinatesX[sinkIndex], 0,
                        weight);

                this.solverY.addConnection(
                        sourceFixed, sourceIndex, this.coordinatesY[sourceIndex] + sourceOffset, sourceOffset,
                        sinkFixed, sinkIndex, this.coordinatesY[sinkIndex] + sinkOffset, sinkOffset,
                        weight);
            }
        }
    }


    private boolean isFixed(int blockIndex) {
        return blockIndex < this.numIOBlocks;
    }

    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
