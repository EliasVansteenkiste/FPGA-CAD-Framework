package placers.analyticalplacer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;

import mathtools.CGSolver;
import mathtools.Crs;

import architecture.FourLutSanitized;
import architecture.Site;

import packers.BlePacker;
import packers.ClbPacker;
import placers.BoundingBoxNetCC;
import placers.PlacementManipulatorIOCLB;
import placers.Rplace;
import placers.Vplace;
import placers.SAPlacer.EfficientBoundingBoxData;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import placers.SAPlacer.Swap;
import placers.SAPlacer.SAPlacer;
import timinganalysis.TimingGraph;
import tools.CsvReader;
import tools.CsvWriter;
import visual.ArchitecturePanel;
import circuit.Ble;
import circuit.Block;
import circuit.BlockType;
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
		boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
			    getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
		if(isDebug)
		{
			System.out.println("Debugging");
		}
		else
		{
			System.out.println("Not debugging");
		}
		
		//Wait for enter to start (necessary for easy profiling)
//		System.out.println("Hit any key to continue...");
//		try
//		{
//			System.in.read();
//		}
//		catch(IOException ioe)
//		{
//			System.out.println("Something went wrong");
//		}
		
		BlifReader blifReader = new BlifReader();
		PrePackedCircuit prePackedCircuit;
		try
		{
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/i1.blif", 6);
			prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ecc.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/C17.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ex5p.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/apex5.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/bbrtas.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/s27.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/clma.blif", 6);
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
		
		//visualLegalizerTest();
		
		//testCostCalculator(packedCircuit);
	}
	
//	public static void main(String[] args)
//	{
//		File folder = new File("benchmarks/Blif/6/");
//		File[] listOfFiles = folder.listFiles();
//		CsvWriter csvWriter;
//		CsvReader csvReader = new CsvReader();
//		boolean success = csvReader.readFile("benchmarks.csv");
//		String[] alreadyDoneFiles;
//		if(success)
//		{
//			csvWriter = new CsvWriter(csvReader.getData(), csvReader.getNbColumns());
//			alreadyDoneFiles = csvReader.getColumn(0, 1, csvReader.getNbRows() - 1);
//		}
//		else
//		{
//			csvWriter = new CsvWriter(13);
//			csvWriter.addRow(new String[] {"Benchmark name", "Nb Clbs", "Nb of inputs", "Nb of outputs", "SA time", 
//					"SA cost", "SA max delay", "Analytical solve time", "Analytical anneal time", "Analytical cost pre-anneal", 
//					"Analytical cost post-anneal", "Analytical max delay pre-anneal", "Analytical max delay post-anneal"});
//			alreadyDoneFiles = null;
//		}
//		for(int i = 0; i < listOfFiles.length; i++)
//		{
//			if(listOfFiles[i].isFile())
//			{
//				String fileName = listOfFiles[i].getName();
//				if(fileName.substring(fileName.length() - 4).contains("blif"))
//				{
//					System.out.println("Processing benchmark: " + fileName);
//					String totalFilename = "benchmarks/Blif/6/" + fileName;
//					if(alreadyDone(totalFilename, alreadyDoneFiles))
//					{
//						System.out.println("Already done this benchmark!");
//					}
//					else
//					{
//						processBenchmark(totalFilename,csvWriter);
//					}
//				}
//			}
//			csvWriter.writeFile("benchmarks.csv");
//		}
//	}
	
	private static boolean alreadyDone(String fileName, String[] alreadyDoneFiles)
	{
		for(int i = 0; i < alreadyDoneFiles.length; i++)
		{
			if(alreadyDoneFiles[i].contains(fileName))
			{
				return true;
			}
		}
		return false;
	}
	
//	public static void main(String[] args)
//	{
//		CsvReader csvReader = new CsvReader();
//		boolean test = csvReader.readFile("benchmarks.csv");
//		if(test)
//		{
//			System.out.println("Succeeded");
//		}
//		else
//		{
//			System.out.println("Failed");
//		}
//		CsvWriter csvWriter = new CsvWriter(csvReader.getData(), csvReader.getNbColumns());
//		csvWriter.addRow(new String[] {"Test", "Test", "Test", "Test", "Test", "Test", "Test", "Test", "Test", "Test", "Test", "Test", "Test", });
//		String[] columnOne = csvReader.getColumn(0, 1, csvReader.getNbRows() - 1);
//		for(int i = 0; i < 2; i++)
//		{
//			System.out.println(columnOne[i]);
//		}
//	}
	
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
		int archSize = calculateArchDimension(c);
		int height = archSize;
		int width = archSize;
