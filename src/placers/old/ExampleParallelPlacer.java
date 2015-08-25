package placers.old;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import placers.Rplace;

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
public class ExampleParallelPlacer {
	
	
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws ParseException 
	 * @throws placement.parser.ParseException 
	 * @throws architecture.ParseException 
	 * @throws ParseException 
	 */
	
	public static void main(String[] args) throws FileNotFoundException, placement.parser.ParseException, ParseException {
		String netFile = "cc.net";
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
		//Placement manipulator
		PlacementManipulatorIOCLB pm = new PlacementManipulatorIOCLB(a,c,rand);
		//Cost Calculator
		BoundingBoxNetCC bbncc = new BoundingBoxNetCC(c);
		//Random placement
		Rplace.placeCLBsandIOs(c, a, rand);
		pm.PlacementCLBsConsistencyCheck();
		System.out.println("Total Cost random placement: "+bbncc.calculateTotalCost());
		
		pplacer parplacer= new pplacer(pm,bbncc);
		final long startTime = System.nanoTime();
		final long endTime;
		try {
			 parplacer.place(placementEffort);
		} finally {
		  endTime = System.nanoTime();
		}
		final long duration = endTime - startTime;
		System.out.println("Runtime: "+(duration/1.0E9));
		System.out.println("Total Cost of placement: "+bbncc.calculateTotalCost());
		
		
		
	
	}


}




