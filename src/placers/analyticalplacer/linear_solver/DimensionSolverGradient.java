package placers.analyticalplacer.linear_solver;

import java.util.Arrays;
import util.Logger;


public class DimensionSolverGradient implements DimensionSolver {
    
    private final double[] coordinates;
    private final int numIOBlocks;
    
    private final double[] directions, totalPositiveNetSize, totalNegativeNetSize;
    private final int[] numPositiveNets, numNegativeNets;
    private final double gradientSpeed;
    
    public DimensionSolverGradient(double[] coordinates, int numIOBlocks, double gradientSpeed) {
        this.coordinates = coordinates;
        this.numIOBlocks = numIOBlocks;
        this.gradientSpeed = gradientSpeed;
        
        int numBlocks = coordinates.length;
        
        this.directions = new double[numBlocks];
        this.numPositiveNets = new int[numBlocks];
        this.numNegativeNets = new int[numBlocks];
        this.totalPositiveNetSize = new double[numBlocks];
        this.totalNegativeNetSize = new double[numBlocks];
        
        //Arrays.fill(this.totalPositiveNetSize, Double.MAX_VALUE);
        //Arrays.fill(this.totalNegativeNetSize, Double.MAX_VALUE);
    }
    
    
    @Override
    public void addConnection(
            boolean minFixed, int minIndex, double minCoordinate,
            boolean maxFixed, int maxIndex, double maxCoordinate,
            double weightMultiplier) {
        
        if(minCoordinate > maxCoordinate) {
            boolean tmpFixed = minFixed;
            minFixed = maxFixed;
            maxFixed = tmpFixed;
            
            int tmpIndex = minIndex;
            minIndex = maxIndex;
            maxIndex = tmpIndex;
            
            double tmpCoordinate = minCoordinate;
            minCoordinate = maxCoordinate;
            maxCoordinate = tmpCoordinate;
        }
        
        double netSize = maxCoordinate - minCoordinate;
        double weight = netSize == 0 ? 0 : weightMultiplier;
        

        if(!minFixed) {
            this.totalPositiveNetSize[minIndex] += netSize;
            this.numPositiveNets[minIndex] += 1;
            if(weight != 0) {
                //this.totalPositiveNetSize[minIndex] = Math.min(netSize, this.totalPositiveNetSize[minIndex]);
                this.directions[minIndex] += weight;
            }
            
        }
        
        if(!maxFixed) {
            this.totalNegativeNetSize[maxIndex] += netSize;
            this.numNegativeNets[maxIndex] += 1;
            if(weight != 0) {
                //this.totalNegativeNetSize[maxIndex] = Math.min(netSize, this.totalNegativeNetSize[maxIndex]);
                this.directions[maxIndex] -= weight;
            }
        }
    }
    
    @Override
    public void solve() {
        int numBlocks = this.coordinates.length;
        for(int i = this.numIOBlocks; i < numBlocks; i++) {
            double force = 0;
            
            if(this.directions[i] > 0) {
                force = this.totalPositiveNetSize[i] / this.numPositiveNets[i];
                //force = this.totalPositiveNetSize[i];
                
            } else if(this.directions[i] < 0) {
                force = -this.totalNegativeNetSize[i] / this.numNegativeNets[i];
                //force = -this.totalNegativeNetSize[i];
            }
            
            /*if(i == 400) {
                System.out.format("\n%f, %f, %f, %d, %f, %d",
                        this.coordinates[i], force,
                        this.totalPositiveNetSize[i], this.numPositiveNets[i],
                        this.totalNegativeNetSize[i], this.numNegativeNets[i]);
            }*/
            
            this.coordinates[i] += this.gradientSpeed * force;
        }
    }
}
