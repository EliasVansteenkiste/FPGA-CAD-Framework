package pack.partition;

import pack.netlist.P;

public class Edge{
	private P source;
	private P sink;
	private int net_weight;
	private String name;
	
	public Edge(P src, P snk, int nw, String n){
		this.source = src;
		this.sink = snk;
		this.net_weight = nw;
		this.name = n;
	}
	public int getNetWeight(){
		return this.net_weight;
	}
	public P getSource(){
		return this.source;
	}
	public P getSink(){
		return this.sink;
	}
	public String getName(){
		return this.name;
	}
}
