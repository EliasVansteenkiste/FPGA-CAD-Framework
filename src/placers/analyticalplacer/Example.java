package placers.analyticalplacer;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;

import mathtools.CGSolver;
import mathtools.Crs;

import architecture.FourLutSanitized;

import packers.BlePacker;
import packers.ClbPacker;
import placers.BoundingBoxNetCC;
import placers.PlacementManipulatorIOCLB;
import placers.Rplace;
import placers.Vplace;
import timinganalysis.TimingGraph;
import visual.ArchitecturePanel;
import circuit.Ble;
import circuit.Clb;
import circuit.Flipflop;
import circuit.Input;
import circuit.Lut;
import circuit.Net;
import circuit.Output;
import circuit.PackedCircuit;
import circuit.Pin;
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
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/C17.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ex5p.blif", 6);
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
		//System.out.println();
		//System.out.println("SA placed block locations");
		//printPlacedCircuit(packedCircuit);
		
		//System.out.println("\nANALYTICAL PLACEMENT:");
		//analyticalPlace(packedCircuit, prePackedCircuit);
		//printPlacedCircuit(packedCircuit);
		
		//System.out.println("\nANALYTICAL PLACEMENT TWO");
		//analyticalPlaceTwo(packedCircuit, prePackedCircuit);
		
		//System.out.println("\nANALYTICAL PLACEMENT THREE");
		//analyticalPlaceThree(packedCircuit, prePackedCircuit);
		
		//System.out.println("\nANALYTICAL PLACEMENT FOUR");
		//analyticalPlaceFour(packedCircuit, prePackedCircuit, false);
		
		visualAnalytical(packedCircuit);
		//visualSA(packedCircuit);
	}
	
//	public static void main(String[] args)
//	{
//		PackedCircuit circuit = constructTestCircuit();
//		//printPackedCircuit(circuit);
//		
//		visualAnalytical(circuit);
//		//printPlacedCircuit(circuit);
//	}
	
