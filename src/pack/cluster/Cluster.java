package pack.cluster;

import java.util.ArrayList;

import pack.architecture.Architecture;
import pack.main.Simulation;
import pack.netlist.Netlist;
import pack.partition.Partition;

public class Cluster {
	private Netlist root;
	private Partition partition;
	private Architecture architecture;
	private Simulation simulation;
	
	private ArrayList<LogicBlock> logicBlocks;
	
	public Cluster(Netlist netlist, Architecture architecture, Partition partition, Simulation simulation){
		this.root = netlist;
		this.partition = partition;
		this.architecture = architecture;
		this.simulation = simulation;
	}
	public void packing(){
		this.logicBlocks = new ArrayList<LogicBlock>();
		
		TPack tpack = new TPack(this.root, this.partition, this.architecture, this.simulation);
		tpack.seedBasedPacking();
		this.logicBlocks.addAll(tpack.getLogicBlocks());
	}
	public void writeNetlistFile(){
		NetFileWriter writer = new NetFileWriter(this.logicBlocks, this.root);
			
		writer.netlistInputs();
		writer.netlistOutputs();
			
		writer.makeNetFile();
		writer.printHeaderToNetFile();
			
		writer.printLogicBlocksToNetFile();
		
		writer.finishNetFile();
	}
}