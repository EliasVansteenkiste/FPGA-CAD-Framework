package place.placers.analytical;

import place.mathtools.CGSolver;
import place.mathtools.Csr;


class DimensionSolverAnalytical {

    private final double[] coordinates;
    private final Csr matrix;
    private final double[] vector;
    private final int numIOBlocks;

    private final double pseudoWeight;
    private final double epsilon;


    DimensionSolverAnalytical(double[] coordinates, int numIOBlocks, double pseudoWeight, double epsilon) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;

        this.pseudoWeight = pseudoWeight;
        this.epsilon = epsilon;

        int numMovableBlocks = coordinates.length - numIOBlocks;

        this.matrix = new Csr(numMovableBlocks);
        this.vector = new double[numMovableBlocks];
    }


    void addPseudoConnection(int blockIndex, int legalCoordinate) {
        double weight = this.pseudoWeight;
        int relativeIndex = blockIndex - this.numIOBlocks;

        this.matrix.addElement(relativeIndex, relativeIndex, weight);
        this.vector[relativeIndex] += weight * legalCoordinate;
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
                this.matrix.addElement(relativeIndex2, relativeIndex2, weight);
                this.vector[relativeIndex2] += weight * (coordinate1 - offset2);
            }

        } else if(fixed2) {
            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.vector[relativeIndex1] += weight * (coordinate2 - offset1);

        } else {
            this.matrix.addElement(relativeIndex1, relativeIndex1, weight);
            this.matrix.addElement(relativeIndex1, relativeIndex2, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex1, -weight);
            this.matrix.addElement(relativeIndex2, relativeIndex2, weight);
        }
    }

    void solve() {
        this.matrix.prepareArrays();
        CGSolver solver = new CGSolver(this.matrix, this.vector);
        double[] solution = solver.solve(this.epsilon);

        int numMovableBlocks = this.coordinates.length - this.numIOBlocks;
        System.arraycopy(solution, 0, this.coordinates, this.numIOBlocks, numMovableBlocks);
    }
}
