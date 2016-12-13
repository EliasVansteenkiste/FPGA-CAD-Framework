package pack.util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Output {
	private static String fileName;
	private static ArrayList<String> files;
	private static String outputBuffer;
	
	public static void path(String simulationFile){
		DateFormat dateFormat = new SimpleDateFormat("ddMMM_HHmm");
		Calendar cal = Calendar.getInstance();
		Output.fileName = Util.localFolder() + "simulation" + "/" + simulationFile + "_" + dateFormat.format(cal.getTime()) + ".txt";
		Output.outputBuffer = new String();
		Output.files = new ArrayList<String>();
	}
	public static void addFile(String file){
		Output.files.add(file);
	}
	public static void removeFile(String file){
		Output.files.remove(file);
	}
	public static void flush(){
		Output.writeToFile();
	}
	public synchronized static void print(String line){
		Output.outputBuffer += line;
		if(Output.outputBuffer.length() > 1024){
			Output.writeToFile();
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
		for(String otherFile:Output.files){
			try {
				file = new FileWriter(otherFile,true);
				file.write(Output.outputBuffer);
				file.flush();
				file.close();
			} catch (IOException e) {
				System.err.println("Error in writing console output to file " + otherFile + ": " + e.getMessage());
				e.printStackTrace();
			}
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