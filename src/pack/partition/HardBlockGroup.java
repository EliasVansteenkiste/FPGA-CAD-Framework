package pack.partition;

import java.util.ArrayList;
import java.util.HashSet;

import pack.netlist.B;
import pack.netlist.Model;
import pack.util.ErrorLog;

public class HardBlockGroup implements Comparable<HardBlockGroup>{
	private String type;
	private int availablePlacesInOneBlock;
	private int requiredBlocks;
	private int availableBlocks;
	private int freePositions;
	private HashSet<Integer> primitives;
	private int id;
	private static int globalIdCounter;
	
	public HardBlockGroup(ArrayList<B> blocks, Model model){
		this.type = null;
		this.primitives = new HashSet<Integer>();
		for(B b:blocks){
			if(this.type == null){
				this.type = b.get_type();
			}else if(!this.type.equals(b.get_type())){
				ErrorLog.print(this.type + " <!=> " + b.get_type());
			}
			if(!this.primitives.contains(b.get_number())){
				this.primitives.add(b.get_number());
			}else{
				ErrorLog.print("This hard block group already contains blockNumber " + b.get_number());
			}
		}
		
		//RAM TYPE
		if(model.get_stratixiv_ram_slices_9() > 0){
			this.availablePlacesInOneBlock = model.get_stratixiv_ram_slices_9();
		}else if(model.get_stratixiv_ram_slices_144() > 0){
			this.availablePlacesInOneBlock = model.get_stratixiv_ram_slices_144();
		}else{
			ErrorLog.print("Unexpected situation");
		}
		this.requiredBlocks = (int)Math.ceil((1.0*blocks.size())/(1.0*this.availablePlacesInOneBlock));
		this.availableBlocks = this.requiredBlocks;
		this.freePositions = (this.requiredBlocks*this.availablePlacesInOneBlock) - this.size();
		this.id = HardBlockGroup.globalIdCounter++;
	}
	public String getTame(){
		return this.type;
	}
	public HashSet<Integer> getPrimitiveNumbers(){
		return this.primitives;
	}
	public int size(){
		return this.primitives.size();
	}
	public int getIdNumber(){
		return this.id;
	}
	public int getNumberOfRequiredBlocks(){
		return this.requiredBlocks;
	}
	public int getNumberOfAvailablePlacesInOneBlock(){
		return this.availablePlacesInOneBlock;
	}
	public int getNumberOfAvailableBlocks(){
		return this.availableBlocks;
	}
	public int getNumberOfFreePositions(){
		return this.freePositions;
	}
	public void moleculeGenerated(ArrayList<B> primitiveAtoms){
		this.availableBlocks -= 1;
		this.freePositions -= (this.availablePlacesInOneBlock - primitiveAtoms.size());
		if(this.freePositions < 0){
			ErrorLog.print("To many unused positions in the group with type " + this.type + " | Free Postitions = " + this.freePositions);
		}
		for(B atom:primitiveAtoms){
			this.primitives.remove(atom.get_number());
		}
	}
	@Override public int hashCode() {
		return this.id;
    }
	@Override public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof HardBlockGroup)) return false;
	    HardBlockGroup otherMyClass = (HardBlockGroup)other;
	    if(otherMyClass.getIdNumber() == this.getIdNumber()) return true;
	    return false;
	}
	@Override public int compareTo(HardBlockGroup rg) {
		if(this.getIdNumber() == rg.getIdNumber()){
			return 0;
		}else if(this.getIdNumber() > rg.getIdNumber()){
			return -1;
		}else{
			return 1;
		}
	}
}
