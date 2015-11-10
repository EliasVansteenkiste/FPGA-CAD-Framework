package placers.analyticalplacer.linear_solver;

import mathtools.CGSolver;
import mathtools.Csr;


class DimensionSolverComplete extends DimensionSolver {

    private final double[] coordinates;
    private final Csr matrix;
    private final double[] vector;
    private final int numIOBlocks;

    private final double pseudoWeight;
    private final double epsilon;


    DimensionSolverComplete(double[] coordinates, int numIOBlocks, double pseudoWeight, double epsilon) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;

        this.pseudoWeight = pseudoWeight;
        this.epsilon = epsilon;

        int numMovableBlocks = coordinates.length - numIOBlocks;

        this.matrix = new Csr(numMovableBlocks);
        this.vector = new double[numMovableBlocks];
    }


    void addPseudoConnection(int blockIndex, double coordinate, int legalCoordinate) {
        double weight = this.pseudoWeight / Math.max(Math.abs(coordinate - legalCoordinate), 0.005);
        int relativeIndex = blockIndex - this.numIOBlocks;

        this.matrix.addElement(relativeIndex, relativeIndex, weight);
        this.vector[relativeIndex] += weight * legalCoordinate;
    }

    @Override
    void addConnection(
            boolean minFixed, int minIndex, double minCoordinate,
            boolean maxFixed, int maxIndex, double maxCoordinate,
            double weightMultiplier) {

        double weight = weightMultiplier / Math.max(maxCoordinate - minCoordinate, 0.005);
        int minRelativeIndex = minIndex - this.numIOBlocks;
        int maxRelativeIndex = maxIndex - this.numIOBlocks;

        if(minFixed) {
            if(!maxFixed) {
                this.matrix.addElement(maxRelativeIndex, maxRelativeIndex, weight);
                this.vector[maxRelativeIndex] += weight * minCoordinate;
            }

        } else if(maxFixed) {
            this.matrix.addElement(minRelativeIndex, minRelativeIndex, weight);
            this.vector[minRelativeIndex] += weight * maxCoordinate;

        } else {
            this.matrix.addElement(minRelativeIndex, minRelativeIndex, weight);
            this.matrix.addElement(minRelativeIndex, maxRelativeIndex, -weight);
            this.matrix.addElement(maxRelativeIndex, minRelativeIndex, -weight);
            this.matrix.addElement(maxRelativeIndex, maxRelativeIndex, weight);
        }
    }

    @Override
    void solve() {
        this.matrix.prepareArrays();
        CGSolver solver = new CGSolver(this.matrix, this.vector);
        double[] solution = solver.solve(this.epsilon);

        int numMovableBlocks = this.coordinates.length - this.numIOBlocks;
        System.arraycopy(solution, 0, this.coordinates, this.numIOBlocks, numMovableBlocks);
    }
}
