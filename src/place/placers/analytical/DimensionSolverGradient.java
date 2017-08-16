package place.placers.analytical;

import java.util.Arrays;

class DimensionSolverGradient {

    private final double[] coordinates;

    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final double[] numPositiveNets, numNegativeNets;
    private final double halfMaxConnectionLength;

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
    private final String[] leafNode;

    DimensionSolverGradient(double[] coordinates, double maxConnectionLength, boolean[] fixed, String[] leafNode, double beta1, double beta2, double eps) {
        this.coordinates = coordinates;
        this.halfMaxConnectionLength = maxConnectionLength / 2;

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
        this.leafNode = leafNode;
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


    void processConnection(int firstIndex, int secondIndex, double coorDifference, double weight, boolean critical) {
    	if(coorDifference > 0.0){
    		this.addConnection(firstIndex, secondIndex, coorDifference, weight, critical);
    	}else if(coorDifference < 0.0){
    		this.addConnection(secondIndex, firstIndex, -coorDifference, weight, critical);
    	}
    }
    private void addConnection(int minIndex, int maxIndex, double coorDifference, double weight, boolean critical) {
    	double netSize;
    	if(critical){
            netSize = 2 * (5 * this.halfMaxConnectionLength) * coorDifference / ((5 * this.halfMaxConnectionLength) + coorDifference);
    	}else{
            netSize = 2 * this.halfMaxConnectionLength * coorDifference / (this.halfMaxConnectionLength + coorDifference);
            
        	if(this.leafNode[minIndex].equals(this.leafNode[maxIndex])){
        		//netSize *= 1.75;
        	}else{
        		//netSize *= 0.8;
        	}
    	}

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

    	double gradient;
    	if(direction > 0) {
    		gradient = this.totalPositiveNetSize[i] / this.numPositiveNets[i];
    	} else if(direction < 0) {
    		gradient = - this.totalNegativeNetSize[i] / this.numNegativeNets[i];
    	} else {
    		return;
    	}

    	if(this.legalIsSet){
        	gradient = (1 - this.pseudoWeight) * gradient + this.pseudoWeight * (this.legalCoordinates[i] - currentCoordinate);
        }

        this.momentum[i] = this.beta1 * this.momentum[i] + (1 - this.beta1) * gradient;
        this.speeds[i] = this.beta2 * this.speeds[i] + (1 - this.beta2) * gradient * gradient;

        this.coordinates[i] += this.stepSize * this.momentum[i] / (Math.sqrt(this.speeds[i]) + this.eps);
    }
}
