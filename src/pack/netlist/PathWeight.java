package pack.netlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import pack.architecture.Architecture;
import pack.main.Simulation;
import pack.util.ErrorLog;
import pack.util.Output;
import pack.util.Timing;
import pack.util.Util;

public class PathWeight {
	private Netlist root;
	private Architecture arch;
	private Simulation simulation;
	
	private ArrayList<P> startPins;
	private ArrayList<P> endPins;
	
	private int maxArrivalTime;

	private int numEdges;
	private int timingEdges;
	
	private DelayMap delayMap;
	
	private ArrayList<CombLoop> tempCut;
	
	public PathWeight(Netlist netlist, Architecture architecture, Simulation simulation){
		this.root = netlist;
		this.arch = architecture;
		this.simulation = simulation;
		
		this.numEdges = 0;
		this.timingEdges = 0;
		
		this.tempCut = new ArrayList<CombLoop>();
	}
	public int get_max_arr_time(){
		return this.maxArrivalTime;
	}
	public void assign_net_weight(){
		Output.println("Timing edges:");
		
		Timing pathTimer = new Timing();
		pathTimer.start();
		
		this.initializeConnectionDelayMap();
		
		this.set_pins();
		this.cut_combinational_loops();
		this.initialise_timing_information();
		this.reconnect_combinational_loops();
		this.set_pins();
		
		Output.newLine();
		this.critical_edges();
		
		Output.println("\tThere are " + this.timingEdges + " timing edges added to the circuit out of a total of " + this.numEdges + " which is " + Util.round(((float)this.timingEdges)/this.numEdges*100,2) + "% of the total number of edges");
		Output.newLine();
		
		pathTimer.stop();
		Output.println("\tPath took " + pathTimer.time());
		Output.newLine();
	}
	private void initializeConnectionDelayMap(){
		this.delayMap = new DelayMap();
		for(N n:this.root.get_nets()){
			P sourcePin = null;
			if(n.has_source()){
				sourcePin = n.get_source_pin();
			}else{
				for(P p:n.get_terminal_pins()){
					if(p.is_start_pin()){
						sourcePin = p;
					}
				}
			}
			if(sourcePin == null){
				ErrorLog.print("Problem on net " + n.get_name());
			}
			for(P sinkPin:n.get_sink_pins()){
				int delay = this.arch.get_connection_delay(sourcePin, sinkPin);
				this.delayMap.addDelay(sourcePin, sinkPin, delay);
			}
			for(P terminalPin:n.get_terminal_pins()){
				if(terminalPin.is_end_pin()){
					int delay = this.arch.get_connection_delay(sourcePin, terminalPin);
					this.delayMap.addDelay(sourcePin, terminalPin, delay);
				}
			}
		}
	}
	private void initialise_timing_information(){
		boolean printDistribution = false;
		this.assign_arrival_time();
		this.max_arrival_time();
		if(printDistribution)this.arr_dist();
		
		this.assign_required_time();
		if(printDistribution)this.req_dist();
	}
	public void set_pins(){
		this.startPins = new ArrayList<P>();
		this.endPins = new ArrayList<P>();
		for(B b:this.root.get_blocks()){
			if(b.is_sequential()){	
				for(P pin:b.get_input_pins()){
					this.endPins.add(pin);
					if(!pin.is_end_pin()){
						Output.println("This should be an end pin");
					}
				}
				for(P pin:b.get_output_pins()){
					this.startPins.add(pin);
					if(!pin.is_start_pin()){
						Output.println("This should be a start pin");
					}
				}
			}
		}
		for(T terminal:this.root.get_terminals()){
			P pin = terminal.get_pin();
			if(pin.is_start_pin()){
				if(pin.get_net().has_source()){
					ErrorLog.print("A net can not have an input pin and a source block simultaneously");
				}
				this.startPins.add(pin);
			}else if(pin.is_end_pin()){
				this.endPins.add(pin);
			}else{
				ErrorLog.print("This terminal pin has an unrecognized type: " + terminal.get_type());
			}
		}
	}
	private void cut_combinational_loops(){
		this.tempCut = new ArrayList<CombLoop>();
		
		Timing t = new Timing();
		t.start();
		
		HashSet<P> analyzed = new HashSet<P>();
		HashSet<P> visited = new HashSet<P>();
		int comb = 0;
		for(P startPin:this.startPins){
			comb = this.analyze_pin(startPin, comb, visited, analyzed);
		}
		for(B b:this.root.get_blocks()){
			if(!b.has_inputs()){
				for(String outputPort:b.get_output_ports()){
					for(P startPin:b.get_output_pins(outputPort)){
						comb = this.analyze_pin(startPin, comb, visited, analyzed);
					}
				}
			}
		}
		
		t.stop();
		Output.println("\tFind combinational loops took " + t.toString() + " netlist has " + comb + " combinational loops");
		Output.newLine();
		
		//Add the cut sink pins to the end pins
		for(CombLoop combLoop:this.tempCut){
			this.endPins.add(combLoop.getPin());
		}
	}
	private void reconnect_combinational_loops(){
		for(CombLoop combLoop:this.tempCut){
			combLoop.reconnect(this.arch, this.delayMap);
		}
	}
	public int analyze_pin(P startPin, int comb, HashSet<P> visited, HashSet<P> analyzed){
		visited.add(startPin);
		for(P sinkPin:new HashSet<P>(startPin.get_net().get_sink_pins())){
		//for(P sinkPin:startPin.get_net().get_sink_pins()){
			if(sinkPin.has_block()){
				B b = sinkPin.get_block();
				if(b.is_sequential()){
					//END OF PATH
				}else{
					for(String outputPort:b.get_output_ports()){
						for(P sourcePin:b.get_output_pins(outputPort)){
							if(!visited.contains(sourcePin)){
								comb = this.analyze_pin(sourcePin, comb, visited, analyzed);
							}
						}
					}
				}
			}
		}
		if(startPin.has_block()){
			B b = startPin.get_block();
			if(b.is_sequential()){
				//DO NOTHING
			}else if(!b.has_inputs()){
				//DO NOTHING
			}else{
				HashSet<P> combPins = new HashSet<P>(b.get_input_pins());
				comb = this.analyze_pin_for_comb_loop(startPin, combPins, new HashSet<P>(), analyzed, comb);
				analyzed.add(startPin);
			}
		}
		return comb;
	}
	public int analyze_pin_for_comb_loop(P p, HashSet<P> combPins, HashSet<P> visited, HashSet<P> analyzed, int comb){
		if(analyzed.contains(p)){
			//ALREADY CHECKED
		}else if(visited.contains(p)){
			//ALREADY VISITED
		}else{
			visited.add(p);
			if(combPins.contains(p)){
				this.tempCut.add(new CombLoop(p));
				comb += 1;
			//SOURCE PIN
			}else if(p.is_source_pin()){
				for(P sinkPin:new HashSet<P>(p.get_net().get_sink_pins())){
				//for(P sinkPin:p.get_net().get_sink_pins()){
					comb = this.analyze_pin_for_comb_loop(sinkPin, combPins, visited, analyzed, comb);
				}
			//SINK PIN
			}else if(p.is_sink_pin()){
				if(p.has_block()){
					B b = p.get_block();
					if(b.is_sequential()){
						//END OF PATH
					}else{
						for(String outputPort:b.get_output_ports()){
							for(P sourcePin:b.get_output_pins(outputPort)){
								comb = this.analyze_pin_for_comb_loop(sourcePin, combPins, visited, analyzed, comb);
							}
						}
					}
				}else{
					//END OF PATH
				}
			}else{
				Output.println("A pin should be a source pin or a sink pin");
			}
		}
		return comb;
	}
	
