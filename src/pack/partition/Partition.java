package pack.partition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import pack.architecture.Architecture;
import pack.main.Simulation;
import pack.netlist.B;
import pack.netlist.Netlist;
import pack.util.ErrorLog;
import pack.util.Info;
import pack.util.Output;
import pack.util.ThreadPool;
import pack.util.Timing;
import pack.util.Util;

public class Partition{
	private Netlist root;
	private Architecture architecture;
	private Simulation simulation;
	
	private int maxNetlistSize;
	private Param param;
	private ThreadPool threadPool;
	
	private long startTime;
	private ArrayList<String> timeSteps;
	
	private Stack stack;
	private ArrayList<HMetis> hMetisPool;
	private HashMap<Thread, NetGen> netGenThreadPool;
	
	private int metisIt;
	private CutEdges cutEdges;
	
	private static final boolean debug = false;
	
	private int numberOfCutEdges = 0;
	
	public Partition(Netlist netlist, Architecture architecture, Simulation simulation, int maxDelay){
		this.root = netlist;
		this.architecture = architecture;
		this.simulation = simulation;

		Output.println("PHASE 1: PARTITIONING");
		Output.newLine();
		Output.println("\tSettings: ");

		//Stop criterium
		this.maxNetlistSize = this.simulation.getIntValue("max_pack_size");
		Output.println("\t\tMaximum netlist size: " + Util.parseDigit(this.maxNetlistSize));
		Output.newLine();
		
		Output.println("\t\tTiming edge weight update: " + this.simulation.getBooleanValue("timing_edge_weight_update"));
		Output.newLine();
		
		this.param = new Param(this.simulation);
		Output.print(this.param.getHMetisParameters("\t\t"));
		Output.newLine();
		
		this.metisIt = 0;
		this.cutEdges = new CutEdges(maxDelay);
		
		//Thread pool
		int poolSize = this.simulation.getIntValue("num_threads");
		Output.println("\t\tPartition pool size: " + poolSize);
		this.threadPool = new ThreadPool(poolSize);
				
		this.hMetisPool = new ArrayList<HMetis>();
		this.netGenThreadPool = new HashMap<Thread, NetGen>();
		Output.newLine();
		
		this.numberOfCutEdges = 0;
		
		this.deleteExistingFiles();
	}
	public void partitionate(){
		this.startTime = System.nanoTime();
		this.timeSteps = new ArrayList<>();
		
		//Stack
		this.stack = new Stack();
		
		//Partition
		Output.println("\tPartitionate netlist:");
		this.processChildNetlist(this.root);
		while(!this.stack.isEmpty() || !this.hMetisPool.isEmpty() || !this.netGenThreadPool.isEmpty()){
			this.startHMetis();
			this.finishHMetis();
			this.finishNetGen();
		}
		Output.newLine();
		
		//Testers
		this.eachParentHasTwoChildren();
		
		for(int i=1; i<this.timeSteps.size();i++){
			Info.add("partitioning", "subcircuit" + "\t" + i + "\t=>\t" + this.timeSteps.get(i).replace(".", ","));
		}
		
		int i = 1;
		int j = 1;
		while(i<this.timeSteps.size()){
			Info.add("hierarchylevel", "subcircuit" + "\t" + j + "\t" + this.timeSteps.get(i).replace(".", ","));
			i *= 2;
			j += 1;
		}
		
		Output.println("\tThere are " + this.numberOfCutEdges + " edges cut during partitioning");
		Output.newLine();
		
		Output.println("\t" + "A maximum of " + this.threadPool.maxUsage() + " threads is used during partitioning");
		Output.newLine();
	}
	
