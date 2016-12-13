package pack.partition;

import java.util.HashMap;
import java.util.HashSet;

import pack.netlist.B;
import pack.netlist.P;
import pack.util.ErrorLog;
import pack.util.Output;
import pack.util.Util;

public class CutEdges {
	private int maxArrivalTime;
	private HashMap<String,HashMap<String,Integer>> remainigInputTime;
	private HashMap<String,HashMap<String,Integer>> remainigOutputTime;
	private HashSet<String> cutNetNames;
	
	public CutEdges(int maxArrivalTime){
		this.maxArrivalTime = maxArrivalTime;
		this.remainigInputTime = new HashMap<String,HashMap<String,Integer>>();
		this.remainigOutputTime = new HashMap<String,HashMap<String,Integer>>();
		this.cutNetNames = new HashSet<String>();
	}
	public void addCriticalEdge(Edge critEdge){
		this.cutNetNames.add(critEdge.getName());
		P sourcePin = critEdge.getSource();
		String sourcePinAtomId = new String();
		if(sourcePin.has_block()){
			B b = sourcePin.get_block();
			if(b.has_atoms()){
				if(Util.isMoleculeType(b.get_type())){
					String pinPortName = sourcePin.get_port();
					String blockName = pinPortName.substring(0, pinPortName.indexOf("."));
					String portName = pinPortName.substring(pinPortName.indexOf(".") + 1,pinPortName.length());
					B atom = null;
					for(B ab:b.get_atoms()){
						if(ab.get_name().equals(blockName)){
							if(atom == null){
								atom = ab;
							}else{
								ErrorLog.print("Atom with name " + blockName + " already found");
							}
						}
					}
					if(atom == null){
						ErrorLog.print("No atom block with name " + blockName + " found");
					}
					sourcePinAtomId = "b" + atom.get_number() + "_" + "n" + sourcePin.get_net().get_number() + "_" + portName;
				}else{
					ErrorLog.print("Unexpected block type: " + b.get_type());
				}
			}else{
				sourcePinAtomId = sourcePin.get_id();
			}
		}else{
			sourcePinAtomId = sourcePin.get_id();
		}
		P sinkPin = critEdge.getSink();
		String sinkPinAtomId = new String();
		if(sinkPin.has_block()){
			B b = sinkPin.get_block();
			if(b.has_atoms()){
				if(Util.isMoleculeType(b.get_type())){
					String pinPortName = sinkPin.get_port();
					String blockName = pinPortName.substring(0, pinPortName.indexOf("."));
					String portName = pinPortName.substring(pinPortName.indexOf(".") + 1,pinPortName.length());
					B atom = null;
					for(B ab:b.get_atoms()){
						if(ab.get_name().equals(blockName)){
							if(atom == null){
								atom = ab;
							}else{
								ErrorLog.print("Atom with name " + blockName + " already found");
							}
						}
					}
					if(atom == null){
						ErrorLog.print("No atom block with name " + blockName + " found");
					}
					sinkPinAtomId = "b" + atom.get_number() + "_" + "n" + sinkPin.get_net().get_number() + "_" + portName;
					//System.out.println("sinkPinAtomId: " + sinkPinAtomId);
				}else{
					ErrorLog.print("Unexpected block type: " + b.get_type());
				}
			}else{
				sinkPinAtomId = sinkPin.get_id();
			}
		}else{
			sinkPinAtomId = sinkPin.get_id();
		}
		
		int outputDelay = this.maxArrivalTime - sourcePin.get_required_time();
		int inputDelay = sinkPin.get_arrival_time();
	
		if(!this.remainigOutputTime.containsKey(critEdge.getName())){
			this.remainigOutputTime.put(critEdge.getName(), new HashMap<String,Integer>());
			this.remainigOutputTime.get(critEdge.getName()).put(sourcePinAtomId, outputDelay);
		}else{
			if(this.remainigOutputTime.get(critEdge.getName()).keySet().size() != 1){
				ErrorLog.print("Size of the keyset should be 1, instead the size is equal to " + this.remainigOutputTime.get(critEdge.getName()).keySet().size() );
			}
			for(String key:this.remainigOutputTime.get(critEdge.getName()).keySet()){
				if(this.remainigOutputTime.get(critEdge.getName()).get(key) != (outputDelay)){
					Output.println("Different remaining output times found");
					Output.println("this.remainigOutputTime.get(critEdge.get_name()).get(key): " + this.remainigOutputTime.get(critEdge.getName()).get(key));
					ErrorLog.print("this.maxArrivalTime - sourcePin.get_required_time(): " + (outputDelay));
				}
			}
		}
		if(this.remainigInputTime.containsKey(critEdge.getName())){
			if(this.remainigInputTime.get(critEdge.getName()).containsKey(sinkPinAtomId)){
				System.out.println("Crit edge: " + critEdge.getName());
				System.out.println("Sink pin: " + sinkPinAtomId);
				ErrorLog.print("Unexpected situation");
			}
			this.remainigInputTime.get(critEdge.getName()).put(sinkPinAtomId, inputDelay);
		}else{
			this.remainigInputTime.put(critEdge.getName(), new HashMap<String,Integer>());
			this.remainigInputTime.get(critEdge.getName()).put(sinkPinAtomId, inputDelay);
		}
	}
	public HashMap<String,HashMap<String,Integer>> getRemainingOutputTime(){
		return this.remainigOutputTime;
	}
	public HashMap<String,HashMap<String,Integer>> getRemainingInputTime(){
		return this.remainigInputTime;
	}
	public HashSet<String> getCutNetNames(){
		return this.cutNetNames;
	}
}
