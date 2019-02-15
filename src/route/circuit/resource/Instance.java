package route.circuit.resource;

import java.util.HashMap;
import java.util.Map;

import route.circuit.architecture.BlockType;
import route.circuit.block.GlobalBlock;

public class Instance {
	private final Site parent;
	
	private final BlockType blockType;
	private GlobalBlock block;
	 
	public final Map<String, Source> sources;
	public final Map<String, Sink> sinks;
		
	public Instance(Site parent, BlockType blockType) {
		this.parent = parent;
		
		this.blockType = blockType;
		this.block = null;
		
		this.sources = new HashMap<>();
		this.sinks = new HashMap<>();
	}
	
	public boolean isEmpty() {
		return this.block == null;
	}
	public void setBlock(GlobalBlock block) {
		this.block = block;
	}
	
	public boolean containsSource(String portName) {
		return this.sources.containsKey(portName);
	}
	public boolean containsSink(String portName) {
		return this.sinks.containsKey(portName);
	}
	
	public void addSource(Source source) {
		this.sources.put(source.getName(), source);
	}
	public void addSink(Sink sink) {
		this.sinks.put(sink.getName(), sink);
	}
	
	public Source getSource(String name) {
    	if(this.sources.containsKey(name)) {
    		return this.sources.get(name);
    	} else {
    		System.err.println("Source " + name + " not found!");
    		this.printSources();
    		return null;
    	}
    }
    public Sink getSink(String name) {
    	if(this.sinks.containsKey(name)) {
    		return this.sinks.get(name);
    	} else {
    		System.err.println("Sink " + name + " not found!");
    		this.printSinks();
    		return null;
    	}
    }
    
    private void printSources() {
    	System.out.println("Sources of " + this.parent.toString());
    	for(Source source : this.sources.values()) {
    		System.out.println("\t" + source.getName());
    	}
    }
    private void printSinks() {
    	System.out.println("Sinks of " + this.parent.toString());
    	for(Sink sink : this.sinks.values()) {
    		System.out.println("\t" + sink.getName());
    	}
    }
    
    public Site getParentSite() {
    	return this.parent;
    }
    public BlockType getBlockType() {
    	return this.blockType;
    }
    
	public void sanityCheck() {
		if(!this.isEmpty()) {
			if(this.block.getSiteInstance() != this) {
				throw new RuntimeException();
			}
		}
	}
	
	public String checkString() {
		StringBuffer checkString = new StringBuffer();
		checkString.append(this.blockType.toString());
		for(String sourcePort : this.sources.keySet()) {
			checkString.append(sourcePort);
		}
		for(String sinkPort : this.sinks.keySet()) {
			checkString.append(sinkPort);
		}
		return checkString.toString();
	}
}
