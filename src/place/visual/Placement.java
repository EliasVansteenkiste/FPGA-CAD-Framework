package place.visual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

class Placement {

    private String name;
    private Circuit circuit;
    private int numBlocks;

    private Map<GlobalBlock, Coordinate> blocks;
    private double bbCost;
    
    private HashMap<BlockType,ArrayList<int[]>> legalizationAreas;


    Placement(String name, Circuit circuit) {
        this.initializeData(name, circuit);

        for(GlobalBlock block : this.circuit.getGlobalBlocks()) {
            this.blocks.put(block, new Coordinate(block.getColumn(), block.getRow()));
        }
        
        this.legalizationAreas = new HashMap<BlockType,ArrayList<int[]>>();
        this.bbCost = -1.0;

    }


    Placement(String name, Circuit circuit, Map<GlobalBlock, NetBlock> blockIndexes, int[] x, int[] y, HashMap<BlockType,ArrayList<int[]>> legalizationAreas, double bbCost) {
        this.initializeData(name, circuit);

        for(Map.Entry<GlobalBlock, NetBlock> blockIndexEntry : blockIndexes.entrySet()) {
            GlobalBlock block = blockIndexEntry.getKey();
            NetBlock netBlock = blockIndexEntry.getValue();

            int index = netBlock.getBlockIndex();
            float offset = netBlock.getOffset();

            this.blocks.put(block, new Coordinate(x[index], y[index] + Math.ceil(offset)));
        }
        
        this.legalizationAreas = legalizationAreas;
        this.bbCost = bbCost;
    }

    Placement(String name, Circuit circuit, Map<GlobalBlock, NetBlock> blockIndexes, double[] x, double[] y, HashMap<BlockType,ArrayList<int[]>> legalizationAreas, double bbCost) {
        this.initializeData(name, circuit);

        for(Map.Entry<GlobalBlock, NetBlock> blockIndexEntry : blockIndexes.entrySet()) {
            GlobalBlock block = blockIndexEntry.getKey();
            NetBlock netBlock = blockIndexEntry.getValue();

            int index = netBlock.getBlockIndex();
            float offset = netBlock.getOffset();

            this.blocks.put(block, new Coordinate(x[index], y[index] + Math.ceil(offset)));
        }
        
        this.legalizationAreas = legalizationAreas;
        this.bbCost = bbCost;
    }

    private void initializeData(String name, Circuit circuit) {
        this.name = circuit.getName() + " | " + name;
        this.circuit = circuit;
        this.numBlocks = circuit.getGlobalBlocks().size();

        this.blocks = new HashMap<GlobalBlock, Coordinate>();
    }



    public String getName() {
        return this.name;
    }
    public int getNumBlocks() {
        return this.numBlocks;
    }
    public int getWidth() {
        return this.circuit.getWidth();
    }
    public int getHeight() {
        return this.circuit.getHeight();
    }

    public Set<Map.Entry<GlobalBlock, Coordinate>> blocks() {
        return this.blocks.entrySet();
    }
    public HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return this.legalizationAreas;
    }
    public boolean hasBBCost(){
    	return !(this.bbCost < 0.0);
    }
    public double getBBCost(){
    	return this.bbCost;
    }
}