//	public static void main(String[] args)
//	{
//		crsBugReconstruct();
//	}
	
	private static void visualAnalytical(PackedCircuit c)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		AnalyticalPlacerFour placer = new AnalyticalPlacerFour(a, c, bbncc);
		placer.place();
		
		System.out.println("Total cost analytical placement: " + bbncc.calculateTotalCost());
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		Vplace saPlacer= new Vplace(pm,bbncc);
		saPlacer.lowTempAnneal(300, 5, 2000);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost after low temperature anneal: " + bbncc.calculateTotalCost());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(950,950);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void visualSA(PackedCircuit c)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		Double placementEffort = 10.0;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		Vplace placer= new Vplace(pm,bbncc);
		placer.place(placementEffort);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost SA placement: " + bbncc.calculateTotalCost());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(950,950);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void analyticalPlace(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		AnalyticalPlacer placer = new AnalyticalPlacer(a, c, bbncc);
		placer.place();
		
		System.out.println("Total cost analytical placement: " + bbncc.calculateTotalCost());
		
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,new Random(1));
		pm.PlacementCLBsConsistencyCheck();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after analytical placement: " + maxDelayUpdated);
	}
	
	private static void analyticalPlaceTwo(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		AnalyticalPlacerTwo placer = new AnalyticalPlacerTwo(a, c, bbncc);
		placer.place();
		
		System.out.println("Total cost analytical placement: " + bbncc.calculateTotalCost());
		
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,new Random(1));
		pm.PlacementCLBsConsistencyCheck();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after analytical placement: " + maxDelayUpdated);
	}
	
	private static void analyticalPlaceThree(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		AnalyticalPlacerThree placer = new AnalyticalPlacerThree(a, c, bbncc);
		placer.place();
		
		System.out.println("Total cost analytical placement: " + bbncc.calculateTotalCost());
		
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,new Random(1));
		pm.PlacementCLBsConsistencyCheck();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after analytical placement: " + maxDelayUpdated);
	}
	
	private static void analyticalPlaceFour(PackedCircuit c, PrePackedCircuit prePackedCircuit, boolean writeCSV)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		AnalyticalPlacerFour placer = new AnalyticalPlacerFour(a, c, bbncc);
		if(writeCSV)
		{
			placer.place("convergence.csv");
		}
		else
		{
			placer.place();
		}
		
		System.out.println("Total cost analytical placement: " + bbncc.calculateTotalCost());
		
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,new Random(1));
		pm.PlacementCLBsConsistencyCheck();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after analytical placement: " + maxDelayUpdated);
	}
	
	private static void simulatedAnnealingPlace(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		Double placementEffort = 10.;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total Cost random placement: " + bbncc.calculateTotalCost());
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay random placement: " + maxDelay);
		
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
		System.out.println("Total cost SA placement: " + bbncc.calculateTotalCost());
		pm.PlacementCLBsConsistencyCheck();
		
		timingGraph.updateDelays();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after SA placement: " + maxDelayUpdated);
	}
	
	private static PackedCircuit constructTestCircuit()
	{
		PackedCircuit circuit = new PackedCircuit();
		Input input3 = new Input("input_3");
		circuit.inputs.put(input3.name, input3);
		Output output7 = new Output("output_7");
		circuit.outputs.put(output7.name, output7);
		Clb clba = new Clb("clb_a", 1, 6);
		circuit.clbs.put(clba.name, clba);
		Clb clbb = new Clb("clb_b", 1, 6);
		circuit.clbs.put(clbb.name, clbb);
		Clb clbc = new Clb("clb_c", 1, 6);
		circuit.clbs.put(clbc.name, clbc);
		Clb clbd = new Clb("clb_d", 1, 6);
		circuit.clbs.put(clbd.name, clbd);
		
		Net net1 = new Net(input3.name);
		net1.source = input3.output;
		net1.sinks = new Vector<Pin>();
		net1.sinks.add(clba.input[0]);
		net1.sinks.add(clbc.input[0]);
		circuit.getNets().put(net1.name, net1);
		
		Net net2 = new Net(clbc.name);
		net2.source = clbc.output[0];
		net2.sinks = new Vector<Pin>();
		net2.sinks.add(output7.input);
		circuit.getNets().put(net2.name, net2);
		
		Net net3 = new Net(clba.name);
		net3.source = clba.output[0];
		net3.sinks = new Vector<Pin>();
		net3.sinks.add(clbb.input[0]);
		net3.sinks.add(clbc.input[1]);
		circuit.getNets().put(net3.name, net3);
		
		Net net4 = new Net(clbb.name);
		net4.source = clbb.output[0];
		net4.sinks = new Vector<Pin>();
		net4.sinks.add(clbc.input[2]);
		net4.sinks.add(clbd.input[0]);
		circuit.getNets().put(net4.name, net4);
		
		Net net5 = new Net(clbd.name);
		net5.source = clbd.output[0];
		net5.sinks = new Vector<Pin>();
		net5.sinks.add(clba.input[1]);
		net5.sinks.add(clbb.input[1]);
		circuit.getNets().put(net5.name, net5);
		
		return circuit;
	}
	
	private static void printPlacedCircuit(PackedCircuit packedCircuit)
	{
		System.out.println("INPUTS:");
		for(Input input:packedCircuit.inputs.values())
		{
			System.out.println(input.name + ": (" + input.getSite().x + "," + input.getSite().y + "," + input.getSite().n + ")");
		}
		System.out.println("\nOUTPUTS:");
		for(Output output:packedCircuit.outputs.values())
		{
			System.out.println(output.name + ": (" + output.getSite().x + "," + output.getSite().y + "," +  output.getSite().n + ")");
		}
		System.out.println("\nCLBs:");
		for(Clb clb:packedCircuit.clbs.values())
		{
			System.out.println(clb.name + ": (" + clb.getSite().x + "," + clb.getSite().y + ")");
		}
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
		crsBuilder.setElement(0, 5, 69.69);
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
		
		System.out.println();
		System.out.println("Print in matrix format:");
		for(int i = 0; i < 6; i++)
		{
			for(int j = 0; j < 6; j++)
			{
				System.out.print(crsBuilder.getElement(i, j) + "\t");
			}
			System.out.println();
		}
	}
	
	private static void crsBugReconstruct()
	{
		Crs crsBuilder = new Crs(2);
		crsBuilder.setElement(1, 1, 20.5);
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				System.out.print(crsBuilder.getElement(i, j) + "\t");
			}
			System.out.println();
		}
		System.out.println();
		crsBuilder.setElement(0, 0, 19.5);
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				System.out.print(crsBuilder.getElement(i, j) + "\t");
			}
			System.out.println();
		}
		System.out.println();
		crsBuilder.setElement(0, 1, 18.5);
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				System.out.print(crsBuilder.getElement(i, j) + "\t");
			}
			System.out.println();
		}
		System.out.println();
		
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
	
	private static void testCGSolver()
	{
		Crs crsBuilder = new Crs(10);
		crsBuilder.setElement(0, 0, 7.3);
		crsBuilder.setElement(0, 3, 2.1);
		crsBuilder.setElement(0, 7, 1.3);
		crsBuilder.setElement(1, 1, 9.2);
		crsBuilder.setElement(1, 2, 0.9);
		crsBuilder.setElement(1, 5, 1.1);
		crsBuilder.setElement(2, 1, 0.9);
		crsBuilder.setElement(2, 2, 8.7);
		crsBuilder.setElement(2, 6, 1.7);
		crsBuilder.setElement(3, 0, 2.1);
		crsBuilder.setElement(3, 3, 7.6);
		crsBuilder.setElement(3, 4, 0.7);
		crsBuilder.setElement(4, 3, 0.7);
		crsBuilder.setElement(4, 4, 8.1);
		crsBuilder.setElement(4, 8, 0.6);
		crsBuilder.setElement(5, 1, 1.1);
		crsBuilder.setElement(5, 5, 9.1);
		crsBuilder.setElement(5, 9, 0.5);
		crsBuilder.setElement(6, 2, 1.7);
		crsBuilder.setElement(6, 6, 8.9);
		crsBuilder.setElement(6, 9, 1.4);
		crsBuilder.setElement(7, 0, 1.3);
		crsBuilder.setElement(7, 7, 7.9);
		crsBuilder.setElement(7, 8, 0.9);
		crsBuilder.setElement(8, 4, 0.6);
		crsBuilder.setElement(8, 7, 0.9);
		crsBuilder.setElement(8, 8, 8.8);
		crsBuilder.setElement(9, 5, 0.5);
		crsBuilder.setElement(9, 6, 1.4);
		crsBuilder.setElement(9, 9, 8.9);
		
		double[] vector = {10.7, 12.7, 13.5, 14.3, 11.3, 10.9, 13.3, 14.4, 12.2, 11.1};
		double epselon = 0.000001;
		
		CGSolver solver = new CGSolver(crsBuilder, vector);
		double[] solution = solver.solve(epselon);
		
		System.out.println("Solution: ");
		for(int i = 0; i < solution.length; i++)
		{
			System.out.println(solution[i] + "   ");
		}
	}
	
}
