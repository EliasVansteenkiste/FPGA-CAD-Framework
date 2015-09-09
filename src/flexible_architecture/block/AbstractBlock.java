package flexible_architecture.block;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.net.AbstractNet;

public abstract class AbstractBlock {
	
	private String name;
	private BlockType type;
	
	protected LocalBlock[] children;
	private int numChildren = 0;
	
	private Map<String, AbstractNet[]> inputs;
	private Map<String, AbstractNet[]> outputs;
	
	
	public AbstractBlock(String name, BlockType type) {
		this.name = name;
		this.type = type;
		
		this.children = new LocalBlock[type.getMaxChildren()];
		
		this.inputs = new HashMap<String, AbstractNet[]>();
		this.outputs = new HashMap<String, AbstractNet[]>();
	}
	
	
	public void addChild(LocalBlock child) {
		this.children[this.numChildren] = child;
		this.numChildren++;
	}
	public AbstractBlock[] getChildren() {
		return Arrays.copyOfRange(this.children, 0, this.numChildren);
	}
	
	
	public Map<String, AbstractNet[]> getInputs() {
		return this.inputs;
	}
	public AbstractNet[] getInputs(String type) {
		if(this.inputs.containsKey(type)) {
			return this.inputs.get(type);
		} else {
			return null;
		}
	}
	public void setInputs(Map<String, AbstractNet[]> inputs) {
		this.inputs = inputs;
	}
	
	
	public Map<String, AbstractNet[]> getOutputs() {
		return this.outputs;
	}
	public AbstractNet[] getOutputs(String type) {
		if(this.outputs.containsKey(type)) {
			return this.outputs.get(type);
		} else {
			return null;
		}
	}
	public void setOutputs(Map<String, AbstractNet[]> outputs) {
		this.outputs = outputs;
	}
}
