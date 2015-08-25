package placers.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import placers.random.RandomPlacer;

import architecture.FourLutSanitized;
import circuit.PackedCircuit;
import circuit.parser.netlist.ParseException;
import circuit.parser.netlist.Readnetlist;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Elias.
 *         Created 31-jul.-2012.
 */
public class Example {

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
//		String placementOutFile = "cc.p";
		int height = 12;
		int width = 12;
		int trackwidth = 1;
		Double placementEffort = 10.;
// why not reading net list if it means to show that cc.net is found	
		System.out.println("Read in netlist.");
		Readnetlist parser=new Readnetlist(new FileInputStream(new File(netFile)));
		PackedCircuit c=parser.read(true);

		System.out.println("Constructing architecture.");
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
			
		Random rand = new Random(1);
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);

		//Random placement
		RandomPlacer.placeCLBsandIOs(c, a, rand);
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
