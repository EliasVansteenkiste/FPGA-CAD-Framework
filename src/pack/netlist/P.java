package pack.netlist;

import java.util.Comparator;

import pack.architecture.Architecture;
import pack.util.ErrorLog;
import pack.util.Output;
import pack.util.Util;

public class P implements Comparable<P>{
	private N net;
	private B block;
	private T terminal;
	private String port;
	
	private String id;
	
	private boolean isSourcePin;
	private boolean isSinkPin;
	
	private boolean isEndPin;
	private boolean isStartPin;
	
	private int arrivalTime;
	private boolean hasArrivalTime;
	
	private int requiredTime;
	private boolean hasReq;
	
	private boolean hasBlock;
	private boolean hasTerminal;

	private boolean hasCriticality;
	private double criticality;
	
	private int netWeight;
	
	static final boolean DEBUG = true;
	
	private boolean upstream;
	private boolean downstream;
	
	public P(B b, N n, String port, boolean isSourcePin, boolean isSinkPin, int netWeight, Integer arrivalTime, Integer requiredTime, boolean isStartPin, boolean isEndPin){
		this.net = n;
		this.block = b;
		this.terminal = null;
		this.port = port;
		
		this.id = "b" + b.get_number() + "_" + "n" + n.get_number() + "_" + port;
		
		if(isSourcePin && isSinkPin){
			ErrorLog.print("This pin is source and sink");
		}else{
			this.isSourcePin = isSourcePin;
			this.isSinkPin = isSinkPin;
		}
		
		if(isStartPin){
			if(this.isSourcePin){
				this.isStartPin = true;
			}else{
				ErrorLog.print("A start pin should be a source pin");
			}
		}else{
			this.isStartPin = false;
		}
		if(isEndPin){
			if(this.isSinkPin){
				this.isEndPin = true;
			}else{
				ErrorLog.print("An end pin should be a sink pin");
			}
		}else{
			this.isEndPin = false;
		}
		
		if(arrivalTime == null){
			this.hasArrivalTime = false;
			this.arrivalTime = 0;
		}else{
			this.hasArrivalTime = true;
			this.arrivalTime = arrivalTime;
		}
		if(requiredTime == null){
			this.hasReq = false;
			this.requiredTime = Integer.MAX_VALUE;
		}else{
			this.hasReq = true;
			this.requiredTime = requiredTime;
		}
		
		this.hasBlock = true;
		this.hasTerminal = false;
		
		if(this.is_sink_pin()){
			this.netWeight = netWeight;
		}else{
			if(netWeight != 0){
				ErrorLog.print("NetWeight should be 0 but is equal to: " + netWeight);
			}
			this.netWeight = 0;
		}
		this.hasCriticality = false;
		
		this.upstream = false;
		this.downstream = false;
	}
	public P(T t, N n, String port, boolean isSourcePin, boolean isSinkPin, int netWeight, Integer arrivalTime, Integer requiredTime, boolean isStartPin, boolean isEndPin){
		this.net = n;
		this.block = null;
		this.terminal = t;
		this.port = port;
		
		this.id = "t" + t.get_number() + "_" + "n" + n.get_number() + "_" + port;
		
		if(isSourcePin && isSinkPin){
			ErrorLog.print("This pin is source and sink");
		}else{
			this.isSourcePin = isSourcePin;
			this.isSinkPin = isSinkPin;
		}
		
		if(isStartPin){
			if(this.isSourcePin){
				this.isStartPin = true;
			}else{
				ErrorLog.print("A start pin should be a source pin");
			}
		}else{
			this.isStartPin = false;
		}
		if(isEndPin){
			if(this.isSinkPin){
				this.isEndPin = true;
			}else{
				ErrorLog.print("An end pin should be a sink pin");
			}
		}else{
			this.isEndPin = false;
		}

		if(arrivalTime == null){
			this.hasArrivalTime = false;
			this.arrivalTime = 0;
		}else{
			this.hasArrivalTime = true;
			this.arrivalTime = arrivalTime;
		}
		if(requiredTime == null){
			this.requiredTime = Integer.MAX_VALUE;
			this.hasReq = false;
		}else{
			this.requiredTime = requiredTime;
			this.hasReq = true;
		}
		
		this.hasBlock = false;
		this.hasTerminal = true;
		
		if(this.is_sink_pin()){
			this.netWeight = netWeight;
		}else{
			this.netWeight = 0;
		}
		this.hasCriticality = false;
		
		this.upstream = false;
		this.downstream = false;
	}
	public P(B b, N n, P copyPin){
		this(b, n, copyPin.get_port(), copyPin.is_source_pin(), copyPin.is_sink_pin(), copyPin.get_net_weight(), copyPin.get_arrival_time(), copyPin.get_required_time(), copyPin.is_start_pin(), copyPin.is_end_pin());
	}
	public P(T t, N n, P copyPin){
		this(t, n, copyPin.get_port(), copyPin.is_source_pin(), copyPin.is_sink_pin(), copyPin.get_net_weight(), copyPin.get_arrival_time(), copyPin.get_required_time(), copyPin.is_start_pin(), copyPin.is_end_pin());
	}

