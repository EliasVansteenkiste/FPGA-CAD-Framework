package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import mathtools.HungarianAlgorithm;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;

public class BipartiteLegalizer extends Legalizer {

    BipartiteLegalizer(
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
        List<AbstractSite> sites = this.circuit.getSites(blockType);

        double[][] costMatrix = this.buildCostMatrix(sites, blocksStart, blocksEnd);

        HungarianAlgorithm solver = new HungarianAlgorithm(costMatrix);
        int[] solution = solver.execute();

        this.updateLegal(sites, solution, blocksStart, blocksEnd);
    }


    private double[][] buildCostMatrix(List<AbstractSite> sites, int blocksStart, int blocksEnd) {

        int numSites = sites.size();
        int numBlocks = blocksEnd - blocksStart;

        double[][] costMatrix = new double[numBlocks][numSites];
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            double linearX = this.linearX[blocksStart + blockIndex];
            double linearY = this.linearY[blocksStart + blockIndex];

            for(int siteIndex = 0; siteIndex < numSites; siteIndex++) {
                AbstractSite site = sites.get(siteIndex);
                int legalX = site.getX();
                int legalY = site.getY();

                costMatrix[blockIndex][siteIndex] = this.distance(linearX, linearY, legalX, legalY);
            }
        }

        return costMatrix;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        //return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }


    private void updateLegal(List<AbstractSite> sites, int[] solution, int blocksStart, int blocksEnd) {
        for(int blockIndex = blocksStart; blockIndex < blocksEnd; blockIndex++) {
            int siteIndex = solution[blockIndex - blocksStart];
            AbstractSite site = sites.get(siteIndex);

            this.tmpLegalX[blockIndex] = site.getX();
            this.tmpLegalY[blockIndex] = site.getY();
        }
    }
}
