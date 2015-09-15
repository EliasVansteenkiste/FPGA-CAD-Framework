package placers.SAPlacer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import flexible_architecture.Circuit;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.site.AbstractSite;

public abstract class SAPlacer extends Placer
{
	
	static {
		defaultOptions.put("inner_num", "1");
	}
	
	private double Rlimd;
	private int Rlim;
	protected double T;
	protected boolean greedy;
	
	protected EfficientCostCalculator calculator;
	protected Random random;
	
	public SAPlacer(FlexibleArchitecture architecture, Circuit circuit, HashMap<String, String> options)
	{
		super(architecture, circuit, options);
		this.calculator = new EfficientBoundingBoxNetCC(circuit);
	}
	
	protected Swap findSwap(int Rlim) {
		GlobalBlock fromBlock;
		do {
			fromBlock = this.circuit.getRandomBlock(this.random);
		} while(fromBlock.isFixed());
		
		GlobalBlock toBlock = this.circuit.getRandomSite(fromBlock, Rlim, this.random).getRandomBlock(this.random);
		
		return new Swap(fromBlock, toBlock);
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
	
	protected void setRlimd(double Rlimd) {
		this.Rlimd = Rlimd;
		this.updateIntRlim();
	}
	
	protected void updateRlim(double alpha) {
		int maxFPGADimension = Math.max(this.circuit.getHeight(), this.circuit.getWidth());
		this.updateRlim(alpha, maxFPGADimension);
	}
	
	protected void updateRlim(double alpha, int maxValue) {
		this.Rlimd *= (1 - 0.44 + alpha);
		
		if(this.Rlimd > maxValue) {
			this. Rlimd = maxValue;
		}
		
		if(this.Rlimd < 1) {
			this.Rlimd = 1;
		}
		
		this.updateIntRlim();
	}
	
	private void updateIntRlim() {
		this.Rlim = (int) Math.round(this.Rlimd);
	}
}