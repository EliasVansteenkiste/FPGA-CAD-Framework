package prepackedcircuit;

import java.util.HashMap;
import java.util.Map;

import circuit.Input;
import circuit.Net;
import circuit.Output;

public class PrePackedCircuit 
{
	
	private String name;
	private Map<String,Output> outputs;
	private Map<String,Input> inputs;
	private Map<String,Lut> luts;
	private Map<String,Flipflop> flipflops;
	private Map<String,Net>	nets;
	
	public PrePackedCircuit()
	{
		outputs = new HashMap<String,Output>();
		inputs = new HashMap<String,Input>();
		luts = new HashMap<String,Lut>();
		flipflops = new HashMap<String,Flipflop>();
		nets = new HashMap<String,Net>();
	}

	public void addOutput(Output output)
	{
		this.outputs.put(output.name, output);
	}
	
	public void addInput(Input input)
	{
		this.inputs.put(input.name, input);
	}
	
	public void addLut(Lut lut)
	{
		this.luts.put(lut.name, lut);
	}
	
	public void addFlipflop(Flipflop flipflop)
	{
		this.flipflops.put(flipflop.name, flipflop);
	}
	
	public void addNet(Net net)
	{
		this.nets.put(net.name, net);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Map<String, Output> getOutputs() {
		return outputs;
	}

	public Map<String, Input> getInputs() {
		return inputs;
	}

	public Map<String, Lut> getLuts() {
		return luts;
	}

	public Map<String, Flipflop> getFlipflops() {
		return flipflops;
	}

	public Map<String, Net> getNets() {
		return nets;
	}

	@Override
	public String toString()
	{
		return this.name;
	}
	
}
