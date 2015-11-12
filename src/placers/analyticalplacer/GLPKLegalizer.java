package placers.analyticalplacer;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;

public class GLPKLegalizer extends Legalizer {

    GLPKLegalizer(
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
    void legalizeBlockType(double tileCapacity, BlockType blockType, int blocksStart, int blocksEnd) {
        // There are numBlocks*numSites binary variables: is a certain block mapped to a certain site?
        // For a given block and site, the variable index is given by (blockIndex * numSites + siteIndex).

        int numBlocks = blocksEnd - blocksStart;

        List<AbstractSite> sites = this.circuit.getSites(blockType);
        int numSites = sites.size();

        int numVariables = numBlocks * numSites;
        int numConstraints = 2*numBlocks + numSites;

        // Create solver
        glp_prob lp = GLPK.glp_create_prob();
        GLPK.glp_set_obj_dir (lp, GLPKConstants.GLP_MIN);
        GLPK.glp_add_cols(lp, numVariables);
        GLPK.glp_add_rows(lp, numConstraints);


        // Define columns and objective function
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            double blockX = this.linearX[blockIndex + blocksStart];
            double blockY = this.linearY[blockIndex + blocksStart];

            int variableIndex = blockIndex * numSites;
            for(AbstractSite site : sites) {
                int siteX = site.getX();
                int siteY = site.getY();

                double weight = Math.sqrt(Math.pow(blockX - siteX, 2) + Math.pow(blockY - siteY, 2));

                GLPK.glp_set_col_kind(lp, variableIndex + 1, GLPKConstants.GLP_BV);
                GLPK.glp_set_obj_coef(lp, variableIndex + 1, weight);
                variableIndex++;
            }
        }


        // Define constraints

        int constraintIndex = 0;

        // Each block must be linked to exactly 1 site
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            SWIGTYPE_p_int ind = GLPK.new_intArray(numSites + 1);
            SWIGTYPE_p_double val = GLPK.new_doubleArray(numSites + 1);

            for(int siteIndex = 0; siteIndex < numSites; siteIndex++) {
                int variableIndex = blockIndex * numSites + siteIndex;
                int variableWeight = 1;

                GLPK.intArray_setitem(ind, siteIndex + 1, variableIndex + 1);
                GLPK.doubleArray_setitem(val, siteIndex + 1, variableWeight);
            }

            GLPK.glp_set_row_bnds(lp, constraintIndex + 1, GLPKConstants.GLP_FX, 1, 1);
            GLPK.glp_set_mat_row(lp, constraintIndex + 1, numSites, ind, val);

            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);

            constraintIndex++;
        }

        // Each site can contain at most 1 block
        for(int siteIndex = 0; siteIndex < numSites; siteIndex++) {
            SWIGTYPE_p_int ind = GLPK.new_intArray(numBlocks + 1);
            SWIGTYPE_p_double val = GLPK.new_doubleArray(numBlocks + 1);

            for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
                int variableIndex = blockIndex * numSites + siteIndex;
                int variableWeight = 1;

                GLPK.intArray_setitem(ind, blockIndex + 1, variableIndex + 1);
                GLPK.doubleArray_setitem(val, blockIndex + 1, variableWeight);
            }

            GLPK.glp_set_row_bnds(lp, constraintIndex + 1, GLPKConstants.GLP_DB, 0, 1);
            GLPK.glp_set_mat_row(lp, constraintIndex + 1, numBlocks, ind, val);

            GLPK.delete_intArray(ind);
            GLPK.delete_doubleArray(val);

            constraintIndex++;
        }


        // Solve
        glp_iocp param_iocp = new glp_iocp();
        GLPK.glp_init_iocp(param_iocp);
        param_iocp.setPresolve(GLPKConstants.GLP_ON);
        param_iocp.setMsg_lev(GLPKConstants.GLP_MSG_OFF);

        int ret = GLPK.glp_intopt(lp, param_iocp);

        if(ret != 0) {
            System.exit(1);
        }


        double[] solution = new double[numVariables];
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            for(int siteIndex = 0; siteIndex < numSites; siteIndex ++) {
                int variableIndex = blockIndex * numSites + siteIndex;
                solution[variableIndex] = GLPK.glp_mip_col_val(lp, variableIndex + 1);
            }
        }

        // Update tmpLegal arrays
        int variableIndex = 0;
        for(int blockIndex = 0; blockIndex < numBlocks; blockIndex++) {
            for(int siteIndex = 0; siteIndex < numSites; siteIndex ++) {

                double value = GLPK.glp_mip_col_val(lp, variableIndex + 1);
                if(value > 0.5) {
                    AbstractSite legalSite = sites.get(siteIndex);
                    this.tmpLegalX[blockIndex + blocksStart] = legalSite.getX();
                    this.tmpLegalY[blockIndex + blocksStart] = legalSite.getY();
                    break;
                }
                variableIndex++;
            }
        }

        GLPK.glp_delete_prob(lp);
    }
}