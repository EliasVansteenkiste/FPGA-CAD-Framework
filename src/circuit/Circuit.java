package circuit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.architecture.Architecture;
import circuit.block.AbstractBlock;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.block.IOSite;
import circuit.block.LeafBlock;
import circuit.block.Macro;
import circuit.block.Site;
import circuit.block.TimingGraph;
import circuit.pin.GlobalPin;

public class Circuit {

    private String name;
    private transient int width, height;

    private Architecture architecture;

    private TimingGraph timingGraph;


    private Map<BlockType, List<AbstractBlock>> blocks;

    private List<BlockType> globalBlockTypes;
    private List<BlockType> leafBlockTypes;
    private List<GlobalBlock> globalBlockList = new ArrayList<GlobalBlock>();
    private List<LeafBlock> leafBlockList = new ArrayList<LeafBlock>();
    private List<Macro> macros = new ArrayList<Macro>();

    private List<BlockType> columns;
    private Map<BlockType, List<Integer>> columnsPerBlockType;
    private List<List<List<Integer>>> nearbyColumns;

    private AbstractSite[][] sites;


    public Circuit(String name, Architecture architecture, Map<BlockType, List<AbstractBlock>> blocks) {
        this.name = name;
        this.architecture = architecture;

        this.blocks = blocks;

        this.timingGraph = new TimingGraph(this);
    }


    public void initializeData() {
        this.loadBlocks();

        this.timingGraph.build();

        for(List<AbstractBlock> blocksOfType : this.blocks.values()) {
            for(AbstractBlock block : blocksOfType) {
                block.compact();
            }
        }
    }


    private void loadBlocks() {
        for(BlockType blockType : BlockType.getBlockTypes()) {
            if(!this.blocks.containsKey(blockType)) {
                this.blocks.put(blockType, new ArrayList<AbstractBlock>(0));
            }
        }

        this.globalBlockTypes = BlockType.getGlobalBlockTypes();
        this.leafBlockTypes = BlockType.getLeafBlockTypes();

        for(BlockType blockType : this.globalBlockTypes) {
            @SuppressWarnings("unchecked")
            List<GlobalBlock> blocksOfType = (List<GlobalBlock>) (List<?>) this.blocks.get(blockType);
            this.globalBlockList.addAll(blocksOfType);
        }

        for(BlockType blockType : this.leafBlockTypes) {
            @SuppressWarnings("unchecked")
            List<LeafBlock> blocksOfType = (List<LeafBlock>) (List<?>) this.blocks.get(blockType);
            this.leafBlockList.addAll(blocksOfType);
        }

        this.loadMacros();

        this.createColumns();
        this.createSites();
    }

    private void loadMacros() {
        for(BlockType blockType : this.globalBlockTypes) {

            // Skip block types that don't have a carry chain
            if(blockType.getCarryFromPort() == null) {
                continue;
            }

            for(AbstractBlock abstractBlock : this.blocks.get(blockType)) {
                GlobalBlock block = (GlobalBlock) abstractBlock;
                GlobalPin carryIn = block.getCarryIn();
                GlobalPin carryOut = block.getCarryOut();

                if(carryIn.getSource() == null && carryOut.getNumSinks() > 0) {
                    List<GlobalBlock> macroBlocks = new ArrayList<>();
                    macroBlocks.add(block);

                    while(carryOut.getNumSinks() != 0) {
                        block = carryOut.getSink(0).getOwner();
                        carryOut = block.getCarryOut();
                        macroBlocks.add(block);
                    }

                    Macro macro = new Macro(macroBlocks);
                    this.macros.add(macro);
                }
            }
        }
    }


    private void createColumns() {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        BlockType clbType = BlockType.getBlockTypes(BlockCategory.CLB).get(0);
        List<BlockType> hardBlockTypes = BlockType.getBlockTypes(BlockCategory.HARDBLOCK);

        // Create a list of all global block types except the IO block type,
        // sorted by priority
        List<BlockType> blockTypes = new ArrayList<BlockType>();
        blockTypes.add(clbType);
        blockTypes.addAll(hardBlockTypes);

        Collections.sort(blockTypes, new Comparator<BlockType>() {
            @Override
            public int compare(BlockType b1, BlockType b2) {
                return Integer.compare(b2.getPriority(), b1.getPriority());
            }
        });


        this.calculateSize(ioType, blockTypes);

        // Fill some extra data containers to quickly calculate
        // often used data
        this.cacheColumns(ioType, blockTypes);
        this.cacheColumnsPerBlockType(blockTypes);
        this.cacheNearbyColumns();
    }


    private void calculateSize(BlockType ioType, List<BlockType> blockTypes) {
        /**
         * Set the width and height, either fixed or automatically sized
         */
        if(this.architecture.isAutoSized()) {
            this.autoSize(ioType, blockTypes);

        } else {
            this.width = this.architecture.getWidth();
            this.height = this.architecture.getHeight();
        }
    }