	public boolean has_block(){
		return this.hasBlock;
	}
	public boolean has_terminal(){
		return this.hasTerminal;
	}
	
	public N get_net(){
		return this.net;
	}
	public B get_block(){
		if(this.hasBlock){
			return this.block;
		}else{
			Output.println("This pin has no block");
			return null;
		}
	}
	public T get_terminal(){
		if(this.hasTerminal){
			return this.terminal;
		}else{
			Output.println("This pin has no pad");
			return null;
		}
	}
	public String get_port(){
		return this.port;
	}
	public String get_port_name(){
		if(this.port.endsWith("]")){
			return this.port.substring(0, this.port.lastIndexOf("["));
		}else{
			return this.port;
		}
	}
	public int get_pin_num(){
		if(this.port.endsWith("]")){
			return Integer.parseInt(this.port.substring(this.port.lastIndexOf("[") + 1, this.port.length() - 1));
		}else{
			return 0;
		}
	}
	public String get_id(){
		return this.id;
	}
	
	public boolean is_source_pin(){
		return this.isSourcePin;
	}
	public boolean is_sink_pin(){
		return this.isSinkPin;
	}
	
	public boolean is_start_pin(){
		return this.isStartPin;
	}
	public boolean is_end_pin(){
		return this.isEndPin;
	}
	
	//ARRIVAL TIME
	public void set_arrival_time(int val){
		if(this.has_arrival_time()){
			if(this.get_arrival_time() != val){
				ErrorLog.print("ERROR: LOOP DETECTED");
				ErrorLog.print("\t=> The arrival time of net " + this.get_net().toString() +  " is now " + val + " instead of " + this.get_arrival_time() + " and the function is called from block " + this.get_block().get_name());
			}
		}else{
			this.hasArrivalTime = true;
			this.arrivalTime = val;
		}
	}
	public int get_arrival_time(){
		if(this.has_arrival_time()){
			return this.arrivalTime;
		}else{
			Output.println("This pin has no arrival time:\n");
			Output.println(this.toInfoString());
			Output.println("\tBlock name: " + this.block.get_name());
			Output.println("\tDetailed architecture name: " + this.get_detailed_architecture_name());
			ErrorLog.print("This pin has no arrival time");
			return 0;
		}
	}
	public boolean has_arrival_time(){
		return this.hasArrivalTime;
	}
	public int recursive_arrival_time(Architecture arch, DelayMap connectionDelay){
		if(this.has_arrival_time()){
			return this.get_arrival_time();
		}
		if(this.get_net().has_source()){
			P sourcePin = this.get_net().get_source_pin();
			if(sourcePin.is_start_pin()){
				sourcePin.set_arrival_time(0);
				this.set_arrival_time(sourcePin.get_arrival_time() + connectionDelay.getDelay(sourcePin, this));
				return this.get_arrival_time();
			}else{
				B sourceBlock = sourcePin.get_block();
				if(sourceBlock.has_inputs()){
					int maxArrivalTime = 0;
					for(String inputPort:sourceBlock.get_input_ports()){
						for(P inputPin:sourceBlock.get_input_pins(inputPort)){
							if(arch.valid_connection(sourceBlock.get_type(), inputPin.get_port_name(),sourcePin.get_port_name())){
								int localArrivalTime = inputPin.recursive_arrival_time(arch, connectionDelay) + arch.get_block_delay(sourceBlock.get_type(),inputPin.get_port_name(), sourcePin.get_port_name());
								if(localArrivalTime > maxArrivalTime) maxArrivalTime = localArrivalTime;
							}
						}
					}
					sourcePin.set_arrival_time(maxArrivalTime);
					this.set_arrival_time(sourcePin.get_arrival_time() + connectionDelay.getDelay(sourcePin, this));
					return this.get_arrival_time();
				}else{
					//Constant generator
					sourcePin.set_arrival_time(0);
					this.set_arrival_time(sourcePin.get_arrival_time() + connectionDelay.getDelay(sourcePin, this));
					return this.get_arrival_time();
				}
			}
		}else{
			P sourcePin = null;
			for(P terminalPin:this.get_net().get_terminal_pins()){
				if(terminalPin.get_terminal().is_input_type()){
					if(sourcePin == null){
						sourcePin = terminalPin;
					}else{
						ErrorLog.print("Multiple intput terminal found while 1 expexted");
					}
				}
			}
			if(sourcePin == null){
				ErrorLog.print("No input terminal found while 1 expexted");
			}
			if(DEBUG){
				if(!sourcePin.is_start_pin()){
					Output.println("This source pin of the pad should be a start pin");
				}
			}
			sourcePin.set_arrival_time(0);
			this.set_arrival_time(sourcePin.get_arrival_time() + connectionDelay.getDelay(sourcePin, this));
			return this.get_arrival_time();
		}
	}
	
