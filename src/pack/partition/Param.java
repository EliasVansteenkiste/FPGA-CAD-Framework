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
	private double alpha;
	
	private int maxFanout;
	
	private String hmetis_folder;
	private String circuitName;
	private int simulationID;
		
	public Param(Simulation simulation){
		this.nparts = 2;

		this.reconst = 0;
		this.dbglvl = 0;
		
		int quality = simulation.getIntValue("hmetis_quality");
		this.ubfactor =  simulation.getIntValue("unbalance_factor");
		this.maxFanout = simulation.getIntValue("max_fanout");
		
		this.alpha = simulation.getDoubleValue("area_exponent_alpha");
		
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
		
		this.hmetis_folder = simulation.getStringValue("hmetis_folder");
		this.circuitName = simulation.getStringValue("circuit");
		this.simulationID = simulation.getSimulationID();
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
	public double alpha(){
		return this.alpha;
	}
	public String getGraphFile(int thread){
		return this.hmetis_folder + "files/" + this.circuitName + "_" + this.simulationID + "_" + thread;
	}
	public String[] getHMetisLine(int thread){
		return new String[]{this.hmetis_folder + "hmetis", this.getGraphFile(thread), Util.str(this.nparts), Util.str(this.ubfactor), Util.str(this.nruns), Util.str(this.cType), Util.str(this.rType), Util.str(this.vCycle), Util.str(this.reconst), Util.str(this.dbglvl)};
	}
	public void printHMetisLine(int thread){
    	for(String part:this.getHMetisLine(thread)){
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
	public String getHMetisFolder(){
		return this.hmetis_folder;
	}
	public String getCircuitName(){
		return this.circuitName;
	}
	public int getSimulationID(){
		return this.simulationID;
	}
}
