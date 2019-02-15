package place.placers.analytical;

import place.placers.analytical.AnalyticalAndGradientPlacer.CritConn;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

class LinearSolverAnalytical {

    private double[] coordinatesX, coordinatesY;
    private DimensionSolverAnalytical solverX, solverY;

    private boolean[] fixed;

    LinearSolverAnalytical(
            double[] coordinatesX,
            double[] coordinatesY,
            double pseudoWeight,
            double epsilon,
            boolean[] fixed) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.fixed = fixed;

        this.solverX = new DimensionSolverAnalytical(coordinatesX, pseudoWeight, epsilon, fixed);
        this.solverY = new DimensionSolverAnalytical(coordinatesY, pseudoWeight, epsilon, fixed);
    }


    void addPseudoConnections(double[] legalX, double[] legalY) {
        int numBlocks = this.coordinatesX.length;
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
        	if(!this.isFixed(blockIndex)){
                this.solverX.addPseudoConnection(blockIndex, legalX[blockIndex]);
                this.solverY.addPseudoConnection(blockIndex, legalY[blockIndex]);
        	}
        }
    }

    void processNetWLD(Net net) {
        int numNetBlocks = net.blocks.length;
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks) / (numNetBlocks - 1);

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

    void processNetTD(CritConn critConn){
        boolean sourceFixed = this.isFixed(critConn.sourceIndex);
        boolean sinkFixed = this.isFixed(critConn.sinkIndex);

        this.solverX.addConnection(
                sourceFixed, critConn.sourceIndex, this.coordinatesX[critConn.sourceIndex], 0,
                sinkFixed, critConn.sinkIndex, this.coordinatesX[critConn.sinkIndex], 0,
                critConn.weight);

        this.solverY.addConnection(
                sourceFixed, critConn.sourceIndex, this.coordinatesY[critConn.sourceIndex] + critConn.sourceOffset, critConn.sourceOffset,
                sinkFixed, critConn.sinkIndex, this.coordinatesY[critConn.sinkIndex] + critConn.sinkOffset, critConn.sinkOffset,
                critConn.weight);
    }

    private boolean isFixed(int blockIndex){
        return this.fixed[blockIndex];
    }

    void solve(){
        this.solverX.solve();
        this.solverY.solve();
    }
}
