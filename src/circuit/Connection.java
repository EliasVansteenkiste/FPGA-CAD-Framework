package circuit;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import architecture.RouteNode;


public class Connection {
	public Pin source;
	public Pin sink;
	public Pin equivSink;
	public int fanin;
	public int fanout;
	public String name;
	public double weight;
	public Vector<Integer> modes;
	
	public Set<RouteNode> routeNodes;
	
	public Connection(String name) {
		super();
		this.name = name;
		this.fanin = 1;
		this.fanout = 1;
	}

	public Connection(Pin source, Pin sink) {
		super();
		this.sink = sink;
		this.source = source;
		this.routeNodes = new HashSet<RouteNode>();
		this.fanin = 1;
		this.fanout = 1;
	}
	
	public Connection(Pin source, Pin sink, Pin equivSink) {
		super();
		this.sink = sink;
		this.source = source;
		this.equivSink = equivSink;
		this.routeNodes = new HashSet<RouteNode>();
		this.name=source.toString()+"_"+sink.toString()+"_"+equivSink.toString();
		this.fanin = 1;
		this.fanout = 1;
	}
	
	public Connection(String name, Pin source, Pin sink) {
		super();
		this.name = name;
		this.sink = sink;
		this.source = source;
		this.routeNodes = new HashSet<RouteNode>();
		this.fanin = 1;
		this.fanout = 1;
	}
	
	public Connection(String name, Pin source, Pin sink, Pin equivSink) {
		super();
		this.name = name;
		this.sink = sink;
		this.source = source;
		this.equivSink = equivSink;
		this.routeNodes = new HashSet<RouteNode>();
		this.fanin = 1;
		this.fanout = 1;
	}
	
	public Connection(Connection con) {
		super();
		this.sink = con.sink;
		this.source = con.source;
		this.equivSink = con.equivSink;
		this.routeNodes = new HashSet<RouteNode>();
		this.routeNodes.addAll(con.routeNodes);
		this.fanin = 1;
		this.fanout = 1;
	}
	
	public void resetConnection(){
		this.routeNodes = new HashSet<RouteNode>();
	}
	
	public void installSelfReferredPins(){
		Connection soso = new Connection(source,source,source);
		source.con = soso;
		Connection sisi = new Connection(sink,sink,sink);
		sink.con = sisi;
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public boolean equals(Object o){
		if (o == null) return false;
	    if (!(o instanceof Connection)) return false;
	    Connection co = (Connection) o;
		if((co.sink==this.sink)&&(co.source==this.source)){
			return true;
		}
		else{
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		return this.sink.hashCode()^this.source.hashCode();
	}
	
}
