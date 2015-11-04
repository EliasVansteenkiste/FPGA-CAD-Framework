package circuit.pin;

import java.util.ArrayList;
import java.util.List;

import circuit.architecture.PortType;
import circuit.block.AbstractBlock;



import util.Logger;

public abstract class AbstractPin {
    
    private AbstractBlock owner;
    private PortType portType;
    private int index;
    
    private AbstractPin source;
    private List<AbstractPin> sinks;
    
    public AbstractPin(AbstractBlock owner, PortType portType, int index) {
        this.owner = owner;
        this.portType = portType;
        this.index = index;
        
        //TODO: is this memory efficient? Does the size remain 1 after 1 element is added?
        this.sinks = new ArrayList<AbstractPin>(1);
    }
    
    
    public AbstractBlock getOwner() {
        return this.owner;
    }
    public PortType getPortType() {
        return this.portType;
    }
    public String getPortName() {
        return this.portType.getName();
    }
    public int getIndex() {
        return this.index;
    }
    
    public boolean isOutput() {
        return this.portType.isOutput();
    }
    public boolean isInput() {
        return this.portType.isInput();
    }
    
    
    public AbstractPin getSource() {
        return this.source;
    }
    public void setSource(AbstractPin source) {
        this.source = source;
    }
    
    public int getNumSinks() {
        return this.sinks.size();
    }
    public List<AbstractPin> getSinks() {
        return this.sinks;
    }
    public AbstractPin getSink(int index) {
        try {
            return this.sinks.get(index);
        } catch(IndexOutOfBoundsException exception) {
            Logger.raise("Port doesn't have " + index + "pins", exception);
            return null;
        }
    }
    
    public void addSink(AbstractPin sink) {
        this.sinks.add(sink);
    }
    
    
    @Override
    public String toString() {
        return this.owner.toString() + "." + this.portType.getName() + "[" + this.index + "]";
    }
}
