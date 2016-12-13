package pack.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import pack.main.Simulation;

public class RunVPR {
	private String folder;
	private String architecture;
	private String benchmark;
	private Simulation simulation;
	
	private boolean pack;
	private boolean place;
	private boolean route;
	
	private int place_seed = 0;
	private int routeChanWidth;
	
	public RunVPR(){
		this.pack = false;
		this.place = false;
		this.route = false;
		
		this.routeChanWidth = -1;
	}
	public RunVPR(String folder, Simulation simulation){
		this.folder = folder;
		this.simulation = simulation;
		
		if(this.simulation.baseline()){
			this.enable_pack();//SET THIS ON WHEN ONLY VPR
		}
		
		if(this.simulation.place()){
			this.enable_place();
			this.random_place_seed();
		}
		
		if(this.simulation.route()){
			this.enable_route();
			this.set_channel_width(Util.channelWidth(this.simulation));
		}
		
		this.set_architecture(this.simulation.architecture());
		this.set_benhmark(this.simulation.benchmark());
		
		if(this.pack || this.place || this.route){
			this.run();
		}
	}
	public void random_place_seed(){
		this.place_seed = (int)Math.round(Math.random()*1000);
	}
	public void set_architecture(String architecture){
		this.architecture = architecture;
	}
	public void set_benhmark(String benchmark){
		this.benchmark = benchmark;
	}
	public void enable_pack(){
		this.pack = true;
	}
	public void enable_place(){
		this.place = true;
	}
	public void enable_route(){
		this.route = true;
	}
	public void set_channel_width(int channelWidth){
		this.routeChanWidth = channelWidth;
	}
	
