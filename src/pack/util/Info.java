package pack.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pack.main.Simulation;

public class Info {
	private static Map<String,ArrayList<String>> info = new HashMap<>();
	private static boolean enabled = false;
	
	public static void enabled(boolean enabled){
		Info.enabled = enabled;
	}
	
	public synchronized static void add(String type, String line){
		if(Info.enabled){
			if(Info.info.containsKey(type)){
				Info.info.get(type).add(line);
			}else{
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(line);
				Info.info.put(type, temp);
			}
		}
	}
	public static void finish(Simulation simulation){
		if(Info.enabled){
			for(String key:Info.info.keySet()){
				FileWriter file = null;
				try {
					file = new FileWriter(simulation.getStringValue("result_folder") + "stats" + "." + key.replace(" ", "") + ".txt");
					for(String line:Info.info.get(key)){
						file.write(line + "\n");
					}
					file.flush();
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Info.info = new HashMap<String,ArrayList<String>>();
		}
	}
}