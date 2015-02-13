package placers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import architecture.FourLutSanitized;
import packers.BlePacker;
import placement.parser.Placement;
import placement.parser.Readplaatsing;
import prepackedcircuit.Ble;
import prepackedcircuit.Flipflop;
import prepackedcircuit.Lut;
import prepackedcircuit.PrePackedCircuit;
import prepackedcircuit.parser.blif.BlifReader;

import circuit.Circuit;
import circuit.Input;
import circuit.Net;
import circuit.Output;
import circuit.parser.netlist.ParseException;
import circuit.parser.netlist.Readnetlist;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Elias.
 *         Created 31-jul.-2012.
 */
public class ExampleComparisonVPR {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws ParseException 
	 * @throws placement.parser.ParseException 
	 * @throws architecture.ParseException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws FileNotFoundException, placement.parser.ParseException, ParseException {
		String netFile = "benchmarks/cc.net";
		String placementVPRFile = "benchmarks/cc.vpr.p";
//		String placementOutFile = "cc.p";
		int height = 12;
		int width = 12;
		int trackwidth = 4;
		Double placementEffort = 10.;
		
		System.out.println("Read in netlist.");
		Readnetlist parser=new Readnetlist(new FileInputStream(new File(netFile)));
		Circuit c=parser.read(true);

		System.out.println("Read in VPR placement.");		
		Readplaatsing plaats_parser=new Readplaatsing(new FileInputStream(new File(placementVPRFile)));
		Placement p=plaats_parser.read();

		System.out.println("Constructing architecture.");
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		//Apply VPR placement
		c.place(p, a);
		
		// The below is new
		//****************************************************************************************************************************
		
		BlifReader blifReader = new BlifReader();
		PrePackedCircuit prePackedCircuit;
		try
		{
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/2/i1.blif");
			prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ecc.blif");
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read blif file!");
			return;
		}
		
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
		
		System.out.println();
		System.out.println();
		System.out.println("PACKED CIRCUIT:");
		BlePacker packer = new BlePacker(prePackedCircuit);
		PrePackedCircuit packedCircuit = packer.pack();
		Collection<Input> packedInputs = packedCircuit.getInputs().values();
		System.out.println("Inputs: " + packedInputs.size());
		for(Input input:packedInputs)
		{
			System.out.println(input.toString());
		}
		System.out.println();
		Collection<Output> packedOutputs = packedCircuit.getOutputs().values();
		System.out.println("Outputs: " + packedOutputs.size());
		for(Output output:packedOutputs)
		{
			System.out.println(output.toString());
		}
		System.out.println();
		Collection<Ble> packedBles = packedCircuit.getBles().values();
		System.out.println("BLEs: " + packedBles.size());
		int nbFlipflops = 0;
		int nbLUTs = 0;
		for(Ble ble:packedBles)
		{
			System.out.print("LUT: ");
			if(ble.getLut() != null)
			{
				System.out.print(ble.getLut().name);
				nbLUTs++;
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
		System.out.println();
		
		//****************************************************************************************************************************
		// The above is new
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		System.out.println("Total Cost VPR placement: "+bbncc.calculateTotalCost());
		pm.PlacementCLBsConsistencyCheck();
		
		//Random placement
		Rplace.placeCLBsandIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total Cost random placement: "+bbncc.calculateTotalCost());
		

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
		System.out.println("Total Cost javaVPR placement: "+bbncc.calculateTotalCost());

		
	}

}
