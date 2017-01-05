package pack.partition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import pack.architecture.Architecture;
import pack.netlist.B;
import pack.netlist.N;
import pack.netlist.Netlist;
import pack.netlist.P;
import pack.util.ErrorLog;
import pack.util.Info;
import pack.util.Output;
import pack.util.Timing;
import pack.util.Util;

public class HMetis {
	private Netlist netlist;
	private int size;
	private Param param;
	
	private ArrayList<B> blocks;
	private Part[] result;
	
	private Process proc;
	private int thread;
	
	private HashMap<Integer,Integer> blockMap;
	
	private int metisIt;

	private ArrayList<Edge> edges;
	private ArrayList<Edge> criticalEdges;
	private Integer numberOfEdges;
	private Integer numberOfCriticalEdges;
	
	private boolean isRunning;
	private boolean isFinished;
	
	private double partitioningRuntime;
	private int numberOfcutEdges;

	
	public HMetis(Netlist netlist, int thread, int metisIt, Param param){
		this.netlist = netlist;
		this.size = this.netlist.atom_count();
		this.param = param;
		
		this.result = new Part[param.nparts()];
		for(int i=0; i<param.nparts(); i++){
			this.result[i] = new Part();
			this.result[i].setPartNumber(i);
		}
		this.blocks = new ArrayList<B>();
		for(B b:netlist.get_blocks()){
			this.blocks.add(b);
		}
		
		this.thread = thread;
		this.metisIt = metisIt;
		
		this.edges = new ArrayList<Edge>();
		this.criticalEdges = new ArrayList<Edge>();
		this.numberOfEdges = 0;
		this.numberOfCriticalEdges = 0;
		
		this.isFinished = false;
	}

	public Netlist getNetlist(){
		return this.netlist;
	}
	public int size(){
		return this.size;
	}
	public Part[] getResult(){
		return this.result;
	}