	//PARTITION
	public void startHMetis(){
		while(!this.threadPool.isEmpty() && !this.stack.isEmpty()){
			Netlist parent = this.stack.pullNetlist();
			int thread = this.threadPool.getThread();
			HMetis hMetis = new HMetis(parent, thread, this.metisIt, this.param);
			this.metisIt += 1;
			hMetis.startRun();
			this.hMetisPool.add(hMetis);
		}
	}
	public void finishHMetis(){
		HMetis hMetisMax = null;
		for(HMetis hMetis:this.hMetisPool){
			if(!hMetis.isRunning()){
				if(!hMetis.isFinished()){
					this.threadPool.addThread(hMetis.getThreadNumber());
					hMetis.finishRun();
				}		
				if(hMetisMax == null){
					hMetisMax = hMetis;
				}else if(hMetis.size() > hMetisMax.size()){
					hMetisMax = hMetis;
				}
			}
		}
		if(hMetisMax != null){
			this.hMetisPool.remove(hMetisMax);
			
			Netlist parent = hMetisMax.getNetlist();
			Part[] result = hMetisMax.getResult();
			
			this.numberOfCutEdges += hMetisMax.numberOfCutEdges();
		
			if(debug)this.analyzeParts(result);
			this.hardBlockSwap(result);
			
			//FINISH BIPARTITION
			this.finishPartition(parent, result, hMetisMax);
			
			if(parent.get_children().size() == 2){
				if(parent.get_level() != 0){
					parent.clean_up();
				}
			}
		}
	}
	public void finishNetGen(){
		for(Thread thread:new HashSet<Thread>(this.netGenThreadPool.keySet())){
			if(!thread.isAlive()){
				NetGen ng = this.netGenThreadPool.remove(thread);
				
				Netlist result = ng.result();
				Netlist parent = ng.parent();
				result.updateFamily(parent);
				
				this.threadPool.addThread(ng.thread());
				
				this.processChildNetlist(result);
				
				if(parent.get_children().size() == 2){
					if(parent.get_level() != 0){
						parent.clean_up();
					}
				}
			}
		}
	}
	public void finishPartition(Netlist parent, Part[] result, HMetis hMetis){
		this.testBipartition(parent, result);
		
		Part X,Y = null;
		if(result[0].size() > result[1].size()){//Process smallest benchmark first
			X = result[1];
			Y = result[0];
		}else{
			X = result[0];
			Y = result[1];
		}

		//CUT CRITICAL EDGES
		for(Edge critEdge:hMetis.cutCriticalEdges(this.architecture)){
			this.cutEdges.addCriticalEdge(critEdge);//THESE CRITICAL EDGES ARE ADDED TO SDC FILE
		}
		
		if(this.simulation.getBooleanValue("timing_edge_weight_update")){
			hMetis.increasePinWeightOnPadWithCutEdge(this.architecture);
		}
		
		if(!this.threadPool.isEmpty() && this.threadPool.size() > 1){
			NetGen ngx = new NetGen(X, parent, this.threadPool.getThread());
			Thread tx = new Thread(ngx);
			tx.start();
			if(this.netGenThreadPool.containsKey(tx)){
				ErrorLog.print("Duplicate thread!");
			}
			this.netGenThreadPool.put(tx, ngx);
		}else{
			Netlist childX = new Netlist(X, parent);
			childX.updateFamily(parent);
			this.processChildNetlist(childX);
		}
		if(!this.threadPool.isEmpty() && this.threadPool.size() > 1){
			NetGen ngy = new NetGen(Y, parent, this.threadPool.getThread());
			Thread ty = new Thread(ngy);
			ty.start();
			if(this.netGenThreadPool.containsKey(ty)){
				ErrorLog.print("Duplicate thread!");
			}
			this.netGenThreadPool.put(ty, ngy);
		}else{
			Netlist childY = new Netlist(Y, parent);
			childY.updateFamily(parent);
			this.processChildNetlist(childY);
		}
	}
	private void analyzeParts(Part[] result){
		for(Part part:result){
			int partNum = part.getPartNumber();
			for(B b:part.getBlocks()){
				if(b.get_part() != partNum){
					ErrorLog.print("PartNum is not equal to b.get_part()\n\tPartNum: " + partNum + "\n\tb.get_part(): " + b.get_part());
				}
			}
		}
	}
	public void hardBlockSwap(Part[] result){
		if(result[0].numDSPPrimitives() + result[1].numDSPPrimitives() > 0){
			SwapDSP swapDSP = new SwapDSP(result);
			swapDSP.run();
		}
		if(result[0].numRAMPrimitives() + result[1].numRAMPrimitives() > 0){
			SwapRAM swapRAM = new SwapRAM(result);
			swapRAM.run();
		}
	}
	public void processChildNetlist(Netlist child){
		if(child.atom_count() > this.maxNetlistSize){
			this.stack.pushNetlist(child);
		}
		this.timeSteps.add(Timing.currentTime(this.startTime) + "\t" + this.threadPool.usedThreads());
		this.startHMetis();
	}
	private void testBipartition(Netlist root, Part[] result){
		Info.add("partstat", "Netlist " + root.toString() + " has two parts of size " + result[0].size() + " and size " + result[1].size());
		int blockCount = 0;
		for(Part part:result){
			blockCount += part.size();
		}
		if(blockCount != root.block_count()){
			Output.println("Blocks lost during partitioning:\n\tPartition block count:\t" + blockCount + "\n\tParent block count:\t" + root.block_count());
		}
		if(result[0].isEmpty() || result[1].isEmpty()){
			ErrorLog.print("Error in bipartitioning" + "\n\tSize part[0] = " + result[0].size() + "\n\tSize part[1] = " + result[1].size());
		}
	}
	public CutEdges getCutEdges(){
		return this.cutEdges;
	}
	private void eachParentHasTwoChildren(){
		Netlist parent = this.root;
		ArrayList<Netlist> currentWork = new ArrayList<Netlist>();
		ArrayList<Netlist> nextWork = new ArrayList<Netlist>();
		nextWork.add(parent);
		
		while(nextWork.size()>0){
			currentWork = new ArrayList<Netlist>(nextWork);
			nextWork = new ArrayList<Netlist>();
			while(!currentWork.isEmpty()){
				parent = currentWork.remove(0);
				if(parent.has_children()){
					if(parent.get_children().size()==2){
						for(Netlist child:parent.get_children()){
							nextWork.add(child);
						}
					}else{
						Output.println("Netlist " + parent.toString() + " does not have 2 children: " + parent.get_children().size());
						for(Netlist child:parent.get_children()){
							nextWork.add(child);
						}
					}
				}
			}
		}
	}
	private void deleteExistingFiles(){
		File folder = new File(this.simulation.getStringValue("hmetis_folder") + "files/");
		File[] listOfFiles = folder.listFiles();
		for(int i = 0; i < listOfFiles.length; i++){
			File file = listOfFiles[i];
			if(file.isFile()){
				if(file.getName().contains(this.root.get_blif() + "_" + this.simulation.getSimulationID())){
					file.delete();
				}
			}
		}
	}
}