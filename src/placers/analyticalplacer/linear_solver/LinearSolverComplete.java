package placers.analyticalplacer.linear_solver;

import mathtools.CGSolver;
import mathtools.Crs;


public class LinearSolverComplete implements LinearSolver {
    
    private final Crs matrix;
    private final double[] vector;
    private final int numIOBlocks;
    private final double epsilon;
    
    public LinearSolverComplete(int numIOBlocks, int numMovableBlocks, double epsilon) {
        this.numIOBlocks = numIOBlocks;
        this.epsilon = epsilon;
        
        this.matrix = new Crs(numMovableBlocks);
        this.vector = new double[numMovableBlocks];
    }
    
    @Override
    public void addConnection(
            boolean fixed1, int index1, double coordinate1,
            boolean fixed2, int index2, double coordinate2,
            double weightMultiplier) {
        
        double weight = weightMultiplier / Math.max(Math.abs(coordinate1 - coordinate2), 0.005);
        int relativeIndex1 = index1 - this.numIOBlocks;
        int relativeIndex2 = index2 - this.numIOBlocks;
        
        if(!fixed1 && !fixed2) {
            matrix.setElement(relativeIndex1, relativeIndex1, matrix.getElement(relativeIndex1, relativeIndex1) + weight);
			matrix.setElement(relativeIndex1, relativeIndex2, matrix.getElement(relativeIndex1, relativeIndex2) - weight);
			matrix.setElement(relativeIndex2, relativeIndex1, matrix.getElement(relativeIndex2, relativeIndex1) - weight);
			matrix.setElement(relativeIndex2, relativeIndex2, matrix.getElement(relativeIndex2, relativeIndex2) + weight);
        
        } else if(fixed1) {
            matrix.setElement(relativeIndex2, relativeIndex2, matrix.getElement(relativeIndex2, relativeIndex2) + weight);
			vector[relativeIndex2] += weight * coordinate1;
        
        } else if(fixed2) {
            matrix.setElement(relativeIndex1, relativeIndex1, matrix.getElement(relativeIndex1, relativeIndex1) + weight);
			vector[relativeIndex1] += weight * coordinate2;
        }
    }
    
    @Override
    public double[] solve() {
        CGSolver solver = new CGSolver(this.matrix, this.vector);
        return solver.solve(this.epsilon);
    }
}
