package placers.analyticalplacer.linear_solver;


public abstract class LinearSolver {
    
    protected double[] coordinatesX, coordinatesY;
    private int numIOBlocks;
    protected DimensionSolver solverX, solverY;
    
    LinearSolver(double[] coordinatesX, double[] coordinatesY, int numIOBlocks) {
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;
        
        this.numIOBlocks = numIOBlocks;
    }
    
    public abstract void processNet(int[] blockIndexes);
    
    
    public void addPseudoConnection(int blockIndex, int legalX, int legalY, double pseudoWeightFactor) {
        this.solverX.addConnection(
                false, blockIndex, this.coordinatesX[blockIndex],
                true, -1, legalX,
                pseudoWeightFactor);
        
        this.solverY.addConnection(
                false, blockIndex, this.coordinatesY[blockIndex],
                true, -1, legalY,
                pseudoWeightFactor);
    }
    
    public void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
    
    
    protected boolean isFixed(int blockIndex) {
        return false; // DEBUG
        //return blockIndex < this.numIOBlocks;
    }
}
