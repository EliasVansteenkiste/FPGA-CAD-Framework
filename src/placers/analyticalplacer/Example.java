package placers.analyticalplacer;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import packers.BlePacker;
import circuit.Ble;
import circuit.Flipflop;
import circuit.Input;
import circuit.Lut;
import circuit.Net;
import circuit.Output;
import circuit.PrePackedCircuit;
import circuit.BlePackedCircuit;
import circuit.parser.blif.BlifReader;

public class Example 
{
	
	public static void main(String[] args)
	{
		BlifReader blifReader = new BlifReader();
		PrePackedCircuit prePackedCircuit;
		try
		{
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/i1.blif", 6);
			prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ecc.blif", 6);
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read blif file!");
			return;
		}
		
		printUnpackedCircuit(prePackedCircuit);
		
		BlePacker packer = new BlePacker(prePackedCircuit);
		BlePackedCircuit blePackedCircuit = packer.pack();
		
		printBlePackedCircuit(blePackedCircuit);
	}
	
	private static void printUnpackedCircuit(PrePackedCircuit prePackedCircuit)
	{
		System.out.println();
		Collection<Input> inputs = prePackedCircuit.getInputs().values();
		System.out.println("Inputs: " + inputs.size());
		for(Input input:inputs)
		{
			System.out.println(input.toString());
		}
		System.out.println();
		Collection<Output> outputs = prePackedCircuit.getOutputs().values();
		System.out.println("Outputs: " + outputs.size());
		for(Output output:outputs)
		{
			System.out.println(output.toString());
		}
		System.out.println();
		Collection<Lut> luts = prePackedCircuit.getLuts().values();
		System.out.println("LUTs: " + luts.size());
		for(Lut lut:luts)
		{
			System.out.println(lut.toString());
		}
		System.out.println();
		Collection<Flipflop> flipflops = prePackedCircuit.getFlipflops().values();
		System.out.println("Flipflops: " + flipflops.size());
		for(Flipflop flipflop:flipflops)
		{
			System.out.println(flipflop.toString());
		}
		System.out.println();
		Iterator<Net> iterator = prePackedCircuit.getNets().values().iterator();
		System.out.println("Nets: " + prePackedCircuit.getNets().values().size());
		while(iterator.hasNext())
		{
			Net currentNet = iterator.next();
			System.out.print("Source: " + currentNet.source.name + " Sinks: ");
			int vectorSize = currentNet.sinks.size();
			for(int i = 0; i < vectorSize; i++)
			{
				if(i < vectorSize - 1)
				{
					System.out.print(currentNet.sinks.get(i).name + ", ");
				}
				else
				{
					System.out.print(currentNet.sinks.get(i).name);
				}
			}
			System.out.println();
		}
	}
	
	private static void printBlePackedCircuit(BlePackedCircuit blePackedCircuit)
	{
		System.out.println();
		System.out.println();
		System.out.println("PACKED CIRCUIT:");
		Collection<Input> packedInputs = blePackedCircuit.getInputs().values();
		System.out.println("Inputs: " + packedInputs.size());
		for(Input input:packedInputs)
		{
			System.out.println(input.toString());
		}
		System.out.println();
		Collection<Output> packedOutputs = blePackedCircuit.getOutputs().values();
		System.out.println("Outputs: " + packedOutputs.size());
		for(Output output:packedOutputs)
		{
			System.out.println(output.toString());
		}
		System.out.println();
		Collection<Ble> packedBles = blePackedCircuit.getBles().values();
		System.out.println("BLEs: " + packedBles.size());
		int nbFlipflops = 0;
		int nbLUTs = 0;
		boolean allSixInputLuts = true;
		boolean allSixInputBles = true;
		for(Ble ble:packedBles)
		{
			if(ble.getNbInputs() != 6)
			{
				allSixInputBles = false;
			}
			System.out.print("LUT: ");
			if(ble.getLut() != null)
			{
				System.out.print(ble.getLut().name);
				nbLUTs++;
				if(ble.getLut().getNbInputs() != 6)
				{
					allSixInputLuts = false;
				}
			}
			else
			{
				System.out.print("none");
			}
			System.out.print(", FF: ");
			if(ble.getFlipflop() != null)
			{
				System.out.print(ble.getFlipflop().name);
				nbFlipflops++;
			}
			else
			{
				System.out.print("none");
			}
			System.out.println();
		}
		System.out.println("Nb of LUTs: " + nbLUTs);
		System.out.println("Nb of FFs: " + nbFlipflops);
		if(allSixInputLuts)
		{
			System.out.println("All LUTs have 6 inputs");
		}
		else
		{
			System.out.println("Not all LUTs have 6 inputs");
		}
		if(allSixInputBles)
		{
			System.out.println("All BLEs have 6 inputs");
		}
		else
		{
			System.out.println("Not all BLEs have 6 inputs");
		}
		System.out.println();
		Iterator<Net> packedNetsIterator = blePackedCircuit.getNets().values().iterator();
		System.out.println("Nets: " + blePackedCircuit.getNets().values().size());
		while(packedNetsIterator.hasNext())
		{
			Net currentNet = packedNetsIterator.next();
			System.out.print("Source: " + currentNet.source.name + " Sinks: ");
			int vectorSize = currentNet.sinks.size();
			for(int i = 0; i < vectorSize; i++)
			{
				if(i < vectorSize - 1)
				{
					System.out.print(currentNet.sinks.get(i).name + ", ");
				}
				else
				{
					System.out.print(currentNet.sinks.get(i).name);
				}
			}
			System.out.println();
		}
		System.out.println();
	}
	
}
