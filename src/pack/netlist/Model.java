package pack.netlist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import pack.util.ErrorLog;

public class Model {
	private String name;
	private ArrayList<String> inputPorts;
	private ArrayList<String> outputPorts;
	private HashMap<String,Integer> pinsOnPort;
	private String internals;
	private int occurences;
	
	//TODO make architecture independent
	private int ramSlices9;
	private int ramSlices144;

	public Model(String name, String archFile){
		this.name = name;
		this.inputPorts = new ArrayList<String>();
		this.outputPorts = new ArrayList<String>();
		this.pinsOnPort = new HashMap<String,Integer>();
		this.occurences = 0;
		
		if(this.name.contains("stratixiv_ram_block")){
			this.assign_stratixiv_ram_slices(archFile);
		}
	}
	public Model(Model model){
		this.name = model.get_name();
		this.inputPorts = new ArrayList<String>();
		for(String inputPort:model.get_input_ports()){
			this.inputPorts.add(inputPort);
		}
		this.outputPorts = new ArrayList<String>();
		for(String outputPort:model.get_output_ports()){
			this.outputPorts.add(outputPort);
		}
		this.pinsOnPort = new HashMap<String,Integer>();
		for(String port:model.pinsOnPort.keySet()){
			this.pinsOnPort.put(port, model.pins_on_port(port));
		}
		this.occurences = 0;
		
		this.ramSlices9 = model.get_stratixiv_ram_slices_9();
		this.ramSlices144 = model.get_stratixiv_ram_slices_144();
	}
	public Model(Model model, String newName){
		this.name = newName;
		this.inputPorts = new ArrayList<String>();
		for(String inputPort:model.get_input_ports()){
			this.inputPorts.add(inputPort);
		}
		this.outputPorts = new ArrayList<String>();
		for(String outputPort:model.get_output_ports()){
			this.outputPorts.add(outputPort);
		}
		this.pinsOnPort = new HashMap<String,Integer>();
		for(String port:model.pinsOnPort.keySet()){
			this.pinsOnPort.put(port, model.pins_on_port(port));
		}
		this.occurences = 0;
		
		if(newName.contains("M9K") && newName.contains("M144K")){
			ErrorLog.print("The name of the model should contain M9K or M144K, not both => " + newName);
		}
		if(newName.contains("M9K")){
			this.ramSlices9 = model.get_stratixiv_ram_slices_9();
			this.ramSlices144 = 0;
		}else if(newName.contains("M144K")){
			this.ramSlices9 = 0;
			this.ramSlices144 = model.get_stratixiv_ram_slices_144();
		}else{
			ErrorLog.print("Unexpexted name for new model: " + newName);
		}
	}
	public String get_name() {
		return name;
	}
	public void increment_occurences() {
		this.occurences++;
	}
	public void decrement_occurences() {
		this.occurences--;
	}
	public int get_occurences() {
		return this.occurences;
	}
	public boolean is_input(String port) {
		return this.inputPorts.contains(port);
	}
	public boolean is_output(String port) {
		return this.outputPorts.contains(port);
	}
	public ArrayList<String> get_input_ports() {
		return this.inputPorts;
	}
	public ArrayList<String> get_output_ports() {
		return this.outputPorts;
	}
	public String get_first_output_port(){
		return this.outputPorts.get(0);
	}
	public void add_input_port(String inputPort){
		if(this.inputPorts.contains(inputPort)){
			this.pinsOnPort.put(inputPort, this.pinsOnPort.get(inputPort)+1);
		}else{
			this.inputPorts.add(inputPort);
			this.pinsOnPort.put(inputPort, 1);
		}
	}
	public void add_output_port(String outputPort){
		if(this.outputPorts.contains(outputPort)){
			this.pinsOnPort.put(outputPort, this.pinsOnPort.get(outputPort)+1);
		}else{
			this.outputPorts.add(outputPort);
			this.pinsOnPort.put(outputPort, 1);
		}
	}
	public int pins_on_port(String port){
		return this.pinsOnPort.get(port);
	}
	public void add_to_internals(String line){
		if(internals == null) internals = line;
		else internals += "\n"+ line;
	}
	public String get_internals() {
		return internals;
	}
	public String to_blif_string(){
		String out =".model "+this.name+"\n";
		out += ".inputs";
		int no = 0;
		for(String input:this.inputPorts){
			if(this.pinsOnPort.get(input) > 1){
				for(int i=0;i<this.pinsOnPort.get(input);i++){
					if(no>9){
						no = 0;
						out += "\\\n";
					}
					out += " " + input + "[" + i + "]";
					no++;
				}
			}else{
				if(no>9){
					no = 0;
					out += "\\\n";
				}
				out += " " + input;
				no++;
			}
			
		}
		out += "\n";
		out += ".outputs";
		no = 0;
		for(String output:this.outputPorts){
			if(this.pinsOnPort.get(output) > 1){
				for(int i=0;i<this.pinsOnPort.get(output);i++){
					if(no>9){
						no = 0;
						out += "\\\n";
					}
					out += " " + output + "[" + i + "]";
					no++;
				}
			}else{
				if(no>9){
					no = 0;
					out += "\\\n";
				}
				out += " " + output;
				no++;
			}
			
		}
		out += "\n";
		if(this.internals == null){//TODO HACK
			this.internals = ".blackbox";
		}
		out += this.internals;
		out += "\n";
		out += ".end";
		out += "\n";
		return out;
	}
	private int parse(String line){
		String[] words = line.split(" ");
		for(String word:words){
			if(word.contains("num_pins")){
				word = word.replace(" ", "");
				word = word.replace("num_pins", "");
				word = word.replace("=", "");
				word = word.replace("\"", "");
				return Integer.parseInt(word);
			}
		}
		ErrorLog.print("No num_pins object found");
		return -1;
	}
	
