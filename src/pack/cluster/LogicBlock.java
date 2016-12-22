package pack.cluster;

import java.util.ArrayList;
import java.util.HashMap;

import pack.util.ErrorLog;
import pack.util.Util;

public class LogicBlock {
	private String name;
	private String instance;
	private int instanceNumber;
	private String mode;
	
	private HashMap<String, String[]> inputs;
	private HashMap<String, String[]> outputs;
	private HashMap<String, String[]> clocks;
	
	private ArrayList<LogicBlock> childBlocks;
	
	private static int num = 0;
	
	public LogicBlock(String name, String instance, int instanceNumber,  String mode, int level, LogicBlock parent){
		this.name = name;
		this.instance = instance;
		this.instanceNumber = instanceNumber;
		this.mode = mode;
		
		this.inputs = new HashMap<String, String[]>();
		this.outputs = new HashMap<String, String[]>();
		this.clocks = new HashMap<String, String[]>();
		
		this.childBlocks = new ArrayList<LogicBlock>();
	}
	
	//SET PARAMETERS
	public void setName(){
		this.name = "lb" + LogicBlock.num;
		LogicBlock.num += 1;
	}
	public void setInstanceNumber(int instanceNumber){
		this.instanceNumber = instanceNumber;
	}
	
	//HAS PARAMETER
	public boolean hasName(){
		return !(this.name == null);
	}
	public boolean hasInstance(){
		return !(this.instance == null);
	}
	public boolean hasMode(){
		return !(this.mode == null);
	}
	public boolean isEmpty(){
		return !this.hasName();
	}
	
	//GET PARAMETER
	public String getName(){
		if(this.hasName()){
			return this.name;
		}else{
			ErrorLog.print("This logic block has no name");
			return null;
		}
	}
	public String getMode(){
		if(this.hasMode()){
			return this.mode;
		}else{
			ErrorLog.print("This logic block has no mode");
			return null;
		}
	}
	public String getInstance(){
		if(this.hasInstance()){
			return this.instance;
		}else{
			ErrorLog.print("This logic block has no instance");
			return null;
		}
	}
	public String[] getInputs(String port){
		if(this.inputs.containsKey(port)){
			return this.inputs.get(port);
		}else{
			ErrorLog.print("Input port " + port + " not found");
			return null;
		}
	}
	public void setInput(String port, int pos, String name){
		if(this.inputs.containsKey(port)){
			if(this.inputs.get(port).length > pos){
				this.inputs.get(port)[pos] = name;
			}else{
				ErrorLog.print("Input port " + port + " only has " + this.inputs.get(port).length + " positions, which is less than " + pos);
			}
		}else{
			ErrorLog.print("Input port " + port + " not found");
		}
	}
	
	//CHILD LOGIC BLOCKS
	public ArrayList<LogicBlock> getNonEmptyChildBlocks(){
		ArrayList<LogicBlock> result = new ArrayList<LogicBlock>();
		for(LogicBlock child:this.childBlocks){
			if(!child.isEmpty()){
				result.add(child);
			}
		}
		return result;
	}
	public int numberOfNonEmptyChildBlocks(){
		int res = 0;
		for(LogicBlock child:this.childBlocks){
			if(!child.isEmpty()){
				res += 1;
			}
		}
		return res;
	}
	
	//IO SPECIFIC FUNCTIONS
	public void removeInpadBlock(){
		boolean inpadRemoved = false;
		for(LogicBlock child:this.childBlocks){
			if(child.hasMode()){
				if(child.getMode().equals("inpad")){
					if(!inpadRemoved){
						inpadRemoved = true;
						if(this.outputs.containsKey("core_out")){
							boolean pinRemoved = false;
							for(int i=0; i<this.outputs.get("core_out").length; i++){
								if(this.outputs.get("core_out")[i].contains("pad")){
									if(!pinRemoved){
										this.outputs.get("core_out")[i] = "open";
										pinRemoved = true;
									}else{
										ErrorLog.print("Already pin from inpad removed in io block");
									}
								}
							}
							if(!pinRemoved){
								ErrorLog.print("No pin from inpad removed in io block");
							}
						}else{
							ErrorLog.print("No core_out port found on output");
						}
						child.name = null;
						child.mode = null;
						child.inputs = new HashMap<String, String[]>();
						child.outputs = new HashMap<String, String[]>();
						child.clocks = new HashMap<String, String[]>();
						child.childBlocks = new ArrayList<LogicBlock>();
					}else{
						ErrorLog.print("Inpad already removed");
					}
				}
			}
		}
		if(!inpadRemoved){
			ErrorLog.print("No inpad removed");
		}
	}
	public void removeOutpadBlock(){
		boolean outpadRemoved = false;
		for(LogicBlock child:this.childBlocks){
			if(child.hasMode()){
				if(child.getMode().equals("outpad")){
					if(!outpadRemoved){
						outpadRemoved = true;
						if(this.inputs.containsKey("core_in")){
							this.inputs.get("core_in")[child.getCoreInPosition()] = "open";
						}else{
							ErrorLog.print("No core_in port found on input");
						}
						child.name = null;
						child.mode = null;
						child.inputs = new HashMap<String, String[]>();
						child.outputs = new HashMap<String, String[]>();
						child.clocks = new HashMap<String, String[]>();
						child.childBlocks = new ArrayList<LogicBlock>();
					}else{
						ErrorLog.print("Outpad already removed");
					}
				}
			}
		}
		if(!outpadRemoved){
			ErrorLog.print("No outpad removed");
		}
	}
	public int getCoreInPosition(){
		if(this.hasMode()){
			if(this.getMode().equals("outpad")){
				if(this.inputs.size() == 1){
					if(this.inputs.containsKey("drive_off_chip")){
						if(this.inputs.get("drive_off_chip").length == 1){
							String pin = this.inputs.get("drive_off_chip")[0];
							while(pin.contains("\t")){
								pin = pin.replace("\t", "");
							}
							pin = pin.replace("io.core_in[", "");
							pin = pin.replace("]->drive_off_chip", "");
							while(pin.contains(" ")){
								pin = pin.replace(" ", "");
							}
							return Integer.parseInt(pin);
						}
					}
				}
			}
		}
		ErrorLog.print("Postition not found");
		return -1;
	}
	
