package pack.main;

import java.util.ArrayList;
import java.util.HashMap;

import pack.util.ErrorLog;
import pack.util.Util;

public class Simulation{
	
	//GENERAL CONFIGURATION
	private String architecture = null;
	private ArrayList<String> benchmarks = new ArrayList<String>();
	private String currentBenchmark = null;
	private Integer interations = null;
	
	//SIMULATION CONFIGURATION
	private Integer numSubcircuits = null;
	private Integer numThreads = null;
	private Integer maxFanout = null;
	private Integer hmetisQuality = null;
	private Double minCrit = null;
	private Integer maxPerCritEdge = null;
	private Integer timingWeight = null;
	private Double multiplyFactor = null;
	
	private Boolean fixedSize = null;
	
	private Boolean place = null;
	private Boolean route = null;
	private Boolean vprBaseline = null;
	
	//BENCHMARK //PARAMETER //ITERATIONS
	private ArrayList<String> parameters;
	private HashMap<String, HashMap<String, ArrayList<String>>> results;
	
	//INITIALISATION
	public Simulation(String architecture, int iterations, int numSubcircuits, int numThreads, int maxFanout, int hmetisQuality, double minCrit, int maxPerCritEdge, int timingWeigh, double multiplyFactor, boolean fixedSize, boolean place, boolean route, boolean vprBaseline){
		this.benchmarks = new ArrayList<String>();
	
		this.architecture = architecture;
		this.interations = iterations;
		
		this.numSubcircuits = numSubcircuits;
		this.numThreads = numThreads;
		this.maxFanout = maxFanout;
		this.hmetisQuality = hmetisQuality;
		this.minCrit = minCrit;
		this.maxPerCritEdge = maxPerCritEdge;
		this.timingWeight = timingWeigh;
		this.multiplyFactor = multiplyFactor;
		
		this.fixedSize = fixedSize;
		
		this.place = place;
		this.route = route;
		this.vprBaseline = vprBaseline;
		
		this.parameters = new ArrayList<String>();
		this.results = new HashMap<String, HashMap<String, ArrayList<String>>>();
	}
	public void add_benchmark(String benchmark){
		this.benchmarks.add(benchmark);
	}
	public void set_current_benchmark(String bench){
		this.currentBenchmark = bench;
	}
	
	//GETTERS
	public String architecture(){
		if(this.architecture == null) ErrorLog.print("This simulation has no value for architecture");
		return this.architecture;
	}
	public ArrayList<String> benchmarks(){
		if(this.benchmarks.isEmpty()) ErrorLog.print("This simulation has no values for benchmarks");
		return this.benchmarks;
	}
	public String benchmark(){
		if(this.currentBenchmark == null) ErrorLog.print("This simulation has no value for benchmark");
		return this.currentBenchmark;
	}
	public int iterations(){
		if(this.interations == null) ErrorLog.print("This simulation has no value for iterations");
		return this.interations;
	}

	public int numSubcircuits(){
		if(this.numSubcircuits == null) ErrorLog.print("This simulation has no value for partitions");
		return this.numSubcircuits;
	}
	public int numPartitionThreads(){
		if(this.numThreads == null) ErrorLog.print("This simulation has no value for metis pool");
		return this.numThreads;
	}
	public int numPackThreads(){
		if(this.numThreads == null) ErrorLog.print("This simulation has no value for pack pool");
		return this.numThreads;
	}
	public int maxFanout(){
		if(this.maxFanout == null) ErrorLog.print("This simulation has no value for max fanout");
		return this.maxFanout;
	}
	public int hmetisQuality(){
		if(this.hmetisQuality == null) ErrorLog.print("This simulation has no value for hmetis");
		return this.hmetisQuality;
	}
	public double min_crit(){
		if(this.minCrit == null) ErrorLog.print("This simulation has no value for min crit");
		return this.minCrit;
	}
	public int max_per_crit_edge(){
		if(this.maxPerCritEdge == null) ErrorLog.print("This simulation has no value for max percentage critical edges");
		return this.maxPerCritEdge;
	}
	public int timing_weight(){
		if(this.timingWeight == null) ErrorLog.print("This simulation has no value for timing weight");
		return this.timingWeight;
	}
	public double multiply_factor(){
		if(this.multiplyFactor == null) ErrorLog.print("This simulation has no value for multiply factor");
		return this.multiplyFactor;
	}
	public boolean fixed_size(){
		if(this.fixedSize == null) ErrorLog.print("This simulation has no value for fixed size");
		return this.fixedSize;
	}
	public boolean place(){
		if(this.place == null) ErrorLog.print("This simulation has no value for place");
		return this.place;
	}
	public boolean route(){
		if(this.route == null) ErrorLog.print("This simulation has no value for route");
		return this.route;
	}
	public boolean baseline(){
		if(this.vprBaseline == null) ErrorLog.print("This simulation has no value for VPR baseline");
		return this.vprBaseline;
	}
	
