package route.circuit;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import route.circuit.architecture.Architecture;
import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.BlockType;
import route.circuit.block.AbstractBlock;
import route.circuit.block.GlobalBlock;
import route.circuit.pin.AbstractPin;
import route.circuit.pin.Pin;
import route.circuit.resource.ResourceGraph;
import route.circuit.timing.TimingGraph;
import route.route.Connection;
import route.route.Net;

public class Circuit {
    private String name;
    private int width, height;

    private Architecture architecture;
    private TimingGraph timingGraph;
    private ResourceGraph resourceGraph;

    private Set<String> globalNets;
    private Map<BlockType, List<AbstractBlock>> blocks;
    
	private Set<Connection> cons;
	private Set<Net> nets;
	private boolean conRouted;

    private List<BlockType> globalBlockTypes;
    private List<GlobalBlock> globalBlockList = new ArrayList<GlobalBlock>();
    
    private List<BlockType> columns;
    private Map<BlockType, List<Integer>> columnsPerBlockType;
    private List<List<List<Integer>>> nearbyColumns;
    
    public Circuit(String name, Architecture architecture, Map<BlockType, List<AbstractBlock>> blocks) {
        this.name = name;
        this.architecture = architecture;

        this.blocks = blocks;
        
        this.timingGraph = new TimingGraph(this);
        this.resourceGraph = new ResourceGraph(this);
    }
    
    public void initializeData() {
        this.loadBlocks();

        this.timingGraph.build();
        this.resourceGraph.build();

        for(List<AbstractBlock> blocksOfType : this.blocks.values()) {
            for(AbstractBlock block : blocksOfType) {
                block.compact();
            }
        }
        
        this.initializeGlobalNets();
    }
    
    private void initializeGlobalNets() {
    	this.globalNets = new HashSet<>();
    	this.globalNets.add("vcc");
    	this.globalNets.add("gnd");
    	
    	BufferedReader br = null;
    	try {
			br = new BufferedReader(new FileReader(this.architecture.getSDCFile()));
			
			String line = null;
			while((line = br.readLine()) != null){
				line = line.trim();
				
				if(line.contains("create_clock") && !line.contains("-name") && line.contains("-period")) {
					line = line.replace("\n", "");
					line = line.replace("\t", "");
					while(line.contains("  ")) line = line.replace("  ", " ");
					
					line = line.replace("{ ", "{");
					line = line.replace(" }", "}");
					
					String globalNet = line.split(" ")[3];
					
					globalNet = globalNet.replace("\\\\", "\\");
					globalNet = globalNet.replace("\\|", "|");
					globalNet = globalNet.replace("\\[", "[");
					globalNet = globalNet.replace("\\]", "]");
					
					if(globalNet.charAt(0) == '{' && globalNet.charAt(globalNet.length() - 1) == '}') {
						globalNet = globalNet.substring(1, globalNet.length() - 1);
					}
					
					globalNet = globalNet.trim();
					
					this.globalNets.add(globalNet);
				}
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
        
        this.createColumns();
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
    
    /***************
     * CONNECTIONS *
     ***************/
    public void loadNetsAndConnections() {
    	short boundingBoxRange = 3;
    	
    	this.cons = new HashSet<>();
    	this.nets = new HashSet<>();
        
        int id = 0;
    	for(GlobalBlock globalBlock : this.getGlobalBlocks()) {
        	for(AbstractPin abstractSourcePin : globalBlock.getOutputPins()) {
        		Pin sourcePin = (Pin) abstractSourcePin;
        		
        		if(sourcePin.getNumSinks() > 0) {
        			String netName = sourcePin.getNetName();
            		
        			if(!this.globalNets.contains(netName)) {
    	        		Set<Connection> net = new HashSet<>();
    	        		
    	        		for(AbstractPin abstractSinkPin : sourcePin.getSinks()) {
    	        			Pin sinkPin = (Pin) abstractSinkPin;
    	        			
    	        			Connection c = new Connection(id, sourcePin, sinkPin);
    	        			this.cons.add(c);
    	        			net.add(c);
    	        			
    	        			id++;
    	        		}
    	        		
    	        		this.nets.add(new Net(net, boundingBoxRange));
        			}
        		}
        	}
        }
    }
    public int maximumNetLength() {
    	int maximumNetLength = 0;
    	for(Net net : this.nets) {
    		int netLength = net.wireLength();
    		if(netLength > maximumNetLength) {
    			maximumNetLength = netLength;
    		}
    	}
    	return maximumNetLength;
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
    public ResourceGraph getResourceGraph() {
    	return this.resourceGraph;
    }
    
    public BlockType getColumnType(int column) {
        return this.columns.get(column);
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

    public List<Integer> getColumnsPerBlockType(BlockType blockType) {
        return this.columnsPerBlockType.get(blockType);
    }

    public GlobalBlock getRandomBlock(Random random) {
        int index = random.nextInt(this.globalBlockList.size());
        return this.globalBlockList.get(index);
    }


    public double ratioUsedCLB(){
        int numCLB = 0;
        int numCLBPos = 0;
        for(BlockType clbType:BlockType.getBlockTypes(BlockCategory.CLB)){
        	numCLB += this.getBlocks(clbType).size();
        	numCLBPos += this.getColumnsPerBlockType(clbType).size() * this.getHeight();
        }
        double ratio = ((double) numCLB) / numCLBPos;
        
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
    
    public void setConRouted(boolean value) {
    	this.conRouted = value;
    }
    public boolean conRouted() {
    	return this.conRouted;
    }
    
    public Set<Connection> getConnections() {
    	return this.cons;
    }
    public Set<Net> getNets() {
    	return this.nets;
    }
 
    @Override
    public String toString() {
        return this.getName();
    }
}
