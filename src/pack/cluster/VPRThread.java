package pack.cluster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import pack.main.Simulation;
import pack.util.Info;
import pack.util.Output;
import pack.util.Util;

public class VPRThread {
	private String folder;
	private int thread;
	
	private Process proc;
	
	private Simulation simulation;
	
	private String run;
	
	private int size;

	public VPRThread(String folder, int thread, Simulation simulation){
		this.folder = folder;
		this.thread = thread;
		
		this.simulation = simulation;
		
		this.run = new String();
	}
	public boolean isRunning() {
	    try {
	       this.proc.exitValue();
	       
	       BufferedReader reader =  new BufferedReader(new InputStreamReader(this.proc.getInputStream()));
	      
	       String line = reader.readLine();
	       double runtime = 0.0;
	       while(line != null){
	    	   if(line.contains("Packing took")){
	    		   line = line.replace("Packing took ", "");
	    		   line = line.replace(" seconds", "");
	    		   runtime = Double.parseDouble(line);
	    	   }
	    	   line = reader.readLine();
	       }
	       String output = this.size + "\t" + runtime + "\t" + runtime/this.size * 1000;
	       output = output.replace(".", ",");
	       Info.add("RuntimePerBlock", output);
	       return false;
	    } catch (Exception e) {
	       return true;
	    }
	}
	public void run(int size, int area){
		this.run = new String();
		this.run += Util.localFolder() + "vtr-no-output/vpr/vpr" + " ";
    	this.run += Util.run_folder() + "arch.pack.xml" + " ";
    	this.run += this.folder + this.simulation.benchmark() + "_" + Util.getSimulationId() + "_" + this.thread + " ";
    	this.run += "-pack" + " ";

    	ProcessBuilder pb = new ProcessBuilder(this.run.split(" "));	
    	try {
			this.proc = pb.start();
        }catch (IOException e) {
			Output.println("Problems with vpr process");
			e.printStackTrace();
		}
    	
		Output.println("\t\t" + this.simulation.benchmark() + " | " + "Thread: " + Util.fill(this.thread,2) + " | Blocks: " + Util.fill(size,5) + " | Area: " + area);
		
		this.size = size;
		
    	//System.out.println("\t\t\t" + this.run);
	}
	public String getCommand(){
		return this.run;
	}
	public int getThread(){
		return this.thread;
	}
}