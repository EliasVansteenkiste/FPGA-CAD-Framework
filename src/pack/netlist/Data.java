package pack.netlist;

import java.util.HashSet;

public class Data {
	private String blif;
	private HashSet<B> floatingBlocks;
	
	public Data(){
		this.blif = null;
	}
	
	//FLOATING BLOCKS
	public void add_floating_block(B b){
		if(this.floatingBlocks == null){
			this.floatingBlocks = new HashSet<B>();
		}
		this.floatingBlocks.add(b);
	}
	public HashSet<B> get_floating_blocks(){
		return this.floatingBlocks;
	}
	
	//BLIF
	public void set_blif(String blif){
		this.blif = blif;
	}
	public String get_blif(){
		return this.blif;
	}
}
