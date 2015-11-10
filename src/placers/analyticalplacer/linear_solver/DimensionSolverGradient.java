package placers.analyticalplacer.linear_solver;



public class DimensionSolverGradient extends DimensionSolver {

    private final double[] coordinates;

    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final int[] numPositiveNets, numNegativeNets;
    private final double gradientSpeed;

    private double pseudoWeight;
    private double[] pseudoGoal;

    public DimensionSolverGradient(double[] coordinates, double pseudoWeight, double gradientSpeed) {
        this.coordinates = coordinates;
        this.pseudoWeight = pseudoWeight;
        this.gradientSpeed = gradientSpeed;

        int numBlocks = coordinates.length;

        this.directions = new double[numBlocks];
        this.numPositiveNets = new int[numBlocks];
        this.numNegativeNets = new int[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];

        this.pseudoGoal = new double[numBlocks];
    }


    @Override
    public void addConnection(
            boolean minFixed, int minIndex, double minCoordinate,
            boolean maxFixed, int maxIndex, double maxCoordinate,
            double weightMultiplier, boolean isPseudoConnection) {

        if(isPseudoConnection) {
            if(minIndex >= 0) {
                this.pseudoGoal[minIndex] = maxCoordinate;
            }
            if(maxIndex >= 0) {
                this.pseudoGoal[maxIndex] = minCoordinate;
            }

        } else {

            double difference = maxCoordinate - minCoordinate;
            double netSize = 20 * difference / (10 + difference);
            double weight = netSize == 0 ? 0 : weightMultiplier;

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
    }

    @Override
    public void solve() {
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
             * Then we calculate the next coordinate C2 of this block, using gradient speed G:
             * C2 = G * W + (1-G) * C1
             *
             * -> C2 = (1-G)*C1 + G*(P*L + (1-P)*N)
             */

            double direction = this.directions[i];
            double netGoal = this.coordinates[i];
            if(direction > 0) {
                netGoal += this.totalPositiveNetSize[i] / this.numPositiveNets[i];

            } else if(direction < 0) {
                netGoal -= this.totalNegativeNetSize[i] / this.numNegativeNets[i];
            }

            this.coordinates[i] += this.gradientSpeed * (netGoal + this.pseudoWeight * (this.pseudoGoal[i] - netGoal) - this.coordinates[i]);
        }
    }
}