	//RESULTS
	public void addSimulationResult(String parameter, double value){
		this.add_simulation_result(parameter, Util.str(value));
	}
	public void add_simulation_result(String parameter, String value){
		if(!this.parameters.contains(parameter)){
			this.parameters.add(parameter);
		}
		if(!this.results.containsKey(this.currentBenchmark)){
			this.results.put(this.currentBenchmark, new HashMap<String,ArrayList<String>>());
		}
		if(!this.results.get(this.currentBenchmark).containsKey(parameter)){
			this.results.get(this.currentBenchmark).put(parameter, new ArrayList<String>());
		}
		this.results.get(this.currentBenchmark).get(parameter).add(value);
	}
	private String get_simulation_settings(){
		StringBuffer result = new StringBuffer();
		result.append("################################################################################" + "\n");
		result.append("############################# SIMULATION SETTINGS ##############################" + "\n");
		result.append("################################################################################" + "\n");
		result.append("Fixed size: " + this.fixedSize + "\n");
		result.append(Util.fill("Timing edge weight: " + this.timingWeight, 25) + " | " + Util.fill("Minimum criticality: " + this.minCrit, 25) + "\n");
		result.append(Util.fill("Metis pool: " + this.numThreads, 25) + " | Pack pool: " + this.numThreads + "\n");
		result.append(Util.fill("Partitions: " + this.numSubcircuits, 25) + " | " + Util.fill("hMetis: " + this.hmetisQuality, 25) + " | Max fanout: " + this.maxFanout + "\n");
		result.append("################################################################################" + "\n\n");
		return result.toString();
	}
	public String get_results(){
		StringBuffer result = new StringBuffer();
		
		result.append(this.get_simulation_settings());

		for(String parameter:this.parameters){
			result.append("--------------------------------------------------------------------------------" + "\n");
			result.append("\t\t\t\t" + parameter + "\n");
			result.append("--------------------------------------------------------------------------------" + "\n");
			for(String bench:this.benchmarks){
				if(bench.length()>6){
					result.append(bench.substring(0,6) + "\t");
				}else{
					result.append(bench + "\t");
				}
			}
			result.append("\n");
			for(int run = 0; run<this.iterations(); run ++){
				boolean tabReq = false;
				for(String bench:this.benchmarks){
					if(!tabReq)tabReq = true;
					else result.append("\t");
					if(this.results.containsKey(bench)){
						if(this.results.get(bench).containsKey(parameter)){
							if(this.results.get(bench).get(parameter).size() > run){
								result.append(this.results.get(bench).get(parameter).get(run).replace(".", ","));
							}else{
								result.append("NaN");
							}
						}else{
							result.append("NaN");
						}
					}else{
						result.append("NaN");
					}
				}
				result.append("\n");
			}
			result.append("\n");
		}
		return result.toString();
	}
	public String print_geomean_results(){
		StringBuffer result = new StringBuffer();
		
		result.append(this.get_simulation_settings());
		
		for(String parameter:this.parameters){
			ArrayList<Double> geomeanValues = new ArrayList<Double>();
			for(String bench:this.benchmarks){
				if(this.results.containsKey(bench)){
					if(this.results.get(bench).containsKey(parameter)){
						double sum = 0.0;
						int avCount = 0;
						for(int run = 0; run<this.iterations(); run ++){
							if(this.results.get(bench).get(parameter).size() > run){
								String value = this.results.get(bench).get(parameter).get(run);
								if(!value.equals("NaN")){
									double doubleVal = Double.parseDouble(value);
									if(doubleVal != 0.0){
										sum += doubleVal;
										avCount += 1;
									}
								}
							}
						}
						geomeanValues.add(sum/avCount);
					}
				}
			}
			double total = 1.0;
			for(double val:geomeanValues){
				total *= val;
			}
			double res = Math.pow(total, 1.0/geomeanValues.size());
			result.append(Util.fill(parameter, 20) + ": " + Util.str(Util.round(res,4)).replace(".", ",") + "\n");
		}
		result.append("################################################################################" + "\n");
		return result.toString();
	}
}