package placers.analytical;

import org.la4j.LinearAlgebra;
import org.la4j.Matrices;
import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.linear.GaussianSolver;
import org.la4j.linear.JacobiSolver;


class DimensionSolverAnalytical {

    private final double[] coordinates;
    private final Matrix matrix;
    private final Vector vector;
    private final int numIOBlocks;

    private final double pseudoWeight;


    DimensionSolverAnalytical(double[] coordinates, int numIOBlocks, double pseudoWeight, double epsilon) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;

        this.pseudoWeight = pseudoWeight;

        int numMovableBlocks = coordinates.length - numIOBlocks;

        this.matrix = Matrix.diagonal(numMovableBlocks, 0);
        this.vector = Vector.zero(numMovableBlocks);
    }


    void addPseudoConnection(int blockIndex, int legalCoordinate) {
        double weight = this.pseudoWeight;
        int relativeIndex = blockIndex - this.numIOBlocks;

        this.matrix.set(relativeIndex, relativeIndex, weight + this.matrix.get(relativeIndex, relativeIndex));
        this.vector.set(relativeIndex, weight * legalCoordinate + this.vector.get(relativeIndex));
    }


    void addConnection(
            boolean fixed1, int index1, double coordinate1, double offset1,
            boolean fixed2, int index2, double coordinate2, double offset2,
            double weightMultiplier) {

        double weight = weightMultiplier / Math.max(Math.abs(coordinate1 - coordinate2), 0.005);
        int relativeIndex1 = index1 - this.numIOBlocks;
        int relativeIndex2 = index2 - this.numIOBlocks;

        if(fixed1) {
            if(!fixed2) {
                this.matrix.set(relativeIndex2, relativeIndex2, weight + this.matrix.get(relativeIndex2, relativeIndex2));
                this.vector.set(relativeIndex2, weight * (coordinate1 - offset2) + this.vector.get(relativeIndex2));
            }

        } else if(fixed2) {
            this.matrix.set(relativeIndex1, relativeIndex1, weight + this.matrix.get(relativeIndex1, relativeIndex1));
            this.vector.set(relativeIndex1, weight * (coordinate2 - offset1) + this.vector.get(relativeIndex1));

        } else {
            this.matrix.set(relativeIndex1, relativeIndex1, weight + this.matrix.get(relativeIndex1, relativeIndex1));
            this.matrix.set(relativeIndex1, relativeIndex2, -weight + this.matrix.get(relativeIndex1, relativeIndex2));
            this.matrix.set(relativeIndex2, relativeIndex1, -weight + this.matrix.get(relativeIndex2, relativeIndex1));
            this.matrix.set(relativeIndex2, relativeIndex2, weight + this.matrix.get(relativeIndex2, relativeIndex2));
        }
    }

    void solve() {
        GaussianSolver solver = new GaussianSolver(this.matrix);
        Vector solution = solver.solve(this.vector);

        int numMovableBlocks = this.coordinates.length - this.numIOBlocks;
        for(int i = 0; i < numMovableBlocks; i++) {
            this.coordinates[this.numIOBlocks + i] = solution.get(i);
        }
    }
}
