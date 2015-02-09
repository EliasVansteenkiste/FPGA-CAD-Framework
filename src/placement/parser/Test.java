package placement.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import circuit.Circuit;
import circuit.parser.netlist.Readnetlist;

public class Test {

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws FileNotFoundException 
	 * @throws ParseException 
	 * @throws placement.parser.ParseException 
	 * @throws circuit.parser.netlist.ParseException 
	 */
	public static void main(String[] args) throws FileNotFoundException, ParseException, ParseException, placement.parser.ParseException, circuit.parser.netlist.ParseException {
		Readplaatsing plaats_parser=new Readplaatsing(new FileInputStream("e64-4lut.p"));
		Placement p=plaats_parser.read();
		Readnetlist netlist_parser=new Readnetlist(new FileInputStream("e64-4lut.net"));
		Circuit c=netlist_parser.read(true);
		
		Placement IO_p=p.IOPlacement(c);
		
		System.out.println(p);
		
	}

}
