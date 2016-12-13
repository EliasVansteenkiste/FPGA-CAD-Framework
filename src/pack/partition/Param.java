package pack.partition;

import pack.main.Simulation;
import pack.netlist.Netlist;
import pack.util.ErrorLog;
import pack.util.Output;
import pack.util.Util;

public class Param{
	private int nparts;
	private int ubfactor;
	private int nruns;
	private int cType;
	private int rType;
	private int vCycle;
	private int reconst;
	private int dbglvl;
	
	private int maxFanout;
		
	public Param(Simulation simulation){
		this.nparts = 2;

		this.reconst = 0;
		this.dbglvl = 0;
		
		String qualityString = "" + simulation.hmetisQuality() + "";
		int quality = Integer.parseInt(qualityString.substring(0, 1));
		this.ubfactor =  Integer.parseInt(qualityString.substring(1, 3));

		if(quality == 1){
			this.cType = 1;
			this.rType = 3;
			this.vCycle = 3;
			this.nruns = 10;
			Output.println("\t\tquality\truntime\tcut");
			Output.println("\t\t==> 1\t5,76 s\t471");
			Output.println("\t\t    2\t3,38 s\t483");
			Output.println("\t\t    3\t2,00 s\t504");
			Output.println("\t\t    4\t0,58 s\t606");
			Output.newLine();
		}else if(quality == 2){
			this.cType = 1;
			this.rType = 3;
			this.vCycle = 1;
			this.nruns = 10;
			Output.println("\t\tquality\truntime\tcut");
			Output.println("\t\t    1\t5,76 s\t471");
			Output.println("\t\t==> 2\t3,38 s\t483");
			Output.println("\t\t    3\t2,00 s\t504");
			Output.println("\t\t    4\t0,58 s\t606");
			Output.newLine();
		}else if(quality == 3){
			this.cType = 1;
			this.rType = 3;
			this.vCycle = 1;
			this.nruns = 5;
			Output.println("\t\tquality\truntime\tcut");
			Output.println("\t\t    1\t5,76 s\t471");
			Output.println("\t\t    2\t3,38 s\t483");
			Output.println("\t\t==> 3\t2,00 s\t504");
			Output.println("\t\t    4\t0,58 s\t606");
			Output.newLine();
		}else if(quality == 4){
			this.cType = 1;
			this.rType = 3;
			this.vCycle = 0;
			this.nruns = 2;
			Output.println("\t\tquality\truntime\tcut");
			Output.println("\t\t    1\t5,76 s\t471");
			Output.println("\t\t    2\t3,38 s\t483");
			Output.println("\t\t    3\t2,00 s\t504");
			Output.println("\t\t==> 4\t0,58 s\t606");
			Output.newLine();
		}else{
			ErrorLog.print("Unknown hmetis quality parameter => " + quality);
		}
		
		this.maxFanout = simulation.maxFanout();
	}
	public String getHMetisParameters(String tabs){
		int length = 12;
		String s = new String();
		s += tabs + "### hMetis parameters ###" + "\n";
		s += tabs + Util.fill("Nparts:", length) + this.nparts + "\n";
		s += tabs + Util.fill("UBfactor:", length) + this.ubfactor + "\n";
		s += tabs + Util.fill("Nruns:", length) + this.nruns + "\n";
		s += tabs + Util.fill("CType:", length) + this.cType + "\n";
		s += tabs + Util.fill("RType:", length) + this.rType + "\n";
		s += tabs + Util.fill("Vcycle:", length) + this.vCycle + "\n";
		s += tabs + Util.fill("Reconsts:", length) + this.reconst + "\n";
		s += tabs + Util.fill("dbglvl:", length) + this.dbglvl + "\n";
		s += tabs + Util.fill("max fanout:", length) + this.maxFanout + "\n";
		s += tabs + "#########################" + "\n";
		return s;
	}
	
	public int nparts(){
		return this.nparts;
	}
	public int maxFanout(){
		return this.maxFanout;
	}
	public String getGraphFile(String blif, int thread){
		return Util.localFolder() + "hmetis/files/" + blif + "_" + Util.getSimulationId() + "_" + thread;
	}
	public String[] getHMetisLine(String blif, int thread){
		return new String[]{Util.localFolder() + "hmetis/hmetis", this.getGraphFile(blif, thread), ""+this.nparts, ""+this.ubfactor, ""+this.nruns, ""+this.cType, ""+this.rType, ""+this.vCycle, ""+this.reconst, ""+this.dbglvl};
	}
	public void printHMetisLine(String blif, int thread){
    	for(String part:this.getHMetisLine(blif,thread)){
    		Output.print(part + " ");
    	}
    	Output.newLine();
	}
	public String getInfoLine(Netlist netlist, int edges, int criticalEdges, int metisIteration, int thread){
		int blockCount = netlist.atom_count();
		int area = netlist.get_area();
		
		StringBuffer outputLine = new StringBuffer();
    	outputLine.append("\t\t");
    	outputLine.append(netlist.get_blif() + " | ");
    	outputLine.append("Thread: " + Util.fill(thread, 2) + " | ");
    	outputLine.append("Blocks: " + Util.fill(blockCount, 7) + " | ");
    	outputLine.append("Area: "+ Util.fill(Util.parseDigit(area),7) + " | ");
    	outputLine.append("Parts: " + this.nparts + " | ");
    	outputLine.append(Util.fill(criticalEdges, 6) + " crit edges | ");
    	double percentageCritEdges = Util.round(((1.0*criticalEdges)/(1.0*edges)*100.0),2);
    	outputLine.append(Util.fill(percentageCritEdges, 5) + "% crit edges | ");
    	outputLine.append("hMetis it " + Util.fill(metisIteration, 3) + " | ");
		return outputLine.toString();
	}
}