//		int height = 30;
//		int width = 30;
		int trackwidth = 4;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		int legalizer = 3;
		AnalyticalPlacerFive analyticalPlacer = new AnalyticalPlacerFive(a, c, legalizer);
		//AnalyticalPlacerFour placer = new AnalyticalPlacerFour(a,c,bbncc);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		long analyticalStartTime;
		long analyticalEndTime;
		long SAStartTime;
		long SAEndTime;
		
		//Analytical phase
		analyticalStartTime = System.nanoTime();
		analyticalPlacer.place();
		analyticalEndTime = System.nanoTime();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(c);
		SAPlacer saPlacer= new SAPlacer(effcc, a, c);
		
		//SA phase
		SAStartTime = System.nanoTime();
		//saPlacer.customAnneal(30, 4, 12500);
		//saPlacer.place(4.0);
		saPlacer.lowTempAnneal(4.0);
		SAEndTime = System.nanoTime();
		
		double AnalyticalTime = (double)(analyticalEndTime - analyticalStartTime) / 1000000000.0;
		double SATime = (double)(SAEndTime - SAStartTime) / 1000000000.0;
		
		System.out.printf("Time necessary to place: %.3f s\n", AnalyticalTime + SATime);
		System.out.printf("\tAnalytical placement time: %.3f s\n", AnalyticalTime);
		System.out.printf("\tSimulated annealing refinement time: %.3f s\n", SATime);
		
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost after low temperature anneal: " + effcc.calculateTotalCost());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(945,970);
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
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(c);
		
		SAPlacer placer= new SAPlacer(effcc, a, c);
		
		long startTime;
		long endTime;
		startTime = System.nanoTime();
		placer.place(placementEffort);
		endTime = System.nanoTime();
		
		System.out.printf("Time necessary to place: %.3f s\n", (double)(endTime - startTime)/1000000000);
		
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost SA placement: " + effcc.calculateTotalCost());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(950,950);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void processBenchmark(String totalFilename, CsvWriter csvWriter)
	{
		double[] sAResults = new double[6];
		processSABenchmark(sAResults,totalFilename);
		double sATime = sAResults[0];
		double sACost = sAResults[1];
		int nbClbs = (int)Math.round(sAResults[2]);
		int nbInputs = (int)Math.round(sAResults[3]);
		int nbOutputs = (int)Math.round(sAResults[4]);
		double sAMaxDelay = sAResults[5];

		double[] analyticalResults = new double[6];
		processAnalyticalBenchmark(analyticalResults, totalFilename);
		double analyticalSolveTime = analyticalResults[0];
		double analyticalAnnealTime = analyticalResults[1];
		double analyticalCostBefore = analyticalResults[2];
		double analyticalCostAfter = analyticalResults[3];
		double analyticalMaxDelayBefore = analyticalResults[4];
		double analyticalMaxDelayAfter = analyticalResults[5];
		
		String nbClbsString = String.format("%d", nbClbs);
		String nbInputsString = String.format("%d", nbInputs);
		String nbOutputsString = String.format("%d", nbOutputs);
		String sATimeString = String.format("%.3f", sATime);
		String sACostString = String.format("%.3f", sACost);
		String sAMaxDelayString = String.format("%.3f", sAMaxDelay);
		String analyticalSolveTimeString = String.format("%.3f", analyticalSolveTime);
		String analyticalAnnealTimeString = String.format("%.3f", analyticalAnnealTime);
		String analyticalCostBeforeString = String.format("%.3f", analyticalCostBefore);
		String analyticalCostAfterString = String.format("%.3f", analyticalCostAfter);
		String analyticalMaxDelayBeforeString = String.format("%.3f", analyticalMaxDelayBefore);
		String analyticalMaxDelayAfterString = String.format("%.3f", analyticalMaxDelayAfter);
		
		csvWriter.addRow(new String[] {totalFilename, nbClbsString, nbInputsString, nbOutputsString, sATimeString, sACostString, sAMaxDelayString, 
						analyticalSolveTimeString, analyticalAnnealTimeString, analyticalCostBeforeString, analyticalCostAfterString, 
						analyticalMaxDelayBeforeString, analyticalMaxDelayAfterString});
	}
	
	private static void processAnalyticalBenchmark(double[] results, String totalFilename)
	{
		BlifReader blifReader = new BlifReader();
		PrePackedCircuit prePackedCircuit;
		try
		{
			prePackedCircuit =  blifReader.readBlif(totalFilename, 6);
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read blif file!");
			return;
		}
	
		BlePacker blePacker = new BlePacker(prePackedCircuit);
		BlePackedCircuit blePackedCircuit = blePacker.pack();
	
		ClbPacker clbPacker = new ClbPacker(blePackedCircuit);
		PackedCircuit packedCircuit = clbPacker.pack();
	
		int dimension = calculateArchDimension(packedCircuit);
		
		int height = dimension;
		int width = dimension;
		int trackwidth = 4;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		int legalizer = 3;
		AnalyticalPlacerFive placer = new AnalyticalPlacerFive(a, packedCircuit, legalizer);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,packedCircuit,rand);
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		
		SAPlacer saPlacer= new SAPlacer(effcc, a, packedCircuit);
		
		long startTime;
		long analyticalEndTime;
		long annealStartTime;
		long endTime;
		startTime = System.nanoTime();
		placer.place();
		analyticalEndTime = System.nanoTime();
		
		results[2] = effcc.calculateTotalCost();
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelayBefore = timingGraph.calculateMaximalDelay();
		results[4] = maxDelayBefore;
		
		annealStartTime = System.nanoTime();
		saPlacer.lowTempAnneal(4.0);
		endTime = System.nanoTime();
		
		results[0] = (double)(analyticalEndTime - startTime)/1000000000;
		results[1] = (double)(endTime - annealStartTime)/1000000000;
		
		pm.PlacementCLBsConsistencyCheck();
		results[3] = effcc.calculateTotalCost();
		TimingGraph timingGraphTwo = new TimingGraph(prePackedCircuit);
		timingGraphTwo.buildTimingGraph();
		double maxDelayAfter = timingGraphTwo.calculateMaximalDelay();
		results[5] = maxDelayAfter;
	}
	
	private static void processSABenchmark(double[] results, String totalFilename)
	{
		BlifReader blifReader = new BlifReader();
		PrePackedCircuit prePackedCircuit;
		try
		{
			prePackedCircuit =  blifReader.readBlif(totalFilename, 6);
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read blif file!");
			return;
		}
	
		BlePacker blePacker = new BlePacker(prePackedCircuit);
		BlePackedCircuit blePackedCircuit = blePacker.pack();
	
		ClbPacker clbPacker = new ClbPacker(blePackedCircuit);
		PackedCircuit packedCircuit = clbPacker.pack();
	
		int dimension = calculateArchDimension(packedCircuit);
		
		int height = dimension;
		int width = dimension;
		int trackwidth = 4;
		
		Double placementEffort = 10.0;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,packedCircuit,rand);
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(packedCircuit, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		SAPlacer placer= new SAPlacer(effcc, a, packedCircuit);
		
		long startTime;
		long endTime;
		startTime = System.nanoTime();
		placer.place(placementEffort);
		endTime = System.nanoTime();
		
		results[0] = (double)(endTime - startTime)/1000000000;
		
		pm.PlacementCLBsConsistencyCheck();
		results[1] = effcc.calculateTotalCost();
		results[2] = packedCircuit.clbs.values().size();
		results[3] = packedCircuit.getInputs().values().size();
		results[4] = packedCircuit.getOutputs().values().size();
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		results[5] = maxDelay;
	}
	
	private static int calculateArchDimension(PackedCircuit circuit)
	{
		int nbInputs = circuit.getInputs().values().size();
		int nbOutputs = circuit.getOutputs().values().size();
		int nbClbs = circuit.clbs.values().size();
		int maxIO;
		if(nbInputs > nbOutputs)
		{
			maxIO = nbInputs;
		}
		else
		{
			maxIO = nbOutputs;
		}
		int x1 = (maxIO + 3) / 4;
		int x2 = (int)Math.ceil(Math.sqrt(nbClbs * 1.20));
		int x;
		if(x1 > x2)
		{
			x = x1;
		}
		else
		{
			x = x2;
		}
		return x;
	}
	
	private static void visualLegalizerTest()
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		int size = 45;
		double[] linearX = new double[size];
		double[] linearY = new double[size];
		String[] names = new String[size];
		for(int i = 0; i < size; i++)
		{
			linearX[i] = 15.0 + 0.2*i;
			linearY[i] = 15.0 + 0.2*i;
			names[i] = String.format("Nb_%d", i);
		}
		
		PackedCircuit circuit = new PackedCircuit();
		
		LegalizerOne legalizer = new LegalizerOne(1, 30, 1, 30, size);
		legalizer.legalize(linearX, linearY, circuit.getNets().values(), null);
		int[] legalX = new int[size];
		int[] legalY = new int[size];
		legalizer.getBestLegal(legalX, legalY);
		
		Map<String,Clb> clbs = circuit.clbs;
		for(int i = 0; i < size; i++)
		{
			String name = names[i];
			Clb clb = new Clb(name,1,6);
			Site site = a.getSite(legalX[i], legalY[i], 0);
			site.block = clb;
			clb.setSite(site);
			clbs.put(name,clb);
		}
		
		Clb clb = circuit.clbs.get("Nb_0");
		if(clb != null)
		{
			System.out.println("Nb_0: (" + clb.getSite().x + "," + clb.getSite().y + ")");
		}
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(945,970);
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
//		int archSize = calculateArchDimension(c);
//		int width = archSize;
//		int height = archSize;
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		Double placementEffort = 10.0;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(c);
		//BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total Cost random placement: " + effcc.calculateTotalCost());
		//System.out.println("Total Cost random placement: " + bbncc.calculateTotalCost());
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay random placement: " + maxDelay);
		
		SAPlacer placer= new SAPlacer(effcc, a, c);
		//Vplace placer= new Vplace(pm, bbncc);
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
		System.out.println("Total cost SA placement: " + effcc.calculateTotalCost());
		//System.out.println("Total cost SA placement: " + bbncc.calculateTotalCost());
		pm.PlacementCLBsConsistencyCheck();		
		timingGraph.updateDelays();
		double maxDelayUpdated = timingGraph.calculateMaximalDelay();
		System.out.println("Critical path delay after SA placement: " + maxDelayUpdated);
	}
	
	private static void testCostCalculator(PackedCircuit c)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		Random rand = new Random(1);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(c, a, rand);
		
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(c);
		
		c.vBlocks = new Vector<Block>();
		c.vBlocks.addAll(c.clbs.values());
		c.vBlocks.addAll(c.inputs.values());
		c.vBlocks.addAll(c.outputs.values());
		
		for(int i = 0; i < 10000; i++)
		{
			Swap swap=new Swap();
			Block b = c.vBlocks.elementAt(rand.nextInt(c.vBlocks.size()));
			swap.pl1 = b.getSite();
			if(b.type==BlockType.CLB)
			{
				swap.pl2 = a.randomSite(15, swap.pl1);
			}
			else if(b.type == BlockType.INPUT)
			{
				swap.pl2 = a.randomISite(15, swap.pl1);
			}
			else if(b.type == BlockType.OUTPUT)
			{
				swap.pl2 = a.randomOSite(15, swap.pl1);
			}
			double deltaCostOld = bbncc.calculateDeltaCost(swap);
			double deltaCostNew = effcc.calculateDeltaCost(swap);
			if(deltaCostOld < 0)
			{
				swap.apply();
				effcc.pushThrough();
			}
			else
			{
				effcc.revert();
			}
			if(!(deltaCostOld > deltaCostNew - 0.05 && deltaCostOld < deltaCostNew + 0.05) || 
						!(bbncc.calculateTotalCost() > effcc.calculateTotalCost() - 0.05 && bbncc.calculateTotalCost() < effcc.calculateTotalCost() + 0.05))
			{
				System.out.printf("Old total = %.3f; new total = %.3f; old delta = %.3f; new delta = %.3f\n", 
						bbncc.calculateTotalCost(), effcc.calculateTotalCost(), deltaCostOld, deltaCostNew);
				if(swap.pl1.block == null)
				{
					System.out.println("First block null");
				}
				if(swap.pl2.block == null)
				{
					System.out.println("Second block null");
				}
				Block consideredBlock = swap.pl2.block;
				for(Net net: c.getNets().values())
				{
					boolean affected = false;
					if(net.source.owner == consideredBlock)
					{
						affected = true;
					}
					for(Pin pin: net.sinks)
					{
						if(pin.owner == consideredBlock)
						{
							affected = true;
							break;
						}
					}
					if(affected)
					{
						System.out.printf("Net blocks: (%d,%d), ", net.source.owner.getSite().x, net.source.owner.getSite().y);
						for(Pin pin: net.sinks)
						{
							System.out.printf("(%d,%d), ", pin.owner.getSite().x, pin.owner.getSite().y);
						}
						System.out.println();
					}
				}
				break;
			}
		}
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
