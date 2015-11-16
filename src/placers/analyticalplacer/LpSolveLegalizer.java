package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;

public class LpSolveLegalizer extends Legalizer {

    LpSolveLegalizer(
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
        // There are numBlocks*numSites binary variables: is a certain block mapped to a certain site?
        // For a given block and site, the variable index is given by (blockIndex * numSites + siteIndex).
        try {
            this.legalizeBlockTypeThrowing(blockType, blocksStart, blocksEnd);
        } catch(LpSolveException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void legalizeBlockTypeThrowing(BlockType blockType, int blocksStart, int blocksEnd) throws LpSolveException {

        int numBlocks = blocksEnd - blocksStart;

        List<AbstractSite> sites = this.circuit.getSites(blockType);
        int numSites = sites.size();

        int numVariables = numBlocks * numSites;
        int numConstraints = numBlocks + numSites;

        LpSolve solver = LpSolve.makeLp(numConstraints, numVariables);

        double[] costFunction = new double[numVariables + 1];
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            double blockX = this.linearX[blockIndex + blocksStart];
            double blockY = this.linearY[blockIndex + blocksStart];

            int variableIndex = blockIndex * numSites;
            for(AbstractSite site : sites) {
                int siteX = site.getX();
                int siteY = site.getY();

                double weight = Math.sqrt(Math.pow(blockX - siteX, 2) + Math.pow(blockY - siteY, 2));

                costFunction[variableIndex + 1] = weight;
                variableIndex++;
            }
        }

        solver.setObjFn(costFunction);


        for(int variableIndex = 0; variableIndex < numVariables; variableIndex++) {
            solver.setBinary(variableIndex + 1, true);
        }


        // Each block must be linked to exactly 1 site
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            double[] blockConstraint = new double[numVariables + 1];

            for(int siteIndex = 0; siteIndex < numSites; siteIndex++) {
                int variableIndex = blockIndex * numSites + siteIndex;
                blockConstraint[variableIndex + 1] = 1;
            }

            solver.addConstraint(blockConstraint, LpSolve.EQ, 1);
        }

        // Each site can contain at most 1 block
        for(int siteIndex = 0; siteIndex < numSites; siteIndex++) {
            double[] siteConstraint = new double[numVariables + 1];

            for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
                int variableIndex = blockIndex * numSites + siteIndex;
                siteConstraint[variableIndex + 1] = 1;
            }

            solver.addConstraint(siteConstraint, LpSolve.LE, 1);
        }


        solver.setVerbose(LpSolve.IMPORTANT);
        solver.setScalelimit(1);
        solver.setScaling(1024);

        solver.setPresolve(1, 0);
        solver.solve();

        double[] solution = solver.getPtrVariables();

        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            for(int siteIndex = 0; siteIndex < numSites; siteIndex ++) {

                int variableIndex = blockIndex * numSites + siteIndex;

                if(solution[variableIndex] > 0) {
                    AbstractSite legalSite = sites.get(siteIndex);
                    this.tmpLegalX[blockIndex + blocksStart] = legalSite.getX();
                    this.tmpLegalY[blockIndex + blocksStart] = legalSite.getY();
                    break;
                }
            }
        }
    }
}