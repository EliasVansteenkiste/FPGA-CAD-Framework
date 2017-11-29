package pack.architecture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pack.util.ErrorLog;
import pack.util.Output;

public class Element {
	private String type;
	private String name;
	
	private HashMap<String,Port> inputPorts;
	private HashMap<String,Port> outputPorts;
	private Port clockPort;
	private ArrayList<String> portOrder;
	
	private HashMap<String,String> properties;
	private ArrayList<String> interconnectLines;
	
	private Element parent;
	private ArrayList<Element> children;
	
	//Delay information
	private HashMap<String,Integer> delayMatrix;//delay in ps
	private HashMap<String,Integer> setupTime;//delay in ps
	private HashMap<String,Integer> clockToOutput;//delay in ps
	
	public Element(Line line, String type){
		this.type = type;
		this.name = line.get_value("name");
		
		this.inputPorts = new HashMap<String,Port>();
		this.outputPorts = new HashMap<String,Port>();
		this.clockPort = null;
		this.portOrder = new ArrayList<String>();

		this.properties = new HashMap<String, String>();
		for(String property:line.get_properties()){
			if(!property.equals("name")){
				this.properties.put(property, line.get_value(property));
			}
		}
		this.interconnectLines = new ArrayList<String>();
		this.children = new ArrayList<Element>();
		
		this.delayMatrix = new HashMap<String,Integer>();
		this.setupTime = new HashMap<String,Integer>();
		this.clockToOutput = new HashMap<String,Integer>();
	}
	
