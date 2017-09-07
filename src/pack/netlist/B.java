package pack.netlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import pack.partition.HardBlockGroup;
import pack.partition.Part;
import pack.util.ErrorLog;
import pack.util.Output;

public class B {
	private String name;
	private int number;
	private String type;
	
	private HashMap<String,ArrayList<P>> inputPins;
	private HashMap<String,ArrayList<P>> outputPins;
	private String clock;

	private ArrayList<B> atoms;
	
	private Integer part = null;
	
	private String truthTable;
	
	private HardBlockGroup hbg;

	public B(String name, int number, String type, String clock, HardBlockGroup rg){
		this.name = name;
		this.number = number;

		this.type = type;

		this.inputPins = new HashMap<String,ArrayList<P>>();
		this.outputPins = new HashMap<String,ArrayList<P>>();
		this.clock = clock;

		this.hbg = rg;
	}
	
	//TRIM AND CLEAN UP
	public void trim(){
		for(String inputPort:this.get_input_ports()){
			this.get_input_pins(inputPort).trimToSize();
		}
		for(String outputPort:this.get_output_ports()){
			this.get_output_pins(outputPort).trimToSize();
		}
		if(this.has_atoms()){
			this.atoms.trimToSize();
		}
	}
	public void clean_up(){
		this.name = null;
		this.type = null;
		this.inputPins = null;
		this.outputPins = null;
		this.atoms = null;
		this.truthTable = null;
	}
	
	//GETTERS AND SETTERS
	public String get_name(){
		return this.name;
	}
	public int get_number(){
		return this.number;
	}
	public void set_type(String type){
		if(!type.contains("stratixiv_ram_block")){
			ErrorLog.print("This function should only be used for stratixiv_ram_block, not for " + this.type);
		}else{
			this.type = type;
		}
	}
	public String get_type(){
		return this.type;
	}
	
	//REMOVE
	public void remove_input_pin(P inputPin){
		boolean inputPinRemoved = false;
		for(String inputPort:this.inputPins.keySet()){
			for(int i=this.inputPins.get(inputPort).size()-1;i>-1;i--){
				if(this.inputPins.get(inputPort).get(i).equals(inputPin)){
					if(!inputPinRemoved){
						this.inputPins.get(inputPort).remove(i);
						inputPinRemoved = true;
					}else{
						ErrorLog.print("Input pin already removed");
					}
				}
			}
		}
		for(String inputPort:new HashSet<String>(this.inputPins.keySet())){
			if(this.inputPins.get(inputPort).isEmpty()){
				this.inputPins.remove(inputPort);
			}
		}
		if(!inputPinRemoved){
			ErrorLog.print("No input pin removed!");
		}
	}
	public void remove_output_pin(P outputPin){
		boolean outputPinRemoved = false;
		for(String outputPort:this.outputPins.keySet()){
			for(int i=this.outputPins.get(outputPort).size()-1;i>-1;i--){
				if(this.outputPins.get(outputPort).get(i).equals(outputPin)){
					if(!outputPinRemoved){
						this.outputPins.get(outputPort).remove(i);
						outputPinRemoved = true;
					}else{
						ErrorLog.print("Output pin already removed");
					}
				}
			}
		}
		ArrayList<String> outputPorts = new ArrayList<String>(this.outputPins.keySet());
		for(String outputPort:outputPorts){
			if(this.outputPins.get(outputPort).isEmpty()){
				this.outputPins.remove(outputPort);
			}
		}
		if(!outputPinRemoved){
			ErrorLog.print("No output pin removed!");
		}
	}
	