	//REQUIRED TIME
	public boolean has_required_time(){
		return this.hasReq;
	}
	public void set_required_time(int val){
		if(this.has_required_time()){
			if(this.get_required_time() != val){
				ErrorLog.print("ERROR: LOOP DETECTED => ReqTime: " + this.requiredTime);
			}
		}else{
			this.hasReq = true;
			this.requiredTime = val;
		}
	}
	public int get_required_time(){
		if(this.has_required_time()){
			return this.requiredTime;
		}else{
			Output.println("Pin has no required time value: " + this.get_id());
			Output.println("\tPort name: " + this.get_port());
			Output.println("\tNet name: " + this.get_net().get_name());
			if(this.has_block()){
				Output.println("\tBlock name: " + this.get_block().get_name());
			}else{
				Output.println("\tTerminal name: " + this.get_terminal().get_name());
			}
			ErrorLog.print("This pin has no required time value");
			return 0;
		}
	}
	public int recursive_required_time(Architecture arch, DelayMap connectionDelay){
		if(this.has_required_time()){
			return this.get_required_time();
		}
		if(this.is_source_pin()){
			int minRequiredTime = Integer.MAX_VALUE;
			for(P sinkPin:this.get_net().get_sink_pins()){
				int localRequiredTime = sinkPin.recursive_required_time(arch, connectionDelay) - connectionDelay.getDelay(this, sinkPin);

				if(localRequiredTime < minRequiredTime){
					minRequiredTime = localRequiredTime;
				}
			}
			for(P terminalPin:this.get_net().get_terminal_pins()){
				if(terminalPin.is_end_pin()){
					int localRequiredTime = terminalPin.recursive_required_time(arch, connectionDelay) - connectionDelay.getDelay(this, terminalPin);
					
					if(localRequiredTime < minRequiredTime){
						minRequiredTime = localRequiredTime;
					}
				}
			}
			this.set_required_time(minRequiredTime);
			return this.get_required_time();
		}else if(this.is_sink_pin()){
			B sinkBlock = this.get_block();
			int minRequiredTime = Integer.MAX_VALUE;
			for(String outputPort:sinkBlock.get_output_ports()){
				for(P outputPin:sinkBlock.get_output_pins(outputPort)){
					if(arch.valid_connection(sinkBlock.get_type(), this.get_port_name(), outputPin.get_port_name())){
						int localRequiredTime = outputPin.recursive_required_time(arch, connectionDelay) - arch.get_block_delay(sinkBlock.get_type(), this.get_port_name(), outputPin.get_port_name());
						if(localRequiredTime < minRequiredTime){
							minRequiredTime = localRequiredTime;
						}
					}
				}
			}
			this.set_required_time(minRequiredTime);
			return this.get_required_time();
		}else{
			ErrorLog.print("Unexpected situation, a pin should be a start pin or an end pin");
			return 0;
		}
	}
	
	//NET WEIGHT
	public void set_net_weight(int netWeight){
		if(this.is_source_pin()){
			ErrorLog.print("Only sink pins have a net weight");
		}else if(this.netWeight != 1){
			ErrorLog.print("This sink pin already had a net weight => " + this.netWeight);
		}else{
			this.netWeight = netWeight;
		}
	}
	public int get_net_weight(){
		return this.netWeight;
	}
	public void downStreamUpdate(){
		if(!this.downstream){
			this.downstream = true;
			N net = this.get_net();
			if(net.has_source()){
				B sourceBlock = net.get_source_pin().get_block();
				if(!sourceBlock.is_sequential()){
					for(String inputPort:sourceBlock.get_input_ports()){
						for(P inputPin:sourceBlock.get_input_pins(inputPort)){
							if(inputPin.get_net_weight() > 1){
								inputPin.increaseNetWeight();
								inputPin.downStreamUpdate();
							}
						}
					}
				}
			}
		}
	}
	public void upStreamUpdate(){
		if(!this.upstream){
			this.upstream = true;
			if(this.has_block()){
				B sinkBlock = this.get_block();
				if(!sinkBlock.is_sequential()){
					for(String outputPort:sinkBlock.get_output_ports()){
						for(P outputPin:sinkBlock.get_output_pins(outputPort)){
							N outputNet = outputPin.get_net();
							for(P sinkPin:outputNet.get_sink_pins()){
								if(sinkPin.get_net_weight() > 1){
									sinkPin.increaseNetWeight();
									sinkPin.upStreamUpdate();
								}
							}
						}
					}
				}
			}
		}
	}
	public void increaseNetWeight(){
		if(this.netWeight > 1 && this.netWeight < 100){
			this.netWeight *= 100;
		}
	}
	
