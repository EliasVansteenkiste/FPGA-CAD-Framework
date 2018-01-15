package place.circuit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import place.circuit.architecture.Architecture;
import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.BlockType;
import place.circuit.block.AbstractBlock;
import place.circuit.block.AbstractSite;
import place.circuit.block.GlobalBlock;
import place.circuit.block.IOSite;
import place.circuit.block.Macro;
import place.circuit.block.Site;
import place.circuit.pin.GlobalPin;
import place.circuit.timing.TimingGraph;

public class Circuit {

    private String name;
    private transient int width, height;

    private Architecture architecture;

    private TimingGraph timingGraph;


    private Map<BlockType, List<AbstractBlock>> blocks;

    private List<BlockType> globalBlockTypes;
    private List<GlobalBlock> globalBlockList = new ArrayList<GlobalBlock>();
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
    public String stats(){
    	String s = new String();
    	s += "-------------------------------";
    	s += "\n";
    	s += "Type" + "\t" + "Col" + "\t" + "Loc" + "\t" + "Loc/Col";
    	s += "\n";
    	s += "-------------------------------";
    	s += "\n";
    	for(BlockType blockType:this.globalBlockTypes){
    		String columns = "-";
    		String columnHeight = "-";
    		if(this.columnsPerBlockType.get(blockType) != null){
    			columns = "" + (this.columnsPerBlockType.get(blockType).size()) + "";
    			columnHeight = "" + (this.height / blockType.getHeight()) + "";
    		}
        	s += blockType.getName() + "\t" + columns + "\t" + this.getCapacity(blockType) + "\t" + columnHeight + "\n";
        }
    	return s;
    }


    private void loadBlocks() {
        for(BlockType blockType : BlockType.getBlockTypes()) {
            if(!this.blocks.containsKey(blockType)) {
                this.blocks.put(blockType, new ArrayList<AbstractBlock>(0));
            }
        }

        this.globalBlockTypes = BlockType.getGlobalBlockTypes();

        for(BlockType blockType : this.globalBlockTypes) {
            @SuppressWarnings("unchecked")
            List<GlobalBlock> blocksOfType = (List<GlobalBlock>) (List<?>) this.blocks.get(blockType);
            this.globalBlockList.addAll(blocksOfType);
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
        	System.out.println("Auto size: " +  this.width + "x" + this.height + "\n");
        } else {
            this.width = this.architecture.getWidth();
            this.height = this.architecture.getHeight();
            System.out.println("Fixed size: " +  this.width + "x" + this.height + "\n");
        }
    }

