package placers.analyticalplacer;

import java.util.List;

import circuit.block.TimingEdge;

import util.Pair;


abstract class LinearSolver {

    protected double[] coordinatesX, coordinatesY;
    private int numIOBlocks;

    LinearSolver(double[] coordinatesX, double[] coordinatesY, int numIOBlocks) {
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.numIOBlocks = numIOBlocks;
    }

    abstract void processNetWLD(int[] blockIndexes);
    abstract void processNetTD(List<Pair<Integer, TimingEdge>> net);
    abstract void addPseudoConnections(int[] legalX, int[] legalY);
    abstract void solve();


    protected boolean isFixed(int blockIndex) {
        return blockIndex < this.numIOBlocks;
    }

    protected int getNumIOBlocks() {
        return this.numIOBlocks;
    }
}
