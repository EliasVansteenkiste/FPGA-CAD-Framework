package placers.analytical;

import java.util.Arrays;


class DimensionSolverGradient {

    private final double[] coordinates;

    private double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private int[] numPositiveNets, numNegativeNets;
    private final double halfMaxConnectionLength, speedAveraging, stepSize;

    private final double[] speeds;

    private double pseudoWeight = 0;
    private boolean legalIsSet = false;
    private int[] legalCoordinates;


    DimensionSolverGradient(double[] coordinates, double stepSize, double maxConnectionLength, double speedAveraging) {
        this.coordinates = coordinates;
        this.stepSize = stepSize;
        this.halfMaxConnectionLength = maxConnectionLength / 2;
        this.speedAveraging = speedAveraging;

        int numBlocks = coordinates.length;

        this.speeds = new double[numBlocks];

        this.directions = new double[numBlocks];
        this.numPositiveNets = new int[numBlocks];
        this.numNegativeNets = new int[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
    }


    void initializeIteration(double pseudoWeight) {
        this.pseudoWeight = pseudoWeight;

        Arrays.fill(this.directions, 0.0);

        Arrays.fill(this.numPositiveNets, 0);
        Arrays.fill(this.numNegativeNets, 0);

        Arrays.fill(this.totalPositiveNetSize, 0.0);
        Arrays.fill(this.totalNegativeNetSize, 0.0);
    }

    void setLegal(int[] legal) {
        this.legalCoordinates = legal;
        this.legalIsSet = true;
    }


    void addConnection(int minIndex, int maxIndex, double coorDifference, double weight) {

        double netSize = 2 * this.halfMaxConnectionLength * coorDifference / (this.halfMaxConnectionLength + coorDifference);

        this.totalPositiveNetSize[minIndex] += netSize;
        this.numPositiveNets[minIndex] += 1;
        this.directions[minIndex] += weight;

        this.totalNegativeNetSize[maxIndex] += netSize;
        this.numNegativeNets[maxIndex] += 1;
        this.directions[maxIndex] -= weight;
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
            if(direction > 0) {
                netGoal += this.totalPositiveNetSize[i] / this.numPositiveNets[i];

            } else if(direction < 0) {
                netGoal -= this.totalNegativeNetSize[i] / this.numNegativeNets[i];

            } else {
                continue;
            }

            /*if(this.firstSolve) {
                this.coordinates[i] += this.stepSize * (netGoal - currentCoordinate);

            } else {
                this.coordinates[i] += this.stepSize * (netGoal + this.pseudoWeight * (this.legalCoordinates[i] - netGoal) - currentCoordinate);
            }*/

            double newSpeed;
            if(this.legalIsSet) {
                newSpeed = this.stepSize * (netGoal + this.pseudoWeight * (this.legalCoordinates[i] - netGoal) - currentCoordinate);

            } else {
                newSpeed = this.stepSize * (netGoal - currentCoordinate);
            }

            this.speeds[i] = (1 - this.speedAveraging) * this.speeds[i] + this.speedAveraging * newSpeed;
            this.coordinates[i] += this.speeds[i];
        }
    }
}
