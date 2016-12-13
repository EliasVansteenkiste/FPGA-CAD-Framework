package pack.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import pack.main.Simulation;
import pack.util.Output;

public class Util {
	private static String localFolder;
	private static String runFolder;
	private static String benchFolder;
	private static int simulationID = 0;
	private static HashMap<String,String[]> blifSpecificLine;
	public static boolean fileExists(String file){
		File f = new File(file);
		if(f.exists() && !f.isDirectory()) { 
		    return true;
		}else{
			return false;
		}
	}
	public static boolean folderExists(String folder){
		File f = new File(folder);
		if(f.exists() && f.isDirectory()) { 
		    return true;
		}else{
			return false;
		}
	}
	public static void makeFolder(String folder){
		if(!folderExists(folder)){
			File dir = new File(folder);
			dir.mkdir();
		}else{
			Output.println("Folder already exists");
		}
	}
	public static void copyAndMixBlifFile(String originalFile, String newFile){
		Timing t = new Timing();
		t.start();
		Output.print("Copy blif file and mix up input and output pins");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(originalFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
		
			ArrayList<String> inputs = new ArrayList<String>();
			ArrayList<String> outputs = new ArrayList<String>();
			
			boolean first_input = true;
			boolean first_output = true;

			String line = reader.readLine();
			while (line != null) {
				if(line.contains(".inputs") && first_input){
					first_input = false;
					do{
						for(String input:line.replace(".inputs","").replace("\\"," ").split(" ")){
							inputs.add(input);
						}
						line = reader.readLine();
					}while(!Util.lineContainsBlifPrimitive(line));
					
					int no = 0;
					writer.write(".inputs");
					while(!inputs.isEmpty()){
						int pos = (int)Math.floor(Math.random()*inputs.size());
						String input = inputs.remove(pos);
						if(no > 50){
							writer.write(" \\");
							writer.newLine();
							no = 0;
						}
						writer.write(" " + input);
						no += input.length();
					}
					writer.newLine();
					writer.newLine();
				}
				if(line.contains(".outputs") && first_output){
					first_output = false;
					do{
						for(String output:line.replace(".outputs","").replace("\\"," ").split(" ")){
							outputs.add(output);
						}
						line = reader.readLine();
					}
					while(!Util.lineContainsBlifPrimitive(line));
					
					int no = 0;
					writer.write(".outputs");
					while(!outputs.isEmpty()){
						int pos = (int)Math.floor(Math.random()*outputs.size());
						String output = outputs.remove(pos);
						if(no > 50){
							writer.write(" \\");
							writer.newLine();
							no = 0;
						}
						writer.write(" " + output);
						no += output.length();
					}
					writer.newLine();
					writer.newLine();
				}
				writer.write(line);
				writer.newLine();
				line = reader.readLine();
			}
			reader.close();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.err.println("Problem in reading the BLIFfile: " + e.getMessage());
			e.printStackTrace();
		}
		t.end();
		Output.println(" | Took " + t.toString());
	}
	public static void copyFile(String originalFile, String newFile){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(originalFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));

