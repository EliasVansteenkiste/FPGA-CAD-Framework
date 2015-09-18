package placers.SAPlacer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import placers.Placer;

import architecture.Architecture;
import architecture.HardBlockSite;
import circuit.Block;
import circuit.BlockType;
import circuit.Clb;
import circuit.HardBlock;
import circuit.Input;
import circuit.Output;
import circuit.PackedCircuit;

public abstract class SAPlacer extends Placer
{
	
	static {
		defaultOptions.put("inner_num", "1");
	}
	
	private double Rlimd;
	private int Rlim, maxRlim;
	protected double T;
	protected boolean greedy;
	
	protected EfficientCostCalculator calculator;
	protected Random rand;
	
	public SAPlacer(Architecture architecture, PackedCircuit circuit, Map<String, String> options)
	{
		super(architecture, circuit, options);
		this.calculator = new EfficientBoundingBoxNetCC(circuit);
		circuit.fillVector();
	}
	
	protected Swap findSwap(int Rlim) {
		Swap swap=new Swap();
		Block b;
		
		do{
			b = circuit.vBlocks.elementAt(rand.nextInt(circuit.vBlocks.size()));
		}while(b.fixed);
		swap.pl1 = b.getSite();
		if(b.type==BlockType.CLB)
		{
			swap.pl2 = architecture.randomClbSite(Rlim, swap.pl1);
		}
		else if(b.type == BlockType.HARDBLOCK_CLOCKED || b.type == BlockType.HARDBLOCK_UNCLOCKED)
		{
			swap.pl2 = architecture.randomHardBlockSite(Rlim, (HardBlockSite)swap.pl1);
		}
		else if(b.type == BlockType.INPUT || b.type == BlockType.OUTPUT)
		{
			swap.pl2 = architecture.randomIOSite(Rlim, swap.pl1);
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
	
	protected void updateTemperature(double alpha) {
		double gamma;
		
		if (alpha > 0.96)     	gamma=0.5;
		else if (alpha > 0.8)	gamma=0.9;
		else if (alpha > 0.15)	gamma=0.95;
		else 					gamma=0.8;
		
		this.T *= gamma;
	}
	
	
	
	protected int getRlim() {
		return this.Rlim;
	}
	protected double getRlimd() {
		return this.Rlimd;
	}
	
	protected void setMaxRlim(int maxRlim) {
		this.maxRlim = maxRlim;
	}
	protected void setRlimd(double Rlimd) {
		this.Rlimd = Rlimd;
		this.updateIntRlim();
	}
	
	protected void updateRlim(double alpha) {
		this.updateRlim(alpha, this.maxRlim);
	}
	
	protected void updateRlim(double alpha, int maxValue) {
		this.Rlimd *= (1 - 0.44 + alpha);
		
		if(this.Rlimd > maxValue) {
			this.Rlimd = maxValue;
		}
		
		if(this.Rlimd < 1) {
			this.Rlimd = 1;
		}
		
		this.updateIntRlim();
	}
	
	private void updateIntRlim() {
		this.Rlim = (int) Math.round(this.Rlimd);
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
			int curX = curClb.getSite().getX();
			int curY = curClb.getSite().getY();
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