package pack.main;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import pack.util.ErrorLog;

public class Simulation{
	private LinkedHashMap<String, Option> options;
	private int maxNameSize;
	private int maxDescriptionSize;
	private int simulationID;
	
	public Simulation(){
		this.options = new LinkedHashMap<String, Option>();
		
		this.addOption(new Option("result_folder","description_todo", String.class));//TODO Description
		this.addOption(new Option("vpr_folder","description_todo", String.class));//TODO Description
		this.addOption(new Option("hmetis_folder","description_todo", String.class));//TODO Description
		
		this.addOption(new Option("circuit","description_todo", String.class));//TODO Description
		this.addOption(new Option("architecture","description_todo", String.class));//TODO Description
		
		this.addOption(new Option("max_pack_size","The maxmimum number of blocks in the independent subcircuits for seed based packing", 2500));
		this.addOption(new Option("num_threads","The number of available threads", 4));
		this.addOption(new Option("max_fanout","The maximum fanout of the added nets during partitioning", 100));
		this.addOption(new Option("hmetis_quality","description_todo", 2));//TODO Descriptions
		this.addOption(new Option("unbalance_factor","description_todo", 25));//TODO Description
		this.addOption(new Option("min_crit","description_todo", 0.7));//TODO Description
		this.addOption(new Option("max_per_crit_edge","description_todo", 20));//TODO Description
		this.addOption(new Option("timing_weight","description_todo", 10));//TODO Description
		this.addOption(new Option("multiply_factor","description_todo", 1.0));//TODO Description
		this.addOption(new Option("fixed_size","description_todo", true));//TODO Description
		
		this.addOption(new Option("logfile","Print console output to logfile", false));
		
		this.addOption(new Option("area_exponent_alpha","Scaling power exponent for area", 0.0));
		this.addOption(new Option("timing_edge_weight_update", "Update the weight on the critical paths with a cut edge", true));

		this.simulationID = (int)Math.round(Math.random()*1000000);
	}
	public boolean hasOption(Option option){
		return this.hasOption(option.getName());
	}
	public boolean hasOption(String name){
		if(this.options.containsKey(name)){
			return true;
		}
		return false;
	}
	public void addOption(Option option){
		if(this.hasOption(option)){
			ErrorLog.print("Duplicate option: " + option.getName() + " " + option.getDescription());
		}
		this.options.put(option.getName(), option);
		if(option.getName().length() > this.maxNameSize){
			this.maxNameSize = option.getName().length();
		}
		if(option.getDescription().length() > this.maxDescriptionSize){
			this.maxDescriptionSize = option.getDescription().length();
		}
	}
	public Option getOption(String name){
		if(this.hasOption(name)){
			return this.options.get(name);
		}else{
			ErrorLog.print("Option " + name + " not found");
		}
		return null;
	}

	

	public void setOptionValue(String name, Object value){
		if(this.hasOption(name)){
			this.options.get(name).setValue(value);
		}else{
			ErrorLog.print("Option " + name + " not found");
		}
	}
	
	public String getStringValue(String name){
		Option option = this.getOption(name);
		if(!option.getType().equals(String.class)) {
        	ErrorLog.print("Option " + option.getName() + " is not of class " + String.class + ", class is equal to " + option.getType());
    	}
    	return (String)option.getValue();
	}
	public Double getDoubleValue(String name){
		Option option = this.getOption(name);
		if(!option.getType().equals(Double.class)) {
        	ErrorLog.print("Option " + option.getName() + " is not of class " + Double.class + ", class is equal to " + option.getType());
    	}
    	return (Double)option.getValue();
	}
	public Integer getIntValue(String name){
		Option option = this.getOption(name);
		if(!option.getType().equals(Integer.class)) {
        	ErrorLog.print("Option " + option.getName() + " is not of class " + Integer.class + ", class is equal to " + option.getType());
    	}
    	return (Integer)option.getValue();
	}
	public Boolean getBooleanValue(String name){
		Option option = this.getOption(name);
		if(!option.getType().equals(Boolean.class)) {
        	ErrorLog.print("Option " + option.getName() + " is not of class " + Boolean.class + ", class is equal to " + option.getType());
    	}
    	return (Boolean)option.getValue();
	}
	
	public int getSimulationID(){
		return this.simulationID;
	}
	
	public boolean isDouble(String word){
		if(Pattern.matches("([0-9]*)\\.([0-9]*)", word)){
			return true;
		}else if(Pattern.matches("([0-9]*)\\,([0-9]*)", word)){
			return true;
		}
		return false;
	}
	public boolean isInt(String word){
		if(Pattern.matches("([0-9]*)", word)){
			return true;
		}
		return false;
	}
	public boolean isBoolean(String word){
		if(word.equals("true")){
			return true;
		}else if(word.equals("True")){
			return true;
		}else if(word.equals("false")){
			return true;
		}else if(word.equals("False")){
			return true;
		}
		return false;
	}
	public void parseArgs(String[]args){
		for(String arg:args){
			if(arg.equals("-h") || arg.equals("-help")){
				System.out.println(this.toString());
				System.exit(0);
			}
		}
		for(int i=0;i<args.length;i++){
			if(args[i].contains("-")){
				String name = args[i].replace("-", "");
				String value = args[i+1];
				if(this.isDouble(value)){
					this.getOption(name).setValue(Double.parseDouble(value));
				}else if(this.isInt(value)){
					this.getOption(name).setValue(Integer.parseInt(value));
				}else if(this.isBoolean(value)){
					this.getOption(name).setValue(Boolean.parseBoolean(value));
				}else{
					this.getOption(name).setValue(value);
				}
				i += 1;
			}
		}
	}

	public String toString(){
		String output = new String();
		output += "\tMultiPart: a partitioning based packing tool\n\n";
		output += "\tCommand line options:\n\n";
		String name = "OPTION";
		while(name.length() < this.maxNameSize) name = name + " ";
		String description = "DESCRIPTION";
		while(description.length() < this.maxDescriptionSize) description = description + " ";
		output += "\t\t" + name + "    " + description + "    " + "DEFAULT" + "\n\n";
		
		for(Option option:this.options.values()){
			name = option.getName();
			while(name.length() < this.maxNameSize) name = name + " ";
			description = option.getDescription();
			while(description.length() < this.maxDescriptionSize) description = description + " ";
			output += "\t\t-" + name + "    " + description;
			if(option.hasDefaultValue()) output += "    " + option.getDefaultValue();
			output += "\n";
		}
		return output;
	}
	public String toValueString(){
		String output = new String();
		for(Option option:this.options.values()){
			String name = option.getName();
			while(name.length() < this.maxNameSize) name = name + " ";
			output += name + "    " + option.getValue() + "\n";
		}
		return output;
	}
}