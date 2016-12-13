package pack.architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pack.util.ErrorLog;

public class Pin implements Comparable<Pin>{
	private String pinType;
	private String portName;
	private int pinNum;
	private String blifModel;
	private int number;
	private Block parentBlock;
	private ArrayList<Conn> inputConnections;
	private ArrayList<Conn> outputConnections;
	
	private static int nextId = 0;
	
	//Dijkstra
	private int delay = Integer.MAX_VALUE;//ps
	private Pin previous = null;
	private final Map<Pin,Integer> neighbours = new HashMap<>();

	public Pin(String pinType, String portName, int pinNum, Block parentBlock){
		this.pinType = pinType;
		this.portName = portName;
		this.pinNum = pinNum;
		this.parentBlock = parentBlock;
		this.inputConnections = new ArrayList<Conn>();
		this.outputConnections = new ArrayList<Conn>();
		this.number = Pin.nextId++;
		if(parentBlock.has_blif_model()){
			this.blifModel = parentBlock.get_blif_model();
		}
	}
	public String get_type(){
		return this.pinType;
	}
	public String get_name(){
		if(this.is_blif()){
			return this.blifModel + "." + this.portName + "[" + this.pinNum + "]";
		}else{
			return this.parentBlock.get_name() + "." + this.portName + "[" + this.pinNum + "]";
		}
	}
	public String get_port_name(){
		return this.portName;
	}
	public int get_pin_num(){
		return this.pinNum;
	}
	public int get_number(){
		return this.number;
	}
	public Block get_parent(){
		return this.parentBlock;
	}
	public boolean is_blif(){
		return this.blifModel != null;
	}
	public String get_blif_name(){
		return this.blifModel;
	}
	
	//Connections
	public void add_input_connection(Conn connection){
		this.inputConnections.add(connection);
	}
	public boolean has_intput_connections(){
		return !this.inputConnections.isEmpty();
	}
	public ArrayList<Conn> get_input_connections(){
		return this.inputConnections;
	}
	public void add_output_connection(Conn connection){
		this.outputConnections.add(connection);
	}
	public boolean has_output_connections(){
		return !this.outputConnections.isEmpty();
	}
	public ArrayList<Conn> get_output_connections(){
		return this.outputConnections;
	}
	
	//Dijkstra
	public void set_delay(int delay){
		this.delay = delay;
	}
	public int get_delay(){
		return this.delay;
	}
	public void set_previous(Pin previous){
		this.previous = previous;
	}
	public Pin get_previous(){
		return this.previous;
	}
	public void assign_neighbours(){
		for(Conn conn:this.outputConnections){
			Pin sink = conn.get_sink();
			if(this.neighbours.containsKey(sink)){
				ErrorLog.print("This situation should not occur");
			}
			int delay = conn.get_delay();
			
			//Setup time is included in connection delay
			Block block = sink.get_parent();
			if(block.has_blif_model()){
				if(block.has_setup_delay(sink.get_port_name())){
					delay += block.get_setup(sink.get_port_name());
				}
			}
			this.neighbours.put(sink, delay);
		}
	}
	public Map<Pin, Integer> get_neighbours(){
		return this.neighbours;
	}
	public String printPath(){
		if(this == this.previous){
			return this.get_name();
		}else if(this.previous == null){
			return this.get_name() + "(unreached)";
		}else{
			return this.previous.printPath() + " -> " + this.get_name() + "(" + this.delay + ")";
		}
	}
	@Override
	public int compareTo(Pin other) {
		return Integer.compare(this.delay, other.get_delay());
	}
}