package circuit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import architecture.RouteNode;

public class Net extends Block{

	public Set<RouteNode> routeNodes;
	
	public Pin source;
	public Vector<Pin> sinks;
	
	@Override
	public String toString() {
		return name;
	}
	
	public Net(Net net) {
		super(net.name, BlockType.NET);
		
//		blocks = new HashSet<Block>();
//		for (Block block:net.blocks)
//			this.blocks.add(block);
		
		sinks = new Vector<Pin>();
		this.sinks.addAll(net.sinks);
		source = net.source;
		routeNodes = new HashSet<RouteNode>(net.routeNodes);
		
		// Make sure all cons of the sinks are well connected
		for(int i = 0; i < sinks.size(); i++)
		{
			sinks.get(i).con = net.sinks.get(i).con;
		}
	}

	public Net(String name) {
		super(name, BlockType.NET);
		sinks = new Vector<Pin>();
		routeNodes = new HashSet<RouteNode>();
	}

	public Collection<Block> blocks() {
		Vector<Block> result = new Vector<Block>();
		result.add(source.owner);
		for (Pin sink: sinks) {
			result.add(sink.owner);
		}
		return result;
	}

	public void addSource(Pin output) {
		if (source==null){
			source = output;
			source.routingBlock = this;
			// Make sure all sinks are well connected
			for(int i = 0; i < sinks.size(); i++)
			{
				sinks.get(i).con = new Connection(source, sinks.get(i));
			}
		}else{
			System.out.println("Net "+source.name+" has  multiple sources!");
		}
	}

	public void addSink(Pin input) {
		if(!sinks.contains(input)){
			sinks.add(input);
			input.routingBlock = this;
			input.con = new Connection(this.source, input);
		}
	}

}
