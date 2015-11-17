package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;

public class NewLegalizer extends Legalizer {

    private static enum Axis {X, Y};

    // Contain the properties of the blockType that is currently being legalized
    private BlockType blockType;
    private int blockStart, blockRepeat, blockHeight;

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

        this.blockStart = this.blockType.getStart();
        this.blockHeight = this.blockType.getHeight();
        this.blockRepeat = this.blockType.getRepeat();

        // Make a matrix that contains the blocks that are closest to each position
        initializeBlockMatrix(blocksStart, blocksEnd);

        int xCenter = this.width / 2;
        int yCenter = this.height / 2;
        int maxDimension = Math.max(xCenter, yCenter);

        // TODO: make this more efficient
        // idea: get a list of overutilized blocks in initializeBlockMatrix();
        this.tryNewArea(xCenter, yCenter);

        for(int centerDist1 = 1; centerDist1 < maxDimension; centerDist1++) {
            for(int centerDist2 = -centerDist1; centerDist2 < centerDist1; centerDist2++) {
                this.tryNewArea(xCenter + centerDist1, yCenter + centerDist2);
                this.tryNewArea(xCenter - centerDist1, yCenter - centerDist2);
                this.tryNewArea(xCenter + centerDist2, yCenter - centerDist1);
                this.tryNewArea(xCenter - centerDist2, yCenter + centerDist1);
            }
        }

