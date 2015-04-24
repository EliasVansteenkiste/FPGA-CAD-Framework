package circuit;

import java.util.HashMap;
import java.util.Map;

/*
 * Represents a circuit that has not yet been packed and thus cannot be placed yet
 * This circuit can contain inputs, outputs, LUTs, FFs and nets
 * This class can be used to represent a circuit before packing
 */
public class PrePackedCircuit extends Circuit
{
	
	private Map<String,Lut> luts;
	private Map<String,Flipflop> flipflops;
	private int nbLutInputs;
	
	public PrePackedCircuit(int nbLutInputs)
	{
		super();
		this.nbLutInputs = nbLutInputs;
		this.luts = new HashMap<String,Lut>();
		this.flipflops = new HashMap<String,Flipflop>();
	}
	
	public PrePackedCircuit(int nbLutInputs, String name)
	{
		super(name);
		this.nbLutInputs = nbLutInputs;
		this.luts = new HashMap<String,Lut>();
		this.flipflops = new HashMap<String,Flipflop>();
	}
	
	public PrePackedCircuit(Map<String,Output> outputs, Map<String,Input> inputs, int nbLutInputs)
	{
		super(outputs, inputs);
		this.nbLutInputs = nbLutInputs;
		this.luts = new HashMap<String,Lut>();
		this.flipflops = new HashMap<String,Flipflop>();
	}
	
	public void addLut(Lut lut)
	{
		if(lut.getInputs().length == this.nbLutInputs)
		{
			this.luts.put(lut.name, lut);
		}
		else
		{
			System.err.println("Wrong lut size!");
		}
	}
	
	public void addFlipflop(Flipflop flipflop)
	{
		this.flipflops.put(flipflop.name, flipflop);
	}

	public Map<String, Lut> getLuts() {
		return luts;
	}

	public Map<String, Flipflop> getFlipflops() {
		return flipflops;
	}
	
	public int getNbLutInputs()
	{
		return this.nbLutInputs;
	}
	
}
