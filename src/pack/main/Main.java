package pack.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import pack.architecture.Architecture;
import pack.cluster.Cluster;
import pack.netlist.Netlist;
import pack.netlist.PathWeight;
import pack.partition.Partition;
import pack.util.ErrorLog;
import pack.util.Info;
import pack.util.Output;
import pack.util.RunVPR;
import pack.util.Timing;
import pack.util.Util;

public class Main {
	public static void main(String[] args){
		Util.setLocalFolder();
		for(String arg:args){
			ArrayList<Simulation> simulations = Main.read_simulations(arg);
			Output.path(arg);
			Util.setBenchmarkFolder(simulations.get(0).architecture().replace(".xml", ""));

			for(Simulation simulation:simulations){
				for(String blif:simulation.benchmarks()){
					simulation.set_current_benchmark(blif);
					Output.println("################################################################################");
					Output.println("\t\t\t\t" + blif);
					Output.println("################################################################################");
					Output.newLine();
					for(int it=0; it<simulation.iterations(); it++){
						Output.println("--------------------------------------------------------------------------------");
						Output.println("\t\t\t\t" + "iteration " + it);
						Output.println("--------------------------------------------------------------------------------");
						Util.makeRunFolder(blif);
						String runFolder = Util.run_folder();
						Output.addFile(runFolder + blif + ".txt");
						Util.copyFile(Util.benchmarkFolder() + blif + ".blif", runFolder + blif + ".blif");
						Util.copyFile(Util.benchmarkFolder() + blif + ".sdc", runFolder + blif + ".sdc");
						Util.copyArchFile(simulation.fixed_size(), Util.sizeX(simulation), Util.sizeY(simulation));
				
						if(!simulation.baseline()){
							//NETLIST
							Netlist netlist = new Netlist(blif, simulation);
							
							//////// PACKING TIMER ////////
							Timing multiPartTimer = new Timing();
							multiPartTimer.start();
							///////////////////////////////
							
							Architecture arch1 = new Architecture(simulation.architecture(), simulation);
							arch1.generate_light_architecture(netlist.get_models().keySet());
								
							Architecture arch2 = new Architecture(simulation.architecture(), simulation);
							arch2.generate_pack_architecture(netlist.get_models().keySet());
							
							//ARCHITECTURE
							Architecture archLight = new Architecture(simulation.architecture(), simulation);
							archLight.initialize();
							
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
							netlist.pre_pack_ram(simulation);//MOET ALS LAATSTE, GEBRUIKT INFO OVER AANTAL DSP BLOKKEN
							
							netlist.netlist_checker();
							
							Partition partition = new Partition(netlist, archLight, simulation, path.get_max_arr_time());
							
							Timing partitioningTimer = new Timing();
							partitioningTimer.start();
							partition.partitionate();
							partitioningTimer.end();
							Output.println("\tPartitioning took " + partitioningTimer.toString());
							simulation.addSimulationResult("Partitioning [s]", Util.round(partitioningTimer.time(),2));
							Output.newLine();
							
							netlist.test_dsp_distribution();
							
							Info.finish();
							
							Cluster pack = new Cluster(netlist, archLight, partition, simulation);
							
							Timing seedBasedPackingTimer = new Timing();
							seedBasedPackingTimer.start();
							pack.packing();
							seedBasedPackingTimer.end();
							Output.println("\tSeed based packing took " + seedBasedPackingTimer.toString());
							simulation.addSimulationResult("Seed based packing [s]", Util.round(seedBasedPackingTimer.time(),2));
							Output.newLine();
							
							//////// PACKING TIMER ////////
							multiPartTimer.end();
							Output.println("\tMultiPart took " + multiPartTimer.toString());
							simulation.addSimulationResult("MultiPart took", multiPartTimer.time());
							Output.newLine();
							///////////////////////////////
							
							pack.writeNetlistFile();
							
							Info.finish();

							//CLEAR ALL OBJECTS
							netlist = null;
							archLight = null;
							path = null;
							partition = null;
							pack = null;
							System.gc();
						}
						
						new RunVPR(runFolder, simulation);

						Output.newLine();
						Output.flush();
						Output.removeFile(runFolder + blif + ".txt");
					}
				}
				Output.println(simulation.get_results());
			}
			Output.newLine();
			Output.newLine();
			Output.println("################################################################################");
			Output.println("#################################### SUMMARY ###################################");
			Output.println("################################################################################");
			Output.newLine();
			Output.newLine();
			Output.println("################################################################################");
			Output.println("###################################### ALL #####################################");
			Output.println("################################################################################");
			Output.newLine();
			for(Simulation simulation:simulations){
				Output.println(simulation.get_results());
			}
			Output.newLine();
			Output.newLine();
			Output.println("################################################################################");
			Output.println("#################################### GEOMEAN ###################################");
			Output.println("################################################################################");
			Output.newLine();
			for(Simulation simulation:simulations){
				Output.println(simulation.print_geomean_results());
			}
			Output.flush();
		}
	}
	private static ArrayList<Simulation> read_simulations(String simulationFile){
		ArrayList<Simulation> simulations = new ArrayList<Simulation>();
		HashMap<String,ArrayList<String>> simulationSettings = Main.read_file(simulationFile);
		for(String partitions:simulationSettings.get("partitions")){
			for(String availableThreads:simulationSettings.get("threads")){
				for(String maxFanout:simulationSettings.get("max_fanout")){
					for(String hMetisQuality:simulationSettings.get("hmetis_quality")){
						for(String minCrit:simulationSettings.get("min_crit")){
							for(String maxPerCritEdge:simulationSettings.get("maxPerCritEdge")){
								for(String timingWeight:simulationSettings.get("timing_weight")){
									for(String multiplyFactor:simulationSettings.get("mult_factor")){
										Simulation simulation = new Simulation(
												simulationSettings.get("architecture").get(0),
												Integer.parseInt(simulationSettings.get("iterations").get(0)),
												Integer.parseInt(partitions),
												Integer.parseInt(availableThreads),
												Integer.parseInt(maxFanout),
												Integer.parseInt(hMetisQuality),
												Double.parseDouble(minCrit),
												Integer.parseInt(maxPerCritEdge),
												Integer.parseInt(timingWeight),
												Double.parseDouble(multiplyFactor),
												Boolean.parseBoolean(simulationSettings.get("fixed_size").get(0)),
												Boolean.parseBoolean(simulationSettings.get("place").get(0)),
												Boolean.parseBoolean(simulationSettings.get("route").get(0)),
												Boolean.parseBoolean(simulationSettings.get("baseline").get(0)));
										for(String bench:simulationSettings.get("benchmarks")){
											Main.test_benchmark(bench);
											simulation.add_benchmark(bench);
										}
										simulations.add(simulation);	
									}
								}
							}
						}
					}
				}
			}
		}
		return simulations;
	}
	//READ FILE
	private static HashMap<String,ArrayList<String>> read_file(String simulationFile){
		HashMap<String,ArrayList<String>> simulationSettings = new HashMap<String,ArrayList<String>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(Util.localFolder() + "simulation/" + simulationFile + ".txt"));
			String line = br.readLine();
			while (line != null) {
				line = line.replace("\\","");
				line = line.replace("}", "");
				line = line.replace("{", "");
				while(line.contains("f0 ")){
					line = line.replace("f0 ","f0");
				}
				line = line.replace("f0","");
				while(line.contains("f1 ")){
					line = line.replace("f1 ","f1");
				}
				line = line.replace("f1","");
				if(line.length() > 0){
					if(line.charAt(0) == '-'){
						line = line.replace("\t", "@");
						line = line.replace(" ", "@");
						line = line.replace(";", "@");
		
						while(line.contains("@@")){
							line = line.replace("@@", "@");
						}
						String type = null;
						ArrayList<String> values = new ArrayList<String>();
						for(String s:line.split("@")){
							if(s.charAt(0) == '-'){
								type = s.replace("-", "");
							}else{
								values.add(s);
							}
						}
						if(type == null){
							ErrorLog.print("Error on line: " + line);
						}else if(values.isEmpty()){
							ErrorLog.print("Error on line: " + line);
						}
						simulationSettings.put(type,values);
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.err.println("Problem in reading the BLIFfile: " + e.getMessage());
			e.printStackTrace();
		}
		return simulationSettings;
	}
	private static void test_benchmark(String benchmark){
		switch(benchmark){
			case "neuron":break;
			case "sparcT1_core":break;
			case "stereo_vision":break;
			case "cholesky_mc":break;
			case "des90":break;
			case "SLAM_spheric":break;
			case "segmentation":break;
			case "bitonic_mesh":break;
			case "dart":break;
			case "openCV":break;
			case "stap_qrd":break;
			case "minres":break;
			case "cholesky_bdti":break;
			case "sparcT2_core":break;
			case "denoise":break;
			case "gsm_switch":break;
			case "mes_noc":break;
			case "LU230":break;
			case "sparcT1_chip2":break;
			case "directrf":break;
			case "bitcoin_miner":break;
			
			case "EKF-SLAM":break;
			case "SURF_desc":break;
			case "CHERI":break;
			case "stap_steering":break;
			case "random":break;
			case "MCML":break;
			case "jacobi":break;
			case "JPEG":break;
			case "carpat":break;
			case "leon3mp":break;
			case "MMM":break;
			case "smithwaterman":break;
			case "CH_DFSIN":break;
			case "uoft_raytracer":break;
			case "Reed_Solomon":break;
			case "leon2":break;
			case "sudoku_check":break;
			case "radar20":break;
			case "wb_conmax":break;
			case "ucsb_152_tap_fir":break;
			
			default:ErrorLog.print(benchmark + " is not a valid benchmark");break;
		}
	}
}