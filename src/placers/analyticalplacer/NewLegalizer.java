package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;

public class NewLegalizer extends Legalizer {

    private static enum Axis {X, Y};

    // Contain the properties of the blockType that is currently being legalized
    private BlockType blockType;
    private int blockHeight, blockRepeat;

    private boolean[][] occupied;
    private List<List<List<Integer>>> blockMatrix;

    NewLegalizer(
            Circuit circuit,
            CostCalculator costCalculator,
            Map<GlobalBlock, Integer> blockIndexes,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY) throws IllegalArgumentException {

        super(circuit, costCalculator, blockIndexes, blockTypes, blockTypeIndexStarts, linearX, linearY);
    }

    @Override
    protected void legalizeBlockType(double tileCapacity, BlockType blockType, int blocksStart, int blocksEnd) {
        this.blockType = blockType;
        this.blockHeight = this.blockType.getHeight();
        this.blockRepeat = this.blockType.getRepeat();

        // Make a matrix that contains the blocks that are closest to each position
        initializeBlockMatrix(blocksStart, blocksEnd);

        this.occupied = new boolean[this.width][this.height];

        int xCenter = this.width / 2;
        int yCenter = this.height / 2;
        int maxDimension = Math.max(xCenter, yCenter);

        this.tryNewArea(xCenter, yCenter);

        for(int centerDist1 = 1; centerDist1 < maxDimension; centerDist1++) {
            for(int centerDist2 = -centerDist1; centerDist2 < centerDist1; centerDist2++) {
                this.tryNewArea(xCenter + centerDist1, yCenter + centerDist2);
                this.tryNewArea(xCenter - centerDist1, yCenter - centerDist2);
                this.tryNewArea(xCenter + centerDist2, yCenter - centerDist1);
                this.tryNewArea(xCenter - centerDist2, yCenter + centerDist1);
            }
        }
    }


    private void initializeBlockMatrix(int blocksStart, int blocksEnd) {

        // Initialize the matrix to contain a list of block indexes at each coordinate
        this.blockMatrix = new ArrayList<List<List<Integer>>>(this.width);
        for(int x = 0; x < this.width; x++) {
            List<List<Integer>> blockColumn = new ArrayList<List<Integer>>(this.height);
            for(int y = 0; y < this.height; y++) {
                blockColumn.add(new ArrayList<Integer>());
            }
            this.blockMatrix.add(blockColumn);
        }


        // Loop through all the blocks of the correct block type and add them to their closest position
        for(int index = blocksStart; index < blocksEnd; index++) {
            AbstractSite site = this.getClosestSite(this.linearX[index], this.linearY[index]);
            int x = site.getX();
            int y = site.getY();

            this.blockMatrix.get(x).get(y).add(index);
        }
    }


    private AbstractSite getClosestSite(double x, double y) {

        switch(this.blockType.getCategory()) {
            case IO:
                int siteX, siteY;
                if(x > y) {
                    if(x > this.height - y - 1) {
                        // Right quadrant
                        siteX = this.width - 1;
                        siteY = (int) Math.max(Math.min(Math.round(y), this.height - 2), 1);

                    } else {
                        // Top quadrant
                        siteX = (int)  Math.max(Math.min(Math.round(x), this.height - 2), 1);
                        siteY = 0;
                    }

                } else {
                    if(x > this.height - y - 1) {
                        //Bottom quadrant
                        siteX = (int)  Math.max(Math.min(Math.round(x), this.height - 2), 1);
                        siteY = this.height - 1;

                    } else {
                        // Left quadrant
                        siteX = 0;
                        siteY = (int) Math.max(Math.min(Math.round(y), this.height - 2), 1);
                    }
                }

                return this.circuit.getSite(siteX, siteY);

            case CLB:
                int row = (int) Math.round(Math.max(Math.min(y, this.height - 2), 1));

                // Get closest column
                // Not easy to do this with calculations if there are multiple hardblock types
                // So just trial and error
                int column = (int) Math.round(x);
                int step = 1;
                int direction = (x > column) ? 1 : -1;

                while(true) {
                    if(column > 0 && column < this.width-1 && this.circuit.getColumnType(column).equals(this.blockType)) {
                        break;
                    }

                    column += direction * step;
                    step++;
                    direction *= -1;
                }

                return this.circuit.getSite(column, row);


            // Hardblocks
            default:
                int start = this.blockType.getStart();
                int repeat = this.blockType.getRepeat();
                int blockHeight = this.blockType.getHeight();

                int numRows = (int) Math.floor((this.height - 2) / blockHeight);
                int numColumns = (int) Math.floor((this.width - start - 2) / repeat + 1);

                int columnIndex = (int) Math.round(Math.max(Math.min((x - start) / repeat, numColumns - 1), 0));
                int rowIndex = (int) Math.round(Math.max(Math.min((y - 1) / blockHeight, numRows - 1), 0));

                return this.circuit.getSite(columnIndex * repeat + start, rowIndex * blockHeight + 1);
        }
    }


    private void tryNewArea(int x, int y) {
        if(x > 0 && x < this.width - 1
                && y > 0 && y < this.height - 1
                && this.blockMatrix.get(x).get(y).size() >= 1
                && !this.occupied[x][y]) {
            this.growAndLegalizeArea(x, y);
        }
    }

    private void growAndLegalizeArea(int x, int y) {

    }
}
