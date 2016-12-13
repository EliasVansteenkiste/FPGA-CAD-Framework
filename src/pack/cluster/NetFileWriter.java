package pack.cluster;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import pack.netlist.B;
import pack.netlist.N;
import pack.netlist.Netlist;
import pack.netlist.P;
import pack.util.Output;
import pack.util.Timing;
import pack.util.Util;

public class NetFileWriter {
	private Netlist root;
	
	private ArrayList<LogicBlock> logicBlocks;
	
	private ArrayList<N> netlistInputs;
	private ArrayList<N> netlistOutputs;

	private BufferedWriter writer;
	
	private int tabs;
	private ArrayList<String> names;
	
	private Timing t;

	private int blockC;
	
	public NetFileWriter(ArrayList<LogicBlock> logicBlocks, Netlist root){
		this.root = root;
		
		this.logicBlocks = logicBlocks;
		
		this.t = new Timing();
		this.t.start();
		
		this.blockC = 0;
		
		this.tabs = 0;
		this.names = new ArrayList<String>();
	}
	public void netlistInputs(){
		this.netlistInputs = new ArrayList<N>();
		for(B b:this.root.get_blocks()){
			for(N n:b.get_input_nets()){
				if(!n.has_source() && n.has_terminals()){
					if(!this.netlistInputs.contains(n)){
						this.netlistInputs.add(n);
					}
				}
			}
		}
		Collections.sort(this.netlistInputs, N.NetFanoutComparator);
	}
	public void netlistOutputs(){
		this.netlistOutputs = new ArrayList<N>();
		for(B b:this.root.get_blocks()){
			for(N n:b.get_output_nets()){
				if(n.has_terminals()){
					if(!this.netlistOutputs.contains(n)){
						this.netlistOutputs.add(n);
					}
				}
			}
		}
		if(this.root.has_floating_blocks()){
			for(B b:this.root.get_floating_blocks()){
				for(N n:b.get_output_nets()){
					if(n.has_terminals()){
						if(!this.netlistOutputs.contains(n)){
							this.netlistOutputs.add(n);
						}
					}
				}
			}
		}
	}
	public void makeNetFile(){
		makeNetFile(this.root);
	}
	public void printHeaderToNetFile(){
		this.writeBlockToNetFile(Util.run_folder() + this.root.get_blif() + ".net", "FPGA_packed_netlist[0]", null);
		//for(String clock:this.root.get_clocks())add(clock);
		for(N input:this.netlistInputs)add(input);
		this.writeInputsToNetFile(null, true, true);
		for(N output:this.netlistOutputs){
			for(P terminalOutputPin:output.get_terminal_pins()){
				if(terminalOutputPin.get_terminal().is_output_type()){
					add("out:" + terminalOutputPin.get_terminal().toString());
				}
			}
		}
		writeOutputsToNetFile(null, true, true);
		for(String clock:this.root.get_clocks()) add(clock);
		writeClocksToNetFile(null);
	}

	public void printLogicBlocksToNetFile(){
		for(LogicBlock lb:this.logicBlocks){
			lb.setInstanceNumber(this.blockC++);
			this.writeToNetFile(lb.toNetString(1));
		}
	}
	public void finishNetFile(){
		closeNetFile();

		this.t.end();
		Output.println("\tNetfile writer took " + this.t.toString() + " seconds");
		Output.newLine();
	}
	
	//// WRITERS ////
	private void writeBlockToNetFile(String name, String instance, String mode){
		writeToNetFile(Util.tabs(this.tabs));
		writeToNetFile("<block");
		if(name != null){
			writeToNetFile(" name=\"");
			writeToNetFile(name);
			writeToNetFile("\"");
		}
		if(instance != null){
			writeToNetFile(" instance=\"");
			writeToNetFile(instance);
			writeToNetFile("\"");
		}
		if(mode != null){
			writeToNetFile(" mode=\"");
			writeToNetFile(mode);
			writeToNetFile("\"");
		}
		writeToNetFile(">\n");
		
		this.tabs += 1;
	}
	private void writeInputsToNetFile(String port_name, boolean start, boolean end){
		if(start){
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("<inputs>");
			writeToNetFile("\n");
			this.tabs += 1;
		}
		if(port_name!=null){
			writeToNetFile(Util.tabs(tabs));
			writeToNetFile("<port name=\"");
			writeToNetFile(port_name);
			writeToNetFile("\">");
			writeToNetFile("\n");
			this.tabs += 1;
		}
		for(String input:this.names){
			writeToNetFile(Util.tabs(tabs));
			writeToNetFile(input);
			writeToNetFile("\n");
		}
		this.names.clear();
		if(port_name!=null){
			this.tabs -= 1;	
			writeToNetFile(Util.tabs(tabs));
			writeToNetFile("</port>");
			writeToNetFile("\n");
		}
		if(end){
			this.tabs -= 1;
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("</inputs>");
			writeToNetFile("\n");
		}
	}
	private void writeOutputsToNetFile(String port_name, boolean start, boolean end){
		if(start){
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("<outputs>");
			writeToNetFile("\n");
			this.tabs += 1;
		}
		if(port_name!=null){
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("<port name=\"");
			writeToNetFile(port_name);
			writeToNetFile("\">");
			writeToNetFile("\n");
			this.tabs += 1;
		}
		for(String output:this.names){
			writeToNetFile(Util.tabs(tabs));
			writeToNetFile(output);
			writeToNetFile("\n");
		}
		this.names.clear();
		if(port_name!=null){
			this.tabs -= 1;
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("</port>");
			writeToNetFile("\n");
		}
		if(end){
			this.tabs -= 1;
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("</outputs>");
			writeToNetFile("\n");
		}
	}
	private void writeClocksToNetFile(String port_name){
		writeToNetFile(Util.tabs(this.tabs));
		writeToNetFile("<clocks>");
		writeToNetFile("\n");
		this.tabs += 1;
		if(port_name!=null){
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("<port name=\"");
			writeToNetFile(port_name);
			writeToNetFile("\">");
			writeToNetFile("\n");
			this.tabs += 1;
		}
		for(String clock:this.names){
			writeToNetFile(Util.tabs(tabs));
			writeToNetFile(clock);
			writeToNetFile("\n");
		}
		this.names.clear();
		this.tabs -= 1;
		if(port_name!=null){
			writeToNetFile(Util.tabs(this.tabs));
			writeToNetFile("</port>");
			writeToNetFile("\n");
			this.tabs -= 1;
		}
		writeToNetFile(Util.tabs(this.tabs));
		writeToNetFile("</clocks>");
		writeToNetFile("\n");
	}
	
	//// WRITER ////
	private void makeNetFile(Netlist root){
		try {
			FileWriter w = new FileWriter(Util.run_folder() + root.get_blif() + ".net");
			this.writer = new BufferedWriter(w);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void writeToNetFile(String line){
		//System.out.print(line);
		try {
			this.writer.write(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void closeNetFile(){
		try {
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//// NAMES ////
	public void add(N n){
		this.names.add(n.toString());
	}
	public void add(String s){
		this.names.add(s);
	}
}