    private void autoSize(BlockType ioType, List<BlockType> blockTypes) {
        int[] numColumnsPerType = new int[blockTypes.size()];

        boolean bigEnough = false;
        double autoRatio = this.architecture.getAutoRatio();
        int size = 2;
        this.width = size;
        this.height = size;
        int previousWidth;

        while(!bigEnough) {
            size += 1;

            // Enlarge the architecture by at most 1 block in each
            // dimension
            previousWidth = this.width;
            if(autoRatio >= 1) {
                this.width = size;
                this.height = (int) Math.round(this.width / autoRatio);

            } else {
                this.height = size;
                this.width = (int) Math.round(this.height * autoRatio);
            }

            // If a column was added: check which block type that column contains
            if(this.width > previousWidth) {
                int column = this.width - 2;

                for(int blockTypeIndex = 0; blockTypeIndex < blockTypes.size(); blockTypeIndex++) {
                    BlockType blockType = blockTypes.get(blockTypeIndex);
                    if(column % blockType.getRepeat() == blockType.getStart() || blockType.getRepeat() == 1) {
                        numColumnsPerType[blockTypeIndex] += 1;
                        break;
                    }
                }
            }


            // Check if the architecture is large enough
            int ioCapacity = (this.width + this.height - 4) * 2 * this.architecture.getIoCapacity();
            if(ioCapacity >= this.getBlocks(ioType).size()) {
                bigEnough = true;

                for(int blockTypeIndex = 0; blockTypeIndex < blockTypes.size(); blockTypeIndex++) {
                    BlockType blockType = blockTypes.get(blockTypeIndex);

                    int blocksPerColumn = (this.height - 2) / blockType.getHeight();
                    int capacity = numColumnsPerType[blockTypeIndex] * blocksPerColumn;

                    if(capacity < this.blocks.get(blockType).size()) {
                        bigEnough = false;
                        break;
                    }
                }
            }
        }
    }

    private void cacheColumns(BlockType ioType, List<BlockType> blockTypes) {
        /**
         * Make a list that contains the block type of each column
         */

        this.columns = new ArrayList<BlockType>(this.width);
        this.columns.add(ioType);
        for(int column = 1; column < this.width - 1; column++) {
            for(BlockType blockType : blockTypes) {
                if(column % blockType.getRepeat() == blockType.getStart() || blockType.getRepeat() == 1) {
                    this.columns.add(blockType);
                    break;
                }
            }
        }
        this.columns.add(ioType);
    }

    private void cacheColumnsPerBlockType(List<BlockType> blockTypes) {
        /**
         *  For each block type: make a list of the columns that contain
         *  blocks of that type
         */

        this.columnsPerBlockType = new HashMap<BlockType, List<Integer>>();
        for(BlockType blockType : blockTypes) {
            this.columnsPerBlockType.put(blockType, new ArrayList<Integer>());
        }
        for(int column = 1; column < this.width - 1; column++) {
            this.columnsPerBlockType.get(this.columns.get(column)).add(column);
        }
    }

    private void cacheNearbyColumns() {
        /**
         * Given a column index and a distance, we want to quickly
         * find all the columns that are within [distance] of the
         * current column and that have the same block type.
         * nearbyColumns facilitates this.
         */

        this.nearbyColumns = new ArrayList<List<List<Integer>>>();
        this.nearbyColumns.add(null);
        int size = Math.max(this.width, this.height) - 2;

        // Loop through all the columns
        for(int column = 1; column < this.width - 1; column++) {
            BlockType columnType = this.columns.get(column);

            // previousNearbyColumns will contain all the column indexes
            // that are within a certain increasing distance to this
            // column, and that have the same block type.
            List<Integer> previousNearbyColumns = new ArrayList<>();
            previousNearbyColumns.add(column);

            // For each distance, nearbyColumnsPerDistance[distance] will
            // contain a list like previousNearbyColumns.
            List<List<Integer>> nearbyColumnsPerDistance = new ArrayList<>();
            nearbyColumnsPerDistance.add(previousNearbyColumns);

            // Loop through all the possible distances
            for(int distance = 1; distance < size; distance++) {
                List<Integer> newNearbyColumns = new ArrayList<>(previousNearbyColumns);

                // Add the column to the left and right, if they have the correct block type
                int left = column - distance;
                if(left >= 1 && this.columns.get(left).equals(columnType)) {
                    newNearbyColumns.add(left);
                }

                int right = column + distance;
                if(right <= this.width - 2 && this.columns.get(right).equals(columnType)) {
                    newNearbyColumns.add(right);
                }

                nearbyColumnsPerDistance.add(newNearbyColumns);
                previousNearbyColumns = newNearbyColumns;
            }

            this.nearbyColumns.add(nearbyColumnsPerDistance);
        }
    }


