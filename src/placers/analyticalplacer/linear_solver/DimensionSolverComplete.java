package placers.analyticalplacer.linear_solver;

import mathtools.CGSolver;
import mathtools.Csr;


public class DimensionSolverComplete extends DimensionSolver {

    private final double[] coordinates;
    private final Csr matrix;
    private final double[] vector;
    private final int numIOBlocks;
    private final double epsilon;

    public DimensionSolverComplete(double[] coordinates, int numIOBlocks, double epsilon) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;
        this.epsilon = epsilon;

        int numMovableBlocks = coordinates.length - numIOBlocks;

        this.matrix = new Csr(numMovableBlocks);
        this.vector = new double[numMovableBlocks];
    }

    @Override
    public void addConnection(
            boolean minFixed, int minIndex, double minCoordinate,
            boolean maxFixed, int maxIndex, double maxCoordinate,
            double weightMultiplier, boolean isPseudoConnection) {

        double weight = weightMultiplier / Math.max(Math.abs(minCoordinate - maxCoordinate), 0.005);
        int relativeIndex1 = minIndex - this.numIOBlocks;
        int relativeIndex2 = maxIndex - this.numIOBlocks;

        if(!minFixed && !maxFixed) {
            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.matrix.addElement(relativeIndex1, relativeIndex2, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex1, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex2, weight);

        } else if(minFixed) {
            this.matrix.addElement(relativeIndex2, relativeIndex2, weight);
            this.vector[relativeIndex2] += weight * minCoordinate;

        } else if(maxFixed) {
            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.vector[relativeIndex1] += weight * maxCoordinate;
        }
    }

    @Override
    public void solve() {
        this.matrix.prepareArrays();
        CGSolver solver = new CGSolver(this.matrix, this.vector);
        double[] solution = solver.solve(this.epsilon);

        int numMovableBlocks = this.coordinates.length - this.numIOBlocks;
        System.arraycopy(solution, 0, this.coordinates, this.numIOBlocks, numMovableBlocks);
    }
}