	//Pins
	public void add_input_pin(String inputPort, P inputPin){
		if(!this.has_input_port(inputPort)){
			ArrayList<P> temp = new ArrayList<P>();
			temp.add(inputPin);
			this.inputPins.put(inputPort, temp);
		}else{
			this.inputPins.get(inputPort).add(inputPin);
		}
	}
	public void add_output_pin(String outputPort, P outputPin){
		if(!this.has_output_port(outputPort)){
			ArrayList<P> temp = new ArrayList<P>();
			temp.add(outputPin);
			this.outputPins.put(outputPort, temp);
		}else{
			this.outputPins.get(outputPort).add(outputPin);
		}
	}
	public ArrayList<P> get_input_pins(String inputPort){
		if(this.has_input_port(inputPort)){
			return this.inputPins.get(inputPort);
		}else{
			return new ArrayList<P>();
		}
	}
	public ArrayList<P> get_output_pins(String outputPort){
		if(this.has_output_port(outputPort)){
			return this.outputPins.get(outputPort);
		}else{
			return new ArrayList<P>();
		}
	}
	public ArrayList<P> get_pins(String port){
		if(this.has_input_port(port)){
			return this.get_input_pins(port);
		}else if(this.has_output_port(port)){
			return this.get_output_pins(port);
		}else{
			return new ArrayList<P>();
		}
	}	
	public ArrayList<N> get_nets(String port){
		ArrayList<N> result = new ArrayList<N>();
		for(P pin:this.get_pins(port)){
			result.add(pin.get_net());
		}
		return result;
	}
	public boolean has_input_port(String inputPort){
		return this.inputPins.containsKey(inputPort);
	}
	public boolean has_output_port(String outputPort){
		return this.outputPins.containsKey(outputPort);
	}
	public boolean has_port(String port){
		return this.has_input_port(port) || this.has_output_port(port);
	}
	public boolean has_pins(){
		return this.has_inputs() || this.has_outputs();
	}
	public boolean has_inputs(){
		return !this.inputPins.isEmpty();
	}
	public boolean has_outputs(){
		return !this.outputPins.isEmpty();
	}
	public ArrayList<P> get_pins(){
		ArrayList<P> result = new ArrayList<P>();
		if(this.has_pins()){
			for(String inputPort:this.inputPins.keySet()){
				result.addAll(this.inputPins.get(inputPort));
			}
			for(String outputPort:this.outputPins.keySet()){
				result.addAll(this.outputPins.get(outputPort));
			}
		}
		return result;
	}
	public ArrayList<N> get_nets(){
		ArrayList<N> nets = new ArrayList<N>();
		for(P pin:this.get_pins()){
			nets.add(pin.get_net());
		}
		return nets;
	}
	public ArrayList<P> get_input_pins(){
		ArrayList<P> inputPins = new ArrayList<P>();
		for(String inputPort:this.inputPins.keySet()){
			inputPins.addAll(this.inputPins.get(inputPort));
		}
		return inputPins;
	}
	public ArrayList<N> get_input_nets(){
		ArrayList<N> inputNets = new ArrayList<N>();
		for(P inputPin:this.get_input_pins()){
			inputNets.add(inputPin.get_net());
		}
		return inputNets;
	}
	public ArrayList<P> get_output_pins(){
		ArrayList<P> outputPins = new ArrayList<P>();
		for(String outputPort:this.outputPins.keySet()){
			outputPins.addAll(this.outputPins.get(outputPort));
		}
		return outputPins;
	}
	public ArrayList<N> get_output_nets(){
		ArrayList<N> outputNets = new ArrayList<N>();
		for(P outputPin:this.get_output_pins()){
			outputNets.add(outputPin.get_net());
		}
		return outputNets;
	}
	
	//STRING
	public String toString(){
		return this.get_name();
	}
	