	//DEPTH AND REQ
	private void assign_arrival_time(){
		for(P endPin:this.endPins){
			endPin.recursive_arrival_time(this.arch, this.delayMap);
		}
		for(B b:this.root.get_blocks()){
			if(!b.has_outputs()){
				for(String inputPort:b.get_input_ports()){
					for(P inputPin:b.get_input_pins(inputPort)){
						inputPin.recursive_arrival_time(this.arch, this.delayMap);
					}
				}
			}
		}
	}
	private void max_arrival_time(){
		this.maxArrivalTime = 0;
		for(P endPin:this.endPins){
			if(endPin.get_arrival_time() > this.maxArrivalTime){
				this.maxArrivalTime = endPin.get_arrival_time();
			}
		}
	}
	private void assign_required_time(){
		for(P endPin:this.endPins){
			endPin.set_required_time(this.maxArrivalTime);
		}
		for(P startPin:this.startPins){
			startPin.recursive_required_time(this.arch, this.delayMap);
		}
		for(B b:this.root.get_blocks()){
			if(!b.has_inputs()){
				for(String outputPort:b.get_output_ports()){
					for(P outputPin:b.get_output_pins(outputPort)){
						outputPin.recursive_required_time(this.arch, this.delayMap);
					}
				}
			}
		}
	}
	
