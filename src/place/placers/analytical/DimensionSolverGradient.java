package place.placers.analytical;

import java.util.Arrays;


class DimensionSolverGradient {

    private final double[] coordinates;

    private double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private double[] numPositiveNets, numNegativeNets;
    private final double halfMaxConnectionLength, speedAveraging;

    private double stepSize;

    private final double[] speeds;
    private final double[] momentum;
    private final double beta1;
    private final double beta2;
    private final double eps;

    private double pseudoWeight = 0;
    private boolean legalIsSet = false;
    private int[] legalCoordinates;
    
    private boolean[] fixed;
    
    private final boolean useAdam;

    DimensionSolverGradient(double[] coordinates, double maxConnectionLength, double speedAveraging, boolean[] fixed, double beta1, double beta2, double eps) {
        this.coordinates = coordinates;
        this.halfMaxConnectionLength = maxConnectionLength / 2;
        this.speedAveraging = speedAveraging;

        int numBlocks = coordinates.length;

        this.speeds = new double[numBlocks];
        this.momentum = new double[numBlocks];
        
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.eps = eps;

        this.directions = new double[numBlocks];
        this.numPositiveNets = new double[numBlocks];
        this.numNegativeNets = new double[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
        
        this.fixed = fixed;
        
        this.useAdam = true;
    }

    void initializeIteration(double pseudoWeight, double learningRate) {
        this.pseudoWeight = pseudoWeight;
        this.stepSize = learningRate;

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

        this.totalPositiveNetSize[minIndex] += weight * netSize;
        this.numPositiveNets[minIndex] += weight;
        this.directions[minIndex] += weight;

        this.totalNegativeNetSize[maxIndex] += weight * netSize;
        this.numNegativeNets[maxIndex] += weight;
        this.directions[maxIndex] -= weight;
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
    	double direction = this.directions[i];
    	double currentCoordinate = this.coordinates[i];

    	double netGoal = currentCoordinate;
    	if(direction > 0) {
    		netGoal += this.totalPositiveNetSize[i] / this.numPositiveNets[i];
    	} else if(direction < 0) {
    		netGoal -= this.totalNegativeNetSize[i] / this.numNegativeNets[i];
    	} else {
    		return;
    	}
    	
    	double gradient;
    	if(this.legalIsSet){
        	gradient = netGoal + this.pseudoWeight * (this.legalCoordinates[i] - netGoal) - currentCoordinate;
        }else{
        	gradient = netGoal - currentCoordinate;
        }

    	if(this.useAdam){
        	this.momentum[i] = this.beta1 * this.momentum[i] + (1 - this.beta1) * gradient;
        	this.speeds[i] = this.beta2 * this.speeds[i] + (1 - this.beta2) * gradient * gradient;

        	this.coordinates[i] += this.stepSize * this.momentum[i] / (Math.sqrt(this.speeds[i]) + this.eps);
    	}else{
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
    		
        	double newSpeed = this.stepSize * gradient;
        	this.speeds[i] = this.speedAveraging * this.speeds[i] + (1 - this.speedAveraging) * newSpeed;
        	this.coordinates[i] += this.speeds[i];
    	}   	
    }
}