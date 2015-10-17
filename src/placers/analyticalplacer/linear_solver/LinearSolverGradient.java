package placers.analyticalplacer.linear_solver;


public class LinearSolverGradient implements LinearSolver {
    
    private double[] coordinates;
    private double gradientSpeed;
    
    public LinearSolverGradient(double[] linear, int numIOBlocks, double gradientSpeed) {
        this.gradientSpeed = gradientSpeed;
        
        int numBlocks = linear.length;
        int numMovableBlocks = numBlocks - numIOBlocks;
    
        this.coordinates = new double[numBlocks];
        System.arraycopy(linear, numIOBlocks, this.coordinates, numIOBlocks, numMovableBlocks);        
    }
    
    @Override
    public void addConnection(
            boolean fixed1, int index1, double coordinate1,
            boolean fixed2, int index2, double coordinate2,
            double weightMultiplier) {
        
        double weight = this.gradientSpeed * weightMultiplier * (coordinate2 - coordinate1) / (0.5 + Math.abs(coordinate1 - coordinate2));

        if(!fixed1) {
            this.coordinates[index1] += weight;
        }
        if(!fixed2) {
            this.coordinates[index2] -= weight;
        }
    }
    
    @Override
    public double[] solve() {
        return this.coordinates;
    }
}