	//PRINT DISTRIBUTION
	private void arr_dist(){
		Output.println("\tArrival time distribution [ps] (MAX " + this.maxArrivalTime + "ps):");
		int[] dist = new int[this.maxArrivalTime+1];
		for(int i = 0; i<this.maxArrivalTime+1; i++){
			dist[i] = 0;
		}
		for(P pin:this.endPins){
			dist[pin.get_arrival_time()] += 1;
		}
		Output.print("\t\t");
		for(int i=0; i<dist.length; i++){
			if(dist[i]>0){
				Output.print(i + "\t");
			}
		}
		Output.newLine();
		Output.print("\t\t");
		for(int i=0; i<dist.length; i++){
			if(dist[i]>0){
				Output.print(dist[i] + "\t");
			}
		}
		Output.newLine();
		Output.newLine();
		Output.newLine();
	}
	private void req_dist(){
		Output.println("\tRequired time distribution [ps]:");
		int[] dist = new int[this.maxArrivalTime+1];
		for(int i = 0; i<this.maxArrivalTime+1; i++){
			dist[i] = 0;
		}
		for(P pin:this.startPins){
			if(pin.get_required_time() > this.maxArrivalTime){
				//UNCONSTRAINED PIN (OCCURS WHEN A BLOCK HAS NO OUTPUT)
			}else{
				dist[pin.get_required_time()] += 1;
			}
		}
		Output.print("\t\t");
		for(int i=0; i<dist.length; i++){
			if(dist[i]>0){
				Output.print(i + "\t");
			}
		}
		Output.newLine();
		Output.print("\t\t");
		for(int i=0; i<dist.length; i++){
			if(dist[i]>0){
				Output.print(dist[i] + "\t");
			}
		}
		Output.newLine();
		Output.newLine();
	}
	