	public boolean isRunning(){
		if(this.isRunning == true){
			try {
		        this.proc.exitValue();
		        this.isRunning = false;
		        return false;
		    }catch (IllegalThreadStateException e) {
		    	this.isRunning = true;
		        return true;
		    }
		}else{
			return this.isRunning;
		}
	}
	public boolean isFinished(){
		return this.isFinished;
	}
    public void startRun(){
    	this.blockMap = new HashMap<Integer,Integer>();
		this.makeBlockMap();
    	this.writeToFile();
    	this.run_hMetis();
    }
    public void finishRun(){	
    	this.readHMetisTerminalOutput();
    	this.readHMetisResultsFromFile();
    	this.isFinished = true;
    }
    private void makeBlockMap(){
    	int blockNumber = 1;
    	for(B b:this.netlist.get_blocks()){
    		this.blockMap.put(b.get_number(), blockNumber++);
    	}
    }
    private void readHMetisTerminalOutput(){
		try {
			BufferedReader hMetisOutput = new BufferedReader(new InputStreamReader(this.proc.getInputStream()));
			String line = hMetisOutput.readLine();
			while(line != null){
				if(line.contains("Hyperedge Cut:")){
					String cut = line.replace("(minimize)", "");
					cut = cut.replace("Hyperedge Cut:", "");
					cut = cut.replace("\t", "");
					cut = cut.replace("\n", "");
					cut = cut.replace(" ", "");
					this.numberOfcutEdges = Integer.parseInt(cut);
				}else if(line.contains("Partitioning Time:")){
					String time = line.replace("Partitioning Time:", "");
					time = time.replace("sec", "");
					time = time.replace("\t", "");
					time = time.replace("\n", "");
					time = time.replace(" ", "");
					time = time.replace(".", ",");
					this.partitioningRuntime = Double.parseDouble(time.replace(",", "."));
				}
				line = hMetisOutput.readLine();
			}	
			hMetisOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	private void writeToFile(){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(this.param.getHMetisFolder() + "files/" + this.netlist.get_blif() + "_" + this.param.getSimulationID() + "_" + this.thread));
			int numberOfInternalNets = 0;
			for(N n:this.netlist.get_nets()){
				if(n.add_net_to_hmetis_partitioning(this.param.maxFanout())){
					numberOfInternalNets += 1;
					if(n.has_source()){
						for(P sinkPin:n.get_sink_pins()){
							this.numberOfEdges += 1;
							if(sinkPin.get_net_weight()>1){
								numberOfInternalNets += 1;
								this.numberOfCriticalEdges += 1;
							}
						}
					}
				}
			}
			
			//HEADER LINE
			int numberOfBlocks = this.netlist.block_count();
			bw.write(numberOfInternalNets + " " + numberOfBlocks + " " + "11");
			bw.newLine();
			
			//NETS
			for(N n:this.netlist.get_nets()){
				if(n.add_net_to_hmetis_partitioning(this.param.maxFanout())){
					this.writeConnections(n, bw);
				}
			}
			
			//BLOCKS
			int blockNumber = 1;
			for(B b:this.netlist.get_blocks()){
				if(this.blockMap.get(b.get_number()) != blockNumber){
					ErrorLog.print("Error in the block numbering");
				}
				int area = b.get_area();
				area = (int)Math.ceil(Math.pow(area, this.param.alpha()));
				
				//Safety build in to assure that alpha exponent is equal to zero
				if(area != 1){
					ErrorLog.print("Area should be equal to 1, instead it is equal to " + area);
				}
				bw.write(area + "");
				bw.newLine();

				blockNumber += 1;
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.err.println("Problem with writing to hmetis file: " + e.getMessage());
			e.printStackTrace();
		}
	}
	private void writeConnections(N parentNet, BufferedWriter writer){
		try {
			if(parentNet.has_source()){
				P sourcePin = parentNet.get_source_pin();
				B sourceBlock = sourcePin.get_block();
				if(parentNet.has_terminals()){
					writer.write("1");
				}else{
					writer.write("2");
				}
				writer.write(" " + this.blockMap.get(sourceBlock.get_number()));
				
				for(P sinkPin:parentNet.get_sink_pins()){
					B sinkBlock = sinkPin.get_block();
					writer.write(" " + this.blockMap.get(sinkBlock.get_number()));
				}
				if(parentNet.get_sink_pins().size() > this.param.maxFanout()){
					ErrorLog.print("Max fanout is equal to " + this.param.maxFanout() + " => " + parentNet.get_sink_pins().size());
				}
				writer.newLine();
				for(P sinkPin:parentNet.get_sink_pins()){
					Edge edge = new Edge(sourcePin, sinkPin, sinkPin.get_net_weight(), parentNet.get_name());
					this.edges.add(edge);
					if(sinkPin.get_net_weight() > 1){
						B sinkBlock = sinkPin.get_block();
						
						//PRINT CONNECTION
						writer.write(sinkPin.get_net_weight() + " " + this.blockMap.get(sourceBlock.get_number()) + " " + this.blockMap.get(sinkBlock.get_number()));
						writer.newLine();
						
						this.criticalEdges.add(edge);
					}
				}
			}else{
				if(parentNet.has_terminals()){
					writer.write("1");
				}else{
					ErrorLog.print("Unexpected situation, this net has no source so should have a terminal");
				}
				for(P sinkPin:parentNet.get_sink_pins()){
					B sinkBlock = sinkPin.get_block();
					writer.write(" " + this.blockMap.get(sinkBlock.get_number()));
				}
				if(parentNet.get_sink_pins().size() > this.param.maxFanout()){
					ErrorLog.print("Max fanout is equal to " + this.param.maxFanout() + " => " + parentNet.get_sink_pins().size());
				}
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void run_hMetis(){
		try{
        	ProcessBuilder pb = new ProcessBuilder(this.param.getHMetisLine(this.thread));	
        	this.proc = pb.start();
        	Output.println(this.param.getInfoLine(this.netlist, this.numberOfEdges, this.numberOfCriticalEdges, this.metisIt, this.thread));
        	//this.param.printHMetisLine(this.thread);
        } catch (IOException e) {
			e.printStackTrace();
		}
		this.isRunning = true;
	}
	public int numberOfCutEdges(){
		return this.numberOfcutEdges;
	}
	private void readHMetisResultsFromFile(){
		boolean printCutInformation = true;
		if(printCutInformation){
			Info.add("cut", "atom_count" + "\t" + this.netlist.atom_count() + "\t" + "cut_edges" + "\t" + this.numberOfcutEdges);	
		}
		
		boolean printPartitioningRuntime = true;
		if(printPartitioningRuntime){
			Info.add("hmetis", "atom_count" + "\t" + this.netlist.atom_count() + "\t" + "runtime" + "\t" + Util.str(this.partitioningRuntime).replace(".",","));	
		}
		
		String resultFile = this.param.getHMetisFolder() + "files/" + this.netlist.get_blif() + "_" + this.param.getSimulationID() + "_" + this.thread + ".part." + this.param.nparts();
		
		//CHECK IF FILE EXISTS
		if(!Util.fileExists(resultFile)){
			Output.newLine();
			this.param.printHMetisLine(this.thread);
        	Output.newLine();
			Output.println("Input file: " + this.param.getHMetisFolder() + "files/" + this.netlist.get_blif() + "_" + this.param.getSimulationID() + "_" + this.thread);
			Output.println("Result file: " + resultFile);
        	Output.newLine();
			ErrorLog.print("Problem with hMetis, result file not found: " + resultFile);
		}
		try {	
			BufferedReader reader = new BufferedReader(new FileReader(resultFile));
			String text = null;
			int blockNumber = 0;
			while ((text = reader.readLine()) != null) {
				int part = Integer.parseInt(text);
				B b = this.blocks.get(blockNumber);
				this.result[part].add(b);
				blockNumber += 1;
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Problem with reading from hmetis file: " + e.getMessage());
			e.printStackTrace();
		}
		
		//Delete the files
		File metisNetlistFile = new File(this.param.getHMetisFolder() + "files/" + this.netlist.get_blif() + "_" + this.param.getSimulationID()  + "_" + this.thread);
		metisNetlistFile.delete();
		File metisResultFile = new File(this.param.getHMetisFolder() + "files/" + this.netlist.get_blif() + "_" + this.param.getSimulationID()  + "_" + this.thread + ".part." + this.param.nparts());
		metisResultFile.delete();
	}
	public int getThreadNumber(){
		return this.thread;
	}
	public ArrayList<Edge> cutCriticalEdges(Architecture arch){
		ArrayList<Edge> result = new ArrayList<Edge>();
		int globalConnectionsCut = 0;
		int localConnectionsCut = 0;
		boolean printAllEdges = false;
		for(Edge critEdge:this.criticalEdges){
			P sourcePin = critEdge.getSource();
			P sinkPin = critEdge.getSink();
			if(sourcePin.get_block().get_part() != sinkPin.get_block().get_part()){
				if(printAllEdges)Output.println("\t\t\t" + "hMetis iteration " + this.metisIt + ": Edge between block " + critEdge.getSource().get_block().get_name() + "(" + critEdge.getSource().get_block().get_number() + ")" + " and block " + critEdge.getSink().get_block().get_name() + "(" + critEdge.getSink().get_block().get_number() + ")" + " on net " + critEdge.getName() + " with weight " + critEdge.getNetWeight() + " is cut | " + critEdge.getSource().get_block().get_type() + "." + critEdge.getSource().get_port() + " => " + critEdge.getSink().get_block().get_type() + "." + critEdge.getSink().get_port());
				result.add(critEdge);
				String source = sourcePin.get_light_architecture_name();
				String sink = sinkPin.get_light_architecture_name();
				if(arch.is_connected_via_global_connection(source, sink)){
					globalConnectionsCut += 1;
				}else{
					localConnectionsCut += 1;
				}
			}
		}
		if(!printAllEdges && !result.isEmpty())Output.println("\t\t\t" + "hMetis iteration " + this.metisIt + " | " + Util.fill(globalConnectionsCut, 3) + " global edges cut" + " | " + Util.fill(localConnectionsCut, 3) + " local edges cut");
		return result;
	}
	public void increasePinWeightOnPadWithCutEdge(Architecture arch){
		Timing t = new Timing();
		t.start();
		boolean increasePinWeightOnPadWithCutEdge = true;
		if(increasePinWeightOnPadWithCutEdge){
			for(Edge critEdge:this.criticalEdges){
				P sourcePin = critEdge.getSource();
				P sinkPin = critEdge.getSink();
				if(sourcePin.get_block().get_part() != sinkPin.get_block().get_part()){
					String source = sourcePin.get_light_architecture_name();
					String sink = sinkPin.get_light_architecture_name();
					if(!arch.is_connected_via_global_connection(source, sink)){//CRITICAL INTRA COMPLEX BLOCK CONNECTION IS CUT
						sinkPin.downStreamUpdate();
						sinkPin.upStreamUpdate();
					}
				}
			}
		}else{
			for(Edge critEdge:this.criticalEdges){
				P sourcePin = critEdge.getSource();
				P sinkPin = critEdge.getSink();
				if(sourcePin.get_block().get_part() != sinkPin.get_block().get_part()){
					Output.println("\t\t\tincreasePinWeightOnPadWithCutEdge is turned off");
					break;
				}
			}
		}
		t.end();
		if(t.time() > 0.2) Output.println("\t\t\tIncrease pin weight on pad with cut edge took " + t.toString());
	}
}