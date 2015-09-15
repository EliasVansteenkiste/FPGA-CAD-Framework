package flexible_architecture.pin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import flexible_architecture.architecture.PortType;
import flexible_architecture.block.AbstractBlock;

import util.Logger;

public abstract class AbstractPin {
	
	private AbstractBlock owner;
	private PortType portType;
	private String portName;
	private int index;
	
	private AbstractPin source;
	private List<AbstractPin> sinks;
	
	public AbstractPin(AbstractBlock owner, PortType portType, String portName, int index) {
		this.owner = owner;
		this.portType = portType;
		this.portName = portName;
		this.index = index;
		
		//TODO: is this memory efficient? Does the size remain 1 after 1 element is added?
		this.sinks = new ArrayList<AbstractPin>(1);
	}
	
	public AbstractBlock getOwner() {
		return this.owner;
	}
	
	public AbstractPin setSource(AbstractPin source) {
		AbstractPin oldSource = this.source;
		this.source = source;
		return oldSource;
	}
	
	public int getNumSinks() {
		return this.sinks.size();
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
	
	
	public String toString() {
		return this.owner.toString() + "." + this.portName + "[" + this.index + "]";
	}
}
