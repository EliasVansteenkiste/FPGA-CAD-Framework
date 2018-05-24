package place.placers.analytical;

import java.util.Arrays;

class DimensionForceGradient {
    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final double[] numPositiveNets, numNegativeNets;
    private final double halfMaxConnectionLength;

    DimensionForceGradient(int numBlocks, double maxConnectionLength) {
        this.halfMaxConnectionLength = maxConnectionLength / 2;

        this.directions = new double[numBlocks];
        this.numPositiveNets = new double[numBlocks];
        this.numNegativeNets = new double[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
    }

    void initializeIteration() {
        Arrays.fill(this.directions, 0.0);

        Arrays.fill(this.numPositiveNets, 0.0);
        Arrays.fill(this.numNegativeNets, 0.0);

        Arrays.fill(this.totalPositiveNetSize, 0.0);
        Arrays.fill(this.totalNegativeNetSize, 0.0);
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
    	}
    	
    	if(weight < 0) System.out.println(weight);//TODO REMOVE

        this.totalPositiveNetSize[minIndex] += weight * netSize;
        this.numPositiveNets[minIndex] += weight;
        this.directions[minIndex] += weight;
        
        this.totalNegativeNetSize[maxIndex] += weight * netSize;
        this.numNegativeNets[maxIndex] += weight;
        this.directions[maxIndex] -= weight;
    }
    
    public double getDirection(int i) {
    	return this.directions[i];
    }
    public double getTotalPositiveNetSize(int i) {
    	return this.totalPositiveNetSize[i];
    }
    public double getTotalNegativeNetSize(int i) {
    	return this.totalNegativeNetSize[i];
    }
    public double getNumPositiveNets(int i) {
    	return this.numPositiveNets[i];
    }
    public double getNumNegativeNets(int i) {
    	return this.numNegativeNets[i];
    }
}