	//DELAY
	public void add_delay(String inputPortName, String outputPortName, int delay){
		if(inputPortName.contains(this.name) && (inputPortName.indexOf(".") == inputPortName.lastIndexOf("."))){
			inputPortName = inputPortName.replace(this.name, "").replace(".", "");
		}else{
			ErrorLog.print("Unexpected name for input port: " + inputPortName + " | name " + this.name);
		}
		if(outputPortName.contains(this.name) && (outputPortName.indexOf(".") == outputPortName.lastIndexOf("."))){
			outputPortName = outputPortName.replace(this.name, "").replace(".", "");
		}else{
			ErrorLog.print("Unexpected name for output port: " + outputPortName + " | name " + this.name);
		}
		if(!this.inputPorts.containsKey(inputPortName)){
			ErrorLog.print("This block " + this.name + " does not have input port " + inputPortName);
		}
		if(!this.outputPorts.containsKey(outputPortName)){
			ErrorLog.print("This block " + this.name + " does not have output port " + outputPortName);
		}
		this.delayMatrix.put(this.get_key(inputPortName, outputPortName), delay);
	}
	public void add_setup(String inputPortName, String clockName, int delay){
		if(!this.has_clock()){
			ErrorLog.print("Only sequential blocks have a setup time");
		}
		if(!this.clockPort.get_name().equals(clockName)){
			ErrorLog.print("Wrong name of clock port");
		}
		if(inputPortName.contains(this.name) && (inputPortName.indexOf(".") == inputPortName.lastIndexOf("."))){
			inputPortName = inputPortName.replace(this.name, "").replace(".", "");
		}else{
			ErrorLog.print("Unexpected name for input port: " + inputPortName + " | name " + this.name);
		}
		if(!this.inputPorts.containsKey(inputPortName)){
			ErrorLog.print("This block " + this.name + " does not have input port " + inputPortName);
		}
		this.setupTime.put(inputPortName, delay);
	}
	public void add_clock_to_output(String clock, String outputPortName, int delay){
		if(!this.has_clock()){
			ErrorLog.print("Only sequential blocks have a setup time");
		}
		if(!this.clockPort.get_name().equals(clock)){
			ErrorLog.print("Wrong name of clock port");
		}
		if(outputPortName.contains(this.name) && (outputPortName.indexOf(".") == outputPortName.lastIndexOf("."))){
			outputPortName = outputPortName.replace(this.name, "").replace(".", "");
		}else{
			ErrorLog.print("Unexpected name for input port: " + outputPortName + " | name " + this.name);
		}
		if(!this.outputPorts.containsKey(outputPortName)){
			ErrorLog.print("This block " + this.name + " does not have output port " + outputPortName);
		}
		this.clockToOutput.put(outputPortName, delay);
	}
	public int get_delay(String input, String output){
		String key = this.get_key(input, output);
		if(this.delayMatrix.containsKey(key)){
			return this.delayMatrix.get(key);
		}else{
			Output.println("No valid connection between " + input + " and " + output + " found");
			return 0;
		}
	}
	public int get_setup(String input){
		if(this.setupTime.containsKey(input)){
			return this.setupTime.get(input);
		}else{
			Output.println("No setup time for port " + input + " found");
			return 0;
		}
	}
	public int get_clock_to_output(String output){
		if(this.clockToOutput.containsKey(output)){
			return this.clockToOutput.get(output);
		}else{
			Output.println("No clock to output time for port " + output + " found");
			return 0;
		}
	}
	private String get_key(String input, String output){
		return input + "@@@" + output;
	}
	public boolean valid_connection(String input, String output){
		String key = this.get_key(input, output);
		return this.delayMatrix.containsKey(key);
	}
	public boolean has_setup_delay(String input){
		return this.setupTime.containsKey(input);
	}
	public boolean has_clock_to_output_delay(String output){
		return this.clockToOutput.containsKey(output);
	}
	//SETTERS
	public void add_input(Port inputPort){
		if(this.type.equals("pb_type")){
			this.inputPorts.put(inputPort.get_name(), inputPort);
		}else{
			ErrorLog.print("Type " + this.type + " should not have input ports");
		}
		this.portOrder.add(inputPort.get_name());
	}
	public void add_output(Port outputPort){
		if(this.type.equals("pb_type")){
			this.outputPorts.put(outputPort.get_name(), outputPort);
		}else{
			ErrorLog.print("Type " + this.type + " should not have output ports");
		}
		this.portOrder.add(outputPort.get_name());
	}
	public void set_clock(Port clockPort){
		if(this.type.equals("pb_type")){
			if(this.clockPort == null){
				this.clockPort = clockPort;
			}else{
				ErrorLog.print("This block " + this.name + " already has a clock port");
			}
		}else{
			ErrorLog.print("Type " + this.type + " should not have clock ports");
		}
		this.portOrder.add(clockPort.get_name());
	}
	public void add_interconnect_line(String line){
		if(this.type.equals("pb_type") && this.has_parent()){
			ErrorLog.print("pb_type " + this.name + " has interconnect lines => " + line);
		}
		this.interconnectLines.add(line);
	}
	public void set_parent(Element parent){
		this.parent = parent;
	}
	public void add_child(Element child){
		if(!this.children.contains(child)){
			this.children.add(child);
		}else{
			ErrorLog.print("Child already added");
		}
	}
	
