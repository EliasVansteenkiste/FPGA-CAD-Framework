package placers.analyticalplacer.linear_solver;

import placers.analyticalplacer.AnalyticalPlacer;


public class LinearSolverGradient extends LinearSolver {
    
    public LinearSolverGradient(double[] coordinatesX, double[] coordinatesY, int numIOBlocks, double stepSize) {
        super(coordinatesX, coordinatesY, numIOBlocks);
        
        this.solverX = new DimensionSolverGradient(coordinatesX, numIOBlocks, stepSize);
        this.solverY = new DimensionSolverGradient(coordinatesX, numIOBlocks, stepSize);
    }
    
    @Override
    public void processNet(int[] blockIndexes) {
        int numNetBlocks = blockIndexes.length;
        
        double weightMultiplier = AnalyticalPlacer.getWeight(numNetBlocks);
        
        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = blockIndexes[0], blockIndex2 = blockIndexes[1];
            boolean fixed1 = isFixed(blockIndex1), fixed2 = isFixed(blockIndex2);
            
            this.solverX.addConnection(fixed1, blockIndex1, this.coordinatesX[blockIndex1], fixed2, blockIndex2, this.coordinatesX[blockIndex2], weightMultiplier);
            this.solverY.addConnection(fixed1, blockIndex1, this.coordinatesY[blockIndex1], fixed2, blockIndex2, this.coordinatesY[blockIndex2], weightMultiplier);
            
            return;
        }
        
        
		// For bigger nets, we have to find the min and max block
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
			   minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		int minXIndex = -1, maxXIndex = -1,
			minYIndex = -1, maxYIndex = -1;
		
		for(int i = 0; i < numNetBlocks; i++) {
			int blockIndex = blockIndexes[i];
			double x = this.coordinatesX[blockIndex], y = this.coordinatesY[blockIndex];
			
			if(x < minX) {
				minX = x;
				minXIndex = blockIndex;
			}
			if(x > maxX) {
				maxX = x;
				maxXIndex = blockIndex;
			}
			
			if(y < minY) {
				minY = y;
				minYIndex = blockIndex;
			}
			if(y > maxY) {
				maxY = y;
				maxYIndex = blockIndex;
			}
		}
		
        
		boolean minXFixed = isFixed(minXIndex),maxXFixed = isFixed(maxXIndex),
				minYFixed = isFixed(minYIndex),maxYFixed = isFixed(maxYIndex);
		
        // Add connections between the min and max block
        this.solverX.addConnection(
                    minXFixed, minXIndex, minX,
                    maxXFixed, maxXIndex, maxX,
                    weightMultiplier);
            
        this.solverY.addConnection(
                minYFixed, minYIndex, minY,
                maxYFixed, maxYIndex, maxY,
                weightMultiplier);
    }
    
    
    public void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
}
