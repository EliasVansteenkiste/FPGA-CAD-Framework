package placers.analyticalplacer;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import mathtools.Crs;

import architecture.FourLutSanitized;

import packers.BlePacker;
import packers.ClbPacker;
import placers.BoundingBoxNetCC;
import placers.PlacementManipulatorIOCLB;
import placers.Rplace;
import placers.Vplace;
import timinganalysis.TimingGraph;
import circuit.Ble;
import circuit.Clb;
import circuit.Flipflop;
import circuit.Input;
import circuit.Lut;
import circuit.Net;
import circuit.Output;
import circuit.PackedCircuit;
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
		
		//printUnpackedCircuit(prePackedCircuit);
		
		BlePacker blePacker = new BlePacker(prePackedCircuit);
		BlePackedCircuit blePackedCircuit = blePacker.pack();
		
		//printBlePackedCircuit(blePackedCircuit);
		
		ClbPacker clbPacker = new ClbPacker(blePackedCircuit);
		PackedCircuit packedCircuit = clbPacker.pack();
		
		//printPackedCircuit(packedCircuit);
		
		//System.out.println("SIMULATED ANNEALING PLACEMENT:");
		//simulatedAnnealingPlace(packedCircuit, prePackedCircuit);
		
		testCrs();
		
	}
	
	private static void simulatedAnnealingPlace(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int height = 16;
		int width = 16;
		int trackwidth = 4;
		Double placementEffort = 10.;
		
		System.out.println("Constructing architecture.");
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		//Random placement
		Rplace.placeCLBsandIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total Cost random placement: " + bbncc.calculateTotalCost());
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay before SA: " + maxDelay);
		
		Vplace placer= new Vplace(pm,bbncc);
		//Time placement process
		final long startTime = System.nanoTime();
		final long endTime;
		try {
			placer.place(placementEffort);
		} finally {
		  endTime = System.nanoTime();
		}
		final long duration = endTime - startTime;
		System.out.println("Runtime: "+(duration/1.0E9));
		System.out.println("Total Cost javaVPR placement: " + bbncc.calculateTotalCost());
		pm.PlacementCLBsConsistencyCheck();
		
		timingGraph.updateDelays();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after SA: " + maxDelayUpdated);
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
		System.out.println("BLE PACKED CIRCUIT:");
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
	
	private static void printPackedCircuit(PackedCircuit circuit)
	{
		System.out.println();
		System.out.println();
		System.out.println("PACKED CIRCUIT:");
		Collection<Input> inputs = circuit.getInputs().values();
		System.out.println("Inputs: " + inputs.size());
		for(Input input:inputs)
		{
			System.out.println(input.toString());
		}
		System.out.println();
		Collection<Output> outputs = circuit.getOutputs().values();
		System.out.println("Outputs: " + outputs.size());
		for(Output output:outputs)
		{
			System.out.println(output.toString());
		}
		System.out.println();
		Collection<Clb> clbs = circuit.clbs.values();
		System.out.println("CLBs: " + clbs.size());
		System.out.println();
		Iterator<Net> netsIterator = circuit.getNets().values().iterator();
		System.out.println("Nets: " + circuit.getNets().values().size());
		while(netsIterator.hasNext())
		{
			Net currentNet = netsIterator.next();
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
	
	private static void testCrs()
	{
		Crs crsBuilder = new Crs(6);
		crsBuilder.setElement(0, 0, 1.5);
		crsBuilder.setElement(1, 2, 3.3);
		crsBuilder.setElement(4, 4, 10.6);
		crsBuilder.setElement(3, 3, 4.9);
		crsBuilder.setElement(2, 5, 22.1);
		crsBuilder.setElement(5, 1, 30.7);
		crsBuilder.setElement(1, 0, 36.4);
		crsBuilder.setElement(1, 4, 39.4);
		crsBuilder.setElement(1, 4, 40.4);
		
		double[] val = crsBuilder.getVal();
		int[] col_ind = crsBuilder.getCol_ind();
		int[] row_ptr = crsBuilder.getRow_ptr();
		System.out.print("Values array: ");
		for(int i = 0; i < val.length; i++)
		{
			System.out.print(val[i] + " ");
		}
		System.out.println();
		System.out.print("Column index array: ");
		for(int i = 0; i < col_ind.length; i++)
		{
			System.out.print(col_ind[i] + " ");
		}
		System.out.println();
		System.out.print("Row pointer array: ");
		for(int i = 0; i < row_ptr.length; i++)
		{
			System.out.print(row_ptr[i] + " ");
		}
	}
	
}