	//GETTERS
	public String get_name(){
		return this.name;
	}
	public String get_type(){
		return this.type;
	}
	public Element get_parent(){
		return this.parent;
	}
	public String get_value(String property){
		if(this.properties.containsKey(property)){
			return this.properties.get(property);
		}else{
			ErrorLog.print("Poperty " + property + " not found in block " + this.get_name());
			return null;
		}
	}
	public boolean has_clock_port(String portName){
		if(this.has_clock()){
			return this.clockPort.get_name().equals(portName);
		}else{
			return false;
		}
	}
	public boolean has_input_port(String portName){
		return this.inputPorts.containsKey(portName);
	}
	public boolean has_output_port(String portName){
		return this.outputPorts.containsKey(portName);
	}
	public Port get_port(String portName){
		boolean isInput = false;
		boolean isOutput = false;
		boolean isClock = false;
		if(this.inputPorts.containsKey(portName)) isInput = true;
		if(this.outputPorts.containsKey(portName)) isOutput = true;
		if(this.has_clock()){
			if(this.clockPort.get_name().equals(portName)) isClock = true;
		}

		if(isInput && isOutput){
			ErrorLog.print("Port " + portName + " is input and output port of block " + this.get_name());	
			return null;
		}else if(isInput && isClock){
			ErrorLog.print("Port " + portName + " is input and clock port of block " + this.get_name());	
			return null;
		}else if(isClock && isOutput){
			ErrorLog.print("Port " + portName + " is clock and output port of block " + this.get_name());	
			return null;
		}else if(!isInput && !isOutput && !isClock){
			for(String inputPort:this.inputPorts.keySet()){
				Output.println("InputPort: \"" + inputPort + "\"");
			}
			for(String outputPort:this.outputPorts.keySet()){
				Output.println("OutputPort: \"" + outputPort + "\"");
			}
			if(this.has_clock()){
				Output.println("ClockPort: \"" + this.clockPort.get_name() + "\"");
			}
			ErrorLog.print("Port \"" + portName + "\" is no port of block " + this.get_name() + " isInput: " + isInput + " isOutput: " + isOutput);	
			return null;
		}else if(isInput){
			return this.inputPorts.get(portName);
		}else if(isOutput){
			return this.outputPorts.get(portName);
		}else if(isClock){
			return this.clockPort;
		}else{
			ErrorLog.print("Unexpected sitation");
			return null;
		}
	}
	public Collection<Port> get_input_ports(){
		return this.inputPorts.values();
	}
	public Collection<Port> get_input_and_clock_ports(){
		Collection<Port> res = new HashSet<Port>();
		res.addAll(this.inputPorts.values());
		if(this.has_clock()){
			res.add(this.clockPort);
		}
		return res;
	}
	public Collection<Port> get_output_ports(){
		return this.outputPorts.values();
	}
	public Port get_clock_port(){
		if(this.has_clock()){
			return this.clockPort;
		}else{
			ErrorLog.print("This block has no clock port");
			return null;
		}
	}
	
	//CHILDREN
	public ArrayList<Element> get_children(){
		return this.children;
	}
	public void remove_child(Element child){
		if(this.children.contains(child)){
			this.children.remove(child);
		}else{
			ErrorLog.print("This block " + this.name + " does not contain child " + child.get_name());
		}
	}
	
	//HAS
	public boolean has_parent(){
		return this.parent != null;
	}
	public boolean has_children(){
		return !this.children.isEmpty();
	}
	public boolean has_clock(){
		return this.clockPort != null;
	}
	
	//BLIF MODEL
	public boolean has_blif_model(){
		return this.properties.containsKey("blif_model");
	}
	public String get_blif_model(){
		if(this.has_blif_model()){
			String value = this.get_value("blif_model");
			return value.replace(".subckt ", "");
		}else{
			ErrorLog.print("This element has no blif model");
			return null;
		}
	}
	public Set<String> get_blif_models(){
		Set<String> blifModels = new HashSet<String>();
		if(this.has_children()){
			for(Element child:this.children){
				blifModels.addAll(child.get_blif_models());
			}
		}else if(this.has_blif_model()){
			blifModels.add(this.get_blif_model());
		}
		return blifModels;
	}
	
	//ANALYZE
	public boolean remove_block(Set<String> usedModels, Set<String> removableModels){
		if(this.has_children()){
			boolean removeBlock = true;
			for(Element child:this.children){
				if(!child.remove_block(usedModels, removableModels)){
					removeBlock = false;
				}
			}
			return removeBlock;
		}else if(this.has_blif_model()){
			String model = this.get_blif_model();
			if(!usedModels.contains(model) && removableModels.contains(model)){
				return true;
			}else{
				return false;
			}
		}else{
			return true;
		}
	}
	