	//STRING
	public String toInfoString(){
		String s = new String();
		s += "Pin id: " + this.get_id();
		s += "\n";
		s += "Block name: " + this.get_block().toString() + "| Block number: " + this.get_block().get_number(); 
		s += "\n";
		s += "Net name : " + this.get_net().toString() + " | Net number : " + this.get_net().get_number();
		s += "\n";
		return s;
	}
	public String toString(){	
		return this.get_id();
	}

	//EQUALS
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof P)) return false;
	    P otherMyClass = (P)other;
	    if(otherMyClass.get_id().equals(this.get_id())) return true;
	    return false;
	}
	
	//NAME
	public String get_detailed_architecture_name(){
		String pinPortName = this.get_port_name();
		if(this.has_block()){
			if(this.get_block().has_atoms()){
				if(Util.isMoleculeType(this.get_block().get_type())){
					String blockName = pinPortName.substring(0, pinPortName.indexOf("."));
					String portName = pinPortName.substring(pinPortName.indexOf(".") + 1,pinPortName.length());
					B atomBlock = null;
					for(B atom:this.get_block().get_atoms()){
						if(atom.get_name().equals(blockName)){
							if(atomBlock == null){
								atomBlock = atom;
							}else{
								ErrorLog.print("Block with name " + blockName + " already found in the atoms");
							}
						}
					}
					if(atomBlock == null){
						ErrorLog.print("Any of the atom blocks corresponds to " + blockName + " | Port name: " + pinPortName);
					}
					if(atomBlock.has_port(portName)){
						return atomBlock.get_type() + "." + portName + "[" +  this.get_pin_num() + "]";
					}else{
						ErrorLog.print("Atom block " + blockName + " does not have port " + portName);
					}
				}else{
					ErrorLog.print("Unexpected block type: " + this.get_block().get_type());
				}
				return null;
			}else{
				return this.get_block().get_type() + "." + pinPortName + "[" +  this.get_pin_num() + "]";
			}
		}else{
			ErrorLog.print("This pin should have a block instead of a terminal");
			return null;
		}
	}
	public String get_light_architecture_name(){
		String pinPortName = this.get_port_name();
		if(this.has_block()){
			if(this.get_block().has_atoms()){
				if(Util.isMoleculeType(this.get_block().get_type())){
					if(pinPortName.indexOf(".") < 0){
						System.out.println(this.block.get_type());
					}
					String blockName = pinPortName.substring(0, pinPortName.indexOf("."));
					String portName = pinPortName.substring(pinPortName.indexOf(".") + 1,pinPortName.length());
					B atomBlock = null;
					for(B atom:this.get_block().get_atoms()){
						if(atom.get_name().equals(blockName)){
							if(atomBlock == null){
								atomBlock = atom;
							}else{
								ErrorLog.print("Block with name " + blockName + " already found in the atoms");
							}
						}
					}
					if(atomBlock == null){
						ErrorLog.print("Any of the atom blocks corresponds to " + blockName + " | Port name: " + pinPortName);
					}
					if(atomBlock.has_port(portName)){
						return atomBlock.get_type() + "." + portName;
					}else{
						ErrorLog.print("Atom block " + blockName + " does not have port " + portName);
					}
				}else{
					ErrorLog.print("Unexpected block type: " + this.get_block().get_type());
				}
				return null;
			}else{
				return this.get_block().get_type() + "." + pinPortName;
			}
		}else{
			ErrorLog.print("This pin should have a block instead of a terminal");
			return null;
		}
	}
	public void set_criticality(double criticality){
		this.criticality = criticality;
		this.hasCriticality = true;
	}
	public double criticality(){
		if(!this.hasCriticality)ErrorLog.print("No criticality assigned");
		return this.criticality;
	}	
	@Override public int hashCode() {
		return this.id.hashCode();
    }
	@Override public int compareTo(P p) {
		return this.get_id().compareTo(p.get_id());
	}
	public static Comparator<P> PinCriticalityComparator = new Comparator<P>(){
		public int compare(P p1, P p2){
			if(p1.criticality() == p2.criticality()){
				return 0;
			}else if(p1.criticality() > p2.criticality()){
				return -1;
			}else{
				return 1;
			}
		}
	};
}
