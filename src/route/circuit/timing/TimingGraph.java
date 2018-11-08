package route.circuit.timing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import route.util.Pair;
import route.circuit.Circuit;
import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.BlockType;
import route.circuit.architecture.PortType;
import route.circuit.block.AbstractBlock;
import route.circuit.block.GlobalBlock;
import route.circuit.block.LeafBlock;
import route.circuit.pin.AbstractPin;
import route.circuit.pin.GlobalPin;
import route.circuit.pin.LeafPin;
import route.circuit.resource.RouteNode;
import route.circuit.timing.TimingNode.Position;
import route.route.Connection;

public class TimingGraph {

    private static String VIRTUAL_IO_CLOCK = "virtual-io-clock";

    private Circuit circuit;

    // A map of clock domains, along with their unique id
    private Map<String, Integer> clockNamesToDomains = new HashMap<>();
    private int numClockDomains = 0;
    private int virtualIoClockDomain;

    private List<TimingNode> timingNodes = new ArrayList<>();
    private List<TimingNode> rootNodes, leafNodes;

    private List<TimingEdge> timingEdges  = new ArrayList<>();
    private List<List<TimingEdge>> timingNets = new ArrayList<>();

    private double globalMaxDelay;
    
    //Tarjan's strongly connected components algorithm
    private int index;
    private Stack<TimingNode> stack;
    private List<SCC> scc;

    public TimingGraph(Circuit circuit) {
        this.circuit = circuit;

        // Create the virtual io clock domain
        this.virtualIoClockDomain = 0;
        this.clockNamesToDomains.put(VIRTUAL_IO_CLOCK, this.virtualIoClockDomain);
    }

    /******************************************
     * These functions build the timing graph *
     ******************************************/

    public void build() {

        this.buildGraph();
        
        this.setRootAndLeafNodes();
        
        this.cutCombLoop();
        
        System.out.println("Timing Graph:");
        
        System.out.println("   Num timing nodes " + this.timingNodes.size());
        System.out.println("      Root " + this.rootNodes.size());
        System.out.println("      Leaf " + this.leafNodes.size());
        System.out.println("   Num timing edges " + this.timingEdges.size());
        System.out.println();
    }
    
    public void initializeTiming() {
    	this.calculatePlacementEstimatedWireDelay();
    	this.calculateArrivalAndRequiredTimes();
    	this.calculateEdgeCriticality();
    	
    	System.out.println("Timing information (based on placement estimated wire delay)");
        System.out.printf("   Max delay: %.3f\n", this.getMaxDelay());
        System.out.printf("   Timing cost: %.3e\n", this.calculateTotalCost());
        System.out.println();
    }

