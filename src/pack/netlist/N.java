package pack.netlist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import pack.util.ErrorLog;

public class N implements Comparable<N>{
	private String name;
	private int number;
	
	private P source;
	private HashSet<P> sinks;
	private HashSet<P> terminals;
	private boolean isCut;
	
	public N(String name, int number){
		this.name = name;
		this.number = number;
		
		this.source = null;
		this.sinks = new HashSet<P>();
		this.terminals = new HashSet<P>();
		this.isCut = false;
	}
	
	//GETTERS
	public String get_name(){
		return this.name;
	}
	public int get_number(){
		return this.number;
	}
	
	//TRIM AND CLEAN UP
	public void clean_up(){
		this.name = null;
		this.source = null;
		this.sinks = null;
		this.terminals = null;
	}
		
	//SOURCE
	public void set_source(P p){
		if(this.source == null){
			this.source = p;
		}else{
			ErrorLog.print("Try to add " + p.get_block().toString() + " as a source, this net " + this.toString() + " already has a source " + this.source.get_block().toString() + "!");
		}
	}
	public B get_source_block(){
		if(this.source == null){
			ErrorLog.print("Unable to return source block because the net " + this.toString() + " has no source");
			return null;
		}else{
			return this.source.get_block();
		}
	}
	public P get_source_pin(){
		if(this.source == null){
			ErrorLog.print("Unable to return source pin because the net " + this.toString() + " has no source");
			return null;
		}else{
			return this.source;
		}
	}
	public boolean has_source(){
		return !(this.source == null);
	}
	public void remove_source(P sourcePin){
		if(this.source.equals(sourcePin)){
			this.source = null;
		}else{
			ErrorLog.print("This is no source pin");
		}
	}
	
	//SINKS
	public void add_sink(P p){
		this.sinks.add(p);
	}
	public boolean has_sinks(){
		return !this.sinks.isEmpty();
	}
	public ArrayList<B> get_sink_blocks(){
		ArrayList<B> sinkBlocks = new ArrayList<B>();
		for(P sinkPin:this.sinks){
			sinkBlocks.add(sinkPin.get_block());
		}
		return sinkBlocks;
	}
	public HashSet<P> get_sink_pins(){
		return this.sinks;
	}
	public void remove_sink(P sink){
		this.sinks.remove(sink);//EACH PIN SHOULD BE UNIQUE
	}
	
	//Valid
	public boolean valid(){
		if(this.has_source() && this.has_sinks()){
			return true;
		}else if(this.has_terminals() && this.has_sinks()){
			return true;
		}else if(this.has_source() && this.has_terminals()){
			return true;
		}else{
			return false;
		}
	}
	public boolean has_two_nodes(){
		if(!this.has_sinks() && !this.has_terminals() && !this.has_source()){
			ErrorLog.print("Net " + this.toString() + " has no connections");
			return false;
		}
		if(this.get_sink_blocks().size() == 1 && !this.has_terminals() && !this.has_source()){
			ErrorLog.print("Net " + this.toString() + " only has a sink " + this.get_sink_blocks().get(0).toString() + " attached");
			return false;
		}
		if(!this.has_sinks() && !this.has_terminals() && this.has_source()){
			ErrorLog.print("Net " + this.toString() + " only has a source " + this.get_source_block().toString() + " attached");
			return false;
		}
		if(!this.has_sinks() && this.has_terminals() && !this.has_source()){
			ErrorLog.print("Net " + this.toString() + " only has " + this.terminals.size() + " terminals and not blocks attached");
			return false;
		}
		return true;
	}
	
	//PIN
	public void set_terminal_pin(P p){
		if(!this.has_terminals()){
			this.terminals.add(p);
			if(p.get_terminal().is_cut_type()){
				this.isCut = true;
			}
		}else{
			ErrorLog.print("Try to add " + p.get_terminal().toString() + " as a terminal, this net already has " + this.terminals.size() + " terminals");
		}
	}
	public void add_terminal_pin(P p){
		this.terminals.add(p);
		if(p.get_terminal().is_cut_type()){
			this.isCut = true;
		}
	}
	public HashSet<P> get_terminal_pins(){
		return this.terminals;
	}
	public boolean has_terminals(){
		return !this.terminals.isEmpty();
	}
	public boolean is_cut(){
		return this.isCut;
	}
	
	//GENERAL FUNCTIONALITY
	public ArrayList<B> get_blocks(){
		ArrayList<B> blocks = new ArrayList<B>();
		if(this.has_source()){
			blocks.add(this.get_source_block());
		}
		blocks.addAll(this.get_sink_blocks());
		return blocks;
	}
	public int fanout(){
		if(this.has_sinks()){
			return this.sinks.size();
		}else{
			return 0;
		}
	}
	
	//STRING
	public String toString(){
		return this.get_name();
	}
	
	//ADD NET TO METIS PARTITIONING
	public boolean add_net_to_hmetis_partitioning(int maxFanout){
		if(this.has_source()){
			if(this.has_sinks()){
				if(this.get_sink_pins().size()<=maxFanout){
					return true;
				}else{
					//this.test_net_for_critical_connections();
					return false;
				}
			}else{
				return false;
			}
		}else if(this.has_sinks()){
			if(this.get_sink_pins().size() > 1){
				if(this.get_sink_pins().size()<=maxFanout){
					return true;
				}else{
					//this.test_net_for_critical_connections();
					return false;
				}
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	//OVERRIDE
	@Override public int compareTo(N n){
		if(this.get_number() == n.get_number()){
			return 0;
		}else if(this.get_number() > n.get_number()){
			return 1;
		}else{
			return -1;
		}
	}
	public static Comparator<N> NetFanoutComparator = new Comparator<N>(){
		public int compare(N n1, N n2){
			if(n1.fanout() == n2.fanout()){
				return 0;
			}else if(n1.fanout() > n2.fanout()){
				return 1;
			}else{
				return -1;
			}
		}
	};
	@Override public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof N)) return false;
	    N otherMyClass = (N)other;
	    if(otherMyClass.get_number() == this.get_number()) return true;
	    return false;
	}
	@Override public int hashCode() {
		return this.number;
    }
}
