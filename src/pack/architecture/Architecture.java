package pack.architecture;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import pack.main.Simulation;
import pack.netlist.P;
import pack.util.ErrorLog;
import pack.util.Output;
import pack.util.Timing;
import pack.util.Util;

public class Architecture {
	private String name;
	private Simulation simulation;
	
	private ArrayList<String> lines;
	
	private Set<String> modelSet;
	private Set<String> removedModels;
	private final ArrayList<Pin> pins;
	private HashMap<String,ArrayList<Block>> blifBlocks;
	private HashMap<String,ArrayList<Pin>> blifPins;
	private ArrayList<Block> complexBlocks;
	private int numConn;
	private HashMap<Integer, HashMap<Integer, Conn>> connections;
	private HashMap<String, HashMap<String, Boolean>> globalConnections;
	private HashMap<String, HashMap<String,Integer>> delayMap;
	
	private int sizeX;
	private int sizeY;

	public Architecture(Simulation simulation){
		this.name = simulation.getStringValue("architecture");
		this.simulation = simulation;
		this.modelSet = new HashSet<String>();
		this.removedModels = new HashSet<String>();
		this.pins = new ArrayList<Pin>();
		this.complexBlocks = new ArrayList<Block>();
		this.blifBlocks = new HashMap<String,ArrayList<Block>>();
		this.blifPins = new HashMap<String,ArrayList<Pin>>();
		this.numConn = 0;
		this.connections = new HashMap<Integer,HashMap<Integer,Conn>>();
		this.globalConnections = new HashMap<String,HashMap<String, Boolean>>();
		
		this.delayMap = new HashMap<String, HashMap<String,Integer>>();
	}
	
	//READ AND PARSE FILE
	private ArrayList<String> read_file(String fileName){
		if(!Util.fileExists(fileName)){
			Output.println("Architecture file " + fileName + " does not exist");
		}
		Output.println("\tFilename: " + fileName);
		
		ArrayList<String> lines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
		    String line = br.readLine();
		    while (line != null) {
		    	if(line.contains("<!--") && !line.contains("-->")){
		    		StringBuilder sb = new StringBuilder();
		    		while(!line.contains("-->")){
		    			sb.append(line);
		    			line = br.readLine();
		    		}
		    		sb.append(line);
		    		lines.add(sb.toString());
		    	}else{
		    		lines.add(line);
		    	}
		        line = br.readLine();
		    }
		    br.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return parse_file(lines);
	}
	private ArrayList<String> parse_file(ArrayList<String> lines){//Trim lines | Remove comment and empty lines
		ArrayList<String> res = new ArrayList<String>();
		for(String line:lines){
			if(line.contains("<!--") && line.contains("-->")){
				int startPos = line.indexOf("<!--");
				int endPos = line.indexOf("-->", startPos);
				String comment = line.substring(startPos, endPos+3);
				line = line.replace(comment, "");
			}
			line = line.trim();
			while(line.contains("\t")) line = line.replace("\t", "");
			while(line.contains("  ")) line = line.replace("  ", " ");
			
			if(line.length() > 0){
				res.add(line);
			}
		}
		return res;
	}
	
