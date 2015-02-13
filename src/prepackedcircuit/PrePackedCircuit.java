package prepackedcircuit;

import java.util.HashMap;
import java.util.Map;

import circuit.Input;
import circuit.Net;
import circuit.Output;

/*
 * Represents a circuit that has not yet been completely packed and thus cannot be placed yet
 * This circuit can contain inputs, outputs, LUTs, FFs, BLEs and nets
 * This class can be used to represent a circuit during different phases of technology mapping and packing
 */
public class PrePackedCircuit 
{
	
	private String name;
	private Map<String,Output> outputs;
	private Map<String,Input> inputs;
	private Map<String,Lut> luts;
	private Map<String,Flipflop> flipflops;
	private Map<String,Ble> bles;
	private Map<String,Net>	nets;
	
	public PrePackedCircuit()
	{
		this.outputs = new HashMap<String,Output>();
		this.inputs = new HashMap<String,Input>();
		this.luts = new HashMap<String,Lut>();
		this.flipflops = new HashMap<String,Flipflop>();
		this.bles = new HashMap<String,Ble>();
		this.nets = new HashMap<String,Net>();
	}
	
	public PrePackedCircuit(Map<String,Output> outputs, Map<String,Input> inputs)
	{
		this.outputs = outputs;
		this.inputs = inputs;
		this.luts = new HashMap<String,Lut>();
		this.flipflops = new HashMap<String,Flipflop>();
		this.bles = new HashMap<String,Ble>();
		this.nets = new HashMap<String,Net>();
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
	
	public void addBle(Ble ble)
	{
		this.bles.put(ble.name, ble);
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
	
	public Map<String, Ble> getBles()
	{
		return bles;
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
