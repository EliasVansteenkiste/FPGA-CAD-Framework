package placers.analyticalplacer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
import placers.SAPlacer.TD_SAPlacer;
import placers.SAPlacer.WLD_SAPlacer;
import timinganalysis.TimingGraph;
import timinganalysis.TimingGraphOld;
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
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ecc.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/C17.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/bbara.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/ex5p.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/apex5.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/apex4.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/bbrtas.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/s27.blif", 6);
			prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/clma.blif", 6);
			//prePackedCircuit =  blifReader.readBlif("benchmarks/Blif/6/s38584.1.blif", 6);
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
		
		//visualAnalytical(packedCircuit, prePackedCircuit);
		visualTDAnalytical(packedCircuit, prePackedCircuit);
		
		//visualTDanalyticalTestCircuit();
		
		//visualSA(prePackedCircuit, packedCircuit);
		//visualTDSA(prePackedCircuit, packedCircuit);
		
		//RunWlVsTdSaBenchmarks();
		//RunTDSaVsAnalyticalBenchmarks();
		
		//visualLegalizerTest();
		
		//testCostCalculator(packedCircuit);
		//testTimingCostCalculator(prePackedCircuit, packedCircuit);
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
	
	private static void visualTDanalyticalTestCircuit()
	{
		PrePackedCircuit prePackedCircuit = new PrePackedCircuit(6);
		PackedCircuit c = new PackedCircuit();
		FourLutSanitized a = constructTestCircuit(prePackedCircuit, c);
		
		TD_AnalyticalPlacerOne tDAnalyticalPlacer = new TD_AnalyticalPlacerOne(a, c, prePackedCircuit);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		tDAnalyticalPlacer.place();
		
		pm.PlacementCLBsConsistencyCheck();
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(c);
		System.out.println("Total cost after low temperature anneal: " + effcc.calculateTotalCost());
		
		//TimingGraphOld timingGraphOld = new TimingGraphOld(prePackedCircuit);
		//timingGraphOld.buildTimingGraph();
		//double maxDelayOld = timingGraphOld.calculateMaximalDelay();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		
		//System.out.println("Max delay old = " + maxDelayOld + ", max delay new = " + maxDelay);
		System.out.println("Max delay new timing graph = " + maxDelay);
		
		//System.out.println(timingGraph.getMapString());
		//System.out.println(timingGraph.getStartSlacks());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(945,970);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void RunTDSaVsAnalyticalBenchmarks()
	{
		String toDoFileName = "BenchmarksToDo.txt";
		String csvFileName = "benchmarksTDAnalyticalVsWLDAnalyticalVsTDSaVsWLDSa.csv";
		String[] fileNamesToDo;
		try
		{
			File toDoFile = new File(toDoFileName);
			if(!toDoFile.exists())
			{
				System.out.println("No TODO file found\nAborting...");
			}
			FileReader fileReader = new FileReader(toDoFile.getAbsoluteFile());
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			ArrayList<String> rowsList = new ArrayList<>();
			String curLine = bufferedReader.readLine();
			int nbRows = 0;
			while(curLine != null)
			{
				rowsList.add(curLine);
				nbRows++;
				curLine = bufferedReader.readLine();
			}
			bufferedReader.close();
			fileNamesToDo = new String[nbRows];
			rowsList.toArray(fileNamesToDo);
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read TODO file: " + toDoFileName);
			return;
		}
		
		CsvWriter csvWriter;
		CsvReader csvReader = new CsvReader();
		boolean success = csvReader.readFile(csvFileName);
		String[] alreadyDoneFiles;
		if(success)
		{
			csvWriter = new CsvWriter(csvReader.getData(), csvReader.getNbColumns());
			alreadyDoneFiles = csvReader.getColumn(0, 1, csvReader.getNbRows() - 1);
		}
		else
		{
			csvWriter = new CsvWriter(12);
			csvWriter.addRow(new String[] {"Benchmark name", "Nb Clbs", "Nb of inputs", "Nb of outputs", "WL TD_Analytical", 
					"Max delay TD_Analytical", "WL WLD_Analytical", "Max delay WLD_Analytical", "WL TD_SA", "Max delay TD_SA", 
					"WL WLD_SA", "Max delay WLD_SA"});
			alreadyDoneFiles = null;
		}
		for(int i = 0; i < fileNamesToDo.length; i++)
		{
			if(fileNamesToDo[i].substring(fileNamesToDo[i].length() - 4).contains("blif"))
			{
				System.out.println("Processing benchmark: " + fileNamesToDo[i]);
				String totalFilename = fileNamesToDo[i];
				if(alreadyDone(totalFilename, alreadyDoneFiles))
				{
					System.out.println("Already done this benchmark!");
				}
				else
				{
					double[] tdAnalyticalResults = new double[2];
					processTDAnalyticalBenchmark_NoRefinement(tdAnalyticalResults, totalFilename);
					double tdAnalyticalMaxDelay = tdAnalyticalResults[0];
					double tdAnalyticalWL = tdAnalyticalResults[1];
					
					double[] wldAnalyticalResults = new double[2];
					processWLDAnalyticalBenchmark_NoRefinement(wldAnalyticalResults, totalFilename);
					double wldAnalyticalMaxDelay = wldAnalyticalResults[0];
					double wldAnalyticalWL = wldAnalyticalResults[1];
					
					double[] tdSAResults = new double[6];
					processTDSABenchmark(tdSAResults, totalFilename);
					double tdSAWL = tdSAResults[1];
					int nbClbs = (int)Math.round(tdSAResults[2]);
					int nbInputs = (int)Math.round(tdSAResults[3]);
					int nbOutputs = (int)Math.round(tdSAResults[4]);
					double tdSAMaxDelay = tdSAResults[5];
					
					double[] wldSAResults = new double[6];
					processWLDSABenchmark(wldSAResults, totalFilename);
					double wldSAWL = wldSAResults[1];
					double wldSAMaxDelay = wldSAResults[5];
					
					String nbClbsString = String.format("%d", nbClbs);
					String nbInputsString = String.format("%d", nbInputs);
					String nbOutputsString = String.format("%d", nbOutputs);
					String tdAnalyticalMaxDelayString = String.format("%.3f", tdAnalyticalMaxDelay);
					String tdAnalyticalWLString = String.format("%.3f", tdAnalyticalWL);
					String wldAnalyticalMaxDelayString = String.format("%.3f", wldAnalyticalMaxDelay);
					String wldAnalyticalWLString = String.format("%.3f", wldAnalyticalWL);
					String tdSAWLString = String.format("%.3f", tdSAWL);
					String tdSAMaxDelayString = String.format("%.3f", tdSAMaxDelay);
					String wldSAWLString = String.format("%.3f", wldSAWL);
					String wldSAMaxDelayString = String.format("%.3f", wldSAMaxDelay);
					
					csvWriter.addRow(new String[] {totalFilename, nbClbsString, nbInputsString, nbOutputsString, tdAnalyticalWLString,
							tdAnalyticalMaxDelayString, wldAnalyticalWLString, wldAnalyticalMaxDelayString, tdSAWLString, 
							tdSAMaxDelayString, wldSAWLString, wldSAMaxDelayString});
				}
			}
			csvWriter.writeFile(csvFileName);
		}
	}
	
	private static void RunWlVsTdSaBenchmarks()
	{
		String toDoFileName = "BenchmarksToDo.txt";
		String[] fileNamesToDo;
		try
		{
			File toDoFile = new File(toDoFileName);
			if(!toDoFile.exists())
			{
				System.out.println("No TODO file found\nAborting...");
			}
			FileReader fileReader = new FileReader(toDoFile.getAbsoluteFile());
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			ArrayList<String> rowsList = new ArrayList<>();
			String curLine = bufferedReader.readLine();
			int nbRows = 0;
			while(curLine != null)
			{
				rowsList.add(curLine);
				nbRows++;
				curLine = bufferedReader.readLine();
			}
			bufferedReader.close();
			fileNamesToDo = new String[nbRows];
			rowsList.toArray(fileNamesToDo);
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read TODO file: " + toDoFileName);
			return;
		}
		
		CsvWriter csvWriter;
		CsvReader csvReader = new CsvReader();
		boolean success = csvReader.readFile("benchmarksSa_TdVsWld.csv");
		String[] alreadyDoneFiles;
		if(success)
		{
			csvWriter = new CsvWriter(csvReader.getData(), csvReader.getNbColumns());
			alreadyDoneFiles = csvReader.getColumn(0, 1, csvReader.getNbRows() - 1);
		}
		else
		{
			csvWriter = new CsvWriter(10);
			csvWriter.addRow(new String[] {"Benchmark name", "Nb Clbs", "Nb of inputs", "Nb of outputs", "WLD SA time", 
					"WLD SA cost", "WLD SA max delay", "TD SA time", "TD SA cost", "TD SA max delay"});
			alreadyDoneFiles = null;
		}
		for(int i = 0; i < fileNamesToDo.length; i++)
		{
			if(fileNamesToDo[i].substring(fileNamesToDo[i].length() - 4).contains("blif"))
			{
				System.out.println("Processing benchmark: " + fileNamesToDo[i]);
				String totalFilename = fileNamesToDo[i];
				if(alreadyDone(totalFilename, alreadyDoneFiles))
				{
					System.out.println("Already done this benchmark!");
				}
				else
				{
					double[] wldSAResults = new double[6];
					processWLDSABenchmark(wldSAResults,totalFilename);
					double wldSATime = wldSAResults[0];
					double wldSACost = wldSAResults[1];
					int nbClbs = (int)Math.round(wldSAResults[2]);
					int nbInputs = (int)Math.round(wldSAResults[3]);
					int nbOutputs = (int)Math.round(wldSAResults[4]);
					double wldSAMaxDelay = wldSAResults[5];

					double[] tdSAResults = new double[6];
					processTDSABenchmark(tdSAResults, totalFilename);
					double tdSATime = tdSAResults[0];
					double tdSACost = tdSAResults[1];
					double tdSAMaxDelay = tdSAResults[5];
					
					String nbClbsString = String.format("%d", nbClbs);
					String nbInputsString = String.format("%d", nbInputs);
					String nbOutputsString = String.format("%d", nbOutputs);
					String wldSATimeString = String.format("%.3f", wldSATime);
					String wldSACostString = String.format("%.3f", wldSACost);
					String wldSAMaxDelayString = String.format("%.3f", wldSAMaxDelay);
					String tdSATimeString = String.format("%.3f", tdSATime);
					String tdSACostString = String.format("%.3f", tdSACost);
					String tdSAMaxDelayString = String.format("%.3f", tdSAMaxDelay);
					
					
					csvWriter.addRow(new String[] {totalFilename, nbClbsString, nbInputsString, nbOutputsString, wldSATimeString, wldSACostString, 
									wldSAMaxDelayString, tdSATimeString, tdSACostString, tdSAMaxDelayString});
				}
			}
			csvWriter.writeFile("benchmarksSa_TdVsWld.csv");
		}
	}
	
	private static boolean alreadyDone(String fileName, String[] alreadyDoneFiles)
	{
		if(alreadyDoneFiles != null)
		{
			for(int i = 0; i < alreadyDoneFiles.length; i++)
			{
				if(alreadyDoneFiles[i].contains(fileName))
				{
					return true;
				}
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
//		//testCrs();
//		crsBugReconstruct();
//	}
	
	private static void visualAnalytical(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int archSize = calculateArchDimension(c);
		int height = archSize;
		int width = archSize;
//		int height = 30;
//		int width = 30;
		int trackwidth = 4;
		
		System.out.println(prePackedCircuit.getName() + ": LUTs: " + prePackedCircuit.getLuts().values().size() + ", FFs: " + prePackedCircuit.getFlipflops().values().size() 
				+ ", inputs: " + prePackedCircuit.getInputs().values().size() + ", outputs: " + prePackedCircuit.getOutputs().values().size());
		
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
		WLD_SAPlacer saPlacer= new WLD_SAPlacer(effcc, a, c);
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelayBeforeRefinement = timingGraph.calculateMaximalDelay();
		
		//SA phase
		SAStartTime = System.nanoTime();
		//saPlacer.customAnneal(30, 4, 12500);
		//saPlacer.place(4.0);
		//saPlacer.lowTempAnneal(4.0);
		SAEndTime = System.nanoTime();
		
		double AnalyticalTime = (double)(analyticalEndTime - analyticalStartTime) / 1000000000.0;
		double SATime = (double)(SAEndTime - SAStartTime) / 1000000000.0;
		
		System.out.printf("Time necessary to place: %.3f s\n", AnalyticalTime + SATime);
		System.out.printf("\tAnalytical placement time: %.3f s\n", AnalyticalTime);
		System.out.printf("\tSimulated annealing refinement time: %.3f s\n", SATime);
		
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost after low temperature anneal: " + effcc.calculateTotalCost());
		
		timingGraph.updateDelays();
		double maxDelayAfterRefinement = timingGraph.calculateMaximalDelay();
		
		TimingGraph newTimingGraph = new TimingGraph(prePackedCircuit);
		newTimingGraph.buildTimingGraph();
		double maxDelayAfterRefinement2 = newTimingGraph.calculateMaximalDelay();
		
		System.out.println("Max delay before refinement: " + maxDelayBeforeRefinement);
		System.out.println("Max delay after refinement: " + maxDelayAfterRefinement + " (" + maxDelayAfterRefinement2 + ")");
		
		//System.out.println(timingGraph.getMapString());
		//System.out.println(timingGraph.getStartSlacks());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(945,970);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void visualTDAnalytical(PackedCircuit c, PrePackedCircuit prePackedCircuit)
	{
		int archSize = calculateArchDimension(c);
		int height = archSize;
		int width = archSize;
		int trackwidth = 4;
		
		System.out.println(prePackedCircuit.getName() + ": LUTs: " + prePackedCircuit.getLuts().values().size() + ", FFs: " + prePackedCircuit.getFlipflops().values().size() 
				+ ", inputs: " + prePackedCircuit.getInputs().values().size() + ", outputs: " + prePackedCircuit.getOutputs().values().size());
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		TD_AnalyticalPlacerOne tDAnalyticalPlacer = new TD_AnalyticalPlacerOne(a, c, prePackedCircuit);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		
		tDAnalyticalPlacer.place();
		
		pm.PlacementCLBsConsistencyCheck();
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(c);
		System.out.println("Total cost after low temperature anneal: " + effcc.calculateTotalCost());
		
		//TimingGraphOld timingGraphOld = new TimingGraphOld(prePackedCircuit);
		//timingGraphOld.buildTimingGraph();
		//double maxDelayOld = timingGraphOld.calculateMaximalDelay();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		
		//System.out.println("Max delay old = " + maxDelayOld + ", max delay new = " + maxDelay);
		System.out.println("Max delay new timing graph = " + maxDelay);
		
		//System.out.println(timingGraph.getMapString());
		//System.out.println(timingGraph.getStartSlacks());
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(945,970);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void visualTDSA(PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit)
	{
		int archSize = calculateArchDimension(packedCircuit);
		int height = archSize;
		int width = archSize;
		int trackwidth = 4;
		Double placementEffort = 10.0;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,packedCircuit,rand);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(packedCircuit, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		TimingGraph tcc = new TimingGraph(prePackedCircuit);
		tcc.buildTimingGraph();
		
		TD_SAPlacer placer= new TD_SAPlacer(effcc, a, packedCircuit, tcc);
		
		long startTime;
		long endTime;
		startTime = System.nanoTime();
		placer.place(placementEffort);
		endTime = System.nanoTime();
		
		System.out.printf("Time necessary to place: %.3f s\n", (double)(endTime - startTime)/1000000000);
		
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost SA placement: " + effcc.calculateTotalCost());
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.println("Maximum delay: " + maxDelay);
		
		ArchitecturePanel panel = new ArchitecturePanel(890, a, false);
		
		JFrame frame = new JFrame("Architecture");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(950,950);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void visualSA(PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		Double placementEffort = 10.0;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,packedCircuit,rand);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(packedCircuit, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		
		WLD_SAPlacer placer= new WLD_SAPlacer(effcc, a, packedCircuit);
		
		long startTime;
		long endTime;
		startTime = System.nanoTime();
		placer.place(placementEffort);
		endTime = System.nanoTime();
		
		System.out.printf("Time necessary to place: %.3f s\n", (double)(endTime - startTime)/1000000000);
		
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total cost SA placement: " + effcc.calculateTotalCost());
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		System.out.println("Maximum delay: " + maxDelay);
		
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
		processWLDSABenchmark(sAResults,totalFilename);
		double sATime = sAResults[0];
		double sACost = sAResults[1];
		int nbClbs = (int)Math.round(sAResults[2]);
		int nbInputs = (int)Math.round(sAResults[3]);
		int nbOutputs = (int)Math.round(sAResults[4]);
		double sAMaxDelay = sAResults[5];

		double[] analyticalResults = new double[6];
		processWLDAnalyticalBenchmark(analyticalResults, totalFilename);
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
	
	private static void processWLDAnalyticalBenchmark(double[] results, String totalFilename)
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
		
		long startTime;
		long analyticalEndTime;
		long annealStartTime;
		long endTime;
		startTime = System.nanoTime();
		placer.place();
		analyticalEndTime = System.nanoTime();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		WLD_SAPlacer saPlacer= new WLD_SAPlacer(effcc, a, packedCircuit);
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
	
	private static void processWLDAnalyticalBenchmark_NoRefinement(double[] results, String totalFilename)
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
		
		placer.place();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		double totalBBCost = effcc.calculateTotalCost();
		
		results[0] = maxDelay;
		results[1] = totalBBCost;
	}
	
	private static void processTDAnalyticalBenchmark_NoRefinement(double[] results, String totalFilename)
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
		TD_AnalyticalPlacerOne placer = new TD_AnalyticalPlacerOne(a, packedCircuit, prePackedCircuit);
		
		placer.place();
		
		TimingGraph timingGraph = new TimingGraph(prePackedCircuit);
		timingGraph.buildTimingGraph();
		double maxDelay = timingGraph.calculateMaximalDelay();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		double totalBBCost = effcc.calculateTotalCost();
		
		results[0] = maxDelay;
		results[1] = totalBBCost;
	}
	
	private static void processWLDSABenchmark(double[] results, String totalFilename)
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
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(packedCircuit, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		
		WLD_SAPlacer placer= new WLD_SAPlacer(effcc, a, packedCircuit);
		
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
	
	private static void processTDSABenchmark(double[] results, String totalFilename)
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
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(packedCircuit, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(packedCircuit);
		TimingGraph tcc = new TimingGraph(prePackedCircuit);
		tcc.buildTimingGraph();
		
		TD_SAPlacer placer= new TD_SAPlacer(effcc, a, packedCircuit, tcc);
		
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
		
		SAPlacer placer= new WLD_SAPlacer(effcc, a, c);
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
	
	private static void testTimingCostCalculator(PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit)
	{
		int height = 30;
		int width = 30;
		int trackwidth = 4;
		
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		Random rand = new Random(1);
		
		//Random placement
		Rplace.placeCLBsandFixedIOs(packedCircuit, a, rand);
		
		TimingGraph tcc = new TimingGraph(prePackedCircuit);
		tcc.buildTimingGraph();
		double startCost = tcc.calculateTotalCost();
		double startMaxDelay = tcc.calculateMaximalDelay();
		
		System.out.println("\nOriginal cost = " + startCost);
		System.out.println("Original max delay = " + startMaxDelay);
		System.out.println();
		
		packedCircuit.vBlocks = new Vector<Block>();
		packedCircuit.vBlocks.addAll(packedCircuit.clbs.values());
		
		for(int i = 0; i < 10000; i++)
		{
			Swap swap=new Swap();
			Block b = packedCircuit.vBlocks.elementAt(rand.nextInt(packedCircuit.vBlocks.size()));
			swap.pl1 = b.getSite();
			swap.pl2 = a.randomSite(15, swap.pl1);
			
			double deltaCost = tcc.calculateDeltaCost(swap);
			if(deltaCost < 0)
			{
				swap.apply();
				tcc.pushThrough();
			}
			else
			{
				tcc.revert();
			}
		}
		
		tcc.recalculateAllSlacksCriticalities();
		double totalCost = tcc.calculateTotalCost();
		double maxDelay = tcc.calculateMaximalDelay();
		
		TimingGraph newTimingGraph = new TimingGraph(prePackedCircuit);
		newTimingGraph.buildTimingGraph();
		double newCost = newTimingGraph.calculateTotalCost();
		double newMaxDelay = newTimingGraph.calculateMaximalDelay();
		
		System.out.println("Swapped cost = " + totalCost + ", new cost = " + newCost);
		System.out.println("Old maximum delay = " + maxDelay + ", new maximal delay = " + newMaxDelay);
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
	
	private static FourLutSanitized constructTestCircuit(PrePackedCircuit prePackedCircuit, PackedCircuit packedCircuit)
	{
		//Input
		Input input3 = new Input("input_3");
		prePackedCircuit.inputs.put(input3.name, input3);
		packedCircuit.inputs.put(input3.name, input3);
		//Output
		Output output7 = new Output("output_7");
		prePackedCircuit.outputs.put(output7.name, output7);
		packedCircuit.outputs.put(output7.name, output7);
		//Block a
		Lut luta = new Lut("lut_a", 1, 6);
		Ble blea = new Ble("ble_a", 6, null, luta, false);
		Clb clba = new Clb("clb_a", 1, 6, blea);
		prePackedCircuit.getLuts().put(luta.name, luta);
		packedCircuit.clbs.put(clba.name, clba);
		//Block b
		Lut lutb = new Lut("lut_b", 1, 6);
		Ble bleb = new Ble("ble_b", 6, null, lutb, false);
		Clb clbb = new Clb("clb_b", 1, 6, bleb);
		prePackedCircuit.getLuts().put(lutb.name, lutb);
		packedCircuit.clbs.put(clbb.name, clbb);
		//Block c
		Lut lutc = new Lut("lut_c", 1, 6);
		Ble blec = new Ble("ble_c", 6, null, lutc, false);
		Clb clbc = new Clb("clb_c", 1, 6, blec);
		prePackedCircuit.getLuts().put(lutc.name, lutc);
		packedCircuit.clbs.put(clbc.name, clbc);
		//Block d
		Lut lutd = new Lut("lut_d", 1, 6);
		Ble bled = new Ble("ble_d", 6, null, lutd, false);
		Clb clbd = new Clb("clb_d", 1, 6, bled);
		prePackedCircuit.getLuts().put(lutd.name, lutd);
		packedCircuit.clbs.put(clbd.name, clbd);
		
		Net prePackedNet1 = new Net(input3.name);
		prePackedNet1.source = input3.output;
		prePackedNet1.sinks = new Vector<Pin>();
		prePackedNet1.sinks.add(luta.getInputs()[0]);
		prePackedNet1.sinks.add(lutb.getInputs()[0]);
		prePackedCircuit.getNets().put(prePackedNet1.name, prePackedNet1);
		Net packedNet1 = new Net(input3.name);
		packedNet1.source = input3.output;
		packedNet1.sinks = new Vector<Pin>();
		packedNet1.sinks.add(clba.input[0]);
		packedNet1.sinks.add(clbb.input[0]);
		packedCircuit.getNets().put(packedNet1.name, packedNet1);
		
		Net prePackedNet2 = new Net(luta.name);
		prePackedNet2.source = luta.getOutputs()[0];
		prePackedNet2.sinks = new Vector<Pin>();
		prePackedNet2.sinks.add(lutc.getInputs()[0]);
		prePackedCircuit.getNets().put(prePackedNet2.name, prePackedNet2);
		Net packedNet2 = new Net(clba.name);
		packedNet2.source = clba.output[0];
		packedNet2.sinks = new Vector<Pin>();
		packedNet2.sinks.add(clbc.input[0]);
		packedCircuit.getNets().put(packedNet2.name, packedNet2);
		
		Net prePackedNet3 = new Net(lutb.name);
		prePackedNet3.source = lutb.getOutputs()[0];
		prePackedNet3.sinks = new Vector<Pin>();
		prePackedNet3.sinks.add(luta.getInputs()[1]);
		prePackedNet3.sinks.add(lutc.getInputs()[1]);
		prePackedNet3.sinks.add(lutd.getInputs()[0]);
		prePackedCircuit.getNets().put(prePackedNet3.name, prePackedNet3);
		Net packedNet3 = new Net(clbb.name);
		packedNet3.source = clbb.output[0];
		packedNet3.sinks = new Vector<Pin>();
		packedNet3.sinks.add(clba.input[1]);
		packedNet3.sinks.add(clbc.input[1]);
		packedNet3.sinks.add(clbd.input[0]);
		packedCircuit.getNets().put(packedNet3.name, packedNet3);
		
		Net prePackedNet4 = new Net(lutc.name);
		prePackedNet4.source = lutc.getOutputs()[0];
		prePackedNet4.sinks = new Vector<Pin>();
		prePackedNet4.sinks.add(lutd.getInputs()[1]);
		prePackedCircuit.getNets().put(prePackedNet4.name, prePackedNet4);
		Net packedNet4 = new Net(clbc.name);
		packedNet4.source = clbc.output[0];
		packedNet4.sinks = new Vector<Pin>();
		packedNet4.sinks.add(clbd.input[1]);
		packedCircuit.getNets().put(packedNet4.name, packedNet4);
		
		Net prePackedNet5 = new Net(lutd.name);
		prePackedNet5.source = lutd.getOutputs()[0];
		prePackedNet5.sinks = new Vector<Pin>();
		prePackedNet5.sinks.add(output7.input);
		prePackedCircuit.getNets().put(prePackedNet5.name, prePackedNet5);
		Net packedNet5 = new Net(clbd.name);
		packedNet5.source = clbd.output[0];
		packedNet5.sinks = new Vector<Pin>();
		packedNet5.sinks.add(output7.input);
		packedCircuit.getNets().put(packedNet5.name, packedNet5);
		
		FourLutSanitized architecture = new FourLutSanitized(10, 10, 4);
		architecture.Isites.get(4).block = input3;
		input3.setSite(architecture.Isites.get(4));
		architecture.Osites.get(12).block = output7;
		output7.setSite(architecture.Osites.get(12));
		architecture.getSite(1, 4, 0).block = clba;
		clba.setSite(architecture.getSite(1, 4, 0));
		architecture.getSite(1, 5, 0).block = clbb;
		clbb.setSite(architecture.getSite(1, 5, 0));
		architecture.getSite(1, 6, 0).block = clbc;
		clbc.setSite(architecture.getSite(1, 6, 0));
		architecture.getSite(1, 8, 0).block = clbd;
		clbd.setSite(architecture.getSite(1, 8, 0));
		
		return architecture;
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