        for(int x = this.blockStart; x < this.width - 1; x += this.blockRepeat) {
            if(!this.circuit.getColumnType(x).equals(this.blockType)) {
                continue;
            }

            for(int y = 1; y < this.height - 1; y += this.blockHeight) {
                for(int blockIndex : this.blockMatrix.get(x).get(y)) {

                    if(blockIndex == 3054 || blockIndex == 3051) {
                        int d = 0;
                    }

                    this.tmpLegalX[blockIndex] = x;
                    this.tmpLegalY[blockIndex] = y;
                }
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
        boolean overOccupied = false;
        try {
            overOccupied = this.blockMatrix.get(x).get(y).size() > this.tileCapacity;

        } catch(ArrayIndexOutOfBoundsException error) {
            return;
        }

        if(overOccupied) {
            this.growAndLegalizeArea(x, y);
        }
    }

    private void growAndLegalizeArea(int x, int y) {
        NewLegalizerArea area = new NewLegalizerArea(x, y, this.tileCapacity, this.blockType);

        area.incrementTiles();
        area.addBlockIndexes(this.blockMatrix.get(x).get(y));

        while(area.getOccupation() > area.getCapacity()) {
            int[] direction = area.nextGrowDirection();
            boolean success = this.growArea(area, direction);

            if(success) {
                area.grow();
            } else {
                area.disableDirection();
            }
        }

        this.legalizeArea(area);
    }

    private boolean growArea(NewLegalizerArea area, int[] direction) {

        int rowStart, rowEnd, columnStart, columnEnd;

        // Check if growing the area would go out of the bounds of the FPGA
        if(direction[1] == 0) {
            rowStart = area.top;
            rowEnd = area.bottom;

            if(direction[0] == 1) {
                columnStart = area.right + this.blockRepeat;
                if(columnStart > this.width - 2) {
                    return false;
                }

            } else {
                columnStart = area.left - this.blockRepeat;
                if(columnStart < 1) {
                    return false;
                }
            }

            columnEnd = columnStart;

        } else {
            columnStart = area.left;
            columnEnd = area.right;

            if(direction[1] == 1) {
                rowStart = area.bottom + this.blockHeight;
                if(rowStart + this.blockHeight > this.height - 1) {
                    return false;
                }

            } else {
                rowStart = area.top - this.blockHeight;
                if(rowStart < 1) {
                    return false;
                }
            }

            rowEnd = rowStart;
        }


        for(int x = columnStart; x <= columnEnd; x += this.blockRepeat) {
            if(!this.circuit.getColumnType(x).equals(this.blockType)) {
                continue;
            }

            for(int y = rowStart; y <= rowEnd; y += this.blockHeight) {
                area.addBlockIndexes(this.blockMatrix.get(x).get(y));
                area.incrementTiles();
            }
        }

        return true;
    }


    private void legalizeArea(NewLegalizerArea area) {
        List<Integer> blockIndexes = area.getBlockIndexes();
        int[] coordinates = {area.left, area.top, area.right, area.bottom};
        this.legalizeArea(coordinates, blockIndexes, Axis.X);
    }

    private void legalizeArea(
            int[] coordinates,
            List<Integer> blockIndexes,
            Axis axis) {

        boolean smallestPossibleX = (coordinates[2] - coordinates[0] < this.blockRepeat);
        boolean smallestPossibleY = (coordinates[3] - coordinates[1] < this.blockHeight);

        // These are the conditions under which the area is small enough to finish

        // If there are no blocks left in the area: clear the tiles in the area
        if(blockIndexes.size() == 0) {
            for(int x = coordinates[0]; x <= coordinates[2]; x += this.blockRepeat) {

                if(!this.circuit.getColumnType(x).equals(this.blockType)) {
                    continue;
                }

                for(int y = coordinates[1]; y <= coordinates[3]; y += this.blockHeight) {
                    if((x == 39 || x == 40) && y == 62) {
                        int d = 0;
                    }

                    this.blockMatrix.get(x).get(y).clear();
                }
            }

            return;

        // If the area is only one tile big: place all the blocks on this tile
        } else if(smallestPossibleX && smallestPossibleY) {
            int x = coordinates[0], y = coordinates[1];
            if((x == 39 || x == 40) && y == 62) {
                int d = 0;
            }
            this.blockMatrix.get(x).get(y).clear();

            for(Integer blockIndex : blockIndexes) {
                this.blockMatrix.get(x).get(y).add(blockIndex);
            }

            return;

        // If there is only one block but multiple tiles: place the block on the closest tile
        } else if(blockIndexes.size() == 0) {
            int blockIndex = blockIndexes.get(0);
            double linearX = this.linearX[blockIndex];
            double linearY = this.linearY[blockIndex];

            double minDistance = Double.MAX_VALUE;
            int minX = -1, minY = -1;

            for(int x = coordinates[0]; x <= coordinates[2]; x += this.blockRepeat) {
                if(!this.circuit.getColumnType(x).equals(this.blockType)) {
                    continue;
                }

                for(int y = coordinates[1]; y <= coordinates[3]; y += this.blockHeight) {
                    if((x == 39 || x == 40) && y == 62) {
                        int d = 0;
                    }
                    this.blockMatrix.get(x).get(y).clear();

                    double distance = Math.pow(linearX - x, 2) + Math.pow(linearY - y, 2);
                    if(distance < minDistance) {
                        minDistance = distance;
                        minX = x;
                        minY = y;
                    }
                }
            }

            this.blockMatrix.get(minX).get(minY).add(blockIndex);

            return;


        // These are the conditions under which the area should immediatly be split further

        // If splitting horizontally is not possible: split vertically
        } else if(smallestPossibleX && axis == Axis.X) {
            this.legalizeArea(coordinates, blockIndexes, Axis.Y);
            return;

         // If splitting vertically is not possible: split horizontally
        } else if(smallestPossibleY && axis == Axis.Y) {
            this.legalizeArea(coordinates, blockIndexes, Axis.X);
            return;
        }



        // Split area along axis and store ratio between the two subareas
        // Sort blocks along axis
        int[] coordinates1 = new int[4], coordinates2 = new int[4];
        System.arraycopy(coordinates, 0, coordinates1, 0, 4);
        System.arraycopy(coordinates, 0, coordinates2, 0, 4);

        double splitRatio;
        Axis newAxis;

        if(axis == Axis.X) {

            // If the blockType is CLB
            if(this.blockType.getCategory() == BlockCategory.CLB) {
                int numClbColumns = 0;
                for(int column = coordinates[0]; column <= coordinates[2]; column++) {
                    if(this.circuit.getColumnType(column).getCategory() == BlockCategory.CLB) {
                        numClbColumns++;
                    }
                }

                int splitColumn = -1;
                int halfNumClbColumns = 0;
                for(int column = coordinates[0]; column <= coordinates[2]; column++) {
                    if(this.circuit.getColumnType(column).getCategory() == BlockCategory.CLB) {
                        halfNumClbColumns++;
                    }

                    if(halfNumClbColumns >= numClbColumns / 2) {
                        splitColumn = column;
                        break;
                    }
                }

                splitRatio = halfNumClbColumns / (double) numClbColumns;

                coordinates1[2] = splitColumn;
                coordinates2[0] = splitColumn + 1;

            // Else: it's a hardblock
            } else {
                int numColumns = (coordinates[2] - coordinates[0]) / this.blockRepeat + 1;
                splitRatio = (numColumns / 2) / (double) numColumns;

                coordinates1[2] = coordinates[0] + (numColumns / 2 - 1) * this.blockRepeat;
                coordinates2[0] = coordinates[0] + (numColumns / 2) * this.blockRepeat;
            }

            Collections.sort(blockIndexes, new BlockComparator(this.linearX));

            newAxis = Axis.Y;

        } else {

            // If the blockType is CLB
            if(this.blockRepeat == 1) {
                int splitRow = (coordinates[1] + coordinates[3]) / 2;
                splitRatio = (splitRow - coordinates[1] + 1) / (double) (coordinates[3] - coordinates[1] + 1);

                coordinates1[3] = splitRow;
                coordinates2[1] = splitRow + 1;

            // Else: it's a hardblock
            } else {
                int numRows = (coordinates[3] - coordinates[1]) / this.blockHeight + 1;
                splitRatio = (numRows / 2) / (double) numRows;

                coordinates1[3] = coordinates[1] + (numRows / 2 - 1) * this.blockHeight;
                coordinates2[1] = coordinates[1] + (numRows / 2) * this.blockHeight;
            }

            Collections.sort(blockIndexes, new BlockComparator(this.linearY));

            newAxis = Axis.X;
        }


        // Split blocks in two lists with a ratio approx. equal to area split
        int split = (int) Math.ceil(splitRatio * blockIndexes.size());
        List<Integer> blocks1 = new ArrayList<>(blockIndexes.subList(0, split));
        List<Integer> blocks2 = new ArrayList<>(blockIndexes.subList(split, blockIndexes.size()));

        this.legalizeArea(coordinates1, blocks1, newAxis);
        this.legalizeArea(coordinates2, blocks2, newAxis);
    }
}
