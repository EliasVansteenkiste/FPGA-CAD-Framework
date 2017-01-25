package place.placers.analytical;

import java.util.Arrays;


class DimensionSolverGradient {

    private final double[] coordinates;

    private double[] pullDirection, totalPositiveNetSize, totalNegativeNetSize;
    private double[] numPositiveNets, numNegativeNets;

    private double[] pushDirection, totalPositiveForce, totalNegativeForce;
    private double[] numPositiveForce, numNegativeForce;

    private final double halfMaxConnectionLength, speedAveraging;

    private final double stepSize;
    private double alpha;//Ratio between pulling and pushing force

    private final double[] speeds;

    private double pseudoWeight = 0;
    private boolean legalIsSet = false;
    private int[] legalCoordinates;

    private boolean[] fixed;

    DimensionSolverGradient(double[] coordinates, double stepSize, double maxConnectionLength, double speedAveraging, boolean[] fixed) {
        this.coordinates = coordinates;
        this.stepSize = stepSize;
        this.halfMaxConnectionLength = maxConnectionLength / 2;
        this.speedAveraging = speedAveraging;

        int numBlocks = coordinates.length;

        this.speeds = new double[numBlocks];

        this.pullDirection = new double[numBlocks];
        this.pushDirection = new double[numBlocks];

        this.numPositiveNets = new double[numBlocks];
        this.numNegativeNets = new double[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];

        this.numPositiveForce = new double[numBlocks];
        this.numNegativeForce = new double[numBlocks];
        this.totalPositiveForce = new double[numBlocks];
        this.totalNegativeForce = new double[numBlocks];
        
        this.fixed = fixed;
    }


    void initializeIteration(double pseudoWeight, double alpha) {
        this.pseudoWeight = pseudoWeight;
        this.alpha = alpha;

        Arrays.fill(this.pullDirection, 0.0);
        Arrays.fill(this.pushDirection, 0.0);

        Arrays.fill(this.numPositiveNets, 0.0);
        Arrays.fill(this.numNegativeNets, 0.0);

        Arrays.fill(this.totalPositiveNetSize, 0.0);
        Arrays.fill(this.totalNegativeNetSize, 0.0);

        Arrays.fill(this.numPositiveForce, 0.0);
        Arrays.fill(this.numNegativeForce, 0.0);

        Arrays.fill(this.totalPositiveForce, 0.0);
        Arrays.fill(this.totalNegativeForce, 0.0);
    }

    void setLegal(int[] legal) {
        this.legalCoordinates = legal;
        this.legalIsSet = true;
    }

    void addOverlapForce(int minIndex, int maxIndex, double force){
    	this.totalNegativeForce[minIndex] += force;
    	this.totalPositiveForce[maxIndex] += force;

    	this.numNegativeForce[minIndex] += 1;
    	this.numPositiveForce[maxIndex] += 1;

    	this.pushDirection[minIndex] -= force;
    	this.pushDirection[maxIndex] += force;
    }

    void addConnection(int minIndex, int maxIndex, double coorDifference, double weight) {
    	//Weight is proportional to the number of sinks in a net. Nets with more sinks result in a larger total wirelength decrease when shortened.
    	//Netsize is a pulling force based on a modified version of Hooke's law. The larger the distance, the larger the force.
        double netSize = 2 * this.halfMaxConnectionLength * coorDifference / (this.halfMaxConnectionLength + coorDifference);//Modified Hooke's law

        this.totalPositiveNetSize[minIndex] += weight * netSize;
        this.numPositiveNets[minIndex] += weight;
        this.pullDirection[minIndex] += weight;

        this.totalNegativeNetSize[maxIndex] += weight * netSize;
        this.numNegativeNets[maxIndex] += weight;
        this.pullDirection[maxIndex] -= weight;
    }

    void solve() {
        int numBlocks = this.coordinates.length;

        for(int i = 0; i < numBlocks; i++) {
        	if(!this.fixed[i]){
        		this.doSolve(i);
        	}
        }
    }
    void doSolve(int i){
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

    	double pullDirection = this.pullDirection[i];
    	double pushDirection = this.pushDirection[i];

    	double currentCoordinate = this.coordinates[i];
    	double netGoal = currentCoordinate;

    	if(pullDirection == 0.0 && pushDirection == 0.0){
    		return; //There are no forces on the considered block
    	}

    	//Pulling force
    	if(pullDirection > 0.0) {
    		netGoal += (1-this.alpha) * (this.totalPositiveNetSize[i] / this.numPositiveNets[i]);
    	} else if(pullDirection < 0.0) {
    		netGoal -= (1-this.alpha) * (this.totalNegativeNetSize[i] / this.numNegativeNets[i]);
    	}

    	//Pushing force
    	if(pushDirection > 0.0) {
    		netGoal += this.alpha * (this.totalPositiveForce[i] / this.numPositiveForce[i]);
    	} else if(pushDirection < 0.0) {
    		netGoal -= this.alpha * (this.totalNegativeForce[i] / this.numNegativeForce[i]);
    	}

    	double newSpeed;
    	if(this.legalIsSet) {
    		newSpeed = this.stepSize * (netGoal + this.pseudoWeight * (this.legalCoordinates[i] - netGoal) - currentCoordinate);
    	} else {
    		newSpeed = this.stepSize * (netGoal - currentCoordinate);
    	}

    	this.speeds[i] = this.speedAveraging * this.speeds[i] + (1 - this.speedAveraging) * newSpeed;
    	this.coordinates[i] += this.speeds[i];
    }
}