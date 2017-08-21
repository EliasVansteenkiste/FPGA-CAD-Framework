package pack.cluster;

import java.util.ArrayList;

import pack.architecture.Architecture;
import pack.main.Simulation;
import pack.netlist.Netlist;
import pack.partition.Partition;
import pack.util.Info;
import pack.util.Util;

public class Cluster {
	private Netlist root;
	private Partition partition;
	private Architecture architecture;
	private Simulation simulation;

	private ArrayList<Netlist> leafNodes;
	private ArrayList<LogicBlock> logicBlocks;
	
	public Cluster(Netlist netlist, Architecture architecture, Partition partition, Simulation simulation){
		this.root = netlist;
		this.partition = partition;
		this.architecture = architecture;
		this.simulation = simulation;
	}
	public void packing(){
		this.logicBlocks = new ArrayList<LogicBlock>();
		this.leafNodes = new ArrayList<Netlist>();
		
		TPack tpack = new TPack(this.root, this.partition, this.architecture, this.simulation);
		tpack.seedBasedPacking();
		
		this.logicBlocks.addAll(tpack.getLogicBlocks());
		this.leafNodes.addAll(this.root.get_leaf_nodes());
	}
	public void writeHierarchyFile(){
		int logicBlockCounter = 0;
		for(LogicBlock lb:this.logicBlocks){
			if(lb.isFloating()){
				logicBlockCounter++;
			}
		}
		Info.add("hierarchy", "Leaf Node: floating blocks (" + logicBlockCounter + " lb) " + this.randomColor());
		for(LogicBlock lb:this.logicBlocks){
			if(lb.isFloating()){
				Info.add("hierarchy", "\t" + lb.getInfo());
			}
		}
		for(Netlist leafNode:this.leafNodes){
			Info.add("hierarchy", "Leaf Node: " + leafNode.getHierarchyIdentifier() + " (" + leafNode.getLogicBlocks().size() + " lb) " + this.randomColor());
			for(LogicBlock lb:leafNode.getLogicBlocks()){
				Info.add("hierarchy", "\t" + lb.getInfo());
			}
		}
	}
	private String randomColor(){
		return "[Color: (" + Util.str((int)(Math.random()*255)) + "," + Util.str((int)(Math.random()*255)) + "," + Util.str((int)(Math.random()*255)) + ")]";
	}
	public void writeNetlistFile(){
		NetFileWriter writer = new NetFileWriter(this.logicBlocks, this.root);
		String result_folder = this.simulation.getStringValue("result_folder");
			
		writer.netlistInputs();
		writer.netlistOutputs();
			
		writer.makeNetFile(result_folder);
		
		writer.printHeaderToNetFile(result_folder);
		writer.printLogicBlocksToNetFile();
		writer.finishNetFile();
	}
}