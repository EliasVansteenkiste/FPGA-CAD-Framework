package place.placers.analytical;

import java.util.List;

import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

class CostCalculatorWLD extends CostCalculator {

    private List<Net> nets;

    CostCalculatorWLD(List<Net> nets) {
        this.nets = nets;
    }


    @Override
    protected double calculate(boolean tmp) {
        double cost = 0.0;
//        System.out.println(this.nets.size());

        for(Net net : this.nets) {
            int numNetBlocks = net.blocks.length;

            NetBlock initialBlock = net.blocks[0];
            int initialBlockIndex = initialBlock.blockIndex;
            double minX = getX(initialBlockIndex),
                   minY = getY(initialBlockIndex) + initialBlock.offset;
            double maxX = minX,
                   maxY = minY;

            for(int i = 1; i < numNetBlocks; i++) {
                NetBlock block = net.blocks[i];
                int blockIndex = block.blockIndex;
                double x = getX(blockIndex),
                       y = getY(blockIndex) + block.offset;

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
