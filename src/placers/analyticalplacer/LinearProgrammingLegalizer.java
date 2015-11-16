package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;

public class LinearProgrammingLegalizer extends Legalizer {

    LinearProgrammingLegalizer(
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

        int numBlocks = blocksEnd - blocksStart;

        List<AbstractSite> sites = this.circuit.getSites(blockType);
        int numSites = sites.size();

        int numVariables = numBlocks * numSites;

        List<Variable> variables = new ArrayList<Variable>(numVariables);

        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            double blockX = this.linearX[blockIndex + blocksStart];
            double blockY = this.linearY[blockIndex + blocksStart];

            for(AbstractSite site : sites) {
                int siteX = site.getX();
                int siteY = site.getY();

                String variableName = String.format("%d (%d,%d)", blockIndex, siteX, siteY);
                double weight = Math.sqrt(Math.pow(blockX - siteX, 2) + Math.pow(blockY - siteY, 2));

                Variable variable = new Variable(variableName).binary().weight(weight);
                variables.add(variable);
            }
        }

        ExpressionsBasedModel optimizationModel = new ExpressionsBasedModel();
        optimizationModel.addVariables(variables);

        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            String expressionName = String.format("Block %d", blockIndex);
            Expression expression = optimizationModel.addExpression(expressionName).level(1);

            List<Variable> blockVariables = variables.subList(blockIndex * numSites, (blockIndex+1) * numSites);
            expression.setLinearFactorsSimple(blockVariables);
        }

        for(int siteIndex = 0; siteIndex < numSites; siteIndex++) {
            String expressionName = String.format("Site %d", siteIndex);
            Expression expression = optimizationModel.addExpression(expressionName).lower(0).upper(1);

            List<Variable> siteVariables = new ArrayList<Variable>(numBlocks);
            for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
                siteVariables.add(variables.get(blockIndex * numSites + siteIndex));
            }

            expression.setLinearFactorsSimple(siteVariables);
        }

        Optimisation.Result optimizationResult = optimizationModel.minimise();
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            for(int siteIndex = 0; siteIndex < numSites; siteIndex ++) {

                int variableIndex = blockIndex * numSites + siteIndex;

                if(optimizationResult.doubleValue(variableIndex) > 0) {
                    AbstractSite legalSite = sites.get(siteIndex);
                    this.tmpLegalX[blockIndex + blocksStart] = legalSite.getX();
                    this.tmpLegalY[blockIndex + blocksStart] = legalSite.getY();
                    break;
                }
            }
        }
    }
}