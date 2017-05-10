package place.placers.analytical;

import java.util.Arrays;


class DimensionSolverGradient {

    private final double[] coordinates;

    private double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private double[] numPositiveNets, numNegativeNets;
    private final double halfMaxConnectionLength;

    private double oldStepSize, stepSize;

    private final double[] speeds;
    private final double[] momentum;
    private final double beta1;
    private final double beta2;
    private final double eps;

    private double pseudoWeight = 0;
    private boolean legalIsSet = false;
    private int[] legalCoordinates;
    
    private int iteration;
    
    private boolean[] fixed;

    DimensionSolverGradient(double[] coordinates, double stepSize, double maxConnectionLength, double speedAveraging, boolean[] fixed) {
        this.coordinates = coordinates;
        this.oldStepSize = stepSize;
        this.halfMaxConnectionLength = maxConnectionLength / 2;

        int numBlocks = coordinates.length;

        this.speeds = new double[numBlocks];
        this.momentum = new double[numBlocks];
        
        this.beta1 = 0.9;
        this.beta2 = 0.999;
        this.eps = 1e-8;

        this.directions = new double[numBlocks];
        this.numPositiveNets = new double[numBlocks];
        this.numNegativeNets = new double[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
        
        this.fixed = fixed;
    }

    void initialize(){
    	this.iteration = 0;
    	this.stepSize = this.oldStepSize;
    }
    void initializeIteration(double pseudoWeight) {
        this.pseudoWeight = pseudoWeight;

        Arrays.fill(this.directions, 0.0);

        Arrays.fill(this.numPositiveNets, 0);
        Arrays.fill(this.numNegativeNets, 0);

        Arrays.fill(this.totalPositiveNetSize, 0.0);
        Arrays.fill(this.totalNegativeNetSize, 0.0);
        
        this.iteration++;
        
        if(this.iteration > 5){
        	this.stepSize *= 0.95;
        }
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
    	if(direction > 0){
    		netGoal += this.totalPositiveNetSize[i] / this.numPositiveNets[i];
    	}else if(direction < 0){
    		netGoal -= this.totalNegativeNetSize[i] / this.numNegativeNets[i];
    	}else{
    		return;
    	}

    	double g;
    	if(this.legalIsSet){
        	g = netGoal + this.pseudoWeight * (this.legalCoordinates[i] - netGoal) - currentCoordinate;
        }else{
        	g = netGoal - currentCoordinate;
        }
    	
    	this.momentum[i] = this.beta1 * this.momentum[i] + (1 - this.beta1) * g;
    	this.speeds[i] = this.beta2 * this.speeds[i] + (1 - this.beta2) * g * g;

    	this.coordinates[i] += this.stepSize * this.momentum[i] / (Math.sqrt(this.speeds[i]) + this.eps);
    }
}