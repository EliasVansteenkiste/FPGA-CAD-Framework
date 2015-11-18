package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import util.Pair;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.GlobalBlock;
import circuit.block.Site;

public class SortingLegalizer extends Legalizer {
    SortingLegalizer(
            Circuit circuit,
            CostCalculator costCalculator,
            Map<GlobalBlock, Integer> blockIndexes,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY) {

        super(circuit, costCalculator, blockIndexes, blockTypes, blockTypeIndexStarts, linearX, linearY);
    }

    @Override
    protected void legalizeBlockType(double tileCapacity, BlockType blockType, int blocksStart, int blocksEnd) {
        List<List<Site>> siteMatrix = this.buildSiteMatrix(blockType);

        int numBlocks = blocksEnd - blocksStart;

        List<Pair<Double, Integer>> blocksX = new ArrayList<>(numBlocks);
        List<Pair<Double, Integer>> blocksY = new ArrayList<>(numBlocks);

        for(int blockIndex = blocksStart; blockIndex < blocksEnd; blockIndex++) {
            double x = this.linearX[blockIndex];
            double y = this.linearY[blockIndex];

            blocksX.add(new Pair<Double, Integer>(x, blockIndex));
            blocksY.add(new Pair<Double, Integer>(y, blockIndex));
        }

        Collections.sort(blocksX);
        Collections.sort(blocksY);

        double multiplierX = siteMatrix.size() / (numBlocks + 1.0);
        double multiplierY = siteMatrix.get(0).size() / (numBlocks + 1.0);

        for(int i = 0; i < numBlocks; i++) {
            int blockIndexX = blocksX.get(i).getValue();
            int x = siteMatrix.get((int) (i * multiplierX)).get(0).getX();
            this.tmpLegalX[blockIndexX] = x;

            int blockIndexY = blocksY.get(i).getValue();
            int y = siteMatrix.get(0).get((int) (i * multiplierY)).getY();
            this.tmpLegalY[blockIndexY] = y;
        }
    }


    private List<List<Site>> buildSiteMatrix(BlockType blockType) {
        List<List<Site>> siteMatrix = new ArrayList<List<Site>>();

        for(int column = 1; column < this.width - 1; column++) {
            if(this.circuit.getColumnType(column).equals(blockType)) {
                List<Site> siteColumn = new ArrayList<Site>();

                for(int row = 1; row < this.height - 1; row++) {
                    Site site = (Site) this.circuit.getSite(column, row, true);
                    if(site != null) {
                        siteColumn.add(site);
                    }
                }

                siteMatrix.add(siteColumn);
            }
        }

        return siteMatrix;
    }
}
