package placers.analyticalplacer;


class DimensionSolverGradient {

    private final double[] coordinates;

    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final int[] numPositiveNets, numNegativeNets;
    private final double stepSize;

    private double pseudoWeight;
    private int[] legalCoordinates;
    private boolean firstSolve;

    DimensionSolverGradient(double[] coordinates, double pseudoWeight, double stepSize) {
        this.coordinates = coordinates;
        this.pseudoWeight = pseudoWeight;
        this.stepSize = stepSize;

        int numBlocks = coordinates.length;

        this.directions = new double[numBlocks];
        this.numPositiveNets = new int[numBlocks];
        this.numNegativeNets = new int[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];

        this.firstSolve = true;
    }

    void setLegal(int[] legal) {
        this.legalCoordinates = legal;
        this.firstSolve = false;
    }

    void addConnectionMinMaxUnknown(int index1, int index2, double coorDifference, double weight) {
        if(coorDifference > 0) {
            this.addConnection(index1, index2, coorDifference, weight);
        } else {
            this.addConnection(index2, index1, -coorDifference, weight);
        }
    }
    void addConnection(int minIndex, int maxIndex, double coorDifference, double weight) {

        // TODO: platforming this net size works a bit better than not doing it
        // but the optimal maximal value isn't found yet
        double netSize = 20 * coorDifference / (10 + coorDifference);

        if(minIndex >= 0) {
            this.totalPositiveNetSize[minIndex] += netSize;
            this.numPositiveNets[minIndex] += 1;
            this.directions[minIndex] += weight;
        }

        if(maxIndex >= 0) {
            this.totalNegativeNetSize[maxIndex] += netSize;
            this.numNegativeNets[maxIndex] += 1;
            this.directions[maxIndex] -= weight;
        }
    }

    void solve() {
        int numBlocks = this.coordinates.length;

        for(int i = 0; i < numBlocks; i++) {
            /* This calculation is a bit complex. There are 3 coordinates at play:
             * - The current coordinate (C1)
             * - The "optimal" coordinate, based on the connected nets (N)
             * - The legal coordinate, based on the previous legalization iteration (L)
             *
             * We calculate a weighted coordinate W in between N and L, based on the
             * pseudoWeight P:
             * W = P * L + (1-P) * N
             *
             * Then we calculate the next coordinate C2 of this block, using step size S:
             * C2 = S * W + (1-S) * C1
             *
             * => C2 = (1-S)*C1 + S*(P*L + (1-P)*N)
             *
             * In place, we can rewrite as:
             * => C1 += S * (N + P*(L-N) - C1)
             */

            double direction = this.directions[i];
            double currentCoordinate = this.coordinates[i];

            double netGoal = currentCoordinate;
            if(direction > 0.000001) {
                netGoal += this.totalPositiveNetSize[i] / this.numPositiveNets[i];

            } else if(direction < -0.000001) {
                netGoal -= this.totalNegativeNetSize[i] / this.numNegativeNets[i];

            } else {
                continue;
            }

            if(this.firstSolve) {
                this.coordinates[i] += this.stepSize * (netGoal - currentCoordinate);

            } else {
                this.coordinates[i] += this.stepSize * (netGoal + this.pseudoWeight * (this.legalCoordinates[i] - netGoal) - currentCoordinate);
            }
        }
    }
}
