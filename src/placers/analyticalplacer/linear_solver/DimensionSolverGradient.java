package placers.analyticalplacer.linear_solver;



public class DimensionSolverGradient implements DimensionSolver {

    private final double[] coordinates;

    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final int[] numPositiveNets, numNegativeNets;
    private final double gradientSpeed;

    public DimensionSolverGradient(double[] coordinates, double gradientSpeed) {
        this.coordinates = coordinates;
        this.gradientSpeed = gradientSpeed;

        int numBlocks = coordinates.length;

        this.directions = new double[numBlocks];
        this.numPositiveNets = new int[numBlocks];
        this.numNegativeNets = new int[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
    }


    @Override
    public void addConnection(
            boolean minFixed, int minIndex, double minCoordinate,
            boolean maxFixed, int maxIndex, double maxCoordinate,
            double weightMultiplier) {

        if(minCoordinate > maxCoordinate) {
            boolean tmpFixed = minFixed;
            minFixed = maxFixed;
            maxFixed = tmpFixed;

            int tmpIndex = minIndex;
            minIndex = maxIndex;
            maxIndex = tmpIndex;

            double tmpCoordinate = minCoordinate;
            minCoordinate = maxCoordinate;
            maxCoordinate = tmpCoordinate;
        }

        //double netSize = 15 * Math.tanh((maxCoordinate - minCoordinate) / 15);
        double netSize = 20 * (maxCoordinate - minCoordinate) / (10 + maxCoordinate - minCoordinate);
        //double netSize = maxCoordinate - minCoordinate;
        double weight = netSize == 0 ? 0 : weightMultiplier;

        if(minIndex >= 0) {
            this.totalPositiveNetSize[minIndex] += netSize;
            this.numPositiveNets[minIndex] += 1;
            if(weight != 0) {
                this.directions[minIndex] += weight;
            }
        }

        if(maxIndex >= 0) {
            this.totalNegativeNetSize[maxIndex] += netSize;
            this.numNegativeNets[maxIndex] += 1;
            if(weight != 0) {
                this.directions[maxIndex] -= weight;
            }
        }
    }

    @Override
    public void solve() {
        int numBlocks = this.coordinates.length;
        //for(int i = this.numIOBlocks; i < numBlocks; i++) {
        for(int i = 0; i < numBlocks; i++) {
            double direction = this.directions[i];
            double force = 0;

            if(direction > 0) {
                force = this.totalPositiveNetSize[i] / this.numPositiveNets[i];

            } else if(direction < 0) {
                force = -this.totalNegativeNetSize[i] / this.numNegativeNets[i];
            }

            this.coordinates[i] += this.gradientSpeed * force;
        }
    }
}
