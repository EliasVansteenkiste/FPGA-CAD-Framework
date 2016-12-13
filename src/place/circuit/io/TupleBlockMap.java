package place.circuit.io;

import java.util.HashMap;
import java.util.Map;

import place.circuit.block.AbstractBlock;


class TupleBlockMap {

    private AbstractBlock block;
    private Map<String, String> map;

    TupleBlockMap(AbstractBlock block) {
        this(block, new HashMap<String, String>());
    }

    TupleBlockMap(AbstractBlock block, Map<String, String> map) {
        this.block = block;
        this.map = map;
    }

    AbstractBlock getBlock() {
        return this.block;
    }

    Map<String, String> getMap() {
        return this.map;
    }
}