	//RAM SLICES
	public int get_stratixiv_ram_slices_9(){
		return this.ramSlices9;
	}
	public int get_stratixiv_ram_slices_144(){
		return this.ramSlices144;
	}
	private void assign_stratixiv_ram_slices(String archFile){
		boolean M9K = false;
		boolean M144K = false;
		boolean readPort = false;
		
		boolean M9KFound = false;
		boolean M144KFound = false;
		
		int dataIn1 = 0;
		int dataIn2 = 0;
		int dataOut1 = 0; 
		int dataOut2 = 0;
		
		int num_pb = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(archFile));
		    String line = br.readLine();
		    while (line != null) {
		    	if(line.contains("<pb_type") && line.contains("name=\"M9K\"")){
					M9K = true;
					M144K = false;
					readPort = false;
					if(!M9KFound){
						M9KFound = true;
					}else{
						ErrorLog.print("Already an M9K RAM block found in architecture");
					}
				}
		    	else if(line.contains("<pb_type") && line.contains("name=\"M144K\"")){
					M9K = false;
					M144K = true;
					readPort = false;
					if(!M144KFound){
						M144KFound = true;
					}else{
						ErrorLog.print("Already an M144K RAM block found in architecture");
					}
				}
				if(M9K || M144K){
					if(line.contains("\".subckt " + this.name + "\"")){
						readPort = true;
						dataIn1 = 0;
						dataIn2 = 0;
						dataOut1 = 0;
						dataOut2 = 0;

						int start = line.indexOf("num_pb=");
						start = line.indexOf("\"", start+1);
						int stop = line.indexOf("\"", start+1);
						num_pb = Integer.parseInt(line.substring(start+1, stop));
					}
				}
				if(readPort){
					if(line.contains("port_class=\"data_in1\"")){
						dataIn1 = parse(line);
					}else if(line.contains("port_class=\"data_in2\"")){
						dataIn2 = parse(line);
					}else if(line.contains("port_class=\"data_out1\"")){
						dataOut1 = parse(line);
					}else if(line.contains("port_class=\"data_out2\"")){
						dataOut2 = parse(line);
					}
				}
				if(line.contains("</pb_type>") && readPort){
					int memorySlices = 0;
					if(dataIn1 > 0){
						memorySlices = dataIn1;
					}
					if(dataIn2 > 0){
						if(memorySlices == 0){
							memorySlices = dataIn2;
						}else if(dataIn2 != memorySlices){
							ErrorLog.print(this.name + "\n\tdata_in1\t" + dataIn1 + "\n\tdata_in2\t" + dataIn2 + "\n\tdata_out1\t" + dataOut1 + "\n\tdata_out2\t" + dataOut2);
						}
					}
					if(dataOut1 > 0){
						if(memorySlices == 0){
							memorySlices = dataOut1;
						}else if(dataOut1 != memorySlices){
							ErrorLog.print(this.name + "\n\tdata_in1\t" + dataIn1 + "\n\tdata_in2\t" + dataIn2 + "\n\tdata_out1\t" + dataOut1 + "\n\tdata_out2\t" + dataOut2);
						}
					}
					if(dataOut2 > 0){
						if(memorySlices == 0){
							memorySlices = dataOut2;
						}else if(dataOut2 != memorySlices){
							ErrorLog.print(this.name + "\n\tdata_in1\t" + dataIn1 + "\n\tdata_in2\t" + dataIn2 + "\n\tdata_out1\t" + dataOut1 + "\n\tdata_out2\t" + dataOut2);
						}
					}
					if(memorySlices == 0){
						memorySlices = num_pb;
					}
					
					if(M9K){
						this.ramSlices9 = memorySlices;
					}else if(M144K){
						this.ramSlices144 = memorySlices;
					}else{
						ErrorLog.print(this.name);
					}
					M9K = false;
					M144K = false;
					readPort = false;
				}
		        line = br.readLine();
		    }
		    br.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		if(!M9KFound){
			ErrorLog.print("No M9K RAM block found in architecture");
		}
		if(!M144KFound){
			ErrorLog.print("No M144K RAM block found in architecture");
		}
	}
}