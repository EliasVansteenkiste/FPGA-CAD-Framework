package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import util.Pair;
import circuit.block.TimingEdge;

class LinearSolverGradient extends LinearSolver {

    private DimensionSolverGradient solverX, solverY;
    private double criticalityThreshold;
    private double timingTradeoff;

    private double avDiff = 0;

    LinearSolverGradient(double[] coordinatesX, double[] coordinatesY, int numIOBlocks, double pseudoWeight, double criticalityThreshold, double stepSize) {
        super(coordinatesX, coordinatesY, numIOBlocks);

        this.criticalityThreshold = criticalityThreshold;
        //this.timingTradeoff = pseudoWeight;
        this.timingTradeoff = 0.5;

        this.solverX = new DimensionSolverGradient(coordinatesX, pseudoWeight, stepSize);
        this.solverY = new DimensionSolverGradient(coordinatesY, pseudoWeight, stepSize);
    }

    public void reset(double pseudoWeight) {
        this.solverX.reset(pseudoWeight);
        this.solverY.reset(pseudoWeight);
    }

    @Override
    void addPseudoConnections(int[] legalX, int[] legalY) {
        this.solverX.setLegal(legalX);
        this.solverY.setLegal(legalY);
    }

    @Override
    void processNetWLD(int[] blockIndexes) {
        int numNetBlocks = blockIndexes.length;

        double weight = (1 - this.timingTradeoff) * AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

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

        /*int numNetBlocks = blockIndexes.length;
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks) / (numNetBlocks - 1);

        int index1, index2, index3;
        switch(numNetBlocks) {
        case 1:
            System.err.println("This shouldn't happen");

        case 2:
            index1 = blockIndexes[0];
            index2 = blockIndexes[1];

            this.solverX.addConnectionMinMaxUnknown(index1, index2, this.coordinatesX[index2] - this.coordinatesX[index1], weight);
            this.solverY.addConnectionMinMaxUnknown(index1, index2, this.coordinatesY[index2] - this.coordinatesY[index1], weight);

            break;

        case 3:
            index1 = blockIndexes[0];
            index2 = blockIndexes[1];
            index3 = blockIndexes[2];

            this.solverX.addConnectionMinMaxUnknown(index1, index2, this.coordinatesX[index2] - this.coordinatesX[index1], weight);
            this.solverY.addConnectionMinMaxUnknown(index1, index2, this.coordinatesY[index2] - this.coordinatesY[index1], weight);

            this.solverX.addConnectionMinMaxUnknown(index1, index3, this.coordinatesX[index3] - this.coordinatesX[index1], weight);
            this.solverY.addConnectionMinMaxUnknown(index1, index3, this.coordinatesY[index3] - this.coordinatesY[index1], weight);

            this.solverX.addConnectionMinMaxUnknown(index2, index3, this.coordinatesX[index3] - this.coordinatesX[index2], weight);
            this.solverY.addConnectionMinMaxUnknown(index2, index3, this.coordinatesY[index3] - this.coordinatesY[index2], weight);

            break;

        default:
            double centerX = 0, centerY = 0;
            for(int i = 0; i < numNetBlocks; i++) {
                int blockIndex = blockIndexes[i];
                centerX += this.coordinatesX[blockIndex];
                centerY += this.coordinatesY[blockIndex];
            }

            centerX /= numNetBlocks;
            centerY /= numNetBlocks;

            for(int i = 0; i < numNetBlocks; i++) {
                int blockIndex = blockIndexes[i];

                this.solverX.addConnectionMinMaxUnknown(blockIndex, -1, centerX - this.coordinatesX[blockIndex], weight);
                this.solverY.addConnectionMinMaxUnknown(blockIndex, -1, centerY - this.coordinatesY[blockIndex], weight);
            }
        }*/
    }


    @Override
    void processNetTD(List<Pair<Integer, TimingEdge>> net) {
        /*int numPins = net.size();
        int sourceIndex = net.get(0).getFirst();

        for(int i = 1; i < numPins; i++) {
            Pair<Integer, TimingEdge> entry = net.get(i);
            double criticality = entry.getSecond().getCriticality();

            if(criticality > this.criticalityThreshold || true) {
                int sinkIndex = entry.getFirst();
                double weight = this.timingTradeoff * 2.0 / numPins * criticality;

                double sourceCoordinate = this.coordinatesX[sourceIndex];
                double sinkCoordinate = this.coordinatesX[sinkIndex];

                //double sinkFactor = Math.min(this.timingTradeoff * 2, 1);
                double sinkFactor = 0.5;
                sinkCoordinate = (1 - sinkFactor) * sourceCoordinate + sinkFactor * this.coordinatesX[sinkIndex];
                this.solverX.addConnectionMinMaxUnknown(sourceIndex, sinkIndex, sinkCoordinate - sourceCoordinate, weight);

                sourceCoordinate = this.coordinatesY[sourceIndex];
                sinkCoordinate = this.coordinatesY[sinkIndex];
                sinkCoordinate = (1 - sinkFactor) * sourceCoordinate + sinkFactor * this.coordinatesY[sinkIndex];
                this.solverY.addConnectionMinMaxUnknown(sourceIndex, sinkIndex, sinkCoordinate - sourceCoordinate, weight);
            }
        }*/

        int numNetBlocks = net.size();
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks) / (numNetBlocks - 1);


        int sourceIndex = net.get(0).getFirst();
        double sourceX = this.coordinatesX[sourceIndex];
        double sourceY = this.coordinatesY[sourceIndex];

        double centerX = sourceX;
        double centerY = sourceY;
        double meanCriticality = 1;

        for(int i = 1; i < numNetBlocks; i++) {
            Pair<Integer, TimingEdge> sinkEntry = net.get(i);
            int sinkIndex = sinkEntry.getFirst();
            double criticality = sinkEntry.getSecond().getCriticality();

            centerX += this.coordinatesX[sinkIndex];
            centerY += this.coordinatesY[sinkIndex];
            meanCriticality *= criticality;
        }

        centerX /= numNetBlocks;
        centerY /= numNetBlocks;
        meanCriticality = Math.pow(meanCriticality, 1.0 / numNetBlocks);
        // TODO: check if total criticality works better than mean criticality

        this.solverX.addConnectionMinMaxUnknown(sourceIndex, -1, centerX - sourceX, weight);
        this.solverY.addConnectionMinMaxUnknown(sourceIndex, -1, centerY - sourceY, weight);

        for(int i = 1; i < numNetBlocks; i++) {
            Pair<Integer, TimingEdge> sinkEntry = net.get(i);
            int sinkIndex = sinkEntry.getFirst();
            double criticality = sinkEntry.getSecond().getCriticality();

            this.solverX.addConnectionMinMaxUnknown(sinkIndex, -1, centerX - this.coordinatesX[sinkIndex], weight);
            this.solverY.addConnectionMinMaxUnknown(sinkIndex, -1, centerY - this.coordinatesY[sinkIndex], weight);

            this.avDiff += (centerX - this.coordinatesX[sinkIndex]);
        }
    }


    @Override
    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