    private void buildGraph() {

        List<Double> clockDelays = new ArrayList<Double>();

        // Create all timing nodes
        for(BlockType leafBlockType : BlockType.getLeafBlockTypes()) {
            boolean isClocked = leafBlockType.isClocked();

            for(AbstractBlock abstractBlock : this.circuit.getBlocks(leafBlockType)) {
                LeafBlock block = (LeafBlock) abstractBlock;
                boolean isConstantGenerator = isClocked ? false : this.isConstantGenerator(block);

                // Get the clock domain and clock setup time
                int clockDomain = -1;
                double clockDelay = 0;
                if(isClocked) {
                    Pair<Integer, Double> clockDomainAndDelay = this.getClockDomainAndDelay(block);
                    clockDomain = clockDomainAndDelay.getFirst();

                    // We don't include the clock setup time in the critical path delay,
                    // because VPR also doesn't does this for the critical path. When you
                    // let VPR print out the critical path the clock setup time IS included,
                    // but it isn't included in the stdout estimation.
                    // clockDelay = clockDomainAndDelay.getSecond();
                } else if(isConstantGenerator) {
                    clockDomain = this.virtualIoClockDomain;
                }

                // If the block is clocked: create TimingNodes for the input pins
                if(isClocked) {
                    for(AbstractPin abstractPin : block.getInputPins()) {
                        if(abstractPin.getSource() != null) {
                            LeafPin inputPin = (LeafPin) abstractPin;
                            TimingNode node = new TimingNode(block.getGlobalParent(), inputPin, Position.LEAF, clockDomain);
                            inputPin.setTimingNode(node);

                            clockDelays.add(0.0);
                            this.timingNodes.add(node);
                        }
                    }
                }


                // Add the output pins of this timing graph root or intermediate node
                Position position = (isClocked || isConstantGenerator) ? Position.ROOT : Position.INTERMEDIATE;
                for(AbstractPin abstractPin : block.getOutputPins()) {
                    LeafPin outputPin = (LeafPin) abstractPin;
                    
                    if(outputPin.getNumSinks() > 0 && !this.isSourceOfClockNet(outputPin)) {
                    	
                    	TimingNode node = new TimingNode(block.getGlobalParent(), outputPin, position, clockDomain);
                    	outputPin.setTimingNode(node);

                    	this.timingNodes.add(node);

                    	if(position == Position.ROOT) {
                    		clockDelays.add(clockDelay + outputPin.getPortType().getSetupTime());
                    	} else {
                    		clockDelays.add(0.0);
                    	}
                    }
                }
            }
        }
        
        //Add timing nodes for the connections
        for(GlobalBlock globalBlock : this.circuit.getGlobalBlocks()) {
        	for(AbstractPin abstractPin : globalBlock.getOutputPins()) {
        		GlobalPin outputPin = (GlobalPin) abstractPin;
        		
        		AbstractPin current = outputPin;
        		while(current.getSource() != null) {
        			current = current.getSource();
        		}
        		
        		AbstractPin pathSource = current;
        		if(pathSource.hasTimingNode()) {
        			if(outputPin.getNumSinks() > 0) {
            			TimingNode node = new TimingNode(globalBlock, outputPin, Position.C_SOURCE, -1);
            			outputPin.setTimingNode(node);
            			
            			this.timingNodes.add(node);  
            			
            			clockDelays.add(0.0);
            		}
        		}
        	}
        }
        for(GlobalBlock globalBlock : this.circuit.getGlobalBlocks()) {
        	for(AbstractPin abstractPin : globalBlock.getInputPins()) {
        		GlobalPin inputPin = (GlobalPin) abstractPin;
        		
        		if(inputPin.getSource() != null) {
        			if(inputPin.getSource().hasTimingNode()) {
            			TimingNode node = new TimingNode(globalBlock, inputPin, Position.C_SINK, -1);
            			inputPin.setTimingNode(node);
            			
            			this.timingNodes.add(node);  
            			
            			clockDelays.add(0.0);
            		}
        		}
        	}
        }
        
        //Connect the timing nodes
        int numNodes = this.timingNodes.size();
        for(int i = 0; i < numNodes; i++) {
            TimingNode node = this.timingNodes.get(i);
            
            if(node.getPosition() != Position.LEAF && !this.isSourceOfClockNet(node)) {
                this.traverseFromSource(node, clockDelays.get(i));
            }
        }
        
        /******************************************
         *     Test functions to check if all     * 
         *     timing nodes are connected         *
         ******************************************/
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.ROOT) || node.getPosition().equals(Position.INTERMEDIATE)) {
        		if(node.getNumSinks() == 0) {
        			System.out.println(node + " " + node.getPosition() + " has no sinks");
        		}
        	}
        }
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.INTERMEDIATE) || node.getPosition().equals(Position.LEAF)) {
        		if(node.getNumSources() == 0) {
        			System.out.println(node + " " + node.getPosition() + " has no sources");
        		}
        	}
        }
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.C_SOURCE)) {
        		for(AbstractPin abstractPin : node.getPin().getSinks()) {
        			if(!abstractPin.hasTimingNode()) {
        				System.out.println(node.getPin() + " => " + abstractPin);
        			}
        		}
        	}
        }
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.C_SINK)) {
        		if(!node.getPin().getSource().hasTimingNode()) {
        			System.out.println(node.getPin().getSource() + " => " + node.getPin());
        		}
        	}
        }
        /*******************************************/
        
        for(TimingNode node : this.timingNodes) {
            node.compact();
        }
    }
    
    private boolean isSourceOfClockNet(TimingNode node) {
    	return this.isSourceOfClockNet(node.getPin());
    }
    private boolean isSourceOfClockNet(AbstractPin sourcePin) {
    	Stack<AbstractPin> work = new Stack<>();
    	work.add(sourcePin);
    	
    	while(!work.isEmpty()) {
    		AbstractPin current = work.pop();
    		if(current.getNumSinks() > 0) {
    			for(AbstractPin sink : current.getSinks()) {
    				work.add(sink);
    			}
    		} else {
    			if(!current.isClock()) return false;
    		}
    	}
    	//System.out.println("Source of clock net " + sourcePin);
    	return true;
    }
    
    private boolean isConstantGenerator(LeafBlock block) {
        for(AbstractPin inputPin : block.getInputPins()) {
            if(inputPin.getSource() != null) {
                return false;
            }
        }

        return true;
    }

    private Pair<Integer, Double> getClockDomainAndDelay(LeafBlock block) {
        /**
         * This method should only be called for clocked blocks.
         */

        String clockName;
        double clockDelay = 0;

        // If the block is an input
        if(block.getGlobalParent().getCategory() == BlockCategory.IO) {
            clockName = VIRTUAL_IO_CLOCK;

        // If the block is a regular block
        } else{
            // A clocked leaf block has exactly 1 clock pin
            List<AbstractPin> clockPins = block.getClockPins();
            assert(clockPins.size() == 1);
            AbstractPin clockPin = clockPins.get(0);

            while(true) {
                AbstractPin sourcePin = clockPin.getSource();
                if(sourcePin == null) {
                    break;
                }

                clockDelay += sourcePin.getPortType().getDelay(clockPin.getPortType());
                clockPin = sourcePin;
            }

            clockName = clockPin.getOwner().getName();
            assert(!clockName.equals(VIRTUAL_IO_CLOCK));

            if(!this.clockNamesToDomains.containsKey(clockName)) {
                this.clockNamesToDomains.put(clockName, this.numClockDomains);
                this.numClockDomains++;
            }
        }

        return new Pair<Integer, Double>(this.clockNamesToDomains.get(clockName), clockDelay);
    }

    private void traverseFromSource(TimingNode pathSourceNode, double clockDelay) {

        GlobalBlock pathSourceBlock = pathSourceNode.getGlobalBlock();
        AbstractPin pathSourcePin = pathSourceNode.getPin();

        Map<GlobalBlock, List<TimingEdge>> sourceTimingNets = new HashMap<>();

        Stack<TraversePair> todo = new Stack<>();
        todo.push(new TraversePair(pathSourcePin, clockDelay));

        while(!todo.empty()) {
            TraversePair traverseEntry = todo.pop();
            AbstractPin sourcePin = traverseEntry.pin;
            double delay = traverseEntry.delay;

            AbstractBlock sourceBlock = sourcePin.getOwner();
            PortType sourcePortType = sourcePin.getPortType();

            if((sourcePin.hasTimingNode() && sourcePin != pathSourcePin) || this.isEndpin(sourceBlock, sourcePin, pathSourcePin)) {
                delay += sourcePortType.getSetupTime();
                TimingNode pathSinkNode = sourcePin.getTimingNode();

                // If pathSinkNode is null, this sinkPin doesn't have any sinks
                // so isn't used in the timing graph
                if(pathSinkNode != null) {
                    TimingEdge edge = pathSourceNode.addSink(pathSinkNode, delay, this.circuit.getArchitecture().getDelayTables());
                    this.timingEdges.add(edge);

                    GlobalBlock pathSinkBlock = pathSinkNode.getGlobalBlock();
                    if(pathSinkBlock != pathSourceBlock) {
                        if(!sourceTimingNets.containsKey(pathSinkBlock)) {
                            sourceTimingNets.put(pathSinkBlock, new ArrayList<TimingEdge>());
                        }
                        sourceTimingNets.get(pathSinkBlock).add(edge);
                    }
                }

            } else {
                List<AbstractPin> sinkPins;
                if(sourceBlock.isLeaf() && sourcePin != pathSourcePin) {
                    sinkPins = sourceBlock.getOutputPins();
                } else {
                    sinkPins = sourcePin.getSinks();
                }

                for(AbstractPin sinkPin : sinkPins) {
                    if(sinkPin != null) {
                        double sourceSinkDelay = sourcePortType.getDelay(sinkPin.getPortType());
                        
                        if(sourceSinkDelay >= 0.0){ // Only consider existing connections
                            double totalDelay = delay + sourceSinkDelay;

                            // Loops around an unclocked block are rare, but not inexistent.
                            // The proper way to handle these is probably to add the loop
                            // delay to all other output pins of this block.
                            if(sinkPin != pathSourcePin) {
                                todo.push(new TraversePair(sinkPin, totalDelay));
                            }
                        }
                    }
                }
            }
        }

        for(List<TimingEdge> timingNet : sourceTimingNets.values()) {
            this.timingNets.add(timingNet);
        }
    }

    private boolean isEndpin(AbstractBlock block, AbstractPin pin, AbstractPin pathSourcePin) {
        return block.isLeaf() && (pin != pathSourcePin) && (block.isClocked() && pin.isInput() || pin.isOutput());
    }

    private class TraversePair {
        AbstractPin pin;
        double delay;

        TraversePair(AbstractPin pin, double delay) {
            this.pin = pin;
            this.delay = delay;
        }
    }

    private void setRootAndLeafNodes(){
    	this.rootNodes = new ArrayList<>();
    	this.leafNodes = new ArrayList<>();
    	
        for(TimingNode timingNode:this.timingNodes){
        	if(timingNode.getPosition().equals(Position.ROOT)){
        		this.rootNodes.add(timingNode);
        	}else if(timingNode.getPosition().equals(Position.LEAF)){
        		this.leafNodes.add(timingNode);
        	}
        }
    }

    /****************************************************
     * Functionality to find combinational loops with   *
     * Tarjan's strongly connected components algorithm *
     ****************************************************/
    private void cutCombLoop(){

    	long start = System.nanoTime();

    	System.out.println("Cut combinational loops iteratively");

    	int iteration = 0;
    	boolean finalIteration = false;
    	
    	while(!finalIteration){

    		int cutLoops = 0;
    		finalIteration = true;

    		//Initialize iteration
        	this.index = 0;
        	this.stack = new Stack<>();
        	this.scc = new ArrayList<>();
        	for(TimingNode v:this.timingNodes){
        		v.reset();
        	}

        	//Find SCC
        	for(TimingNode v:this.timingNodes){
        		if(v.undefined()){
        			strongConnect(v);
        		}
        	}

        	//Analyze SCC
        	for(SCC scc:this.scc){
        		if(scc.size > 1){
        			this.timingEdges.remove(scc.cutLoop());
        			finalIteration = false;
        			cutLoops++;
        		}
        	}
        	
        	System.out.println("\titeration " + iteration + " | " + cutLoops + " loops cut");
    	}

    	long end = System.nanoTime();
    	double time = (end - start) * 1e-9;

    	System.out.printf("\n\tcut loops took %.2f s\n\n", time);
    }
    private void strongConnect(TimingNode v){
    	v.setIndex(this.index);
    	v.setLowLink(this.index);
    	this.index++;

    	this.stack.add(v);
    	v.putOnStack();

    	for(TimingEdge e:v.getSinks()){
    		TimingNode w = e.getSink();
    		if(w.undefined()){
    			strongConnect(w);
    			
    			int lowLink = Math.min(v.getLowLink(), w.getLowLink());
    			v.setLowLink(lowLink);
    		}else if(w.onStack()){
    			int lowLink = Math.min(v.getLowLink(), w.getIndex());
    			v.setLowLink(lowLink);
    		}
    	}
    	
    	if(v.getLowLink() == v.getIndex()){
    		TimingNode w;
    		SCC scc = new SCC();
    		do{
    			w = this.stack.pop();
    			w.removeFromStack();
    			scc.addElement(w);
    		}while(w != v);
    		this.scc.add(scc);
    	}
    }
    
    public class SCC {
    	//Class for strongly connected component
    	List<TimingNode> elements;
    	int size;

    	SCC(){
    		this.elements = new ArrayList<>();
    		this.size = 0;
    	}

    	void addElement(TimingNode v){
    		this.elements.add(v);
    		this.size++;
    	}

    	TimingEdge cutLoop(){
    		TimingNode v = this.elements.get(this.elements.size()-1);

    		for(TimingEdge e:v.getSinks()){
    			TimingNode w = e.getSink();
    			if(w == this.elements.get(this.elements.size()-2)){
    	    		v.removeSink(e);
    	    		w.removeSource(e);

    	    		return e;
    			}
    		}
    		System.out.println("Edge not cut correctly");
    		return null;
    	}
    }

    /****************************************************************
     * These functions calculate the criticality of all connections *
     ****************************************************************/
    public double getMaxDelay() {
        return this.globalMaxDelay * 1e9;
    }

    public void calculatePlacementEstimatedWireDelay() {
    	for(TimingEdge edge : this.timingEdges) {
    		edge.calculatePlacementEstimatedWireDelay();
    	}
    }
    public void calculateActualWireDelay() {
    	//Set wire delay of the connections
    	for(Connection connection : this.circuit.getConnections()) {
    		double wireDelay = 0.0;
    		for(RouteNode routeNode : connection.routeNodes) {
    			wireDelay += routeNode.getDelay();
    		}
    		connection.timingEdge.setWireDelay(wireDelay);
    	}
    }
    public void calculateArrivalAndRequiredTimes() {
    	//Initialization
        for(TimingNode node : this.timingNodes) {
            node.resetArrivalTime();
            node.resetRequiredTime();
        }
        this.globalMaxDelay = 0;

    	//Arrival time
        for(TimingNode rootNode: this.rootNodes){
        	rootNode.setArrivalTime(0.0);
        }
        for(TimingNode leafNode: this.leafNodes){
        	leafNode.recursiveArrivalTime();
        	if(leafNode.getArrivalTime() > this.globalMaxDelay){
        		this.globalMaxDelay = leafNode.getArrivalTime();
        	}
        }

    	//Required time
    	for(TimingNode leafNode: this.leafNodes) {
    		leafNode.setRequiredTime(0.0);
    	}
        for(TimingNode rootNode: this.rootNodes) {
        	rootNode.recursiveRequiredTime();
        }
    }
    public void calculateEdgeCriticality() {
    	for(TimingEdge edge : this.timingEdges) {
    		edge.calculateCriticality(this.globalMaxDelay, 1, 1);
    	}
    }
    public void calculateConnectionCriticality(double maxCriticality, double criticalityExponent) {
        for(Connection connection : this.circuit.getConnections()) {
        	connection.timingEdge.calculateCriticality(this.globalMaxDelay, maxCriticality, criticalityExponent);
        }
    }
    
    public String criticalPathToString() {
    	List<TimingNode> criticalPath = new ArrayList<>();
		TimingNode node = this.getEndNodeOfCriticalPath();
		criticalPath.add(node);
		while(!node.getSources().isEmpty()){
    		node = this.getSourceNodeOnCriticalPath(node);
    		criticalPath.add(node);
    	}
    	
    	int maxLen = 25;
    	for(TimingNode criticalNode:criticalPath){
    		if(criticalNode.toString().length() > maxLen){
    			maxLen = criticalNode.toString().length();
    		}
    	}
    	
    	System.out.println();
    	String delay = String.format("Critical path: %.3f ns", this.globalMaxDelay * Math.pow(10, 9));
    	String result = String.format("%-" + maxLen + "s  %-3s %-3s  %-9s %-8s\n", delay, "x", "y", "Tarr (ns)", "LeafNode");
    	result += String.format("%-" + maxLen + "s..%-3s.%-3s..%-9s.%-8s\n","","","","","").replace(" ", "-").replace(".", " ");
    	for(TimingNode criticalNode:criticalPath){
    		result += this.printNode(criticalNode, maxLen);
    	}
    	
    	double netDelay = 0.0;
    	int numNets = 0;
    	for(TimingNode sinkNode : criticalPath) {
    		if(sinkNode.getPosition() == Position.C_SINK) {
    			netDelay += sinkNode.getArrivalTime() - sinkNode.getSources().get(0).getSource().getArrivalTime();
    			numNets++;
    		}
    	}
    	
    	result += "\nNum Nets  " +numNets + "\n";
    	result += "Net Delay " + String.format("%.2e", netDelay) + "\n";
    	return result;
    }
    private TimingNode getEndNodeOfCriticalPath(){
    	TimingNode endNode = null;
    	for(TimingNode leafNode: this.leafNodes){
    		if(compareDouble(leafNode.getArrivalTime(), this.globalMaxDelay)){
    			if(endNode == null){
    				endNode = leafNode;
    			}else{
    				System.out.println("Warning: more than one end node has an arrival time equal to the critical path delay");
    			}
    		}
    	}
    	return endNode;
    }
    private TimingNode getSourceNodeOnCriticalPath(TimingNode sinkNode){
    	TimingNode sourceNode = null;
		for(TimingEdge edge: sinkNode.getSources()){
			if(this.compareDouble(edge.getSource().getArrivalTime(), sinkNode.getArrivalTime() - edge.getTotalDelay())){
				if(sourceNode == null){
					sourceNode = edge.getSource();
				}else{
					sourceNode = edge.getSource();
					System.out.println("Warning: more than one source node on the critical path");
				}
			}
		}
		return sourceNode;
    }
    private String printNode(TimingNode node, int maxLen){
    	String nodeInfo = node.toString();
    	int x = node.getGlobalBlock().getColumn();
    	int y = node.getGlobalBlock().getRow();
    	double delay = node.getArrivalTime() * Math.pow(10, 9);
    	
    	return String.format("%-" + maxLen + "s  %-3d %-3d  %-9s\n", nodeInfo, x, y, String.format("%.3f", delay));
    }
    private boolean compareDouble(double var1, double var2){
    	return Math.abs(var1 - var2) < Math.pow(10, -12);
    }
    
    public double calculateTotalCost() {
    	double totalCost = 0;

    	for(List<TimingEdge> net : this.timingNets) {
    		double netCost = 0;
    		for(TimingEdge edge : net) {
    			double cost = edge.getCost();
    			if(cost > netCost) {
    				netCost = cost;
    			}
    		}
 
    		totalCost += netCost;
    	}

    	return totalCost;
    }
}
