package placers;

import java.util.HashMap;
import java.util.Map;

import circuit.Block;
import circuit.PackedCircuit;
import circuit.Clb;
import circuit.Input;
import circuit.Output;
import circuit.Net;;

public class BoundingBoxNetCC implements CostCalculator {
	Map<Net, BoundingBoxData> netData;
	Map<Block, BlockData> blockData;
	PackedCircuit circuit;
	

	public BoundingBoxNetCC(PackedCircuit c) {
		this.circuit=c;
		this.initializeData();
	}
	
	public PackedCircuit getCircuit(){
		return circuit;
	}

	private double calculateNetCost(Block block) {
		double result=0;
		if (block!=null) {
			//System.out.println(blockData.get(block).blocks.size());
			for (Block b:blockData.get(block).blocks) {
				BoundingBoxData nd;
				if((nd = netData.get(b))!=null){
					result+=nd.calculateCost();
				}
			}
		}
		return result;
	}

	public double calculateTotalCost() {
		double totaleKost=0;
		for(BoundingBoxData bbd: netData.values()) {
			totaleKost+=bbd.calculateCost();
		}
		return totaleKost;
	}

	public double averageNetCost() {
		return calculateTotalCost()/netData.size();
	}

	private void initializeData() {
		netData = new HashMap<Net, BoundingBoxData>();
		for(Net net:circuit.nets.values()) {
			netData.put(net, new BoundingBoxData(net.blocks()));
		}
		
		blockData = new HashMap<Block, BlockData>();
		for(Clb block:circuit.clbs.values()) {
			blockData.put(block, new BlockData(block));
		}
		for(Input block:circuit.inputs.values()) {
			blockData.put(block, new BlockData(block));
		}
		for(Output block:circuit.outputs.values()) {
			blockData.put(block, new BlockData(block));
		}
	}


	public double calculateDeltaCost(Swap swap) {
		double costBefore=calculateNetCost(swap.pl1.block)+calculateNetCost(swap.pl2.block);
		
		if (swap.pl1.block!=null) {
			swap.pl1.block.site=swap.pl2;
		}
		if (swap.pl2.block!=null) {
			swap.pl2.block.site=swap.pl1;
		}
	
		
		double costAfter=calculateNetCost(swap.pl1.block)+calculateNetCost(swap.pl2.block);
		double deltaCost = costAfter-costBefore;
		
		if (swap.pl1.block!=null){
			swap.pl1.block.site=swap.pl1;
		}
		if (swap.pl2.block!=null){
			swap.pl2.block.site=swap.pl2;
		}
		
		return deltaCost;
	}
	
	public void apply(Swap swap) {
		if (swap.pl1.block!=null) swap.pl1.block.site=swap.pl2;
		if (swap.pl2.block!=null) swap.pl2.block.site=swap.pl1;
		Block temp1=swap.pl1.block;
		swap.pl1.block=swap.pl2.block;
		swap.pl2.block=temp1;
	}

}