	//CONNECTIVITY
	public boolean is_connected_with_other_block(){
		for(String inputPort:this.inputPins.keySet()){
			for(P inputPin:this.inputPins.get(inputPort)){
				N inputNet = inputPin.get_net();
				if(inputNet.has_source()){
					P sourcePin = inputNet.get_source_pin();
					if(sourcePin.has_block()){
						B sourceBlock = sourcePin.get_block();
						if(sourceBlock.get_number() != this.get_number()){
							return true;
						}
					}
				}
				for(P sinkPin:inputNet.get_sink_pins()){
					if(sinkPin.has_block()){
						B sinkBlock = sinkPin.get_block();
						if(sinkBlock.get_number() != this.get_number()){
							return true;
						}
					}
				}
			}
		}		
		for(String outputPort:this.outputPins.keySet()){
			for(P outputPin:this.outputPins.get(outputPort)){
				N outputNet = outputPin.get_net();
				for(P sinkPin:outputNet.get_sink_pins()){
					if(sinkPin.has_block()){
						B sinkBlock = sinkPin.get_block();
						if(sinkBlock.get_number() != this.get_number()){
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	//INTERNAL NETLIST
	public boolean has_atoms(){
		if(this.atoms == null){
			return false;
		}else if(this.atoms.isEmpty()){
			return false;
		}else{
			return true;
		}
	}
	public void add_atom(B atom){
		if(!has_atoms()){
			this.atoms = new ArrayList<B>();
		}
		this.atoms.add(atom);
	}
	public ArrayList<B> get_atoms(){
		if(this.has_atoms()){
			return this.atoms;
		}else{
			return new ArrayList<B>();
		}
	}
	
	//CLOCK
	public boolean has_clock(){
		return !(this.clock == null);
	}
	public String get_clock(){
		return this.clock;
	}
	public boolean is_sequential(){
		return this.has_clock();
	}
	
	//CLEAN COPY
	public B clean_copy(){
		int terminalN = 0;
		HardBlockGroup rg = null;
		if(this.hasHardBlockGroup()){
			rg = this.getHardBlockGroup();
		}
		B cleanCopy = new B(this.get_name(), this.get_number(), this.get_type(), this.get_clock(), rg);
		if(this.has_atoms()){
			for(B atom:this.get_atoms()){
				cleanCopy.add_atom(atom);
			}
		}
		cleanCopy.set_truth_table(this.get_truth_table());
		for(String inputPort:this.get_input_ports()){
			for(P inputPin:this.get_input_pins(inputPort)){
				N inputNet = inputPin.get_net();
				
				N newN = new N(inputNet.get_name(), inputNet.get_number());
				T newT = new T(inputNet.get_name(), terminalN++, "NULL");
				
				P p1 = new P(newT, newN, "ter", false, false, 1, null, null, false, false);
				newN.set_terminal_pin(p1);
				newT.set_pin(p1);
				
				P p2 = new P(cleanCopy, newN, inputPin);
				cleanCopy.add_input_pin(inputPort, p2);
				newN.add_sink(p2);
			}
		}
		for(String outputPort:this.get_output_ports()){
			for(P outputPin:this.get_output_pins(outputPort)){
				N outputNet = outputPin.get_net();
				
				N newN = new N(outputNet.get_name(), outputNet.get_number());
				T newT = new T(outputNet.get_name(),terminalN++, "NULL");
				
				P p1 = new P(newT, newN, "ter", false, false, 1, null, null, false, false);
				newN.set_terminal_pin(p1);
				newT.set_pin(p1);
				
				P p2 = new P(cleanCopy, newN, outputPin);
				cleanCopy.add_output_pin(outputPort, p2);
				newN.set_source(p2);
			}
		}
		return cleanCopy;
	}
	
	//PORT
	public Set<String> get_input_ports(){
		return this.inputPins.keySet();
	}
	public Set<String> get_output_ports(){
		return this.outputPins.keySet();
	}
	
	//HASH
	public String get_hash(){
		StringBuffer hash = new StringBuffer();
		if(this.get_type().contains("stratixiv_ram_block")){
			ArrayList<String> hashedInputs = new ArrayList<String>();
			hashedInputs.add("portaaddr");
			hashedInputs.add("portbaddr");
			hashedInputs.add("ena0");
			hashedInputs.add("ena1");
			hashedInputs.add("ena2");
			hashedInputs.add("ena3");
			hashedInputs.add("clr0");
			hashedInputs.add("clr1");
			hashedInputs.add("portare");
			hashedInputs.add("portbre");
			hashedInputs.add("portawe");
			hashedInputs.add("portbwe");
			hashedInputs.add("portaaddrstall");
			hashedInputs.add("portbaddrstall");
			hashedInputs.add("portabyteenamasks");
			hashedInputs.add("portbbyteenamasks");
			
			hashedInputs.add("clk0");
			
			hash.append(this.get_type());
			for(String inputPort:hashedInputs){
				if(this.has_input_port(inputPort)){
					hash.append(inputPort);
					for(P inputPin:this.get_input_pins(inputPort)){
						hash.append(inputPin.get_net().toString());
					}
				}
			}
		}else{
			Output.println("BlockType is " + this.get_type() + ", should be stratixiv_ram_block");
		}
		return hash.toString();
	}
	
	//PART
	public void set_part(int val){
		this.part = val;
	}
	public void remove_part(){
		this.part = null;
	}
	public int get_part(){
		if(this.part != null){
			return this.part;
		}else{
			ErrorLog.print("This block has no part");
			return 0;
		}
	}
	
	//BLIF
	public void to_blif_string(Blif blif, Model model){
		boolean printUnconnectedPins = false;
		ArrayList<String> parts = new ArrayList<String>();
		if(this.get_type().equals(".latch")){
			parts.addAll(this.net_string("in"));
			parts.addAll(this.net_string("out"));
			parts.add("re");
			parts.addAll(this.net_string("clk"));
			parts.add("0");
			blif.add_latch(".latch" + toString(parts));
		}else if(this.get_type().equals(".names")){
			parts.addAll(this.net_string("in"));
			parts.addAll(this.net_string("out"));
			blif.add_gate(".names" + toString(parts) + "\n" + this.get_truth_table());
		}else{
			for(String inputPort:model.get_input_ports()){
				parts.addAll(input_port_to_blif(inputPort, model, printUnconnectedPins));
			}
			Boolean firstOutputPort = true;
			for(String outputPort:model.get_output_ports()){
				parts.addAll(output_port_to_blif(outputPort, model, blif, firstOutputPort, printUnconnectedPins));
				firstOutputPort = false;
			}
			if(this.get_type().contains("stratixiv_pll")){
				blif.add_pll(this.get_name(), ".subckt " + this.get_type() + toString(parts));
			}else{
				blif.add_subcircuit(this.get_name(), ".subckt " + this.get_type() + toString(parts));
			}
			blif.add_model(model);
		}
	}
	private ArrayList<String> net_string(String port){
		ArrayList<String> result = new ArrayList<String>();
		for(N net:this.get_nets(port)){
			result.add(net.get_name());
		}
		return result;
	}
	private ArrayList<String> input_port_to_blif(String port, Model model, boolean printUnconnectedPins){
		ArrayList<String> result = new ArrayList<String>();
		
		ArrayList<N> nets = this.get_nets(port);
		int pinsOnPort = model.pins_on_port(port);
		if(pinsOnPort == 1){
			if(nets.size() == 1){
				result.add(port + "=" + nets.get(0).get_name());
			}else if(nets.size() > 1){
				ErrorLog.print("This port should have maximum one net");
			}else{
				if(printUnconnectedPins)result.add(port + "=" + "unconn");
			}
		}else{
			for(int i=0; i<pinsOnPort; i++){
				if(nets.size()<=i){
					if(printUnconnectedPins)result.add(port + "[" + i + "]" + "=" + "unconn");
				}else{
					result.add(port + "[" + i + "]" + "=" + nets.get(i).get_name());
				}
			}
		}
		return result;
	}
	public ArrayList<String> output_port_to_blif(String port, Model model, Blif blif, boolean firstOutputPort, boolean printUnconnectedPins){
		ArrayList<String> result = new ArrayList<String>();
		
		ArrayList<N> nets = this.get_nets(port);
		int pinsOnPort = model.pins_on_port(port);
		if(pinsOnPort == 1){
			if(nets.size() == 1){
				if(firstOutputPort){
					if(nets.get(0).get_name().equals(this.get_name())){
						result.add(port + "=" + this.get_name());
					}else{
						ErrorLog.print(this.get_name() + " is not the name of the first output port " + nets.get(0).get_name());
					}
					firstOutputPort = false;
				}else{
					result.add(port + "=" + nets.get(0).get_name());
				}
			}else if(nets.size() > 1){
				ErrorLog.print("This port should have maximum one net");
			}else{
				if(firstOutputPort){
					result.add(port + "=" + this.get_name());
					firstOutputPort = false;
				}else{
					if(printUnconnectedPins)result.add(port + "=" + blif.get_dummy_net());
				}
			}
		}else{
			for(int i=0; i<model.pins_on_port(port); i++){
				if(nets.size() <= i){
					if(firstOutputPort){
						result.add(port + "[" + i + "]" + "=" + this.get_name());
						firstOutputPort = false;
					}else{
						if(printUnconnectedPins)result.add(port + "[" + i + "]" + "=" + blif.get_dummy_net());
					}
				}else{
					if(firstOutputPort){
						if(nets.get(i).get_name().equals(this.get_name())){
							result.add(port + "[" + i + "]" + "=" + this.get_name());
						}else{
							ErrorLog.print(this.get_name() + " is not the name of the first output port " + nets.get(0).get_name());
						}
						firstOutputPort = false;
					}else{
						result.add(port + "[" + i + "]" + "=" + nets.get(i).get_name());
					}
				}
			}
		}
		return result;
	}
	public String toString(ArrayList<String> parts){
		StringBuffer s = new StringBuffer();
		int no = 0;
		for(String part:parts){
			if(no > 50){
				s.append(" \\\n");
				no = 0;
			}
			s.append(" " + part);
			no += part.length() + 1;
		}
		return s.toString();
	}
	
	//TRUTH TABLE
	public boolean has_truth_table(){
		return !(this.truthTable == null);
	}
	public String get_truth_table(){
		if(this.has_truth_table()){
			return this.truthTable;
		}else{
			return null;
		}
	}
	public void set_truth_table(String val){
		if(this.has_truth_table()){
			Output.println("Block " + this.toString() + " already has a truth table");
		}else{
			this.truthTable = val;
		}
	}
	
	//CARRY CHAIN
	public B get_next_block_in_chain(String type){
		if(this.has_output_port(type)){
			if(this.get_output_pins(type).size() == 1){
				N outNet = this.get_output_pins(type).get(0).get_net();
				if(outNet.get_sink_pins().size() == 1){
					P inPin = null;
					for(P p:outNet.get_sink_pins()){
						if(inPin == null){
							inPin = p;
						}else{
							ErrorLog.print("There should only be one in pin");
						}
					}
					if(inPin.has_block()){
						return inPin.get_block();
					}else{
						ErrorLog.print("Block expexted on this pin");
						return null;
					}
				}else{
					ErrorLog.print("Fanout one expected on out net");
					return null;
				}
			}else{
				ErrorLog.print("Only one out pin expected");
				return null;
			}
		}else{
			ErrorLog.print("This block should have an out pin");
			return null;
		}
	}
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof B)) return false;
	    B otherMyClass = (B)other;
	    if(otherMyClass.get_number() == this.get_number()) return true;
	    return false;
	}
	
	//RAM BLOCKS
	public HashSet<P> get_data_inputs(){
		if(this.get_type().contains("stratixiv_ram_block")){
			HashSet<P> dataInputPins = new HashSet<P>();
			if(this.has_input_port("portadatain")){
				ArrayList<P> pins = this.get_input_pins("portadatain");
				if(pins.size() != 1){
					ErrorLog.print("Error in ram block " + this.toString() + " on input pin " + "portadatain" + " => " + pins.size() + " pins");
				}
				dataInputPins.add(pins.get(0));
			}
			if(this.has_input_port("portbdatain")){
				ArrayList<P> pins = this.get_input_pins("portbdatain");
				if(pins.size() != 1){
					ErrorLog.print("Error in ram block " + this.toString() + " on input pin " + "portbdatain" + " => " + pins.size() + " pins");
				}
				dataInputPins.add(pins.get(0));
			}
			return dataInputPins;
		}else{
			ErrorLog.print("No RAM block => " + this.get_type());
			return null;
		}
	}
	public void setHardBlockGroup(HardBlockGroup hbg){
		this.hbg = hbg;
	}
	public boolean hasHardBlockGroup(){
		return this.hbg != null;
	}
	public HardBlockGroup getHardBlockGroup(){
		if(this.hasHardBlockGroup()){
			return this.hbg;
		}else{
			ErrorLog.print("This block " + this.toString() + " has no rgroup");
			return null;
		}
		
	}
	public void move(Part from, Part to){
		from.remove(this);
		to.add(this);
	}
	public void move(Part from, Part to,  ArrayList<HashMap<HardBlockGroup,ArrayList<B>>> hashedRam){
		from.remove(this);
		to.add(this);
		
		hashedRam.get(from.getPartNumber()).get(this.getHardBlockGroup()).remove(this);
		if(hashedRam.get(from.getPartNumber()).get(this.getHardBlockGroup()).isEmpty()){
			hashedRam.get(from.getPartNumber()).remove(this.getHardBlockGroup());
		}
		
		if(!hashedRam.get(to.getPartNumber()).containsKey(this.getHardBlockGroup())){
			hashedRam.get(to.getPartNumber()).put(this.getHardBlockGroup(),new ArrayList<B>());
		}
		hashedRam.get(to.getPartNumber()).get(this.getHardBlockGroup()).add(this);
	}
	
	@Override public int hashCode() {
		return this.number;
    }
}