	//INITIALIZE ARCHITECTURE
	public void initialize(){
		Output.println("Initialize architecture:");
		this.lines = this.read_file(this.simulation.getStringValue("result_folder") + "arch.light.xml");
		this.get_models();
		this.get_complex_blocks(true);
		this.setDimensions();
		Output.newLine();
	}
	private void get_models(){
		for(int i=0;i<this.lines.size();i++){
			String line = this.lines.get(i);
			if(line.contains("<model ")){
				int name = line.indexOf("name=");
				int start = line.indexOf("\"", name+1);
				int stop = line.indexOf("\"", start+1);
				String model = line.substring(start+1, stop);
				this.modelSet.add(model);
			}
		}
	}
	private void get_complex_blocks(boolean loadConnections){
		Timing t = new Timing();
		t.start();
		Element current = null;
		boolean complexBlockLines = false;
		boolean interconnectLines = false;
		boolean complete = false;
		boolean mux = false;
		boolean direct = false;
		for(int i=0;i<this.lines.size();i++){
			String line = this.lines.get(i);
			if(line.contains("<complexblocklist>")){
				complexBlockLines = true;
			}else if(line.contains("</complexblocklist>")){
				complexBlockLines = false;
			}else if(complexBlockLines){
				//Blocks and modes
				if(line.contains("<pb_type")){
					Element parent = current;
					current = new Block(new Line(line));
					if(current.has_blif_model()){
						String blifModel = current.get_blif_model();
						if(!this.blifBlocks.containsKey(blifModel)){
							this.blifBlocks.put(blifModel, new ArrayList<Block>());
						}
						this.blifBlocks.get(blifModel).add((Block)current);
					}
					if(parent != null){
						current.set_parent(parent);
						parent.add_child(current);
					}
				}else if(line.contains("<mode")){
					Element parent = current;
					current = new Mode(new Line(line));
					if(parent != null){
						current.set_parent(parent);
						parent.add_child(current);
					}
				}else if(line.contains("</pb_type>")){
					if(current.has_parent()){
						current = current.get_parent();
					}else{
						this.complexBlocks.add((Block)current);
						current = null;
					}
				}else if(line.contains("</mode>")){
					if(current.has_parent()){
						current = current.get_parent();
					}else{
						ErrorLog.print("Mode has no parent block");
					}
				//Block pins
				}else if(line.contains("<input")){
					Line l = new Line(line);
					if(l.get_type().equals("input")){
						Port inputPort = new Port(l, (Block)current);
						this.pins.addAll(inputPort.get_pins());
						current.add_input(inputPort);
					}else{
						ErrorLog.print("This line should have type input instead of " + l.get_type() + " | " + line);
					}
				}else if(line.contains("<output")){
					Line l = new Line(line);
					if(l.get_type().equals("output")){
						Port outputPort = new Port(l, (Block)current);
						this.pins.addAll(outputPort.get_pins());
						current.add_output(outputPort);
					}else{
						ErrorLog.print("This line should have type output instead of " + l.get_type() + " | " + line);
					}
				}else if(line.contains("<clock")){
					Line l = new Line(line);
					if(l.get_type().equals("clock")){
						Port clockPort = new Port(l, (Block)current);
						this.pins.addAll(clockPort.get_pins());
						current.set_clock(clockPort);
					}else{
						ErrorLog.print("This line should have type clock instead of " + l.get_type() + " | " + line);
					}	
				//Delay information
				}else if(line.contains("<delay_constant") && !interconnectLines){
					Line l = new Line(line);
					if(l.get_type().equals("delay_constant")){
						String[] inputs = l.get_value("in_port").split(" ");
						String[] outputs = l.get_value("out_port").split(" ");
						double secondDelay = Double.parseDouble(l.get_value("max"));
						int picoSecondDelay = (int)Math.round(secondDelay*Math.pow(10, 12));
						for(String input:inputs){
							if(input.contains("[") || input.contains("]")) ErrorLog.print("Wrong input format " + input);
							for(String output:outputs){
								if(output.contains("[") || output.contains("]")) ErrorLog.print("Wrong output format " + output);
								current.add_delay(input, output, picoSecondDelay);
							}
						}
					}else{
						ErrorLog.print("This line should have type delay_constant instead of " + l.get_type() + " | " + line);
					}
				}else if(line.contains("<T_clock_to_Q")){
					Line l = new Line(line);
					if(l.get_type().equals("T_clock_to_Q")){
						String clock = l.get_value("clock");
						if(clock.contains("[") || clock.contains("]")) ErrorLog.print("Wrong clock format " + clock);
						String[] outputs = l.get_value("port").split(" ");
						double secondDelay = Double.parseDouble(l.get_value("max"));
						int picoSecondDelay = (int)Math.round(secondDelay*Math.pow(10, 12));
						for(String output:outputs){
							if(output.contains("[") || output.contains("]")) ErrorLog.print("Wrong output format " + output);
							current.add_clock_to_output(clock, output, picoSecondDelay);
						}
					}else{
						ErrorLog.print("This line should have type T_clock_to_Q instead of " + l.get_type() + " | " + line);
					}
				}else if(line.contains("<T_setup")){
					Line l = new Line(line);
					if(l.get_type().equals("T_setup")){
						String[] inputs = l.get_value("port").split(" ");
						String clock = l.get_value("clock");
						if(clock.contains("[") || clock.contains("]")) ErrorLog.print("Wrong clock format " + clock);
						double secondDelay = Double.parseDouble(l.get_value("value"));
						int picoSecondDelay = (int)Math.round(secondDelay*Math.pow(10, 12));
						for(String input:inputs){
							if(input.contains("[") || input.contains("]")) ErrorLog.print("Wrong input format " + input);
							current.add_setup(input, clock, picoSecondDelay);
						}
					}else{
						ErrorLog.print("This line should have type T_setup instead of " + l.get_type() + " | " + line);
					}
				//Delay matrix
				}else if(line.contains("<delay_matrix")){
					Line l = new Line(line);
					if(l.get_type().equals("delay_matrix")){
						String[] inputs = l.get_value("in_port").split(" ");
						String[] outputs = l.get_value("out_port").split(" ");
						double secondDelay = 0.0;
						line = this.lines.get(++i);
						while(!line.contains("</delay_matrix>")){
							double localDelay = Double.parseDouble(line.replace("\t", "").replace(" ", ""));
							if(secondDelay == 0.0){
								secondDelay = localDelay;
							}else if(secondDelay != localDelay){
								ErrorLog.print("Problem in delay matrix | Delay  = " + secondDelay + " | Local delay = " + localDelay);
							}
							line = lines.get(++i);
						}
						int picoSecondDelay = (int)Math.round(secondDelay*Math.pow(10, 12));
						for(String input:inputs){
							if(input.contains("[") || input.contains("]")) ErrorLog.print("Wrong input format " + input);
							for(String output:outputs){
								if(output.contains("[") || output.contains("]")) ErrorLog.print("Wrong output format " + output);
								current.add_delay(input, output, picoSecondDelay);
							}
						}
					}else{
						ErrorLog.print("This line should have type delay_matrix instead of " + l.get_type() + " | " + line);
					}
				//INTERCONNECT LINES
				}else if(line.contains("<interconnect>")){
					interconnectLines = true;
					current.add_interconnect_line(line);
				}else if(line.contains("</interconnect>")){
					interconnectLines = false;
					current.add_interconnect_line(line);
				//Complete crossbar
				}else if(line.contains("<complete") && loadConnections){
					current.add_interconnect_line(line);
					Line l = new Line(line);
					boolean valid = true;
					if(l.get_value("input").length() == 0) valid = false; //No input found
					if(l.get_value("output").length() == 0) valid = false; //No output found
					if(valid){
						HashMap<String, Block> interconnectedBlocks = this.get_interconnected_blocks((Mode)current);

						String name = l.get_value("name");
						String[] inputPorts = l.get_value("input").split(" ");
						String[] outputPorts = l.get_value("output").split(" ");
							
						for(String inputPort:inputPorts){
							for(String outputPort:outputPorts){
								Block sourceBlock = interconnectedBlocks.get(process_block_name(inputPort.split("\\.")[0], interconnectedBlocks));
								Block sinkBlock = interconnectedBlocks.get(process_block_name(outputPort.split("\\.")[0], interconnectedBlocks));
								
								ArrayList<Pin> inputPins = this.get_pins(inputPort.split("\\.")[1], sourceBlock);
								ArrayList<Pin> outputPins = this.get_pins(outputPort.split("\\.")[1], sinkBlock);
							
								for(Pin inputPin:inputPins){
									for(Pin outputPin:outputPins){
										boolean add = true;
										if(this.connections.containsKey(inputPin.get_number())){
											if(this.connections.get(inputPin.get_number()).containsKey(outputPin.get_number())){
												add = false;
											}
										}
										if(add){
											Conn conn = new Conn(name, inputPin, outputPin, false);
											inputPin.add_output_connection(conn);
											outputPin.add_input_connection(conn);
											if(!this.connections.containsKey(inputPin.get_number())){
												this.connections.put(inputPin.get_number(), new HashMap<Integer,Conn>());
											}
											this.connections.get(inputPin.get_number()).put(outputPin.get_number(), conn);
											this.numConn += 1;
										}
									}
								}							
							}
						}
					}else{
						//Output.println("Non valid complete interconnect line: " + line);
					}
					complete = true;
					mux = false;
					direct = false;
				//Direct connection
				}else if(line.contains("<direct") && loadConnections){
					current.add_interconnect_line(line);
					Line l = new Line(line);
					boolean valid = true;
					if(l.get_value("input").length() == 0) valid = false; //No input found
					if(l.get_value("output").length() == 0) valid = false; //No output found
					if(valid){
						HashMap<String, Block> interconnectedBlocks = this.get_interconnected_blocks((Mode)current);
						
						String name = l.get_value("name");
						String[] inputPorts = l.get_value("input").split(" ");
						String[] outputPorts = l.get_value("output").split(" ");
							
						for(String inputPort:inputPorts){
							for(String outputPort:outputPorts){
								Block sourceBlock = interconnectedBlocks.get(process_block_name(inputPort.split("\\.")[0], interconnectedBlocks));
								Block sinkBlock =interconnectedBlocks.get(process_block_name(outputPort.split("\\.")[0], interconnectedBlocks));
								
								ArrayList<Pin> inputPins = this.get_pins(inputPort.split("\\.")[1], sourceBlock);
								ArrayList<Pin> outputPins = this.get_pins(outputPort.split("\\.")[1], sinkBlock);
								
								if(inputPins.size() != outputPins.size()){
									ErrorLog.print("The number of input pins is not equal to the number of output pins in direct connection | input pins: " + inputPins.size() + " | output pins: " + outputPins.size());
								}
								for(int p=0;p<inputPins.size();p++){
									Pin inputPin = inputPins.get(p);
									Pin outputPin = outputPins.get(p);
									boolean add = true;
									if(this.connections.containsKey(inputPin.get_number())){
										if(this.connections.get(inputPin.get_number()).containsKey(outputPin.get_number())){
											add = false;
										}
									}
									if(add){
										Conn conn = new Conn(name, inputPin, outputPin, false);
										inputPin.add_output_connection(conn);
										outputPin.add_input_connection(conn);
										if(!this.connections.containsKey(inputPin.get_number())){
											this.connections.put(inputPin.get_number(), new HashMap<Integer,Conn>());
										}
										this.connections.get(inputPin.get_number()).put(outputPin.get_number(), conn);
										this.numConn += 1;
									}
								}					
							}
						}
					}else{
						Output.println("Non valid direct interconnect line: " + line);
					}
					complete = false;
					mux = false;
					direct = true;
				//Multiplexer
				}else if(line.contains("<mux") && loadConnections){
					current.add_interconnect_line(line);
					Line l = new Line(line);
					boolean valid = true;
					if(l.get_value("input").length() == 0) valid = false; //No input found
					if(l.get_value("output").length() == 0) valid = false; //No output found
					if(valid){
						HashMap<String, Block> interconnectedBlocks = this.get_interconnected_blocks((Mode)current);
						
						String name = l.get_value("name");
						String[] inputPorts = l.get_value("input").split(" ");
						String[] outputPorts = l.get_value("output").split(" ");
							
						for(String inputPort:inputPorts){
							for(String outputPort:outputPorts){
								Block sourceBlock = interconnectedBlocks.get(process_block_name(inputPort.split("\\.")[0], interconnectedBlocks));
								Block sinkBlock =interconnectedBlocks.get(process_block_name(outputPort.split("\\.")[0], interconnectedBlocks));
								
								ArrayList<Pin> inputPins = this.get_pins(inputPort.split("\\.")[1], sourceBlock);
								ArrayList<Pin> outputPins = this.get_pins(outputPort.split("\\.")[1], sinkBlock);
								if(outputPins.size()!= 1){
									ErrorLog.print("To many output pins in mux connection | output pins: " + outputPins.size() + " | " + line);
								}
								Pin outputPin = outputPins.get(0);
								for(Pin inputPin:inputPins){
									boolean add = true;
									if(this.connections.containsKey(inputPin.get_number())){
										if(this.connections.get(inputPin.get_number()).containsKey(outputPin.get_number())){
											add = false;
										}
									}
									if(add){
										Conn conn = new Conn(name, inputPin, outputPin, false);
										inputPin.add_output_connection(conn);
										outputPin.add_input_connection(conn);
										if(!this.connections.containsKey(inputPin.get_number())){
											this.connections.put(inputPin.get_number(), new HashMap<Integer,Conn>());
										}
										this.connections.get(inputPin.get_number()).put(outputPin.get_number(), conn);
										this.numConn += 1;
									}
								}
							}
						}
					}else{
						Output.println("Non valid mux interconnect line: " + line);
					}
					complete = false;
					mux = true;
					direct = false;
				}else if(line.contains("<delay_constant") && interconnectLines && loadConnections){
					current.add_interconnect_line(line);
					Line l = new Line(line);
					boolean valid = true;
					if(l.get_value("in_port").length() == 0) valid = false; //No input found
					if(l.get_value("out_port").length() == 0) valid = false; //No output found
					if(valid){
						HashMap<String, Block> interconnectedBlocks = this.get_interconnected_blocks((Mode)current);

						double connectionSecondDelay = Double.parseDouble(l.get_value("max"));
						int connectionPicoSecondDelay = (int)Math.round(connectionSecondDelay*Math.pow(10, 12));
						
						String[] inputPorts = l.get_value("in_port").split(" ");
						String[] outputPorts = l.get_value("out_port").split(" ");
							
						for(String inputPort:inputPorts){
							for(String outputPort:outputPorts){
								Block sourceBlock = interconnectedBlocks.get(process_block_name(inputPort.split("\\.")[0], interconnectedBlocks));
								Block sinkBlock = interconnectedBlocks.get(process_block_name(outputPort.split("\\.")[0], interconnectedBlocks));
								
								ArrayList<Pin> inputPins = this.get_pins(inputPort.split("\\.")[1], sourceBlock);
								ArrayList<Pin> outputPins = this.get_pins(outputPort.split("\\.")[1], sinkBlock);
								
								if(complete){
									for(Pin inputPin:inputPins){
										for(Pin outputPin:outputPins){
											Conn conn = this.connections.get(inputPin.get_number()).get(outputPin.get_number());
											int localDelay = conn.get_delay();
											if(localDelay < 0){
												conn.set_delay(connectionPicoSecondDelay);
											}else if(localDelay != connectionPicoSecondDelay){
												Output.println(localDelay + "!=" + connectionPicoSecondDelay);
											}
										}
									}
								}else if(mux){
									if(outputPins.size() != 1){
										ErrorLog.print("Mux should have only one output");
									}
									Pin outputPin = outputPins.get(0);
									for(Pin inputPin:inputPins){
										Conn conn = this.connections.get(inputPin.get_number()).get(outputPin.get_number());
										int localDelay = conn.get_delay();
										if(localDelay < 0){
											conn.set_delay(connectionPicoSecondDelay);
										}else if(localDelay != connectionPicoSecondDelay){
											Output.println(localDelay + "!=" + connectionPicoSecondDelay);
										}
									}
								}else if(direct){
									if(inputPins.size() != outputPins.size()){
										ErrorLog.print("The number of input pins is not equal to the number of output pins in direct connection | input pins: " + inputPins.size() + " | output pins: " + outputPins.size());
									}
									for(int p=0;p<inputPins.size();p++){
										Pin inputPin = inputPins.get(p);
										Pin outputPin = outputPins.get(p);
										Conn conn = this.connections.get(inputPin.get_number()).get(outputPin.get_number());
										int localDelay = conn.get_delay();
										if(localDelay < 0){
											conn.set_delay(connectionPicoSecondDelay);
										}else if(localDelay != connectionPicoSecondDelay){
											Output.println(localDelay + "!=" + connectionPicoSecondDelay);
										}
									}
								}
							}
						}
					}else{
						//Output.println("Non valid delay constant line: " + line);
					}
				}else{
					current.add_interconnect_line(line);
				}
			}
		}
		if(loadConnections){
			//Delay of short connection in global routing architecture 
			//      => this value is architecture specific! 
			//TODO Determine this value based on architecture file
			int interconnectDelay = 132;

			for(Block sourceBlock:this.complexBlocks){
				for(Port sourcePort:sourceBlock.get_output_ports()){
					for(Pin sourcePin:sourcePort.get_pins()){
						for(Block sinkBlock:this.complexBlocks){
							for(Port sinkPort:sinkBlock.get_input_and_clock_ports()){
								for(Pin sinkPin:sinkPort.get_pins()){
									Conn conn = new Conn("global_interconnect", sourcePin, sinkPin, true);
									conn.set_delay(interconnectDelay);
									sourcePin.add_output_connection(conn);
									sinkPin.add_input_connection(conn);
									if(!this.connections.containsKey(sourcePin.get_number())){
										this.connections.put(sourcePin.get_number(), new HashMap<Integer,Conn>());
									}
									if(!this.connections.get(sourcePin.get_number()).containsKey(sinkPin.get_number())){
										this.connections.get(sourcePin.get_number()).put(sinkPin.get_number(), conn);
										this.numConn += 1;
									}else{
										Output.println("There is already a connection between " + sourcePin.get_name() + " and " + sinkPin.get_name());
									}
								}
							}
						}
					}
				}
			}
			//Set delay of all connection without a delay to O
			for(int source:this.connections.keySet()){
				for(int sink:this.connections.get(source).keySet()){
					Conn conn = this.connections.get(source).get(sink);
					if(conn.get_delay()<0){
						conn.set_delay(0);
					}
				}
			}
			//Assign blif pins
			for(String blifName:this.blifBlocks.keySet()){
				for(Block blifBlock:this.blifBlocks.get(blifName)){
					for(Port inputPort:blifBlock.get_input_and_clock_ports()){
						for(Pin inputPin:inputPort.get_pins()){
							if(!this.blifPins.containsKey(inputPin.get_name())){
								this.blifPins.put(inputPin.get_name(), new ArrayList<Pin>());
							}
							this.blifPins.get(inputPin.get_name()).add(inputPin);
						}
					}
					for(Port outputPort:blifBlock.get_output_ports()){
						for(Pin outputPin:outputPort.get_pins()){
							if(!this.blifPins.containsKey(outputPin.get_name())){
								this.blifPins.put(outputPin.get_name(), new ArrayList<Pin>());
							}
							this.blifPins.get(outputPin.get_name()).add(outputPin);
						}
					}
				}
			}
			//Assign neighbours of each pin
			for(Pin pin:this.pins){
				pin.assign_neighbours();
			}
			Output.println("\t" + this.numConn + " connections");
		}
		t.stop();
		Output.println("\tArchitecture generation took " + t.toString());
		this.test();
	}
	private String process_block_name(String blockName, HashMap<String,Block> interconnectedBlocks){
		if(blockName.contains("[") && blockName.contains("]") && blockName.contains(":")){
			Element block = interconnectedBlocks.get(blockName.substring(0,blockName.indexOf("[")));
			int startNum = Integer.parseInt(blockName.substring(blockName.indexOf("[")+1,blockName.indexOf(":")));
			int endNum = Integer.parseInt(blockName.substring(blockName.indexOf(":")+1,blockName.indexOf("]")));
			if(endNum < startNum){
				int temp = endNum;
				endNum = startNum;
				startNum = temp;
			}
			if(Integer.parseInt(block.get_value("num_pb")) != (endNum - startNum + 1)){
				ErrorLog.print("Non symmetric connections for block " + blockName);
			}
			return blockName.substring(0,blockName.indexOf("["));
		}else if(blockName.contains("[") && blockName.contains("]") && !blockName.contains(":")){
			Element block = interconnectedBlocks.get(blockName.substring(0,blockName.indexOf("[")));
			if(Integer.parseInt(block.get_value("num_pb")) == 1){
				return blockName.substring(0,blockName.indexOf("["));
			}else{
				return blockName.substring(0,blockName.indexOf("["));
			}
		}else{
			return blockName;
		}
	}
	private ArrayList<Pin> get_pins(String portName, Block block){
		if(portName.contains("[") && portName.contains("]") && portName.contains(":")){
			Port port = block.get_port(portName.substring(0,portName.indexOf("[")));
			int startNum = Integer.parseInt(portName.substring(portName.indexOf("[")+1,portName.indexOf(":")));
			int endNum = Integer.parseInt(portName.substring(portName.indexOf(":")+1,portName.indexOf("]")));
			if(endNum < startNum){
				int temp = endNum;
				endNum = startNum;
				startNum = temp;
			}
			ArrayList<Pin> pins = new ArrayList<Pin>();
			for(int pinNum=startNum;pinNum<=endNum;pinNum++){
				pins.add(port.get_pin(pinNum));
			}
			return pins;
		}else if(portName.contains("[") && portName.contains("]") && !portName.contains(":")){
			Port port = block.get_port(portName.substring(0,portName.indexOf("[")));
			int pinNum = Integer.parseInt(portName.substring(portName.indexOf("[")+1,portName.indexOf("]")));
			ArrayList<Pin> pins = new ArrayList<Pin>();
			pins.add(port.get_pin(pinNum));
			return pins;
		}else{
			Port port = block.get_port(portName);
			return port.get_pins();
		}
	}
	private HashMap<String, Block> get_interconnected_blocks(Mode current){
		HashMap<String, Block> interconnectedBlocks = new HashMap<String, Block>();
		Element parent = current.get_parent();
		interconnectedBlocks.put(parent.get_name(), (Block)parent);
		for(Element child:current.get_children()){
			if(!interconnectedBlocks.containsKey(child.get_name())){
				interconnectedBlocks.put(child.get_name(), (Block)child);
			}else{
				ErrorLog.print("Duplicate child block names");
			}
		}
		return interconnectedBlocks;
	}
	private void test(){
		for(String blifModel:this.blifBlocks.keySet()){
			for(Block blifBlock:this.blifBlocks.get(blifModel)){
				for(Port inputPort:blifBlock.get_input_and_clock_ports()){
					for(Pin inputPin:inputPort.get_pins()){
						if(inputPin.has_output_connections()){
							ErrorLog.print("This inputPin should not have output connections");
						}
					}
				}
				for(Port outputPort:blifBlock.get_output_ports()){
					for(Pin outputPin:outputPort.get_pins()){
						if(outputPin.has_intput_connections()){
							ErrorLog.print("This outputPin should not have input connections");
						}
					}
				}
			}
		}
	}
	
