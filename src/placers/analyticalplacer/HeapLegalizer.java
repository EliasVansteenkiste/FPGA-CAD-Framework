package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.architecture.BlockCategory;
import circuit.block.AbstractSite;

/**
 * This is approximately the legalizer as proposed in
 * Heterogeneous Analytical Placement (HeAP).
 *
 */
class HeapLegalizer extends Legalizer {

    private static enum Axis {X, Y};

    // Contain the properties of the blockType that is currently being legalized
    private BlockType blockType;
    private int blockStart, blockRepeat, blockHeight;

    // These are temporary data structures
    private HeapLegalizerArea[][] areaPointers;
    private List<List<List<Integer>>> blockMatrix;


    HeapLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY) throws IllegalArgumentException {

        super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY);
    }


    @Override
    protected void legalizeBlockType(double tileCapacity, BlockType blockType, int blocksStart, int blocksEnd) {
        this.blockType = blockType;

        this.blockStart = this.blockType.getStart();
        this.blockHeight = this.blockType.getHeight();
        this.blockRepeat = this.blockType.getRepeat();

        // Make a matrix that contains the blocks that are closest to each position
        initializeBlockMatrix(blocksStart, blocksEnd);

        // Build a set of disjunct areas that are not over-utilized
        this.areaPointers = new HeapLegalizerArea[this.width][this.height];
        List<HeapLegalizerArea> areas = this.growAreas();

        // Legalize all unabsorbed areas
        for(HeapLegalizerArea area : areas) {
            if(!area.isAbsorbed()) {
                //System.out.printf("(%d, %d, %d, %d), %b\n", area.left, area.top, area.right, area.bottom, area.isAbsorbed());
                this.legalizeArea(area);
            }
        }
    }


    private void initializeBlockMatrix(int blocksStart, int blocksEnd) {

        // Initialize the matrix to contain a linked list at each coordinate
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


    private List<HeapLegalizerArea> growAreas() {
        List<Integer> columns = new ArrayList<Integer>();

        // This dummy element is added to simplify the test inside the while loop
        columns.add(Integer.MIN_VALUE);
        for(int column = this.blockStart; column < this.width - 1; column += this.blockRepeat) {
            if(this.circuit.getColumnType(column).equals(this.blockType)) {
                columns.add(column);
            }
        }
        int columnStartIndex = columns.size() / 2;
        int columnEndIndex = (columns.size() + 1) / 2;
        double centerX = (columns.get(columnStartIndex) + columns.get(columnEndIndex)) / 2.0;


        List<Integer> rows = new ArrayList<Integer>();
        rows.add(Integer.MIN_VALUE);
        for(int row = 1; row < this.height - this.blockHeight; row += this.blockHeight) {
            rows.add(row);
        }
        int rowStartIndex = rows.size() / 2;
        int rowEndIndex = (rows.size() + 1) / 2;
        double centerY = (rows.get(rowStartIndex) + rows.get(rowEndIndex)) / 2.0;

        List<HeapLegalizerArea> areas = new ArrayList<HeapLegalizerArea>();

        // Grow from the center coordinate(s)
        for(int rowIndex = rowStartIndex; rowIndex <= rowEndIndex; rowIndex++) {
            int row = rows.get(rowIndex);

            for(int columnIndex = columnStartIndex; columnIndex <= columnEndIndex; columnIndex++) {
                int column = columns.get(columnIndex);

                this.tryNewArea(areas, column, row);
            }
        }

        while(columnStartIndex > 1 || rowStartIndex > 1) {
            // Run over the two closest columns
            if(centerX - columns.get(columnStartIndex - 1) <= centerY - rows.get(rowStartIndex - 1)) {
                columnStartIndex--;
                columnEndIndex++;

                int column1 = columns.get(columnStartIndex);
                int column2 = columns.get(columnEndIndex);
                for(int i = (rowEndIndex - rowStartIndex) / 2; i >= 0; i--) {
                    int row1 = rows.get(rowStartIndex + i);
                    int row2 = rows.get(rowEndIndex - i);

                    this.tryNewArea(areas, column1, row1);
                    this.tryNewArea(areas, column1, row2);
                    this.tryNewArea(areas, column2, row1);
                    this.tryNewArea(areas, column2, row2);
                }

            // Run over the two closest rows
            } else {
                rowStartIndex--;
                rowEndIndex++;

                int row1 = rows.get(rowStartIndex);
                int row2 = rows.get(rowEndIndex);
                for(int i = (columnEndIndex - columnStartIndex) / 2; i >= 0; i--) {
                    int column1 = columns.get(columnStartIndex + i);
                    int column2 = columns.get(columnEndIndex - i);

                    this.tryNewArea(areas, column1, row1);
                    this.tryNewArea(areas, column1, row2);
                    this.tryNewArea(areas, column2, row1);
                    this.tryNewArea(areas, column2, row2);
                }
            }
        }

        return areas;
    }


    private void tryNewArea(List<HeapLegalizerArea> areas, int x, int y) {
        if(this.blockMatrix.get(x).get(y).size() >= 1
                && this.areaPointers[x][y] == null) {
            HeapLegalizerArea newArea = this.newArea(x, y);
            areas.add(newArea);
        }
    }

    private HeapLegalizerArea newArea(int x, int y) {

        // left, top, right, bottom
        HeapLegalizerArea area = new HeapLegalizerArea(
                new BlockComparator(this.linearX),
                new BlockComparator(this.linearY),
                x,
                y,
                this.tileCapacity,
                this.blockType);

        area.incrementTiles();
        area.addBlockIndexes(this.blockMatrix.get(x).get(y));
        this.areaPointers[x][y] = area;

        while(area.getOccupation() > area.getCapacity()) {
            int[] direction = area.nextGrowDirection();
            HeapLegalizerArea goalArea = new HeapLegalizerArea(area, direction);

            boolean growthPossible = goalArea.isLegal(this.width, this.height);
            if(growthPossible) {
                this.growArea(area, goalArea);

            } else {
                area.disableDirection();
            }
        }

        return area;
    }


    private void growArea(HeapLegalizerArea area, HeapLegalizerArea goalArea) {

        // While goalArea is not completely covered by area
        while(true) {
            int rowStart, rowEnd, columnStart, columnEnd;

            // Check if growing the area would go out of the bounds of the FPGA
            if(goalArea.right > area.right || goalArea.left < area.left) {
                rowStart = area.top;
                rowEnd = area.bottom;

                if(goalArea.right > area.right) {
                    area.right += this.blockRepeat;
                    columnStart = area.right;

                } else {
                    area.left -= this.blockRepeat;
                    columnStart = area.left;
                }

                columnEnd = columnStart;

            } else if(goalArea.bottom > area.bottom || goalArea.top < area.top) {
                columnStart = area.left;
                columnEnd = area.right;

                if(goalArea.bottom > area.bottom) {
                    area.bottom += this.blockHeight;
                    rowStart = area.bottom;

                } else {
                    area.top -= this.blockHeight;
                    rowStart = area.top;
                }

                rowEnd = rowStart;

            } else {
                return;
            }



            for(int y = rowStart; y <= rowEnd; y += this.blockHeight) {
                for(int x = columnStart; x <= columnEnd; x += this.blockRepeat) {

                    // If this tile is occupied by an unabsorbed area
                    HeapLegalizerArea neighbour = this.areaPointers[x][y];
                    if(neighbour != null && !neighbour.isAbsorbed()) {
                        neighbour.absorb();

                        // Update the goal area to contain the absorbed area
                        goalArea.left = Math.min(goalArea.left, neighbour.left);
                        goalArea.right = Math.max(goalArea.right, neighbour.right);
                        goalArea.top = Math.min(goalArea.top, neighbour.top);
                        goalArea.bottom = Math.max(goalArea.bottom, neighbour.bottom);
                    }

                    // Update the area pointer
                    this.areaPointers[x][y] = area;

                    // Update the capacity and occupancy
                    AbstractSite site = this.circuit.getSite(x, y, true);
                    if(site != null && site.getType().equals(this.blockType)) {
                        area.addBlockIndexes(this.blockMatrix.get(x).get(y));
                        area.incrementTiles();
                    }
                }
            }
        }
    }



    private void legalizeArea(HeapLegalizerArea area) {
        TwoDimLinkedList<Integer> blockIndexes = area.getBlockIndexes();
        int[] coordinates = {area.left, area.top, area.right, area.bottom};
        this.legalizeArea(coordinates, blockIndexes, Axis.X);
    }

    private void legalizeArea(
            int[] coordinates,
            TwoDimLinkedList<Integer> blockIndexes,
            Axis axis) {

        // If the area is only one tile big: place all the blocks on this tile
        if(coordinates[2] - coordinates[0] < this.blockRepeat && coordinates[3] - coordinates[1] < this.blockHeight) {

            for(Integer blockIndex : blockIndexes) {
                this.legalX[blockIndex] = coordinates[0];
                this.legalY[blockIndex] = coordinates[1];
            }

            return;

        } else if(blockIndexes.size() == 0) {
            return;

        } else if(blockIndexes.size() == 1) {
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
                    double distance = Math.pow(linearX - x, 2) + Math.pow(linearY - y, 2);
                    if(distance < minDistance) {
                        minDistance = distance;
                        minX = x;
                        minY = y;
                    }
                }
            }

            this.legalX[blockIndex] = minX;
            this.legalY[blockIndex] = minY;
            return;

        } else if(coordinates[2] - coordinates[0] < this.blockRepeat && axis == Axis.X) {
            this.legalizeArea(coordinates, blockIndexes, Axis.Y);
            return;

        } else if(coordinates[3] - coordinates[1] < this.blockHeight && axis == Axis.Y) {
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

            newAxis = Axis.X;
        }

        // Split blocks in two lists with a ratio approx. equal to area split
        int splitIndex = (int) Math.ceil(splitRatio * blockIndexes.size());
        TwoDimLinkedList<Integer> otherBlockIndexes;
        if(axis == Axis.X) {
            otherBlockIndexes = blockIndexes.splitX(splitIndex);
        } else {
            otherBlockIndexes = blockIndexes.splitY(splitIndex);
        }

        boolean found = false;
        for(Integer blockIndex : blockIndexes) {
            if(blockIndex == 1386) {
                found = true;
                break;
            }
        }
        for(Integer blockIndex : otherBlockIndexes) {
            if(blockIndex == 1386) {
                found = true;
                break;
            }
        }
        if(!found) {
            found = true;
        }

        this.legalizeArea(coordinates1, blockIndexes, newAxis);
        this.legalizeArea(coordinates2, otherBlockIndexes, newAxis);
    }




    class BlockComparator implements Comparator<Integer> {

        private double[] coordinates;

        public BlockComparator(double[] coordinates) {
            this.coordinates = coordinates;
        }

        @Override
        public int compare(Integer index1, Integer index2) {
            return new Double(this.coordinates[index1]).compareTo(this.coordinates[index2]);
        }
    }
}