	//ASSIGN CRITICAL EDGES
	public void critical_edges(){

		Output.println("\t###############################################################");
		Output.println("\t########################### TIMING  ###########################");
		Output.println("\t###############################################################");
		Output.newLine();
		this.find_critical_edges_type();
		Output.println("\t###############################################################");
		Output.newLine();
	}
	public void find_critical_edges_type(){
		//THIS TYPE HAS A MORE COMPLEX METHOD TO DEFINE THE CRITICAL EDGES AS USED IN THE FPL2016 PAPER
		double minCriticality = this.simulation.getDoubleValue("min_crit");
		int maxPercentageCriticalEdges = this.simulation.getIntValue("max_per_crit_edge");
		
		Output.println("\tMinimum criticality               | " + minCriticality);
		Output.println("\tMaximum percentage critical edges | " + maxPercentageCriticalEdges);
		Output.newLine();

		//HashSet<P> edges = this.find_all_edges_and_assign_criticality_to_each_edge();
		//this.print_edge_criticality(edges);
		
		ArrayList<P> edges = new ArrayList<P>(this.find_all_edges_and_assign_criticality_to_each_edge());
		Collections.sort(edges, P.PinCriticalityComparator);
		
		//Test edge sort
		Timing t1 = new Timing();
		t1.start();
		double criticality1 = Double.MAX_VALUE;
		for(P p:edges){
			double localCriticality = p.criticality();
			if(localCriticality <= criticality1){
				criticality1 = localCriticality;
			}else{
				ErrorLog.print("Problem in sort");
			}
		}
		t1.stop();

		//Find critical edges
		ArrayList<P> criticalEdges = new ArrayList<P>();
		for(P edge:edges){
			if(edge.criticality() >= minCriticality){
				criticalEdges.add(edge);
			}
		}
		
		//Test criticalEdge sort
		Timing t2 = new Timing();
		t2.start();
		double criticality2 = Double.MAX_VALUE;
		for(P p:criticalEdges){
			double localCriticality = p.criticality();
			if(localCriticality <= criticality2){
				criticality2 = localCriticality;
			}else{
				ErrorLog.print("Problem in sort");
			}
		}
		t2.stop();
		
		Output.println("\tTest sort took " + (t1.time() + t2.time()));
		Output.newLine();
		
		//Limit amount of critical edges
		int maximumNumberOfCriticalEdges = (int)Math.round(edges.size()*maxPercentageCriticalEdges*0.01);
		Output.println("\tThe netlist has " + criticalEdges.size() + " critical edges, max is equal to " + maximumNumberOfCriticalEdges);
		if(criticalEdges.size() > maximumNumberOfCriticalEdges){
			criticalEdges = new ArrayList<P>(criticalEdges.subList(0, maximumNumberOfCriticalEdges));
			Output.println("\t\t=> The number of critical edges is limited to " + criticalEdges.size());
			Output.println("\t\t=> New criticality is equivalent to " + Util.round(criticalEdges.get(criticalEdges.size()-1).criticality(),3));
		}
		Output.newLine();
		
		this.assign_critical_edges(criticalEdges);
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	public HashSet<P> find_all_edges_and_assign_criticality_to_each_edge(){
		HashSet<P> edges = new HashSet<P>();
		this.numEdges = 0;
		for(N net:this.root.get_nets()){
			//Find source pin
			P sourcePin = null;
			if(net.has_source()){
				sourcePin = net.get_source_pin();
			}
			for(P terminalPin:net.get_terminal_pins()){
				if(terminalPin.is_source_pin()){
					if(sourcePin == null){
						sourcePin = terminalPin;
					}else{
						ErrorLog.print("Already a source pin found for this net: " + net.toString());
					}
				}
			}
			if(sourcePin == null){
				ErrorLog.print("No source pin found for this net: " + net.toString());
			}
			//Add all edges
			for(P sinkPin:net.get_sink_pins()){
				Integer slack = this.arch.slack(sourcePin, sinkPin);
				double criticality = 1.0 - (slack.doubleValue()/this.maxArrivalTime);
				
				sinkPin.set_criticality(criticality);
				edges.add(sinkPin);
				this.numEdges += 1;
			}
			for(P terminalPin:net.get_terminal_pins()){
				if(terminalPin.is_sink_pin()){
					P sinkPin = terminalPin;
					
					Integer slack = this.arch.slack(sourcePin, sinkPin);
					double criticality = 1.0 - (slack.doubleValue()/this.maxArrivalTime);
					
					sinkPin.set_criticality(criticality);
					edges.add(sinkPin);
					this.numEdges += 1;
				}
			}
		}
		if(edges.size() != this.numEdges){
			Output.println("Problem with num edges:");
			Output.println("\tedges.size(): " + edges.size());
			Output.println("\tthis.numEdges: " + this.numEdges);
			ErrorLog.print("Problem with num edges");
		}
		return edges;
	}
	public void assign_critical_edges(ArrayList<P> criticalEdges){
		int weightFactor = this.simulation.getIntValue("timing_weight");
		double multiplyFactor = this.simulation.getDoubleValue("multiply_factor");
		
		Output.println("\tAssign critical edges:");
		Output.println("\t\tMultiply factor           | " + multiplyFactor);
		Output.println("\t\tInter weight factor       | " + weightFactor);
		Output.println("\t\tIntra weight factor       | " + (int)Math.round(multiplyFactor*weightFactor));
		Output.newLine();
		
		for(P sinkPin:criticalEdges){
			N net = sinkPin.get_net();
			P sourcePin = null;
			if(net.has_source()){
				sourcePin = net.get_source_pin();
			}
			for(P terminalPin:net.get_terminal_pins()){
				if(terminalPin.is_source_pin()){
					if(sourcePin == null){
						sourcePin = terminalPin;
					}else{
						ErrorLog.print("Already a source pin found for this net: " + net.toString());
					}
				}
			}
			if(sourcePin == null){
				ErrorLog.print("No source pin found for this net: " + net.toString());
			}
			
			if(sourcePin.has_terminal() || sinkPin.has_terminal()){
				//GLOBAL INTERCONNECTION
				sinkPin.set_net_weight((int)(Math.round(sinkPin.criticality()*weightFactor)));
				this.timingEdges += 1;
			}else{
				String source = sourcePin.get_light_architecture_name();
				String sink = sinkPin.get_light_architecture_name();
				if(this.arch.is_connected_via_global_connection(source, sink)){
					//GLOBAL INTERCONNECTION
					sinkPin.set_net_weight((int)(Math.round(sinkPin.criticality()*weightFactor)));
					this.timingEdges += 1;
				}else{
					//LOCAL INTRACONNECTION
					sinkPin.set_net_weight((int)(Math.round(sinkPin.criticality()*multiplyFactor*weightFactor)));
					this.timingEdges += 1;
				}
			}
		}
		if(this.timingEdges != criticalEdges.size()){
			ErrorLog.print("Wrong number of timing edges =>\n\tCritical edges: " + criticalEdges.size() + "\n\tTiming edges: " + this.timingEdges);
		}
	}
}