package pack.architecture;

import java.util.HashMap;
import java.util.Set;

import pack.util.ErrorLog;

public class Line {
	private String originalLine;
	private String type;
	private HashMap<String,String> properties;
	
	public Line(String line){
		this.originalLine = line;
		
		line = line.replace("\t", "");
		while(line.contains("  ")){
			line = line.replace("  ", " ");
		}
		line = line.replace(" <", "<");
		line = line.replace("> ", ">");
		
		line = line.replace("<", "");
		line = line.replace("/>", "");
		line = line.replace(">", "");
		
		boolean cut = true;
		char[] array = line.toCharArray();
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<array.length;i++){
			if(array[i] == '\"'){
				if(cut){
					cut = false; 
				}else {
					cut = true;
				}
			}
			if(array[i] == ' '){
				if(cut){
					sb.append('@');
				}else{
					sb.append(array[i]);
				}
			}else{
				sb.append(array[i]);
			}
		}
		line = sb.toString();
		
		this.properties = new HashMap<String, String>();
		String[] words = line.split("@");
		this.type = words[0];
		for(int i=1; i<words.length; i++){
			String[] parts = words[i].split("=");
			if(parts.length != 2){
				ErrorLog.print("The number of parts is equal to " + parts.length + " | " + words[i] + " | " + this.originalLine);
			}
			this.properties.put(parts[0], parts[1].replace("\"",""));
		}
	}
	public String get_type(){
		return this.type;
	}
	public Set<String> get_properties(){
		return this.properties.keySet();
	}
	public boolean has_property(String property){
		return this.properties.containsKey(property);
	}
	public String get_value(String property){
		if(this.has_property(property)){
			return this.properties.get(property);
		}else{
			ErrorLog.print("This line has no propery " + property + " | " + this.originalLine);
			return null;
		}	
	}
	public String get_line(){
		return this.originalLine;
	}
}
