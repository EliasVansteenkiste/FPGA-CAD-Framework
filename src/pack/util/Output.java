package pack.util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import pack.main.Simulation;

public class Output {
	private static boolean logfile;
	private static String fileName;
	private static String outputBuffer;
	
	public static void path(Simulation simulation){
		if(simulation.getBooleanValue("logfile")){
			Output.logfile = true;
			DateFormat dateFormat = new SimpleDateFormat("ddMMM_HHmm");
			Calendar cal = Calendar.getInstance();
			Output.fileName = simulation.getStringValue("result_folder") + "MultiPart" + "_" + dateFormat.format(cal.getTime()) + ".log";
			Output.outputBuffer = new String();
		}else{
			Output.logfile = false;
		}
	}
	public synchronized static void flush(){
		if(Output.logfile){
			Output.writeToFile();
		}
	}
	public synchronized static void print(String line){
		if(Output.logfile){
			Output.outputBuffer += line;
			if(Output.outputBuffer.length() > 1024){
				Output.writeToFile();
			}
		}
		System.out.print(line);
	}
	private static void writeToFile(){
		FileWriter file = null;
		try {
			file = new FileWriter(Output.fileName,true);
			file.write(Output.outputBuffer);
			file.flush();
			file.close();
		} catch (IOException e) {
			System.err.println("Error in writing console output to file " + Output.fileName + ": " + e.getMessage());
			e.printStackTrace();
		}
		Output.outputBuffer = new String();
	}
	public static void println(String line){
		Output.print(line + "\n");
	}
	public static void newLine(){
		Output.print("\n");
	}
}