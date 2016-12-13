package pack.partition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import pack.netlist.B;
import pack.netlist.N;
import pack.netlist.P;
import pack.util.ErrorLog;

public class Part {
	private HashSet<B> blocks;
	private HashSet<B> primitivesDSP;
	private HashMap<HardBlockGroup,HashSet<B>> primitivesRAM;
	
	private int numDSPPrimitives;
	private int numRAMPrimitives;

	private int part;

	private ArrayList<ArrayList<B>> ramMolecules;
	
	public Part(){
		this.blocks = new HashSet<B>();
		this.primitivesDSP = new HashSet<B>();
		this.primitivesRAM = new HashMap<HardBlockGroup,HashSet<B>>();
		
		this.numDSPPrimitives = 0;
		this.numRAMPrimitives = 0;
		
		this.part = -1;
		
		this.ramMolecules = new ArrayList<ArrayList<B>>();
	}
	
	//PART NUMBER
	public void setPartNumber(int part){
		this.part = part;
	}
	public int getPartNumber(){
		return this.part;
	}
	
	//ADD AND REMOVE BLOCKS
	public void add(B b){
		b.set_part(this.part);
		this.blocks.add(b);
		if(b.get_type().equals("HALF_DSP")){
			this.numDSPPrimitives += 1;
			this.primitivesDSP.add(b);
		}else if(b.hasHardBlockGroup()){
			HardBlockGroup hbg = b.getHardBlockGroup();
			if(!this.primitivesRAM.containsKey(hbg)){
				this.primitivesRAM.put(hbg, new HashSet<B>());
			}
			this.primitivesRAM.get(hbg).add(b);
			this.numRAMPrimitives += 1;
		}
	}
	public void remove(B b){
		b.remove_part();
		if(this.blocks.remove(b) == false){
			ErrorLog.print("Block " + b.toString() + " not removed correctly");
		}
		if(b.get_type().equals("HALF_DSP")){
			this.numDSPPrimitives -= 1;
			this.primitivesDSP.remove(b);
		}else if(b.get_type().contains("stratixiv_ram_block")){
			HardBlockGroup hbg = b.getHardBlockGroup();
			this.primitivesRAM.get(hbg).remove(b);
			if(this.primitivesRAM.get(hbg).isEmpty()){
				this.primitivesRAM.remove(hbg);
			}
			this.numRAMPrimitives -= 1;
		}
		for(ArrayList<B> molecule:this.ramMolecules){
			if(molecule.contains(b)){
				ErrorLog.print("Block " + b.toString() + " is element of ram molecule and should not be removed");
			}
		}
	}
	
	//GETTERS
	public HashSet<B> getBlocks(){
		return this.blocks;
	}
	public HashSet<B> getDSPPrimitives(){
		return this.primitivesDSP;
	}
	public HashSet<B> getRAMPrimitives(HardBlockGroup rg){
		return this.primitivesRAM.get(rg);
	}
	public int numDSPPrimitives(){
		return this.numDSPPrimitives;
	}
	public int numRAMPrimitives(){
		return this.numRAMPrimitives;
	}
	

	public boolean isEmpty(){
		return this.blocks.isEmpty();
	}
	public int size(){
		return this.blocks.size();
	}
	public int area(){
		int area = 0;
		for(B b:this.blocks){
			area += b.get_area();
		}
		return area;
	}

	public boolean hasBlock(B b){
		return (b.get_part() == this.getPartNumber());
	}
	
	public int connections(B b){
		HashSet<N> nets = new HashSet<N>();
		for(String outputPort:b.get_output_ports()){
			for(P outputPin:b.get_output_pins(outputPort)){
				nets.add(outputPin.get_net());
			}
		}
		for(String inputPort:b.get_input_ports()){
			for(P inputPin:b.get_input_pins(inputPort)){
				nets.add(inputPin.get_net());
			}
		}
		int connections = 0;
		for(N n:nets){
			if(n.has_terminals()){
				if(this.isConnected(n, b)){
					connections += 1;
				}
			}else{
				if(this.isConnected(n, b)){
					connections += 2;
				}
			}
		}
		return connections;
	}
	private boolean isConnected(N n,B b){
		if(n.has_source()){
			B sourceBlock = n.get_source_pin().get_block();
			if(!sourceBlock.equals(b)){
				if(sourceBlock.get_part() == this.part){
					return true;
				}			
			}
		}
		for(P sinkPin:n.get_sink_pins()){
			B sinkBlock = sinkPin.get_block();
			if(!sinkBlock.equals(b)){
				if(sinkBlock.get_part() == this.part){
					return true;
				}
			}
		}
		return false;
	}
	
	//RAM MOLECULES
	public void addRamMolecule(ArrayList<B> molecule){
		this.ramMolecules.add(molecule);
	}
	public boolean hasRamMolecule(){
		return !this.ramMolecules.isEmpty();
	}
	public ArrayList<B> getRamMolecule(){
		return this.ramMolecules.remove(0);
	}
}