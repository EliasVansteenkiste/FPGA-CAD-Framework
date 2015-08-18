package placers.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import architecture.old.FourLutSanitized;
import placement.parser.Placement;
import placement.parser.Readplaatsing;
import placers.Rplace;

import circuit.PackedCircuit;
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
		PackedCircuit c=parser.read(true);

		System.out.println("Read in VPR placement.");		
		Readplaatsing plaats_parser=new Readplaatsing(new FileInputStream(new File(placementVPRFile)));
		Placement p=plaats_parser.read();

		System.out.println("Constructing architecture.");
		FourLutSanitized a = new FourLutSanitized(width,height,trackwidth);
		
		//Apply VPR placement
		c.place(p, a);
		
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
