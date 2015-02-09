package placers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import architecture.FourLutSanitized;
import architecture.Site;
import circuit.Block;
import circuit.BlockType;
import circuit.Circuit;
import circuit.Clb;

public class PlacementManipulatorIOCLBParallel implements PlacementManipulator {

	protected FourLutSanitized a;
	Random rand;

	protected Circuit circuit;
	protected int maxFPGAdimension;


	public PlacementManipulatorIOCLBParallel(FourLutSanitized a, Circuit c) {
		this.a=a;
		this.circuit=c;
		maxFPGAdimension = Math.max(a.width,a.height);
		circuit.vBlocks = new Vector<Block>();
		circuit.vBlocks.addAll(circuit.clbs.values());
		circuit.vBlocks.addAll(circuit.inputs.values());
		circuit.vBlocks.addAll(circuit.outputs.values());
		rand = new Random();
		a.rand = rand;
	}
	
	public PlacementManipulatorIOCLBParallel(FourLutSanitized a, Circuit c, Random rand) {
		this.a=a;
		this.circuit=c;
		maxFPGAdimension = Math.max(a.width,a.height);
		circuit.vBlocks = new Vector<Block>();
		circuit.vBlocks.addAll(circuit.clbs.values());
		circuit.vBlocks.addAll(circuit.inputs.values());
		circuit.vBlocks.addAll(circuit.outputs.values());
		this.rand = rand;
		a.rand = rand;
	}

	public Swap findSwap(int Rlim) {
		Swap swap=new Swap();
		Block b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		swap.pl1 = b.site;
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
	
	public void swap(Swap swap) {
		
		if (swap.pl1.block!=null) swap.pl1.block.site=swap.pl2;
		if (swap.pl2.block!=null) swap.pl2.block.site=swap.pl1;
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
		for(Site s:a.siteMap.values()){
			if(s.block!=null&&s.block.type==BlockType.CLB){
				if(clbs.remove(s.block.name)==null){
					System.out.println("Placement consistency check failed! clb:"+s.block+", site:"+s);
					return;
				}
			}
		}
		System.out.println("Placement consistency check passed!");
	}


}