package visual;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import circuit.Circuit;
import circuit.block.GlobalBlock;

class Placement {

    private String name;
    private Circuit circuit;
    private int numBlocks;

    private Map<GlobalBlock, Coordinate> blocks;


    Placement(String name, Circuit circuit) {
        this.initializeData(name, circuit);

        for(GlobalBlock block : this.circuit.getGlobalBlocks()) {
            this.blocks.put(block, new Coordinate(block.getColumn(), block.getRow()));
        }
    }


    Placement(String name, Circuit circuit, Map<GlobalBlock, Integer> blockIndexes, int[] x, int[] y) {
        this.initializeData(name, circuit);

        for(Map.Entry<GlobalBlock, Integer> blockIndexEntry : blockIndexes.entrySet()) {
            GlobalBlock block = blockIndexEntry.getKey();
            int index = blockIndexEntry.getValue();
            this.blocks.put(block, new Coordinate(x[index], y[index]));
        }
    }

    Placement(String name, Circuit circuit, Map<GlobalBlock, Integer> blockIndexes, double[] x, double[] y) {
        this.initializeData(name, circuit);

        for(Map.Entry<GlobalBlock, Integer> blockIndexEntry : blockIndexes.entrySet()) {
            GlobalBlock block = blockIndexEntry.getKey();
            int index = blockIndexEntry.getValue();
            this.blocks.put(block, new Coordinate(x[index], y[index]));
        }
    }

    private void initializeData(String name, Circuit circuit) {
        this.name = name;
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
}
