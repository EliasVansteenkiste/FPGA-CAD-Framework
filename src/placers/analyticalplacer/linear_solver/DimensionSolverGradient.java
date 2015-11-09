package placers.analyticalplacer.linear_solver;



public class DimensionSolverGradient extends DimensionSolver {

    private final double[] coordinates;

    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final int[] numPositiveNets, numNegativeNets;
    private final double gradientSpeed;

    private double pseudoWeight;
    private double[] pseudoGoal;

    public DimensionSolverGradient(double[] coordinates, double gradientSpeed) {
        this.coordinates = coordinates;
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

        double difference = maxCoordinate - minCoordinate;
        double netSize = 20 * difference / (10 + difference);
        double weight = netSize == 0 ? 0 : weightMultiplier;

        if(isPseudoConnection) {
            this.pseudoWeight = weightMultiplier;

            if(minIndex >= 0) {
                this.pseudoGoal[minIndex] = maxCoordinate;
            }
            if(maxIndex >= 0) {
                this.pseudoGoal[maxIndex] = minCoordinate;
            }

        } else {
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
            double direction = this.directions[i];
            double netGoal = 0;

            if(direction > 0) {
                netGoal = this.coordinates[i] + this.totalPositiveNetSize[i] / this.numPositiveNets[i];

            } else if(direction < 0) {
                netGoal = this.coordinates[i] - this.totalNegativeNetSize[i] / this.numNegativeNets[i];
            }

            double newCoordinate = this.gradientSpeed * (((1 - this.pseudoWeight) * netGoal + this.pseudoWeight * this.pseudoGoal[i]) - this.coordinates[i]);
            this.coordinates[i] += this.gradientSpeed * (((1 - this.pseudoWeight) * netGoal + this.pseudoWeight * this.pseudoGoal[i]) - this.coordinates[i]);
        }
    }
}
