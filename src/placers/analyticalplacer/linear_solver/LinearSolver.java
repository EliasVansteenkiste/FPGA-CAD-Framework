package placers.analyticalplacer.linear_solver;


public abstract class LinearSolver {

    protected double[] coordinatesX, coordinatesY;
    private int numIOBlocks;

    LinearSolver(double[] coordinatesX, double[] coordinatesY, int numIOBlocks) {
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.numIOBlocks = numIOBlocks;
    }

    public abstract void processNet(int[] blockIndexes);
    public abstract void addPseudoConnections(int[] legalX, int[] legalY);
    public abstract void solve();


    protected boolean isFixed(int blockIndex) {
        return blockIndex < this.numIOBlocks;
    }

    protected int getNumIOBlocks() {
        return this.numIOBlocks;
    }
}
