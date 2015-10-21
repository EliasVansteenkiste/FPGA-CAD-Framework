package placers.analyticalplacer.linear_solver;


public class DimensionSolverGradient implements DimensionSolver {
    
    private final double[] coordinates;
    private final int numIOBlocks;
    
    private final double[] totalPositiveNetSize, totalNegativeNetSize;
    private final int[] directions, numPositiveNets, numNegativeNets;
    private final double gradientSpeed;
    
    public DimensionSolverGradient(double[] coordinates, int numIOBlocks, double gradientSpeed) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;
        this.gradientSpeed = gradientSpeed;
        
        int numBlocks = coordinates.length;
        
        this.directions = new int[numBlocks];
        this.numPositiveNets = new int[numBlocks];
        this.numNegativeNets = new int[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
    }
    
    
    
    @Override
    public void addConnection(
            boolean fixed1, int index1, double coordinate1,
            boolean fixed2, int index2, double coordinate2,
            double weightMultiplier) {
        
        double netSize = Math.abs(coordinate2 - coordinate1);
        double weight = weightMultiplier * Math.signum(coordinate2 - coordinate1);
        

        if(!fixed1) {
            if(weight > 0) {
                this.totalPositiveNetSize[index1] += netSize;
                this.numPositiveNets[index1] += 1;

            } else {
                this.totalNegativeNetSize[index1] += netSize;
                this.numNegativeNets[index1] += 1;
            }
            
            this.directions[index1] += weight;
        }
        
        if(!fixed2) {
            if(weight < 0) {
                this.totalPositiveNetSize[index2] += netSize;
                this.numPositiveNets[index2] += 1;

            } else {
                this.totalNegativeNetSize[index2] += netSize;
                this.numNegativeNets[index2] += 1;
            }
            
            this.directions[index2] -= weight;
        }
    }
    
    @Override
    public void solve() {
        int numBlocks = coordinates.length;
        for(int i = this.numIOBlocks; i < numBlocks; i++) {
            double force;
            if(this.directions[i] > 0) {
                force = this.totalPositiveNetSize[i] / this.numPositiveNets[i];
            } else {
                force = -this.totalNegativeNetSize[i] / this.numNegativeNets[i];
            }
            this.coordinates[i] += this.gradientSpeed * force;
        }
    }
}
