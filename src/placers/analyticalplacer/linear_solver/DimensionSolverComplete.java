package placers.analyticalplacer.linear_solver;

import mathtools.CGSolver;
import mathtools.Crs;
import mathtools.FastCrs;


public class DimensionSolverComplete implements DimensionSolver {

    private final double[] coordinates;
    private final FastCrs matrix;
    private final double[] vector;
    private final int numIOBlocks;
    private final double epsilon;

    public DimensionSolverComplete(double[] coordinates, int numIOBlocks, double epsilon) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;
        this.epsilon = epsilon;

        int numMovableBlocks = coordinates.length - numIOBlocks;

        this.matrix = new FastCrs(numMovableBlocks);
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

        if(relativeIndex1 == 0 || relativeIndex2 == 0) {
            int d = 0;
        }

        if(!fixed1 && !fixed2) {
            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.matrix.addElement(relativeIndex1, relativeIndex2, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex1, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex2, weight);

        } else if(fixed1) {
            this.matrix.addElement(relativeIndex2, relativeIndex2, weight);
            this.vector[relativeIndex2] += weight * coordinate1;

        } else if(fixed2) {
            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.vector[relativeIndex1] += weight * coordinate2;
        }
    }

    @Override
    public void solve() {
        CGSolver solver = new CGSolver(this.matrix, this.vector);
        double[] solution = solver.solve(this.epsilon);

        int numMovableBlocks = this.coordinates.length - this.numIOBlocks;
        System.arraycopy(solution, 0, this.coordinates, this.numIOBlocks, numMovableBlocks);
    }
}