	//PRINT
	public ArrayList<String> toStringList(){
		ArrayList<String> res = new ArrayList<String>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		sb.append(this.type);
		sb.append(" name=\"");
		sb.append(this.name);
		sb.append("\"");
		for(String property:this.properties.keySet()){
			sb.append(" ");
			sb.append(property + "=\"" + this.properties.get(property) + "\"");
		}
		sb.append(">");
		res.add(sb.toString());
		
		for(String port:this.portOrder){
			if(this.inputPorts.containsKey(port)){
				res.add(this.inputPorts.get(port).toString());
			}else if(this.outputPorts.containsKey(port)){
				res.add(this.outputPorts.get(port).toString());
			}else if(this.clockPort.get_name().equals(port)){
				res.add(this.clockPort.toString());
			}else{
				ErrorLog.print("Port " + port + " not found in " + this.type + " " + this.name);
			}
		}
		for(String inputOutputKey:this.delayMatrix.keySet()){
			res.add(this.delayConstant(inputOutputKey));
		}
		for(String inputPort:this.setupTime.keySet()){
			res.add(this.setupTime(inputPort));
		}
		for(String outputPort:this.clockToOutput.keySet()){
			res.add(this.clockToOutput(outputPort));
		}
		
		if(this.requiresDummyModel()){
			res.addAll(this.getDummyBlock());
		}
		
		for(Element child:this.children){
			res.addAll(child.toStringList());
		}
	
		for(String interconnectLine:this.interconnectLines){
			res.add(interconnectLine);
		}
		
		res.add("</" + this.type + ">");
		return res;
	}
	private String delayConstant(String inputOutputKey){
		String input = inputOutputKey.split("@@@")[0];
		String output = inputOutputKey.split("@@@")[1];
		
		StringBuilder sb = new StringBuilder();
		sb.append("<delay_constant in_port=\"");
		sb.append(this.name);
		sb.append(".");
		sb.append(input);
		sb.append("\" out_port=\"");
		sb.append(this.name);
		sb.append(".");
		sb.append(output);
		sb.append("\" max=\"");
		String value = "" + this.delayMatrix.get(inputOutputKey).doubleValue()*Math.pow(10, -12) + "";
		if(value.contains("999")){
			String repl = "999";
			while(value.contains(repl + "9")){
				repl += "9";
			}
			for(int i = 0; i< 9; i++){
				if(value.contains(i + repl)){
					if(("" + (i+1) + "").length() != 1){
						ErrorLog.print("Problem");
					}
					value = value.replace("" + i + "" + repl, "" + (i+1) + "");
				}
			}
		}
		sb.append(value);
		sb.append("\"/>");
		return sb.toString();
	}
	private String setupTime(String inputPort){
		StringBuilder sb = new StringBuilder();
		sb.append("<T_setup port=\"");
		sb.append(this.name);
		sb.append(".");
		sb.append(inputPort);
		sb.append("\" clock=\"");
		sb.append(this.clockPort.get_name());
		sb.append("\" value=\"");
		String value = "" + this.setupTime.get(inputPort).doubleValue()*Math.pow(10, -12) + "";
		if(value.contains("999")){
			String repl = "999";
			while(value.contains(repl + "9")){
				repl += "9";
			}
			for(int i = 0; i< 9; i++){
				if(value.contains(i + repl)){
					if(("" + (i+1) + "").length() != 1){
						ErrorLog.print("Problem");
					}
					value = value.replace("" + i + "" + repl, "" + (i+1) + "");
				}
			}
		}
		sb.append(value);
		sb.append("\"/>");
		return sb.toString();
	}
	private String clockToOutput(String outputPort){
		StringBuilder sb = new StringBuilder();
		sb.append("<T_clock_to_Q port=\"");
		sb.append(this.name);
		sb.append(".");
		sb.append(outputPort);
		sb.append("\" clock=\"");
		sb.append(this.clockPort.get_name());
		sb.append("\" max=\"");
		String value = "" + this.clockToOutput.get(outputPort).doubleValue()*Math.pow(10, -12) + "";
		if(value.contains("999")){
			String repl = "999";
			while(value.contains(repl + "9")){
				repl += "9";
			}
			for(int i = 0; i< 9; i++){
				if(value.contains(i + repl)){
					if(("" + (i+1) + "").length() != 1){
						ErrorLog.print("Problem");
					}
					value = value.replace("" + i + "" + repl, "" + (i+1) + "");
				}
			}
		}
		sb.append(value);
		sb.append("\"/>");
		return sb.toString();
	}
	
	
	//PACK PATTERNS
	public void remove_pack_patterns(){
		ArrayList<String> newInterconnectLines = new ArrayList<String>();
		for(int i=0;i<this.interconnectLines.size();i++){
			String line = this.interconnectLines.get(i);
			if(line.contains("<direct")){
				if(this.interconnectLines.get(i+1).contains("<pack_pattern")){
					int j = i+1;
					while(!this.interconnectLines.get(j).contains("</direct>")){
						j += 1;
					}
					newInterconnectLines.add(line.replace(">", "/>"));
					i = j;
				}else{
					newInterconnectLines.add(line);
				}
			}else{
				newInterconnectLines.add(line);
			}
		}
		this.interconnectLines = new ArrayList<String>();
		for(String line:newInterconnectLines){
			this.interconnectLines.add(line);
		}
	}
	public void set_pack_pattern(ArrayList<String[]> packPatterns){
		String macMultType = null;
		String macOutType = null;
		String macOutInput = null;
		String macOutOutput = null;
		Boolean halfDSP = false;
		if(this.name.equals("mac_mult_reg")){
			macMultType = "reg";
		}else if(this.name.equals("mac_mult_comb")){
			macMultType = "comb";
		}else if(this.name.equals("half_DSP_normal")){
			halfDSP = true;
		}else{
			macOutType = this.get_name().split("\\.")[0];
			macOutInput = this.get_name().split("\\.")[1].replace("input_type{", "").replace("}", "");
			macOutOutput = this.get_name().split("\\.")[2].replace("output_type{", "").replace("}", "");
		}
		ArrayList<String> newInterconnectLines = new ArrayList<String>();
		if(macMultType == null && macOutType == null && !halfDSP){
			ErrorLog.print("No type found");
		}else if(macMultType != null){
			for(int i=0;i<this.interconnectLines.size();i++){
				String line = this.interconnectLines.get(i);
				if(line.contains("<direct")){
					Line l = new Line(line);
					if(l.get_value("name").equals("dataout_out")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[0].equals(macMultType)){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + this.name + ".dataout[0]" + "\" out_port=\""  + "mac_mult.dataout[0]" + "\"/>");
							}
						}
						newInterconnectLines.add("</direct>");
					}else{
						newInterconnectLines.add(line);
					}
				}else{
					newInterconnectLines.add(line);
				}
			}
		}else if(macOutType != null){
			for(int i=0;i<this.interconnectLines.size();i++){
				String line = this.interconnectLines.get(i);
				if(line.contains("<direct")){
					Line l = new Line(line);
					if(l.get_value("name").equals("dataa_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[1].equals(macOutType) && packPattern[2].equals(macOutInput) && packPattern[3].equals(macOutOutput)){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_out.data_in[0]" + "\" out_port=\""  + "mac_out_" + macOutType + ".dataa[0]" + "\"/>");
								if(!packPattern[4].contains("0")){
									packPattern[4] += "0";
								}
							}
						}
						newInterconnectLines.add("</direct>");
					}else if(l.get_value("name").equals("datab_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[1].equals(macOutType) && packPattern[2].equals(macOutInput) && packPattern[3].equals(macOutOutput)){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_out.data_in[36]" + "\" out_port=\""  + "mac_out_" + macOutType + ".datab[0]" + "\"/>");
								if(!packPattern[4].contains("1")){
									packPattern[4] += "1";
								}
							}
						}
						newInterconnectLines.add("</direct>");
					}else if(l.get_value("name").equals("datac_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[1].equals(macOutType) && packPattern[2].equals(macOutInput) && packPattern[3].equals(macOutOutput)){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_out.data_in[72]" + "\" out_port=\""  + "mac_out_" + macOutType + ".datac[0]" + "\"/>");
								if(!packPattern[4].contains("2")){
									packPattern[4] += "2";
								}
							}
						}
						newInterconnectLines.add("</direct>");
					}else if(l.get_value("name").equals("datad_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[1].equals(macOutType) && packPattern[2].equals(macOutInput) && packPattern[3].equals(macOutOutput)){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_out.data_in[108]" + "\" out_port=\""  + "mac_out_" + macOutType + ".datad[0]" + "\"/>");
								if(!packPattern[4].contains("3")){
									packPattern[4] += "3";
								}
							}
						}
						newInterconnectLines.add("</direct>");
					}else{
						newInterconnectLines.add(line);
					}
				}else{
					newInterconnectLines.add(line);
				}
			}
		}else if(halfDSP){
			for(int i=0;i<this.interconnectLines.size();i++){
				String line = this.interconnectLines.get(i);
				if(line.contains("<direct")){
					Line l = new Line(line);
					if(l.get_value("name").equals("data_mac_mult0_out_to_mac_output_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[4].contains("0")){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_mult[0].dataout[0]" + "\" out_port=\""  + "mac_out.data_in[0]" + "\"/>");
	
							}
						}
						newInterconnectLines.add("</direct>");
					}else if(l.get_value("name").equals("data_mac_mult1_out_to_mac_output_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[4].contains("1")){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_mult[1].dataout[0]" + "\" out_port=\""  + "mac_out.data_in[36]" + "\"/>");

							}
						}
						newInterconnectLines.add("</direct>");
					}else if(l.get_value("name").equals("data_mac_mult2_out_to_mac_output_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[4].contains("2")){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_mult[2].dataout[0]" + "\" out_port=\""  + "mac_out.data_in[72]" + "\"/>");

							}
						}
						newInterconnectLines.add("</direct>");
					}else if(l.get_value("name").equals("data_mac_mult3_out_to_mac_output_in")){
						newInterconnectLines.add(line.replace("/>",">"));
						for(String[] packPattern:packPatterns){
							if(packPattern[4].contains("3")){
								newInterconnectLines.add("<pack_pattern name=\"" + pack_pattern_to_string(packPattern) + "\" in_port=\"" + "mac_mult[3].dataout[0]" + "\" out_port=\""  + "mac_out.data_in[108]" + "\"/>");

							}
						}
						newInterconnectLines.add("</direct>");
					}else{
						newInterconnectLines.add(line);
					}
				}else{
					newInterconnectLines.add(line);
				}
			}
		}else{
			ErrorLog.print("Unexpected situation");
		}
		
		this.interconnectLines = new ArrayList<String>();
		for(String line:newInterconnectLines){
			this.interconnectLines.add(line);
		}
	}
	public static String pack_pattern_to_string(String[] packPattern){
		if(packPattern.length != 5){
			ErrorLog.print("The lenght of the pack pattern should be 5 instead of " + packPattern.length);
		}
		return "mac_mult_" + packPattern[0] + "_to_mac_out_" + packPattern[1] + "_" + packPattern[2] + "_" + packPattern[3];
	}
	
	//BLIF MODEL NAMES
	public void modify_blif_model_names(String append){
		if(this.has_blif_model()){
			if(this.properties.get("blif_model").contains("width")){
				this.properties.put("blif_model", this.properties.get("blif_model") + append);
			}
		}
		for(Element child:this.get_children()){
			child.modify_blif_model_names(append);
		}
	}
	
	//DUMMY BLOCKS
	public List<String> getDummyBlock(){
		List<String> res = new ArrayList<>();
		
		int connectionNumber = 0;
		String dummy_name = this.name + "_dummy_block";
		res.add("<mode name=\"" + dummy_name + "\">");
		res.add("<pb_type name=\"" + dummy_name + "\" blif_model=\".subckt " + this.name + "_dummy\" num_pb=\"1\">");
		
		for(Port input:this.inputPorts.values()) res.add(input.toString());
		for(Port output:this.outputPorts.values()) res.add(output.toString());
		if(this.has_clock()){
			res.add(this.clockPort.toString());
			for(Port input:this.inputPorts.values()) res.add("<T_setup port=\"" + dummy_name + "." + input.get_name() + "\" clock=\"" + this.clockPort.get_name() + "\" value=\"0.0\"/>"); 
			for(Port output:this.outputPorts.values()) res.add("<T_clock_to_Q port=\"" + dummy_name + "." + output.get_name() + "\" clock=\"" + this.clockPort.get_name() + "\" max=\"0.0\"/>"); 
		}
		res.add("</pb_type>");
		res.add("<interconnect>");
		
		for(Port input:this.inputPorts.values()){
			if(input.get_num_pins() > 1){
				res.add("<direct input=\"" + this.name + "." + input.get_name() + "[" + (input.get_num_pins()-1) + ":0]\" name=\"conn" + connectionNumber++ + "\" output=\"" + dummy_name + "." + input.get_name() + "[" + (input.get_num_pins()-1) + ":0]\"/>");
			}else{
				res.add("<direct input=\"" + this.name + "." + input.get_name() + "[0]\" name=\"conn" + connectionNumber++ + "\" output=\"" + dummy_name + "." + input.get_name() + "[0]\"/>");
			}
		}
		for(Port output:this.outputPorts.values()){
			if(output.get_num_pins() > 1){
				res.add("<direct input=\"" + dummy_name + "." + output.get_name() + "[" + (output.get_num_pins()-1) + ":0]\" name=\"conn" + connectionNumber++ + "\" output=\"" + this.name + "." + output.get_name() + "[" + (output.get_num_pins()-1) + ":0]\"/>");
			}else{
				res.add("<direct input=\"" + dummy_name + "." + output.get_name() + "[0]\" name=\"conn" + connectionNumber++ + "\" output=\"" + this.name + "." + output.get_name() + "[0]\"/>");
			}
		}
		if(this.has_clock()){
			if(this.clockPort.get_num_pins() > 1){
				res.add("<direct input=\"" + this.name + "." + this.clockPort.get_name() + "[" + (this.clockPort.get_num_pins()-1) + ":0]\" name=\"conn" + connectionNumber++ + "\" output=\"" + dummy_name + "." + this.clockPort.get_name() + "[" + (this.clockPort.get_num_pins()-1) + ":0]\"/>");
			}else{
				res.add("<direct input=\"" + this.name + "." + this.clockPort.get_name() + "[0]\" name=\"conn" + connectionNumber++ + "\" output=\"" + dummy_name + "." + this.clockPort.get_name() + "[0]\"/>");
			}
		}
		
		res.add("</interconnect>");
		res.add("</mode>");
		
		return res;
	}
	public List<String> getDummyModels(){
		List<String> dummyModels = new ArrayList<>();
		
		if(this.requiresDummyModel()){
			dummyModels.addAll(this.getDummyModel());
		}else{
			for(Element child:this.children){
				dummyModels.addAll(child.getDummyModels());
			}
		}
		return dummyModels;
	}
	public boolean requiresDummyModel(){
		if(this.children.isEmpty()){
			return !this.has_blif_model();
		}else{
			if(this.name.equals("ram_block_M9K") || this.name.equals("ram_block_M144K") || this.name.equals("mac_out") || this.name.equals("mac_mult")){
				return true;
			}else{
				return false;
			}
		}
	}
	public List<String> getDummyModel(){
		List<String> dummyModel = new ArrayList<>();
		
		dummyModel.add("<model name=\"" + this.name + "_dummy\">");
		dummyModel.add("<input_ports>");
		for(String input:this.inputPorts.keySet()){
			dummyModel.add("<port name=\"" + input + "\"/>");
		}
		if(this.has_clock()){
			dummyModel.add("<port name=\"" + this.clockPort.get_name() + "\" is_clock=\"1\"/>");
		}
		dummyModel.add("</input_ports>");
		dummyModel.add("<output_ports>");
		for(String output:this.outputPorts.keySet()){
			dummyModel.add("<port name=\"" + output + "\"/>");
		}
		dummyModel.add("</output_ports>");
		dummyModel.add("</model>");
		
		return dummyModel;
	}
}
