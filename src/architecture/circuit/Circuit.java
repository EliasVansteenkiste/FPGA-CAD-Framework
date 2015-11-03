package architecture.circuit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import timing_graph.TimingGraph;

import architecture.circuit.block.AbstractBlock;
import architecture.circuit.block.AbstractSite;
import architecture.circuit.block.BlockType;
import architecture.circuit.block.FlexibleArchitecture;
import architecture.circuit.block.GlobalBlock;
import architecture.circuit.block.IOSite;
import architecture.circuit.block.Site;
import architecture.circuit.block.BlockType.BlockCategory;
import architecture.circuit.pin.AbstractPin;
import architecture.circuit.pin.GlobalPin;


public class Circuit {
    
    private String name;
    private int width, height;
    
    
    private FlexibleArchitecture architecture;
    private TimingGraph timingGraph;
    
    private Map<BlockType, List<AbstractBlock>> blocks;
    private List<GlobalBlock> globalBlockList = new ArrayList<GlobalBlock>();
    private List<BlockType> globalBlockTypes;
    
    private List<BlockType> columns;
    private Map<BlockType, List<Integer>> columnsPerBlockType;
    
    private AbstractSite[][] sites;
    
    
    public Circuit(String name, FlexibleArchitecture architecture) {
        this.name = name;
        this.architecture = architecture;
    }
    
    public void loadBlocks(Map<BlockType, List<AbstractBlock>> blocks) {
        this.blocks = blocks;
        
        this.createGlobalBlockList();
        
        this.calculateSizeAndColumns(true);
        this.createSites();
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        
        this.calculateSizeAndColumns(false);
        this.createSites();
    }
    
    
    /*************************
     * Timing graph wrapping *
     *************************/
    public void setTimingGraph(TimingGraph timingGraph) {
        this.timingGraph = timingGraph;
    }
    public TimingGraph getTimingGraph() {
        return this.timingGraph;
    }
    
    public void recalculateTimingGraph() {
        this.timingGraph.recalculateAllSlackCriticalities();
    }
    public double calculateTimingCost() {
        return this.timingGraph.calculateTotalCost();
    }
    public double getMaxDelay() {
        return this.timingGraph.getMaxDelay();
    }
    
    
    @SuppressWarnings("unchecked")
    private void createGlobalBlockList() {
        this.globalBlockTypes = BlockType.getGlobalBlockTypes();
        
        for(BlockType blockType : this.globalBlockTypes) {
            if(!this.blocks.containsKey(blockType)) {
                this.blocks.put(blockType, new ArrayList<AbstractBlock>(0));
            }
            
            this.globalBlockList.addAll((List<GlobalBlock>) (List<?>) this.blocks.get(blockType));
        }
        
        // This is not necessary for anything
        //Collections.sort(this.globalBlockList);
    }
    
    public List<GlobalBlock> getGlobalBlocks() {
        return this.globalBlockList;
    }
    
    
    
    private void calculateSizeAndColumns(boolean autoSize) {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        BlockType clbType = BlockType.getBlockTypes(BlockCategory.CLB).get(0);
        List<BlockType> hardBlockTypes = BlockType.getBlockTypes(BlockCategory.HARDBLOCK);
        
        
        this.columnsPerBlockType = new HashMap<BlockType, List<Integer>>();
        this.columnsPerBlockType.put(ioType, new ArrayList<Integer>());
        this.columnsPerBlockType.put(clbType, new ArrayList<Integer>());
        for(BlockType blockType : hardBlockTypes) {
            this.columnsPerBlockType.put(blockType, new ArrayList<Integer>());
        }
        
        
        int numClbColumns = 0;
        int[] numHardBlockColumns = new int[hardBlockTypes.size()];
        for(int i = 0; i < hardBlockTypes.size(); i++) {
            numHardBlockColumns[i] = 0;
        }
        
        
        this.columns = new ArrayList<BlockType>();
        this.columns.add(ioType);
        int size = 2;
        
        boolean tooSmall = true;
        while(tooSmall) {
            for(int i = 0; i < hardBlockTypes.size(); i++) {
                BlockType hardBlockType = hardBlockTypes.get(i);
                int start = hardBlockType.getStart();
                int repeat = hardBlockType.getRepeat();
                
                if((size - 1 - start) % repeat == 0) {
                    this.columns.add(hardBlockType);
                    numHardBlockColumns[i]++;
                    break;
                }
            }
            
            if(this.columns.size() < size) {
                this.columns.add(clbType);
                numClbColumns++;
            }
            
            size++;
            
            if(autoSize) {
                tooSmall = false;
                
                int clbCapacity = (int) ((size - 2) * numClbColumns * this.architecture.getFillGrade());
                int ioCapacity = (size - 2) * 4 * this.architecture.getIoCapacity();
                if(clbCapacity < this.blocks.get(clbType).size() || ioCapacity < this.blocks.get(ioType).size()) { 
                    tooSmall = true;
                    continue;
                }
                
                for(int i = 0; i < hardBlockTypes.size(); i++) {
                    BlockType hardBlockType = hardBlockTypes.get(i);
                    
                    if(!this.blocks.containsKey(hardBlockType)) {
                        continue;
                    }
                    
                    int heightPerBlock = hardBlockType.getHeight();
                    int blocksPerColumn = (size - 2) / heightPerBlock;
                    int capacity = numHardBlockColumns[i] * blocksPerColumn;
                    
                    if(capacity < this.blocks.get(hardBlockType).size()) {
                        tooSmall = true;
                        break;
                    }
                }
                
            } else {
                tooSmall = (size != this.width);
            }
        }
        
        
        this.columns.add(ioType);
        this.width = size;
        this.height = size;
        
        for(int i = 0; i < this.columns.size(); i++) {
            this.columnsPerBlockType.get(this.columns.get(i)).add(i);
        }
    }
    
