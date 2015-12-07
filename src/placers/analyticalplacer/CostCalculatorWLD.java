package placers.analyticalplacer;

import java.util.List;

class CostCalculatorWLD extends CostCalculator {

    private List<int[]> nets;

    CostCalculatorWLD(List<int[]> nets) {
        this.nets = nets;
    }


    @Override
    protected double calculate(boolean tmp) {
        double cost = 0.0;

        for(int[] blockIndexes : this.nets) {
            int numNetBlocks = blockIndexes.length;

            int initialBlockIndex = blockIndexes[0];
            double minX = getX(initialBlockIndex), minY = getY(initialBlockIndex);
            double maxX = minX, maxY = minY;

            for(int i = 1; i < numNetBlocks; i++) {
                int blockIndex = blockIndexes[i];
                double x = getX(blockIndex), y = getY(blockIndex);

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
