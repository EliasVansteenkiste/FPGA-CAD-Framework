package placers.old;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import placers.SAPlacer.Swap;

import architecture.FourLutSanitized;
import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Input;
import circuit.Output;
import circuit.PackedCircuit;
import circuit.Clb;

public class PlacementManipulatorIOCLBParallel implements PlacementManipulator {

	protected FourLutSanitized a;
	Random rand;

	protected PackedCircuit circuit;
	protected int maxFPGAdimension;


	public PlacementManipulatorIOCLBParallel(FourLutSanitized a, PackedCircuit c) {
		this.a=a;
		this.circuit=c;
		maxFPGAdimension = Math.max(a.getWidth(),a.getHeight());
		circuit.vBlocks = new Vector<Block>();
		circuit.vBlocks.addAll(circuit.clbs.values());
		circuit.vBlocks.addAll(circuit.inputs.values());
		circuit.vBlocks.addAll(circuit.outputs.values());
		rand = new Random();
		a.setRand(rand);
	}
	
	public PlacementManipulatorIOCLBParallel(FourLutSanitized a, PackedCircuit c, Random rand) {
		this.a=a;
		this.circuit=c;
		maxFPGAdimension = Math.max(a.getWidth(),a.getHeight());
		circuit.vBlocks = new Vector<Block>();
		circuit.vBlocks.addAll(circuit.clbs.values());
		circuit.vBlocks.addAll(circuit.inputs.values());
		circuit.vBlocks.addAll(circuit.outputs.values());
		this.rand = rand;
		a.setRand(rand);
	}

	public Swap findSwap(int Rlim) {
		Swap swap=new Swap();
		Block b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		swap.pl1 = b.getSite();
		if(b.type==BlockType.CLB){
			swap.pl2 = a.randomSite(Rlim, swap.pl1);
		}else if(b.type == BlockType.INPUT){
			swap.pl2 = a.randomISite(Rlim, swap.pl1);
		}else if(b.type == BlockType.OUTPUT){
			swap.pl2 = a.randomOSite(Rlim, swap.pl1);
		}
		//else{
		//	System.out.println("Something went wrong.");
		//}
		
		return swap;
	}
	
	public Swap findSwapInCircuit()
	{
		Swap swap = new Swap();
		Block b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		swap.pl1 = b.getSite();
		if(b.type == BlockType.CLB)
		{
			int nbClbs = circuit.clbs.values().size();
			Clb[] clbs = new Clb[nbClbs];
			circuit.clbs.values().toArray(clbs);
			Clb clbToSwap = clbs[rand.nextInt(nbClbs)];
			swap.pl2 = clbToSwap.getSite();
		}
		else if(b.type == BlockType.INPUT)
		{
			int nbInputs = circuit.getInputs().values().size();
			Input[] inputs = new Input[nbInputs];
			circuit.getInputs().values().toArray(inputs);
			Input inputToSwap = inputs[rand.nextInt(nbInputs)];
			swap.pl2 = inputToSwap.getSite();
		}
		else if(b.type == BlockType.OUTPUT)
		{
			int nbOutputs = circuit.getOutputs().values().size();
			Output[] outputs = new Output[nbOutputs];
			circuit.getOutputs().values().toArray(outputs);
			Output outputToSwap = outputs[rand.nextInt(nbOutputs)];
			swap.pl2 = outputToSwap.getSite();
		}
		//System.out.println("From (" + swap.pl1.x + "," + swap.pl1.y + ") to (" + swap.pl2.x + "," + swap.pl2.y + ")");
		return swap;
	}
	
	public void swap(Swap swap) {
		
		if (swap.pl1.block!=null) swap.pl1.block.setSite(swap.pl2);
		if (swap.pl2.block!=null) swap.pl2.block.setSite(swap.pl1);
		Block temp1=swap.pl1.block;
		swap.pl1.block=swap.pl2.block;
		swap.pl2.block=temp1;
	}
	

	public int maxFPGAdimension() {
		return maxFPGAdimension;
	}

	public double numBlocks() {
		return circuit.numBlocks();
	}
	
	public void PlacementCLBsConsistencyCheck(){
		Map<String,Clb> clbs = new HashMap<String,Clb>(circuit.clbs);
		for(Site s:a.getSites()){
			if(s.block!=null&&s.block.type==BlockType.CLB){
				if(clbs.remove(s.block.name)==null){
					System.out.println("Placement consistency check failed! clb:"+s.block+", site:"+s);
					return;
				}
			}
		}
		System.out.println("Placement consistency check passed!");
	}

	public PackedCircuit getCircuit()
	{
		return circuit;
	}

}