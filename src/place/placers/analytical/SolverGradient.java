package place.placers.analytical;

class SolverGradient {

    private double[] coordinatesX, coordinatesY;
    private DimensionSolverGradient horizontalSolver, verticalSolver;

    SolverGradient(
    		NetAndCritWorker[] netWorkers,
            double[] coordinatesX,
            double[] coordinatesY,
            boolean[] fixed,
            double beta1,
            double beta2,
            double eps) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;
        
        DimensionForceGradient[] horizontalForces = new DimensionForceGradient[netWorkers.length];
        DimensionForceGradient[] verticalForces = new DimensionForceGradient[netWorkers.length];
        for(int i = 0; i < netWorkers.length; i++) {
        	horizontalForces[i] = netWorkers[i].getHorizontalForces();
        	verticalForces[i] = netWorkers[i].getVerticalForces();
        }
        this.horizontalSolver = new DimensionSolverGradient(horizontalForces, this.coordinatesX, fixed, beta1, beta2, eps);
        this.verticalSolver = new DimensionSolverGradient(verticalForces, this.coordinatesY, fixed, beta1, beta2, eps);
    }

    void solve(double anchorWeight, double learningRate) {
    	this.horizontalSolver.solve(anchorWeight, learningRate);
    	this.verticalSolver.solve(anchorWeight, learningRate);
    }
    
    void addPseudoConnections(double[] legalX, double[] legalY) {
    	this.horizontalSolver.setLegal(legalX);
    	this.verticalSolver.setLegal(legalY);
    }
}