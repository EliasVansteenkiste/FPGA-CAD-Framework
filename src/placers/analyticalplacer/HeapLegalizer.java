package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import placers.analyticalplacer.TwoDimLinkedList.Axis;

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

    // Contain the properties of the blockType that is currently being legalized
    private BlockType blockType;
    private int blockStart, blockRepeat, blockHeight;

    // These are temporary data structures
    private Area[][] areaPointers;
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
        this.areaPointers = new Area[this.width][this.height];
        List<Area> areas = this.growAreas();

        // Legalize all unabsorbed areas
        for(Area area : areas) {
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


    private List<Area> growAreas() {
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

        List<Area> areas = new ArrayList<Area>();

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


    private void tryNewArea(List<Area> areas, int x, int y) {
        if(this.blockMatrix.get(x).get(y).size() >= 1
                && this.areaPointers[x][y] == null) {
            Area newArea = this.newArea(x, y);
            areas.add(newArea);
        }
    }

    private Area newArea(int x, int y) {

        // left, top, right, bottom
        Area area = new Area(
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
            Area goalArea = new Area(area, direction);

            boolean growthPossible = goalArea.isLegal(this.width, this.height);
            if(growthPossible) {
                this.growArea(area, goalArea);

            } else {
                area.disableDirection();
            }
        }

        return area;
    }


    private void growArea(Area area, Area goalArea) {

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
                    Area neighbour = this.areaPointers[x][y];
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



    private void legalizeArea(Area area) {
        TwoDimLinkedList<Integer> blockIndexes = area.getBlockIndexes();
        int[] coordinates = {area.left, area.top, area.right, area.bottom};

        int capacity = 0;
        int columnHeight = (area.bottom - area.top) / this.blockHeight + 1;
        for(int column = area.left; column <= area.right; column += this.blockRepeat) {
            if(this.circuit.getColumnType(column) == this.blockType) {
                capacity += columnHeight;
            }
        }

        this.legalizeArea(coordinates, capacity, blockIndexes, Axis.X);
    }

    private void legalizeArea(
            int[] coordinates,
            int capacity,
            TwoDimLinkedList<Integer> blockIndexes,
            Axis axis) {

        if(blockIndexes.size() == 0) {
            return;

        // If the area is only one tile big: place all the blocks on this tile
        } else if(
                coordinates[2] - coordinates[0] < this.blockRepeat
                && coordinates[3] - coordinates[1] < this.blockHeight) {
            for(Integer blockIndex : blockIndexes) {
                this.legalX[blockIndex] = coordinates[0];
                this.legalY[blockIndex] = coordinates[1];
            }

            return;

        // If there is only one block left: find the closest site in the area
        } else if(blockIndexes.size() == 1) {
            for(int blockIndex : blockIndexes) {
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
            }

            return;

        } else if(coordinates[2] - coordinates[0] < this.blockRepeat && axis == Axis.X) {
            this.legalizeArea(coordinates, capacity, blockIndexes, Axis.Y);
            return;

        } else if(coordinates[3] - coordinates[1] < this.blockHeight && axis == Axis.Y) {
            this.legalizeArea(coordinates, capacity, blockIndexes, Axis.X);
            return;
        }

        // Split area along axis and store ratio between the two subareas
        // Sort blocks along axis
        int[] coordinates1 = new int[4], coordinates2 = new int[4];
        System.arraycopy(coordinates, 0, coordinates1, 0, 4);
        System.arraycopy(coordinates, 0, coordinates2, 0, 4);

        int splitPosition = -1, capacity1;
        Axis newAxis;

        if(axis == Axis.X) {

            int columnHeight = (coordinates[3] - coordinates[1]) / this.blockHeight + 1;
            int numColumns = capacity / columnHeight;
            int numColumnsLeft;

            // If the blockType is CLB
            if(this.blockType.getCategory() == BlockCategory.CLB) {
                numColumnsLeft = 0;
                for(int column = coordinates[0]; column <= coordinates[2]; column++) {
                    if(this.circuit.getColumnType(column).getCategory() == BlockCategory.CLB) {
                        numColumnsLeft++;
                    }

                    if(numColumnsLeft >= numColumns / 2) {
                        splitPosition = column + 1;
                        break;
                    }
                }

            // Else: it's a hardblock
            } else {
                numColumnsLeft = numColumns / 2;
                splitPosition = coordinates[0] + numColumnsLeft * this.blockRepeat;
            }

            capacity1 = numColumnsLeft * columnHeight;
            coordinates1[2] = splitPosition - this.blockRepeat;
            coordinates2[0] = splitPosition;

            newAxis = Axis.Y;


        } else {

            int numRows = (coordinates[3] - coordinates[1]) / this.blockHeight + 1;
            int numRowsTop = numRows / 2;

            int rowWidth = capacity / numRows;
            capacity1 = numRowsTop * rowWidth;

            splitPosition = coordinates[1] + (numRowsTop) * this.blockHeight;
            coordinates1[3] = splitPosition - this.blockHeight;
            coordinates2[1] = splitPosition;

            newAxis = Axis.X;
        }

        int capacity2 = capacity - capacity1;


        int splitIndex = (int) Math.ceil(capacity1 * blockIndexes.size() / (double) capacity);
        TwoDimLinkedList<Integer> otherBlockIndexes = blockIndexes.split(splitIndex, axis);

        this.legalizeArea(coordinates1, capacity1, blockIndexes, newAxis);
        this.legalizeArea(coordinates2, capacity2, otherBlockIndexes, newAxis);
    }




    private class Area {

        int top, bottom, left, right;

        private boolean absorbed = false;

        private double areaTileCapacity;
        private int areaBlockHeight, areaBlockRepeat;

        private int numTiles = 0;
        private TwoDimLinkedList<Integer> blockIndexes;

        private int[][] growDirections = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        private boolean[] originalDirection = {true, true, true, true};
        private int growDirectionIndex = -1;

        private Area(Comparator<Integer> comparatorX, Comparator<Integer> comparatorY) {
            // Thanks to this two-dimensionally linked list, we
            // don't have to sort the list of blocks after each
            // area split: the block list is splitted and resorted
            // in linear time.
            this.blockIndexes = new TwoDimLinkedList<Integer>(comparatorX, comparatorY);
        }

        Area(Area a, int[] direction) {
            this.blockIndexes = new TwoDimLinkedList<Integer>(a.blockIndexes);

            this.areaTileCapacity = a.areaTileCapacity;
            this.areaBlockHeight = a.areaBlockHeight;
            this.areaBlockRepeat = a.areaBlockRepeat;


            this.left = a.left;
            this.right = a.right;

            this.top = a.top;
            this.bottom = a.bottom;

            this.grow(direction);
        }

        Area(Comparator<Integer> comparatorX, Comparator<Integer> comparatorY, int x, int y, double tileCapacity, BlockType blockType) {
            this(comparatorX, comparatorY);

            this.left = x;
            this.right = x;
            this.top = y;
            this.bottom = y;

            this.areaTileCapacity = tileCapacity;
            this.areaBlockHeight = blockType.getHeight();
            this.areaBlockRepeat = blockType.getRepeat();
        }


        void absorb() {
            this.absorbed = true;
        }

        boolean isAbsorbed() {
            return this.absorbed;
        }


        void incrementTiles() {
            this.numTiles++;
        }
        double getCapacity() {
            return this.numTiles * this.areaTileCapacity;
        }


        void addBlockIndexes(Collection<Integer> blockIndexes) {
            this.blockIndexes.addAll(blockIndexes);
        }
        TwoDimLinkedList<Integer> getBlockIndexes() {
            return this.blockIndexes;
        }
        int getOccupation() {
            return this.blockIndexes.size();
        }


        boolean isLegal(int width, int height) {
            return
                    this.left >=1
                    && this.right <= width - 2
                    && this.top >= 1
                    && this.bottom + this.areaBlockHeight <= height - 1;
        }



        int[] nextGrowDirection() {
            int[] direction;
            do {
                this.growDirectionIndex = (this.growDirectionIndex + 1) % 4;
                direction = this.growDirections[this.growDirectionIndex];
            } while(direction[0] == 0 && direction[1] == 0);

            return direction;
        }


        void grow(int[] direction) {
            this.grow(direction[0], direction[1]);
        }
        void grow(int horizontal, int vertical) {
            if(horizontal == -1) {
                this.left -= this.areaBlockRepeat;

            } else if(horizontal == 1) {
                this.right += this.areaBlockRepeat;

            } else if(vertical == -1) {
                this.top -= this.areaBlockHeight;

            } else if(vertical == 1) {
                this.bottom += this.areaBlockHeight;
            }
        }


        void disableDirection() {
            int index = this.growDirectionIndex;
            int oppositeIndex = (index + 2) % 4;

            if(this.originalDirection[index]) {
                if(!this.originalDirection[oppositeIndex]) {
                    this.growDirections[oppositeIndex][0] = 0;
                    this.growDirections[oppositeIndex][1] = 0;
                }

                this.originalDirection[index] = false;
                this.growDirections[index][0] = this.growDirections[oppositeIndex][0];
                this.growDirections[index][1] = this.growDirections[oppositeIndex][1];

            } else {
                this.growDirections[index][0] = 0;
                this.growDirections[index][1] = 0;
                this.growDirections[oppositeIndex][0] = 0;
                this.growDirections[oppositeIndex][1] = 0;
            }

            // Make sure the replacement for the current grow direction is chosen next,
            // since growing in the current direction must have failede
            this.growDirectionIndex--;
        }


        @Override
        public String toString() {
            return String.format("[[%d, %d], [%d, %d]", this.left, this.top, this.right, this.bottom);
        }
    }

    private class BlockComparator implements Comparator<Integer> {

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