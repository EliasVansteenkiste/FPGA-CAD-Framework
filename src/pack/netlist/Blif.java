package pack.netlist;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class Blif {
	private String blif; 
	private String folder;
	private int num;
	private int simulationID;

	private HashSet<String> inputs;
	private HashSet<String> outputs;

	private ArrayList<String> latches;
	private ArrayList<String> gates;

	private ArrayList<String> pll;
	private ArrayList<String> subcircuits;

	private HashSet<Model> models;

	private int dummy_counter;
	
	public Blif(String folder, String blif, int num, int simulationID){
		this.folder = folder;
		this.blif = blif;
		this.num = num;
		this.simulationID = simulationID;
		
		this.inputs = new HashSet<String>();
		this.outputs = new HashSet<String>();
		
		this.latches = new ArrayList<String>();
		this.gates = new ArrayList<String>();
		
		this.pll = new ArrayList<String>();
		this.subcircuits = new ArrayList<String>();
		this.models = new HashSet<Model>();
		
		this.dummy_counter = 0;
	}
	public void add_input(String s){
		this.inputs.add(s);
	}
	public void add_output(String s){
		this.outputs.add(s);
	}
	public void add_latch(String s){
		this.latches.add(s);
	}
	public void add_gate(String s){
		this.gates.add(s);
	}
	public void add_buffer(String net, String terminal){
		String buffer = ".names" + " " + net + " " + terminal + "\n" + "1 1";
		this.gates.add(buffer);
	}
	public void add_subcircuit(String name, String subcircuit){
		String s = "# Subckt " +  this.subcircuits.size() + ": " + name + "\n" + subcircuit;
		this.subcircuits.add(s);
	}
	public void add_pll(String name, String subcircuit){
		String s = "# PLL " +  this.pll.size() + ": " + name + "\n" + subcircuit;
		this.pll.add(s);
	}
	public void add_model(Model model){
		this.models.add(model);
	}
	public String get_dummy_net(){
		return "dummy" + dummy_counter++;
	}
	public void write(){
		try {
			FileWriter writer = null;
			if(num < 0){
				writer = new FileWriter(this.folder + blif + ".blif");
			}else{
				writer = new FileWriter(this.folder + this.blif + "_" + this.simulationID + "_" + this.num + ".blif");
			}
	        BufferedWriter bufferedWriter = new BufferedWriter(writer);
	        bufferedWriter.write(".model " + this.blif);
	        bufferedWriter.newLine();
			
			int no;
			
			//INPUTS
			no = 0;
			bufferedWriter.write(".inputs");
			ArrayList<String> inputArray = new ArrayList<String>(this.inputs);
			while(!inputArray.isEmpty()){
				int pos = (int)Math.floor(Math.random()*inputArray.size());
				String input = inputArray.remove(pos);
				if(no > 50){
					bufferedWriter.write(" \\");
					bufferedWriter.newLine();
					no = 0;
				}
				bufferedWriter.write(" " + input);
				no += input.length();
			}
			bufferedWriter.newLine();
			bufferedWriter.newLine();
			
			//OUTPUTS
			no = 0;
			bufferedWriter.write(".outputs");
			ArrayList<String> outputArray = new ArrayList<String>(this.outputs);
			while(!outputArray.isEmpty()){
				int pos = (int)Math.floor(Math.random()*outputArray.size());
				String output = outputArray.remove(pos);
				if(no > 50){
					bufferedWriter.write(" \\");
					bufferedWriter.newLine();
					no = 0;
				}
				bufferedWriter.write(" " + output);
				no += output.length();
			}
			bufferedWriter.newLine();
			bufferedWriter.newLine();

			for(String pll:this.pll){
				bufferedWriter.write(pll);
				bufferedWriter.newLine();
				bufferedWriter.newLine();
			}
			bufferedWriter.newLine();

			for(String latch:this.latches){
				bufferedWriter.write(latch);
				bufferedWriter.newLine();
			}
			bufferedWriter.newLine();

			for(String gate:this.gates){
				bufferedWriter.write(gate);
				bufferedWriter.newLine();
			}
			bufferedWriter.newLine();

			for(String subcircuit:this.subcircuits){
				bufferedWriter.write(subcircuit);
				bufferedWriter.newLine();
				bufferedWriter.newLine();
			}
			bufferedWriter.newLine();
			
			bufferedWriter.write(".end");
			bufferedWriter.newLine();
			bufferedWriter.newLine();
			
			bufferedWriter.write("# Models");
			bufferedWriter.newLine();
			bufferedWriter.newLine();

			for(Model model:this.models){
				bufferedWriter.write(model.to_blif_string());
				bufferedWriter.newLine();
			}
			bufferedWriter.newLine();

			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}