	//Architecture dimensions
	public void setDimensions(){
		for(int i=0; i<this.lines.size();i++){
			String line = this.lines.get(i);
			if(line.contains("layout") && line.contains("width") && line.contains("height")){
				String[]words = line.split(" ");
				for(String word:words){
					if(word.contains("width")){
						word = word.replace("\"", "");
						word = word.replace("width", "");
						word = word.replace("=", "");
						word = word.replace("/", "");
						word = word.replace(">", "");
						this.sizeX = Integer.parseInt(word);
					}else if(word.contains("height")){
						word = word.replace("\"", "");
						word = word.replace("height", "");
						word = word.replace("=", "");
						word = word.replace("/", "");
						word = word.replace(">", "");
						this.sizeY = Integer.parseInt(word);
					}
				}
			}
		}
	}
	public void removeDimensions(){
		for(int i=0; i<this.lines.size();i++){
			String line = this.lines.get(i);
			if(line.contains("layout") && line.contains("width") && line.contains("height")){
				this.lines.set(i, "<layout auto=\"1.35\"/>");
			}
		}
	}
	
	//GENERATE NETLIST SPECIFIC ARCHITECTURES
	public void generate_light_architecture(Set<String> usedModelsInNetlist){
		Output.println("Generate light architecture:");
		
		boolean fixedSize = this.simulation.getBooleanValue("fixed_size");
		Output.println("\tFixed size: " + fixedSize);
		this.lines = this.read_file(this.simulation.getStringValue("result_folder") + this.name);
		if(fixedSize){
			this.setDimensions();
			Output.println("\t\tSizeX: " + this.sizeX + " | SizeY: " + this.sizeY);
		}
		
		this.get_models();
		this.get_complex_blocks(false);
		
		//This function removes unused blocks and modes from the architecture
		HashSet<String> unremovableBlocks = new HashSet<String>();
		unremovableBlocks.add("io");
		unremovableBlocks.add("io_cell");
		unremovableBlocks.add("dell");
		unremovableBlocks.add("pad");
		unremovableBlocks.add("inpad");
		
		this.analyze_blocks(usedModelsInNetlist, unremovableBlocks);
		
		HashMap<String, ArrayList<String>> models = new HashMap<String, ArrayList<String>>();
		ArrayList<String> device = new ArrayList<String>();
		boolean modelLines = false;
		boolean deviceLines = false;
		for(int i=0;i<this.lines.size();i++){
			String line = this.lines.get(i);
			if(line.contains("<models>")){
				modelLines = true;
			}else if(line.contains("</models>")){
				modelLines = false;
				deviceLines = true;
			}else if(line.contains("<complexblocklist>")){
				deviceLines = false;
			}else if(modelLines){
				if(line.contains("<model ")){
					Line l = new Line(line);
					if(!this.removedModels.contains(l.get_value("name"))){
						ArrayList<String> temp = new ArrayList<String>();
						temp.add(line);
						do{
							line = this.lines.get(++i);
							temp.add(line);
						}while(!line.contains("</model>"));
						models.put(l.get_value("name"), temp);
					}else{
						do{
							line = this.lines.get(++i);
						}while(!line.contains("</model>"));
					}
				}
			}else if(deviceLines){
				device.add(line);
			}
		}
		
		//LIGHT ARCH
		ArrayList<String> lightArch = new ArrayList<String>();
		lightArch.add("<?xml version=\"1.0\"?>");
		lightArch.add("<architecture>");
		lightArch.add("<models>");
		for(String m:models.keySet()){
			ArrayList<String> temp = models.get(m);
			for(int t=0;t<temp.size();t++){
				lightArch.add(temp.get(t));
			}
		}
		lightArch.add("</models>");
		for(String d:device){
			lightArch.add(d);
		}
		lightArch.add("<complexblocklist>");
		for(Block block:this.complexBlocks){
			lightArch.addAll(block.toStringList());
		}
		lightArch.add("</complexblocklist>");
		lightArch.add("</architecture>");
		this.write_arch_file(lightArch,"arch.light.xml");
		
		Output.newLine();
	}
	public void generate_pack_architecture(Set<String> usedModelsInNetlist){
		Output.println("Generate pack architecture:");
		
		boolean fixedSize = this.simulation.getBooleanValue("fixed_size");
		Output.println("\tFixed size: " + fixedSize);
		this.lines = this.read_file(this.simulation.getStringValue("result_folder") + this.name);
		if(fixedSize){
			this.setDimensions();
			this.removeDimensions();
			Output.println("\t\tSizeX: " + this.sizeX + " | SizeY: " + this.sizeY);
		}
		
		this.get_models();
		this.get_complex_blocks(false);
		
		//This function removes unused blocks and modes from the architecture
		HashSet<String> unremovableBlocks = new HashSet<String>();
		unremovableBlocks.add("io");
		unremovableBlocks.add("io_cell");
		unremovableBlocks.add("dell");
		unremovableBlocks.add("pad");
		unremovableBlocks.add("inpad");
		
		unremovableBlocks.add("PLL");
		unremovableBlocks.add("normal");
		unremovableBlocks.add("pll_normal");
		
		unremovableBlocks.add("LAB");

		unremovableBlocks.add("DSP");
		unremovableBlocks.add("full_DSP");
		unremovableBlocks.add("half_DSP");
		unremovableBlocks.add("half_DSP_normal");
		unremovableBlocks.add("mac_mult");
		unremovableBlocks.add("dult");
		unremovableBlocks.add("mac_out");
		unremovableBlocks.add("dout");
		
		unremovableBlocks.add("M9K");
		unremovableBlocks.add("ram");
		unremovableBlocks.add("ram_block_M9K");
		unremovableBlocks.add("B9K");
		
		unremovableBlocks.add("M144K");
		unremovableBlocks.add("ram");
		unremovableBlocks.add("ram_block_M144K");
		unremovableBlocks.add("B144K");
		this.analyze_blocks(usedModelsInNetlist, unremovableBlocks);
		
		//PACK PATTERNS
		boolean dspFound = false;
		for(Element element:this.complexBlocks){
			if(element.get_name().equals("DSP")){
				if(!dspFound){
					this.set_pack_pattern(element);
					dspFound = true;
				}else{
					ErrorLog.print("Two DSP complex blocks found");
				}
			}
		}
		if(!dspFound){
			Output.println("\tWarning: No DSP complex block found");
		}
		
		//MEMORY BLOCKS
		Block M9K = null;
		HashSet<String> M9KModels = new HashSet<String>();
		Block M144K = null;
		HashSet<String> M144KModels = new HashSet<String>();
		for(Block block:this.complexBlocks){
			if(block.get_name().equals("M9K")){
				if(M9K == null){
					M9K = block;
				}else{
					ErrorLog.print("Already an M9K block found");
				}
			}
			if(block.get_name().equals("M144K")){
				if(M144K == null){
					M144K = block;
				}else{
					ErrorLog.print("Already an M144K block found");
				}
			}
		}
		if(M9K != null){
			M9KModels = new HashSet<String>(M9K.get_blif_models());
		}else{
			M9KModels = new HashSet<String>();
			Output.println("\tWarning: No M9K complex block found");
		}
		if(M144K != null){
			M144KModels = new HashSet<String>(M144K.get_blif_models());
		}else{
			M144KModels = new HashSet<String>();
			Output.println("\tWarning: No M144K complex block found");
		}
		
		HashMap<String, ArrayList<String>> models = new HashMap<String, ArrayList<String>>();
		ArrayList<String> device = new ArrayList<String>();
		boolean modelLines = false;
		boolean deviceLines = false;
		for(int i=0;i<this.lines.size();i++){
			String line = this.lines.get(i);
			if(line.contains("<models>")){
				modelLines = true;
			}else if(line.contains("</models>")){
				modelLines = false;
				deviceLines = true;
			}else if(line.contains("<complexblocklist>")){
				deviceLines = false;
			}else if(modelLines){
				if(line.contains("<model ")){
					Line l = new Line(line);
					if(!this.removedModels.contains(l.get_value("name"))){
						ArrayList<String> temp = new ArrayList<String>();
						temp.add(line);
						do{
							line = this.lines.get(++i);
							temp.add(line);
						}while(!line.contains("</model>"));
						models.put(l.get_value("name"), temp);
					}else{
						do{
							line = this.lines.get(++i);
						}while(!line.contains("</model>"));
					}
				}
			}else if(deviceLines){
				device.add(line);
			}
		}
		
		//PACK ARCH
		ArrayList<String> packArch = new ArrayList<String>();
		packArch.add("<?xml version=\"1.0\"?>");
		packArch.add("<architecture>");
		packArch.add("<models>");
		for(String m:models.keySet()){
			ArrayList<String> temp = models.get(m);
			if(m.contains("stratixiv_ram_block") && m.contains("width")){
				if(M9KModels.contains(m)){
					packArch.add(temp.get(0).replace("\">", "") + "_M9K" + "\">");
					for(int t=1;t<temp.size();t++) packArch.add(temp.get(t));
				}
				if(M144KModels.contains(m)){
					packArch.add(temp.get(0).replace("\">", "") + "_M144K" + "\">");
					for(int t=1;t<temp.size();t++) packArch.add(temp.get(t));
				}
			}else{
				for(int t=0;t<temp.size();t++) packArch.add(temp.get(t));
			}
		}
		packArch.add("</models>");
		for(String d:device){
			packArch.add(d);
		}
		packArch.add("<complexblocklist>");
		
		if(M9K != null) M9K.modify_blif_model_names("_M9K");
		if(M144K != null) M144K.modify_blif_model_names("_M144K");
		
		for(Block block:this.complexBlocks){
			packArch.addAll(block.toStringList());
		}
		packArch.add("</complexblocklist>");
		packArch.add("</architecture>");
		this.write_arch_file(packArch, "arch.pack.xml");
		
		Output.newLine();
	}
	private void analyze_blocks(Set<String> usedModels, HashSet<String> unremovableBlocks){
		//TOP LEVEL
		ArrayList<Element> currentLevel = new ArrayList<Element>();
		ArrayList<Element> nextLevel = new ArrayList<Element>();
		
		HashSet<Element> removedBlocks = new HashSet<Element>();
		for(Element block:this.complexBlocks){
			if(unremovableBlocks.contains(block.get_name())){
				nextLevel.add(block);
			}else if(block.remove_block(usedModels, this.modelSet)){
				removedBlocks.add(block);
			}else{
				nextLevel.add(block);
			}
		}
		for(Element removedBlock:removedBlocks){
			this.complexBlocks.remove(removedBlock);
			this.removedModels.addAll(removedBlock.get_blif_models());
		}
		
		//FIRST LEVEL
		while(!nextLevel.isEmpty()){
			currentLevel = nextLevel;
			nextLevel = new ArrayList<Element>();
			for(Element block:currentLevel){
				Set<Element> children = new HashSet<Element>(block.get_children());
				for(Element child:children){
					if(unremovableBlocks.contains(child.get_name())){
						nextLevel.add(child);
					}else if(child.remove_block(usedModels, this.modelSet)){
						block.remove_child(child);
						this.removedModels.addAll(child.get_blif_models());
					}else{
						nextLevel.add(child);
					}
				}
			}
		}
	}
	private void write_arch_file(ArrayList<String> arch, String name){
		int tabs = 0;
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(this.simulation.getStringValue("result_folder") + name));
			for(String line:arch){
				if(line.contains("</")){
					tabs -= 1;
				}
				bw.write(Util.tabs(tabs) + line);
				bw.newLine();
				
				if(line.contains("<") && !line.contains("</")){
					tabs += 1;
				}
				if(line.contains("/>")){
					tabs -= 1;
				}
			}
			bw.close();
		}catch(IOException ex){
			Output.println (ex.toString());
		}
	}
	
	//GET DELAY VALUES
	public boolean valid_connection(String blifModel, String sourcePortName, String sinkPortName){
		boolean validConnection = true;
		if(!this.blifBlocks.containsKey(blifModel)){
			ErrorLog.print("Blif model " + blifModel + " not found");
		}else{
			for(Block block:this.blifBlocks.get(blifModel)){
				if(!block.valid_connection(sourcePortName, sinkPortName)){
					validConnection = false;
				}
			}
		}
		return validConnection;
	}
	public int get_block_delay(String blifModel, String sourcePortName, String sinkPortName){
		int minDelay = Integer.MAX_VALUE;
		
		if(!this.blifBlocks.containsKey(blifModel)){
			ErrorLog.print("Blif model " + blifModel + " not found");
		}else{
			for(Block block:this.blifBlocks.get(blifModel)){
				if(!block.has_input_port(sourcePortName) && !block.has_clock_port(sourcePortName)){
					ErrorLog.print("Block " + blifModel + " does not have source port " + sourcePortName);
				}
				if(!block.has_output_port(sinkPortName)){
					ErrorLog.print("Block " + blifModel + " does not have sink port " + sinkPortName);
				}
				int localMinDelay = block.get_delay(sourcePortName, sinkPortName);
				if(localMinDelay < minDelay){
					minDelay = localMinDelay;
				}
			}
		}
		if(minDelay > 100000){
			Output.println("Large block delay for block " + blifModel + " between " + sourcePortName + " and " + sinkPortName + " => " + minDelay);
		}
		return minDelay;
	}
	public int get_connection_delay(P sourcePin, P sinkPin){
		//Setup time and Tcq delay are included in connection delay
		String source = null, sourceLight = null;
		String sink = null, sinkLight = null;
		if(sourcePin.has_block() && sinkPin.has_block()){
			sourceLight = sourcePin.get_light_architecture_name();
			sinkLight = sinkPin.get_light_architecture_name();
		}else if(sourcePin.has_block() && !sinkPin.has_block()){
			sourceLight = sourcePin.get_light_architecture_name();
			sinkLight = ".output" + "." + sinkPin.get_port_name();
		}else if(!sourcePin.has_block() && sinkPin.has_block()){
			sourceLight = ".input" + "." + sourcePin.get_port_name();
			sinkLight = sinkPin.get_light_architecture_name();
		}else if(!sourcePin.has_block() && !sinkPin.has_block()){
			sourceLight =".input" + "." + sourcePin.get_port_name();
			sinkLight = ".output" + "." + sinkPin.get_port_name();
		}
		
		Integer connectionDelay = null;
		HashMap<String,Integer> tempMap = this.delayMap.get(sourceLight);
		if(tempMap != null){
			connectionDelay = tempMap.get(sinkLight);
			if(connectionDelay == null){
				if(sourcePin.has_block() && sinkPin.has_block()){
					source = sourcePin.get_detailed_architecture_name();
					sink = sinkPin.get_detailed_architecture_name();
				}else if(sourcePin.has_block() && !sinkPin.has_block()){
					source = sourcePin.get_detailed_architecture_name();
					sink = ".output" + "." + sinkPin.get_port_name() + "[" +  sinkPin.get_pin_num() + "]";
				}else if(!sourcePin.has_block() && sinkPin.has_block()){
					source = ".input" + "." + sourcePin.get_port_name() + "[" +  sourcePin.get_pin_num() + "]";
					sink = sinkPin.get_detailed_architecture_name();
				}else if(!sourcePin.has_block() && !sinkPin.has_block()){
					source =".input" + "." + sourcePin.get_port_name() + "[" +  sourcePin.get_pin_num() + "]";
					sink = ".output" + "." + sinkPin.get_port_name() + "[" +  sinkPin.get_pin_num() + "]";
				}
				connectionDelay = this.get_connection_delay(source, sink, sourceLight, sinkLight);
				tempMap.put(sinkLight, connectionDelay);
			}
		}else{
			tempMap = new HashMap<String,Integer>();
			if(sourcePin.has_block() && sinkPin.has_block()){
				source = sourcePin.get_detailed_architecture_name();
				sink = sinkPin.get_detailed_architecture_name();
			}else if(sourcePin.has_block() && !sinkPin.has_block()){
				source = sourcePin.get_detailed_architecture_name();
				sink = ".output" + "." + sinkPin.get_port_name() + "[" +  sinkPin.get_pin_num() + "]";
			}else if(!sourcePin.has_block() && sinkPin.has_block()){
				source = ".input" + "." + sourcePin.get_port_name() + "[" +  sourcePin.get_pin_num() + "]";
				sink = sinkPin.get_detailed_architecture_name();
			}else if(!sourcePin.has_block() && !sinkPin.has_block()){
				source =".input" + "." + sourcePin.get_port_name() + "[" +  sourcePin.get_pin_num() + "]";
				sink = ".output" + "." + sinkPin.get_port_name() + "[" +  sinkPin.get_pin_num() + "]";
			}
			connectionDelay = this.get_connection_delay(source, sink, sourceLight, sinkLight);
			tempMap.put(sinkLight, connectionDelay);
			this.delayMap.put(sourceLight, tempMap);
		}
		return connectionDelay;
	}
	private int get_connection_delay(String sourcePinName, String sinkPinName, String sourceLight, String sinkLight){
		//Setup time and Tcq delay are included in connection delay
		if(!this.blifPins.containsKey(sourcePinName)){
			ErrorLog.print("Architecture does not contain source pin " + sourcePinName);
			return Integer.MAX_VALUE;
		}
		if(!this.blifPins.containsKey(sinkPinName)){
			ErrorLog.print("Architecture does not contain sink pin " + sinkPinName);
			return Integer.MAX_VALUE;
		}
		
		PriorityQueue<Pin> q = new PriorityQueue<Pin>();
		for(Pin p:this.pins){
			if(p.get_name().equals(sourcePinName)){
				p.set_previous(p);
				
				//Clock to output delay is included in connection delay
				Block block = p.get_parent();
				if(block.has_clock_to_output_delay(p.get_port_name())){
					p.set_delay(block.get_clock_to_output(p.get_port_name()));
				}else{
					p.set_delay(0);
				}
			}else{
				p.set_previous(null);
				p.set_delay(Integer.MAX_VALUE);
			}
			q.add(p);
		}
		Pin endPin = this.dijkstra(q, sinkPinName);
		int minDelay = endPin.get_delay();
		
		//GLOBAL CONNECTIONS
		boolean global = false;
		Pin sink = endPin;
		while(!sink.get_name().equals(sourcePinName)){
			Pin source = sink.get_previous();
			if(this.connections.get(source.get_number()).get(sink.get_number()).is_global()){
				if(!global){
					global = true;
				}else{
					ErrorLog.print("2 global connections between " + sourcePinName + " and " + sinkPinName);
				}
			}
			sink = source;
		}
		if(!this.globalConnections.containsKey(sourceLight)){
			this.globalConnections.put(sourceLight, new HashMap<String, Boolean>());
		}
		this.globalConnections.get(sourceLight).put(sinkLight, global);
		
		return minDelay;
	}
	private Pin dijkstra(PriorityQueue<Pin> q, String stopCondition){//, String sourceType, String sinkType){
		Pin u,v;
		while(!q.isEmpty()){
			u = q.poll();
			if(u.get_name().equals(stopCondition)) return u;
			if(u.get_delay() == Integer.MAX_VALUE) break;
			for(Map.Entry<Pin, Integer> a:u.get_neighbours().entrySet()){
				//Output.println(u.get_neighbours().size());
				v = a.getKey();
					
				final int alternateDelay = u.get_delay() + a.getValue();
				if(alternateDelay < v.get_delay()){
					q.remove(v);
					v.set_delay(alternateDelay);
					v.set_previous(u);
					q.add(v);
				}
			}
		}
		ErrorLog.print("Pin " + stopCondition + " not found in Dijkstra algorithm");
		return null;
	}
	public int slack(P sourcePin, P sinkPin){
		int arr = sourcePin.get_arrival_time();
		int req = sinkPin.get_required_time();
		int connDelay = this.get_connection_delay(sourcePin, sinkPin);
		int slack = req - arr - connDelay;
		if(slack < 0){
			Output.println("Slack should be larger than zero but is equal to " + slack);
		}
		return slack;
	}
	public boolean is_connected_via_global_connection(String sourcePinName, String sinkPinName){
		//REMOVE M9K AND M144K APPENDIX
		sourcePinName = sourcePinName.replace("_M9K","");
		sinkPinName = sinkPinName.replace("_M9K","");
		sourcePinName = sourcePinName.replace("_M144K","");
		sinkPinName = sinkPinName.replace("_M144K","");
		
		if(this.globalConnections.containsKey(sourcePinName)){
			if(this.globalConnections.get(sourcePinName).containsKey(sinkPinName)){
				return this.globalConnections.get(sourcePinName).get(sinkPinName);
			}else{
				ErrorLog.print(sinkPinName + " not found as sink of a connection");
				return false;
			}
		}else{
			Output.println("SourcePins:");
			for(String sourcePin:this.globalConnections.keySet()){
				Output.println("\t" + sourcePin);
			}
			ErrorLog.print(sourcePinName + " not found as source of a connection");
			return false;
		}
	}
	
	//PACK PATTERN
	private void set_pack_pattern(Element element){
		Timing t = new Timing();
		t.start();
		this.remove_pack_pattern(element);
		this.add_pack_pattern(element);
		t.stop();
		if(t.time() > 0.5){
			ErrorLog.print("Set pack pattern took " + t.toString());
		}
	}
	private void remove_pack_pattern(Element element){
		ArrayList<Element> work = new ArrayList<Element>();
		work.add(element);
		while(!work.isEmpty()){
			Element current = work.remove(0);
			current.remove_pack_patterns();
			for(Element child:current.get_children()){
				work.add(child);
			}
		}
	}
	private void add_pack_pattern(Element element){
		Element halfDSP = null;
		HashSet<Element> macMults = new HashSet<Element>();
		HashSet<Element> macOuts = new HashSet<Element>();
		ArrayList<Element> work = new ArrayList<Element>();
		work.add(element);
		while(!work.isEmpty()){
			Element current = work.remove(0);
			if(current.get_type().equals("mode") && current.get_name().equals("half_DSP_normal")){
				halfDSP = current;
			}else if(current.get_type().equals("pb_type")){
				if(current.get_name().equals("mac_mult")){
					for(Element child:current.get_children()){
						if(!child.get_name().equals("dult")){
							macMults.add((Mode)child);
						}else{
							Output.println("\tNo pack patterns for " + child.get_name() + " added");
						}
					}
				}else if(current.get_name().equals("mac_out")){
					for(Element child:current.get_children()){
						if(!child.get_name().equals("dout")){
							macOuts.add((Mode)child);
						}else{
							Output.println("\tNo pack patterns for " + child.get_name() + " added");
						}
					}
				}
			}
			for(Element child:current.get_children()){
				work.add(child);
			}
		}
		
		ArrayList<String[]> packPatterns = new ArrayList<String[]>();
		for(Element macMult:macMults){
			String macMultType = macMult.get_name().replace("mac_mult_", "");
			for(Element macOut:macOuts){
				String macOutType = macOut.get_name().split("\\.")[0];
				String macOutInput = macOut.get_name().split("\\.")[1].replace("input_type{", "").replace("}", "");
				String macOutOutput = macOut.get_name().split("\\.")[2].replace("output_type{", "").replace("}", "");
				String[] packPattern = {macMultType,macOutType,macOutInput,macOutOutput,""};
				packPatterns.add(packPattern);
			}
		}
		for(Element macMult:macMults){
			macMult.set_pack_pattern(packPatterns);
		}
		for(Element macOut:macOuts){
			macOut.set_pack_pattern(packPatterns);
		}
		halfDSP.set_pack_pattern(packPatterns);
	}
	
	//SIZE
	public int getSizeX(){
		return this.sizeX;
	}
	public int getSizeY(){
		return this.sizeY;
	}
}
