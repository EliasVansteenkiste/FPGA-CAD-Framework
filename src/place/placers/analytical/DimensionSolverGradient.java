package place.placers.analytical;

import java.util.Arrays;

class DimensionSolverGradient {
	private final DimensionForceGradient[] blockForces;
	
    private final double[] coordinates;
    private final int numBlocks;
    
    private final double[] speeds;
    private final double[] momentum;
    
    private double pseudoWeight, stepSize;
    
    private final double beta1;
    private final double beta2;
    private final double eps;
    
    private boolean legalIsSet = false;
    private double[] legalCoordinates;
    
    private final boolean[] fixed;

    DimensionSolverGradient(DimensionForceGradient[] blockForces, double[] coordinates, boolean[] fixed, double beta1, double beta2, double eps) {
        this.blockForces = blockForces;
        
    	this.coordinates = coordinates;
        this.numBlocks = coordinates.length;

        this.speeds = new double[this.numBlocks];
        this.momentum = new double[this.numBlocks];
        
        Arrays.fill(this.speeds, 0.0);
        Arrays.fill(this.momentum, 0.0);

        this.beta1 = beta1;
        this.beta2 = beta2;
        this.eps = eps;

        this.fixed = fixed;
    }

    void setLegal(double[] legal) {
        this.legalCoordinates = legal;
        this.legalIsSet = true;
    }
    void solve(double pseudoWeight, double stepSize) {
    	this.pseudoWeight = pseudoWeight;
    	this.stepSize = stepSize;

        for(int i = 0; i < this.numBlocks; i++) {
        	if(!this.fixed[i]){
        		this.doSolve(i);
        	}
        }
    }
    void doSolve(int i){
    	double direction = 0.0;
    	for(DimensionForceGradient dimensionForce:this.blockForces) {
    		direction += dimensionForce.getDirection(i);
    	}

    	double gradient;
    	if(direction > 0) {
    		double totalPositiveNetSize = 0.0;
    		double numPositiveNets = 0.0;
    		for(DimensionForceGradient dimensionForce:this.blockForces) {
    			totalPositiveNetSize += dimensionForce.getTotalPositiveNetSize(i);
    			numPositiveNets += dimensionForce.getNumPositiveNets(i);
    		}
    		gradient = totalPositiveNetSize / numPositiveNets;
    	} else if(direction < 0) {
    		double totalNegativeNetSize = 0.0;
    		double numNegativeNets = 0.0;
    		for(DimensionForceGradient dimensionForce:this.blockForces) {
    			totalNegativeNetSize += dimensionForce.getTotalNegativeNetSize(i);
    			numNegativeNets += dimensionForce.getNumNegativeNets(i);
    		}
    		gradient = - totalNegativeNetSize / numNegativeNets;
    	} else {
    		return;
    	}

    	if(this.legalIsSet){
        	gradient = (1.0 - this.pseudoWeight) * gradient + this.pseudoWeight * (this.legalCoordinates[i] - this.coordinates[i]);
        }

        this.momentum[i] = this.beta1 * this.momentum[i] + (1.0 - this.beta1) * gradient;
        this.speeds[i] = this.beta2 * this.speeds[i] + (1.0 - this.beta2) * gradient * gradient;

        this.coordinates[i] += this.stepSize * this.momentum[i] / (Math.sqrt(this.speeds[i]) + this.eps);
    }
}
