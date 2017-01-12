package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import place.circuit.Circuit;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.AbstractSite;
import place.placers.analytical.TwoDimLinkedList.Axis;

/**
 * This is approximately the legalizer as proposed in
 * Heterogeneous Analytical Placement (HeAP).
 *
 */
class HeapLegalizer extends Legalizer {

    // These are temporary data structures
    protected GrowingArea[][] areaPointers;
    protected List<List<List<LegalizerBlock>>> blockMatrix;

    private HashMap<BlockType,ArrayList<int[]>> legalizationAreas;

    HeapLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights) throws IllegalArgumentException {

        super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights);

        // Initialize the matrix to contain a linked list at each coordinate
        this.blockMatrix = new ArrayList<List<List<LegalizerBlock>>>(this.width+2);
        for(int column = 0; column < this.width + 2; column++) {
            List<List<LegalizerBlock>> blockColumn = new ArrayList<>(this.height+2);
            for(int row = 0; row < this.height + 2; row++) {
                blockColumn.add(new ArrayList<LegalizerBlock>());
            }
            this.blockMatrix.add(blockColumn);
        }
    }


    @Override
    protected void legalizeBlockType(double tileCapacity, int blocksStart, int blocksEnd) {
        // Make a matrix that contains the blocks that are closest to each position
        initializeBlockMatrix(blocksStart, blocksEnd);

        // Build a set of disjunct areas that are not over-utilized
        this.areaPointers = new GrowingArea[this.width+2][this.height+2];
        List<GrowingArea> areas = this.growAreas();
        
        this.updateLegalizationAreas(areas);
        
        // Legalize all unabsorbed areas
        for(GrowingArea area : areas) {
            if(!area.isAbsorbed()) {
                this.legalizeArea(area);
            }
        }
    }
    
    @Override
    protected void initializeLegalizationAreas(){
    	this.legalizationAreas = new HashMap<BlockType,ArrayList<int[]>>();
    }
    
    private void updateLegalizationAreas(List<GrowingArea> areas){
        if(!this.legalizationAreas.containsKey(this.blockType))this.legalizationAreas.put(this.blockType, new ArrayList<int[]>());
        for(GrowingArea area : areas) {
        	if(!area.isAbsorbed()) {
        		int top = area.top + this.blockType.getHeight();
        		int bottom = area.bottom;
        		int left = area.left;
        		int right = area.right + 1;
        		int[] temp = {top, bottom, left, right};
        		this.legalizationAreas.get(this.blockType).add(temp);
        	}
        }
    }
    
    @Override
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return this.legalizationAreas;
    }

    private void initializeBlockMatrix(int blocksStart, int blocksEnd) {
        // Clear the block matrix
        for(int column = 0; column < this.width + 2; column++) {
            for(int row = 0; row < this.height + 2; row++) {
                this.blockMatrix.get(column).get(row).clear();
            }
        }

        // Loop through all the blocks of the correct block type and add them to their closest position
        for(int index = blocksStart; index < blocksEnd; index++) {
            double x = this.linearX[index],
                   y = this.linearY[index];
            int height = this.heights[index];

            for(int offset = (1 - height) / 2; offset <= height / 2; offset++) {
                AbstractSite site = this.getClosestSite(x, y + offset);
                int column = site.getColumn();
                int row = site.getRow();

                LegalizerBlock newBlock = new LegalizerBlock(index, offset, height);
                this.blockMatrix.get(column).get(row).add(newBlock);
            }
        }
    }


    private AbstractSite getClosestSite(double x, double y) {

        int column, row;

        if(this.blockCategory == BlockCategory.CLB) {
            column = (int) Math.round(x);
            row = (int) Math.round(Math.max(Math.min(y, this.height), 1));

        } else {
            int numColumns = (int) Math.floor((this.width - this.blockStart) / this.blockRepeat + 1);
            int columnIndex = (int) Math.round(Math.max(Math.min((x - this.blockStart) / this.blockRepeat, numColumns - 1), 0));
            column = columnIndex * this.blockRepeat + this.blockStart;

            int numRows = (int) Math.floor((this.height) / this.blockHeight);
            int rowIndex = (int) Math.round(Math.max(Math.min((y - 1) / this.blockHeight, numRows - 1), 0));
            row = rowIndex * this.blockHeight + 1;
        }

        // Get closest legal column
        int direction = (x > column) ? 1 : -1;
        while(true) {
            if(column > 0 && column < this.width+1 && this.circuit.getColumnType(column).equals(this.blockType)) {
                return this.circuit.getSite(column, row);
            }

            column += direction * this.blockRepeat;
            direction = -(direction + (int) Math.signum(direction));
        }
    }


    protected List<GrowingArea> growAreas() {
        List<Integer> columns = new ArrayList<Integer>();

        // This dummy element is added to simplify the test inside the while loop
        columns.add(Integer.MIN_VALUE);
        for(int column = this.blockStart; column < this.width + 1; column += this.blockRepeat) {
            if(this.circuit.getColumnType(column).equals(this.blockType)) {
                columns.add(column);
            }
        }
        int columnStartIndex = columns.size() / 2;
        int columnEndIndex = (columns.size() + 1) / 2;
        double centerX = (columns.get(columnStartIndex) + columns.get(columnEndIndex)) / 2.0;


        List<Integer> rows = new ArrayList<Integer>();
        rows.add(Integer.MIN_VALUE);
        for(int row = 1; row < this.height + 2 - this.blockHeight; row += this.blockHeight) {
            rows.add(row);
        }
        int rowStartIndex = rows.size() / 2;
        int rowEndIndex = (rows.size() + 1) / 2;
        double centerY = (rows.get(rowStartIndex) + rows.get(rowEndIndex)) / 2.0;

        List<GrowingArea> areas = new ArrayList<GrowingArea>();

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


    private void tryNewArea(List<GrowingArea> areas, int column, int row) {
        if(this.blockMatrix.get(column).get(row).size() >= 1
                && this.areaPointers[column][row] == null) {
            GrowingArea newArea = this.newArea(column, row);
            areas.add(newArea);
        }
    }

    private GrowingArea newArea(int x, int y) {

        // left, top, right, bottom
        GrowingArea area = new GrowingArea(
                this.linearX,
                this.linearY,
                x,
                y,
                this.tileCapacity,
                this.blockHeight,
                this.blockRepeat);

        do {
            this.growAreaOneStep(area);
        } while(area.getOccupation() > area.getCapacity());

        
        return area;
    }

    private void growAreaOneStep(GrowingArea area) {
        while(true) {
            Direction direction = area.nextGrowDirection();
            GrowingArea goalArea = new GrowingArea(area, direction);
            boolean growthPossible = goalArea.isLegal(this.width+2, this.height+2);
            if(growthPossible) {
                this.growArea(area, goalArea);
                return;

            } else {
                direction.disable();
            }
        }
    }


    private void growArea(GrowingArea area, GrowingArea goalArea) {

        // While goalArea is not completely covered by area
        while(true) {
            int rowStart, rowEnd, columnStart, columnEnd;
            // Check if growing the area would go out of the bounds of the FPGA
            if(goalArea.right > area.right || goalArea.left < area.left) {
                rowStart = area.bottom;
                rowEnd = area.top;

                if(goalArea.right > area.right) {
                    area.right += this.blockRepeat;
                    columnStart = area.right;

                } else {
                    area.left -= this.blockRepeat;
                    columnStart = area.left;
                }

                columnEnd = columnStart;

            } else if(goalArea.top > area.top || goalArea.bottom < area.bottom) {
                columnStart = area.left;
                columnEnd = area.right;

                if(goalArea.top > area.top) {
                    area.top += this.blockHeight;
                    rowStart = area.top;

                } else {
                    area.bottom -= this.blockHeight;
                    rowStart = area.bottom;
                }

                rowEnd = rowStart;

            } else {
                return;
            }



            for(int row = rowStart; row <= rowEnd; row += this.blockHeight) {
                for(int column = columnStart; column <= columnEnd; column += this.blockRepeat) {
                    this.addTileToArea(area, goalArea, column, row);
                }
            }
        }
    }

    private void addTileToArea(GrowingArea area, GrowingArea goalArea, int column, int row) {

        // If this tile is occupied by an unabsorbed area
        GrowingArea neighbour = this.areaPointers[column][row];
        if(neighbour != null && !neighbour.isAbsorbed()) {
            neighbour.absorb();

            // Update the goal area to contain the absorbed area
            goalArea.left = Math.min(goalArea.left, neighbour.left);
            goalArea.right = Math.max(goalArea.right, neighbour.right);
            goalArea.bottom = Math.min(goalArea.bottom, neighbour.bottom);
            goalArea.top = Math.max(goalArea.top, neighbour.top);
        }

        // Update the area pointer
        this.areaPointers[column][row] = area;

        // Update the capacity and occupancy
        AbstractSite site = this.circuit.getSite(column, row, true);
        if(site != null && site.getType().equals(this.blockType)) {
            area.incrementTiles();

            for(LegalizerBlock block : this.blockMatrix.get(column).get(row)) {
                // Add this block to the area if it is the root of a macro
                if(block.offset == 0) {
                    area.addBlock(block);
                }

                // If this is a macro:
                // Update the goal area to contain the entire macro
                if(block.macroHeight > 1) {
                    goalArea.top = Math.min(this.height, Math.max(goalArea.top, row + block.macroHeight - 1 - block.offset));
                    goalArea.bottom = Math.max(1, Math.min(goalArea.bottom, row - block.offset));
                }
            }
        }
    }



    private void legalizeArea(GrowingArea area) {
        boolean splitSuccess;
        while(true) {
            // Calculate the capacity of the area
            int numTiles = 0;
            int columnHeight = (area.top - area.bottom) / this.blockHeight + 1;
            for(int column = area.left; column <= area.right; column += this.blockRepeat) {
                if(this.circuit.getColumnType(column) == this.blockType) {
                    numTiles += columnHeight;
                }
            }

            TwoDimLinkedList blocks = area.getBlockIndexes();
            SplittingArea splittingArea = new SplittingArea(area);

            splitSuccess = this.legalizeArea(splittingArea, numTiles, blocks);

            if(splitSuccess) {
                return;
            }

            this.growAreaOneStep(area);
        }
    }

    private boolean legalizeArea(
            SplittingArea area,
            int numTiles,
            TwoDimLinkedList blocks) {

        int sizeX = area.right - area.left + 1,
            sizeY = area.top - area.bottom + 1;
        int numRows = (sizeY - 1) / this.blockHeight + 1;
        int numColumns = numTiles / numRows;

        if(blocks.size() == 0) {
            return true;

        // If the area is only one tile big: place all the blocks on this tile
        } else if(numTiles == 1) {
            int row = area.bottom;

            // Find the first column of the correct type
            int column = -1;
            for(int c = area.left; c <= area.right; c += this.blockRepeat) {
                if(this.circuit.getColumnType(c).equals(this.blockType)) {
                    column = c;
                    break;
                }
            }

            for(LegalizerBlock block : blocks) {
                int blockIndex = block.blockIndex;
                this.legalX[blockIndex] = column;
                this.legalY[blockIndex] = row;
            }

            return true;

        // If there is only one block left: find the closest site in the area
        } else if(blocks.numBlocks() == 1) {
            LegalizerBlock block = blocks.getFirst(Axis.X);
            this.placeBlock(block, area);

            return true;

        } else if(numColumns == 1) {
            // Find the first column of the correct type
            for(int column = area.left; column <= area.right; column += this.blockRepeat) {
                if(this.circuit.getColumnType(column).equals(this.blockType)) {
                    this.placeBlocksInColumn(blocks, column, area.bottom, area.top);

                    return true;
                }
            }
        }


        // Choose which axis to split along

        Axis axis;
        if(sizeX / (double) this.blockRepeat >= sizeY / (double) this.blockHeight) {
            axis = Axis.X;
        } else {
            axis = Axis.Y;
        }

        // Split area along axis and store ratio between the two subareas
        // Sort blocks along axis
        SplittingArea area1 = new SplittingArea(area);
        SplittingArea area2;

        int splitPosition = -1, numTiles1;

        if(axis == Axis.X) {
            int numColumnsLeft = 0;
            for(int column = area.left; column <= area.right; column += this.blockRepeat) {
                if(this.circuit.getColumnType(column).equals(this.blockType)) {
                    numColumnsLeft++;
                }

                if(numColumnsLeft == numColumns / 2) {
                    splitPosition = column + this.blockRepeat;
                    break;
                }
            }

            numTiles1 = numColumnsLeft * numRows;
            area2 = area1.splitHorizontal(splitPosition, this.blockRepeat);


        } else {

            int maxHeight = blocks.maxHeight();

            // If there is a macro that is higher than half of the
            // current area height: place greedily
            if(maxHeight > numRows / 2) {
                return this.placeGreedy(area, blocks);

            } else {
                int numRowsBottom = numRows / 2;
                numTiles1 = numRowsBottom * numColumns;
                splitPosition = area.bottom + (numRowsBottom) * this.blockHeight;

                area2 = area1.splitVertical(splitPosition, this.blockHeight);
            }
        }

        int splitIndex = (int) Math.ceil(numTiles1 * blocks.size() / (double) numTiles);
        int numTiles2 = numTiles - numTiles1;

        TwoDimLinkedList blocks1 = new TwoDimLinkedList(blocks),
                         blocks2 = new TwoDimLinkedList(blocks);
        blocks.split(blocks1, blocks2, splitIndex, axis);

        // If the split failed
        if(blocks1.size() > numTiles1 || blocks2.size() > numTiles2) {
            return false;
        }

        boolean success1 = this.legalizeArea(area1, numTiles1, blocks1);
        boolean success2 = true;
        if(success1) {
            success2 = this.legalizeArea(area2, numTiles2, blocks2);
        }

        if(success1 && success2) {
            return true;
        } else {
            return this.placeGreedy(area, blocks);
        }
    }

    private void placeBlock(LegalizerBlock block, SplittingArea area) {
        int blockIndex = block.blockIndex;
        double linearX = this.linearX[blockIndex];
        double linearY = this.linearY[blockIndex];

        // Find the closest row
        int macroHeight = block.macroHeight;
        if(macroHeight % 2 == 0) {
            linearY -= 0.5;
        }
        int row = (((int) Math.round(linearY) - 1) / this.blockHeight) * this.blockHeight + 1;

        // Make sure the row fits in the coordinates
        if(row - ((macroHeight - 1) / 2) * this.blockHeight < area.bottom) {
            row = area.bottom + ((macroHeight - 1) / 2) * this.blockHeight;
        } else if(row + (macroHeight / 2) * this.blockHeight > area.top) {
            row = area.top - (macroHeight / 2) * this.blockHeight;
        }
        this.legalY[blockIndex] = row;


        // Find the closest column
        int column = (int) Math.round(linearX);

        if(column > area.left && column < area.right) {
            int direction = linearX > column ? 1 : -1;
            while(this.badColumn(column, area)) {
                column += direction;
                direction = -(direction + (int) Math.signum(direction));
            }

        } else {
            int direction;
            if(column <= area.left) {
                column = area.left;
                direction = 1;
            } else {
                column = area.right;
                direction = -1;
            }

            while(this.badColumn(column, area)) {
                column += direction;
            }
        }

        this.legalX[blockIndex] = column;
    }

    private boolean badColumn(int column, SplittingArea area) {
        return
                column < area.left
                || column > area.right
                || !this.circuit.getColumnType(column).equals(this.blockType);
    }


    private void placeBlocksInColumn(TwoDimLinkedList blocks, int column, int rowStart, int rowEnd) {

        double y = rowStart;

        int blocksSize = blocks.size();
        int numRows = (rowEnd - rowStart) / this.blockHeight + 1;
        double rowsPerCell = numRows / blocksSize;

        for(LegalizerBlock block : blocks.blocksY()) {
            int blockIndex = block.blockIndex;
            int height = block.macroHeight;

            int row = (int) Math.round(y + (height - 1) / 2);
            this.legalX[blockIndex] = column;
            this.legalY[blockIndex] = row;

            y += rowsPerCell * height * this.blockHeight;
        }
    }


    private boolean placeGreedy(SplittingArea area, TwoDimLinkedList blocks) {

        // Sort the blocks by size
        List<LegalizerBlock> sortedBlocks = new ArrayList<>();
        for(LegalizerBlock block : blocks) {
            sortedBlocks.add(block);
        }

        Collections.sort(sortedBlocks, new BlockComparator(this.linearX));

        int numBlocks = sortedBlocks.size();
        int columnCapacity = (area.top - area.bottom) / this.blockHeight + 1;
        int splitStart = 0;

        List<Integer> columns = new ArrayList<>();
        List<List<LegalizerBlock>> columnBlocks = new ArrayList<>();
        for(int column = area.left; column <= area.right; column++) {
            if(!this.blockType.equals(this.circuit.getColumnType(column))) {
                continue;
            }

            int occupancy = 0;
            int splitEnd = splitStart;

            boolean blockFound = true;
            while(occupancy < columnCapacity && blockFound) {

                blockFound = false;
                int swapPointer = splitEnd;
                int height = -1;
                while(swapPointer < numBlocks) {
                    height = this.heights[sortedBlocks.get(swapPointer).blockIndex];
                    if(occupancy + height <= columnCapacity) {
                        blockFound = true;
                        break;
                    }

                    swapPointer++;
                }

                if(blockFound) {
                    if(swapPointer != splitEnd) {
                        LegalizerBlock tmp = sortedBlocks.get(splitEnd);
                        sortedBlocks.set(splitEnd, sortedBlocks.get(swapPointer));
                        sortedBlocks.set(swapPointer, tmp);
                    }

                    occupancy += height;
                    splitEnd++;
                }
            }

            columns.add(column);
            columnBlocks.add(sortedBlocks.subList(splitStart, splitEnd));

            splitStart = splitEnd;
        }

        if(splitStart == numBlocks) {
            int numColumns = columns.size();
            for(int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
                this.placeBlocksInColumn(columns.get(columnIndex), area.bottom, columnBlocks.get(columnIndex));
            }

            return true;

        } else {
            return false;
        }
    }

    private void placeBlocksInColumn(int column, int rowStart, List<LegalizerBlock> blocks) {

        Collections.sort(blocks, new BlockComparator(this.linearY));

        int row = rowStart;
        for(LegalizerBlock block : blocks) {
            int blockIndex = block.blockIndex;
            int macroHeight = block.macroHeight;

            row += ((macroHeight - 1) / 2) * this.blockHeight;

            this.legalX[blockIndex] = column;
            this.legalY[blockIndex] = row;

            row += ((macroHeight + 2) / 2) * this.blockHeight;
        }
    }


    private class BlockComparator implements Comparator<LegalizerBlock> {

        private double[] coordinates;

        BlockComparator(double[] coordinates) {
            this.coordinates = coordinates;
        }

        @Override
        public int compare(LegalizerBlock block1, LegalizerBlock block2) {
            return Double.compare(this.coordinates[block1.blockIndex], this.coordinates[block2.blockIndex]);
        }
    }



    class LegalizerBlock {
        int blockIndex;
        int offset;
        int macroHeight;

        LegalizerBlock(int blockIndex, int offset, int macroHeight) {
            this.blockIndex = blockIndex;
            this.offset = offset;
            this.macroHeight = macroHeight;
        }
    }



    private abstract class Area {

        int left, right, bottom, top;

        Area(int left, int right, int bottom, int top) {
            this.left = left;
            this.right = right;
            this.bottom = bottom;
            this.top = top;
        }

        Area(Area area) {
            this(area.left, area.right, area.bottom, area.top);
        }

        @Override
        public String toString() {
            return String.format("h: [%d, %d],\tv: [%d, %d]", this.left, this.right, this.bottom, this.top);
        }
    }

    private class SplittingArea extends Area {

        SplittingArea(int left, int right, int bottom, int top) {
            super(left, right, bottom, top);
        }

        SplittingArea(Area area) {
            super(area);
        }

        SplittingArea splitHorizontal(int split, int space) {
            SplittingArea newArea = new SplittingArea(split, this.right, this.bottom, this.top);
            this.right = split - space;

            return newArea;
        }

        SplittingArea splitVertical(int split, int space) {
            SplittingArea newArea = new SplittingArea(this.left, this.right, split, this.top);
            this.top = split - space;

            return newArea;
        }
    }

    protected class ExtendingArea extends Area {
    	
    	private int centerX;
    	private int centerY;
    	private double ratio;

    	private Direction growLeft;
    	private Direction growRight;
    	private Direction growUp;
    	private Direction growDown;
    	
		public ExtendingArea(GrowingArea area, int centerX, int centerY, double ratio) {
    		super(area.left, area.right, area.bottom, area.top);
    		
    		this.centerX = centerX;
    		this.centerY = centerY;
    		
    		this.ratio = ratio;
    		
    		this.growLeft = area.growLeft;
    		this.growRight = area.growRight;
    		this.growUp = area.growUp;
    		this.growDown = area.growDown;
		}
		public void increaseOneStep(){
			if(this.top - this.bottom == 0){
				this.increaseVertical();
			}else if( (this.right - this.left) / (this.top - this.bottom) > this.ratio){
        		this.increaseVertical();
        	}else{
        		this.increaseHorizontal();
        	}
		}
		public void increaseHorizontal(){
			if(this.growLeft.disabled() && this.growRight.disabled()){
				this.increaseVertical();
			}else if(this.growLeft.disabled()){
				this.right += 1;
			}else if(this.growRight.disabled()){
				this.left -= 1;
			}else if(this.centerX - this.left > this.right - this.centerX){
    			this.right += 1;
    		}else{
    			this.left -= 1;
    		}
		}
		public void increaseVertical(){
			if(this.growDown.disabled() && this.growUp.disabled()){
				this.increaseHorizontal();
			}else if(this.growDown.disabled()){
				this.top += 1;
			}else if(this.growUp.disabled()){
				this.bottom -= 1;
			}else if(this.centerY - this.bottom > this.top - this.centerY){
    			this.top += 1;
    		}else{
    			this.bottom -= 1;
    		}
		}
    }
    protected class GrowingArea extends Area {

        private boolean absorbed = false;

        private double areaTileCapacity;
        private int areaBlockHeight, areaBlockRepeat;

        private int numTiles = 0;
        private TwoDimLinkedList blockIndexes;

        private Direction growRight, growLeft, growUp, growDown;

        private int centerX, centerY;        

        GrowingArea(double[] linearX, double[] linearY, int column, int row, double tileCapacity, int blockHeight, int blockRepeat) {
            super(column, column - blockRepeat, row, row);

            // Thanks to this two-dimensionally linked list, we
            // don't have to sort the list of blocks after each
            // area split: the block list is splitted and resorted
            // in linear time.
            this.blockIndexes = new TwoDimLinkedList(linearX, linearY);

            this.areaTileCapacity = tileCapacity;
            this.areaBlockHeight = blockHeight;
            this.areaBlockRepeat = blockRepeat;
            
            this.centerX = column;
            this.centerY = row;
            
            this.growRight = new Direction(1,0);
            this.growLeft = new Direction(-1,0);
            this.growUp = new Direction(0,1);
            this.growDown = new Direction(0,-1);
        }

        GrowingArea(GrowingArea area, Direction direction) {
            super(area);

            this.blockIndexes = new TwoDimLinkedList(area.blockIndexes);

            this.areaTileCapacity = area.areaTileCapacity;
            this.areaBlockHeight = area.areaBlockHeight;
            this.areaBlockRepeat = area.areaBlockRepeat;

            this.grow(direction);
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


        void addBlock(LegalizerBlock block) {
            this.blockIndexes.add(block);
        }
        TwoDimLinkedList getBlockIndexes() {
            return this.blockIndexes;
        }
        int getOccupation() {
            return this.blockIndexes.size();
        }


        boolean isLegal(int width, int height) {
            return
                    this.left >=1
                    && this.right <= width - 2
                    && this.bottom >= 1
                    && this.top + this.areaBlockHeight <= height - 1;
        }

        Direction nextGrowDirection() {
        	if(this.right < this.left) return this.growRight;
        	if(this.top < this.bottom) return this.growUp;
        	
        	Direction direction;
        	ExtendingArea extendedArea = new ExtendingArea(this, this.centerX, centerY, 1.35);
        	while(true){
        		extendedArea.increaseOneStep();
        		direction = compare(this, extendedArea);
        		if(direction != null){
        			return direction;
        		}
        	}
        }
        public Direction compare(GrowingArea originalArea, ExtendingArea extendedArea){
        	if(extendedArea.top == originalArea.top + this.areaBlockHeight){
        		return this.growUp;
        	}else if(extendedArea.bottom == originalArea.bottom - this.areaBlockHeight){
        		return this.growDown;
        	}else if(extendedArea.right == originalArea.right + this.areaBlockRepeat){
        		return this.growRight;
        	}else if(extendedArea.left == originalArea.left - this.areaBlockRepeat){
        		return this.growLeft;
        	}else{
        		return null;
        	}
        }

        void grow(Direction direction) {
            this.grow(direction.getHorizontal(), direction.getVertical());
        }
        void grow(int horizontal, int vertical) {
            if(horizontal == -1) {
                this.left -= this.areaBlockRepeat;

            } else if(horizontal == 1) {
                this.right += this.areaBlockRepeat;

            } else if(vertical == -1) {
                this.bottom -= this.areaBlockHeight;

            } else if(vertical == 1) {
                this.top += this.areaBlockHeight;
            }
        }
    }
    private class Direction {
    	private int horizontal;
    	private int vertical;
    	private boolean disabled;
    	
    	public Direction(int horizontal, int vertical){
    		this.horizontal = horizontal;
    		this.vertical = vertical;
    		this.disabled = false;
    	}
    	public void disable(){
    		this.disabled = true;
    	}
    	public boolean disabled(){
    		return this.disabled;
    	}
    	public int getHorizontal(){
    		return this.horizontal;
    	}
    	public int getVertical(){
    		return this.vertical;
    	}
    }
}