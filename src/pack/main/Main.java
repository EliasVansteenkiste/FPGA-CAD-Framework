package pack.main;

import pack.architecture.Architecture;
import pack.cluster.Cluster;
import pack.netlist.Netlist;
import pack.netlist.PathWeight;
import pack.partition.Partition;
import pack.util.Info;
import pack.util.Output;
import pack.util.Timing;

public class Main {
	public static void main(String[] args){
		
		Simulation simulation = new Simulation();
		simulation.parseArgs(args);

		Output.path(simulation);//Set path of output
		Output.println(simulation.toValueString());
		Output.newLine();

		//NETLIST
		Netlist netlist = new Netlist(simulation);

		//////// PACKING TIMER ////////
		Timing multiPartTimer = new Timing();
		multiPartTimer.start();
		///////////////////////////////

		//ARCHITECTURE
		Timing architecturetimer = new Timing();
		architecturetimer.start();

		Architecture arch1 = new Architecture(simulation);
		arch1.generate_light_architecture(netlist.get_models().keySet());

		Architecture arch2 = new Architecture(simulation);
		arch2.generate_pack_architecture(netlist.get_models().keySet());

		Architecture archLight = new Architecture(simulation);
		archLight.initialize();

		architecturetimer.end();
		Output.println("Architecture functionality took " + architecturetimer.toString());
		Output.newLine();

		netlist.floating_blocks();

		//Timing edges
		PathWeight path = new PathWeight(netlist, archLight, simulation);
		path.assign_net_weight();

		//DSP Pre-packing
		netlist.pre_pack_dsp();

		//Area assignment
		netlist.assign_area();

		//Pre-packing
		netlist.pre_pack_carry();
		netlist.pre_pack_share();
		netlist.pre_pack_mixed_width_ram();
		netlist.pre_pack_ram(archLight);//MOET ALS LAATSTE, GEBRUIKT INFO OVER AANTAL DSP BLOKKEN

		netlist.netlist_checker();

		Partition partition = new Partition(netlist, archLight, simulation, path.get_max_arr_time());
		
		Timing partitioningTimer = new Timing();
		partitioningTimer.start();
		partition.partitionate();
		partitioningTimer.end();
		Output.println("\tPartitioning took " + partitioningTimer.toString());
		Output.newLine();
		
		netlist.test_dsp_distribution();
		
		Info.finish(simulation);
		
		Cluster pack = new Cluster(netlist, archLight, partition, simulation);
		
		Timing seedBasedPackingTimer = new Timing();
		seedBasedPackingTimer.start();
		pack.packing();
		seedBasedPackingTimer.end();
		Output.println("\tSeed based packing took " + seedBasedPackingTimer.toString());
		Output.newLine();
		
		//////// PACKING TIMER ////////
		multiPartTimer.end();
		Output.println("\tMultiPart took " + multiPartTimer.toString());
		Output.newLine();
		///////////////////////////////
		
		pack.writeNetlistFile();
		
		Info.finish(simulation);
		
		Output.newLine();
		Output.flush();	
	}
}