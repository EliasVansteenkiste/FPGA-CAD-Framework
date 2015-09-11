package flexible_architecture.block;

import java.util.ArrayList;
import java.util.List;

import flexible_architecture.architecture.PortType;

import util.Logger;

public class Pin {
	
	private AbstractBlock owner;
	private PortType portType;
	private String portName;
	private int index;
	private Pin source;
	private List<Pin> sinks;
	
	public Pin(AbstractBlock owner, PortType portType, String portName, int index) {
		this.owner = owner;
		this.portType = portType;
		this.portName = portName;
		this.index = index;
		
		//TODO: is this memory efficient? Does the size remain 1 after 1 element is added?
		this.sinks = new ArrayList<Pin>(1);
	}
	
	public AbstractBlock getOwner() {
		return this.owner;
	}
	
	public Pin setSource(Pin source) {
		Pin oldSource = this.source;
		this.source = source;
		return oldSource;
	}
	
	public List<Pin> getSinks() {
		return this.sinks;
	}
	public Pin getSink(int index) {
		try {
			return this.sinks.get(index);
		} catch(IndexOutOfBoundsException exception) {
			Logger.raise("Port doesn't have " + index + "pins", exception);
			return null;
		}
	}
	public void addSink(Pin sink) {
		this.sinks.add(sink);
	}
	
	
	public String toString() {
		return this.owner.toString() + "." + this.portName + "[" + this.index + "]";
	}
}
