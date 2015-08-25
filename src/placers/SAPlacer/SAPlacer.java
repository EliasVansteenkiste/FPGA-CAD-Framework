package placers.SAPlacer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import architecture.Architecture;
import architecture.HeterogeneousArchitecture;
import architecture.HardBlockSite;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.HardBlock;
import circuit.Input;
import circuit.Output;
import circuit.PackedCircuit;

public abstract class SAPlacer
{
	
	protected double Rlimd;
	protected EfficientCostCalculator calculator;
	protected PackedCircuit circuit;
	protected Architecture architecture;
	protected Random rand;
	
	public SAPlacer(Architecture architecture, PackedCircuit circuit)
	{
		this.architecture = architecture;
		this.circuit = circuit;
		this.calculator = new EfficientBoundingBoxNetCC(circuit);
		circuit.fillVector();
	}
	
	public abstract void place(double inner_num);
	
	public abstract void lowTempAnneal(double innerNum);
	
	protected Swap findSwap(int Rlim)
	{
		Swap swap=new Swap();
		Block b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		swap.pl1 = b.getSite();
		if(b.type==BlockType.CLB)
		{
			swap.pl2 = architecture.randomClbSite(Rlim, swap.pl1);
		}
		else if(b.type == BlockType.HARDBLOCK_CLOCKED || b.type == BlockType.HARDBLOCK_UNCLOCKED)
		{
			swap.pl2 = architecture.randomHardBlockSite(Rlim, (HardBlockSite)swap.pl1);
		}
		else if(b.type == BlockType.INPUT)
		{
			swap.pl2 = architecture.randomISite(Rlim, swap.pl1);
		}
		else if(b.type == BlockType.OUTPUT)
		{
			swap.pl2 = architecture.randomOSite(Rlim, swap.pl1);
		}
		return swap;
	}
	
	protected Swap findSwapInCircuit()
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
		else if(b.type == BlockType.HARDBLOCK_CLOCKED || b.type == BlockType.HARDBLOCK_UNCLOCKED)
		{
			HardBlock hardBlock = (HardBlock)b;
			String typeName = hardBlock.getTypeName();
			Vector<HardBlock> hardBlocksOfType = null;
			for(Vector<HardBlock> hbVector: circuit.getHardBlocks())
			{
				if(hbVector.get(0).getTypeName().contains(typeName))
				{
					hardBlocksOfType = hbVector;
					break;
				}
			}
			HardBlock hbToSwap = hardBlocksOfType.get(rand.nextInt(hardBlocksOfType.size()));
			swap.pl2 = hbToSwap.getSite();
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
	
	protected double updateTemperature(double temperature, double alpha)
	{
		double gamma;
		if (alpha > 0.96)     	gamma=0.5;
		else if (alpha > 0.8)	gamma=0.9;
		else if (alpha > 0.15)	gamma=0.95;
		else 					gamma=0.8;
		return temperature*gamma;
	}
	
	protected int initialRlim()
	{
		int Rlim=(int)Math.round(Rlimd);
		return Rlim;
	}
	
	protected int updateRlim(double alpha)
	{
		Rlimd=Rlimd*(1-0.44+alpha);
		int maxFPGAdimension = Math.max(architecture.getHeight(), architecture.getWidth());
		if(Rlimd>maxFPGAdimension)
		{
			Rlimd=maxFPGAdimension;
		}
		if(Rlimd<1)
		{
			Rlimd=1;
		}
		return  (int) Math.round(Rlimd);
	}
	
	protected int updateRlimLimited(double alpha, int maxValue)
	{
		Rlimd = Rlimd * (1 - 0.44 + alpha);
		if(Rlimd > maxValue)
		{
			Rlimd = maxValue;
		}
		if(Rlimd < 1)
		{
			Rlimd = 1;
		}
		return (int) Math.round(Rlimd);
	}
	
	protected int getBiggestDistance()
	{
		Collection<Clb> clbs = circuit.clbs.values();
		Iterator<Clb> clbIterator = clbs.iterator();
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		while(clbIterator.hasNext())
		{
			Clb curClb = clbIterator.next();
			int curX = curClb.getSite().x;
			int curY = curClb.getSite().y;
			if(curX > maxX)
			{
				maxX = curX;
			}
			if(curX < minX)
			{
				minX = curX;
			}
			if(curY > maxY)
			{
				maxY = curY;
			}
			if(curY < minY)
			{
				minY = curY;
			}
		}
		int maxDistance;
		if(maxX - minX > maxY - minY)
		{
			maxDistance = maxX - minX;
		}
		else
		{
			maxDistance = maxY - minY;
		}
		return maxDistance;
	}
	
}