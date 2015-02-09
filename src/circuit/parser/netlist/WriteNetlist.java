package circuit.parser.netlist;

import java.io.PrintStream;
import java.util.Iterator;

import circuit.Block;
import circuit.BlockType;
import circuit.Circuit;
import circuit.Clb;
import circuit.Input;
import circuit.Net;
import circuit.NetInst;
import circuit.Output;
import circuit.Pin;
import circuit.PinType;
import circuit.Pattern;

public class WriteNetlist {
	Circuit circuit;
	
	public WriteNetlist(Circuit c) {
		circuit = c;
	}

	public void write(PrintStream stream) {
		//Write .global
		for (Tcon tcon: circuit.globals.values()) {
			stream.println(".global " + tcon.name);
			stream.println();
		}
		
		//Write outputs
		for (Output out:circuit.outputs.values()) {
			printOutput(stream, out);
		}
				
		//Writing the CLB's
		for (Iterator<Clb> i=circuit.clbs.values().iterator();i.hasNext();) {
			Clb clb=i.next();
			printClb(stream, clb);
		}
		
		//Write inputs
		for (Iterator<Input> i=circuit.inputs.values().iterator();i.hasNext();){
			Input in=i.next();
			printInput(stream, in);
		}
		
		//Write tcons
		for (Tcon tcon: circuit.tcons.values()) {
			printTcon(stream, tcon);
		}
		
		for (Tcon tcon: circuit.globals.values()) {
			printTcon(stream, tcon);
		}
	}

	private void printOutput(PrintStream stream, Output out) {
		stream.println(".output " + out.name);
		stream.println("pinlist: "+ netName(out.input));

		stream.println();
	}

	private void printClb(PrintStream stream, Clb clb) {
		String subb= new String();
		subb+="subblock: test";
		
		stream.println(".clb "+clb.name);
		stream.print("pinlist:");
		int k=0;
		for(Pin j:clb.input) {
			
			if(j.con!=null) {
				stream.print(" "+netName(j));
				subb+=" "+k;
			}
			else {
				stream.print(" open");
				subb+=" open";
			}
			k++;
			    
		}
		for(Pin j:clb.output) {
			if(j.con!=null) {
				stream.print(" "+netName(j));
				subb+=" "+k;
			}
			else {
				stream.print(" open");
				subb+=" open";
			}
			k++;
		}
		if(clb.clock.con!=null) {
			stream.println(" "+netName(clb.clock));
			subb+= " " + k;
		}
		else {
			stream.println(" open");
			subb+=" open";
		}
		stream.println(subb+" #dummy");
		stream.println();
	}

	private String netName( Pin pin) {
		Block block;
		if (pin.type==PinType.SOURCE) 
			block=pin.con.sink.owner;
		else
			block=pin.con.source.owner;
		
		if (block.type==BlockType.TCON) {
			Tcon tcon=(Tcon)block; 
			if (tcon.pattern.size()!=1)
				return pin.con.name;
			else	
				return tcon.name;
		} else {
			return pin.con.name;
		}
	}

	private void printInput(PrintStream stream, Input in) {
		stream.println(".input " + in.name);
		stream.println("pinlist: " + netName(in.output));
		stream.println();
	}

}
