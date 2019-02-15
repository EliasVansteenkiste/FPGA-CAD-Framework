package route.circuit.resource;

import route.circuit.architecture.BlockType;

public class Site {

    private final int column, row, height;
    
    /***************************************
     * A site can contain multiple instances
     * This is the case for IOs, which have
     * a capacity of two. Each instance has 
     * its own input and output ports and 
     * its own global block
     ***************************************/
    private final Instance[] instances;
    private final int capacity;
    
    public Site(int column, int row, int height, BlockType blockType, int capacity) {
    	this.column = column;
        this.row = row;
        this.height = height;
        
    	this.capacity = capacity;
    	this.instances = new Instance[this.capacity];
    	for(int i = 0; i < this.capacity; i++){
    		this.instances[i] = new Instance(this, blockType);
    	}
    }

    public int getColumn() {
        return this.column;
    }
    public int getRow() {
        return this.row;
    }
    public int getHeigth() {
    	return this.height;
    }
    
    public Instance getInstance(int n) {
    	if(n > (this.capacity - 1)) {
    		throw new RuntimeException();
    	}
    	return this.instances[n];
    }
    
    public boolean addSource(Source source) {
    	String portName = source.getName();
    	for(Instance instance : this.instances) {
    		if(!instance.containsSource(portName)) {
    			instance.addSource(source);
    			return true;
    		}
    	}
    	return false;
    }
    public boolean addSink(Sink sink) {
    	String portName = sink.getName();
    	for(Instance instance : this.instances) {
    		if(!instance.containsSink(portName)) {
    			instance.addSink(sink);
    			return true;
    		}
    	}
    	return false;
    }
	
    @Override
    public int hashCode() {
        // 8191 is a prime number that is larger than any row index can possibly be,
        // plus it's a Mersenne prime which allows for bitwise optimization
        return 8191 * this.column + this.row;
    }

    @Override
    public String toString() {
    	String blockType = null;
    	for(Instance instance : this.instances) {
    		if(blockType == null) {
    			blockType = instance.getBlockType().toString();
    		} else if(!blockType.equals(instance.getBlockType().toString())) {
    			String blockTypesString = null;
    			for(Instance printInstance : this.instances) {
    				blockTypesString += printInstance.getBlockType().toString() + " ";
    			}
    			System.err.println("The site blocks of a site have a different blockType: " + blockTypesString);
    		}
    	}
    	if(this.height == 1){
    		return blockType + " [" + this.column + "," + this.row + "]\t" + "/" + this.capacity;
    	} else {
    		return blockType + " [" + this.column + "," + this.row + "] to [" + this.column + "," + (this.row + this.height - 1) + "]\t" + "/" + this.capacity;
    	}
    }
    
	public void sanityCheck() {
		for(Instance instance : this.instances) {
			instance.sanityCheck();
		}
		
    	String checkString = this.instances[0].checkString();
    	for(Instance instance : this.instances) {
    		if(!instance.checkString().equals(checkString)) {
    			System.err.println("The instances of " + this + " are not equal");
    		}
    	}
	}
}