    private void createSites() {
        this.sites = new AbstractSite[this.width][this.height];

        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        int ioCapacity = this.architecture.getIoCapacity();

        for(int i = 1; i < this.height - 1; i++) {
            this.sites[0][i] = new IOSite(0, i, ioType, ioCapacity);
            this.sites[this.width - 1][i] = new IOSite(this.width - 1, i, ioType, ioCapacity);
        }

        for(int i = 1; i < this.width - 1; i++) {
            this.sites[i][0] = new IOSite(i, 0, ioType, ioCapacity);
            this.sites[i][this.height - 1] = new IOSite(i, this.height - 1, ioType, ioCapacity);
        }

        for(int column = 1; column < this.width - 1; column++) {
            BlockType blockType = this.columns.get(column);

            int blockHeight = blockType.getHeight();
            for(int row = 1; row < this.height - blockHeight; row += blockHeight) {
                this.sites[column][row] = new Site(column, row, blockType);
            }
        }
    }




    /*************************
     * Timing graph wrapping *
     *************************/
    public TimingGraph getTimingGraph() {
        return this.timingGraph;
    }

    public void recalculateTimingGraph() {
        this.timingGraph.recalculateAllSlacksCriticalities(true);
    }
    public double calculateTimingCost() {
        return this.timingGraph.calculateTotalCost();
    }
    public double getMaxDelay() {
        return this.timingGraph.getMaxDelay();
    }



    public List<GlobalBlock> getGlobalBlocks() {
        return this.globalBlockList;
    }
    public List<LeafBlock> getLeafBlocks() {
        return this.leafBlockList;
    }

    public List<Macro> getMacros() {
        return this.macros;
    }



    public String getName() {
        return this.name;
    }
    public int getWidth() {
        return this.width;
    }
    public int getHeight() {
        return this.height;
    }
    public Architecture getArchitecture() {
        return this.architecture;
    }


    public BlockType getColumnType(int column) {
        return this.columns.get(column);
    }

    /*
     * Return the site at coordinate (x, y). If allowNull is false,
     * return the site that overlaps coordinate (x, y) but may not
     * start at that position.
     */
    public AbstractSite getSite(int column, int row) {
        return this.getSite(column, row, false);
    }
    public AbstractSite getSite(int column, int row, boolean allowNull) {
        if(allowNull) {
            return this.sites[column][row];

        } else {
            AbstractSite site = null;
            int topY = row;
            while(site == null) {
                site = this.sites[column][topY];
                topY--;
            }

            return site;
        }
    }

    public int getNumGlobalBlocks() {
        return this.globalBlockList.size();
    }

    public List<BlockType> getGlobalBlockTypes() {
        return this.globalBlockTypes;
    }

    public Set<BlockType> getBlockTypes() {
        return this.blocks.keySet();
    }
    public List<AbstractBlock> getBlocks(BlockType blockType) {
        return this.blocks.get(blockType);
    }


    public List<AbstractSite> getSites(BlockType blockType) {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        List<AbstractSite> sites;

        if(blockType.equals(ioType)) {
            int ioCapacity = this.architecture.getIoCapacity();
            sites = new ArrayList<AbstractSite>((this.width + this.height - 2) * 2 * ioCapacity);

            for(int n = 0; n < ioCapacity; n++) {
                for(int i = 1; i < this.height - 1; i++) {
                    sites.add(this.sites[0][i]);
                    sites.add(this.sites[this.width - 1][i]);
                }

                for(int i = 1; i < this.width - 1; i++) {
                    sites.add(this.sites[i][0]);
                    sites.add(this.sites[i][this.height - 1]);
                }
            }

        } else {
            List<Integer> columns = this.columnsPerBlockType.get(blockType);
            int blockHeight = blockType.getHeight();
            sites = new ArrayList<AbstractSite>(columns.size() * (this.height - 2));

            for(Integer column : columns) {
                for(int row = 1; row < this.height - blockHeight; row += blockHeight) {
                    sites.add(this.sites[column][row]);
                }
            }
        }

        return sites;
    }


    public GlobalBlock getRandomBlock(Random random) {
        int index = random.nextInt(this.globalBlockList.size());
        return this.globalBlockList.get(index);
    }

    public AbstractSite getRandomSite(GlobalBlock block, int distance, Random random) {

        BlockType blockType = block.getType();
        int blockHeight = blockType.getHeight();
        int blockRepeat = blockType.getRepeat();
        if(distance < blockHeight && distance < blockRepeat) {
            return null;
        }

        // Get a random column
        int column = block.getColumn();
        List<Integer> candidateColumns = this.nearbyColumns.get(column).get(distance);
        int randomColumn = candidateColumns.get(random.nextInt(candidateColumns.size()));

        // Get a random row
        int row = block.getRow();
        int rowIndex = (row - 1) / blockHeight;
        int rowIndexDistance = distance / blockHeight;

        int minRowIndex = Math.max(rowIndex - rowIndexDistance, 0);
        int maxRowIndex = Math.min(rowIndex + rowIndexDistance + 1, (this.height - 2) / blockHeight);

        int randomRow = 1 + blockHeight * (minRowIndex + random.nextInt(maxRowIndex - minRowIndex));

        // Return the site found at the random row and column
        return this.getSite(randomColumn, randomRow, false);
    }



    @Override
    public String toString() {
        return this.getName();
    }
}