    private void createSites() {
        this.sites = new AbstractSite[this.width][this.height];
        
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        int ioCapacity = this.architecture.getIoCapacity();
        
        int size = this.width;
        for(int i = 1; i < size - 1; i++) {
            this.sites[0][i] = new IOSite(0, i, ioType, ioCapacity);
            this.sites[i][size-1] = new IOSite(i, size-1, ioType, ioCapacity);
            this.sites[size-1][size-1-i] = new IOSite(size-1, size-1-i, ioType, ioCapacity);
            this.sites[size-1-i][0] = new IOSite(size-1-i, 0, ioType, ioCapacity);
        }
        
        for(int x = 1; x < this.columns.size() - 1; x++) {
            BlockType blockType = this.columns.get(x);
            
            int blockHeight = blockType.getHeight();
            for(int y = 1; y < size - blockHeight; y += blockHeight) {
                this.sites[x][y] = new Site(x, y, blockType);
            }
        }
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
    public FlexibleArchitecture getArchitecture() {
        return this.architecture;
    }
    
    
    public BlockType getColumnType(int x) {
        return this.columns.get(x);
    }
    
    /*
     * Return the site at coordinate (x, y). If allowNull is false,
     * return the site that overlaps coordinate (x, y) but may not
     * start at that position.
     */
    public AbstractSite getSite(int x, int y) {
        return this.getSite(x, y, false);
    }
    public AbstractSite getSite(int x, int y, boolean allowNull) {
        if(allowNull) {
            return this.sites[x][y];
        
        } else {
            AbstractSite site = null;
            int topY = y;
            while(site == null) {
                site = this.sites[x][topY];
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
            int size = this.width;
            int ioCapacity = this.architecture.getIoCapacity();
            sites = new ArrayList<AbstractSite>((size - 1) * 4);
            
            for(int i = 1; i < size - 1; i++) {
                for(int n = 0; n < ioCapacity; n++) {
                    sites.add(this.sites[0][i]);
                    sites.add(this.sites[i][size-1]);
                    sites.add(this.sites[size-1][size-1-i]);
                    sites.add(this.sites[size-1-i][0]);
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
        
        if(distance < block.getType().getHeight() && distance < block.getType().getRepeat()) {
            return null;
        }
        
        int minX = Math.max(0, block.getX() - distance);
        int maxX = Math.min(this.width - 1, block.getX() + distance);
        int minY = Math.max(0, block.getY() - distance);
        int maxY = Math.min(this.height - 1, block.getY() + distance);
        
        //TODO: this should be smarter: only consider sites of the same BlockType
        while(true) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;
            
            AbstractSite randomSite = this.getSite(x, y, true);
            if(randomSite == null || block.getType().equals(randomSite.getType())) {
                return randomSite;
            }
        }
    }
    
    public List<GlobalPin> getGlobalOutputPins() {
        List<GlobalPin> globalPins = new ArrayList<GlobalPin>();
        
        for(AbstractBlock block : this.globalBlockList) {
            List<AbstractPin> pins = block.getOutputPins();
            for(AbstractPin pin : pins) {
                globalPins.add((GlobalPin) pin);
            }
        }
        
        return globalPins;
    }
    
    
    public String toString() {
        return this.getName();
    }
}
