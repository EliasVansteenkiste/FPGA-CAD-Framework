package pack.netlist;

import java.util.HashSet;

import pack.util.Output;

public class Data {
	private String blif;
	
	private int maxBlockNumber;
	private int maxNetNumber;
	private int maxPinNumber;
	
	private HashSet<B> floatingBlocks;
	
	public Data(){
		this.blif = null;
		
		this.maxBlockNumber = 0;
		this.maxNetNumber = 0;
		this.maxPinNumber = 0;
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
	
	//MAX BLOCK, NET AND PIN NUMBER
	public void set_max_block_number(int max){
		this.maxBlockNumber = max;
	}
	public int get_max_block_number(){
		if(this.maxBlockNumber == 0){
			Output.println("Max block number is equal to 0");
		}
		return this.maxBlockNumber;
	}
	public void set_max_net_number(int max){
		this.maxNetNumber = max;
	}
	public int get_max_net_number(){
		if(this.maxNetNumber == 0){
			Output.println("Max net number is equal to 0");
		}
		return this.maxNetNumber;
	}
	public void set_max_pin_number(int max){
		this.maxPinNumber = max;
	}
	public int get_max_pin_number(){
		if(this.maxPinNumber == 0){
			Output.println("Max pin number is equal to 0");
		}
		return this.maxPinNumber;
	}
}
