package placers.analytical;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import circuit.Circuit;
import circuit.architecture.BlockType;

public class ShiftingLegalizer extends Legalizer {

    ShiftingLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights) {

        super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights);
    }

    public ShiftingLegalizer(Legalizer legalizer) {
        super(legalizer);
    }

    @Override
    protected void legalizeBlockType(double tileCapacity, int blocksStart, int blocksEnd) {

        int numBlocks = blocksEnd - blocksStart;
        Integer[] blockIndexes = new Integer[numBlocks];
        for(int i = 0; i < numBlocks; i++) {
            blockIndexes[i] = blocksStart + i;
        }

        Arrays.sort(blockIndexes, new BlockComparator(this.linearX));

        // Count the number of blocks in each column
        int countPointer = 0;
        int columnCapacity = (this.height - 2) / this.blockHeight;
        int[] columnOccupancy = new int[this.width];

        for(int column = this.blockStart; column < this.width - 1; column += this.blockRepeat) {
            if(this.blockType.equals(this.circuit.getColumnType(column))) {
                double boundary = column + this.blockRepeat / 2.0;
                int occupancy = 0;

                while(countPointer < numBlocks) {
                    int blockIndex = blockIndexes[countPointer];
                    if(this.linearX[blockIndex] > boundary) {
                        break;
                    }

                    occupancy += this.heights[blockIndex];
                    countPointer++;
                }
                columnOccupancy[column] = occupancy;

            } else {
                columnOccupancy[column] = columnCapacity;
            }
        }


        // Redistribute the number of blocks in each column
        for(int column = this.blockStart; column < this.width - 1; column += this.blockRepeat) {
            int toColumn = column;
            int direction = this.blockRepeat;
            while(columnOccupancy[column] > columnCapacity) {
                toColumn += direction;
                direction = -(direction + Integer.signum(direction) * this.blockRepeat);

                if(toColumn > 0 && toColumn < this.width - 1 && columnOccupancy[toColumn] < columnCapacity) {
                    int moveBlocks = Math.min(
                            columnOccupancy[column] - columnCapacity,
                            columnCapacity - columnOccupancy[toColumn]);

                    columnOccupancy[column] -= moveBlocks;
                    columnOccupancy[toColumn] += moveBlocks;
                }
            }
        }

        // Find the split positions
        // Reorder some cells to make sure that each column can be
        // filled exactly to the required capacity
        int splitStart = 0;
        for(int column = this.blockStart; column < this.width - 1; column += this.blockRepeat) {

            // Skip columns of the wrong type
            if(!this.blockType.equals(this.circuit.getColumnType(column))) {
                continue;
            }

            int capacity = columnOccupancy[column];
            int occupancy = 0;
            int splitEnd = splitStart;

            while(occupancy < capacity) {
                int swapPointer = splitEnd;
                while(occupancy + this.heights[blockIndexes[swapPointer]] > capacity) {
                    swapPointer++;
                }

                if(swapPointer != splitEnd) {
                    int tmp = blockIndexes[splitEnd];
                    blockIndexes[splitEnd] = blockIndexes[swapPointer];
                    blockIndexes[swapPointer] = tmp;
                }

                occupancy += this.heights[blockIndexes[splitEnd]];
                splitEnd++;
            }

            this.legalizeColumn(column, blockIndexes, splitStart, splitEnd);
            splitStart = splitEnd;
        }
    }

    private void legalizeColumn(int column, Integer[] blockIndexes, int blockStart, int blockEnd) {
        Arrays.sort(blockIndexes, blockStart, blockEnd, new BlockComparator(this.linearY));

        int row = 1;
        for(int blockPointer = blockStart; blockPointer < blockEnd; blockPointer++) {
            int blockIndex = blockIndexes[blockPointer];
            int macroHeight = this.heights[blockIndex];

            row += ((macroHeight - 1) / 2) * this.blockHeight;

            this.legalX[blockIndex] = column;
            this.legalY[blockIndex] = row;

            row += ((macroHeight + 2) / 2) * this.blockHeight;
        }
    }



    private class BlockComparator implements Comparator<Integer> {

        private double[] coordinates;

        BlockComparator(double[] coordinates) {
            this.coordinates = coordinates;
        }

        @Override
        public int compare(Integer blockIndex1, Integer blockIndex2) {
            return Double.compare(this.coordinates[blockIndex1], this.coordinates[blockIndex2]);
        }
    }
}