			String line = reader.readLine();
			while (line != null) {
				writer.write(line);
				writer.newLine();
				line = reader.readLine();
			}
			reader.close();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.err.println("Problem in copy file: " + e.getMessage());
			e.printStackTrace();
		}
	}
	public static void copyArchFile(boolean fixedSize, int sizeX, int sizeY){
		if(fixedSize){
			//<layout width="17" height="17"/>
			Output.println("Copy arch file and set size to " + sizeX + " x " + sizeY);
		}else{
			//<layout auto="1.35"/>
			Output.println("Copy arch file and set ratio to 1,35");
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(Util.localFolder() + "architecture/stratixiv_arch.timing.xml"));
			BufferedWriter writer = new BufferedWriter(new FileWriter(Util.run_folder() + "stratixiv_arch.timing.xml"));
			String line = reader.readLine();
			while (line != null) {
				if(fixedSize){
					if(line.contains("<layout auto=\"1.35\"/>")){
						line = line.replace("<layout auto=\"1.35\"/>", "<layout width=\"" + sizeX + "\" height=\"" + sizeY + "\"/>");
					}
				}
				writer.write(line);
				writer.newLine();
				line = reader.readLine();
			}
			reader.close();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.err.println("Problem in copy arch file: " + e.getMessage());
			e.printStackTrace();
		}
	}
	private static boolean lineContainsBlifPrimitive(String line){
		ArrayList<String> blifPrimitives = Util.getBlifPrimitives();
		for(String blifPrimitive:blifPrimitives){
			if(line.contains(blifPrimitive)){
				return true;
			}
		}
		return false;
	}
	private static ArrayList<String> getBlifPrimitives(){
		ArrayList<String> blifPrimitives = new ArrayList<String>();
		blifPrimitives.add(".model");
		blifPrimitives.add(".inputs");
		blifPrimitives.add(".outputs");
		blifPrimitives.add(".names");
		blifPrimitives.add(".latch");
		blifPrimitives.add(".subckt");
		blifPrimitives.add(".blackbox");
		blifPrimitives.add(".end");
		return blifPrimitives;
	}
	/////////////////////////////////
	//////       FOLDERS       //////
	/////////////////////////////////
	public static void setLocalFolder(){
		if(Util.folderExists("/Users/drvercru/Documents/MultiPart/")){
			Util.localFolder = "/Users/drvercru/Documents/MultiPart/";
		}else{
			Path currentRelativePath = Paths.get("");
			Util.localFolder = currentRelativePath.toAbsolutePath().toString() + "/";
		}
		System.out.println("Current relative path is: " + Util.localFolder());
	}
	public static String localFolder(){
		return Util.localFolder;
	}
	public static void setBenchmarkFolder(String architecture){
		Util.benchFolder = localFolder() + "benchmarks" + "/" + architecture + "/";
	}
	public static String benchmarkFolder(){
		return Util.benchFolder;
	}
	public static String archFolder(){
		return localFolder() + "architecture/";
	}
	public static String resultFolder(){
		return localFolder() + "results/";
	}
	public static String makeRunFolder(String blif){
		String benchFolder = Util.resultFolder() + blif + "/";
		String runFolder = new String();
		if(!Util.folderExists(benchFolder)){
			Util.makeFolder(benchFolder);
		}
		for(int run=0; run<500; run++){
			runFolder = benchFolder + "run" + run + "/";
			if(!Util.folderExists(runFolder)){
				Util.makeFolder(runFolder);
				break;
			}
		}
		Util.runFolder = runFolder;
		return runFolder;
	}
	public static String run_folder(){
		return Util.runFolder;
	}
	/////////////////////////////////
	//////     END FOLDERS     //////
	/////////////////////////////////
	
	//MATH FUNCTIONS
	public static double round(double number, int digits){
		if(digits > 6){
			return number;
		}else{
			double roundNumber = number;
			roundNumber *= Math.pow(10, digits);
			roundNumber = Math.round(roundNumber);
			roundNumber /= Math.pow(10, digits);
			return roundNumber;
		}
	}
	
	//PRINT FUNCTIONS
	public static String tabs(int tabs){
		if(tabs == 0){
			return "";
		}else if(tabs == 1){
			return "  ";
		}else if(tabs == 2){
			return "    ";
		}else if(tabs == 3){
			return "      ";
		}else if(tabs == 4){
			return "        ";
		}else if(tabs == 5){
			return "          ";
		}else if(tabs == 6){
			return "            ";
		}else if(tabs == 7){
			return "              ";
		}else if(tabs == 8){
			return "                ";
		}else if(tabs == 9){
			return "                  ";
		}else if(tabs == 10){
			return "                    ";
		}else if(tabs == 11){
			return "                      ";
		}else if(tabs == 12){
			return "                        ";
		}else if(tabs == 13){
			return "                          ";
		}else if(tabs == 14){
			return "                            ";
		}else if(tabs == 15){
			return "                              ";
		}else if(tabs == 16){
			return "                                ";
		}else{
			ErrorLog.print("To many tabs: " + tabs);
			return "";
		}
	}
	
	//TOSTRING
	public static String str(int i){
		return "" + i + "";
	}
	public static String str(double i){
		return "" + i + "";
	}
	
	public static String fill(String s, int length){
		while(s.length() < length){
			s += " ";
		}
		return s;
	}
	public static String fill(int i, int length){
		String s = Util.str(i);
		while(s.length() < length){
			s += " ";
		}
		return s;
	}
	public static String fill(double d, int length){
		String s = Util.str(d);
		while(s.length() < length){
			s += " ";
		}
		return s;
	}

	public static String parseDigit(double digit){
		if(digit > 1000000){
			return Util.round((1.0*digit)/1000000,1) + "M";
		}else if(digit > 1000){
			return Util.round((1.0*digit)/1000,1) + "K";
		}else{
			return Util.str(digit);
		}
	}
	
	//READ BLIF SPECIFIC STATISTICS
	private static String[] readBlifLine(Simulation simulation){
		try {
			BufferedReader bw = new BufferedReader(new FileReader(Util.benchmarkFolder() + "benchSpecificInfo.txt"));
			String line = bw.readLine();
			while(line != null){
				if(line.contains(simulation.benchmark())){
					line = line.replace("\t", "@");
					line = line.replace(" ", "@");
					while(line.contains("@@")){
						line = line.replace("@@", "@");
					}
					String[] res = line.split("@");
					if(res[0].equals(simulation.benchmark())){
						bw.close();
						return res;
					}
				}
				line = bw.readLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ErrorLog.print("Blif " + simulation.benchmark() + " not found in benchSpecificInfo.txt");
		return null;
	}
	private static String[] getBlifLine(Simulation simulation){
		if(Util.blifSpecificLine == null){
			Util.blifSpecificLine = new HashMap<String,String[]>();
			String[] line = Util.readBlifLine(simulation);
			Util.blifSpecificLine.put(simulation.benchmark(), line);
			if(!line[0].equals(simulation.benchmark()))ErrorLog.print(line[0] + " does not equal " + simulation.benchmark());
			return line;
		}else if(!Util.blifSpecificLine.containsKey(simulation.benchmark())){
			String[] line = Util.readBlifLine(simulation);
			Util.blifSpecificLine.put(simulation.benchmark(), line);
			if(!line[0].equals(simulation.benchmark()))ErrorLog.print(line[0] + " does not equal " + simulation.benchmark());
			return line;
		}else{
			String[] line = Util.blifSpecificLine.get(simulation.benchmark());
			if(!line[0].equals(simulation.benchmark()))ErrorLog.print(line[0] + " does not equal " + simulation.benchmark());
			return line;
		}
	}
	public static int sizeX(Simulation simulation){
		String[] blifLine =  Util.getBlifLine(simulation);
		return Integer.parseInt(blifLine[1]);
	}
	public static int sizeY(Simulation simulation){
		String[] blifLine =  Util.getBlifLine(simulation);
		return Integer.parseInt(blifLine[2]);
	}
	public static int channelWidth(Simulation simulation){
		String[] blifLine =  Util.getBlifLine(simulation);
		return Integer.parseInt(blifLine[3]);
	}
	public static int getSimulationId(){
		if(Util.simulationID == 0){
			while(Util.simulationID == 0){
				Util.simulationID = (int)Math.round(Math.random()*1000000);
			}
		}
		return Util.simulationID;
	}
	public static boolean isMoleculeType(String type){
		if(type.equals("HALF_DSP")){
			return true;
		}else if(type.equals("DSP")){
			return true;
		}else if(type.equals("RAM")){
			return true;
		}else if(type.equals("CARRY_CHAIN")){
			return true;
		}else if(type.equals("SHARE_CHAIN")){
			return true;
		}else if(type.equals("MWR")){
			return true;
		}else{
			return false;
		}
	}
}