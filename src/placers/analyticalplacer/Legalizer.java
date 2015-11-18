package placers.analyticalplacer;

import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.exceptions.PlacementException;

abstract class Legalizer {

    protected Circuit circuit;
    protected int width, height;

    private CostCalculator costCalculator;

    private Map<GlobalBlock, Integer> blockIndexes;
    private List<BlockType> blockTypes;
    private List<Integer> blockTypeIndexStarts;
    private int numBlocks, numIOBlocks, numMovableBlocks;

    private double bestCost;
    protected double tileCapacity;

    protected double[] linearX, linearY;
    private int[] bestLegalX, bestLegalY;
    protected int[] tmpLegalX, tmpLegalY;

    Legalizer(
            Circuit circuit,
            CostCalculator costCalculator,
            Map<GlobalBlock, Integer> blockIndexes,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY) {

        // Store easy stuff
        this.circuit = circuit;
        this.width = this.circuit.getWidth();
        this.height = this.circuit.getHeight();

        this.blockIndexes = blockIndexes;
        this.costCalculator = costCalculator;


        // Store block types
        if(blockTypes.get(0).getCategory() != BlockCategory.IO) {
            throw new IllegalArgumentException("The first block type is not IO");
        }
        if(blockTypes.size() != blockTypeIndexStarts.size() - 1) {
            throw new IllegalArgumentException("The objects blockTypes and blockTypeIndexStarts don't have matching dimensions");
        }

        this.blockTypes = blockTypes;
        this.blockTypeIndexStarts = blockTypeIndexStarts;

        // Store linear solution (this array is updated by the linear solver
        this.linearX = linearX;
        this.linearY = linearY;

        // Cache the number of blocks
        this.numBlocks = linearX.length;
        this.numIOBlocks = blockTypeIndexStarts.get(1);
        this.numMovableBlocks = this.numBlocks - this.numIOBlocks;

        this.bestCost = Double.MAX_VALUE;

        // Initialize the best solution with IO positions
        this.bestLegalX = new int[this.numBlocks];
        this.bestLegalY = new int[this.numBlocks];

        for(int i = 0; i < this.numIOBlocks; i++) {
            this.bestLegalX[i] = (int) this.linearX[i];
            this.bestLegalY[i] = (int) this.linearY[i];
        }

        // Initialize the temporary solution with IO positions
        this.tmpLegalX = new int[this.numBlocks];
        this.tmpLegalY = new int[this.numBlocks];

        System.arraycopy(this.bestLegalX, 0, this.tmpLegalX, 0, this.numBlocks);
        System.arraycopy(this.bestLegalY, 0, this.tmpLegalY, 0, this.numBlocks);
    }

    protected abstract void legalizeBlockType(double tileCapacity, BlockType blockType, int blocksStart, int blocksEnd);



    void legalize(double tileCapacity) throws PlacementException {
        this.tileCapacity = tileCapacity;

        // Skip i = 0: these are IO blocks
        for(int i = 1; i < this.blockTypes.size(); i++) {
            BlockType blockType = this.blockTypes.get(i);
            int blocksStart = this.blockTypeIndexStarts.get(i);
            int blocksEnd = this.blockTypeIndexStarts.get(i + 1);

            if(blocksEnd > blocksStart) {
                legalizeBlockType(tileCapacity, blockType, blocksStart, blocksEnd);
            }
        }

        this.updateBestLegal();
    }


    private void updateBestLegal() throws PlacementException {
        boolean update = this.costCalculator.requiresCircuitUpdate();

        if(update) {
            this.updateCircuit(this.tmpLegalX, this.tmpLegalY);
        }

        double newCost = this.costCalculator.calculate(this.tmpLegalX, this.tmpLegalY);

        if(newCost < this.bestCost && this.tileCapacity <= 1) {
            System.arraycopy(this.tmpLegalX, this.numIOBlocks, this.bestLegalX, this.numIOBlocks, this.numMovableBlocks);
            System.arraycopy(this.tmpLegalY, this.numIOBlocks, this.bestLegalY, this.numIOBlocks, this.numMovableBlocks);
            this.bestCost = newCost;

        } else if(update) {
            this.updateCircuit();
        }
    }




    double getCost() {
        return this.bestCost;
    }

    int[] getLegalX() {
        return this.bestLegalX;
    }
    int[] getLegalY() {
        return this.bestLegalY;
    }
    int[] getAnchorsX() {
        return this.tmpLegalX;
    }
    int[] getAnchorsY() {
        return this.tmpLegalY;
    }

    void updateCircuit() throws PlacementException {
        this.updateCircuit(this.bestLegalX, this.bestLegalY);
    }
    void updateCircuit(int[] x, int[] y) throws PlacementException {
        //Clear all previous locations
        for(GlobalBlock block : this.blockIndexes.keySet()) {
            if(block.getCategory() != BlockCategory.IO) {
                block.removeSite();
            }
        }

        // Update locations
        for(Map.Entry<GlobalBlock, Integer> blockEntry : this.blockIndexes.entrySet()) {
            GlobalBlock block = blockEntry.getKey();

            if(block.getCategory() != BlockCategory.IO) {
                int index = blockEntry.getValue();

                AbstractSite site = this.circuit.getSite(x[index], y[index], true);
                block.setSite(site);
            }
        }
    }
}
