package place.placers.analytical;

import java.util.List;

import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

public class CostCalculator {
	private List<Net> nets;
    private double[] doubleX, doubleY;

    CostCalculator(List<Net> nets) {
        this.nets = nets;
    }

    double calculate(double[] doubleX, double[] doubleY) {
        this.doubleX = doubleX;
        this.doubleY = doubleY;
        
        double cost = 0.0;

        for(Net net : this.nets) {
            int numNetBlocks = net.blocks.length;

            NetBlock initialBlock = net.blocks[0];
            int initialBlockIndex = initialBlock.blockIndex;
            double minX = this.doubleX[initialBlockIndex],
                   minY = this.doubleY[initialBlockIndex] + initialBlock.offset;
            double maxX = minX,
                   maxY = minY;

            for(int i = 1; i < numNetBlocks; i++) {
                NetBlock block = net.blocks[i];
                int blockIndex = block.blockIndex;
                double x = this.doubleX[blockIndex],
                       y = this.doubleY[blockIndex] + block.offset;

                if(x < minX) {
                    minX = x;
                } else if(x > maxX) {
                    maxX = x;
                }

                if(y < minY) {
                    minY = y;
                } else if(y > maxY) {
                    maxY = y;
                }
            }

            cost += ((maxX - minX + 1) + (maxY - minY + 1)) * AnalyticalAndGradientPlacer.getWeight(numNetBlocks);
        }

        return cost / 100;
    }
}