    private void autoSize(BlockType ioType, List<BlockType> blockTypes) {
        int[] numColumnsPerType = new int[blockTypes.size()];

        boolean bigEnough = false;
        double autoRatio = this.architecture.getAutoRatio();
        int size = 0;
        this.width = size;
        this.height = size;
        int previousWidth;

        while(!bigEnough) {
            size += 1;

            // Enlarge the architecture by at most 1 block in each
            // dimension
            // VPR does this really strange: I would expect that the if clause is
            // inverted (if(autoRatio < 1)), maybe this is a bug?
            previousWidth = this.width;
            if(autoRatio >= 1) {
                this.height = size;
                this.width = (int) Math.round(this.height * autoRatio);
            } else {
                this.width = size;
                this.height = (int) Math.round(this.width / autoRatio);
            }

            // If columns have been added: check which block type those columns contain
            for(int column = previousWidth + 1; column < this.width + 1; column++) {
                for(int blockTypeIndex = 0; blockTypeIndex < blockTypes.size(); blockTypeIndex++) {
                    BlockType blockType = blockTypes.get(blockTypeIndex);
                    int repeat = blockType.getRepeat();
                    int start = blockType.getStart();
                    if(column % repeat == start || repeat == -1 && column == start) {
                        numColumnsPerType[blockTypeIndex] += 1;
                        break;
                    }
                }
            }


            // Check if the architecture is large enough
            int ioCapacity = (this.width + this.height) * 2 * this.architecture.getIoCapacity();
            if(ioCapacity >= this.getBlocks(ioType).size()) {
                bigEnough = true;

                for(int blockTypeIndex = 0; blockTypeIndex < blockTypes.size(); blockTypeIndex++) {
                    BlockType blockType = blockTypes.get(blockTypeIndex);

                    int blocksPerColumn = this.height / blockType.getHeight();
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
    	
        this.columns = new ArrayList<BlockType>(this.width+2);
        this.columns.add(ioType);
        for(int column = 1; column < this.width + 1; column++) {
            for(BlockType blockType : blockTypes) {
                int repeat = blockType.getRepeat();
                int start = blockType.getStart();
                if(column % repeat == start || repeat == -1 && column == start) {
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
        for(int column = 1; column < this.width + 1; column++) {
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
        int size = Math.max(this.width, this.height);

        // Loop through all the columns
        for(int column = 1; column < this.width + 1; column++) {
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
                if(right <= this.width && this.columns.get(right).equals(columnType)) {
                    newNearbyColumns.add(right);
                }

                nearbyColumnsPerDistance.add(newNearbyColumns);
                previousNearbyColumns = newNearbyColumns;
            }

            this.nearbyColumns.add(nearbyColumnsPerDistance);
        }
    }

    private void createSites() {
        this.sites = new AbstractSite[this.width+2][this.height+2];

        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        int ioCapacity = this.architecture.getIoCapacity();
        
        for(int i = 1; i < this.height + 1; i++) {
            this.sites[0][i] = new IOSite(0, i, ioType, ioCapacity);
            this.sites[this.width + 1][i] = new IOSite(this.width + 1, i, ioType, ioCapacity);
        }

        for(int i = 1; i < this.width + 1; i++) {
            this.sites[i][0] = new IOSite(i, 0, ioType, ioCapacity);
            this.sites[i][this.height + 1] = new IOSite(i, this.height + 1, ioType, ioCapacity);
        }

        for(int column = 1; column < this.width + 1; column++) {
            BlockType blockType = this.columns.get(column);
            
            int blockHeight = blockType.getHeight();
            for(int row = 1; row < this.height + 2 - blockHeight; row += blockHeight) {
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
        this.timingGraph.calculateCriticalities(true);
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

    public List<Macro> getMacros() {
        return this.macros;
    }


    /*****************
     * Default stuff *
     *****************/
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

    /**
     * Return the site at coordinate (x, y). If allowNull is false,
     * return the site that overlaps coordinate (x, y) but possibly
     * doesn't start at that position.
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

    public int getCapacity(BlockType blockType) {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        if(blockType.equals(ioType)) {
            return (this.height + this.width) * 2;

        } else {
            int numColumns = this.columnsPerBlockType.get(blockType).size();
            int columnHeight = this.height / blockType.getHeight();

            return numColumns * columnHeight;
        }
    }
    public List<AbstractSite> getSites(BlockType blockType) {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        List<AbstractSite> sites;

        if(blockType.equals(ioType)) {
            int ioCapacity = this.architecture.getIoCapacity();
            sites = new ArrayList<AbstractSite>((this.width + this.height) * 2 * ioCapacity);

            for(int n = 0; n < ioCapacity; n++) {
                for(int i = 1; i < this.height + 1; i++) {
                    sites.add(this.sites[0][i]);
                    sites.add(this.sites[this.width + 1][i]);
                }

                for(int i = 1; i < this.width + 1; i++) {
                    sites.add(this.sites[i][0]);
                    sites.add(this.sites[i][this.height + 1]);
                }
            }

        } else {
            List<Integer> columns = this.columnsPerBlockType.get(blockType);
            int blockHeight = blockType.getHeight();
            sites = new ArrayList<AbstractSite>(columns.size() * this.height);

            for(Integer column : columns) {
                for(int row = 1; row < this.height + 2 - blockHeight; row += blockHeight) {
                    sites.add(this.sites[column][row]);
                }
            }
        }

        return sites;
    }

    public List<Integer> getColumnsPerBlockType(BlockType blockType) {
        return this.columnsPerBlockType.get(blockType);
    }


    public GlobalBlock getRandomBlock(Random random) {
        int index = random.nextInt(this.globalBlockList.size());
        return this.globalBlockList.get(index);
    }

    public AbstractSite getRandomSite(
            BlockType blockType,
            int column,
            int columnDistance,
            int minRow,
            int maxRow,
            Random random) {

        int blockHeight = blockType.getHeight();
        int blockRepeat = blockType.getRepeat();

        // Get a random row
        int minRowIndex = (int) Math.ceil((minRow - 1.0) / blockHeight);
        int maxRowIndex = (maxRow - 1) / blockHeight;

        if(maxRowIndex == minRowIndex && columnDistance < blockRepeat) {
            return null;
        }

        // Get a random column
        List<Integer> candidateColumns = this.nearbyColumns.get(column).get(columnDistance);
        int randomColumn = candidateColumns.get(random.nextInt(candidateColumns.size()));

        // Get a random row
        int randomRow = 1 + blockHeight * (minRowIndex + random.nextInt(maxRowIndex + 1 - minRowIndex));

        // Return the site found at the random row and column
        return this.getSite(randomColumn, randomRow, false);
    }


    public double ratioUsedCLB(){
        int numCLB = 0;
        int numCLBPos = 0;
        for(BlockType clbType:BlockType.getBlockTypes(BlockCategory.CLB)){
        	numCLB += this.getBlocks(clbType).size();
        	numCLBPos += this.getColumnsPerBlockType(clbType).size() * this.getHeight();
        }
        double ratio = (double)numCLB / numCLBPos;
        
        return ratio;
    }
    public boolean dense(){
    	if(this.ratioUsedCLB() < 0.4){
        	return false;
        }else {
        	return true;
        }
    }
    public boolean sparse(){
    	return !this.dense();
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