	public void run(){
		Runtime rt = Runtime.getRuntime();
        try{
        	String run = new String();
        	String vprLine = "VPR | ";
        	run += Util.localFolder() + "vtr-verilog-to-routing/" + "vpr/" + "vpr" + " ";
        	run += Util.run_folder() + this.architecture + " ";
        	
        	run += this.folder + benchmark + " ";

        	if(this.pack){
        		run += "-pack" + " ";
        		vprLine += "Pack ";
        	}
        	if(this.place){
        		run += "-place" + " ";
        		vprLine += "Place ";
        	}
        	if(this.route){
        		run += "-route" + " ";
        		run += "-max_router_iterations" + " " + "200" + " ";
        		vprLine += "Route ";
        	}
        	
        	if(this.routeChanWidth > 0){
        		run += "-route_chan_width" + " " + this.routeChanWidth + " ";
        	}
        	if(this.place_seed > 0) run += "-seed" + " " + this.place_seed + " ";
        	
        	//run += "-full_stats" + " ";
        	
        	Output.println(run);
    		DateFormat dateFormat = new SimpleDateFormat("dd MMMM HH:mm");
    		Calendar cal = Calendar.getInstance();
        	Output.println(vprLine + "| Started on " + dateFormat.format(cal.getTime()));
        	Output.newLine();
 
        	Process proc = rt.exec(run.split(" "));
            
        	boolean packingAdded = false;
        	boolean placementAdded = false;
        	boolean routingAdded = false;
        	boolean flowAdded = false;
        	boolean critAdded = false;
        	boolean pCritAdded = false;
        	boolean wireAdded = false;
        	boolean pWireAdded = false;
        	boolean cWidthAdded = false;
        	boolean ptpAdded = false;
        	boolean rNetAdded = false;
        	boolean ioAdded = false;
        	boolean pllAdded = false;
        	boolean labAdded = false;
        	boolean dspAdded = false;
        	boolean m9kAdded = false;
        	boolean m144kAdded = false;
        	boolean sizeAdded = false;
        	
            boolean printOutput = true;
            if(printOutput){
    	        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    	        BufferedWriter bw = new BufferedWriter(new FileWriter(Util.run_folder() + "vpr_output.txt"));
    	        String line = null;			
    	        while ((line = reader.readLine())!= null) {
    	        	boolean print = true;
    	        	if(line.contains("Net is a constant generator")){
    	        		print = false;
    	        	}else if(line.contains("Warning") && line.contains("constant generator")){
    	        		print = false;
    	        	}else if(line.contains("Warning") && line.contains("has no fanout")){
    	        		print = false;
    	        	}else if(line.contains("Warning") && line.contains("logical_block") && line.contains("has only")){
    	        		print = false;
    	        	}else if(line.contains("Removing input.")){
    	        		print = false;
    	        	}else if(line.contains("is a constant generator")){
    	        		print = false;
    	        	}else if(line.contains("Warning") && line.contains("has no edges")){
    	        		print = false;
    	        	}
    	        	if(print){
        	        	bw.write(line);
        	        	bw.newLine();
    	        	}
    	        	if(line.contains("Packing took")){
    	        		String value = line.split("took ")[1].split(" secon")[0];
    	        		value = Util.str(Util.round(Double.parseDouble(value),2));
    	        		Output.println("\tPacking took: " + value.replace(".", ",") + " s");
    	        		this.simulation.add_simulation_result("VPR Pack [s]", value);
    	        		packingAdded = true;
    	        	}else if(line.contains("Placement took")){
    	        		String value = line.split("took ")[1].split(" secon")[0];
    	        		value = Util.str(Util.round(Double.parseDouble(value),2));
    	        		Output.println("\tPlacement took: " + value.replace(".", ",") + " s");
    	        		this.simulation.add_simulation_result("VPR Place [s]", value);
    	        		placementAdded = true;
    	        	}else if(line.contains("Routing took")){
    	        		String value = line.split("took ")[1].split(" secon")[0];
    	        		value = Util.str(Util.round(Double.parseDouble(value),2));
    	        		Output.println("\tRouting took: " + value.replace(".", ",") + " s");
    	        		this.simulation.add_simulation_result("VPR Route [s]", value);
    	        		routingAdded = true;
    	        	}else if(line.contains("The entire flow of VPR took")){
    	        		String value = line.split("took ")[1].split(" secon")[0];
    	        		value = Util.str(Util.round(Double.parseDouble(value),2));
    	        		Output.println("\tVPR flow took: " + value.replace(".", ",") + " s");
    	        		this.simulation.add_simulation_result("VPR Flow [s]", value);
    	        		flowAdded = true;
    	        	}else if(line.contains("Final critical path")){
    	        		String value = line.split("critical path: ")[1].split(" ns")[0];
    	        		value = Util.str(Util.round(Double.parseDouble(value),2));
    	        		Output.println("\tCritical path: " + value.replace(".", ",") + " ns");
    	        		this.simulation.add_simulation_result("Critical path [ns]", value);
    	        		critAdded = true;
    	        	}else if(line.contains("Placement estimated critical path delay")){
    	        		String value = line.split("critical path delay: ")[1].split(" ns")[0];
    	        		value = Util.str(Util.round(Double.parseDouble(value),2));
    	        		Output.println("\tPlacement estimated critical path: " + value.replace(".", ",") + " ns");
    	        		this.simulation.add_simulation_result("Placement estimated critical path [ns]", value);     
    	        		pCritAdded = true;
    	        	}else if(line.contains("Total wirelength")){
    	        		String value = line.split("wirelength: ")[1].split(", average")[0];
    	        		Output.println("\tTotal wirelength: " + value);
    	        		this.simulation.add_simulation_result("Total wirelength", value);
    	        		wireAdded = true;
    	        	}else if(line.contains("BB estimate of min-dist (placement) wire length")){
    	        		String value = line.split("wire length: ")[1].replace(" ", "");
    	        		Output.println("\tPlacement estimated total wirelength: " + value);
    	        		this.simulation.add_simulation_result("Placement estimated total wirelength", value);
    	        		pWireAdded = true;
    	        	}else if(line.contains("Best routing used a channel width factor of")){
    	        		String value = line.split("routing used a channel width factor of ")[1];
    	        		Output.println("\tChannel width: " + value);
    	        		this.simulation.add_simulation_result("Channel width", value);
    	        		cWidthAdded = true;
    	        	}else if(line.contains("point to point connections in this circuit")){
    	        		String value = line.split("are ")[1].split(" point to point connections")[0];
    	        		Output.println("\tPointToPoint connections: " + value);
    	        		this.simulation.add_simulation_result("PointToPoint connections", value);
    	        		ptpAdded = true;
    	        	}else if(line.contains("Number of routed nets (nonglobal):")){
    	        		line = line.replace("(nonglobal):", "");
    	        		String value = line.split("routed nets  ")[1];
    	        		Output.println("\tRouted nets: " + value);
    	        		this.simulation.add_simulation_result("Routed nets", value);
    	        		rNetAdded = true;
    	        	}else if(line.contains("Netlist") && line.contains("blocks of type: io")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		String value = line.split("tlist")[1].split("blocksoftype")[0];
    	        		Output.println("\tIO Blocks: " + value);
    	        		this.simulation.add_simulation_result("IO Blocks", value);
    	        		ioAdded = true;
    	        	}else if(line.contains("Netlist") && line.contains("blocks of type: PLL")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		String value = line.split("tlist")[1].split("blocksoftype")[0];
    	        		Output.println("\tPLL Blocks: " + value);
    	        		this.simulation.add_simulation_result("PLL Blocks", value);
    	        		pllAdded = true;
    	        	}else if(line.contains("Netlist") && line.contains("blocks of type: LAB")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		String value = line.split("tlist")[1].split("blocksoftype")[0];
    	        		Output.println("\tLAB Blocks: " + value);
    	        		this.simulation.add_simulation_result("LAB Blocks", value);
    	        		labAdded = true;
    	        	}else if(line.contains("Netlist") && line.contains("blocks of type: DSP")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		String value = line.split("tlist")[1].split("blocksoftype")[0];
    	        		Output.println("\tDSP Blocks: " + value);
    	        		this.simulation.add_simulation_result("DSP Blocks", value);
    	        		dspAdded = true;
    	        	}else if(line.contains("Netlist") && line.contains("blocks of type: M9K")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		String value = line.split("tlist")[1].split("blocksoftype")[0];
    	        		Output.println("\tM9K Blocks: " + value);
    	        		this.simulation.add_simulation_result("M9K Blocks", value);
    	        		m9kAdded = true;
    	        	}else if(line.contains("Netlist") && line.contains("blocks of type: M144K")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		String value = line.split("tlist")[1].split("blocksoftype")[0];
    	        		Output.println("\tM144K Blocks: " + value);
    	        		this.simulation.add_simulation_result("M144K Blocks", value);
    	        		m144kAdded = true;
		        	}else if(line.contains("The circuit will be mapped into a")){
    	        		while(line.contains(" ")) line = line.replace(" ", "");
    	        		while(line.contains("\t")) line = line.replace("\t", "");
    	        		line = line.replace("Thecircuitwillbemappedintoa", "");
    	        		line = line.replace("arrayofclbs.", "");
		        		String sizeX = line.split("x")[0];
		        		String sizeY = line.split("x")[1];
		        		Output.println("\tSize: " + sizeX + " x " + sizeY);
		        		this.simulation.add_simulation_result("SizeX", sizeX);
		        		this.simulation.add_simulation_result("SizeY", sizeY);
		        		sizeAdded = true;
		        	}
    	        }
    	        bw.flush();
    	        bw.close();
    	        reader.close();
            }
            proc.waitFor();
            
            //If a simulation failed
	        if(!packingAdded){
	        	this.simulation.add_simulation_result("VPR Pack [s]", "NaN");
	        }
	        if(!placementAdded){
	        	this.simulation.add_simulation_result("VPR Place [s]", "NaN");
	        }
	        if(!routingAdded){
	        	this.simulation.add_simulation_result("VPR Route [s]", "NaN");
	        }
	        if(!flowAdded){
	        	this.simulation.add_simulation_result("VPR Flow [s]", "NaN");
	        }
	        if(!critAdded){
	        	 this.simulation.add_simulation_result("Critical path [ns]", "NaN");
	        }
	        if(!pCritAdded){
	        	this.simulation.add_simulation_result("Placement estimated critical path [ns]", "NaN"); 
	        }
	        if(!wireAdded){
	        	this.simulation.add_simulation_result("Total wirelength", "NaN");
	        }
	        if(!pWireAdded){
	        	this.simulation.add_simulation_result("Placement estimated total wirelength", "NaN");
	        }
	        if(!cWidthAdded){
	        	this.simulation.add_simulation_result("Channel width", "NaN");
	        }
	        if(!ptpAdded){
	        	this.simulation.add_simulation_result("PointToPoint connections", "NaN");
	        }
	        if(!rNetAdded){
	        	this.simulation.add_simulation_result("Routed nets", "NaN");
	        }
	        if(!ioAdded){
	        	this.simulation.add_simulation_result("IO Blocks", "NaN");
	        }
	        if(!pllAdded){
	        	this.simulation.add_simulation_result("PLL Blocks", "NaN");
	        }
	        if(!labAdded){
	        	this.simulation.add_simulation_result("LAB Blocks", "NaN");
	        }
	        if(!dspAdded){
	        	this.simulation.add_simulation_result("DSP Blocks", "NaN");
	        }
	        if(!m9kAdded){
	        	this.simulation.add_simulation_result("M9K Blocks", "NaN");
	        }
	        if(!m144kAdded){
	        	this.simulation.add_simulation_result("M144K Blocks", "NaN");
	        }
	        if(!sizeAdded){
        		this.simulation.add_simulation_result("SizeX", "NaN");
        		this.simulation.add_simulation_result("SizeY", "NaN");
	        }
        } catch (IOException e){
			e.printStackTrace();
        } catch (InterruptedException e) {
        	e.printStackTrace();
		}
	}
}
