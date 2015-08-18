package placers.SAPlacer;

import java.util.Random;

import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import circuit.Block;
import circuit.BlockType;
import circuit.PackedCircuit;

public class WLD_GreedyRefiner
{

	private EfficientCostCalculator calculator;
	protected PackedCircuit circuit;
	protected HeterogeneousArchitecture architecture;
	protected Random rand;
	
	public WLD_GreedyRefiner(HeterogeneousArchitecture architecture, PackedCircuit circuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.calculator = new EfficientBoundingBoxNetCC(circuit);
		circuit.fillVector();
	}
	
	public void refine()
	{
		calculator.recalculateFromScratch();
		rand = new Random(1);
		int nbMoves = 20 * (int) Math.round(Math.pow(circuit.numBlocks(),4.0/3.0));
		
		for(int i = 0; i < nbMoves; i++)
		{
			Swap swap = findSwap();
			double deltaCost = calculator.calculateDeltaCost(swap);
			if(deltaCost <= 0) //After swapping the solution quality did not become worse
			{
				swap.apply();
				calculator.pushThrough();
			}
			else
			{
				calculator.revert();
			}
		}
	}
	
	private Swap findSwap()
	{
		int Rlim = 3;
		Swap swap = new Swap();
		Block b;
		do
		{
			b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		}while(b.type == BlockType.INPUT || b.type == BlockType.OUTPUT);
		
		swap.pl1 = b.getSite();
		if(b.type==BlockType.CLB)
		{
			swap.pl2 = architecture.randomClbSite(Rlim, swap.pl1);
		}
		else if(b.type == BlockType.HARDBLOCK_CLOCKED || b.type == BlockType.HARDBLOCK_UNCLOCKED)
		{
			swap.pl2 = architecture.randomHardBlockSite(Rlim, (HardBlockSite)swap.pl1);
		}

		return swap;
	}
	
}
