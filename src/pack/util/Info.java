package pack.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import pack.main.Simulation;

public class Info {
	private static HashMap<String,ArrayList<String>> info = new HashMap<String,ArrayList<String>>();
	public synchronized static void add(String type, String line){
		if(Info.info.containsKey(type)){
			Info.info.get(type).add(line);
		}else{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(line);
			Info.info.put(type, temp);
		}
	}
	public static void finish(Simulation simulation){
		for(String key:Info.info.keySet()){
			FileWriter file = null;
			try {
				file = new FileWriter(simulation.getStringValue("result_folder") + key.replace(" ", "") + ".txt");
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