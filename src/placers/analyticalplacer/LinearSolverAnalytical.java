package placers.analyticalplacer;

import java.util.List;

import circuit.block.TimingEdge;

import util.Pair;



class LinearSolverAnalytical extends LinearSolver {

    private DimensionSolverAnalytical solverX, solverY;
    private double criticalityThreshold;

    LinearSolverAnalytical(double[] coordinatesX, double[] coordinatesY, int numIOBlocks, double pseudoWeight, double criticalityThreshold, double epsilon) {
        super(coordinatesX, coordinatesY, numIOBlocks);
        this.criticalityThreshold = criticalityThreshold;

        this.solverX = new DimensionSolverAnalytical(coordinatesX, numIOBlocks, pseudoWeight, epsilon);
        this.solverY = new DimensionSolverAnalytical(coordinatesY, numIOBlocks, pseudoWeight, epsilon);
    }

    @Override
    void addPseudoConnections(int[] legalX, int[] legalY) {
        int numIOBlocks = this.getNumIOBlocks();
        int numBlocks = this.coordinatesX.length;
        for(int blockIndex = numIOBlocks; blockIndex < numBlocks; blockIndex++) {
            this.solverX.addPseudoConnection(blockIndex, legalX[blockIndex]);
            this.solverY.addPseudoConnection(blockIndex, legalY[blockIndex]);
        }
    }

    @Override
    void processNetWLD(int[] blockIndexes) {

        int numNetBlocks = blockIndexes.length;
        double weightMultiplier = AnalyticalAndGradientPlacer.getWeight(numNetBlocks) / (numNetBlocks - 1);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = blockIndexes[0], blockIndex2 = blockIndexes[1];
            boolean fixed1 = isFixed(blockIndex1), fixed2 = isFixed(blockIndex2);

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


        boolean minXFixed = this.isFixed(minXIndex), maxXFixed = isFixed(maxXIndex),
                minYFixed = this.isFixed(minYIndex), maxYFixed = isFixed(maxYIndex);

        // Add connections from the min and max block to every block inside the net
        for(int i = 0; i < numNetBlocks; i++) {
            int blockIndex = blockIndexes[i];
            boolean isFixed = this.isFixed(blockIndex);
            double x = this.coordinatesX[blockIndex], y = this.coordinatesY[blockIndex];

            if(blockIndex != minXIndex) {
                this.solverX.addConnection(
                        minXFixed, minXIndex, minX,
                        isFixed, blockIndex, x,
                        weightMultiplier);

                if(blockIndex != maxXIndex) {
                    this.solverX.addConnection(
                            isFixed, blockIndex, x,
                            maxXFixed, maxXIndex, maxX,
                            weightMultiplier);
                }
            }

            if(blockIndex != minYIndex) {
                this.solverY.addConnection(
                        minYFixed, minYIndex, minY,
                        isFixed, blockIndex, y,
                        weightMultiplier);

                if(blockIndex != maxYIndex) {
                    this.solverY.addConnection(
                            isFixed, blockIndex, y,
                            maxYFixed, maxYIndex, maxY,
                            weightMultiplier);
                }
            }
        }
    }

    @Override
    void processNetTD(List<Pair<Integer, TimingEdge>> net) {
        int numPins = net.size();
        int sourceIndex = net.get(0).getFirst();

        for(int i = 1; i < numPins; i++) {
            Pair<Integer, TimingEdge> entry = net.get(i);
            double criticality = entry.getSecond().getCriticality();

            if(criticality > this.criticalityThreshold) {
                int sinkIndex = entry.getFirst();
                double weightMultiplier = 2.0 / numPins * criticality;

                this.processConnectionTD(sourceIndex, sinkIndex, weightMultiplier);
            }
        }
    }

    private void processConnectionTD(int sourceIndex, int sinkIndex, double weightMultiplier) {
        boolean sourceFixed = this.isFixed(sourceIndex);
        boolean sinkFixed = this.isFixed(sinkIndex);

        double sourceCoordinate = this.coordinatesX[sourceIndex];
        double sinkCoordinate = this.coordinatesX[sinkIndex];
        if(sourceCoordinate < sinkCoordinate) {
            this.solverX.addConnection(
                    sourceFixed, sourceIndex, sourceCoordinate,
                    sinkFixed, sinkIndex, sinkCoordinate,
                    weightMultiplier);
        } else {
            this.solverX.addConnection(
                    sinkFixed, sinkIndex, sinkCoordinate,
                    sourceFixed, sourceIndex, sourceCoordinate,
                    weightMultiplier);
        }

        sourceCoordinate = this.coordinatesY[sourceIndex];
        sinkCoordinate = this.coordinatesY[sinkIndex];
        if(sourceCoordinate < sinkCoordinate) {
            this.solverY.addConnection(
                    sourceFixed, sourceIndex, sourceCoordinate,
                    sinkFixed, sinkIndex, sinkCoordinate,
                    weightMultiplier);
        } else {
            this.solverY.addConnection(
                    sinkFixed, sinkIndex, sinkCoordinate,
                    sourceFixed, sourceIndex, sourceCoordinate,
                    weightMultiplier);
        }
    }

    @Override
    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