	//ADD
	public void addInput(String port, String[] pins){
		this.inputs.put(port, pins);
	}
	public void addOutput(String port, String[] pins){
		this.outputs.put(port, pins);
	}
	public void addClock(String port, String[] pins){
		this.clocks.put(port, pins);
	}
	public void addChildBlock(LogicBlock childBlock){
		this.childBlocks.add(childBlock);
	}
	
	//TO NET STRING
	public String toNetString(int tabs){
		StringBuilder sb = new StringBuilder();
		
		sb.append(Util.tabs(tabs));
		sb.append("<block name=\"");
		if(this.hasName()){
			sb.append(this.name);
		}else{
			sb.append("open");
		}
		sb.append("\"");
		if(this.hasInstance()){
			sb.append(" instance=\"");
			sb.append(this.instance);
			sb.append("[");
			sb.append(this.instanceNumber);
			sb.append("]\"");
		}else{
			ErrorLog.print("Each complex block should have an instance");
		}
		if(this.hasMode()){
			sb.append(" mode=\"");
			sb.append(this.mode);
			sb.append("\"");
		}
		
		if(this.isEmpty()){
			if(!this.inputs.isEmpty())ErrorLog.print("Open block should not have inputs");
			if(!this.outputs.isEmpty())ErrorLog.print("Open block should not have outputs");
			if(!this.clocks.isEmpty())ErrorLog.print("Open block should not have clocks");
			if(!this.childBlocks.isEmpty())ErrorLog.print("Open block should not child blocks");
			sb.append("/>");
			sb.append("\n");
		}else{
			sb.append(">");
			sb.append("\n");
			
			//Inputs
			sb.append(Util.tabs(tabs+1));
			sb.append("<inputs>");
			sb.append("\n");
			if(!this.inputs.isEmpty()){
				for(String inputPort:this.inputs.keySet()){
					sb.append(Util.tabs(tabs+2));
					sb.append("<port name=\"");
					sb.append(inputPort);
					sb.append("\">");
					for(String inputPin:this.inputs.get(inputPort)){
						sb.append(inputPin + " ");
					}
					sb.append("</port>");
					sb.append("\n");
				}
			}
			sb.append(Util.tabs(tabs+1));
			sb.append("</inputs>");
			sb.append("\n");

			//Outputs
			sb.append(Util.tabs(tabs+1));
			sb.append("<outputs>");
			sb.append("\n");
			if(!this.outputs.isEmpty()){
				for(String outputPort:this.outputs.keySet()){
					sb.append(Util.tabs(tabs+2));
					sb.append("<port name=\"");
					sb.append(outputPort);
					sb.append("\">");
					for(String outputPin:this.outputs.get(outputPort)){
						sb.append(outputPin + " ");
					}
					sb.append("</port>");
					sb.append("\n");
				}
			}
			sb.append(Util.tabs(tabs+1));
			sb.append("</outputs>");
			sb.append("\n");
			
			//Clocks
			sb.append(Util.tabs(tabs+1));
			sb.append("<clocks>");
			sb.append("\n");
			if(!this.clocks.isEmpty()){
				for(String clockPort:this.clocks.keySet()){
					sb.append(Util.tabs(tabs+2));
					sb.append("<port name=\"");
					sb.append(clockPort);
					sb.append("\">");
					for(String clockPin:this.clocks.get(clockPort)){
						sb.append(clockPin + " ");
					}
					sb.append("</port>");
					sb.append("\n");
				}
			}
			sb.append(Util.tabs(tabs+1));
			sb.append("</clocks>");
			sb.append("\n");
			
			for(LogicBlock childBlock:this.childBlocks){
				sb.append(childBlock.toNetString(tabs+1));
			}
			
			sb.append(Util.tabs(tabs));
			sb.append("</block>");
			sb.append("\n");
		}
		return sb.toString();
	}
}