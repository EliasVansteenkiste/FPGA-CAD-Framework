package flexible_architecture;

import java.util.HashMap;
import java.util.Map;

import flexible_architecture.block.AbstractBlock;

public class TupleBlockMap {
	
	private AbstractBlock block;
	private Map<String, String> map;
	
	public TupleBlockMap(AbstractBlock block) {
		this(block, new HashMap<String, String>());
	}
	
	public TupleBlockMap(AbstractBlock block, Map<String, String> map) {
		this.block = block;
		this.map = map;
	}
	
	public AbstractBlock getBlock() {
		return this.block;
	}
	
	public Map<String, String> getMap() {
		return this.map;
	}
}
