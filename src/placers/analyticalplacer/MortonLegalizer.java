package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.GlobalBlock;
import circuit.block.Site;

public class MortonLegalizer extends Legalizer {

    private double minX, minY, maxX, maxY;

    MortonLegalizer(
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
        this.getMinMaxXY(blocksStart, blocksEnd);

        this.fitBlocksToMatrix(blocksStart, blocksEnd, siteMatrix);
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

    private void getMinMaxXY(int blocksStart, int blocksEnd) {
        this.minX = this.linearX[blocksStart];
        this.minY = this.linearY[blocksStart];
        this.maxX = this.minX;
        this.maxY = this.minY;

        for(int blockIndex = blocksStart + 1; blockIndex < blocksEnd; blockIndex++) {
            double x = this.linearX[blockIndex];
            double y = this.linearY[blockIndex];

            if(x < this.minX) {
                this.minX = x;
            } else if(x > this.maxX) {
                this.maxX =x ;
            }

            if(y < this.minY) {
                this.minY = y;
            } else if(y > this.maxY) {
                this.maxY = y;
            }
        }
    }

    private void fitBlocksToMatrix(int blocksStart, int blocksEnd, List<List<Site>> sites) {
        int numSiteColumns = sites.size();
        int numSiteRows = sites.get(0).size();

        double multiplierX = (numSiteColumns - 1) / (this.maxX - this.minX);
        double multiplierY = (numSiteRows - 1) / (this.maxY - this.minY);

        for(int blockIndex = blocksStart; blockIndex < blocksEnd; blockIndex++) {
            int siteColumn = (int) ((this.linearX[blockIndex] - this.minX) * multiplierX);
            int siteRow = (int) ((this.linearY[blockIndex] - this.minY) * multiplierY);

            Site site = sites.get(siteColumn).get(siteRow);
            this.tmpLegalX[blockIndex] = site.getX();
            this.tmpLegalY[blockIndex] = site.getY();
        }
    }
}
